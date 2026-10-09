package com.phonebook;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.phonebook.entity.User;
import com.phonebook.repository.ContactRepository;
import com.phonebook.repository.UserRepository;
import com.phonebook.service.CsvImportService;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.mock.web.MockMultipartFile;

/** CSV import tests (mirrors test_csv.py). */
@SpringBootTest
@AutoConfigureMockMvc
class CsvImportControllerTest {

  private static final String CSV_HEADER = "name,phone_number,email,address";

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

  private static String csvBody(String[]... rows) {
    StringBuilder sb = new StringBuilder(CSV_HEADER);
    for (String[] row : rows) {
      sb.append('\n').append(String.join(",", row));
    }
    sb.append('\n');
    return sb.toString();
  }

  private MvcResult importCsv(String filename, String content) throws Exception {
    MockMultipartFile file =
        new MockMultipartFile(
            "file", filename, "text/csv", content.getBytes(StandardCharsets.UTF_8));
    return mockMvc
        .perform(
            multipart("/contacts/import").file(file).header("Authorization", TestHelper.authHeader(token)))
        .andReturn();
  }

  private MvcResult importCsv(String filename, byte[] content) throws Exception {
    MockMultipartFile file = new MockMultipartFile("file", filename, "text/csv", content);
    return mockMvc
        .perform(
            multipart("/contacts/import").file(file).header("Authorization", TestHelper.authHeader(token)))
        .andReturn();
  }

  private MvcResult listContacts() throws Exception {
    return mockMvc
        .perform(get("/contacts").header("Authorization", TestHelper.authHeader(token)))
        .andReturn();
  }

  // -----------------------------------------------------------------
  // Success cases
  // -----------------------------------------------------------------

  @Test
  void importSingleContact() throws Exception {
    MvcResult result =
        importCsv(
            "contacts.csv",
            csvBody(new String[] {"Rahul Sharma", "+919876543210", "rahul@example.com", "Pune"}));

    assertThat(result.getResponse().getStatus()).isEqualTo(200);
    String body = result.getResponse().getContentAsString();
    assertThat(body).contains("\"total\":1");
    assertThat(body).contains("\"imported\":1");
    assertThat(body).contains("\"failed\":0");
    assertThat(body).contains("\"duplicates\":0");
    assertThat(body).contains("\"errors\":[]");

    String contacts = listContacts().getResponse().getContentAsString();
    assertThat(contacts).contains("Rahul Sharma");
    assertThat(contacts).contains("+919876543210");
    assertThat(contacts).contains("rahul@example.com");
    assertThat(contacts).contains("Pune");
  }

  @Test
  void importMultipleContacts() throws Exception {
    MvcResult result =
        importCsv(
            "contacts.csv",
            csvBody(
                new String[] {"A One", "+11111111111", "a.one@example.com", "City A"},
                new String[] {"B Two", "+22222222222", "b.two@example.com", "City B"},
                new String[] {"C Three", "+33333333333", "", ""}));

    assertThat(result.getResponse().getStatus()).isEqualTo(200);
    assertThat(result.getResponse().getContentAsString()).contains("\"imported\":3");

    // Empty optional cells are stored as NULL.
    mockMvc
        .perform(get("/contacts").header("Authorization", TestHelper.authHeader(token)))
        .andExpect(jsonPath("$.length()", is(3)))
        .andExpect(jsonPath("$[2].email", is((String) null)))
        .andExpect(jsonPath("$[2].address", is((String) null)));
  }

  @Test
  void importMinimalColumns() throws Exception {
    MvcResult result = importCsv("contacts.csv", "name,phone_number\nRahul Sharma,+919876543210\n");

    assertThat(result.getResponse().getStatus()).isEqualTo(200);
    assertThat(result.getResponse().getContentAsString()).contains("\"imported\":1");

    mockMvc
        .perform(get("/contacts").header("Authorization", TestHelper.authHeader(token)))
        .andExpect(jsonPath("$.length()", is(1)))
        .andExpect(jsonPath("$[0].email", is((String) null)))
        .andExpect(jsonPath("$[0].address", is((String) null)));
  }

  @Test
  void importPreservesExistingContacts() throws Exception {
    createContact("Existing User", "+919876543200", "existing@example.com");

    MvcResult result =
        importCsv(
            "contacts.csv",
            csvBody(new String[] {"New User", "+919876543210", "new@example.com", ""}));

    assertThat(result.getResponse().getContentAsString()).contains("\"imported\":1");

    mockMvc
        .perform(get("/contacts").header("Authorization", TestHelper.authHeader(token)))
        .andExpect(jsonPath("$.length()", is(2)))
        .andExpect(jsonPath("$[0].name", is("Existing User")))
        .andExpect(jsonPath("$[1].name", is("New User")));
  }

