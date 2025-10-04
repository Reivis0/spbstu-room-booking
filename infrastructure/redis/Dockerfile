FROM redis:7-alpine

RUN apk add --no-cache bash

COPY redis.conf /usr/local/etc/redis/redis.conf

RUN mkdir -p /data && chown -R redis:redis /data

EXPOSE 6379

CMD [ "redis-server", "/usr/local/etc/redis/redis.conf" ]

HEALTHCHECK --interval=10s --timeout=5s --retries=5 \
    CMD redis-cli ping | grep PONG || exit 1