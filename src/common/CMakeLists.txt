cmake_minimum_required(VERSION 3.15)

project(common)

add_library(common_lib STATIC
    cache/redis_async_client.cpp
    database/postgreSQL_async_client.cpp
    messaging/nats_async_client.cpp
)

target_include_directories(common_lib PUBLIC
    ${CMAKE_CURRENT_SOURCE_DIR}
    ${CMAKE_SOURCE_DIR}/include
    ${CMAKE_SOURCE_DIR}/include/genproto
)

find_package(Threads REQUIRED)
find_package(PkgConfig REQUIRED)
pkg_check_modules(REDIS REQUIRED hiredis)
pkg_check_modules(POSTGRESQL REQUIRED libpq)

target_include_directories(common_lib PRIVATE
    ${REDIS_INCLUDE_DIRS}
    ${POSTGRESQL_INCLUDE_DIRS}
)

target_link_libraries(common_lib PUBLIC
    protos_lib
    ${REDIS_LIBRARIES}
    ${POSTGRESQL_LIBRARIES}
    nats_static 
    Threads::Threads
    dl
)

set_target_properties(common_lib PROPERTIES
    ARCHIVE_OUTPUT_DIRECTORY ${CMAKE_BINARY_DIR}/lib
)