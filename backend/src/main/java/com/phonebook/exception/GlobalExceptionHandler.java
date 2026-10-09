package com.phonebook.exception;

import com.phonebook.dto.ErrorResponse;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.NoHandlerFoundException;

import jakarta.validation.ConstraintViolationException;

/**
 * Translates every error into the Python backend's response shape:
 * a JSON body {"detail": "message"} with the matching status code
 * (401/404/409/413/422/...).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(ApiException.class)
  public ResponseEntity<ErrorResponse> handleApiException(ApiException e) {
    return ResponseEntity.status(e.getStatus()).body(new ErrorResponse(e.getMessage()));
  }

  /** Bean validation errors on request bodies -> 422 (FastAPI compatibility). */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
    String detail =
        e.getBindingResult().getFieldErrors().stream()
            .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
            .collect(Collectors.joining("; "));
    return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(new ErrorResponse(detail));
  }

  /** Bean validation errors on request parameters -> 422. */
  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException e) {
    String detail =
        e.getConstraintViolations().stream()
            .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
            .collect(Collectors.joining("; "));
    return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(new ErrorResponse(detail));
  }

  /** Malformed request body -> 422 (FastAPI compatibility). */
  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ErrorResponse> handleUnreadableBody(HttpMessageNotReadableException e) {
    return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
        .body(new ErrorResponse("Invalid request body"));
  }

  /** Wrong type for a path variable or parameter -> 422 (FastAPI compatibility). */
  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
    return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
        .body(new ErrorResponse("Invalid value for parameter '" + e.getName() + "'"));
  }

  /** Missing multipart file part -> 422 (FastAPI compatibility). */
  @ExceptionHandler(MissingServletRequestPartException.class)
  public ResponseEntity<ErrorResponse> handleMissingPart(MissingServletRequestPartException e) {
    return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
        .body(new ErrorResponse("Missing required part: " + e.getRequestPartName()));
  }

  /** Oversized uploads -> 413. */
  @ExceptionHandler(MaxUploadSizeExceededException.class)
  public ResponseEntity<ErrorResponse> handleMaxUploadSize(MaxUploadSizeExceededException e) {
    return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
        .body(new ErrorResponse("The CSV file is too large (maximum is 5 MB)."));
  }

  /** Unknown routes -> 404 {"detail":"Not Found"} (FastAPI compatibility). */
  @ExceptionHandler(NoHandlerFoundException.class)
  public ResponseEntity<ErrorResponse> handleNotFound(NoHandlerFoundException e) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse("Not Found"));
  }

  /** Wrong HTTP method -> 405 {"detail":"Method Not Allowed"}. */
  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
    return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
        .body(new ErrorResponse("Method Not Allowed"));
  }

  /** Unexpected errors -> 500 with a generic message (never leaks internals). */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleGeneric(Exception e) {
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(new ErrorResponse("Internal server error"));
  }
}
