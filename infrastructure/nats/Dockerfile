FROM nats:2.11-alpine

# Copy NATS CLI for stream initialization from nats-box
# This is a static binary, it doesn't need extra libs
COPY --from=natsio/nats-box:latest /usr/local/bin/nats /usr/local/bin/nats

# Copy configuration and scripts
COPY nats.conf /etc/nats/nats.conf
COPY init_streams.sh /usr/local/bin/init_streams.sh
RUN chmod +x /usr/local/bin/init_streams.sh

# Ensure data directory exists and is world-writable
RUN mkdir -p /data/jetstream && chmod -R 777 /data

EXPOSE 4222 8222 6222

# Copy the proper entrypoint script
COPY entrypoint.sh /usr/local/bin/entrypoint.sh
RUN chmod +x /usr/local/bin/entrypoint.sh

ENTRYPOINT ["/usr/local/bin/entrypoint.sh"]
