#pragma once
#include <prometheus/registry.h>
#include <prometheus/exposer.h>
#include <prometheus/counter.h>
#include <prometheus/histogram.h>
#include <memory>
#include <string>

class MetricsRegistry {
public:
    static MetricsRegistry& instance();
    void start(const std::string& bind_address = "0.0.0.0:8082");

    // NATS метрики
    prometheus::Counter* nats_messages_processed_total{nullptr};
    prometheus::Counter* nats_messages_failed_total{nullptr};

    // Cache метрики
    prometheus::Counter* cache_hits_total{nullptr};
    prometheus::Counter* cache_misses_total{nullptr};

    // Histogram для времени выполнения
    prometheus::Histogram* schedule_parse_duration_seconds{nullptr};
    prometheus::Histogram* db_query_duration_seconds{nullptr};

private:
    MetricsRegistry() = default;
    std::shared_ptr<prometheus::Registry> registry_;
    std::unique_ptr<prometheus::Exposer> exposer_;
};