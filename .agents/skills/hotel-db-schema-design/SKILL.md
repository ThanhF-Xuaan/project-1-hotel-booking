---
name: hotel-db-schema-design
description: Guidance for designing and modifying PostgreSQL database schemas, writing migration SQL scripts, and maintaining data integrity for the hotel booking system. Use this skill when designing new tables, adding columns, writing SQL functions/triggers, or creating indexes.
---

# Database Schema Design Skill — Hotel Booking System (PostgreSQL 16)

This skill guides the design of database changes for the hotel booking system. It enforces the architectural decisions around sparse data storage, financial immutability, and concurrency control.

## Schema Overview

The database is initialized by 8 ordered SQL scripts in `./database/`:

| Script | Purpose |
|---|---|
| `01-init-schema.sql` | All table definitions (CREATE TABLE) |
| `02-functions-and-triggers.sql` | PostgreSQL functions + triggers |
| `03-indexes-and-seed-data.sql` | Performance indexes |
| `04-seed-data-global-master-data.sql` | Country, currency, tax, amenity master data |
| `05-seed-data-organization-base.sql` | Hotel chain and hotel seed data |
| `06-seed-data-catalog-and-room-configuration.sql` | Room type definitions |
| `07-seed-data-inventory-mapping-intance.sql` | Room instance records |
| `08-pricing-engine.sql` | Pricing rules and plans |

**Rule**: Schema changes go into the correct numbered script. **Never** mix DDL and DML in the same script unless they are strongly related (e.g., adding a column and backfilling it).

## Core Design Principles

### Principle 1: Sparse Data Storage (Availability Tables)

> **Problem**: 100 rooms × 365 days × 3 years = 109,500 rows of empty data  
> **Solution**: Only insert rows when state deviates from the default

- `room_slots` and `room_availability`: only insert rows on state change (booking, maintenance, manual block)
- The application interprets **absence of a row** as "Available/READY"
- **Prohibited**: cron jobs or seeding scripts that pre-create empty future slots

```sql
-- CORRECT: Insert only on state change
INSERT INTO room_slots (room_instance_id, stay_date, status, booking_id)
VALUES ($1, $2, 'OCCUPIED', $3);

-- WRONG: Never pre-generate empty slots
-- INSERT INTO room_slots (room_instance_id, stay_date, status) VALUES (..., 'READY');
```

### Principle 2: Financial Snapshot Immutability

> When a booking is confirmed, snapshot all pricing data into transaction detail tables.

All financial detail tables (`booking_details`, `invoice_details`, `booking_charges`) must include:
- `unit_price NUMERIC(15,2) NOT NULL` — price at time of booking
- `vat_rate NUMERIC(5,4) NOT NULL` — VAT rate at time of booking  
- `discount_amount NUMERIC(15,2) NOT NULL DEFAULT 0`
- `service_fee_rate NUMERIC(5,4)` (where applicable)

**Never** JOIN back to `hotel_room_types` or `catalog_items` for historical invoice retrieval.

### Principle 3: Optimistic Locking Version Column

Any table subject to concurrent writes from the online booking flow **must** have:
```sql
version INTEGER NOT NULL DEFAULT 0
```
- `room_availability` — must have `version` column
- Application uses JPA `@Version` on this column
- On conflict: PostgreSQL raises violation, Spring translates to `OptimisticLockingFailureException`

## Table Design Conventions

### Naming Standards
- Table names: `snake_case`, plural (`room_types`, `booking_details`)
- Column names: `snake_case`, singular
- Foreign key columns: `<referenced_table_singular>_id` (e.g., `hotel_id`, `room_type_id`)
- Status enum columns: use `VARCHAR(50)` with a `CHECK` constraint
- Boolean columns: prefix `is_` or `has_` (e.g., `is_active`, `has_breakfast`)

### Required Columns for Every Table

```sql
CREATE TABLE example_table (
    id              BIGSERIAL PRIMARY KEY,
    -- ... domain columns ...
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100)
);
```

Use a trigger to auto-update `updated_at`:
```sql
CREATE TRIGGER trg_example_table_updated_at
BEFORE UPDATE ON example_table
FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
```

### Status Columns

Always use `VARCHAR` with `CHECK` constraints over PostgreSQL custom ENUM types (easier to migrate):

```sql
status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE'
    CHECK (status IN ('ACTIVE', 'INACTIVE', 'ARCHIVED'))
```

## Index Strategy

### When to Add an Index

- Every foreign key column (if not already covered by unique constraint)
- Columns used in `WHERE` clauses in frequently-run queries
- Columns used in `ORDER BY` on large tables
- Composite indexes for multi-column filter patterns

### Index Naming Convention

```sql
-- Single column: idx_<table>_<column>
CREATE INDEX idx_room_slots_stay_date ON room_slots(stay_date);

-- Composite: idx_<table>_<col1>_<col2>
CREATE INDEX idx_room_slots_room_stay ON room_slots(room_instance_id, stay_date);

-- Unique: uq_<table>_<col>
CREATE UNIQUE INDEX uq_room_availability_type_date ON room_availability(room_type_id, availability_date);
```

### Required Indexes for Core Tables

```sql
-- room_slots: queried by date range + room instance
CREATE INDEX idx_room_slots_instance_date ON room_slots(room_instance_id, stay_date);
CREATE INDEX idx_room_slots_date_status ON room_slots(stay_date, status);

-- room_availability: queried by type + date range
CREATE INDEX idx_room_availability_type_date ON room_availability(room_type_id, availability_date);

-- bookings: queried by guest, hotel, status
CREATE INDEX idx_bookings_guest_id ON bookings(guest_id);
CREATE INDEX idx_bookings_hotel_date ON bookings(hotel_id, check_in_date, check_out_date);
CREATE INDEX idx_bookings_status ON bookings(status);
```

## Writing SQL Functions & Triggers

### Function Template

```sql
CREATE OR REPLACE FUNCTION function_name(param1 TYPE, param2 TYPE)
RETURNS RETURN_TYPE
LANGUAGE plpgsql
AS $$
DECLARE
    v_variable TYPE;
BEGIN
    -- logic
    RETURN v_variable;
EXCEPTION
    WHEN OTHERS THEN
        RAISE EXCEPTION 'function_name failed: %', SQLERRM;
END;
$$;
```

### Key Functions to Be Aware Of

- `calculate_room_price(room_type_id, check_in, check_out, guest_count)` → pricing engine
- `get_available_count(room_type_id, date)` → returns available room count for a date
- `update_updated_at_column()` → trigger function for auto-updating `updated_at`

## SQL Script Checklist

Before committing a database change:
- [ ] New table has `id`, `created_at`, `updated_at` columns
- [ ] New table has an `updated_at` trigger
- [ ] All foreign keys are indexed
- [ ] Financial tables include price snapshot columns
- [ ] `room_availability` and `room_slots` have `version` or proper locking mechanism
- [ ] `CHECK` constraints on status columns
- [ ] No pre-generation of empty availability rows
- [ ] New indexes follow naming convention
- [ ] Script is idempotent (uses `CREATE TABLE IF NOT EXISTS`, `CREATE INDEX IF NOT EXISTS`)
