---
description: A structured debugging and bug-fixing process for the hotel booking system. Use this workflow when investigating unexpected behavior, runtime errors, API failures, data inconsistencies, or frontend rendering issues.
---

# Hotel Debug & Fix Workflow

This workflow provides a systematic approach to diagnose and resolve bugs across the hotel booking system stack (Spring Boot backend, React frontend, PostgreSQL, Redis, Keycloak, Docker).

## Steps

### 1. Reproduce & Characterize the Bug

Before touching any code:

* Reproduce the bug with **exact steps** — identify the minimum reproducible case
* Determine the **bug category**:
  - **API Error**: HTTP 4xx/5xx from backend
  - **Data Bug**: Wrong data stored or returned (pricing, availability, booking state)
  - **Concurrency Bug**: Race condition, double-booking, phantom reads
  - **Auth Bug**: 401/403 errors, wrong permissions applied
  - **UI Bug**: Component rendering incorrectly, form validation mismatch
  - **Infrastructure Bug**: Container crash, port conflict, DB connection failure
* Record: what was expected vs. what actually happened

### 2. Locate the Error Source

#### For Backend Errors (API returns 4xx/5xx):

```powershell
# View backend logs
docker compose logs -f backend

# Or if running locally:
# Check the Spring Boot console output
```

* Look for the **full stack trace** — the root cause is at the bottom of the `Caused by:` chain
* Note the exact class, method, and line number
* Check if it's a `NullPointerException`, `DataIntegrityViolationException`, `OptimisticLockingFailureException`, etc.

#### For Database Errors:

```powershell
# View PostgreSQL logs
docker compose logs -f postgres_db

# Connect to the database directly
docker exec -it hotel_postgres_db psql -U postgres -d hotel_booking_db
```

Key SQL to check:
```sql
-- Check table schema
\d room_availability

-- Check current data state
SELECT * FROM bookings WHERE id = <booking_id>;
SELECT * FROM room_slots WHERE room_instance_id = <id> AND stay_date BETWEEN '2025-01-01' AND '2025-01-05';
SELECT * FROM room_availability WHERE room_type_id = <id> AND availability_date = '2025-01-03';

-- Check active locks (deadlock investigation)
SELECT pid, query, state, wait_event_type, wait_event FROM pg_stat_activity WHERE state != 'idle';
SELECT * FROM pg_locks WHERE NOT granted;
```

#### For Redis Errors:

```powershell
# Check Redis logs
docker compose logs -f redis

# Connect to Redis CLI
docker exec -it hotel_redis redis-cli

# Useful Redis commands
KEYS hotel:lock:*         # Check active room locks
TTL hotel:lock:<key>      # Check remaining lock TTL
GET hotel:lock:<key>      # Check lock value
```

#### For Frontend Errors:

* Open Browser DevTools → **Network** tab: inspect failed requests (status code, response body)
* Open Browser DevTools → **Console** tab: look for JavaScript errors and stack traces
* Verify the request URL and payload match what the backend expects
* Check `Authorization` header is present and the token is not expired

### 3. Form a Hypothesis

Based on logs, identify the most probable root cause:

| Symptom | Likely Cause |
|---|---|
| `OptimisticLockingFailureException` | Two concurrent requests updated same row simultaneously — expected behavior for online booking |
| `DataIntegrityViolationException` | Constraint violation (unique, FK, check constraint) — review insert/update data |
| `AccessDeniedException` | JWT token missing, expired, or role mismatch — check Keycloak token claims |
| Double-booked room | Missing or incorrect locking strategy — review `hotel-concurrency-and-pricing` rule |
| Wrong price calculated | Pricing engine query error or missing rule — inspect `pricing_rules` table data |
| Redis lock not releasing | TTL not set, or Redis expired-key listener not configured — check `RedisConfig` |
| Container won't start | Port conflict, missing env var, or DB not healthy yet — check `docker compose ps` |

### 4. Read Relevant Code

**Do not guess** — read the actual source code before making changes:

* Read the **Service** method that handles the failing operation
* Read the **Repository** query that fetches/writes the relevant data
* Read the **Controller** to confirm the request mapping and validation
* Read the **Entity** to check if `@Version`, `@Lock`, or `@Transactional` are present

### 5. Write a Failing Test (if possible)

If the bug is in the service/repository layer, write a test that **currently fails** and **will pass** after the fix:

```java
@Test
void shouldRejectDoubleBookingForSameRoom() {
    // Given: 1 room remaining
    // When: 2 concurrent booking attempts
    // Then: exactly 1 succeeds, 1 receives 409
}
```

This confirms you've understood the bug and will verify the fix.

### 6. Implement the Minimal Fix

* Fix **only** what is necessary — do not refactor unrelated code
* For concurrency bugs: verify fix against the locking matrix in `hotel-concurrency-and-pricing` rule
* For pricing bugs: verify the formula: `Total = (Base + Surcharges - Discounts) × (1 + Service Fee%) × (1 + VAT%)`
* For data bugs: check if a database migration script needs to fix corrupted data

### 7. Verify the Fix

```powershell
# Backend: run all tests
cd backend
.\mvnw.cmd test

# Frontend: TypeScript check
cd frontend
npm run build

# Full system: restart and test
docker compose down
docker compose up -d --build
```

* Confirm the previously failing scenario now works correctly
* Confirm no regression in related scenarios

### 8. Concurrency Bug Special Checklist

If the bug involved race conditions or double-booking:

- [ ] Verified the correct locking mechanism is applied (see `hotel-concurrency-and-pricing` rule)
- [ ] Lock ordering is correct (room_slots before room_availability for double pessimistic)
- [ ] Redis TTL is set for temporary session locks
- [ ] `OptimisticLockingFailureException` is caught and translated to a proper HTTP 409 response
- [ ] Stress-tested with the `pricing-concurrency-verify` skill (concurrent thread simulation)

### 9. Document the Root Cause

After fixing, briefly document:
* **Root Cause**: what was wrong and why
* **Fix Applied**: what change was made and why it solves the problem
* **Prevention**: any guard or test added to prevent recurrence

---

## Quick Reference: Common Error Patterns

### Spring Boot 500 on booking creation
→ Check `@Transactional` on the service method; check lock ordering if pessimistic locking is used

### Frontend shows stale availability data
→ Verify React state is updated after booking; check if cache invalidation is triggered

### Keycloak 401 on valid-looking requests
→ Check token expiry; verify the realm and client ID in `application.yml`; check CORS config

### `docker compose up` fails: port already in use
→ Check what's using ports 3000, 5433, 6379, 8080, 8081 using `netstat -an | findstr :<port>`

### DB init scripts not running
→ The init scripts only run on first volume creation; run `docker compose down -v` to reset
