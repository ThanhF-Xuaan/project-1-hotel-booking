---
name: hotel-api-development
description: Step-by-step guide for building a new Spring Boot REST API feature in the hotel booking system (Java 21 + Spring Boot + JPA + MapStruct + SpringDoc). Use this skill when creating controllers, services, repositories, entities, or DTOs for any backend module.
---

# Hotel API Development Skill

This skill provides structured guidance for adding a new backend feature or API endpoint to the hotel booking system's Spring Boot Modular Monolith.

## Project Package Structure

The backend follows a layered Modular Monolith structure under `src/main/java/`:

```
com.hotel.booking/
├── common/          # Shared utilities, base classes, global exception handlers
│   ├── exception/   # Custom exceptions + GlobalExceptionHandler (@RestControllerAdvice)
│   ├── response/    # Standard ApiResponse, PageResponse wrappers
│   └── util/        # DateUtil, SlugUtil, etc.
├── iam/             # Identity & Access Management (Keycloak integration)
├── inventory/       # Hotel, RoomType, RoomInstance management
├── availability/    # Room availability matrix & calendar
├── pricing/         # Dynamic pricing engine (PricingRule, PricingPlan, Calculator)
├── booking/         # Booking lifecycle, locking, payment integration
├── pos/             # Walk-in booking & Point-of-Sale terminal
├── crm/             # Guest profile, preferences, history
└── analytics/       # Occupancy Rate reports and charts
```

**Rule**: Every new feature lives in its owning module package. Do **not** create cross-module direct class dependencies — use service interfaces.

## Step-by-Step: Building a New API Feature

### Step 1: Define the Entity (if needed)

1. Create the entity class in `<module>/entity/`
2. Annotate with `@Entity`, `@Table(name = "...")`, `@Id`, `@GeneratedValue`
3. Add `@Version Long version` if the entity needs **optimistic locking** (see `hotel-concurrency-and-pricing.md` rule for which tables require it)
4. Extend `BaseEntity` for shared audit fields (`createdAt`, `updatedAt`, `createdBy`)
5. Use `@Enumerated(EnumType.STRING)` for all enum columns

```java
@Entity
@Table(name = "room_types")
public class RoomType extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RoomStatus status;
    // ...
}
```

### Step 2: Create the Repository

1. Create interface in `<module>/repository/` extending `JpaRepository<Entity, Long>`
2. Add custom queries using `@Query` (JPQL preferred; native SQL only when JPQL cannot express it)
3. For pessimistic locking, add `@Lock(LockModeType.PESSIMISTIC_WRITE)` on specific query methods
4. For availability queries spanning date ranges, use native PostgreSQL queries with window functions

### Step 3: Define DTOs

1. Create Request DTO in `<module>/dto/request/` — use Lombok `@Data` or Java records
2. Create Response DTO in `<module>/dto/response/`
3. Add `@NotNull`/`@NotBlank`/`@Valid` constraints on request DTOs
4. Document fields with `@Schema(description = "...")`

### Step 4: Create MapStruct Mapper

1. Create interface in `<module>/mapper/` annotated with `@Mapper(componentModel = "spring")`
2. Define methods: `toResponse(Entity entity)`, `toEntity(CreateRequest req)`
3. Use `@Mapping(target = "...", source = "...")` for field name mismatches
4. For list mappings: `List<Response> toResponseList(List<Entity> entities)`

### Step 5: Implement the Service

1. Create interface `<module>/service/<Name>Service.java`
2. Create implementation `<module>/service/impl/<Name>ServiceImpl.java`
3. Annotate with `@Service`, `@RequiredArgsConstructor`
4. All write methods must be `@Transactional`; read-only methods `@Transactional(readOnly = true)`
5. Throw domain-specific exceptions — never return `null` or catch silently

```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepo;
    private final BookingMapper mapper;

    @Override
    @Transactional  // write operation overrides class-level readOnly
    public BookingResponse createBooking(BookingCreateRequest request) {
        // validate → lock → create → save → return DTO
    }
}
```

### Step 6: Build the Controller

1. Create in `<module>/controller/`
2. Annotate: `@RestController`, `@RequestMapping("/api/...")`, `@Tag(name = "...")`
3. Each method: `@Operation(summary = "...")`, `@ApiResponse`s, proper `ResponseEntity` return type
4. Inject only the Service interface, never the repository directly

### Step 7: Register Security Rules

Update `SecurityConfig` to:
- Open public endpoints with `.permitAll()`
- Restrict admin endpoints with `.hasRole("ADMIN")`
- Add `@PreAuthorize` on service methods for fine-grained control

### Step 8: Write Tests

1. **Unit Test**: Test service logic with mocked dependencies (`@ExtendWith(MockitoExtension.class)`)
2. **Integration Test**: Test the full request/response cycle with `@SpringBootTest` + `MockMvc`
3. Assert HTTP status codes, response body fields, and database state changes

## Common Pitfalls

- **Never** call another module's repository directly — go through that module's service
- **Never** return entity objects from controllers — always map to DTOs
- **Never** do business logic in controllers or repositories
- Optimistic locking: catch `OptimisticLockingFailureException` in the service, translate to `RoomNotAvailableException` (→ HTTP 409)
- Pricing calculations: always delegate to `PricingEngineService` — never duplicate pricing logic in booking service
