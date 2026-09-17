---
description: Runs a structured series of database checks, stress simulations, and pricing pipeline audits to certify the hotel booking system for production readiness.
---

# Concurrency and Pricing Verification Workflow

This workflow guides the agent through an automated suite of checks to verify the system's resilience against overbooking, deadlock resistance, and billing precision before compiling and deploying the containerized Modular Monolith.

## Steps

### 1. Database Schema Integrity Check

Validate that the database schema is prepared for concurrency controls and sparse data storage:

* Query or inspect the PostgreSQL table definitions for `room_availability` to ensure the `version` column exists and is typed as an integer.
* Assert that the `room_slots` table has no pre-generated rows for future empty dates, confirming sparse storage compliance.
* Confirm that billing detail tables (`booking_details`, `invoice_details`, `service_order_details`) possess dedicated columns for historical snapshots (e.g., `unit_price`, `vat_rate`) to avoid dynamic joins.

### 2. Spring Transaction & Lock Audit

Audit the Java source code to ensure proper transaction scopes:

* Inspect `@Transactional` annotations on booking and POS services.
* Ensure all pessimistic lock queries use appropriate lock modes (`PESSIMISTIC_WRITE`) or explicit SQL row locks.
* Verify lock ordering matches the standard: lock leaf physical resources (`room_slots`) prior to locking root aggregate tables (`room_availability`).

### 3. Concurrency Stress Simulation

Run a simulated concurrent booking spike to verify lock safety:

* Execute the multi-threaded simulation script (located in `/workspace/scratch/test_concurrency.py`).
* Simulate 100 concurrent clients attempting to reserve the last remaining room of a specific type.
* Verify that exactly 1 booking succeeds and the remaining 99 are gracefully rejected with a 409 Conflict status.
* Check that no deadlock exceptions are recorded in PostgreSQL logs.

### 4. Pricing Calculation Breakdown Audit

Audit the calculation steps of the Pricing Engine:

* Provide a mock reservation payload containing a holiday stay, 3 adults (exceeding standard capacity of 2), and 2 selected F&B add-ons.
* Assert that the Price Breakdown output displays:
  1. Base Price
  2. Calendar Surcharge (+30%)
  3. Extra Adult Surcharge
  4. Service Fee (e.g., 5% of subtotal)
  5. Correct VAT split (8% on room charges, 10% on F&B)
* Verify the total amount matches the manually verified compound formula.

### 5. Docker Packaging & Readiness Report

Once verification tests pass successfully:

* Run Docker Compose validation checks (`docker compose config`) to ensure services are configured correctly.
* Verify container dependencies: check that backend APIs wait for database and cache services to pass healthchecks before starting up.
* Write a final Production Readiness Report summarizing the test results and logs.
