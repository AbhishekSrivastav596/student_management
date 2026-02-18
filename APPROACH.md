# Student & Staff Management System — Approach Document

## 1. Project Objective

Build a production-grade web application for managing student and staff records with role-based access control, secure authentication, and a responsive dashboard. The system targets institutional use where reliability, security, and maintainability are non-negotiable.

## 2. Core Features

| Domain | Features |
|--------|----------|
| **Authentication** | JWT login/logout, token refresh, password hashing (BCrypt) |
| **Authorization** | Role-based access control — ADMIN (full access), STAFF (limited write) |
| **Student Management** | CRUD operations, search, filter by class/status, paginated listing |
| **Staff Management** | CRUD operations, search, filter by department/role, paginated listing |
| **Dashboard** | Summary metrics (total students, staff, recent activity), role-aware views |
| **Audit** | Created/updated timestamps, soft deletes, created-by tracking |

## 3. High-Level Architecture

```
┌──────────────┐       HTTPS/JSON        ┌──────────────────┐
│  Next.js 14  │ ◄─────────────────────► │  Spring Boot 3   │
│  (Frontend)  │        REST API         │  (Backend API)   │
└──────────────┘                         └────────┬─────────┘
                                                  │ JPA/Hibernate
                                                  ▼
                                         ┌──────────────────┐
                                         │   PostgreSQL 16   │
                                         └──────────────────┘
```

- **Frontend** — Next.js 14 (App Router), TypeScript, Tailwind CSS, React Query
- **Backend** — Spring Boot 3, Java 17, Spring Security, Spring Data JPA
- **Database** — PostgreSQL with Flyway migrations
- **Infra** — Docker Compose for local dev; production-ready Dockerfiles

## 4. Backend Architectural Strategy

**Layered architecture with clear separation:**

```
Controller → Service → Repository → Entity
     ↕            ↕
   DTO          Domain Logic
```

- **Controllers** — Thin REST controllers; input validation via Jakarta Bean Validation
- **Services** — All business logic lives here; transactional boundaries
- **Repositories** — Spring Data JPA interfaces; custom queries via `@Query` or Specifications
- **DTOs** — Separate request/response DTOs; no entity exposure to API consumers
- **Exception handling** — Global `@RestControllerAdvice` with consistent error response format
- **Pagination** — Spring `Pageable` with configurable defaults; cursor-based option for large datasets

## 5. Frontend Architectural Strategy

```
app/
├── (auth)/           # Login, public routes
├── (dashboard)/      # Protected layout
│   ├── students/     # Student pages
│   ├── staff/        # Staff pages
│   └── page.tsx      # Dashboard home
├── components/       # Shared UI components
├── lib/              # API client, auth utils, constants
├── hooks/            # Custom hooks (useAuth, useStudents, etc.)
└── types/            # TypeScript interfaces
```

- **React Query** for server state — caching, background refetching, optimistic updates
- **Tailwind CSS** — utility-first; no custom CSS unless absolutely necessary
- **Route protection** — middleware-based auth check; redirect unauthenticated users
- **Forms** — React Hook Form + Zod validation schemas
- **Error boundaries** — graceful error UI at layout level

## 6. Security Approach

| Layer | Mechanism |
|-------|-----------|
| Authentication | JWT access token (short-lived, 15 min) + refresh token (HTTP-only cookie, 7 days) |
| Password storage | BCrypt with strength 12 |
| Authorization | Method-level `@PreAuthorize` + URL-pattern security in `SecurityFilterChain` |
| Input validation | Jakarta validation (backend) + Zod schemas (frontend) |
| CORS | Explicit allowed origins; no wildcards in production |
| Headers | Content-Security-Policy, X-Frame-Options, X-Content-Type-Options via Spring Security |
| Rate limiting | Bucket4j or Spring Gateway rate limiter on auth endpoints |
| SQL injection | Parameterized queries via JPA (no raw SQL concatenation) |

## 7. Database Design Strategy

**Core entities:**

```
users (id, email, password_hash, role, active, created_at, updated_at)
students (id, first_name, last_name, email, phone, class, section, enrollment_date, status, created_by, created_at, updated_at)
staff (id, first_name, last_name, email, phone, department, designation, join_date, status, created_by, created_at, updated_at)
refresh_tokens (id, user_id, token, expires_at, created_at)
```

- **Migrations** — Flyway; versioned SQL scripts; no auto-DDL in production
- **Indexing** — Indexes on email (unique), status, foreign keys, and common filter columns
- **Soft deletes** — `status` field (ACTIVE/INACTIVE) rather than physical deletion
- **Auditing** — `@CreatedDate`, `@LastModifiedDate` via Spring Data JPA auditing

