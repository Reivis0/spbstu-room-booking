FROM nats:2.11-alpine

RUN apk add --no-cache curl bash wget netcat-openbsd && \
    timeout 60 sh -c 'for i in 1 2 3; do \
        curl -sf --max-time 30 --connect-timeout 10 https://binaries.nats.dev/nats-io/natscli/nats@latest | sh && break || \
        (echo "Retry $i/3..." && sleep 5); \
    done' && \
    mv nats /usr/local/bin/ 2>/dev/null || echo "nats binary not found, continuing..." && \
    chmod +x /usr/local/bin/nats 2>/dev/null || true && \
    apk add --no-cache netcat-openbsd

COPY nats.conf /etc/nats/nats.conf

COPY init_streams.sh /usr/local/bin/init_streams.sh
RUN chmod +x /usr/local/bin/init_streams.sh

RUN mkdir -p /data/jetstream

EXPOSE 4222 8222 6222

CMD ["sh", "-c", "nats-server -c /etc/nats/nats.conf & NATS_PID=$! && sleep 5 && /usr/local/bin/init_streams.sh && wait $NATS_PID"]

HEALTHCHECK --interval=10s --timeout=3s --retries=3 --start-period=20s \
    CMD sh -c 'timeout 3 bash -c "</dev/tcp/localhost/4222" 2>/dev/null && exit 0 || exit 1'
