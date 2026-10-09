package com.phonebook.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.util.List;

/** Per-row validation errors reported by the CSV import endpoint. */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class CsvRowError {

  private int row;
  private List<String> errors;

  public CsvRowError() {
  }

  public CsvRowError(int row, List<String> errors) {
    this.row = row;
    this.errors = errors;
  }

  public int getRow() {
    return row;
  }

  public void setRow(int row) {
    this.row = row;
  }

  public List<String> getErrors() {
    return errors;
  }

  public void setErrors(List<String> errors) {
    this.errors = errors;
  }
}