## 8. Claude Skills (SpecKit Workflow)

This project uses **SpecKit** — a set of Claude slash commands that drive feature development from idea to implementation. Run them in sequence.

| Skill | Command | Purpose |
|-------|---------|---------|
| **Specify** | `/speckit.specify` | Generate a production-ready spec from a plain-English feature description |
| **Clarify** | `/speckit.clarify` | Ask up to 5 targeted questions to resolve spec ambiguities; writes answers back into the spec |
| **Plan** | `/speckit.plan` | Generate `plan.md` — tech stack, architecture, file structure |
| **Tasks** | `/speckit.tasks` | Break the plan into a dependency-ordered, parallelizable `tasks.md` organized by user story |
| **Analyze** | `/speckit.analyze` | Cross-artifact consistency check across `spec.md`, `plan.md`, `tasks.md` — read-only, flags gaps and constitution violations |
| **Checklist** | `/speckit.checklist` | Generate feature-specific quality checklists (UX, security, test) before implementation |
| **Implement** | `/speckit.implement` | Execute all tasks in `tasks.md` phase by phase; checks checklists, marks tasks `[X]` as complete |
| **Constitution** | `/speckit.constitution` | Enforce project governance — SOLID, security, API versioning, test coverage, UI/UX standards |
| **Tasks to Issues** | `/speckit.taskstoissues` | Convert `tasks.md` into GitHub Issues with labels, priority, and dependency links |

**Recommended workflow:**
```
/speckit.specify → /speckit.clarify → /speckit.plan → /speckit.tasks → /speckit.analyze → /speckit.checklist → /speckit.implement
```

**Constitution non-negotiables (enforced by `/speckit.analyze`):**
- Controller → Service → Repository layering — no business logic in controllers
- DTOs for all API responses — no raw entity exposure
- JWT auth + BCrypt passwords — non-negotiable
- API versioned at `/api/v1/` — breaking changes bump version
- Minimum 80% backend unit test coverage
- Pagination mandatory on all list endpoints

## 9. Claude Hooks

Hooks are shell commands Claude runs automatically in response to events. Configure in `.claude/settings.json`.

**Recommended hooks for this project:**

```json
{
  "hooks": {
    "PostToolUse": [
      {
        "matcher": "Edit|Write",
        "hooks": [
          {
            "type": "command",
            "command": "cd backend && mvn checkstyle:check -q 2>&1 | tail -5"
          }
        ]
      }
    ],
    "PreToolUse": [
      {
        "matcher": "Bash",
        "hooks": [
          {
            "type": "command",
            "command": "echo \"[Hook] Running bash in student-management\""
          }
        ]
      }
    ]
  }
}
```

**Hook event reference:**

| Event | Trigger | Recommended Use |
|-------|---------|-----------------|
| `PreToolUse` | Before any tool call | Safety guard, lint before edit |
| `PostToolUse` | After Edit/Write | Auto-format, checkstyle, compile check |
| `Stop` | End of session | Print `git diff --stat` summary |
| `Notification` | Claude waiting for input | Desktop notification |

**Practical hooks for this stack:**
- After any Java file edit → `mvn compile -q` to catch compile errors immediately
- After any test file edit → `mvn test -Dtest=ClassName -q`
- After any frontend file edit → `npm run lint --prefix frontend`
- On session stop → `git status` + `git diff --stat`

## 10. Testing Strategy

### Test Levels

| Level | Tool | Scope |
|-------|------|-------|
| Unit (backend) | JUnit 5 + Mockito | Services, JwtUtil, utility classes |
| Integration (backend) | `@SpringBootTest` + Testcontainers | Repository queries, full controller stack |
| Unit (frontend) | Vitest + React Testing Library | Components, hooks, form validation |
| E2E | Playwright | Login, CRUD flow, pagination |

- CI runs all tests on every PR
- Integration tests use Testcontainers — no shared test database

### Test Cases — Auth (`AuthController`)

| # | Endpoint | Scenario | Expected |
|---|----------|----------|----------|
| A1 | `POST /api/auth/register` | Valid name, email, password | `200 OK` + JWT token |
| A2 | `POST /api/auth/register` | Duplicate email | `400` — `"Email already registered"` |
| A3 | `POST /api/auth/register` | Blank email field | `400` — validation error |
| A4 | `POST /api/auth/login` | Correct credentials | `200 OK` + token + user info |
| A5 | `POST /api/auth/login` | Wrong password | `401 UNAUTHORIZED` |
| A6 | `POST /api/auth/login` | Non-existent email | `401 UNAUTHORIZED` |
| A7 | `GET /api/students` | No `Authorization` header | `403 FORBIDDEN` |
| A8 | `GET /api/students` | Expired / malformed JWT | `403 FORBIDDEN` |

