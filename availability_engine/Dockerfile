FROM ubuntu:22.04 AS build

ENV DEBIAN_FRONTEND=noninteractive

RUN apt-get update && apt-get install -y \
    build-essential \
    cmake \
    git \
    pkg-config \
    libssl-dev \
    libpq-dev \
    libevent-dev \
    libhiredis-dev \
    libprotobuf-dev \
    protobuf-compiler \
    libgrpc++-dev \
    libgrpc-dev \
    protobuf-compiler-grpc \
    libc-ares-dev \
    zlib1g-dev \
    ca-certificates \
    libcurl4-openssl-dev \
    libjsoncpp-dev \
    libgtest-dev \
    libgmock-dev \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /tmp
RUN git clone https://github.com/nats-io/nats.c.git && \
    cd nats.c && \
    cmake . -DNATS_BUILD_WITH_TLS=ON -DNATS_BUILD_STREAMING=OFF && \
    make -j$(nproc) && \
    make install && \
    ldconfig

WORKDIR /app
COPY . .

RUN mkdir build && cd build && \
    cmake .. && \
    make -j$(nproc) VERBOSE=1

FROM ubuntu:22.04

ENV DEBIAN_FRONTEND=noninteractive

RUN apt-get update && apt-get install -y \
    libpq5 \
    libevent-2.1-7 \
    libhiredis0.14 \
    libprotobuf23 \
    libgrpc++1 \
    libssl3 \
    ca-certificates \
    netcat-openbsd \
    libcurl4 \
    libjsoncpp25 \
    && rm -rf /var/lib/apt/lists/*

COPY --from=build /usr/local/lib/libnats* /usr/lib/

WORKDIR /app
COPY --from=build /app/build/bin/availability_engine .

RUN mkdir configs

RUN echo '#!/bin/bash\n\
set -e\n\
\n\
cat > configs/pg_config.ini << EOF\n\
[booking_service]\n\
host=${POSTGRES_HOST:-localhost}\n\
port=${POSTGRES_PORT:-5432}\n\
database_name=${POSTGRES_DB:-booking_mvp_db}\n\
username=${POSTGRES_USER:-postgres}\n\
password=${POSTGRES_PASSWORD:-password}\n\
EOF\n\
\n\
cat > configs/redis_config.ini << EOF\n\
[booking_service]\n\
host=${REDIS_HOST:-localhost}\n\
port=${REDIS_PORT:-6379}\n\
password=${REDIS_PASSWORD:-password}\n\
connection_timeout=${REDIS_TIMEOUT:-5000}\n\
EOF\n\
\n\
cat > configs/nats_config.ini << EOF\n\
[booking_service]\n\
host=${NATS_HOST:-localhost}\n\
client_port=${NATS_PORT:-4222}\n\
EOF\n\
\n\
exec ./availability_engine\n\
' > entrypoint.sh && chmod +x entrypoint.sh

EXPOSE 50051

HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD nc -z localhost 50051 || exit 1

CMD ["./entrypoint.sh"]
