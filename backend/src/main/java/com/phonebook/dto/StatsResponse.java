package com.phonebook.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.util.List;

/** Phonebook statistics for the dashboard. */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class StatsResponse {

  private Long totalContacts;
  private Long recentContacts;
  private Long withEmail;
  private Long withPhone;
  private List<ContactResponse> recentList;

  public StatsResponse() {
  }

  public StatsResponse(
      long totalContacts,
      long recentContacts,
      long withEmail,
      long withPhone,
      List<ContactResponse> recentList) {
    this.totalContacts = totalContacts;
    this.recentContacts = recentContacts;
    this.withEmail = withEmail;
    this.withPhone = withPhone;
    this.recentList = recentList;
  }

  public Long getTotalContacts() {
    return totalContacts;
  }

  public void setTotalContacts(Long totalContacts) {
    this.totalContacts = totalContacts;
  }

  public Long getRecentContacts() {
    return recentContacts;
  }

  public void setRecentContacts(Long recentContacts) {
    this.recentContacts = recentContacts;
  }

  public Long getWithEmail() {
    return withEmail;
  }

  public void setWithEmail(Long withEmail) {
    this.withEmail = withEmail;
  }

  public Long getWithPhone() {
    return withPhone;
  }

  public void setWithPhone(Long withPhone) {
    this.withPhone = withPhone;
  }

  public List<ContactResponse> getRecentList() {
    return recentList;
  }

  public void setRecentList(List<ContactResponse> recentList) {
    this.recentList = recentList;
  }
}
