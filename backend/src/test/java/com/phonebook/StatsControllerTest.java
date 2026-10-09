package com.phonebook;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.phonebook.entity.User;
import com.phonebook.repository.ContactRepository;
import com.phonebook.repository.UserRepository;
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

/** Dashboard statistics tests (mirrors test_auth.py's stats tests). */
@SpringBootTest
@AutoConfigureMockMvc
class StatsControllerTest {

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
                    .header("Authorization", TestHelper.authHeader(token()))
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

  @Test
  void statsWithOneContact() throws Exception {
    String token = token();
    createContact("Alice Smith", "+1234567890", "alice@example.com", null);

    mockMvc
        .perform(get("/stats").header("Authorization", TestHelper.authHeader(token)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total_contacts", is(1)))
        .andExpect(jsonPath("$.with_email", is(1)))
        .andExpect(jsonPath("$.with_phone", is(1)))
        .andExpect(jsonPath("$.recent_contacts", greaterThanOrEqualTo(1)))
        .andExpect(jsonPath("$.recent_list", hasSize(1)))
        .andExpect(jsonPath("$.recent_list[0].name", is("Alice Smith")));
  }

  @Test
  void statsRecentListIsLimitedToFiveNewestFirst() throws Exception {
    String token = token();
    Long lastId = null;
    for (int i = 0; i < 6; i++) {
      lastId =
          createContact(
              "User " + i,
              String.format("+1555%07d", i),
              "user" + i + "@example.com",
              "City " + i);
    }

    mockMvc
        .perform(get("/stats").header("Authorization", TestHelper.authHeader(token)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total_contacts", is(6)))
        .andExpect(jsonPath("$.recent_list", hasSize(5)))
        // Most recently created contact comes first.
        .andExpect(jsonPath("$.recent_list[0].id", is(lastId.intValue())));
  }

  @Test
  void statsCountsContactsWithoutEmail() throws Exception {
    String token = token();
    createContact("No Email", "+15550000001", null, null);
    createContact("With Email", "+15550000002", "with@example.com", null);

    mockMvc
        .perform(get("/stats").header("Authorization", TestHelper.authHeader(token)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total_contacts", is(2)))
        .andExpect(jsonPath("$.with_email", is(1)))
        .andExpect(jsonPath("$.with_phone", is(2)));
  }
}
