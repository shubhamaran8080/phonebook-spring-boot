package com.phonebook.repository;

import com.phonebook.entity.Contact;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ContactRepository extends JpaRepository<Contact, Long> {

  /** Case-insensitive search by name or phone number, ordered by id (pagination metadata for the list endpoint). */
  @Query(
      value =
          "SELECT c FROM Contact c WHERE LOWER(c.name) LIKE LOWER(CONCAT('%', :q, '%'))"
              + " OR LOWER(c.phoneNumber) LIKE LOWER(CONCAT('%', :q, '%')) ORDER BY c.id",
      countQuery =
          "SELECT COUNT(c) FROM Contact c WHERE LOWER(c.name) LIKE LOWER(CONCAT('%', :q, '%'))"
              + " OR LOWER(c.phoneNumber) LIKE LOWER(CONCAT('%', :q, '%'))")
  Page<Contact> search(@Param("q") String q, Pageable pageable);

  /** Same filter as {@link #search}, without pagination (used by the CSV export). */
  @Query(
      "SELECT c FROM Contact c WHERE LOWER(c.name) LIKE LOWER(CONCAT('%', :q, '%'))"
          + " OR LOWER(c.phoneNumber) LIKE LOWER(CONCAT('%', :q, '%')) ORDER BY c.id")
  List<Contact> searchAll(@Param("q") String q);

  @Query("SELECT c.phoneNumber FROM Contact c")
  List<String> findAllPhoneNumbers();

  @Query("SELECT c.email FROM Contact c")
  List<String> findAllEmails();

  @Query("SELECT COUNT(c) FROM Contact c WHERE c.createdAt >= :since")
  long countCreatedSince(@Param("since") java.time.OffsetDateTime since);

  long countByEmailIsNotNull();

  long countByPhoneNumberIsNotNull();

  @Query("SELECT c FROM Contact c ORDER BY c.createdAt DESC, c.id DESC")
  List<Contact> findRecent(Pageable pageable);
}
