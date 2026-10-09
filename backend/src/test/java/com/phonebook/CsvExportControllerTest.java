package com.phonebook;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.phonebook.entity.User;
import com.phonebook.repository.ContactRepository;
import com.phonebook.repository.UserRepository;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/** CSV export tests (mirrors test_csv.py). */
@SpringBootTest
@AutoConfigureMockMvc
class CsvExportControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ContactRepository contactRepository;
  @Autowired private UserRepository userRepository;
  @Autowired private PasswordEncoder passwordEncoder;

  private String token;

  @BeforeEach
  void setUp() throws Exception {
    contactRepository.deleteAll();
    userRepository.deleteAll();
    User user = new User();
    user.setUsername("alice");
    user.setEmail("alice@example.com");
    user.setHashedPassword(passwordEncoder.encode("secret123"));
    userRepository.save(user);
    token = TestHelper.login(mockMvc, "alice", "secret123");
  }

  private Long createContact(String name, String phone, String email, String address)
      throws Exception {
    Map<String, Object> payload = new HashMap<>();
    payload.put("name", name);
    payload.put("phone_number", phone);
    payload.put("email", email);
    payload.put("address", address);
    String body =
        mockMvc
            .perform(
                post("/contacts")
                    .header("Authorization", TestHelper.authHeader(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        new com.fasterxml.jackson.databind.ObjectMapper()
                            .writeValueAsString(payload)))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return new com.fasterxml.jackson.databind.ObjectMapper().readTree(body).get("id").asLong();
  }

  private MvcResult export(String search) throws Exception {
    org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request =
        get("/contacts/export").header("Authorization", TestHelper.authHeader(token));
    if (search != null) {
      request.param("q", search);
    }
    return mockMvc.perform(request).andReturn();
  }

  private static List<List<String>> parseCsv(String csv) throws java.io.IOException {
    try (CSVParser parser = CSVFormat.DEFAULT.parse(new StringReader(csv))) {
      List<List<String>> rows = new ArrayList<>();
      for (CSVRecord record : parser) {
        List<String> values = new ArrayList<>();
        for (String value : record) {
          values.add(value);
        }
        rows.add(values);
      }
      return rows;
    }
  }

  @Test
  void exportAllContactsAcrossPages() throws Exception {
    for (int i = 0; i < 25; i++) {
      createContact(
          "Export User " + String.format("%02d", i),
          String.format("+919876%04d", i),
          "export" + i + "@example.com",
          "City " + i);
    }

    MvcResult result = export(null);

    assertThat(result.getResponse().getStatus()).isEqualTo(200);
    assertThat(result.getResponse().getContentType()).startsWith("text/csv");
    assertThat(result.getResponse().getHeader("Content-Disposition"))
        .contains("attachment")
        .contains("phonebook-export-");

    List<List<String>> rows = parseCsv(result.getResponse().getContentAsString());
    assertThat(rows.get(0)).containsExactly("name", "phone_number", "email", "address");
    assertThat(rows).hasSize(26); // header + 25 contacts
    List<String> names = new ArrayList<>();
    for (List<String> row : rows.subList(1, rows.size())) {
      names.add(row.get(0));
    }
    assertThat(names).contains("Export User 00", "Export User 24");
  }

  @Test
  void exportFilteredSearchResults() throws Exception {
    createContact("Rahul Sharma", "+919876543210", "rahul@example.com", "Pune");
    createContact("Amit Patil", "+919876543211", "amit@example.com", "Mumbai");
    createContact("Someone Else", "+919876543212", "else@example.com", "Delhi");

    MvcResult result = export("Rahul");

    assertThat(result.getResponse().getStatus()).isEqualTo(200);
    List<List<String>> rows = parseCsv(result.getResponse().getContentAsString());
    assertThat(rows).hasSize(2); // header + 1 match
    assertThat(rows.get(1).get(0)).isEqualTo("Rahul Sharma");
    // Phone numbers are prefixed by the CSV-injection guard.
    assertThat(rows.get(1).get(1)).isEqualTo("'+919876543210");
    assertThat(rows.get(1).get(3)).isEqualTo("Pune");
  }

  @Test
  void exportEmptyResultSet() throws Exception {
    MvcResult result = export("no-such-contact-xyz");

    assertThat(result.getResponse().getStatus()).isEqualTo(200);
    assertThat(result.getResponse().getContentAsString())
        .isEqualTo("name,phone_number,email,address\n");
  }

  @Test
  void exportCsvFormulaInjectionProtection() throws Exception {
    createContact("=SUM(A1:A2)", "+919876543210", "test@example.com", "@cmd|' /C calc'!A0");
    createContact("Minus User", "+919876543211", "minus@example.com", "-2+3");
    createContact("Normal User", "+919876543212", "normal@example.com", "Plain Street 1");

    MvcResult result = export(null);
    List<List<String>> rows = parseCsv(result.getResponse().getContentAsString());
    Map<String, List<String>> byName = new HashMap<>();
    for (List<String> row : rows.subList(1, rows.size())) {
      byName.put(row.get(0), row);
    }

    // Dangerous values are prefixed with a single quote...
    assertThat(byName.get("'=SUM(A1:A2)").get(1)).isEqualTo("'+919876543210");
    assertThat(byName.get("'=SUM(A1:A2)").get(3)).isEqualTo("'@cmd|' /C calc'!A0");
    assertThat(byName.get("Minus User").get(3)).isEqualTo("'-2+3");
    // ...while normal data is preserved as-is.
    assertThat(byName.get("Normal User").get(3)).isEqualTo("Plain Street 1");
  }

  @Test
  void exportEscapesCommasAndQuotes() throws Exception {
    createContact("Doe, John", "+919876543210", "john@example.com", "123 \"Main\" St");

    MvcResult result = export(null);
    String csv = result.getResponse().getContentAsString();

    // RFC 4180 quoting: fields with commas/quotes are quoted and
    // embedded quotes are doubled.
    assertThat(csv).contains("\"Doe, John\"");
    assertThat(csv).contains("\"123 \"\"Main\"\" St\"");
    List<List<String>> rows = parseCsv(csv);
    assertThat(rows.get(1).get(0)).isEqualTo("Doe, John");
    assertThat(rows.get(1).get(3)).isEqualTo("123 \"Main\" St");
  }

  @Test
  void exportRequiresAuthentication() throws Exception {
    mockMvc.perform(get("/contacts/export")).andExpect(status().isUnauthorized());
  }

  @Test
  void importStillWorksAlongsideExport() throws Exception {
    // The import and export endpoints must coexist with the
    // /contacts/{id} route.
    createContact("Route User", "+919876543210", "route@example.com", "Route City");

    MvcResult result = export("Route");
    assertThat(result.getResponse().getStatus()).isEqualTo(200);
    assertThat(result.getResponse().getContentAsString()).contains("Route User");

    // A non-numeric id segment is a validation error, not a route
    // collision with /contacts/export.
    mockMvc
        .perform(
            get("/contacts/export")
                .header("Authorization", TestHelper.authHeader(token)))
        .andExpect(status().isOk());
    assertThat(
            mockMvc
                .perform(
                    get("/contacts/not-a-number")
                        .header("Authorization", TestHelper.authHeader(token)))
                .andReturn()
                .getResponse()
                .getStatus())
        .isEqualTo(422);
  }
}
