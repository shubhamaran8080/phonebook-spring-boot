package com.phonebook;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.phonebook.entity.User;
import com.phonebook.repository.ContactRepository;
import com.phonebook.repository.UserRepository;
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

/** Contact CRUD tests (mirrors test_contacts.py). */
@SpringBootTest
@AutoConfigureMockMvc
class ContactControllerTest {

  private static final Map<String, Object> VALID_CONTACT =
      Map.of(
          "name", "John Doe",
          "phone_number", "+1234567890",
          "email", "john@example.com",
          "address", "123 Main St");

  @Autowired private MockMvc mockMvc;
  @Autowired private ContactRepository contactRepository;
  @Autowired private UserRepository userRepository;
  @Autowired private PasswordEncoder passwordEncoder;

  @BeforeEach
  void setUp() {
    contactRepository.deleteAll();
    userRepository.deleteAll();
    User user = new User();
    user.setUsername("alice");
    user.setEmail("alice@example.com");
    user.setHashedPassword(passwordEncoder.encode("secret123"));
    userRepository.save(user);
  }

  private String token() throws Exception {
    return TestHelper.login(mockMvc, "alice", "secret123");
  }

  private MvcResult createContact(Map<String, Object> payload) throws Exception {
    return mockMvc
        .perform(
            post("/contacts")
                .header("Authorization", TestHelper.authHeader(token()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(new com.fasterxml.jackson.databind.ObjectMapper()
                    .writeValueAsString(payload)))
        .andReturn();
  }

  @Test
  void createContact() throws Exception {
    MvcResult result = createContact(VALID_CONTACT);

    assertThat(result.getResponse().getStatus()).isEqualTo(201);
    String body = result.getResponse().getContentAsString();
    assertThat(body).contains("\"name\":\"John Doe\"");
    assertThat(body).contains("\"phone_number\":\"+1234567890\"");
    assertThat(body).contains("\"email\":\"john@example.com\"");
    assertThat(body).contains("\"address\":\"123 Main St\"");
    assertThat(body).contains("\"id\":");
    assertThat(body).contains("\"created_at\":");
  }

  @Test
  void createContactMissingRequiredName() throws Exception {
    mockMvc
        .perform(
            post("/contacts")
                .header("Authorization", TestHelper.authHeader(token()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"phone_number\":\"+1234567890\",\"email\":\"john@example.com\"}"))
        .andExpect(status().isUnprocessableEntity());
  }

  @Test
  void createContactInvalidPhoneNumber() throws Exception {
    for (String bad : new String[] {"12345", "abc", "+0123456789", "+(123)456-7890"}) {
      Map<String, Object> payload = new java.util.HashMap<>(VALID_CONTACT);
      payload.put("phone_number", bad);
      MvcResult result = createContact(payload);
      assertThat(result.getResponse().getStatus())
          .as("phone number %s must be rejected", bad)
          .isEqualTo(422);
    }
  }

  @Test
  void createContactInvalidEmail() throws Exception {
    Map<String, Object> payload = new java.util.HashMap<>(VALID_CONTACT);
    payload.put("email", "not-an-email");

    mockMvc
        .perform(
            post("/contacts")
                .header("Authorization", TestHelper.authHeader(token()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    new com.fasterxml.jackson.databind.ObjectMapper()
                        .writeValueAsString(payload)))
        .andExpect(status().isUnprocessableEntity());
  }

  @Test
  void createContactDuplicatePhoneNumber() throws Exception {
    createContact(VALID_CONTACT);

    mockMvc
        .perform(
            post("/contacts")
                .header("Authorization", TestHelper.authHeader(token()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    new com.fasterxml.jackson.databind.ObjectMapper()
                        .writeValueAsString(VALID_CONTACT)))
        .andExpect(status().isConflict())
        .andExpect(
            jsonPath(
                "$.detail",
                is("A contact with this phone number or email already exists.")));
  }

  @Test
  void createContactDuplicateEmail() throws Exception {
    createContact(VALID_CONTACT);
    Map<String, Object> payload =
        Map.of(
            "name", "Jane Doe",
            "phone_number", "+1987654321",
            "email", "john@example.com");

    mockMvc
        .perform(
            post("/contacts")
                .header("Authorization", TestHelper.authHeader(token()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    new com.fasterxml.jackson.databind.ObjectMapper()
                        .writeValueAsString(payload)))
        .andExpect(status().isConflict());
  }

  @Test
  void listContactsEmpty() throws Exception {
    mockMvc
        .perform(get("/contacts").header("Authorization", TestHelper.authHeader(token())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", is(java.util.Collections.emptyList())))
        .andExpect(jsonPath("$.size()", is(0)));
  }

  @Test
  void getContact() throws Exception {
    MvcResult created = createContact(VALID_CONTACT);
    Long id =
        new com.fasterxml.jackson.databind.ObjectMapper()
            .readTree(created.getResponse().getContentAsString())
            .get("id")
            .asLong();

    mockMvc
        .perform(
            get("/contacts/" + id).header("Authorization", TestHelper.authHeader(token())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id", is(id.intValue())))
        .andExpect(jsonPath("$.name", is("John Doe")));
  }

  @Test
  void getContactNotFound() throws Exception {
    mockMvc
        .perform(get("/contacts/999").header("Authorization", TestHelper.authHeader(token())))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.detail", is("Contact not found")));
  }

  @Test
  void updateContactPartial() throws Exception {
    MvcResult created = createContact(VALID_CONTACT);
    Long id =
        new com.fasterxml.jackson.databind.ObjectMapper()
            .readTree(created.getResponse().getContentAsString())
            .get("id")
            .asLong();

    mockMvc
        .perform(
            put("/contacts/" + id)
                .header("Authorization", TestHelper.authHeader(token()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Jane Doe\",\"address\":\"456 Oak Ave\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name", is("Jane Doe")))
        .andExpect(jsonPath("$.address", is("456 Oak Ave")))
        // Untouched fields keep their values.
        .andExpect(jsonPath("$.phone_number", is("+1234567890")))
        .andExpect(jsonPath("$.email", is("john@example.com")));
  }

  @Test
  void updateContactNotFound() throws Exception {
    mockMvc
        .perform(
            put("/contacts/999")
                .header("Authorization", TestHelper.authHeader(token()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"X\"}"))
        .andExpect(status().isNotFound());
  }

  @Test
  void updateContactDuplicatePhone() throws Exception {
    MvcResult first = createContact(VALID_CONTACT);
    Long firstId =
        new com.fasterxml.jackson.databind.ObjectMapper()
            .readTree(first.getResponse().getContentAsString())
            .get("id")
            .asLong();
    Map<String, Object> second =
        Map.of(
            "name", "Second User",
            "phone_number", "+1987654321",
            "email", "second@example.com");
    MvcResult secondResult = createContact(second);
    Long secondId =
        new com.fasterxml.jackson.databind.ObjectMapper()
            .readTree(secondResult.getResponse().getContentAsString())
            .get("id")
            .asLong();

    mockMvc
        .perform(
            put("/contacts/" + secondId)
                .header("Authorization", TestHelper.authHeader(token()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"phone_number\":\""
                        + new com.fasterxml.jackson.databind.ObjectMapper()
                            .readTree(first.getResponse().getContentAsString())
                            .get("phone_number")
                            .asText()
                        + "\"}"))
        .andExpect(status().isConflict());

    assertThat(firstId).isNotEqualTo(secondId);
  }

  @Test
  void updateContactClearsOptionalFields() throws Exception {
    MvcResult created = createContact(VALID_CONTACT);
    Long id =
        new com.fasterxml.jackson.databind.ObjectMapper()
            .readTree(created.getResponse().getContentAsString())
            .get("id")
            .asLong();

    // Explicit nulls clear the optional fields (frontend behavior).
    mockMvc
        .perform(
            put("/contacts/" + id)
                .header("Authorization", TestHelper.authHeader(token()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"John Doe\",\"phone_number\":\"+1234567890\",\"email\":null,\"address\":null}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email", is(nullValue())))
        .andExpect(jsonPath("$.address", is(nullValue())));
  }

  @Test
  void deleteContact() throws Exception {
    MvcResult created = createContact(VALID_CONTACT);
    Long id =
        new com.fasterxml.jackson.databind.ObjectMapper()
            .readTree(created.getResponse().getContentAsString())
            .get("id")
            .asLong();

    mockMvc
        .perform(delete("/contacts/" + id).header("Authorization", TestHelper.authHeader(token())))
        .andExpect(status().isNoContent());
    mockMvc
        .perform(get("/contacts/" + id).header("Authorization", TestHelper.authHeader(token())))
        .andExpect(status().isNotFound());
    mockMvc
        .perform(delete("/contacts/" + id).header("Authorization", TestHelper.authHeader(token())))
        .andExpect(status().isNotFound());
  }

  @Test
  void healthCheck() throws Exception {
    mockMvc
        .perform(get("/health"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status", is("ok")));
  }
}
