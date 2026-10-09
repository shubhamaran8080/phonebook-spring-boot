# Phonebook Spring Boot — Knowledge Bytes

> Notes explaining the Phonebook project step by step

---

### Byte 1: Overall Project Architecture

**Builds on:** None — starting point

**In plain terms:**

This Phonebook project is a full-stack web application. The user manages contacts through the frontend, the frontend sends requests to the Spring Boot backend, the backend communicates with the PostgreSQL database, and the result is returned to the frontend. A login system protects the contact data: the frontend signs in once, receives a JWT, and sends it with every request.

**The architecture:**

```text
User
  ↓
Vue.js Frontend (JWT stored in localStorage)
  ↓
Spring Boot Backend (JWT verified on protected routes)
  ↓
PostgreSQL Database
  ↓
Spring Boot Response
  ↓
Vue.js Frontend
```

**The stack:**

- **Backend:** Java 17 + Spring Boot 3.3.5 + Spring Security + Spring Data JPA + PostgreSQL
- **Frontend:** Vue 3 + Vite + Vue Router + Axios
- **Database:** PostgreSQL 16 (managed by Flyway migrations)
- **Deployment:** Docker Compose (backend, database, frontend services)

---

### Byte 2: Project Structure

**Builds on:** Byte 1

**In plain terms:**

The project is divided into separate frontend, backend, database, testing, and Docker-related files. Each folder and file has a specific responsibility, which keeps the application organized and easier to maintain.

**The project structure:**

```text
PhonebookSpringBoot/
│
├── backend/
│   ├── src/main/java/com/phonebook/
│   │   ├── PhonebookApplication.java    # Spring Boot entry point
│   │   ├── config/
│   │   │   └── SecurityConfig.java      # Spring Security + CORS + PasswordEncoder
│   │   ├── controller/
│   │   │   ├── HealthController.java     # GET /health
│   │   │   ├── AuthController.java      # /auth/login, /auth/me, /auth/logout
│   │   │   ├── ContactController.java   # /contacts CRUD + import/export
│   │   │   └── StatsController.java     # GET /stats
│   │   ├── dto/
│   │   │   ├── ContactCreateRequest.java
│   │   │   ├── ContactUpdateRequest.java
│   │   │   ├── ContactResponse.java
│   │   │   ├── LoginRequest.java
│   │   │   ├── LoginResponse.java
│   │   │   ├── UserResponse.java
│   │   │   ├── StatsResponse.java
│   │   │   ├── CsvImportResult.java
│   │   │   ├── CsvRowError.java
│   │   │   ├── ErrorResponse.java
│   │   │   └── PhoneNumbers.java       # E.164 regex constant
│   │   ├── entity/
│   │   │   ├── Contact.java             # JPA entity → contacts table
│   │   │   └── User.java                # JPA entity → users table
│   │   ├── exception/
│   │   │   ├── ApiException.java        # custom exception with HTTP status
│   │   │   └── GlobalExceptionHandler.java
│   │   ├── repository/
│   │   │   ├── ContactRepository.java   # Spring Data JPA + custom queries
│   │   │   └── UserRepository.java
│   │   ├── security/
│   │   │   ├── JwtAuthenticationFilter.java
│   │   │   ├── JwtAuthenticationEntryPoint.java
│   │   │   ├── JwtTokenProvider.java
│   │   │   └── SecurityUserService.java
│   │   └── service/
│   │       ├── AuthService.java
│   │       ├── ContactService.java
│   │       ├── CsvExportService.java
│   │       ├── CsvImportService.java
│   │       ├── SeedRunner.java
│   │       └── StatsService.java
│   │
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   └── db/migration/V1__create_schema.sql
│   │
│   ├── src/test/java/com/phonebook/
│   │   ├── AuthControllerTest.java      # 12 tests
│   │   ├── ContactControllerTest.java   # 15 tests
│   │   ├── CsvExportControllerTest.java # 7 tests
│   │   ├── CsvImportControllerTest.java  # 22 tests
│   │   ├── JwtTokenProviderTest.java    # 6 tests
│   │   ├── PhonebookApplicationTests.java # 1 test
│   │   ├── SearchPaginationTest.java    # 7 tests
│   │   ├── StatsControllerTest.java     # 3 tests
│   │   └── TestHelper.java
│   │
│   ├── src/test/resources/application.yml
│   ├── Dockerfile
│   ├── pom.xml
│   └── mvnw / mvnw.cmd                  # Maven Wrapper
│
├── frontend/
│   ├── src/
│   │   ├── main.js          # app entry, 401 → login wiring
│   │   ├── App.vue          # sidebar / hamburger app shell
│   │   ├── router.js        # routes + auth guard
│   │   ├── api.js           # axios wrapper + interceptors
│   │   ├── auth.js          # auth state (token in localStorage)
│   │   ├── style.css        # design system styles
│   │   └── views/
│   │       ├── DashboardView.vue
│   │       ├── ContactList.vue
│   │       ├── ContactDetail.vue
│   │       ├── CreateContact.vue
│   │       ├── LoginView.vue
│   │       └── ProfileView.vue
│   │
│   ├── Dockerfile
│   ├── nginx.conf           # serves the app, proxies /api → backend
│   ├── package.json
│   └── vite.config.js
│
├── docker-compose.yml
├── .env.example
├── .gitignore
└── README.md
```

