---
name: hotel-pricing-concurrency-verify
description: Guides the agent in writing concurrency stress-tests and validating pricing engine calculations for the Smart Hotel Booking system. Use this skill when testing locking behavior, simulating double-bookings, or verifying pricing pipelines.
---

# Pricing and Concurrency Verification Skill

This skill provides step-by-step guidance, decision trees, and testing patterns to help the agent verify that the system handles high-concurrency race conditions successfully and calculates multi-layered prices accurately.

## When to Use This Skill

- Use this when writing integration tests or unit tests for the `booking` and `pricing` modules.
- Use this when verifying if database transaction isolation levels and row-level locks function correctly.
- Use this when testing high-load scenarios (e.g., thousands of clients checking out simultaneously).
- Use this when validating tax, fee, and surcharge breakdowns within the Pricing Pipeline.

## Verification Checklist

### 1. Concurrency Testing Checklist

- [ ] **Transaction Isolation:** Ensure transactional methods in Spring Boot are annotated with `@Transactional(isolation = Isolation.READ_COMMITTED)`.
- [ ] **Locking Verification:** Assert that database row queries for pessimistic writes use JPA `@Lock(LockModeType.PESSIMISTIC_WRITE)` or append `FOR UPDATE` in native queries.
- [ ] **Race Condition Simulation:** Write a test using `CountDownLatch` or `ExecutorService` in JUnit to spawn 50 concurrent threads attempting to book the single remaining room instance.
- [ ] **Optimistic Lock Recovery:** Verify that the application catches `OptimisticLockingFailureException` and rolls back successfully, returning a standard, user-friendly 409 Conflict error.
- [ ] **Lock Expiration (Redis TTL):** Verify that if a Redis session expires, a Redis expired-key listener safely decrements `locked_rooms` in PostgreSQL.

### 2. Pricing Pipeline Validation Checklist

- [ ] **Base Price Resolution:** Confirm base prices match room types.
- [ ] **Calendar Modifier Accuracy:** Check that holidays add the exact percentage multiplier (e.g., +30%).
- [ ] **Surcharge Calculations:** Verify extra guest surcharges are computed based on age policies (e.g., child vs. adult limits).
- [ ] **Tax & Service Fee Compound Order:** Assert that the final total formula is calculated as: `Total = (Base + Surcharges - Discounts) * (1 + Service Fee %) * (1 + VAT %)`. Ensure VAT is calculated on top of service fees, and separate taxes for F&B (e.g., 10%) are handled cleanly.

## Decision Tree: Choosing the Right Locking Strategy

```
                          Is the resource aggregate or physical?
                                      /              \
                                     /                \
                       [Aggregate Inventory]     [Physical Room Slot]
                                  /                      \
             How is it booked?                            Is it booked at front desk (Walk-in)?
              /             \                                    /               \
         (Online)         (Walk-in)                            (Yes)             (No)
            /                 \                                 /                 \
  [Optimistic Lock]   [Double Pessimistic Lock]     [Double Pessimistic]   [Single Pessimistic Lock]
```

## How to Run Verification Scripts

When executing verification scripts, use Python's built-in threading and database client wrappers (`psycopg2`, `redis`) located in `/workspace/scratch/` or target JUnit tests using `mvn test`.