  @Test
  void importToleratesUtf8Bom() throws Exception {
    String content =
        csvBody(new String[] {"BOM User", "+919876543210", "bom@example.com", "BOM City"});
    byte[] withBom = new byte[3 + content.getBytes(StandardCharsets.UTF_8).length];
    withBom[0] = (byte) 0xEF;
    withBom[1] = (byte) 0xBB;
    withBom[2] = (byte) 0xBF;
    System.arraycopy(
        content.getBytes(StandardCharsets.UTF_8), 0, withBom, 3, content.getBytes(StandardCharsets.UTF_8).length);

    MvcResult result = importCsv("contacts.csv", withBom);

    assertThat(result.getResponse().getStatus()).isEqualTo(200);
    assertThat(result.getResponse().getContentAsString()).contains("\"imported\":1");
  }

  @Test
  void importToleratesCaseInsensitiveHeaders() throws Exception {
    MvcResult result =
        importCsv("contacts.csv", "Name,Phone_Number,EMAIL,Address\nCase User,+919876543210,c@example.com,X\n");

    assertThat(result.getResponse().getStatus()).isEqualTo(200);
    assertThat(result.getResponse().getContentAsString()).contains("\"imported\":1");
  }

  // -----------------------------------------------------------------
  // Duplicates
  // -----------------------------------------------------------------

  @Test
  void importDuplicatePhoneNumber() throws Exception {
    createContact("Existing User", "+919876543210", "existing@example.com");

    MvcResult result =
        importCsv(
            "contacts.csv",
            csvBody(new String[] {"New User", "+919876543210", "new@example.com", "X"}));

    assertThat(result.getResponse().getStatus()).isEqualTo(200);
    String body = result.getResponse().getContentAsString();
    assertThat(body).contains("\"imported\":0");
    assertThat(body).contains("\"duplicates\":1");
    assertThat(body).contains("\"failed\":0");

    mockMvc
        .perform(get("/contacts").header("Authorization", TestHelper.authHeader(token)))
        .andExpect(jsonPath("$.length()", is(1)))
        .andExpect(jsonPath("$[0].name", is("Existing User")));
  }

  @Test
  void importDuplicateEmail() throws Exception {
    createContact("Existing User", "+919876543210", "same@example.com");

    MvcResult result =
        importCsv(
            "contacts.csv",
            csvBody(new String[] {"New User", "+919876543299", "same@example.com", ""}));

    assertThat(result.getResponse().getContentAsString()).contains("\"duplicates\":1");
    assertThat(result.getResponse().getContentAsString()).contains("\"imported\":0");
  }

  @Test
  void importDuplicateWithinSameFile() throws Exception {
    MvcResult result =
        importCsv(
            "contacts.csv",
            csvBody(
                new String[] {"First User", "+919876543210", "first@example.com", ""},
                new String[] {"Second User", "+919876543210", "second@example.com", ""}));

    String body = result.getResponse().getContentAsString();
    assertThat(body).contains("\"imported\":1");
    assertThat(body).contains("\"duplicates\":1");
  }

  // -----------------------------------------------------------------
  // Validation errors
  // -----------------------------------------------------------------

  @Test
  void importInvalidEmail() throws Exception {
    MvcResult result =
        importCsv(
            "contacts.csv",
            csvBody(new String[] {"Bad Email", "+919876543210", "not-an-email", ""}));

    assertThat(result.getResponse().getStatus()).isEqualTo(200);
    String body = result.getResponse().getContentAsString();
    assertThat(body).contains("\"imported\":0");
    assertThat(body).contains("\"failed\":1");
    assertThat(body).contains("\"row\":2");
    assertThat(body).contains("email");

    mockMvc
        .perform(get("/contacts").header("Authorization", TestHelper.authHeader(token)))
        .andExpect(jsonPath("$.length()", is(0)));
  }

  @Test
  void importInvalidPhoneNumber() throws Exception {
    MvcResult result =
        importCsv("contacts.csv", csvBody(new String[] {"Bad Phone", "12345", "", ""}));

    String body = result.getResponse().getContentAsString();
    assertThat(body).contains("\"failed\":1");
    assertThat(body).contains("phone_number");
  }

  @Test
  void importMissingName() throws Exception {
    MvcResult result =
        importCsv("contacts.csv", csvBody(new String[] {"", "+919876543210", "", ""}));

    String body = result.getResponse().getContentAsString();
    assertThat(body).contains("\"failed\":1");
    assertThat(body).contains("name");
  }

