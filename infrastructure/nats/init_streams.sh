#!/bin/sh
set -e

echo "Waiting for NATS to be ready..."
until wget --quiet --tries=1 --spider http://localhost:8222/healthz 2>/dev/null; do
  echo "NATS not ready, retrying in 2s..."
  sleep 2
done

echo "Creating NATS streams..."
nats stream add booking_events \
    --server nats://localhost:4222 \
    --subjects "booking.created,booking.updated,booking.cancelled,booking.rejected" \
    --storage file \
    --retention limits \
    --max-age 24h \
    --max-msgs 1000 \
    --max-bytes 100MB \
    --replicas 1 \
    --defaults 2>/dev/null || echo "Stream 'booking_events' already exists"

nats stream add schedule_events \
    --server nats://localhost:4222 \
    --subjects "scheduling.imported" \
    --storage file \
    --max-age 24h \
    --max-msgs 1000 \
    --max-bytes 50MB \
    --replicas 1 \
    --defaults 2>/dev/null || echo "Stream 'schedule_events' already exists"

nats stream add cache_invalidate \
    --server nats://localhost:4222 \
    --subjects "cache.invalidate.room" \
    --storage memory \
    --max-age 10m \
    --max-msgs 1000 \
    --max-bytes 10MB \
    --replicas 1 \
    --defaults 2>/dev/null || echo "Stream 'cache_invalidate' already exists"

nats stream add notifications \
    --server nats://localhost:4222 \
    --subjects "notification.trigger" \
    --storage file \
    --max-age 10h \
    --max-msgs 5000 \
    --max-bytes 50MB \
    --replicas 1 \
    --defaults 2>/dev/null || echo "Stream 'notifications' already exists"

echo "NATS streams created successfully."