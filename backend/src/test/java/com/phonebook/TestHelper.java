package com.phonebook;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Shared helpers for the API test suite. */
final class TestHelper {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private TestHelper() {
  }

  /** Performs a real login request and returns the JWT access token. */
  static String login(MockMvc mockMvc, String username, String password) throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        MAPPER.writeValueAsString(Map.of("username", username, "password", password))))
            .andExpect(status().isOk())
            .andReturn();
    JsonNode body = MAPPER.readTree(result.getResponse().getContentAsString());
    return body.get("access_token").asText();
  }

  static String authHeader(String token) {
    return "Bearer " + token;
  }
}
