package com.phonebook.service;

import com.phonebook.dto.ContactCreateRequest;
import com.phonebook.dto.ContactResponse;
import com.phonebook.dto.ContactUpdateRequest;
import com.phonebook.entity.Contact;
import com.phonebook.exception.ApiException;
import com.phonebook.repository.ContactRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ContactService {

  private final ContactRepository contactRepository;

  public ContactService(ContactRepository contactRepository) {
    this.contactRepository = contactRepository;
  }

  @Transactional(readOnly = true)
  public Contact getContact(Long id) {
    return contactRepository
        .findById(id)
        .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Contact not found"));
  }

  @Transactional(readOnly = true)
  public Page<Contact> search(String q, PageRequest pageRequest) {
    return contactRepository.search(q, pageRequest);
  }

  @Transactional(readOnly = true)
  public Page<Contact> searchAll(PageRequest pageRequest) {
    return contactRepository.findAll(pageRequest);
  }

  @Transactional
  public ContactResponse create(ContactCreateRequest request) {
    Contact contact = new Contact();
    contact.setName(request.getName());
    contact.setPhoneNumber(request.getPhoneNumber());
    contact.setEmail(request.getEmail());
    contact.setAddress(request.getAddress());
    try {
      contactRepository.saveAndFlush(contact);
    } catch (DataIntegrityViolationException e) {
      throw new ApiException(
          HttpStatus.CONFLICT, "A contact with this phone number or email already exists.");
    }
    return ContactResponse.from(contact);
  }

  /** Partial update: only fields explicitly provided by the caller are changed. */
  @Transactional
  public ContactResponse update(Long id, ContactUpdateRequest request) {
    Contact contact = getContact(id);
    if (request.isNameSet()) {
      contact.setName(request.getName());
    }
    if (request.isPhoneNumberSet()) {
      contact.setPhoneNumber(request.getPhoneNumber());
    }
    if (request.isEmailSet()) {
      contact.setEmail(request.getEmail());
    }
    if (request.isAddressSet()) {
      contact.setAddress(request.getAddress());
    }
    try {
      contactRepository.saveAndFlush(contact);
    } catch (DataIntegrityViolationException e) {
      throw new ApiException(
          HttpStatus.CONFLICT, "A contact with this phone number or email already exists.");
    }
    return ContactResponse.from(contact);
  }

  @Transactional
  public void delete(Long id) {
    Contact contact = getContact(id);
    contactRepository.delete(contact);
  }
}
