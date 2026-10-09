package com.phonebook.service;

import com.phonebook.dto.ContactResponse;
import com.phonebook.dto.StatsResponse;
import com.phonebook.repository.ContactRepository;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Dashboard statistics, matching the Python backend's /stats endpoint. */
@Service
public class StatsService {

  private static final int RECENT_LIST_SIZE = 5;
  private static final int RECENT_DAYS = 7;

  private final ContactRepository contactRepository;

  public StatsService(ContactRepository contactRepository) {
    this.contactRepository = contactRepository;
  }

  @Transactional(readOnly = true)
  public StatsResponse getStats() {
    OffsetDateTime since = OffsetDateTime.now().minus(RECENT_DAYS, ChronoUnit.DAYS);
    return new StatsResponse(
        contactRepository.count(),
        contactRepository.countCreatedSince(since),
        contactRepository.countByEmailIsNotNull(),
        contactRepository.countByPhoneNumberIsNotNull(),
        contactRepository.findRecent(PageRequest.of(0, RECENT_LIST_SIZE)).stream()
            .map(ContactResponse::from)
            .toList());
  }
}
