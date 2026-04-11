FROM nats:2.11-alpine

RUN apk add --no-cache curl bash wget && \
    curl -sf https://binaries.nats.dev/nats-io/natscli/nats@latest | sh && \
    mv nats /usr/local/bin/ && \
    chmod +x /usr/local/bin/nats

COPY nats.conf /etc/nats/nats.conf

COPY init_streams.sh /usr/local/bin/init_streams.sh
RUN chmod +x /usr/local/bin/init_streams.sh

RUN mkdir -p /data/jetstream

EXPOSE 4222 8222 6222

CMD ["sh", "-c", "nats-server -c /etc/nats/nats.conf & NATS_PID=$! && sleep 5 && /usr/local/bin/init_streams.sh && wait $NATS_PID"]

HEALTHCHECK --interval=10s --timeout=3s --retries=3 \
    CMD wget --quiet --tries=1 --spider http://localhost:8222/healthz || exit 1
