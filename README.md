# Student & Staff Management System

A full-stack web application for managing student and staff records with JWT-based authentication, CSV import/export, email invitations, and comprehensive testing — built with Spring Boot and Next.js.

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Backend | Spring Boot 3, Java 17, Spring Security |
| Database | PostgreSQL 16 |
| Auth | JWT (jjwt 0.12.5) + BCrypt |
| Email | Resend SDK |
| Frontend | Next.js 14, TypeScript, Tailwind CSS |
| UI Components | shadcn/ui (Radix UI) |
| State | React Query (TanStack Query v5) |
| Testing | JUnit 5, Mockito, Cucumber 7, Playwright |
| Deployment | Docker + Docker Compose |

---

## Project Structure

```
student_management/
├── backend/                          # Spring Boot API
│   ├── src/main/java/com/studentmgmt/
│   │   ├── config/                   # JWT filter, security config
│   │   ├── controller/               # Auth, Student, Staff controllers
│   │   ├── dto/                      # Request/response DTOs
│   │   ├── entity/                   # Student, Staff, User JPA entities
│   │   ├── exception/                # GlobalExceptionHandler
│   │   ├── repository/               # Spring Data JPA repositories
│   │   └── service/                  # Auth, Student, Staff, Email services
│   ├── src/test/
│   │   ├── java/.../service/         # Unit tests (Mockito)
│   │   ├── java/.../integration/     # Integration tests (SpringBootTest)
│   │   ├── java/.../cucumber/        # Cucumber BDD step definitions
│   │   └── resources/features/       # Gherkin feature files
│   └── pom.xml
├── frontend/                         # Next.js app
│   ├── src/
│   │   ├── app/
│   │   │   ├── login/page.tsx        # Login + Register page
│   │   │   └── dashboard/
│   │   │       ├── page.tsx          # Student dashboard (stats, import/export)
│   │   │       └── staff/page.tsx    # Staff management page
│   │   ├── components/ui/            # shadcn components
│   │   ├── context/auth-context.tsx  # Auth state + token management
│   │   └── lib/api.ts                # Axios client + API functions
│   └── tests/e2e/                    # Playwright E2E tests
├── docker-compose.yml
└── APPROACH.md
```

---

## Prerequisites

- Java 17+
- Maven 3.9+
- Node.js 20+
- PostgreSQL 16 (or Docker)

---

## Running Locally

### Option 1 — Docker Compose (recommended)

Starts everything: database, backend, and frontend.

```bash
docker-compose up --build
```

| Service | URL |
|---------|-----|
| Frontend | http://localhost:3000 |
| Backend API | http://localhost:8080 |
| PostgreSQL | localhost:5432 |

---

### Option 2 — Manual

**1. Start PostgreSQL**

```bash
# Using Docker for just the database
docker run -d \
  --name studentdb \
  -e POSTGRES_DB=studentdb \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -p 5432:5432 \
  postgres:16-alpine
```

**2. Run the backend**

```bash
cd backend
mvn spring-boot:run
```

Backend starts on `http://localhost:8080`.

**3. Run the frontend**

```bash
cd frontend
npm install
npm run dev
```

Frontend starts on `http://localhost:3000`.

---

## Environment Configuration

### Backend — `backend/src/main/resources/application.yml`

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/studentdb
    username: postgres
    password: postgres

jwt:
  secret: <your-256-bit-hex-secret>
  expiration: 86400000   # 24 hours in ms
