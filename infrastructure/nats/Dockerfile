FROM nats:2.11-alpine

RUN apk add --no-cache curl bash wget

COPY --from=natsio/nats-box:latest /usr/local/bin/nats /usr/local/bin/nats

COPY init_streams.sh /usr/local/bin/init_streams.sh
RUN chmod +x /usr/local/bin/init_streams.sh

RUN mkdir -p /data/jetstream

EXPOSE 4222 8222 6222

CMD ["sh", "-c", "nats-server -c /etc/nats/nats.conf & NATS_PID=$! && sleep 5 && /usr/local/bin/init_streams.sh && wait $NATS_PID"]