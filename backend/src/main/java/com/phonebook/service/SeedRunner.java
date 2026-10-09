package com.phonebook.service;

import com.phonebook.entity.Contact;
import com.phonebook.entity.User;
import com.phonebook.repository.ContactRepository;
import com.phonebook.repository.UserRepository;
import java.io.Console;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.datafaker.Faker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.boot.CommandLineRunner;

/**
 * Java equivalent of the Python seed.py script.
 *
 * <p>Runs only when explicitly requested (off by default):
 * <pre>
 * java -jar app.jar \
 *   --seed.create-user=true --seed.username=demo \
 *   --seed.password=choose-a-password \
 *   --seed.count=1000
 * </pre>
 * or via environment variables SEED_USERNAME / SEED_PASSWORD / SEED_COUNT.
 *
 * <p>Never resets, truncates or modifies existing data; usernames and
 * generated phone numbers/emails are checked for uniqueness first.
 */
@Component
public class SeedRunner implements CommandLineRunner {

  private static final Logger log = LoggerFactory.getLogger(SeedRunner.class);
  private static final int BATCH_SIZE = 500;
  private static final int MAX_COUNT = 50000;

  private final UserRepository userRepository;
  private final ContactRepository contactRepository;
  private final PasswordEncoder passwordEncoder;

  @Value("${seed.create-user:false}")
  private boolean createUserFlag;

  @Value("${seed.username:}")
  private String username;

  @Value("${seed.password:${SEED_PASSWORD:}}")
  private String password;

  @Value("${seed.email:}")
  private String email;

  @Value("${seed.count:0}")
  private int count;

  public SeedRunner(
      UserRepository userRepository,
      ContactRepository contactRepository,
      PasswordEncoder passwordEncoder) {
    this.userRepository = userRepository;
    this.contactRepository = contactRepository;
    this.passwordEncoder = passwordEncoder;
  }

  @Override
  public void run(String... args) {
    if (!createUserFlag && count <= 0) {
      return;
    }
    log.info("Seeding phonebook data...");
    if (createUserFlag) {
      createUser();
    }
    if (count > 0) {
      generateContacts(count);
    }
    log.info("Seeding finished.");
  }

  private void createUser() {
    if (username == null || username.isBlank()) {
      log.error("--seed.username is required when --seed.create-user is set.");
      return;
    }
    if (userRepository.existsByUsername(username.strip())) {
      log.info("User '{}' already exists - nothing to do.", username);
      return;
    }
    String pwd = resolvePassword();
    if (pwd == null || pwd.isBlank()) {
      log.error(
          "No password provided for the new user. Set --seed.password or the SEED_PASSWORD environment variable.");
      return;
    }
    User user = new User();
    user.setUsername(username.strip());
    user.setEmail(email == null || email.isBlank() ? null : email.strip());
    user.setHashedPassword(passwordEncoder.encode(pwd));
    userRepository.save(user);
    log.info("Created user '{}'.", username);
  }

  private String resolvePassword() {
    if (password != null && !password.isBlank()) {
      return password;
    }
    Console console = System.console();
    if (console != null) {
      char[] chars = console.readPassword("Password for new user: ");
      return chars == null ? null : new String(chars);
    }
    return null;
  }

  private void generateContacts(int count) {
    if (count < 1 || count > MAX_COUNT) {
      log.error("--seed.count must be between 1 and {}. Got: {}", MAX_COUNT, count);
      return;
    }
    Faker faker = new Faker();
    Set<String> usedPhones = new HashSet<>(contactRepository.findAllPhoneNumbers());
    Set<String> usedEmails = new HashSet<>();
    for (String email : contactRepository.findAllEmails()) {
      if (email != null) {
        usedEmails.add(email);
      }
    }

    List<Contact> batch = new ArrayList<>();
    int created = 0;
    int attempts = 0;
    while (created < count && attempts < count * 20) {
      attempts++;

      String phone = "+1" + faker.number().digits(10);
      if (usedPhones.contains(phone)) {
        continue;
      }
      String generatedEmail = faker.internet().emailAddress().toLowerCase();
      while (usedEmails.contains(generatedEmail)) {
        generatedEmail =
            faker.name().username().toLowerCase()
                + "."
                + faker.number().numberBetween(1, 999999)
                + "@example.com";
      }

      usedPhones.add(phone);
      usedEmails.add(generatedEmail);

      Contact contact = new Contact();
      contact.setName(faker.name().fullName());
      contact.setPhoneNumber(phone);
      contact.setEmail(generatedEmail);
      contact.setAddress(faker.address().fullAddress().replace("\n", ", "));
      batch.add(contact);
      created++;

      if (batch.size() >= BATCH_SIZE) {
        contactRepository.saveAll(batch);
        contactRepository.flush();
        batch.clear();
        log.info("  ... {}/{} contacts created", created, count);
      }
    }
    if (!batch.isEmpty()) {
      contactRepository.saveAll(batch);
      contactRepository.flush();
    }
    log.info("Created {} contacts (total in database may be higher).", created);
  }
}
