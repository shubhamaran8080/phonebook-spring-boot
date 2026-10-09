package com.phonebook.controller;

import com.phonebook.dto.LoginRequest;
import com.phonebook.dto.LoginResponse;
import com.phonebook.dto.UserResponse;
import com.phonebook.entity.User;
import com.phonebook.service.AuthService;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {

  private final AuthService authService;

  public AuthController(AuthService authService) {
    this.authService = authService;
  }

  /** Authenticate with a username or email and return a JWT access token. */
  @PostMapping("/auth/login")
  public LoginResponse login(@Valid @RequestBody LoginRequest request) {
    return authService.login(request);
  }

  /** Return the currently authenticated user. */
  @GetMapping("/auth/me")
  public UserResponse getMe(Authentication authentication) {
    String username = ((UserDetails) authentication.getPrincipal()).getUsername();
    User user = authService.findByUsername(username);
    return UserResponse.from(user);
  }

  /**
   * Logout endpoint. JWTs are stateless, so the client discards its
   * token - the endpoint exists for frontend compatibility.
   */
  @PostMapping("/auth/logout")
  public Map<String, String> logout() {
    return Map.of("message", "Logged out");
  }
}
