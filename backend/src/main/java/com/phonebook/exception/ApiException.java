package com.phonebook.exception;

import org.springframework.http.HttpStatus;

/**
 * Application-level exception that maps to a specific HTTP status
 * and a {"detail": "message"} response body.
 */
public class ApiException extends RuntimeException {

  private final HttpStatus status;

  public ApiException(HttpStatus status, String message) {
    super(message);
    this.status = status;
  }

  public ApiException(int status, String message) {
    this(HttpStatus.valueOf(status), message);
  }

  public HttpStatus getStatus() {
    return status;
  }
}
