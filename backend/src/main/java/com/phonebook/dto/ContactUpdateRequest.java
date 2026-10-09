package com.phonebook.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Payload for updating a contact. Only fields that are explicitly
 * provided are changed; omitted fields keep their current value
 * (matching the Python backend's "exclude_unset" behavior).
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ContactUpdateRequest {

  @Size(min = 1, max = 255)
  private String name;
  private boolean nameSet;

  @Pattern(regexp = PhoneNumbers.PHONE_NUMBER_PATTERN)
  private String phoneNumber;
  private boolean phoneNumberSet;

  @Email
  private String email;
  private boolean emailSet;

  private String address;
  private boolean addressSet;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name == null ? null : name.strip();
    this.nameSet = true;
  }

  public boolean isNameSet() {
    return nameSet;
  }

  public String getPhoneNumber() {
    return phoneNumber;
  }

  public void setPhoneNumber(String phoneNumber) {
    this.phoneNumber = phoneNumber == null ? null : phoneNumber.strip();
    this.phoneNumberSet = true;
  }

  public boolean isPhoneNumberSet() {
    return phoneNumberSet;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email == null ? null : email.strip();
    this.emailSet = true;
  }

  public boolean isEmailSet() {
    return emailSet;
  }

  public String getAddress() {
    return address;
  }

  public void setAddress(String address) {
    this.address = address == null ? null : address.strip();
    this.addressSet = true;
  }

  public boolean isAddressSet() {
    return addressSet;
  }
}
