# Phonebook Application (Spring Boot)

A full-stack phonebook app — Java Spring Boot backend with the existing Vue 3 frontend.

- **Backend:** Java 17 + Spring Boot 3.3 + Spring Security + Spring Data JPA + PostgreSQL
- **Frontend:** Vue 3 + Vite + Vue Router + Axios (unchanged from the Python version)
- **Deployment:** Docker Compose (backend, database, frontend services)

This project is a drop-in replacement for the Python/FastAPI backend. The API
contract (routes, status codes, response shapes, pagination headers, error
bodies) is identical, so the Vue.js frontend works without any changes.

## Features

- JWT authentication (login with username or email, BCrypt password hashing)
- Add, view, update, and delete contacts (full CRUD via REST API)
- Dashboard with live statistics from the database
- Contact list with search (by name or phone number) and Google-style pagination
- **Import contacts from CSV and export contacts to CSV** (with per-row
  validation reports, duplicate handling, and formula-injection protection)
- Input validation on both backend and frontend (E.164 phone format, unique phone/email)
- Responsive, modern dashboard UI (sidebar navigation, mobile hamburger menu)
- Seed utility to create the first user and generate thousands of dummy contacts
- Docker Compose setup with isolated database volume

## Architecture

```
+----------------+        /api/*         +----------------+        +----------------+
|   Frontend     | <-------------------> |    Backend     | <----> |   PostgreSQL   |
|  Vue 3/nginx   |   nginx proxy_pass    |  Spring Boot   |  db:   |   db :5432     |
|  http://:8080  |                       |    :8000       |  5432  |  (pgdata vol.) |
+----------------+                       +----------------+        +----------------+
```

All three services run in Docker Compose with `restart: unless-stopped`.
The Compose project is named `phonebook-spring-boot`, so its network and
volume (`phonebook-spring-boot_pgdata`) are completely separate from the
Python project's `phonebook-python_pgdata` volume.

