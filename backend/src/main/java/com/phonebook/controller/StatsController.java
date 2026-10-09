package com.phonebook.controller;

import com.phonebook.dto.StatsResponse;
import com.phonebook.service.StatsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StatsController {

  private final StatsService statsService;

  public StatsController(StatsService statsService) {
    this.statsService = statsService;
  }

  /** Phonebook statistics for the dashboard. */
  @GetMapping("/stats")
  public StatsResponse getStats() {
    return statsService.getStats();
  }
}
