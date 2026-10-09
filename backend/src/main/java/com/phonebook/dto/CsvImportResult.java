package com.phonebook.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.util.List;

/** Summary returned by the CSV import endpoint. */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class CsvImportResult {

  private int total;
  private int imported;
  private int failed;
  private int duplicates;
  private List<CsvRowError> errors;

  public CsvImportResult() {
  }

  public CsvImportResult(int total, int imported, int failed, int duplicates, List<CsvRowError> errors) {
    this.total = total;
    this.imported = imported;
    this.failed = failed;
    this.duplicates = duplicates;
    this.errors = errors;
  }

  public int getTotal() {
    return total;
  }

  public void setTotal(int total) {
    this.total = total;
  }

  public int getImported() {
    return imported;
  }

  public void setImported(int imported) {
    this.imported = imported;
  }

  public int getFailed() {
    return failed;
  }

  public void setFailed(int failed) {
    this.failed = failed;
  }

  public int getDuplicates() {
    return duplicates;
  }

  public void setDuplicates(int duplicates) {
    this.duplicates = duplicates;
  }

  public List<CsvRowError> getErrors() {
    return errors;
  }

  public void setErrors(List<CsvRowError> errors) {
    this.errors = errors;
  }
}
