# Phase 1: Environment and external dependencies
FROM ubuntu:22.04 AS deps

ENV DEBIAN_FRONTEND=noninteractive

RUN apt-get update && apt-get install -y --no-install-recommends \
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
    ccache \
    && rm -rf /var/lib/apt/lists/*

# Build nats.c only once
WORKDIR /tmp/nats
RUN git clone --depth 1 https://github.com/nats-io/nats.c.git . && \
    cmake . -DNATS_BUILD_WITH_TLS=ON -DNATS_BUILD_STREAMING=OFF && \
    make -j$(nproc) && \
    make install && \
    ldconfig

# Phase 2: Application Build
FROM deps AS build
WORKDIR /app

# Copy CMake files first to cache dependencies (if any)
COPY CMakeLists.txt .
COPY src/CMakeLists.txt src/
COPY src/availability_engine/CMakeLists.txt src/availability_engine/
COPY src/common/CMakeLists.txt src/common/
COPY src/ruz_importer/CMakeLists.txt src/ruz_importer/
COPY log/CMakeLists.txt log/
COPY protos/CMakeLists.txt protos/
COPY tests/CMakeLists.txt tests/

# Now copy the source code
COPY . .

# Use BuildKit cache for compilation (ccache)
ENV CMAKE_CXX_COMPILER_LAUNCHER=ccache
RUN --mount=type=cache,target=/root/.ccache \
    mkdir -p build && cd build && \
    cmake .. && \
    make -j$(nproc)

# Phase 3: Runtime image
FROM ubuntu:22.04
ENV DEBIAN_FRONTEND=noninteractive

RUN apt-get update && apt-get install -y --no-install-recommends \
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

WORKDIR /app

COPY --from=deps /usr/local/lib/libnats* /usr/lib/
COPY --from=build /app/build/bin/availability_engine .
COPY --from=build /app/build/bin/ruz_importer .

RUN mkdir configs

# Clean entrypoint setup
RUN printf '#!/bin/bash\n\
set -e\n\
mkdir -p configs\n\
cat > configs/pg_config.ini << EOF\n\
[booking_service]\nhost=${POSTGRES_HOST:-localhost}\nport=${POSTGRES_PORT:-5432}\ndatabase_name=${POSTGRES_DB:-booking_mvp_db}\nusername=${POSTGRES_USER:-postgres}\npassword=${POSTGRES_PASSWORD:-password}\nEOF\n\
cat > configs/redis_config.ini << EOF\n\
[booking_service]\nhost=${REDIS_HOST:-localhost}\nport=${REDIS_PORT:-6379}\npassword=${REDIS_PASSWORD:-password}\nconnection_timeout=${REDIS_TIMEOUT:-5000}\nEOF\n\
cat > configs/nats_config.ini << EOF\n\
[booking_service]\nhost=${NATS_HOST:-localhost}\nclient_port=${NATS_PORT:-4222}\nEOF\n\
cat > configs/ruz_importer_config.ini << EOF\n\
[ruz_importer]\napi_url=${RUZ_API_URL:-https://ruz.spbstu.ru/api/v1/ruz/scheduler}\nimport_interval_seconds=${RUZ_IMPORT_INTERVAL:-1800}\nEOF\n\
\n\
if [ "$1" = "importer" ]; then\n\
    shift\n\
    exec ./ruz_importer "$@"\n\
else\n\
    exec ./availability_engine "$@"\n\
fi\n' > entrypoint.sh && chmod +x entrypoint.sh

EXPOSE 50051 8082

HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD nc -z localhost 50051 || exit 1

ENTRYPOINT ["./entrypoint.sh"]
