package com.phonebook.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/** Response of the login endpoint. */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class LoginResponse {

  private String accessToken;
  private String tokenType;
  private UserResponse user;

  public LoginResponse() {
  }

  public LoginResponse(String accessToken, String tokenType, UserResponse user) {
    this.accessToken = accessToken;
    this.tokenType = tokenType;
    this.user = user;
  }

  public String getAccessToken() {
    return accessToken;
  }

  public void setAccessToken(String accessToken) {
    this.accessToken = accessToken;
  }

  public String getTokenType() {
    return tokenType;
  }

  public void setTokenType(String tokenType) {
    this.tokenType = tokenType;
  }

  public UserResponse getUser() {
    return user;
  }

  public void setUser(UserResponse user) {
    this.user = user;
  }
}
