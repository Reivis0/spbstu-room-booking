FROM ubuntu:22.04 AS build

ENV DEBIAN_FRONTEND=noninteractive

RUN apt-get update && apt-get install -y \
    build-essential \
    cmake \
    git \
    libpq-dev \
    libevent-dev \
    libhiredis-dev \
    libprotobuf-dev \
    protobuf-compiler \
    libgrpc++-dev \
    protobuf-compiler-grpc \
    pkg-config \
    libgrpc-dev \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY . .

RUN mkdir build && cd build && \
    cmake .. && \
    make -j$(nproc)

FROM ubuntu:22.04

ENV DEBIAN_FRONTEND=noninteractive

RUN apt-get update && apt-get install -y \
    libpq5 \
    libevent-2.1-7 \
    libhiredis0.14 \
    libprotobuf23 \
    libgrpc++1 \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY --from=build /app/build/bin/availability_engine .
COPY --from=build /app/build/lib /usr/local/lib/

# Create configs directory
RUN mkdir configs

# Create entrypoint script
RUN echo '#!/bin/bash\n\
\n\
# Generate pg_config.ini\n\
echo "[booking_service]" > configs/pg_config.ini\n\
echo "host=${POSTGRES_HOST:-localhost}" >> configs/pg_config.ini\n\
echo "port=${POSTGRES_PORT:-5432}" >> configs/pg_config.ini\n\
echo "database_name=${POSTGRES_DB:-booking_mvp_db}" >> configs/pg_config.ini\n\
echo "username=${POSTGRES_USER:-postgres}" >> configs/pg_config.ini\n\
echo "password=${POSTGRES_PASSWORD:-password}" >> configs/pg_config.ini\n\
\n\
# Generate redis_config.ini\n\
echo "[booking_service]" > configs/redis_config.ini\n\
echo "host=${REDIS_HOST:-localhost}" >> configs/redis_config.ini\n\
echo "port=${REDIS_PORT:-6379}" >> configs/redis_config.ini\n\
echo "password=${REDIS_PASSWORD:-password}" >> configs/redis_config.ini\n\
echo "connection_timeout=${REDIS_TIMEOUT:-5000}" >> configs/redis_config.ini\n\
\n\
# Generate nats_config.ini\n\
echo "[booking_service]" > configs/nats_config.ini\n\
echo "host=${NATS_HOST:-localhost}" >> configs/nats_config.ini\n\
echo "client_port=${NATS_PORT:-4222}" >> configs/nats_config.ini\n\
\n\
exec ./availability_engine\n\
' > entrypoint.sh && chmod +x entrypoint.sh

CMD ["./entrypoint.sh"]