---

### Byte 3: Spring Boot Application Entry Point

**Builds on:** Byte 2

**In plain terms:**

`PhonebookApplication.java` is the main entry point. It is a standard Spring Boot application class annotated with `@SpringBootApplication`, which enables component scanning, auto-configuration, and property binding. The `main` method starts the embedded Tomcat server on port 8000.

**The code:**

```java
@SpringBootApplication
public class PhonebookApplication {
  public static void main(String[] args) {
    SpringApplication.run(PhonebookApplication.class, args);
  }
}
```

Spring Boot auto-configures the web server, Spring Security, Spring Data JPA, Flyway, and the connection pool (HikariCP) based on the dependencies in `pom.xml` and the properties in `application.yml`.

---

### Byte 4: JPA Entities — `Contact.java` and `User.java`

**Builds on:** Byte 3

**In plain terms:**

Entities are Java classes that map to database tables. Spring Data JPA uses Hibernate under the hood to translate between Java objects and SQL rows. The `Contact` entity represents a row in the `contacts` table, and the `User` entity represents a row in the `users` table used for authentication.

**The Contact entity:**

```java
@Entity
@Table(name = "contacts")
public class Contact {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "name", nullable = false, length = 255)
  private String name;

  @Column(name = "phone_number", nullable = false, length = 20)
  private String phoneNumber;

  @Column(name = "email", length = 255)
  private String email;

  @Column(name = "address", columnDefinition = "text")
  private String address;

  @Column(name = "created_at", nullable = false, updatable = false)
  @CreationTimestamp
  private OffsetDateTime createdAt;
}
```

Key annotations:
- `@Entity` — marks the class as a JPA entity
- `@Table(name = "contacts")` — maps to the `contacts` table
- `@Id` + `@GeneratedValue(strategy = IDENTITY)` — auto-increment primary key
- `@Column(nullable = false)` — maps to a `NOT NULL` database column
- `@CreationTimestamp` — Hibernate sets `createdAt` automatically on insert

The `User` entity stores `username` (unique, indexed), an optional unique `email`, the BCrypt `hashedPassword`, and `createdAt`.

---

### Byte 5: DTOs — Data Transfer Objects

**Builds on:** Byte 4

**In plain terms:**

DTOs define the structure of data entering and leaving the API. They are separate from the entities so the database schema and the API contract can evolve independently. All DTOs use `@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)` so Java field names like `phoneNumber` are serialized as `phone_number` in JSON — matching the original API contract.

**Key DTOs:**

- `ContactCreateRequest` — payload for creating a contact. Uses Bean Validation annotations: `@NotBlank` on `name`, `@Pattern(regexp = "^\\+[1-9]\\d{1,14}$")` on `phoneNumber`, `@Email` on `email`.
- `ContactUpdateRequest` — payload for updating a contact. All fields are optional; only fields explicitly provided in the JSON body are changed (partial update). Each setter sets a boolean flag (e.g., `nameSet`) so the service knows which fields were provided.
- `ContactResponse` — contact as returned by the API. Adds `id` and `createdAt`.
- `LoginRequest` — `username` (accepts username or email) + `password`.
- `LoginResponse` — `accessToken`, `tokenType` ("bearer"), and `user`.
- `UserResponse` — user info returned by the API (never includes the password hash).
- `StatsResponse` — `totalContacts`, `recentContacts`, `withEmail`, `withPhone`, `recentList`.
- `CsvImportResult` — `total`, `imported`, `failed`, `duplicates`, `errors`.
- `CsvRowError` — `row` (file row number) + `errors` (list of messages).
- `ErrorResponse` — `{"detail": "message"}` — the standard error body for every API error.

---

### Byte 6: Repositories — Database Access Layer

**Builds on:** Byte 5

**In plain terms:**

Repositories are interfaces that Spring Data JPA implements automatically. They provide standard CRUD operations (find, save, delete) and custom query methods defined with `@Query` annotations. No SQL is written by hand for standard operations.

**ContactRepository:**

```java
public interface ContactRepository extends JpaRepository<Contact, Long> {

  // Case-insensitive search by name or phone number, paginated
  @Query("SELECT c FROM Contact c WHERE LOWER(c.name) LIKE LOWER(CONCAT('%', :q, '%'))" +
         " OR LOWER(c.phoneNumber) LIKE LOWER(CONCAT('%', :q, '%')) ORDER BY c.id")
  Page<Contact> search(@Param("q") String q, Pageable pageable);

  // Same filter without pagination (used by CSV export)
  @Query("SELECT c FROM Contact c WHERE LOWER(c.name) LIKE LOWER(CONCAT('%', :q, '%'))" +
         " OR LOWER(c.phoneNumber) LIKE LOWER(CONCAT('%', :q, '%')) ORDER BY c.id")
  List<Contact> searchAll(@Param("q") String q);

  // Used by CSV import duplicate detection
  @Query("SELECT c.phoneNumber FROM Contact c")
  List<String> findAllPhoneNumbers();

  @Query("SELECT c.email FROM Contact c")
  List<String> findAllEmails();

  // Used by dashboard statistics
  @Query("SELECT COUNT(c) FROM Contact c WHERE c.createdAt >= :since")
  long countCreatedSince(@Param("since") OffsetDateTime since);

  long countByEmailIsNotNull();
  long countByPhoneNumberIsNotNull();

  @Query("SELECT c FROM Contact c ORDER BY c.createdAt DESC, c.id DESC")
  List<Contact> findRecent(Pageable pageable);
}
```

