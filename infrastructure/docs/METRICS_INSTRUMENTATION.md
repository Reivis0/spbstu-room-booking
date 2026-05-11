# Metrics Instrumentation Guide

This document provides production-grade guidelines for implementing Prometheus metrics in the SPbSTU Room Booking System services.

---

## Table of Contents

1. [Java API Instrumentation](#java-api-instrumentation)
2. [C++ Engine Instrumentation](#c-engine-instrumentation)
3. [Naming Conventions](#naming-conventions)
4. [Label Strategy](#label-strategy)
5. [Best Practices](#best-practices)
6. [Alert Rules](#alert-rules)

---

## Java API Instrumentation

The Booking API uses **Micrometer** with Spring Boot Actuator for metrics collection. Micrometer abstracts the metrics collection and provides a uniform interface for multiple backends (Prometheus, CloudWatch, InfluxDB, etc.).

### Setup

Spring Boot 3.4 includes Micrometer with Prometheus registry by default. Ensure the following dependency is present in `pom.xml`:

```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
    <!-- Version is managed by Spring Boot BOM -->
</dependency>

<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

### Actuator Configuration

In `application.yml` or `application.properties`:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,prometheus,info,metrics
  endpoint:
    prometheus:
      enabled: true
  metrics:
    distribution:
      percentiles-histogram:
        http.server.requests: true
      slo:
        http.server.requests: 0.005,0.01,0.025,0.05,0.1,0.25,0.5,1,2.5
    tags:
      application: booking-api
      service: booking-api
      environment: production
```

### Custom Business Metrics

Inject `MeterRegistry` into your services to create custom metrics:

```java
import io.micrometer.core.instrument.*;
import org.springframework.stereotype.Service;

@Service
public class BookingService {
    private final MeterRegistry meterRegistry;
    private final Counter bookingSuccessCounter;
    private final Counter bookingFailureCounter;
    private final Timer bookingDurationTimer;
    private final Gauge activeBookingsGauge;

    public BookingService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        
        // Counter: Track successful bookings
        this.bookingSuccessCounter = Counter.builder("booking.created")
            .description("Total number of successfully created bookings")
            .tag("service", "booking-api")
            .register(meterRegistry);
        
        // Counter: Track failed bookings
        this.bookingFailureCounter = Counter.builder("booking.failed")
            .description("Total number of failed booking attempts")
            .tag("service", "booking-api")
            .register(meterRegistry);
        
        // Timer: Track booking operation duration
        this.bookingDurationTimer = Timer.builder("booking.duration")
            .description("Time taken to process booking request")
            .publishPercentiles(0.5, 0.95, 0.99)
            .tag("service", "booking-api")
            .register(meterRegistry);
        
        // Gauge: Track active bookings (called periodically)
        this.activeBookingsGauge = Gauge.builder("booking.active", 
                () -> getActiveBookingsCount())
            .description("Number of active bookings")
            .tag("service", "booking-api")
            .register(meterRegistry);
    }

    public void createBooking(BookingRequest request) {
        var startTime = System.currentTimeMillis();
        try {
            // Booking logic here
            validateBooking(request);
            persistBooking(request);
            
            bookingSuccessCounter.increment();
            bookingDurationTimer.record(System.currentTimeMillis() - startTime, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            bookingFailureCounter.increment();
            throw new BookingException("Failed to create booking", e);
        }
    }

    private long getActiveBookingsCount() {
        // Fetch from database or cache
        return bookingRepository.countActiveBookings();
    }
}
```

### Tag/Label Structure

Use consistent tags for all custom metrics:

```java
Counter.builder("metric_name")
    .tag("service", "booking-api")          // Service identifier
    .tag("method", "POST")                  // HTTP method
    .tag("endpoint", "/api/v1/bookings")    // API endpoint
    .tag("status", "success")               // success/failure/error
    .tag("user_type", "student")            // student/faculty/admin
    .register(meterRegistry);
```

### Key Metrics to Implement

| Metric Name | Type | Description | Labels |
|------------|------|-------------|--------|
| `booking.created` | Counter | Successfully created bookings | `service`, `user_type`, `room_type` |
| `booking.cancelled` | Counter | Cancelled bookings | `service`, `reason` |
| `booking.failed` | Counter | Failed booking attempts | `service`, `error_reason` |
| `booking.duration` | Timer | Time to process booking | `service`, `endpoint`, `status` |
| `booking.search.duration` | Timer | Room search operation duration | `service`, `filter_count` |
| `availability.check.duration` | Timer | Time to check availability | `service`, `room_count` |
| `api.request.total` | Counter | Total API requests | `service`, `endpoint`, `method`, `status_code` |
| `auth.login.attempts` | Counter | Login attempts | `service`, `result` |
| `auth.token.issued` | Counter | JWT tokens issued | `service` |
| `db.connection.pool.active` | Gauge | Active DB connections | `service`, `pool_name` |
| `cache.hits` | Counter | Cache hits | `service`, `cache_name` |
| `cache.misses` | Counter | Cache misses | `service`, `cache_name` |

---

## C++ Engine Instrumentation

The Availability Engine uses **prometheus-cpp** for metrics collection. This library provides native C++ bindings to the Prometheus client library.

### Setup

The prometheus-cpp library is included via CMake `FetchContent` in `availability_engine/CMakeLists.txt`:

```cmake
include(FetchContent)

FetchContent_Declare(prometheus-cpp
    GIT_REPOSITORY https://github.com/jupp0r/prometheus-cpp.git
    GIT_TAG v1.1.0
)

FetchContent_MakeAvailable(prometheus-cpp)

target_link_libraries(availability_engine PRIVATE
    prometheus-cpp::core
    prometheus-cpp::pull
)
```

### Basic Metric Creation

```cpp
#include <prometheus/counter.h>
#include <prometheus/gauge.h>
#include <prometheus/histogram.h>
#include <prometheus/summary.h>
#include <prometheus/registry.h>
#include <prometheus/exposer.h>

// Create a Prometheus registry
auto registry = std::make_shared<prometheus::Registry>();

// Create metric families
auto& booking_counter = prometheus::BuildCounter()
    .Name("availability_requests_total")
    .Help("Total number of availability check requests")
    .Labels({{"service", "availability-engine"}})
    .Register(*registry);

auto& active_checks_gauge = prometheus::BuildGauge()
    .Name("active_availability_checks")
    .Help("Number of active availability checks")
    .Labels({{"service", "availability-engine"}})
    .Register(*registry);

auto& check_duration_histogram = prometheus::BuildHistogram()
    .Name("availability_check_duration_seconds")
    .Help("Availability check operation duration")
    .Buckets({0.001, 0.005, 0.01, 0.025, 0.05, 0.1, 0.25, 0.5, 1.0, 2.5, 5.0})
    .Labels({{"service", "availability-engine"}})
    .Register(*registry);

// Expose metrics on port 8082
prometheus::Exposer exposer("0.0.0.0:8082");
exposer.RegisterCollectable(registry);
```

### Counter Usage

Counters only increase or reset. Use for tracking totals:

```cpp
// Get or create a counter instance with specific labels
auto& counter = booking_counter.Add(
    {{"endpoint", "/check_availability"}, 
     {"status", "success"}}
);

// Increment the counter
counter.Increment();      // +1
counter.Increment(5);     // +5
```

### Gauge Usage

Gauges can increase or decrease. Use for current state:

```cpp
// Get or create a gauge instance
auto& gauge = active_checks_gauge.Add(
    {{"service", "availability-engine"}}
);

// Set or modify the gauge
gauge.Set(42);            // Set to 42
gauge.Increment();        // +1
gauge.Decrement();        // -1
```

### Histogram Usage

Histograms track distribution of values:

```cpp
// Get or create a histogram instance
auto& histogram = check_duration_histogram.Add(
    {{"method", "grpc"}, 
     {"status", "success"}}
);

// Observe a value (in seconds)
auto start = std::chrono::high_resolution_clock::now();
// ... perform operation ...
auto end = std::chrono::high_resolution_clock::now();
auto duration = std::chrono::duration<double>(end - start).count();
histogram.Observe(duration);
```

### Complete Example: Wrapping a gRPC Handler

```cpp
#include <prometheus/counter.h>
#include <prometheus/histogram.h>
#include <prometheus/registry.h>

class AvailabilityServiceImpl : public RoomService::Service {
private:
    std::shared_ptr<prometheus::Registry> registry_;
    prometheus::Family<prometheus::Counter>* request_counter_;
    prometheus::Family<prometheus::Histogram>* duration_histogram_;

public:
    AvailabilityServiceImpl(std::shared_ptr<prometheus::Registry> registry)
        : registry_(registry) {
        
        request_counter_ = &prometheus::BuildCounter()
            .Name("grpc_availability_requests_total")
            .Help("Total gRPC availability requests")
            .Labels({{"service", "availability-engine"}})
            .Register(*registry_);
        
        duration_histogram_ = &prometheus::BuildHistogram()
            .Name("grpc_availability_duration_seconds")
            .Help("gRPC availability check duration")
            .Buckets({0.001, 0.01, 0.1, 1.0})
            .Labels({{"service", "availability-engine"}})
            .Register(*registry_);
    }

    ::grpc::Status CheckAvailability(
        ::grpc::ServerContext* context,
        const CheckAvailabilityRequest* request,
        CheckAvailabilityResponse* response) override {
        
        auto start = std::chrono::high_resolution_clock::now();
        
        try {
            // Increment request counter
            auto& req_counter = request_counter_->Add(
                {{"endpoint", "CheckAvailability"}, 
                 {"status", "processing"}}
            );
            req_counter.Increment();
            
            // Perform availability check
            bool available = performAvailabilityCheck(*request);
            response->set_available(available);
            
            // Record successful request
            auto& success_counter = request_counter_->Add(
                {{"endpoint", "CheckAvailability"}, 
                 {"status", "success"}}
            );
            success_counter.Increment();
            
        } catch (const std::exception& e) {
            // Record failure
            auto& error_counter = request_counter_->Add(
                {{"endpoint", "CheckAvailability"}, 
                 {"status", "error"}}
            );
            error_counter.Increment();
            
            return ::grpc::Status(
                ::grpc::StatusCode::INTERNAL, 
                e.what()
            );
        }
        
        // Record duration
        auto end = std::chrono::high_resolution_clock::now();
        auto duration = std::chrono::duration<double>(end - start).count();
        auto& histogram = duration_histogram_->Add(
            {{"endpoint", "CheckAvailability"}, 
             {"status", "success"}}
        );
        histogram.Observe(duration);
        
        return ::grpc::Status::OK;
    }
};
```

### Key Metrics to Implement

| Metric Name | Type | Description | Labels |
|------------|------|-------------|--------|
| `grpc_availability_requests_total` | Counter | Total availability check requests | `endpoint`, `status` |
| `grpc_availability_conflicts_total` | Counter | Bookings with time conflicts | `status` |
| `grpc_availability_duration_seconds` | Histogram | Duration of availability checks | `endpoint`, `status` |
| `active_availability_checks` | Gauge | Concurrent availability checks | `service` |
| `conflict_resolution_duration_seconds` | Histogram | Time to resolve booking conflicts | `service` |
| `schedule_import_duration_seconds` | Histogram | RUZ data import duration | `university`, `status` |
| `imported_events_total` | Counter | Total imported events | `university`, `status` |
| `ruz_api_requests_total` | Counter | RUZ API calls | `endpoint`, `status` |
| `database_query_duration_seconds` | Histogram | Database query execution time | `query_type`, `status` |
| `redis_cache_operations_total` | Counter | Redis operations | `operation`, `status` |
| `nats_publish_duration_seconds` | Histogram | NATS message publish time | `subject`, `status` |

---

## Naming Conventions

Follow these naming conventions for all metrics:

### Metric Names

- **Format**: `{namespace}_{subsystem}_{name}_{unit}`
- **Examples**:
  - `booking_creation_duration_seconds`
  - `availability_check_requests_total`
  - `grpc_request_duration_milliseconds`

### Rules

1. Use lowercase with underscores (snake_case)
2. End with the unit suffix: `_total`, `_seconds`, `_bytes`, `_ratio`, `_count`
3. Keep names descriptive but concise (max 50 chars)
4. Avoid reserved words from Prometheus reserved labels

### Label Names

- **Format**: `{noun}_{adjective}` (snake_case)
- **Examples**:
  - `service`, `endpoint`, `method`, `status`
  - `user_type`, `room_type`, `result`
  - `error_reason`, `cache_name`, `pool_name`

---

## Label Strategy

Use consistent labels across all metrics for correlation:

### Global Labels (All Services)

```
service: booking-api | availability-engine
environment: production | staging | development
instance: {hostname}:{port}
```

### Java API Labels

```
endpoint: /api/v1/bookings
method: GET | POST | PUT | DELETE
status_code: 200 | 400 | 500 | ...
user_type: student | faculty | admin
room_type: auditorium | classroom | lab
```

### C++ Engine Labels

```
endpoint: CheckAvailability | GetSchedule | ...
status: success | error | conflict
method: grpc | http
```

### Best Practices

1. **Limit cardinality**: Avoid using user IDs, request IDs, or timestamps as labels
2. **Use consistent values**: Use fixed enums (not variables)
3. **Add context labels**: Include service and environment for cross-service queries
4. **Keep label count low**: Aim for 5-10 labels per metric maximum

---

## Best Practices

### 1. High Cardinality Avoidance

❌ **Bad**: Using user email as a label
```java
Counter.builder("user.action")
    .tag("user_email", request.getUserEmail())  // Very high cardinality!
    .register(meterRegistry);
```

✅ **Good**: Using aggregated user type
```java
Counter.builder("user.action")
    .tag("user_type", "student")  // Low cardinality, discrete values
    .register(meterRegistry);
```

### 2. Metric Types

- **Counter**: Only increases (total requests, errors, bookings)
- **Gauge**: Can go up/down (active connections, queue size, memory)
- **Histogram**: Distribution of values (request duration, file size)
- **Summary**: Like histogram but includes quantiles (latency p99, p95)

### 3. Instrumentation Points

For the Booking API:

```java
// 1. Request boundary (automatic via Spring Boot)
@RequestMapping("/api/v1/bookings")
public ResponseEntity<BookingResponse> createBooking(@RequestBody BookingRequest req) { }

// 2. Database operations
bookingRepository.save(booking);  // Measured by Spring Data metrics

// 3. External service calls (gRPC to Availability Engine)
availabilityClient.checkAvailability(request);  // Custom histogram

// 4. Cache operations
cache.get(key);  // Cache hit/miss counter

// 5. Business logic milestones
bookingService.validateDates(booking);  // Validation success/failure
```

For the C++ Engine:

```cpp
// 1. gRPC entry/exit points
CheckAvailability() { /* record duration */ }

// 2. Database queries
executeQuery(sql);  // Record query time and result count

// 3. External API calls
fetchRUZSchedule();  // Record HTTP status and latency

// 4. Async operations
publishToNATS(message);  // Record publish time

// 5. Business logic calculations
calculateConflicts(schedules);  // Record algorithm duration
```

### 4. Retention and Aggregation

- **Scrape interval**: 30 seconds (configured in prometheus.yml)
- **Data retention**: 15 days
- **Pre-aggregation**: Use recording rules for expensive calculations
- **Sampling**: Consider sampling for high-volume metrics

### 5. Alerting Strategy

Define alerts based on SLOs:

```yaml
# Example alert rules (in alerts.yml)
groups:
  - name: booking_api
    interval: 30s
    rules:
      - alert: BookingAPIHighErrorRate
        expr: |
          rate(booking_failed[5m]) / rate(booking_created[5m]) > 0.05
        for: 5m
        annotations:
          summary: "Booking API error rate > 5%"
      
      - alert: AvailabilityEngineSlowResponses
        expr: |
          histogram_quantile(0.95, 
            rate(grpc_availability_duration_seconds_bucket[5m])
          ) > 1.0
        for: 5m
        annotations:
          summary: "p95 availability check latency > 1s"
```

---

## Alert Rules

Create `infrastructure/prometheus/alerts.yml`:

```yaml
groups:
  - name: spbstu_booking_system
    interval: 30s
    rules:
      # Booking API alerts
      - alert: BookingAPIDown
        expr: up{job="java-api"} == 0
        for: 1m
        annotations:
          severity: critical
          summary: "Booking API is down"

      - alert: BookingAPIHighLatency
        expr: |
          histogram_quantile(0.99,
            rate(http_server_requests_seconds_bucket{job="java-api"}[5m])
          ) > 2.0
        for: 5m
        annotations:
          severity: warning
          summary: "Booking API p99 latency > 2s"

      - alert: BookingFailureRate
        expr: |
          increase(booking_failed[5m]) > 10
        for: 5m
        annotations:
          severity: warning
          summary: "High booking failure rate"

      # Availability Engine alerts
      - alert: AvailabilityEngineDown
        expr: up{job="cpp-engine"} == 0
        for: 1m
        annotations:
          severity: critical
          summary: "Availability Engine is down"

      - alert: AvailabilityCheckTimeout
        expr: |
          rate(grpc_availability_requests_total{status="timeout"}[5m]) > 0.1
        for: 5m
        annotations:
          severity: warning
          summary: "Availability checks timing out"

      # Database alerts
      - alert: PostgreSQLDown
        expr: up{job="postgres-exporter"} == 0
        for: 1m
        annotations:
          severity: critical
          summary: "PostgreSQL database is down"

      - alert: DatabaseConnectionPoolAlmostFull
        expr: |
          pg_stat_activity_count{datname="booking_db"} 
          / pg_settings_max_connections > 0.8
        for: 5m
        annotations:
          severity: warning
          summary: "Database connection pool > 80% utilized"

      # General infrastructure alerts
      - alert: PrometheusDown
        expr: up{job="prometheus"} == 0
        for: 1m
        annotations:
          severity: critical
          summary: "Prometheus server is down"

      - alert: HighMemoryUsage
        expr: container_memory_usage_bytes / container_spec_memory_limit_bytes > 0.9
        for: 5m
        annotations:
          severity: warning
          summary: "Container memory usage > 90%"
```

---

## Verification

To verify metrics are being collected:

### Java API
```bash
curl -s http://localhost:8081/actuator/prometheus | grep booking_
```

### C++ Engine
```bash
curl -s http://localhost:8082/metrics | grep availability_
```

### Prometheus Query Examples

```promql
# Request rate per endpoint
rate(http_server_requests_seconds_count{job="java-api"}[5m])

# P95 latency
histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m]))

# Availability check success rate
rate(grpc_availability_requests_total{status="success"}[5m])

# Active connections
active_connections{service="booking-api"}

# Database query latency by type
histogram_quantile(0.99, database_query_duration_seconds_bucket)
```

---

## References

- [Prometheus Metrics Types](https://prometheus.io/docs/concepts/metric_types/)
- [Micrometer Documentation](https://micrometer.io/)
- [prometheus-cpp Library](https://github.com/jupp0r/prometheus-cpp)
- [Prometheus Naming Best Practices](https://prometheus.io/docs/practices/naming/)
- [Prometheus Querying](https://prometheus.io/docs/prometheus/latest/querying/basics/)
