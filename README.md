# Student Management System

A full-stack web application for managing student records with JWT-based authentication, built with Spring Boot and Next.js.

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Backend | Spring Boot 3, Java 17, Spring Security |
| Database | PostgreSQL 16 |
| Auth | JWT (jjwt 0.12.5) + BCrypt |
| Frontend | Next.js 14, TypeScript, Tailwind CSS |
| UI Components | shadcn/ui (Radix UI) |
| State | React Query (TanStack Query v5) |
| Deployment | Docker + Docker Compose |

---

## Project Structure

```
student_management/
├── backend/                          # Spring Boot API
│   ├── src/main/java/com/studentmgmt/
│   │   ├── config/                   # JWT filter, security config
│   │   ├── controller/               # AuthController, StudentController
│   │   ├── dto/                      # Request/response DTOs
│   │   ├── entity/                   # Student, User JPA entities
│   │   ├── exception/                # GlobalExceptionHandler
│   │   ├── repository/               # Spring Data JPA repositories
│   │   └── service/                  # AuthService, StudentService
│   ├── src/main/resources/
│   │   └── application.yml
│   └── pom.xml
├── frontend/                         # Next.js app
│   └── src/
│       ├── app/
│       │   ├── login/page.tsx        # Login + Register page
│       │   └── dashboard/page.tsx    # Student CRUD dashboard
│       ├── components/ui/            # shadcn components
│       ├── context/auth-context.tsx  # Auth state + token management
│       └── lib/api.ts                # Axios client + API functions
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
| `GET` | `/api/students` | Yes | List all students (paginated) |
| `GET` | `/api/students?search=john&page=0&size=10` | Yes | Search + paginate |
| `GET` | `/api/students/{id}` | Yes | Get student by ID |
| `POST` | `/api/students` | Yes | Create a student |
| `PUT` | `/api/students/{id}` | Yes | Update a student |
| `DELETE` | `/api/students/{id}` | Yes | Delete a student |

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
users (id, name, email, password, role)
students (id, first_name, last_name, email, phone,
          student_class, section, enrollment_date,
          created_at, updated_at)
```

---

## Usage

1. Open `http://localhost:3000`
2. Click **Register** to create an account
3. Login with your credentials — you'll receive a JWT stored in `localStorage`
4. Use the dashboard to **Add**, **Edit**, **Delete**, and **Search** students
5. Logout via the button in the top-right header

---

## User Roles

| Role | Access |
|------|--------|
| `STAFF` | Default on register — full student CRUD |
| `ADMIN` | Elevated role — assign manually in DB |