**UserRepository** provides `findByUsername`, `findByEmail`, `findByUsernameOrEmail`, and `existsByUsername`.

---

### Byte 7: Service Layer — Business Logic

**Builds on:** Byte 6

**In plain terms:**

Services contain the business logic. Controllers delegate to services, and services use repositories to access the database. This separation keeps controllers thin and makes business logic testable.

**ContactService** — CRUD operations:
- `create(ContactCreateRequest)` — saves a new contact; catches `DataIntegrityViolationException` and throws `ApiException(409)` for duplicate phone/email
- `update(Long id, ContactUpdateRequest)` — partial update: only fields with their `*Set` flag true are applied
- `delete(Long id)` — finds and deletes a contact; throws `ApiException(404)` if not found
- `getContact(Long id)` — finds a contact or throws 404

**AuthService** — authentication:
- `login(LoginRequest)` — finds user by username or email, verifies password with BCrypt, creates JWT
- `findByUsername(String)` — loads user for `/auth/me`

**StatsService** — dashboard statistics:
- `getStats()` — total contacts, contacts created in last 7 days, contacts with email, contacts with phone, 5 most recent contacts

**CsvImportService** — CSV import (covered in Byte 21)

**CsvExportService** — CSV export (covered in Byte 22)

**SeedRunner** — demo data generation (covered in Byte 23)

---

### Byte 8: Controllers — REST API Endpoints

**Builds on:** Byte 7

**In plain terms:**

Controllers handle HTTP requests, validate input, call services, and return responses. Each controller maps to a base path and defines endpoints with annotations like `@GetMapping`, `@PostMapping`, etc.

**HealthController** — `GET /health` returns `{"status": "ok"}`.

**AuthController** — authentication endpoints:
- `POST /auth/login` — accepts `{"username": "...", "password": "..."}`, returns `{"access_token": "...", "token_type": "bearer", "user": {...}}`
- `GET /auth/me` — returns the currently authenticated user (requires Bearer token)
- `POST /auth/logout` — returns `{"message": "Logged out"}` (client discards the token)

**ContactController** — contact management:
- `GET /contacts` — list with `page`, `page_size`, `q` query params; sets `X-Total-Count`, `X-Page`, `X-Page-Size` headers
- `POST /contacts` — create (returns 201)
- `GET /contacts/{id}` — retrieve one
- `PUT /contacts/{id}` — partial update
- `DELETE /contacts/{id}` — delete (returns 204)
- `POST /contacts/import` — CSV import (multipart field "file")
- `GET /contacts/export` — CSV export (optional `q` filter)

**StatsController** — `GET /stats` returns dashboard statistics.

---

### Byte 9: Spring Security Configuration

**Builds on:** Byte 8

**In plain terms:**

`SecurityConfig.java` configures Spring Security for a stateless REST API. It disables CSRF (not needed for JWT), sets session creation to STATELESS, configures CORS, and defines which endpoints are public vs protected.

**The configuration:**

```java
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
  http
    .csrf(AbstractHttpConfigurer::disable)
    .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
    .cors(Customizer.withDefaults())
    .httpBasic(AbstractHttpConfigurer::disable)
    .formLogin(AbstractHttpConfigurer::disable)
    .exceptionHandling(exception -> exception.authenticationEntryPoint(authenticationEntryPoint))
    .authorizeHttpRequests(auth -> auth
      .requestMatchers("/health", "/auth/login", "/auth/logout").permitAll()
      .requestMatchers("/auth/me", "/stats", "/contacts", "/contacts/**").authenticated()
      .anyRequest().permitAll()  // unknown routes → 404
    );
  http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
  return http.build();
}
```

**Endpoint security:**

| Endpoint | Access |
| --- | --- |
| `GET /health` | Public |
| `POST /auth/login` | Public |
| `POST /auth/logout` | Public |
| `GET /auth/me` | Authenticated |
| `GET /stats` | Authenticated |
| `GET/POST /contacts` | Authenticated |
| `GET/PUT/DELETE /contacts/{id}` | Authenticated |
| `POST /contacts/import` | Authenticated |
| `GET /contacts/export` | Authenticated |
| Unknown routes | Falls through to 404 |

The `PasswordEncoder` bean uses `BCryptPasswordEncoder`, which is fully compatible with BCrypt hashes created by other BCrypt implementations.

---

### Byte 10: JWT Authentication — Token Creation and Verification

**Builds on:** Byte 9

**In plain terms:**

`JwtTokenProvider.java` creates and verifies JWT access tokens. Tokens are signed with HS256 and expire after 12 hours (720 minutes). The signing key is derived from the `JWT_SECRET_KEY` environment variable (falling back to `SECRET_KEY`).

**Token creation:**

