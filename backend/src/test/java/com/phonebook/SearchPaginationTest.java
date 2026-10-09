package com.phonebook;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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

/** Search and numbered pagination tests (mirrors test_contacts.py). */
@SpringBootTest
@AutoConfigureMockMvc
class SearchPaginationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ContactRepository contactRepository;
  @Autowired private UserRepository userRepository;
  @Autowired private PasswordEncoder passwordEncoder;

  @BeforeEach
  void setUp() throws Exception {
    contactRepository.deleteAll();
    userRepository.deleteAll();
    User user = new User();
    user.setUsername("alice");
    user.setEmail("alice@example.com");
    user.setHashedPassword(passwordEncoder.encode("secret123"));
    userRepository.save(user);
    for (int i = 0; i < 5; i++) {
      Map<String, Object> payload = new HashMap<>();
      payload.put("name", "User " + i);
      payload.put("phone_number", String.format("+1555%07d", i));
      payload.put("email", "user" + i + "@example.com");
      mockMvc
          .perform(
              post("/contacts")
                  .header("Authorization", TestHelper.authHeader(TestHelper.login(mockMvc, "alice", "secret123")))
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      new com.fasterxml.jackson.databind.ObjectMapper()
                          .writeValueAsString(payload)))
          .andExpect(status().isCreated());
    }
  }

  @Test
  void paginationFirstPage() throws Exception {
    String token = TestHelper.login(mockMvc, "alice", "secret123");

    mockMvc
        .perform(
            get("/contacts")
                .header("Authorization", TestHelper.authHeader(token))
                .param("page", "1")
                .param("page_size", "2"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()", is(2)))
        .andExpect(jsonPath("$[0].name", is("User 0")))
        .andExpect(jsonPath("$[1].name", is("User 1")))
        .andExpect(header().string("X-Total-Count", "5"))
        .andExpect(header().string("X-Page", "1"))
        .andExpect(header().string("X-Page-Size", "2"));
  }

  @Test
  void paginationSecondPage() throws Exception {
    String token = TestHelper.login(mockMvc, "alice", "secret123");

    mockMvc
        .perform(
            get("/contacts")
                .header("Authorization", TestHelper.authHeader(token))
                .param("page", "2")
                .param("page_size", "2"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()", is(2)))
        .andExpect(jsonPath("$[0].name", is("User 2")))
        .andExpect(jsonPath("$[1].name", is("User 3")));
  }

  @Test
  void searchByName() throws Exception {
    String token = TestHelper.login(mockMvc, "alice", "secret123");

    mockMvc
        .perform(
            get("/contacts")
                .header("Authorization", TestHelper.authHeader(token))
                .param("q", "User 1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()", is(1)))
        .andExpect(jsonPath("$[0].name", is("User 1")))
        .andExpect(header().string("X-Total-Count", "1"));
  }

  @Test
  void searchByPhoneNumber() throws Exception {
    String token = TestHelper.login(mockMvc, "alice", "secret123");

    mockMvc
        .perform(
            get("/contacts")
                .header("Authorization", TestHelper.authHeader(token))
                .param("q", "15550000001"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()", is(1)))
        .andExpect(jsonPath("$[0].phone_number", is("+15550000001")));
  }

  @Test
  void searchIsCaseInsensitive() throws Exception {
    String token = TestHelper.login(mockMvc, "alice", "secret123");

    mockMvc
        .perform(
            get("/contacts")
                .header("Authorization", TestHelper.authHeader(token))
                .param("q", "uSeR 3"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()", is(1)))
        .andExpect(jsonPath("$[0].name", is("User 3")));
  }

  @Test
  void searchCombinedWithPagination() throws Exception {
    String token = TestHelper.login(mockMvc, "alice", "secret123");

    mockMvc
        .perform(
            get("/contacts")
                .header("Authorization", TestHelper.authHeader(token))
                .param("q", "User")
                .param("page", "1")
                .param("page_size", "3"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()", is(3)))
        .andExpect(header().string("X-Total-Count", "5"));
  }

  @Test
  void invalidPageSizeIsRejected() throws Exception {
    String token = TestHelper.login(mockMvc, "alice", "secret123");

    mockMvc
        .perform(
            get("/contacts")
                .header("Authorization", TestHelper.authHeader(token))
                .param("page_size", "101"))
        .andExpect(status().isUnprocessableEntity());
    mockMvc
        .perform(
            get("/contacts")
                .header("Authorization", TestHelper.authHeader(token))
                .param("page", "0"))
        .andExpect(status().isUnprocessableEntity());
  }
}