  @Test
  void importPartialValidationErrors() throws Exception {
    MvcResult result =
        importCsv(
            "contacts.csv",
            csvBody(
                new String[] {"Good User", "+919876543210", "good@example.com", ""},
                new String[] {"Bad Phone", "12345", "", ""},
                new String[] {"Another Good", "+919876543212", "another@example.com", ""}));

    String body = result.getResponse().getContentAsString();
    assertThat(body).contains("\"total\":3");
    assertThat(body).contains("\"imported\":2");
    assertThat(body).contains("\"failed\":1");
    // "Bad Phone" is on line 3 of the file.
    assertThat(body).contains("\"row\":3");

    mockMvc
        .perform(get("/contacts").header("Authorization", TestHelper.authHeader(token)))
        .andExpect(jsonPath("$.length()", is(2)));
  }

  // -----------------------------------------------------------------
  // Malformed input and limits
  // -----------------------------------------------------------------

  @Test
  void importEmptyFile() throws Exception {
    MvcResult result = importCsv("contacts.csv", "");

    assertThat(result.getResponse().getStatus()).isEqualTo(400);
    assertThat(result.getResponse().getContentAsString().toLowerCase()).contains("empty");
  }

  @Test
  void importHeadersOnly() throws Exception {
    MvcResult result = importCsv("contacts.csv", CSV_HEADER + "\n");

    assertThat(result.getResponse().getStatus()).isEqualTo(200);
    String body = result.getResponse().getContentAsString();
    assertThat(body).contains("\"total\":0");
    assertThat(body).contains("\"imported\":0");
    assertThat(body).contains("\"failed\":0");
    assertThat(body).contains("\"duplicates\":0");
  }

  @Test
  void importMalformedCsvMissingHeader() throws Exception {
    MvcResult result = importCsv("contacts.csv", "Rahul,+919876543210,rahul@example.com,Pune\n");

    assertThat(result.getResponse().getStatus()).isEqualTo(400);
    assertThat(result.getResponse().getContentAsString()).contains("columns");
  }

  @Test
  void importInvalidEncoding() throws Exception {
    MvcResult result =
        importCsv("contacts.csv", new byte[] {(byte) 0xFF, (byte) 0xFE, 0x00, 0x00, 'g', 'a', 'r', 'b', 'a', 'g', 'e'});

    assertThat(result.getResponse().getStatus()).isEqualTo(400);
    assertThat(result.getResponse().getContentAsString()).contains("UTF-8");
  }

  @Test
  void importUnsupportedFileType() throws Exception {
    MvcResult result =
        importCsv(
            "contacts.txt",
            csvBody(new String[] {"A", "+919876543210", "", ""}));

    assertThat(result.getResponse().getStatus()).isEqualTo(400);
    assertThat(result.getResponse().getContentAsString().toLowerCase()).contains("csv");
  }

  @Test
  void importTooManyRows() throws Exception {
    StringBuilder sb = new StringBuilder(CSV_HEADER);
    for (int i = 0; i <= CsvImportService.IMPORT_MAX_ROWS; i++) {
      sb.append("\nUser ")
          .append(i)
          .append(",+1555")
          .append(String.format("%010d", i))
          .append(",user")
          .append(i)
          .append("@example.com,");
    }
    sb.append('\n');

    MvcResult result = importCsv("contacts.csv", sb.toString());

    assertThat(result.getResponse().getStatus()).isEqualTo(413);
    assertThat(result.getResponse().getContentAsString().toLowerCase()).contains("rows");
  }

  @Test
  void importOversizedFile() throws Exception {
    byte[] content = new byte[5 * 1024 * 1024 + 1];
    Arrays.fill(content, (byte) 'x');

    MvcResult result = importCsv("contacts.csv", content);

    assertThat(result.getResponse().getStatus()).isEqualTo(413);
  }

  @Test
  void importRequiresAuthentication() throws Exception {
    MockMultipartFile file =
        new MockMultipartFile(
            "file",
            "contacts.csv",
            "text/csv",
            csvBody(new String[] {"A", "+919876543210", "", ""}).getBytes(StandardCharsets.UTF_8));

    mockMvc.perform(multipart("/contacts/import").file(file)).andExpect(status().isUnauthorized());
  }

  @Test
  void importMissingFilePart() throws Exception {
    mockMvc
        .perform(
            multipart("/contacts/import").header("Authorization", TestHelper.authHeader(token)))
        .andExpect(status().isUnprocessableEntity());
  }

  // -----------------------------------------------------------------
  // Helpers
  // -----------------------------------------------------------------

  private void createContact(String name, String phone, String email) throws Exception {
    Map<String, Object> payload = new HashMap<>();
    payload.put("name", name);
    payload.put("phone_number", phone);
    payload.put("email", email);
    mockMvc
        .perform(
            post("/contacts")
                .header("Authorization", TestHelper.authHeader(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    new com.fasterxml.jackson.databind.ObjectMapper()
                        .writeValueAsString(payload)))
        .andExpect(status().isCreated());
  }
}