```java
public String createToken(String username) {
  Date now = new Date();
  Date expiry = new Date(now.getTime() + expirationMillis);
  return Jwts.builder()
    .subject(username)
    .issuedAt(now)
    .expiration(expiry)
    .signWith(signingKey)
    .compact();
}
```

The token payload uses the standard `sub` (subject) claim for the username and `exp` for the expiry time. The signing key is SHA-256-derived from the configured secret so secrets of any length work. If no secret is configured, an ephemeral random key is generated on each start — tokens stop working after a restart.

---

### Byte 11: JWT Authentication — Request Filtering

**Builds on:** Byte 10

**In plain terms:**

`JwtAuthenticationFilter.java` is a servlet filter that runs on every request. It extracts the Bearer token from the `Authorization` header, validates it, loads the user from the database, and sets the security context. Invalid or missing tokens leave the context empty, so Spring Security answers 401 via the entry point on protected endpoints.

**The filter logic:**

```java
String header = request.getHeader("Authorization");
if (header != null && header.startsWith("Bearer ")) {
  String token = header.substring(7);
  if (tokenProvider.validateToken(token)) {
    String username = tokenProvider.getUsername(token);
    UserDetails userDetails = userDetailsService.loadUserByUsername(username);
    UsernamePasswordAuthenticationToken authentication =
      new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
    SecurityContextHolder.getContext().setAuthentication(authentication);
  }
  // Invalid token: leave context empty → 401 on protected endpoints
}
filterChain.doFilter(request, response);
```

`SecurityUserService.java` implements Spring Security's `UserDetailsService` to load users from the database by username.

---

### Byte 12: JWT Authentication — 401 Error Response

**Builds on:** Byte 11

**In plain terms:**

`JwtAuthenticationEntryPoint.java` handles unauthenticated requests to protected endpoints. It returns the same 401 response as the original API: status 401, a `WWW-Authenticate: Bearer` header, and a `{"detail": "Not authenticated"}` JSON body.

```java
public void commence(HttpServletRequest request, HttpServletResponse response,
                     AuthenticationException authException) throws IOException {
  response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
  response.setHeader("WWW-Authenticate", "Bearer");
  response.setContentType(MediaType.APPLICATION_JSON_VALUE);
  objectMapper.writeValue(response.getWriter(), new ErrorResponse("Not authenticated"));
}
```

---

### Byte 13: Global Exception Handling

**Builds on:** Byte 12

**In plain terms:**

`GlobalExceptionHandler.java` is a `@RestControllerAdvice` that catches all exceptions thrown by controllers and services, and translates them into the standard `{"detail": "message"}` JSON response with the correct HTTP status code.

**Exception mappings:**

| Exception | HTTP Status | Example |
| --- | --- | --- |
| `ApiException` | Custom (400/404/409/413) | Business logic errors |
| `MethodArgumentNotValidException` | 422 | Bean Validation failures on request body |
| `ConstraintViolationException` | 422 | Bean Validation failures on request params |
| `HttpMessageNotReadableException` | 422 | Malformed JSON body |
| `MethodArgumentTypeMismatchException` | 422 | Wrong type for path variable |
| `MissingServletRequestPartException` | 422 | Missing multipart file |
| `MaxUploadSizeExceededException` | 413 | File too large |
| `NoHandlerFoundException` | 404 | Unknown route |
| `HttpRequestMethodNotSupportedException` | 405 | Wrong HTTP method |
| `Exception` (catch-all) | 500 | Unexpected errors |

This ensures the frontend always receives a consistent error format regardless of what goes wrong.

---

### Byte 14: Flyway Database Migrations

**Builds on:** Byte 13

**In plain terms:**

Flyway manages database schema changes. On startup, it checks which migrations have been applied (recorded in the `flyway_schema_history` table) and runs any new ones. The migration file `V1__create_schema.sql` creates the `users` and `contacts` tables.

**The migration file:** `backend/src/main/resources/db/migration/V1__create_schema.sql`

```sql
CREATE TABLE users (
    id INTEGER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    username VARCHAR(100) NOT NULL,
    email VARCHAR(255),
    hashed_password VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_users_username UNIQUE (username),
    CONSTRAINT uk_users_email UNIQUE (email)
);
CREATE INDEX idx_users_username ON users (username);

CREATE TABLE contacts (
    id INTEGER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    phone_number VARCHAR(20) NOT NULL,
    email VARCHAR(255),
    address TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_contacts_phone UNIQUE (phone_number),
    CONSTRAINT uk_contacts_email UNIQUE (email)
);
CREATE INDEX idx_contacts_phone_number ON contacts (phone_number);
```

**Safety settings in `application.yml`:**
- `spring.jpa.hibernate.ddl-auto: none` — Hibernate never creates, updates, or drops tables
- `spring.flyway.baseline-on-migrate: true` — if the database already has the tables, Flyway baselines instead of trying to re-create them

The migration uses standard SQL (identity columns, `TIMESTAMP WITH TIME ZONE`) so it runs on both PostgreSQL and H2 (used in tests).

---

### Byte 15: Pagination and Search

**Builds on:** Byte 14

**In plain terms:**

The contact list endpoint supports server-side pagination and case-insensitive search. The frontend sends `page` (1-based), `page_size` (1–100, default 10), and an optional `q` search term.

