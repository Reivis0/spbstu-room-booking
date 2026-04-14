cmake_minimum_required(VERSION 3.15)

project(common)

add_library(common_lib STATIC
    cache/redis_async_client.cpp
    database/postgreSQL_async_client.cpp
    database/postgreSQL_connection_pool.cpp
    messaging/nats_async_client.cpp
    metrics/MetricsRegistry.cpp
)

target_include_directories(common_lib PUBLIC
    ${CMAKE_CURRENT_SOURCE_DIR}
    ${CMAKE_CURRENT_SOURCE_DIR}/metrics
    ${CMAKE_SOURCE_DIR}/include
    ${CMAKE_SOURCE_DIR}/include/genproto
)

find_package(Threads REQUIRED)
find_package(PkgConfig REQUIRED)
pkg_check_modules(REDIS REQUIRED hiredis)
find_package(PostgreSQL REQUIRED)

target_include_directories(common_lib PRIVATE
    ${REDIS_INCLUDE_DIRS}
    ${PostgreSQL_INCLUDE_DIRS}
)

target_link_libraries(common_lib PUBLIC
    protos_lib
    logger_lib
    ${REDIS_LIBRARIES}
    PostgreSQL::PostgreSQL
    nats_static 
    Threads::Threads
    dl
    prometheus-cpp::pull
    prometheus-cpp::core
)

set_target_properties(common_lib PROPERTIES
    ARCHIVE_OUTPUT_DIRECTORY ${CMAKE_BINARY_DIR}/lib
)