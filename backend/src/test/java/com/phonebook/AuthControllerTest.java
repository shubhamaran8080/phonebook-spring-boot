package com.phonebook;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.phonebook.entity.User;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.mock.web.MockMultipartFile;

/** Authentication, current-user and authorization tests (mirrors test_auth.py). */
@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private UserRepository userRepository;
  @Autowired private PasswordEncoder passwordEncoder;
  @Autowired private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    userRepository.deleteAll();
  }

  private User createUser(String username, String password, String email) {
    User user = new User();
    user.setUsername(username);
    user.setEmail(email);
    user.setHashedPassword(passwordEncoder.encode(password));
    return userRepository.save(user);
  }

  @Test
  void loginSuccessWithUsername() throws Exception {
    createUser("alice", "secret123", "alice@example.com");

    MvcResult result =
        mockMvc
            .perform(
                post("/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            Map.of("username", "alice", "password", "secret123"))))
            .andExpect(status().isOk())
            .andReturn();

    JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
    assertThat(body.get("token_type").asText()).isEqualTo("bearer");
    assertThat(body.get("access_token").asText()).isNotBlank();
    assertThat(body.get("user").get("username").asText()).isEqualTo("alice");
    assertThat(body.get("user").has("hashed_password")).isFalse();
  }

  @Test
  void loginSuccessWithEmail() throws Exception {
    createUser("alice", "secret123", "alice@example.com");

    mockMvc
        .perform(
            post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        Map.of("username", "alice@example.com", "password", "secret123"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.user.username", is("alice")));
  }

  @Test
  void loginWrongPassword() throws Exception {
    createUser("alice", "secret123", "alice@example.com");

    mockMvc
        .perform(
            post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        Map.of("username", "alice", "password", "wrong"))))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.detail", is("Incorrect username/email or password")));
  }

  @Test
  void loginUnknownUser() throws Exception {
    mockMvc
        .perform(
            post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("username", "nobody", "password", "x"))))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void loginValidation() throws Exception {
    mockMvc
        .perform(
            post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("username", "", "password", ""))))
        .andExpect(status().isUnprocessableEntity());
  }

  @Test
  void meRequiresToken() throws Exception {
    mockMvc
        .perform(get("/auth/me"))
        .andExpect(status().isUnauthorized())
        .andExpect(header().string("WWW-Authenticate", "Bearer"))
        .andExpect(jsonPath("$.detail", is("Not authenticated")));
  }

  @Test
  void meWithToken() throws Exception {
    createUser("alice", "secret123", "alice@example.com");
    String token = TestHelper.login(mockMvc, "alice", "secret123");

    mockMvc
        .perform(get("/auth/me").header("Authorization", TestHelper.authHeader(token)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.username", is("alice")))
        .andExpect(jsonPath("$.email", is("alice@example.com")));
  }

  @Test
  void meWithInvalidToken() throws Exception {
    mockMvc
        .perform(get("/auth/me").header("Authorization", "Bearer not-a-jwt"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void meWithTamperedToken() throws Exception {
    createUser("alice", "secret123", "alice@example.com");
    String token = TestHelper.login(mockMvc, "alice", "secret123");
    String tampered =
        token.substring(0, token.length() - 2) + (token.endsWith("a") ? "b" : "a");

    mockMvc
        .perform(get("/auth/me").header("Authorization", TestHelper.authHeader(tampered)))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void logoutEndpoint() throws Exception {
    mockMvc
        .perform(post("/auth/logout"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message", is("Logged out")));
  }

  @Test
  void protectedContactAndStatsEndpointsRequireAuthentication() throws Exception {
    mockMvc.perform(get("/contacts")).andExpect(status().isUnauthorized());
    mockMvc
        .perform(
            post("/contacts").contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isUnauthorized());
    mockMvc.perform(get("/contacts/1")).andExpect(status().isUnauthorized());
    mockMvc
        .perform(put("/contacts/1").contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isUnauthorized());
    mockMvc.perform(delete("/contacts/1")).andExpect(status().isUnauthorized());
    mockMvc.perform(get("/stats")).andExpect(status().isUnauthorized());
    mockMvc.perform(get("/contacts/export")).andExpect(status().isUnauthorized());
    mockMvc
        .perform(
            multipart("/contacts/import")
                .file(new MockMultipartFile("file", "c.csv", "text/csv", "a,b".getBytes())))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void unknownRouteReturnsFastApiStyleNotFound() throws Exception {
    mockMvc.perform(get("/no-such-route")).andExpect(status().isNotFound());
  }

  @SuppressWarnings("unused")
  private MockHttpServletRequestBuilder unused() {
    return get("/");
  }
}