```

### Frontend — `frontend/.env.local`

```env
NEXT_PUBLIC_API_URL=http://localhost:8080/api
```

---

## API Reference

All protected endpoints require the header:
```
Authorization: Bearer <token>
```

### Auth

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| `POST` | `/api/auth/register` | No | Register a new user |
| `POST` | `/api/auth/login` | No | Login and receive JWT |

**Register request:**
```json
{
  "name": "John Doe",
  "email": "john@example.com",
  "password": "secret123"
}
```

**Login request:**
```json
{
  "email": "john@example.com",
  "password": "secret123"
}
```

**Auth response:**
```json
{
  "token": "eyJhbGci...",
  "name": "John Doe",
  "email": "john@example.com",
  "role": "STAFF"
}
```

---

### Students

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| `GET` | `/api/students` | Yes | List all students (paginated, searchable, filterable) |
| `GET` | `/api/students/{id}` | Yes | Get student by ID |
| `POST` | `/api/students` | Yes | Create a student |
| `PUT` | `/api/students/{id}` | Yes | Update a student |
| `DELETE` | `/api/students/{id}` | Yes | Delete a student |
| `PATCH` | `/api/students/{id}/toggle-active` | Yes | Toggle active/inactive status |
| `GET` | `/api/students/stats` | Yes | Get total, active, inactive counts |
| `GET` | `/api/students/export/csv` | Yes | Export students as CSV file |
| `POST` | `/api/students/import/csv` | Yes | Import students from CSV file |

**Bulk Operations:**

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| `POST` | `/api/students/bulk/delete` | Yes | Delete multiple students |
| `POST` | `/api/students/bulk/activate` | Yes | Activate multiple students |
| `POST` | `/api/students/bulk/deactivate` | Yes | Deactivate multiple students |
| `POST` | `/api/students/bulk/send-invite` | Yes | Send email invitations |

---

### Staff

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| `GET` | `/api/staff` | Yes | List all staff (paginated, searchable) |
| `GET` | `/api/staff/{id}` | Yes | Get staff by ID |
| `POST` | `/api/staff` | Yes | Create a staff member |
| `PUT` | `/api/staff/{id}` | Yes | Update a staff member |
| `DELETE` | `/api/staff/{id}` | Yes | Delete a staff member |
| `PATCH` | `/api/staff/{id}/toggle-active` | Yes | Toggle active/inactive status |
| `POST` | `/api/staff/bulk/delete` | Yes | Bulk delete staff |
| `POST` | `/api/staff/bulk/activate` | Yes | Bulk activate staff |
| `POST` | `/api/staff/bulk/deactivate` | Yes | Bulk deactivate staff |

---

### Request/Response Examples

**Student request body:**
```json
{
  "firstName": "Jane",
  "lastName": "Smith",
  "email": "jane@example.com",
  "phone": "9876543210",
  "studentClass": "10",
  "section": "A",
  "enrollmentDate": "2024-06-01"
}
```

**Stats response:**
```json
{
  "total": 85,
  "active": 15,
  "inactive": 5
}
```

**CSV import response:**
```json
{
  "imported": 10,
  "failed": 2,
  "errors": ["Row 5: firstName and email are required"]
}
```

**Paginated list response:**
```json
{
  "content": [ { "id": 1, "firstName": "Jane", ... } ],
  "totalElements": 42,
  "totalPages": 5,
  "number": 0,
  "size": 10
}
```

---

## Error Responses

All errors follow a consistent format:

```json
{
  "timestamp": "2026-02-18T12:00:00",
  "status": 400,
  "error": "Student not found with id: 99"
}
```

| Status | Cause |
|--------|-------|
| `400` | Validation failure or business rule violation |
| `401` | Wrong credentials |
| `403` | Missing or invalid JWT token |

---

## Database Schema

Tables are auto-created by Hibernate on first startup (`ddl-auto: update`).

```sql
users    (id, name, email, password, role)
students (id, first_name, last_name, email, phone,
          student_class, section, enrollment_date,
          active, created_at, updated_at)
staff    (id, first_name, last_name, email, phone,
          department, position, salary, qualification,
          address, join_date, active, created_at, updated_at)
```

---

## Usage

1. Open `http://localhost:3000`
2. Click **Register** to create an account
3. Login with your credentials — you'll receive a JWT stored in `localStorage`
4. **Students Dashboard** — View stats cards (Total/Active/Inactive), Add, Edit, Delete, Search, and filter students
5. **Import/Export** — Import students from CSV or export the current list as CSV
6. **Bulk Actions** — Select multiple students to activate, deactivate, delete, or send email invitations
7. **Staff Management** — Manage staff records via the sidebar navigation
8. Logout via the button in the top-right header

---

## User Roles

| Role | Access |
|------|--------|
| `STAFF` | Default on register — full student & staff CRUD |
| `ADMIN` | Elevated role — assign manually in DB |

---

## Testing

### Backend Tests

```bash
cd backend

# Run all tests (unit + integration + Cucumber BDD)
mvn test

# Unit tests only (JUnit 5 + Mockito)
mvn test -Dtest="com.studentmgmt.service.StudentServiceTest"

# Integration tests only (@SpringBootTest + H2)
mvn test -Dtest="com.studentmgmt.integration.StudentApiIntegrationTest"

# Cucumber BDD scenarios only
mvn test -Dtest="com.studentmgmt.cucumber.CucumberIntegrationTest"
```

### Frontend E2E Tests

```bash
cd frontend

# Run Playwright tests (headless)
npm run test:e2e

# Run with Playwright UI
npm run test:e2e:ui
```

### Test Coverage

| Type | Tool | Coverage |
|------|------|----------|
| Unit | JUnit 5 + Mockito | StudentService (stats, CSV import/export, parsing) |
| Integration | @SpringBootTest + H2 | API endpoints, repository queries |
| BDD | Cucumber 7 + Gherkin | Student stats, CSV export, CSV import |
| E2E | Playwright | Dashboard stats cards, import/export UI |

### CSV Format

```csv
firstName,lastName,email,phone,class,section,enrollmentDate,active
Jane,Smith,jane@example.com,9876543210,10,A,2024-06-01,true
```
