# System Architecture: Poly Booking

## 1. Overview
Poly Booking is a distributed system for room reservation, specifically tailored for university campuses. It integrates with external schedule systems (RUZ) and provides a high-performance availability engine.

## 2. Component Diagram
- **Booking API (Java/Spring Boot)**: Entry point for users. Handles Authentication, Booking lifecycle (Saga pattern), and Event Outbox.
- **Availability Engine (C++17)**: High-performance service for conflict detection and availability calculations. Communicates via gRPC.
- **RUZ Importer (C++17)**: Background worker that synchronizes university schedules into `schedules_import` table and updates `rooms`/`buildings` metadata.
- **Infrastructure**: PostgreSQL (Main Storage), Redis (Cache), NATS JetStream (Messaging), Prometheus/Grafana (Monitoring).

## 3. Data Flow
1. **Booking Flow**:
   - User requests a booking via `Booking API`.
   - `Booking API` saves booking as `pending` and starts a **Saga**.
   - Saga calls `Availability Engine` via **gRPC** (`Validate` method).
   - `Availability Engine` checks **PostgreSQL** for existing schedules and bookings.
   - If valid, Saga updates booking status to `confirmed`.
   - `Booking API` uses **Transactional Outbox** to publish events to **NATS**.

2. **Import Flow**:
   - `RUZ Importer` fetches data from external RUZ API in batches.
   - Schedules are saved to `schedules_import` table with SHA-256 deduplication.
   - Metadata (`buildings`, `rooms`) is synced from imported records at the end of each university cycle.

## 4. Communication Contracts
### gRPC (RoomService)
- `ComputeIntervals`: (Currently STUB) Calculates available time slots for a room.
- `Validate`: Checks if a specific interval is available by querying PostgreSQL.
- `OcupiedIntervals`: (Currently STUB) Returns a list of busy slots for a room.

### NATS Subjects
- `booking.created`: Emitted when a new booking is pending.
- `booking.confirmed`: Emitted when a booking is finalized.
- `booking.cancelled`: Emitted on user cancellation.
- `events.schedule_refreshed`: (Defined but not yet emitted by Importer) Triggered after data refresh.

## 5. Technology Stack
- **Backend API**: Java 17, Spring Boot 3.4.
- **Performance Engine**: C++17, gRPC, libpq (Async), hiredis (Async).
- **Database**: PostgreSQL 16 with `btree_gist` and `uuid-ossp`.
- **Cache**: Redis 7 (Currently stubbed in C++ services).
- **Messaging**: NATS JetStream.

## 6. Known Implementation Details
- **User Management**: Default users (`admin@arch.local`, `student@arch.local`) are created via Java `@PostConstruct`.
- **Concurrency**: PostgreSQL `EXCLUDE USING GIST` constraint ensures no overlapping bookings at the database level.
- **Service Discovery**: Internal gRPC and NATS hostnames are managed via Docker DNS.