Authentication: the frontend stores the JWT in localStorage and sends it as a
`Authorization: Bearer <token>` header. All contact and stats endpoints require
a valid token; unauthenticated requests get `401` and the app redirects to the
login page. Passwords are stored as BCrypt hashes (compatible with the Python
backend's hashes).

## Prerequisites

- [Docker Desktop](https://www.docker.com/products/docker-desktop) with the WSL 2 backend (Windows), or Docker Engine + Compose v2 (Linux/macOS)
- Or for local development: Java 17+, Maven 3.9+, Node.js 20+, PostgreSQL 16

## Quick start (Docker)

1. Create your environment file (database credentials and JWT secret):

   ```bash
   cp .env.example .env
   ```

2. Build and start the application:

   ```bash
   docker compose up --build -d
   ```

3. **Create the first user** (required before you can sign in):

   ```bash
   docker compose exec backend java -jar app.jar \
     --seed.create-user=true --seed.username=demo --seed.password='your-password'
   ```

4. Open the application:

   | URL | What |
   | --- | --- |
   | <http://localhost:8080> | Frontend (Vue 3 app) |
   | <http://localhost:8000> | Backend API (Spring Boot) |
   | <http://localhost:8000/health> | Health check |

5. Stop the application:

   ```bash
   docker compose down
   ```

6. Stop the application **and delete all data** (removes the PostgreSQL volume):

   ```bash
   docker compose down -v
   ```

## Docker services

| Service   | Image / build                    | Host port | Internal port | Notes                                    |
| --------- | -------------------------------- | --------- | ------------- | ---------------------------------------- |
| `frontend` | `./frontend` (Vite build to nginx) | 8080 → 80 | 80            | nginx proxies `/api/` to the backend     |
| `backend`  | `./backend` (Maven multi-stage)   | 8000 → 8000 | 8000          | Spring Boot; connects to PostgreSQL      |
| `db`       | `postgres:16-alpine`              | (internal) | 5432         | Persistent data in the `pgdata` volume   |

### PostgreSQL configuration

Configured through `.env` (see `.env.example`):

| Variable         | Default         | Where used                                          |
| ---------------- | --------------- | --------------------------------------------------- |
| `POSTGRES_USER`  | `phonebook`     | superuser created by the postgres image             |
| `POSTGRES_PASSWORD` | `change-me`  | password for that user                              |
| `POSTGRES_DB`    | `phonebook_spring` | database created on first startup               |
| `JWT_SECRET_KEY` | (empty)         | JWT signing key; set a long random string in `.env` |

- The backend connects to `jdbc:postgresql://db:5432/phonebook_spring` —
  the Compose **service name** `db`, never `localhost` (inside a container,
  `localhost` refers to the container itself).
- PostgreSQL data persists in the `phonebook-spring-boot_pgdata` Docker volume.
  `docker compose down` keeps the volume; `docker compose down -v` deletes it.
- The backend waits for PostgreSQL via `depends_on: condition: service_healthy`
  (`pg_isready` healthcheck), and creates the `contacts` and `users` tables
  via Flyway migrations on startup.
- If `JWT_SECRET_KEY` is empty, the backend generates an ephemeral random key
  on each start (fine for demos, but users must log in again after a restart).

## API reference

| Method | Endpoint            | Description                                            | Auth |
| ------ | ------------------- | ------------------------------------------------------ | ---- |
| GET    | `/health`           | Health check                                           | —    |
| POST   | `/auth/login`       | Login with username or email, returns a JWT            | —    |
| GET    | `/auth/me`          | Currently logged-in user                               | ✓    |
| POST   | `/auth/logout`      | Logout (client discards the token)                     | —    |
| GET    | `/stats`            | Dashboard statistics + 5 most recent contacts          | ✓    |
| GET    | `/contacts`         | List contacts (`page`, `page_size`, `q` params; total in `X-Total-Count` header) | ✓    |
| POST   | `/contacts`         | Add a new contact                                      | ✓    |
| POST   | `/contacts/import`  | Import contacts from an uploaded CSV file              | ✓    |
| GET    | `/contacts/export`  | Download contacts as CSV (`q` param filters the export) | ✓    |
| GET    | `/contacts/{id}`    | Retrieve a specific contact                            | ✓    |
| PUT    | `/contacts/{id}`    | Update a specific contact (only provided fields change) | ✓    |
| DELETE | `/contacts/{id}`    | Delete a specific contact                              | ✓    |

Call an authenticated endpoint with the header `Authorization: Bearer <access_token>`.

Request/response example (`POST /contacts`):

```json
{
  "name": "John Doe",
  "phone_number": "+1234567890",
  "email": "john@example.com",
  "address": "123 Main St"
}
```

Validation rules:
- `name` is required (max 255 chars)
- `phone_number` is required, unique, and must match E.164 format, e.g. `+1234567890`
- `email` is optional but must be a valid email and unique when provided
- `address` is optional

Error responses: `401` (not authenticated or bad credentials), `404` (contact not found), `409` (duplicate phone number or email),
`422` (validation error), `400` (CSV format error), `413` (CSV too large or too many rows).

## Authentication

- **Login** — `POST /auth/login` with `{"username": "...", "password": "..."}`
  (the `username` field accepts a username *or* an email). Returns
  `{"access_token", "token_type": "bearer", "user": {...}}`.
- Passwords are hashed with **BCrypt** (Spring Security's `BCryptPasswordEncoder`,
  fully compatible with hashes created by the Python `bcrypt` library).
- Tokens are **JWTs** signed with **HS256** using `JWT_SECRET_KEY` (from `.env`) and expire
  after 12 hours. The frontend stores the token in localStorage and
  sends it on every request; a `401` response logs the user out.
- Contact and stats endpoints require the header
  `Authorization: Bearer <token>`.
- Missing or invalid tokens return `401` with a `WWW-Authenticate: Bearer`
  header and a `{"detail": "Not authenticated"}` JSON body.

**Create the first/demo user** (there are no users by default):

```bash
docker compose exec backend java -jar app.jar \
  --seed.create-user=true --seed.username=demo --seed.password='your-password'
```

## Seeding dummy data

The seed utility (`SeedRunner`) generates realistic dummy contacts with
JavaFaker. Phone numbers and emails are generated uniquely, so existing
contacts are never duplicated or modified. It never resets or truncates
existing data.

```bash
docker compose exec backend java -jar app.jar --seed.count=100    # 100 contacts
docker compose exec backend java -jar app.jar --seed.count=1000   # 1,000 contacts
docker compose exec backend java -jar app.jar --seed.count=5000   # 5,000 contacts
```

Both actions can be combined:

```bash
docker compose exec backend java -jar app.jar \
  --seed.create-user=true --seed.username=demo --seed.count=1000
```

The seed operation is manual — it never runs automatically on startup.

Environment variables are also supported: `SEED_USERNAME`, `SEED_PASSWORD`,
`SEED_COUNT`.

## CSV import & export

### Importing contacts

On the **Contacts** page, click **Import CSV** and choose a `.csv` file
(UTF-8 encoded). A sample template is available via the **CSV template**
link next to the page-size selector. The CSV must have a header row with
these columns:

```csv
name,phone_number,email,address
Rahul Sharma,+919876543210,rahul@example.com,Pune
Amit Patil,+919876543211,amit@example.com,Mumbai
```

| Column        | Required | Rules (same as the web form)                              |
| ------------- | -------- | --------------------------------------------------------- |
| `name`        | Yes      | 1-255 characters                                          |
| `phone_number`| Yes      | Unique, E.164 format (`+` followed by 1-15 digits)        |
| `email`       | No       | Valid email, unique when provided; empty cell = no email  |
| `address`     | No       | Free text; empty cell = no address                        |

Column names are matched case-insensitively and tolerate surrounding
whitespace; extra columns are ignored, and `email`/`address` columns may
be omitted entirely.

**How results are reported.** After the import the page shows a summary
with the total rows processed, contacts imported, duplicates skipped, and
failed rows — plus a per-row error list with the file row number and the
validation reason. Invalid rows are never silently skipped.

**Duplicates.** Rows whose phone number or email already exists (or appears
twice in the same file) are counted as duplicates and skipped — they do
not abort the import, and existing contacts are never modified.

**Limits.** Uploads are capped at **5 MB** and **5,000 rows** per file
(larger files are rejected with `413`). All valid, non-duplicate rows are
inserted in a **single transaction** — a database error rolls back the
entire import, so the phonebook never ends up partially updated.

### Exporting contacts

Click **Export CSV** on the Contacts page. The backend generates a
`phonebook-export-<date>.csv` and downloads it. The export contains
**every contact you are authorized to access** — not just the current
page — and respects an active search (only matching contacts are
exported). Empty result sets download a header-only CSV.

**CSV formula-injection protection:** values beginning with `=`, `+`, `-`,
or `@` (which includes every phone number, since they start with `+`) are
prefixed with a single quote (`'`) so spreadsheet applications treat them
as text instead of formulas. The data itself is preserved — Excel, LibreOffice
and Google Sheets display the value unchanged. Note that re-importing an
exported file requires removing that leading quote from the phone numbers
first, because the importer validates the strict E.164 format.

Both endpoints are also available via the API:

```bash
# Import (multipart form field "file")
curl -X POST http://localhost:8000/contacts/import \
  -H "Authorization: Bearer <token>" -F "file=@contacts.csv"

# Export (optionally filtered: ?q=Rahul)
curl http://localhost:8000/contacts/export \
  -H "Authorization: Bearer <token>" -o phonebook-export.csv
```

## Local development (without Docker)

### Backend

```bash
cd backend
./mvnw spring-boot:run
```

The backend defaults to `jdbc:postgresql://localhost:5432/phonebook_spring`
(separate from the Python project's `phonebook` database). Override with
environment variables or `SPRING_DATASOURCE_URL`:

```bash
# Windows
set SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/phonebook_spring
set SPRING_DATASOURCE_USERNAME=phonebook
set SPRING_DATASOURCE_PASSWORD=phonebook
set JWT_SECRET_KEY=your-long-random-secret
./mvnw spring-boot:run
```

```bash
# macOS/Linux
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/phonebook_spring
export SPRING_DATASOURCE_USERNAME=phonebook
export SPRING_DATASOURCE_PASSWORD=phonebook
export JWT_SECRET_KEY=your-long-random-secret
./mvnw spring-boot:run
```

Create the first user and seed data:

```bash
java -jar target/phonebook-spring-boot-1.0.0.jar \
  --seed.create-user=true --seed.username=demo --seed.password='your-password'
java -jar target/phonebook-spring-boot-1.0.0.jar --seed.count=1000
```

### Frontend

```bash
cd frontend
npm install
npm run dev
```

The Vite dev server proxies `/api` requests to `http://localhost:8000`.

## Tests

```bash
cd backend
./mvnw test
```

The test suite (73 tests) covers:
- Login with username and email, wrong credentials, invalid/missing JWTs
- Current-user endpoint and authorization (401s with `WWW-Authenticate: Bearer`)
- Dashboard statistics
- Contact CRUD, validation, duplicate handling (409)
- Search and pagination, including `X-Total-Count` / `X-Page` / `X-Page-Size` headers
- CSV import: valid data, invalid rows, duplicates, encoding, malformed files, size/row limits
- CSV export: all pages, search filtering, empty results, escaping, formula-injection protection
- JWT token creation, validation, tampering, expiration

Tests run against an in-memory H2 database — no PostgreSQL required.

## Project structure

```
PhonebookSpringBoot/
├── docker-compose.yml        # backend + db + frontend services (isolated project)
├── .env.example              # environment variables (database credentials, JWT secret)
├── .github/workflows/ci.yml # GitHub Actions: backend tests + frontend build
├── backend/
│   ├── Dockerfile            # multi-stage Maven build
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── java/com/phonebook/
│       │   │   ├── PhonebookApplication.java
│       │   │   ├── config/SecurityConfig.java
│       │   │   ├── controller/      # Health, Auth, Contact, Stats controllers
│       │   │   ├── dto/             # request/response DTOs (snake_case JSON)
│       │   │   ├── entity/          # JPA entities (Contact, User)
│       │   │   ├── exception/       # ApiException + global error handler
│       │   │   ├── repository/      # Spring Data JPA repositories
│       │   │   ├── security/        # JWT filter, entry point, user service
│       │   │   └── service/         # Contact, Auth, Stats, CSV import/export, Seed
│       │   └── resources/
│       │       ├── application.yml
│       │       └── db/migration/V1__create_schema.sql  # Flyway migration
│       └── test/
│           ├── java/com/phonebook/ # 73 tests (MockMvc + H2)
│           └── resources/application.yml
└── frontend/                 # unchanged Vue 3 app
    ├── Dockerfile
    ├── nginx.conf
    └── src/
        ├── api.js            # axios wrapper (Bearer token interceptor)
        ├── auth.js           # authentication state (token in localStorage)
        ├── router.js         # routes + auth guard
        ├── App.vue
        └── views/            # Dashboard, ContactList, ContactDetail, CreateContact, Profile, Login
```

## Database schema

Managed by Flyway (`V1__create_schema.sql`), identical to the Python
backend's schema:

**users**: `id` (identity PK), `username` (varchar 100, unique, indexed),
`email` (varchar 255, unique, nullable), `hashed_password` (varchar 255),
`created_at` (timestamptz, default now())

**contacts**: `id` (identity PK), `name` (varchar 255), `phone_number`
(varchar 20, unique, indexed), `email` (varchar 255, unique, nullable),
`address` (text, nullable), `created_at` (timestamptz, default now())

Hibernate's `ddl-auto` is set to `none` — Flyway owns the schema. The
migration uses standard SQL so it runs on both PostgreSQL and H2 (tests).

## Troubleshooting

- **"Port already in use"**: change the host ports in `docker-compose.yml`
  (`"8000:8000"`, `"8080:80"`).
- **Backend returns 500 right after `docker compose restart db`**: transient —
  PostgreSQL needs a few seconds to accept connections. Wait ~10 seconds and retry.
- **Tokens invalid after restart**: set `JWT_SECRET_KEY` in `.env`.
- **Reset the database**: `docker compose down -v` (deletes the `pgdata` volume),
  then `docker compose up --build`.
- **Check logs**: `docker compose logs backend`, `docker compose logs db`,
  `docker compose logs frontend`.

## Possible enhancements

- Pinia state management, unit tests for the frontend, and the
  `vue-phone-number-input` component for richer phone input.
- A `docker-compose.override.yml` for local development with hot reload.