### Test Cases — Student CRUD (`StudentController`)

| # | Endpoint | Scenario | Expected |
|---|----------|----------|----------|
| S1 | `GET /api/students` | Authenticated, no data | `200 OK`, `content: []`, `totalElements: 0` |
| S2 | `GET /api/students?search=john` | Search by first name | Returns only matching students |
| S3 | `GET /api/students?page=0&size=5` | Pagination params | `content.length ≤ 5`, correct `totalPages` |
| S4 | `POST /api/students` | Valid student body | `200 OK` with `id` populated |
| S5 | `POST /api/students` | Duplicate email | `400` with error message |
| S6 | `POST /api/students` | Missing `firstName` | `400` — `"First name is required"` |
| S7 | `POST /api/students` | Invalid email format | `400` — `"Invalid email format"` |
| S8 | `GET /api/students/{id}` | Valid ID | `200 OK` with full student data |
| S9 | `GET /api/students/{id}` | Non-existent ID | `400` — `"Student not found with id: X"` |
| S10 | `PUT /api/students/{id}` | Valid update body | `200 OK` with updated fields reflected |
| S11 | `PUT /api/students/{id}` | Non-existent ID | `400` — `"Student not found with id: X"` |
| S12 | `DELETE /api/students/{id}` | Valid existing ID | `204 NO CONTENT` |
| S13 | `DELETE /api/students/{id}` | Non-existent ID | `400` — `"Student not found with id: X"` |

### Service Unit Tests (`StudentServiceTest`)

```java
@Test void createStudent_validInput_savesAndReturnsDto()
@Test void createStudent_duplicateEmail_throwsRuntimeException()
@Test void getById_existingId_returnsDto()
@Test void getById_missingId_throwsRuntimeException()
@Test void getAll_withSearchTerm_callsSearchRepository()
@Test void getAll_noSearchTerm_callsFindAll()
@Test void update_existingStudent_updatesAllMappedFields()
@Test void delete_nonExistentId_throwsRuntimeException()
```

### Frontend Test Cases

| # | Component | Test Scenario |
|---|-----------|---------------|
| F1 | `LoginPage` | Shows error message on failed login |
| F2 | `LoginPage` | Redirects to `/dashboard` on successful login |
| F3 | `LoginPage` | Toggles between login and register mode |
| F4 | `DashboardPage` | Renders student rows from mocked API response |
| F5 | `DashboardPage` | Opens add-student dialog on button click |
| F6 | `DashboardPage` | Calls delete API and invalidates query on confirm |
| F7 | `DashboardLayout` | Redirects to `/login` when no token in localStorage |
| F8 | `api.ts` | Axios interceptor attaches `Authorization: Bearer <token>` |
| F9 | `api.ts` | Axios interceptor redirects to `/login` on `401` response |

## 11. Deployment Strategy

```yaml
# docker-compose.yml structure
services:
  backend:    # Spring Boot JAR, multi-stage Dockerfile
  frontend:   # Next.js standalone build
  db:         # PostgreSQL 16 with named volume
  nginx:      # Reverse proxy (optional, for single-domain setup)
```

- **Multi-stage Docker builds** — separate build and runtime stages for minimal image size
- **Environment configuration** — `.env` files for local; environment variables for production
- **Database migrations** — Flyway runs on application startup
- **Health checks** — Spring Actuator `/health` endpoint; Docker `HEALTHCHECK` directives

## 12. Production Readiness Checklist

- [ ] All environment secrets externalized (no hardcoded credentials)
- [ ] Flyway migrations tested against clean database
- [ ] JWT secret rotation strategy documented
- [ ] CORS restricted to known origins
- [ ] Rate limiting active on authentication endpoints
- [ ] Logging structured (JSON format) with correlation IDs
- [ ] Spring Actuator endpoints secured (not publicly accessible)
- [ ] Docker images scanned for vulnerabilities
- [ ] Database connection pooling configured (HikariCP defaults reviewed)
- [ ] Error responses sanitized (no stack traces in production)
- [ ] Frontend environment variables validated at build time
- [ ] Backup strategy for PostgreSQL defined
- [ ] CI/CD pipeline runs full test suite before deploy

---

**Version**: 1.1 | **Date**: 2026-02-18
