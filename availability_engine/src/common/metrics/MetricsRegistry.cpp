#include "MetricsRegistry.h"

MetricsRegistry& MetricsRegistry::instance() {
    static MetricsRegistry inst;
    return inst;
}

void MetricsRegistry::start(const std::string& bind_address) {
    registry_ = std::make_shared<prometheus::Registry>();
    exposer_ = std::make_unique<prometheus::Exposer>(bind_address);
    exposer_->RegisterCollectable(registry_);

    auto& nats_family = prometheus::BuildCounter()
        .Name("nats_messages_processed_total")
        .Help("Total NATS messages processed successfully")
        .Register(*registry_);
    nats_messages_processed_total = &nats_family.Add({});

    auto& nats_fail_family = prometheus::BuildCounter()
        .Name("nats_messages_failed_total")
        .Help("Total NATS messages failed")
        .Register(*registry_);
    nats_messages_failed_total = &nats_fail_family.Add({});

    auto& cache_hits_family = prometheus::BuildCounter()
        .Name("cache_hits_total")
        .Help("Redis cache hits")
        .Register(*registry_);
    cache_hits_total = &cache_hits_family.Add({});

    auto& cache_misses_family = prometheus::BuildCounter()
        .Name("cache_misses_total")
        .Help("Redis cache misses")
        .Register(*registry_);
    cache_misses_total = &cache_misses_family.Add({});

    auto& parse_hist_family = prometheus::BuildHistogram()
        .Name("schedule_parse_duration_seconds")
        .Help("RUZ schedule parse duration")
        .Register(*registry_);
    schedule_parse_duration_seconds = &parse_hist_family.Add(
        {}, prometheus::Histogram::BucketBoundaries{0.01, 0.05, 0.1, 0.5, 1.0, 5.0}
    );

    auto& db_hist_family = prometheus::BuildHistogram()
        .Name("db_query_duration_seconds")
        .Help("PostgreSQL query duration")
        .Register(*registry_);
    db_query_duration_seconds = &db_hist_family.Add(
        {}, prometheus::Histogram::BucketBoundaries{0.001, 0.005, 0.01, 0.05, 0.1, 0.5}
    );
}