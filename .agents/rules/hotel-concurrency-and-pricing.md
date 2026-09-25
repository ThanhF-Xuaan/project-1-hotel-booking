---
trigger: model_decision
description: Automatically activated when the agent writes backend code (Spring Boot/Python), designs database schemas (PostgreSQL), configures caches (Redis), or modifies booking, pricing, and locking logics.
---

## 1. Concurrency Control & Hybrid Locking Strategy

The system orchestrates multiple locking layers depending on the transaction context. The agent must strictly apply the correct mechanism according to this matrix:

### 1.1. Hybrid Locking Matrix

| Business Flow | Targeted Resource | Locking Mechanism | Implementation Details & Rationale |
| :--- | :--- | :--- | :--- |
| **Online Payment Window** | Virtual Room Inventory (Redis Cache) | **Temporary Session Lock (TTL-based)** | Lock room inventory in Redis with a strict 10-minute Time-To-Live (TTL). If payment fails or times out, a background worker/Redis listener automatically releases the inventory. *Prevents PostgreSQL connection pool exhaustion during external payment redirects.* |
| **Online Booking (Checkout)** | Aggregate Inventory (`room_availability`) | **Optimistic Locking (`version`-based)** | Use JPA `@Version` or database version checking. When booking, verify `available_count > 0` and increment `version`. On conflict, raise `OptimisticLockingFailureException`, abort transaction, and retry or notify user. *Maximizes throughput under high concurrent traffic.* |
| **Walk-in Booking (Front Desk)** | Physical Room (`room_slots`) & Inventory (`room_availability`) | **Double Pessimistic Lock (Sequential Database Locks)** | Acquired during a single transaction for immediate booking. *Guarantees 100% conversion and real-time inventory updates for walk-in guests at the POS terminal, avoiding optimistic lock retries for front-desk staff.* |

### 1.2. Lock Ordering Rule for Deadlock Prevention

In the **Double Pessimistic Lock** flow (Walk-in), locks must be acquired in a strict hierarchical order to prevent circular wait states (Deadlocks):

1. **Lock `room_slots` (Granular / Leaf Resource) FIRST.**
2. **Lock `room_availability` (Aggregate / Parent Resource) SECOND.**

*Fail-Fast Principle:* If the specific physical room is unavailable (e.g., in `MAINTENANCE` or occupied), the transaction fails immediately (Fail-Fast) and rolls back without holding a broad lock on the aggregate availability. This minimizes lock contention on high-demand room types.

---

## 2. Sparse Data Storage Architecture

To prevent database "Data Explosion" (e.g., 100 rooms × 365 days = 36,500 rows of blank slots annually), the system must utilize a sparse data model:

* **Default State Assumption:** Any physical room slot in the future without an explicit record is programmatically interpreted as `READY` (Vacant/Available).
* **Insertion Condition:** Database rows in `room_slots` and `room_availability` must only be created (`INSERT`) upon an explicit state change (e.g., a confirmed booking, manual block, or scheduled `MAINTENANCE`).
* **Strict Prohibition:** Never write background cron jobs or seeding scripts to pre-generate blank "empty" slots for future dates.

---

## 3. Financial Consistency & Immutability

Financial records are subject to strict auditing standards. Data must be immutable once finalized:

* **Data Snapshotting (Denormalization):** When a booking is confirmed or an invoice is generated, the agent must duplicate and save standard attributes—including room type name, final `unit_price`, `vat_rate`, and `discount_amount`—directly into transaction detail tables (`booking_details`, `invoice_details`, `booking_charges`).
* **No Dynamic Joins for History:** When retrieving historical invoices or receipt details, the system must read from the snapshots rather than performing dynamic SQL `JOIN`s back to catalog masters (`hotel_room_types`, `catalog_items`). This prevents past accounting figures from changing if an admin updates catalog prices or tax rules in the future.

---

## 4. Modular Monolith Code Isolation

To preserve scalability while avoiding microservices overhead, follow strict modular boundaries:

* Core domain models (Inventory, Booking, Pricing, POS, CRM, IAM) must reside in isolated packages.
* Cross-module communication must happen via clean service interfaces, but share a single physical database to allow safe, localized transactional boundaries using Spring's `@Transactional`.