**How it works:**

1. The controller receives `page`, `page_size`, and `q` as query parameters
2. It creates a `PageRequest` with `Sort.by("id").ascending()` (contacts are always ordered by ID)
3. If `q` is provided, `ContactRepository.search(q, pageRequest)` runs a JPQL query with `LOWER(...) LIKE LOWER(CONCAT('%', :q, '%'))` on both `name` and `phoneNumber`
4. If `q` is empty, `ContactRepository.findAll(pageRequest)` returns all contacts
5. The response includes pagination headers: `X-Total-Count`, `X-Page`, `X-Page-Size`

**The response headers:**

| Header | Value | Meaning |
| --- | --- | --- |
| `X-Total-Count` | e.g. `42` | Total number of matching contacts |
| `X-Page` | e.g. `1` | Current page number |
| `X-Page-Size` | e.g. `10` | Items per page |

The frontend reads `X-Total-Count` to calculate the total number of pages for the pagination UI.

---

### Byte 16: CSV Import — Overview

**Builds on:** Byte 15

**In plain terms:**

Users can bulk-load contacts by uploading a `.csv` file from the Contacts page. The backend parses the file with Apache Commons CSV, validates every row using the same Bean Validation rules as the web form, skips duplicates, and commits all valid rows in one transaction.

**Import rules:**
- File must have a `.csv` extension
- Maximum file size: 5 MB
- Maximum rows: 5,000
- Must be UTF-8 encoded (BOM tolerated)
- Must have `name` and `phone_number` columns (case-insensitive, whitespace-tolerant)
- `email` and `address` columns are optional
- Empty optional cells are stored as `NULL`
- Duplicate phone numbers or emails (existing or within the file) are skipped and counted
- All valid rows are committed in a single transaction

---

### Byte 17: CSV Import — Step by Step

**Builds on:** Byte 16

**In plain terms:**

`CsvImportService.java` implements the import logic. Here is what happens step by step:

**Step 1 — File validation:**
```java
String filename = file.getOriginalFilename().toLowerCase();
if (!filename.endsWith(".csv")) {
  throw new ApiException(HttpStatus.BAD_REQUEST, "Only .csv files can be imported.");
}
```

**Step 2 — Size check:** Read at most `IMPORT_MAX_BYTES + 1` bytes. If the content is larger, reject with 413.

**Step 3 — Encoding check:** Decode as strict UTF-8 (with BOM stripping). Invalid encoding → 400.

**Step 4 — Header parsing:** Map column names case-insensitively to canonical field names. Require `name` and `phone_number` columns.

**Step 5 — Row validation:** For each row, build a `ContactCreateRequest` and validate it with the Bean Validation API. Invalid rows are collected with their file row number.

**Step 6 — Duplicate detection:** Load all existing phone numbers and emails into sets. For each valid row, check if the phone or email already exists (or appeared earlier in the same file). Duplicates are counted and skipped.

**Step 7 — Atomic commit:** All valid, non-duplicate rows are saved in a single transaction. If a database error occurs, the entire import is rolled back and a 409 is returned.

**The response:**

```json
{
  "total": 10,
  "imported": 8,
  "failed": 1,
  "duplicates": 1,
  "errors": [
    {"row": 3, "errors": ["phone_number: must match \"^\\+[1-9]\\d{1,14}$\""]}
  ]
}
```

---

### Byte 18: CSV Export

**Builds on:** Byte 17

**In plain terms:**

The **Export CSV** button downloads every contact the user is authorized to access — not just the current page — as a CSV file. If a search is active, only matching contacts are exported.

**How it works:**

1. The controller receives an optional `q` search parameter
2. It sets the response content type to `text/csv` and the `Content-Disposition` header to `attachment; filename="phonebook-export-<date>.csv"`
3. `CsvExportService.writeCsv()` writes the CSV directly to the response output stream using Apache Commons CSV
4. The export uses the same search filter as the list endpoint (`ContactRepository.searchAll(q)`)

**Formula-injection protection:**

Values that spreadsheet applications could interpret as formulas (starting with `=`, `+`, `-`, or `@`) are prefixed with a single quote. Since every phone number starts with `+`, they are all protected:

```java
private static String sanitize(String value) {
  if (value != null && !value.isEmpty() && "+=-@".indexOf(value.charAt(0)) >= 0) {
    return "'" + value;
  }
  return value == null ? "" : value;
}
```

The data itself is preserved — Excel, LibreOffice, and Google Sheets display the value unchanged. An empty result set downloads a header-only CSV.

---

### Byte 19: Seed Data Generation

**Builds on:** Byte 18

**In plain terms:**

`SeedRunner.java` is a command-line runner that creates demo data. It runs only when explicitly requested (off by default). It can create the first user and generate thousands of realistic dummy contacts with JavaFaker.

**Usage:**

```bash
# Create a demo user (password prompted if not supplied)
docker compose exec backend java -jar app.jar \
  --seed.create-user=true --seed.username=demo --seed.password='your-password'

# Generate dummy contacts
docker compose exec backend java -jar app.jar --seed.count=1000

# Combine both
docker compose exec backend java -jar app.jar \
  --seed.create-user=true --seed.username=demo --seed.password='your-password' --seed.count=1000
```

