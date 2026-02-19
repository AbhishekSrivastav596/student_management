# Student Management System - Development Guidelines

## Tech Stack

### Frontend (Next.js)
- **Framework**: Next.js 14.1.4 (App Router) + React 18 + TypeScript 5.4
- **Styling**: Tailwind CSS 3.4 (class-based dark mode, CSS variable theming)
- **UI Components**: Shadcn/ui pattern (Radix UI primitives in `components/ui/`)
- **Forms**: React Hook Form + Zod validation
- **State**: TanStack React Query (server state), React Context (auth)
- **HTTP**: Axios with JWT interceptor
- **Package Manager**: npm

### Backend (Spring Boot)
- **Framework**: Spring Boot 3.2.4 + Java 17
- **Build**: Maven 3.9
- **Database**: PostgreSQL 16 (prod), H2 in-memory (test, PostgreSQL mode)
- **ORM**: Spring Data JPA + Hibernate
- **Security**: Spring Security + JWT (JJWT 0.12.5)
- **Testing**: Cucumber 7.15 + JUnit Platform

## Project Structure

```
frontend/
  src/
    app/            # Next.js App Router pages (layout.tsx, page.tsx)
    components/ui/  # Shadcn-style reusable UI components
    context/        # React Context providers (auth-context.tsx)
    lib/            # Utilities (api.ts, utils.ts)
  tests/e2e/        # Playwright E2E tests

backend/
  src/main/java/com/studentmgmt/
    entity/         # JPA entities (User, Student)
    dto/            # Request/Response DTOs
    repository/     # Spring Data JPA repositories
    service/        # Business logic
    controller/     # REST controllers
    config/         # Security, JWT config
    exception/      # Global exception handler
  src/test/
    java/.../cucumber/  # Cucumber step definitions
    resources/features/ # .feature files (BDD)
```

## Commands

### Frontend
```bash
cd frontend
npm run dev          # Dev server on :3000
npm run build        # Production build
npm run lint         # Next.js linting
npm run test:e2e     # Playwright E2E tests
npm run test:e2e:ui  # Playwright with UI
```

### Backend
```bash
cd backend
mvn clean package              # Build JAR
mvn spring-boot:run            # Run dev server on :8080
mvn test                       # Run Cucumber/JUnit tests
mvn package -DskipTests        # Build without tests
```

### Docker
```bash
docker-compose up              # Start all services (PostgreSQL, Backend, Frontend)
```

## API

- **Base URL**: `http://localhost:8080/api`
- **Auth**: `POST /api/auth/register`, `POST /api/auth/login` (returns JWT)
- **Students**: CRUD at `/students` (paginated + search), `/students/{id}`
- **Security**: JWT Bearer token in `Authorization` header

## Code Conventions

### Frontend
- Functional components with hooks
- Path alias: `@/*` maps to `src/*`
- File naming: kebab-case for routes, PascalCase for components
- API calls grouped in `lib/api.ts` as objects (`authApi`, `studentApi`)
- Class merging utility: `cn()` from `lib/utils.ts`

### Backend
- Package: `com.studentmgmt.*`
- Lombok for boilerplate (`@Data`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor`)
- Naming: `*Controller`, `*Service`, `*Repository`, `*Dto`, `*Request`, `*Response`
- Entity classes: plain names (`Student`, `User`)
- Jakarta validation annotations on DTOs
- Global exception handling via `@ControllerAdvice`

## Database

- **Production**: PostgreSQL on `localhost:5432/studentdb` (DDL: `update`)
- **Testing**: H2 in-memory with `create-drop` DDL, PostgreSQL compatibility mode
- **Credentials**: Set in `application.yml` / `docker-compose.yml`

## Testing Strategy

- **E2E**: Playwright (Chromium) - login flows, registration, error scenarios
- **BDD**: Cucumber feature files - authentication scenarios with Spring context
- **Test DB**: H2 in-memory, config in `application-test.yml`
