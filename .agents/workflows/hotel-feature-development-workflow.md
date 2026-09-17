---
description: A structured end-to-end process for building a new feature (API endpoint + frontend page) in the hotel booking system. Use this workflow when implementing any new functionality across the full stack (Spring Boot backend + React frontend + PostgreSQL).
---

# Hotel Feature Development Workflow

This workflow ensures every new feature is developed consistently, tested correctly, and integrated cleanly across all system layers — from database to UI.

## Steps

### 1. Understand & Plan the Feature

Before writing any code, fully understand the requirements:

* Identify which **module** the feature belongs to (e.g., `booking`, `inventory`, `pricing`, `analytics`)
* List all **API endpoints** required (HTTP method, URL, request/response shape)
* Identify **database changes** needed (new tables, new columns, new indexes)
* Determine **access control** requirements (which roles can access each endpoint: `ROLE_ADMIN`, `ROLE_RECEPTIONIST`, `ROLE_GUEST`)
* Check if the feature touches **concurrency-sensitive resources** (room slots, inventory) — if yes, read the `hotel-concurrency-and-pricing` rule before proceeding

### 2. Database Layer (if schema changes are needed)

Read the `hotel-db-schema-design` skill, then:

* Design new table(s) following sparse data and financial immutability principles
* Add the DDL to the appropriate script in `./database/` (maintain script ordering)
* Add required indexes (all foreign keys, frequently filtered columns)
* Add `updated_at` trigger for new tables
* Verify the script is **idempotent** (uses `IF NOT EXISTS`)
* Test locally: `docker compose down -v && docker compose up -d postgres_db` and confirm scripts execute without errors

### 3. Backend: Entity & Repository

Read the `hotel-api-development` skill, then:

* Create the JPA Entity class in `<module>/entity/` with proper annotations
* Add `@Version` column if entity is subject to concurrent writes
* Create the Spring Data JPA Repository interface in `<module>/repository/`
* Add custom queries for business-specific lookups
* Write a **unit test** for any custom JPQL/native query using `@DataJpaTest`

### 4. Backend: Service Layer

* Create the Service interface and implementation in `<module>/service/`
* Implement business logic with proper `@Transactional` boundaries
* Handle locking per the `hotel-concurrency-and-pricing` rule:
  - Online booking → Optimistic Lock on `room_availability`
  - Walk-in booking → Double Pessimistic Lock (room_slots first, then room_availability)
  - Payment window → Redis TTL lock
* Write **unit tests** for the service using Mockito mocks
* Test edge cases: resource not found, concurrent access, invalid state transitions

### 5. Backend: DTOs, Mapper, Controller

* Define Request and Response DTOs with validation annotations
* Create MapStruct Mapper interface
* Build the Controller with proper HTTP status codes and SpringDoc annotations
* Verify all endpoints are secured with appropriate roles
* Manually test with Swagger UI at `http://localhost:8080/swagger-ui.html`

### 6. Backend Build Verification

```powershell
# In ./backend directory
.\mvnw.cmd test
.\mvnw.cmd spring-boot:run
```

* Confirm all tests pass
* Confirm Spring Boot starts without errors
* Confirm new endpoints appear in Swagger UI with correct schemas

### 7. Frontend: Types & Service

Read the `hotel-frontend-feature` skill, then:

* Define TypeScript interfaces in `src/types/<domain>.ts` matching the backend response DTOs
* Create API service functions in `src/services/<domain>Service.ts` using the configured axios instance
* Ensure date fields use `YYYY-MM-DD` string format

### 8. Frontend: Custom Hook & Components

* Create a custom hook in `src/hooks/use<Feature>.ts` to encapsulate data fetching and state
* Build reusable components in `src/components/<domain>/`
* Implement loading state (skeleton/spinner) and error state (error message + retry button)
* Apply proper form validation with `react-hook-form` + `zod` for any user input

### 9. Frontend: Page & Routing

* Create the page component in `src/pages/<domain>/`
* Add lazy-loaded route to `src/router/index.tsx`
* Wrap admin pages in `<ProtectedRoute>` with correct role checks
* Test navigation from the relevant entry points (menu, buttons)

### 10. Frontend Build Verification

```powershell
# In ./frontend directory
npm run build
```

* Confirm TypeScript compilation succeeds with no type errors
* Confirm the build completes without warnings about missing imports or unused variables
* Test the feature end-to-end in the browser at `http://localhost:5173`

### 11. End-to-End Integration Test

With all services running (`docker compose up -d`):

* Walk through the complete user flow for the new feature
* Verify data persistence: create data → refresh page → confirm data is still there
* Verify access control: attempt to access restricted endpoints without the required role
* Verify error handling: submit invalid data → confirm user-friendly error messages appear

### 12. Docker Build Verification

```powershell
# From project root
docker compose up -d --build
```

* Confirm all 5 containers start healthy
* Confirm `docker compose ps` shows all services as `healthy`
* Confirm the new feature works in the Docker environment (not just local dev)

### 13. Completion Checklist

- [ ] Database migration script is idempotent and tested
- [ ] Unit tests written for service layer
- [ ] All API endpoints documented in Swagger
- [ ] Frontend TypeScript compiles without errors
- [ ] Feature tested end-to-end in Docker environment
- [ ] No hardcoded credentials or secrets introduced
- [ ] Code follows rules: `api-design-standards`, `frontend-react-standards`, `hotel-concurrency-and-pricing`