**Safety features:**
- Idempotent: checks if the user already exists before creating
- Unique phone numbers and emails: generated values are checked against existing rows and within the batch
- Never resets, truncates, or modifies existing data
- Contacts are inserted in batches of 500
- Environment variables `SEED_USERNAME`, `SEED_PASSWORD`, `SEED_COUNT` are also supported

---

### Byte 20: Docker Compose — Running the Complete Application

**Builds on:** Byte 19

**In plain terms:**

`docker-compose.yml` defines the services required to run the Phonebook application together. The Compose project is named `phonebook-spring-boot`, so its network and volume are completely separate from any other project.

**The services:**

| Service | Image / Build | Host Port | Internal Port | Notes |
| --- | --- | --- | --- | --- |
| `db` | `postgres:16-alpine` | (internal) | 5432 | Persistent data in `pgdata` volume |
| `backend` | `./backend` (Maven multi-stage) | 8000 → 8000 | 8000 | Spring Boot application |
| `frontend` | `./frontend` (Vite + nginx) | 8080 → 80 | 80 | Vue.js app, proxies `/api/` to backend |

**Key configuration:**
- All services use `restart: unless-stopped`
- The backend waits for the database healthcheck (`pg_isready`) before starting
- The frontend's nginx proxies `/api/` requests to `http://backend:8080/`
- The database is not exposed to the host (internal only)

**Commands:**

```bash
# Start everything
docker compose up -d

# Rebuild images and start
docker compose up -d --build

# Check status
docker compose ps

# View logs
docker compose logs -f backend

# Stop (keeps data)
docker compose down

# Stop and delete all data
docker compose down -v
```

---

### Byte 21: Environment Variables

**Builds on:** Byte 20

**In plain terms:**

Configuration lives in `.env` (see `.env.example`). Docker Compose passes these values into the containers. No secret is hardcoded in the source code.

**The variables:**

| Variable | Default | Used By | Purpose |
| --- | --- | --- | --- |
| `POSTGRES_USER` | `phonebook` | db, backend | PostgreSQL username |
| `POSTGRES_PASSWORD` | `change-me` | db, backend | PostgreSQL password |
| `POSTGRES_DB` | `phonebook_spring` | db, backend | Database name |
| `JWT_SECRET_KEY` | (empty) | backend | JWT signing key |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/phonebook_spring` | backend | Database URL override |
| `SPRING_DATASOURCE_USERNAME` | `phonebook` | backend | Database username override |
| `SPRING_DATASOURCE_PASSWORD` | `phonebook` | backend | Database password override |

**JWT secret behavior:**
- If `JWT_SECRET_KEY` is set, it is used to sign tokens
- If empty, the backend generates an ephemeral random key on each start (tokens die on restart)
- `SECRET_KEY` is accepted as a fallback for compatibility

---

### Byte 22: Safe Database Usage

**Builds on:** Byte 21

**In plain terms:**

The Java project uses a **separate database** (`phonebook_spring`) from any other project. The Docker Compose project creates its own network and volume, so there is no risk of interfering with other databases.

**Safety measures:**

1. **Separate database name:** `phonebook_spring` (not `phonebook`)
2. **Separate Docker volume:** `phonebook-spring-boot_pgdata` (not `phonebookpython_pgdata`)
3. **Separate Docker network:** `phonebook-spring-boot_default`
4. **`ddl-auto: none`:** Hibernate never creates, updates, or drops tables
5. **`baseline-on-migrate: true`:** Flyway baselines existing databases instead of failing
6. **Tests use H2:** The test suite runs against an in-memory H2 database, never touching PostgreSQL

---

### Byte 23: Running Tests

**Builds on:** Byte 22

**In plain terms:**

The backend has 73 automated tests across 8 test classes. Tests run against an in-memory H2 database — no PostgreSQL required.

**Test classes and counts:**

| Test Class | Tests | What It Covers |
| --- | --- | --- |
| `AuthControllerTest` | 12 | Login (username/email), wrong password, invalid/missing JWT, `/auth/me`, 401s |
| `ContactControllerTest` | 15 | CRUD, validation (422), duplicates (409), partial update, delete |
| `CsvExportControllerTest` | 7 | Export all pages, search filter, empty results, escaping, formula injection |
| `CsvImportControllerTest` | 22 | Valid/invalid rows, duplicates, encoding, malformed files, size/row limits |
| `JwtTokenProviderTest` | 6 | Token creation, validation, tampering, expiration, short/empty secrets |
| `PhonebookApplicationTests` | 1 | Spring context loads |
| `SearchPaginationTest` | 7 | Search by name/phone, case-insensitivity, pagination headers |
| `StatsControllerTest` | 3 | Dashboard stats, recent list, email/phone counts |
| **Total** | **73** | |

**Running tests:**

```bash
cd backend
./mvnw test
```

The test configuration (`src/test/resources/application.yml`) uses an in-memory H2 database with the same Flyway migration, proving the SQL works on both H2 and PostgreSQL.

---

### Byte 24: Frontend Entry Point — `main.js`

**Builds on:** Byte 23

**In plain terms:**

`frontend/src/main.js` is the entry point of the Vue.js frontend. It creates the Vue application, connects the router, loads the global stylesheet, and registers a handler that sends the user back to the login screen whenever the backend answers a request with 401 (for example, an expired token).

**The code:**

```javascript
import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import { onUnauthorized } from './api'
import './style.css'

