package com.phonebook.service;

import com.phonebook.dto.ContactCreateRequest;
import com.phonebook.dto.CsvImportResult;
import com.phonebook.dto.CsvRowError;
import com.phonebook.entity.Contact;
import com.phonebook.exception.ApiException;
import com.phonebook.repository.ContactRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * CSV import with the exact behavior of the Python backend:
 * .csv files only, UTF-8 (BOM tolerated), max 5 MB / 5000 rows,
 * per-row validation with file row numbers, duplicate detection
 * against existing records and within the file, and a single
 * transaction for all valid rows.
 */
@Service
public class CsvImportService {

  static final List<String> CSV_COLUMNS = Arrays.asList("name", "phone_number", "email", "address");
  public static final int IMPORT_MAX_BYTES = 5 * 1024 * 1024; // 5 MB
  public static final int IMPORT_MAX_ROWS = 5000;

  private final ContactRepository contactRepository;
  private final Validator validator;

  public CsvImportService(ContactRepository contactRepository, Validator validator) {
    this.contactRepository = contactRepository;
    this.validator = validator;
  }

  @Transactional
  public CsvImportResult importContacts(MultipartFile file) {
    String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
    if (!filename.endsWith(".csv")) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "Only .csv files can be imported.");
    }

    // Read at most the size limit (plus one extra byte) so oversized
    // files are rejected without being fully loaded.
    byte[] content;
    try (InputStream input = file.getInputStream()) {
      content = readLimited(input);
    } catch (IOException e) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "The CSV file could not be read.");
    }
    if (content.length > IMPORT_MAX_BYTES) {
      throw new ApiException(
          HttpStatus.PAYLOAD_TOO_LARGE, "The CSV file is too large (maximum is 5 MB).");
    }

    String text = decodeUtf8(content);
    if (text.strip().isEmpty()) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "The CSV file is empty.");
    }

    List<Map<String, String>> validRows = new ArrayList<>();
    List<CsvRowError> errors = new ArrayList<>();
    try (CSVParser parser =
        CSVFormat.DEFAULT.builder().setHeader().build().parse(new StringReader(text))) {

      // Map the headers as written in the file to the canonical field
      // names (case-insensitive, whitespace-tolerant).
      Map<String, String> columnMap = new HashMap<>();
      for (String header : parser.getHeaderMap().keySet()) {
        if (header == null) {
          continue;
        }
        String canonical = header.strip().toLowerCase();
        if (CSV_COLUMNS.contains(canonical)) {
          columnMap.put(header, canonical);
        }
      }
      if (!columnMap.containsValue("name") || !columnMap.containsValue("phone_number")) {
        throw new ApiException(
            HttpStatus.BAD_REQUEST, "The CSV must include 'name' and 'phone_number' columns.");
      }

      for (CSVRecord record : parser) {
        // Commons CSV numbers data records starting at 1; the Python
        // backend reports reader.line_num where the header is line 1,
        // so the first data row must be reported as row 2.
        int rowNumber = (int) record.getRecordNumber() + 1;
        if (validRows.size() + errors.size() >= IMPORT_MAX_ROWS) {
          throw new ApiException(
              HttpStatus.PAYLOAD_TOO_LARGE,
              "The CSV file has too many rows (maximum is 5000).");
        }

        Map<String, String> payload = new HashMap<>();
        for (Map.Entry<String, String> entry : columnMap.entrySet()) {
          String value = record.get(entry.getKey());
          value = value == null ? "" : value.strip();
          // Empty optional cells are stored as NULL, like the web form.
          if (value.isEmpty()
              && ("email".equals(entry.getValue()) || "address".equals(entry.getValue()))) {
            payload.put(entry.getValue(), null);
          } else {
            payload.put(entry.getValue(), value);
          }
        }

        ContactCreateRequest dto =
            new ContactCreateRequest(
                payload.get("name"), payload.get("phone_number"), payload.get("email"), payload.get("address"));
        Set<ConstraintViolation<ContactCreateRequest>> violations = validator.validate(dto);
        if (!violations.isEmpty()) {
          List<String> messages =
              violations.stream()
                  .map(
                      violation ->
                          toSnakeCase(violation.getPropertyPath().toString())
                              + ": "
                              + violation.getMessage())
                  .toList();
          errors.add(new CsvRowError(rowNumber, messages));
          continue;
        }
        validRows.add(payload);
      }
    } catch (IOException e) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "The CSV file could not be parsed.");
    }

    // Pre-check existing phone numbers and emails so duplicates are
    // reported instead of aborting the import (same approach as the seeder).
    Set<String> usedPhones = new HashSet<>(contactRepository.findAllPhoneNumbers());
    Set<String> usedEmails = new HashSet<>();
    for (String email : contactRepository.findAllEmails()) {
      if (email != null) {
        usedEmails.add(email);
      }
    }

    int imported = 0;
    int duplicates = 0;
    List<Contact> toInsert = new ArrayList<>();
    for (Map<String, String> payload : validRows) {
      String phone = payload.get("phone_number");
      String email = payload.get("email");
      if (usedPhones.contains(phone) || (email != null && usedEmails.contains(email))) {
        duplicates++;
        continue;
      }
      Contact contact = new Contact();
      contact.setName(payload.get("name"));
      contact.setPhoneNumber(phone);
      contact.setEmail(email);
      contact.setAddress(payload.get("address"));
      toInsert.add(contact);
      usedPhones.add(phone);
      if (email != null) {
        usedEmails.add(email);
      }
      imported++;
    }

    // Single commit: either all valid, non-duplicate rows are stored
    // or (on a database error) nothing is stored.
    try {
      contactRepository.saveAll(toInsert);
      contactRepository.flush();
    } catch (DataIntegrityViolationException e) {
      throw new ApiException(
          HttpStatus.CONFLICT,
          "The import was cancelled because of a duplicate phone number or email. "
              + "No contacts were imported.");
    }

    return new CsvImportResult(
        validRows.size() + errors.size(), imported, errors.size(), duplicates, errors);
  }

  /** Converts a Java property path (camelCase) to the API field name (snake_case). */
  private static String toSnakeCase(String camelCase) {
    return camelCase.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
  }

  /** Reads at most IMPORT_MAX_BYTES + 1 bytes. */
  private static byte[] readLimited(InputStream input) throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    byte[] buffer = new byte[8192];
    int maxRead = IMPORT_MAX_BYTES + 1;
    int total = 0;
    int read;
    while (total < maxRead
        && (read = input.read(buffer, 0, Math.min(buffer.length, maxRead - total))) != -1) {
      out.write(buffer, 0, read);
      total += read;
    }
    return out.toByteArray();
  }

  /** Strict UTF-8 decoding with optional BOM (like Python's utf-8-sig). */
  private static String decodeUtf8(byte[] content) {
    int offset = 0;
    if (content.length >= 3
        && (content[0] & 0xFF) == 0xEF
        && (content[1] & 0xFF) == 0xBB
        && (content[2] & 0xFF) == 0xBF) {
      offset = 3;
    }
    CharsetDecoder decoder =
        StandardCharsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT);
    try {
      return decoder.decode(ByteBuffer.wrap(content, offset, content.length - offset)).toString();
    } catch (java.nio.charset.CharacterCodingException e) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "The CSV file must be UTF-8 encoded.");
    }
  }
}
