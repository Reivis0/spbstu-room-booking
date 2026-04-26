# 🎓 SPbSTU Room Booking System (GEMINI.md)

This project is a microservices-based system designed for searching and booking university rooms (auditoriums), specifically integrated with the SPbSTU (St. Petersburg Polytechnic University) schedule system (RUZ).

## 🏗️ Architecture Overview

The system consists of several specialized components:

1.  **Booking API (`booking_API/`)**:
    *   **Tech Stack**: Java 17, Spring Boot 3.4.
    *   **Responsibility**: Core business logic, user management (JWT-based), room directory, and high-level booking orchestration.
    *   **Storage**: PostgreSQL (main DB), Redis (sessions/cache).
    *   **Communication**: gRPC client (to Availability Engine), NATS (event-driven updates).

2.  **Availability Engine (`availability_engine/`)**:
    *   **Tech Stack**: C++17, gRPC, NATS, Redis.
    *   **Responsibility**: High-performance availability calculations, validation of booking intervals, and conflict detection.
    *   **Communication**: gRPC server (exposed on `50051`), NATS (subscribes to updates).

3.  **RUZ Importer (`availability_engine/src/ruz_importer/`)**:
    *   **Tech Stack**: C++, libcurl, libjsoncpp.
    *   **Responsibility**: Periodic import of university schedules from external APIs (RUZ) into the PostgreSQL database. Supports multiple universities (SPbSTU, LETI, SPbGU).

4.  **Frontend (`frontend/`)**:
    *   **Tech Stack**: React 18, TypeScript, Zustand, TailwindCSS (inferred).
    *   **Responsibility**: User interface for searching rooms and managing bookings.

5.  **Infrastructure (`infrastructure/`)**:
    *   **Components**: PostgreSQL 16 (Flyway migrations), Redis 7, NATS JetStream, Grafana, Prometheus, Nginx.
    *   **Orchestration**: Docker Compose.

## 🚀 Building and Running

The project uses a root-level `Makefile` to simplify orchestration.

### Prerequisites
*   Docker & Docker Compose
*   Java 17 (for local API development)
*   CMake & C++17 compiler (for local engine development)

### Key Commands

*   **Build everything**: `make build` (uses BuildKit for optimization)
*   **Run all services**: `make up`
*   **Run only infrastructure (DB, Redis, NATS)**: `make infra`
*   **Run migrations manually**: `make migrate-up`
*   **Stop and clean volumes**: `make clean`
*   **View logs**: `make logs`

### Component-Specific Management
*   **Restart Java API**: `make api`
*   **Restart C++ Engine**: `make engine`
*   **Restart Frontend**: `make ui`

## 🛠️ Development Conventions

### General
*   **Microservices**: Adhere to the separation of concerns between Java (business logic) and C++ (compute-heavy availability logic).
*   **Communication**: All gRPC changes must be updated in `availability_engine/protos/room_service/message.proto` and then re-compiled in both services.
*   **Environment**: Use `.env` (copy from `.env.example`).

### Database
*   **Migrations**: Use Flyway. New migrations should be created via `make migrate-create` to ensure correct versioning (`Vxxxx__name.sql` and `Uxxxx__name.sql`).
*   **Schema**: PostgreSQL uses GIST indices and exclusion constraints to prevent overlapping time slots natively.

### C++ (Availability Engine & Importer)
*   **Style**: Modern C++17, RAII, smart pointers.
*   **Build**: CMake-based. Dependencies are managed via `FetchContent` (NATS, Prometheus-cpp).
*   **Logging**: Use the internal `Logger` class (`LOG_INFO`, `LOG_ERROR`).

### Java (Booking API)
*   **Style**: Idiomatic Spring Boot with Lombok.
*   **Validation**: Jakarta Validation (Hibernate Validator).
*   **Security**: Spring Security + JWT.

### Testing
*   **C++**: GTest for unit and integration tests (`availability_engine/tests/`).
*   **Java**: JUnit 5 + Spring Boot Test.
*   **Verification**: Always run `make test` before pushing.

## 📊 Monitoring and Observability
*   **Metrics**: Both services expose Prometheus metrics.
*   **Dashboards**: Pre-configured Grafana dashboards are located in `infrastructure/grafana/dashboards/`.
*   **Logs**: Centralized logging in Docker; infrastructure logs are persisted in `infrastructure/postgreSQL/logs/`.