onUnauthorized(() => {
  if (router.currentRoute.value.name !== 'login') {
    router.push({ name: 'login' })
  }
})

createApp(App).use(router).mount('#app')
```

---

### Byte 25: API Communication — `api.js`

**Builds on:** Byte 24

**In plain terms:**

`frontend/src/api.js` is responsible for communicating with the Spring Boot backend. It creates an axios instance whose base URL is `/api` — inside Docker, nginx proxies `/api` to the backend.

**The code:**

```javascript
const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api'
})
```

The file exports the API methods (`login`, `logout`, `getMe`, `getStats`, `listContacts`, `getContact`, `createContact`, `updateContact`, `deleteContact`, `importContacts`, `exportContacts`) and an `extractError` helper that turns API errors into readable messages.

---

### Byte 26: Token Attachment and 401 Handling

**Builds on:** Byte 25

**In plain terms:**

The axios instance in `api.js` uses interceptors so that no component has to handle tokens manually. Every outgoing request automatically gets the `Authorization: Bearer <token>` header when a token exists, and every 401 response clears the stored session and notifies the app — which redirects to the login page.

**The code:**

```javascript
api.interceptors.request.use((config) => {
  if (auth.token) {
    config.headers.Authorization = `Bearer ${auth.token}`
  }
  return config
})

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      clearAuth()
      if (unauthorizedHandler) unauthorizedHandler()
    }
    return Promise.reject(error)
  }
)
```

---

### Byte 27: Frontend Authentication State — `auth.js`

**Builds on:** Byte 26

**In plain terms:**

`frontend/src/auth.js` holds the signed-in state of the frontend. After login, the JWT and the user profile are stored in the browser's localStorage, so the user stays signed in across page refreshes. Every Vue component can import the shared reactive `auth` object.

**The code:**

```javascript
const TOKEN_KEY = 'phonebook_token'
const USER_KEY = 'phonebook_user'

export const auth = reactive({
  token: localStorage.getItem(TOKEN_KEY) || null,
  user: JSON.parse(localStorage.getItem(USER_KEY) || 'null'),
  loggingIn: false
})

export function setAuth(token, user) {
  auth.token = token
  auth.user = user
  localStorage.setItem(TOKEN_KEY, token)
  if (user) localStorage.setItem(USER_KEY, JSON.stringify(user))
}

