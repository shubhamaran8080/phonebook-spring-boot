package com.phonebook.service;

import com.phonebook.entity.Contact;
import com.phonebook.repository.ContactRepository;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.stereotype.Service;

/**
 * CSV export with the exact behavior of the Python backend: every
 * contact matching the optional search term (not just one page),
 * LF line endings, and formula-injection protection for values that
 * start with "=", "+", "-" or "@".
 */
@Service
public class CsvExportService {

  private static final String[] CSV_COLUMNS = {"name", "phone_number", "email", "address"};

  private final ContactRepository contactRepository;

  public CsvExportService(ContactRepository contactRepository) {
    this.contactRepository = contactRepository;
  }

  public void writeCsv(OutputStream output, String search) throws IOException {
    List<Contact> contacts =
        (search == null || search.isBlank())
            ? contactRepository.findAll(org.springframework.data.domain.Sort.by("id").ascending())
            : contactRepository.searchAll(search);

    CSVFormat format = CSVFormat.DEFAULT.builder().setRecordSeparator("\n").build();
    try (Writer writer = new OutputStreamWriter(output, StandardCharsets.UTF_8);
        CSVPrinter printer = new CSVPrinter(writer, format)) {
      printer.printRecord(CSV_COLUMNS[0], CSV_COLUMNS[1], CSV_COLUMNS[2], CSV_COLUMNS[3]);
      for (Contact contact : contacts) {
        printer.printRecord(
            sanitize(contact.getName()),
            sanitize(contact.getPhoneNumber()),
            sanitize(contact.getEmail()),
            sanitize(contact.getAddress()));
      }
      printer.flush();
    }
  }

  /**
   * Values that spreadsheet applications could interpret as formulas
   * (every phone number, since they start with "+") are prefixed with
   * a single quote. The data itself is preserved.
   */
  private static String sanitize(String value) {
    if (value != null && !value.isEmpty() && "+=-@".indexOf(value.charAt(0)) >= 0) {
      return "'" + value;
    }
    return value == null ? "" : value;
  }
}
