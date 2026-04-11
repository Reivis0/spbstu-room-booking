cmake_minimum_required(VERSION 3.15)

project(availability_engine)

find_package(PkgConfig REQUIRED)
find_package(Threads REQUIRED)

pkg_check_modules(LIBEVENT REQUIRED libevent)
pkg_check_modules(REDIS REQUIRED hiredis)
pkg_check_modules(POSTGRESQL REQUIRED libpq)


file(COPY ${CMAKE_SOURCE_DIR}/include/configs/
     DESTINATION ${CMAKE_BINARY_DIR}/bin/configs)

add_executable(availability_engine
    src/main.cpp
    src/async_room_service.cpp
)

target_compile_definitions(availability_engine PRIVATE
    CONFIG_PATH="${CMAKE_SOURCE_DIR}/include/configs")

target_include_directories(availability_engine PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}/include
    ${CMAKE_SOURCE_DIR}/include
    ${CMAKE_SOURCE_DIR}/include/genproto
    ${CMAKE_SOURCE_DIR}/log
    ${CMAKE_SOURCE_DIR}/src/common
    ${REDIS_INCLUDE_DIRS}
    ${POSTGRESQL_INCLUDE_DIRS}
    ${LIBEVENT_INCLUDE_DIRS}
)

target_link_libraries(availability_engine PRIVATE
    protos_lib
    common_lib
    ${REDIS_LIBRARIES}
    ${POSTGRESQL_LIBRARIES}
    nats_static
    ${LIBEVENT_LIBRARIES}
    Threads::Threads
    dl
)

target_compile_options(availability_engine PRIVATE
    ${REDIS_CFLAGS_OTHER}
    ${POSTGRESQL_CFLAGS_OTHER}
    ${LIBEVENT_CFLAGS_OTHER}
)

set_target_properties(availability_engine PROPERTIES
    RUNTIME_OUTPUT_DIRECTORY ${CMAKE_BINARY_DIR}/bin
)