export function clearAuth() {
  auth.token = null
  auth.user = null
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
}
```

---

### Byte 28: Vue Application Root — `App.vue`

**Builds on:** Byte 27

**In plain terms:**

`frontend/src/App.vue` is the main/root Vue component. It provides the common structure: a sidebar navigation on desktop, a hamburger slide-over menu on mobile, a top bar, and the `<router-view>` where the current page is displayed. The sidebar and top bar are only rendered when a user is signed in.

---

### Byte 29: Frontend Routing — `router.js`

**Builds on:** Byte 28

**In plain terms:**

`frontend/src/router.js` defines the routes of the Vue application. Every protected route carries `requiresAuth: true` metadata, and a navigation guard redirects signed-out users to the login page.

**The routes:**

```javascript
const routes = [
  { path: '/', name: 'dashboard', component: Dashboard, meta: { requiresAuth: true, title: 'Dashboard' } },
  { path: '/contacts', name: 'contacts', component: ContactList, meta: { requiresAuth: true, title: 'Contacts' } },
  { path: '/contacts/new', name: 'create-contact', component: CreateContact, meta: { requiresAuth: true, title: 'Add Contact' } },
  { path: '/contacts/:id', name: 'contact-detail', component: ContactDetail, props: true, meta: { requiresAuth: true, title: 'Contact Details' } },
  { path: '/profile', name: 'profile', component: Profile, meta: { requiresAuth: true, title: 'Account' } },
  { path: '/login', name: 'login', component: Login, meta: { title: 'Sign in' } },
  { path: '/:pathMatch(.*)*', redirect: '/' }
]
```

---

### Byte 30: Protecting Frontend Routes — the Navigation Guard

**Builds on:** Byte 29

**In plain terms:**

A Vue Router guard checks the `requiresAuth` metadata of each route before navigation and redirects to the login page — remembering the originally requested URL so the user can be sent back after signing in.

**The code:**

```javascript
router.beforeEach((to) => {
  if (to.meta.requiresAuth && !isAuthenticated.value) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  if (to.name === 'login' && isAuthenticated.value) {
    return { name: 'dashboard' }
  }
})
```

---

### Byte 31: The Login Screen — `LoginView.vue`

**Builds on:** Byte 30

**In plain terms:**

`frontend/src/views/LoginView.vue` is the sign-in page. It collects an email-or-username and a password, validates that both are filled in, calls the login API, stores the returned token with `setAuth`, and navigates to the dashboard (or back to the page the user originally requested). Errors such as a wrong password are shown on the page.

---

### Byte 32: Dashboard Statistics — `GET /stats`

**Builds on:** Byte 31

**In plain terms:**

The dashboard shows live statistics that the backend calculates from the database: the total number of contacts, how many were added in the last 7 days, how many have an email address, how many have a phone number, and the 5 most recently added contacts.

**The controller:**

```java
@GetMapping("/stats")
public StatsResponse getStats() {
  return statsService.getStats();
}
```

**The service:**

```java
public StatsResponse getStats() {
  OffsetDateTime since = OffsetDateTime.now().minus(7, ChronoUnit.DAYS);
  return new StatsResponse(
    contactRepository.count(),
    contactRepository.countCreatedSince(since),
    contactRepository.countByEmailIsNotNull(),
    contactRepository.countByPhoneNumberIsNotNull(),
    contactRepository.findRecent(PageRequest.of(0, 5)).stream()
      .map(ContactResponse::from).toList()
  );
}
```

---

### Byte 33: The Dashboard View — `DashboardView.vue`

**Builds on:** Byte 32

**In plain terms:**

`frontend/src/views/DashboardView.vue` is the landing page after sign-in. It fetches `/stats` when the page loads and renders four stat cards plus a list of the most recent contacts. Loading, error, and empty states are handled explicitly.

---

### Byte 34: Contact List View — `ContactList.vue`

**Builds on:** Byte 33

**In plain terms:**

`frontend/src/views/ContactList.vue` displays the contacts. It fetches contacts through `api.listContacts({ page, pageSize, search })`, reads the total from the `X-Total-Count` response header, and provides a search box, a page-size selector, skeleton rows while loading, empty and error states, pagination buttons, and an Edit link and Delete button per contact.

---

### Byte 35: Creating and Editing Contacts

**Builds on:** Byte 34

**In plain terms:**

`CreateContact.vue` is the add-contact form. `ContactDetail.vue` is both the detail view and the edit form for one contact. Both validate fields (name required, phone in E.164 format, optional email) and send data to the backend. Empty optional fields are sent as `null` so they are cleared in the database.

---

### Byte 36: Responsive Navigation — Sidebar and Hamburger Menu

**Builds on:** Byte 35

**In plain terms:**

The application shell in `App.vue` provides the menu. On wide screens the sidebar is always visible; on narrow screens it hides behind a hamburger button and slides in as an overlay with a backdrop. The menu contains Dashboard, Contacts, Add Contact, and Account links, a user chip showing the signed-in username, and a Logout button.

---

### Byte 37: The Account Page — `ProfileView.vue`

**Builds on:** Byte 36

**In plain terms:**

`frontend/src/views/ProfileView.vue` shows the signed-in user's information (username, email, member since) and a Logout button. It fetches fresh data from `/auth/me` on mount, falling back to the stored session if the API call fails.

---

### Byte 38: Important Edge Cases

**Builds on:** Byte 37

**In plain terms:**

A production-ready application must handle situations where things do not go as expected.

**Examples:**

```text
Invalid phone number
  ↓
Validation error (422)

Duplicate phone number
  ↓
Database constraint error (409)

Unknown contact ID
  ↓
Contact not found (404)

Missing/invalid/expired token
  ↓
Not authenticated (401)

Database unavailable
  ↓
Backend/database error (500)
```

---

### Byte 39: The Complete Picture

**Builds on:** Byte 38

**In plain terms:**

The Phonebook application is a full-stack system where each layer has a specific responsibility. Vue handles the user interface, Spring Boot handles API requests and validation, Spring Data JPA handles database interaction, and PostgreSQL stores the data.

**The complete flow:**

```text
User
  ↓
Vue.js Frontend (login page → token in localStorage)
  ↓
api.js (adds Bearer token to every request)
  ↓
Spring Boot Controller (JWT verified by JwtAuthenticationFilter)
  ↓
Bean Validation (422 on invalid input)
  ↓
Service Layer
  ↓
Spring Data JPA Repository
  ↓
PostgreSQL
  ↓
Database Response
  ↓
Spring Boot Response ({"detail":"message"} on error)
  ↓
Vue Frontend
  ↓
Updated User Interface
```

**API summary:**

| Endpoint | Purpose | Auth |
| --- | --- | --- |
| `GET /health` | Health check | — |
| `POST /auth/login` | Sign in, returns a JWT | — |
| `GET /auth/me` | Current signed-in user | ✓ |
| `POST /auth/logout` | Sign out (client discards token) | — |
| `GET /stats` | Dashboard statistics + recent contacts | ✓ |
| `GET /contacts` | List contacts (`page`, `page_size`, `q`; total in `X-Total-Count`) | ✓ |
| `POST /contacts` | Add a contact | ✓ |
| `POST /contacts/import` | Import contacts from an uploaded CSV | ✓ |
| `GET /contacts/export` | Download contacts as CSV (`q` filters) | ✓ |
| `GET /contacts/{id}` | Get one contact | ✓ |
| `PUT /contacts/{id}` | Update a contact (partial) | ✓ |
| `DELETE /contacts/{id}` | Delete a contact | ✓ |

**Why it matters:**

The separation of responsibilities makes the project easier to understand, test, maintain, and deploy. Docker Compose brings the complete environment together — frontend, backend, and PostgreSQL — so the application can be started consistently with `docker compose up -d --build`.
