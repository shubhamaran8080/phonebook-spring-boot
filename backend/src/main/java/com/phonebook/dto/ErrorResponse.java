package com.phonebook.dto;

/**
 * Error body used for every API error, matching the Python
 * backend's response shape: {"detail": "message"}.
 */
public class ErrorResponse {

  private String detail;

  public ErrorResponse() {
  }

  public ErrorResponse(String detail) {
    this.detail = detail;
  }

  public String getDetail() {
    return detail;
  }

  public void setDetail(String detail) {
    this.detail = detail;
  }
}
