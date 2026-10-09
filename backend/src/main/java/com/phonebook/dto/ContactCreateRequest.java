package com.phonebook.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Payload for creating a contact. */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ContactCreateRequest {

  @NotBlank
  @Size(max = 255)
  private String name;

  @NotBlank
  @Pattern(regexp = PhoneNumbers.PHONE_NUMBER_PATTERN)
  private String phoneNumber;

  @Email
  private String email;

  private String address;

  public ContactCreateRequest() {
  }

  public ContactCreateRequest(String name, String phoneNumber, String email, String address) {
    this.name = name;
    this.phoneNumber = phoneNumber;
    this.email = email;
    this.address = address;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name == null ? null : name.strip();
  }

  public String getPhoneNumber() {
    return phoneNumber;
  }

  public void setPhoneNumber(String phoneNumber) {
    this.phoneNumber = phoneNumber == null ? null : phoneNumber.strip();
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email == null ? null : email.strip();
  }

  public String getAddress() {
    return address;
  }

  public void setAddress(String address) {
    this.address = address == null ? null : address.strip();
  }
}
