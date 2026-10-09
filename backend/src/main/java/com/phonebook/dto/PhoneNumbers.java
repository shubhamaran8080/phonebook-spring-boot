package com.phonebook.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** E.164 style: a leading '+', a country code starting 1-9, then up to 14 digits. */
public final class PhoneNumbers {
  public static final String PHONE_NUMBER_PATTERN = "^\\+[1-9]\\d{1,14}$";

  private PhoneNumbers() {
  }
}
