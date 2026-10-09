package com.phonebook.controller;

import com.phonebook.dto.ContactCreateRequest;
import com.phonebook.dto.ContactResponse;
import com.phonebook.dto.ContactUpdateRequest;
import com.phonebook.dto.CsvImportResult;
import com.phonebook.entity.Contact;
import com.phonebook.service.ContactService;
import com.phonebook.service.CsvExportService;
import com.phonebook.service.CsvImportService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Contact management API. Paths, query parameters, status codes,
 * pagination headers and error bodies match the Python backend
 * so the existing Vue.js frontend works unchanged.
 */
@RestController
@Validated
@RequestMapping("/contacts")
public class ContactController {

  private final ContactService contactService;
  private final CsvImportService csvImportService;
  private final CsvExportService csvExportService;

  public ContactController(
      ContactService contactService,
      CsvImportService csvImportService,
      CsvExportService csvExportService) {
    this.contactService = contactService;
    this.csvImportService = csvImportService;
    this.csvExportService = csvExportService;
  }

  /** List contacts with server-side pagination and optional search. */
  @GetMapping
  public ResponseEntity<List<ContactResponse>> listContacts(
      @RequestParam(name = "page", defaultValue = "1") @Min(1) Integer page,
      @RequestParam(name = "page_size", defaultValue = "10") @Min(1) @Max(100) Integer page_size,
      @RequestParam(name = "q", required = false) String q,
      HttpServletResponse response) {
    PageRequest pageRequest = PageRequest.of(page - 1, page_size, Sort.by("id").ascending());
    Page<Contact> result =
        (q == null || q.isBlank())
            ? contactService.searchAll(pageRequest)
            : contactService.search(q, pageRequest);

    // Pagination metadata in headers so the body stays a plain list.
    response.setHeader("X-Total-Count", String.valueOf(result.getTotalElements()));
    response.setHeader("X-Page", String.valueOf(page));
    response.setHeader("X-Page-Size", String.valueOf(page_size));
    return ResponseEntity.ok(result.map(ContactResponse::from).getContent());
  }

  @PostMapping
  public ResponseEntity<ContactResponse> createContact(@Valid @RequestBody ContactCreateRequest request) {
    return ResponseEntity.status(201).body(contactService.create(request));
  }

  /** Import contacts from an uploaded UTF-8 CSV file. */
  @PostMapping("/import")
  public ResponseEntity<CsvImportResult> importContacts(
      @RequestParam("file") MultipartFile file) {
    return ResponseEntity.ok(csvImportService.importContacts(file));
  }

  /** Download all contacts (optionally filtered by a search term) as CSV. */
  @GetMapping("/export")
  public void exportContacts(
      @RequestParam(name = "q", required = false) String q, HttpServletResponse response)
      throws IOException {
    String filename =
        "phonebook-export-" + LocalDate.now(ZoneOffset.UTC).toString() + ".csv";
    response.setContentType("text/csv");
    response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
    csvExportService.writeCsv(response.getOutputStream(), q);
  }

  @GetMapping("/{id}")
  public ResponseEntity<ContactResponse> getContact(@PathVariable Long id) {
    return ResponseEntity.ok(ContactResponse.from(contactService.getContact(id)));
  }

  /** Update a contact; only fields provided by the caller are changed. */
  @PutMapping("/{id}")
  public ResponseEntity<ContactResponse> updateContact(
      @PathVariable Long id, @Valid @RequestBody ContactUpdateRequest request) {
    return ResponseEntity.ok(contactService.update(id, request));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteContact(@PathVariable Long id) {
    contactService.delete(id);
    return ResponseEntity.noContent().build();
  }
}
