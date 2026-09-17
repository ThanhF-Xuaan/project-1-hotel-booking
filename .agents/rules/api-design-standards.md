---
trigger: model_decision
description: Automatically activated when the agent writes or modifies Spring Boot REST controllers, DTOs, service classes, repositories, or any backend API endpoint in the hotel booking system.
---

# REST API Design Standards — Hotel Booking System (Spring Boot)

## 1. URL & HTTP Method Conventions

- Use **plural nouns** for resource collections: `/api/hotels`, `/api/bookings`, `/api/room-types`
- Use **kebab-case** for multi-word path segments: `/api/room-types`, `/api/booking-details`
- Nest resources to express ownership: `/api/hotels/{hotelId}/room-types/{typeId}/rooms`
- **Never** use verbs in URIs (wrong: `/api/getHotel`, correct: `GET /api/hotels/{id}`)
- HTTP verb mapping:
  - `GET` — Read only, never modifies state
  - `POST` — Create new resource
  - `PUT` — Full replacement update
  - `PATCH` — Partial update
  - `DELETE` — Delete resource

## 2. Controller Layer Rules

- All controllers must be annotated with `@RestController` and `@RequestMapping("/api/...")`
- Each controller maps to exactly **one** domain aggregate (e.g., `BookingController` only handles booking operations)
- Controllers must **never** contain business logic — delegate everything to `@Service` beans
- Use `ResponseEntity<T>` with explicit HTTP status codes:
  - `200 OK` for successful reads
  - `201 Created` for successful POST with `Location` header pointing to new resource
  - `204 No Content` for successful DELETE
  - `400 Bad Request` for validation failures
  - `404 Not Found` for missing resources
  - `409 Conflict` for optimistic locking failures or overbooking

## 3. DTO Rules (Data Transfer Objects)

- **Never expose JPA Entity objects directly** in controller responses — always use dedicated DTOs
- Use **record** classes or Lombok `@Value` for immutable request/response DTOs
- Separate Request and Response DTOs (e.g., `BookingCreateRequest`, `BookingResponse`)
- Use **MapStruct** `@Mapper` interfaces for Entity ↔ DTO conversion — never write manual mapping in services
- Apply Jakarta Validation annotations on request DTOs (`@NotNull`, `@NotBlank`, `@Min`, `@Max`, `@Valid`)

## 4. Exception Handling

- Use a **global `@RestControllerAdvice`** class to handle all exceptions centrally
- Standard error response body format:
  ```json
  {
    "timestamp": "ISO-8601",
    "status": 400,
    "error": "Bad Request",
    "message": "Validation failed for field 'checkOutDate'",
    "path": "/api/bookings"
  }
  ```
- Map business exceptions consistently:
  - `ResourceNotFoundException` → `404`
  - `ValidationException` → `400`
  - `OptimisticLockingFailureException` → `409`
  - `RoomNotAvailableException` → `409`
  - `UnauthorizedException` → `403`

## 5. Pagination & Filtering

- All list endpoints must support pagination via Spring's `Pageable` (params: `page`, `size`, `sort`)
- Response wraps in `Page<T>` or a custom `PageResponse<T>` DTO exposing `content`, `totalElements`, `totalPages`, `currentPage`
- Filtering parameters are passed as query params — **never** in the request body for GET requests
- Default page size: **20**; maximum page size: **100** (enforce with `@PageableDefault`)

## 6. Security Rules

- All endpoints require JWT Bearer token unless explicitly marked public via `@PermitAll` / security config
- Use `@PreAuthorize` annotations on service methods for role-based access (`ROLE_ADMIN`, `ROLE_RECEPTIONIST`, `ROLE_GUEST`)
- Extract authenticated user from `SecurityContextHolder` — never trust client-supplied user ID in request body
- Never log full JWT tokens or passwords

## 7. API Documentation

- All public controller methods must have `@Operation(summary = "...")` and `@ApiResponse` annotations from SpringDoc OpenAPI 3
- Request body schemas must have `@Schema(description = "...")` on fields
- Group endpoints using `@Tag(name = "Booking Management")` on the controller class
