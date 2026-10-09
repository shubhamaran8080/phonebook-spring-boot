package com.phonebook.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.NotBlank;

/** Login payload. "username" accepts a username or an email address. */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class LoginRequest {

  @NotBlank
  private String username;

  @NotBlank
  private String password;

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }
}
