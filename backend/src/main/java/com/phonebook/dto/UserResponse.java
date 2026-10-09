package com.phonebook.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.phonebook.entity.User;
import java.time.OffsetDateTime;

/** User info returned by the API (never includes the password hash). */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class UserResponse {

  private Long id;
  private String username;
  private String email;
  private OffsetDateTime createdAt;

  public UserResponse() {
  }

  public UserResponse(Long id, String username, String email, OffsetDateTime createdAt) {
    this.id = id;
    this.username = username;
    this.email = email;
    this.createdAt = createdAt;
  }

  public static UserResponse from(User user) {
    return new UserResponse(user.getId(), user.getUsername(), user.getEmail(), user.getCreatedAt());
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(OffsetDateTime createdAt) {
    this.createdAt = createdAt;
  }
}
