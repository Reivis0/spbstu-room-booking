#!/bin/sh
set -e

# Start NATS server in background with any provided arguments
nats-server -c /etc/nats/nats.conf "$@" &
NATS_PID=$!

# Wait for NATS to start and create streams
/usr/local/bin/init_streams.sh

# Stay in foreground
wait $NATS_PID
