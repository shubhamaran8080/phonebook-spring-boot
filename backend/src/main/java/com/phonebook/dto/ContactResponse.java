package com.phonebook.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.phonebook.entity.Contact;
import java.time.OffsetDateTime;

/** Contact as returned by the API. */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ContactResponse {

  private Long id;
  private String name;
  private String phoneNumber;
  private String email;
  private String address;
  private OffsetDateTime createdAt;

  public ContactResponse() {
  }

  public ContactResponse(
      Long id, String name, String phoneNumber, String email, String address, OffsetDateTime createdAt) {
    this.id = id;
    this.name = name;
    this.phoneNumber = phoneNumber;
    this.email = email;
    this.address = address;
    this.createdAt = createdAt;
  }

  public static ContactResponse from(Contact contact) {
    return new ContactResponse(
        contact.getId(),
        contact.getName(),
        contact.getPhoneNumber(),
        contact.getEmail(),
        contact.getAddress(),
        contact.getCreatedAt());
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getPhoneNumber() {
    return phoneNumber;
  }

  public void setPhoneNumber(String phoneNumber) {
    this.phoneNumber = phoneNumber;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getAddress() {
    return address;
  }

  public void setAddress(String address) {
    this.address = address;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(OffsetDateTime createdAt) {
    this.createdAt = createdAt;
  }
}
