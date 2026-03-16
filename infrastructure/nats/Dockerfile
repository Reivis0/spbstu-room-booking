FROM nats:2.11-alpine

RUN apk add --no-cache curl bash wget

COPY --from=natsio/nats-box:latest /usr/local/bin/nats /usr/local/bin/nats

COPY init_streams.sh /usr/local/bin/init_streams.sh
RUN chmod +x /usr/local/bin/init_streams.sh

RUN mkdir -p /data/jetstream

EXPOSE 4222 8222 6222

# Create a proper wrapper script to ensure signals are passed
RUN echo '#!/bin/sh' > /usr/local/bin/entrypoint.sh && \
    echo 'nats-server -c /etc/nats/nats.conf & NATS_PID=$!' >> /usr/local/bin/entrypoint.sh && \
    echo 'sleep 5 && /usr/local/bin/init_streams.sh &' >> /usr/local/bin/entrypoint.sh && \
    echo 'wait $NATS_PID' >> /usr/local/bin/entrypoint.sh && \
    chmod +x /usr/local/bin/entrypoint.sh

CMD ["/usr/local/bin/entrypoint.sh"]