cmake_minimum_required(VERSION 3.15)

project(availability_engine_tests)

find_package(PkgConfig REQUIRED)
find_package(GTest REQUIRED)

pkg_check_modules(JSONCPP REQUIRED jsoncpp)
pkg_check_modules(LIBEVENT REQUIRED libevent)
find_package(CURL REQUIRED)

include_directories(
    ${CMAKE_SOURCE_DIR}/include
    ${CMAKE_SOURCE_DIR}/log
    ${CMAKE_SOURCE_DIR}/src/common
    ${CMAKE_SOURCE_DIR}/src/ruz_importer/include
    ${CMAKE_SOURCE_DIR}/src/availability_engine/include
)

# Test for AvailabilityEngine
add_executable(test_availability_engine
    test_availability_engine.cpp
    ${CMAKE_SOURCE_DIR}/src/availability_engine/src/async_room_service.cpp
)

target_link_libraries(test_availability_engine PRIVATE
    GTest::gtest_main
    common_lib
    protos_lib
    logger_lib
    ${REDIS_LIBRARIES}
    ${POSTGRESQL_LIBRARIES}
    ${PQXX_LIBRARIES}
    nats_static
    ${LIBEVENT_LIBRARIES}
    event_pthreads
    Threads::Threads
    dl
)

# Test for PostgreSQL Async Client
add_executable(test_postgresql_async_client
    test_postgresql_async_client.cpp
)

target_link_libraries(test_postgresql_async_client PRIVATE
    GTest::gtest_main
    common_lib
    ${POSTGRESQL_LIBRARIES}
    Threads::Threads
)

# Test for PostgreSQL Connection Pool
add_executable(test_postgresql_connection_pool
    test_postgresql_connection_pool.cpp
)

target_link_libraries(test_postgresql_connection_pool PRIVATE
    GTest::gtest_main
    common_lib
    ${POSTGRESQL_LIBRARIES}
    Threads::Threads
)

# Test for Redis Async Client
add_executable(test_redis_async_client
    test_redis_async_client.cpp
)

target_link_libraries(test_redis_async_client PRIVATE
    GTest::gtest_main
    common_lib
    ${LIBEVENT_LIBRARIES}
    event_pthreads
    ${REDIS_LIBRARIES}
    Threads::Threads
)

# Test for NATS Client
add_executable(test_nats_client
    test_nats_client.cpp
)

target_link_libraries(test_nats_client PRIVATE
    GTest::gtest_main
    common_lib
    nats_static
    Threads::Threads
)

# Test for RUZ Importer
add_executable(test_ruz_importer
    test_ruz_importer.cpp
    ${CMAKE_SOURCE_DIR}/src/ruz_importer/src/ruz_importer.cpp
    ${CMAKE_SOURCE_DIR}/src/ruz_importer/src/http_client.cpp
    ${CMAKE_SOURCE_DIR}/src/ruz_importer/src/data_processor.cpp
    ${CMAKE_SOURCE_DIR}/src/ruz_importer/src/config.cpp
    ${CMAKE_SOURCE_DIR}/src/ruz_importer/src/spbptu_parser.cpp
    ${CMAKE_SOURCE_DIR}/src/ruz_importer/src/spbgu_parser.cpp
    ${CMAKE_SOURCE_DIR}/src/ruz_importer/src/leti_parser.cpp
)

target_include_directories(test_ruz_importer PRIVATE
    ${JSONCPP_INCLUDE_DIRS}
    ${CURL_INCLUDE_DIRS}
    ${POSTGRESQL_INCLUDE_DIRS}
)

target_link_libraries(test_ruz_importer PRIVATE
    GTest::gtest_main
    common_lib
    logger_lib
    nats_static
    ${CURL_LIBRARIES}
    ${JSONCPP_LIBRARIES}
    ${POSTGRESQL_LIBRARIES}
    ${REDIS_LIBRARIES}
    ${LIBEVENT_LIBRARIES}
    event_pthreads
    Threads::Threads
    ssl
    crypto
    dl
)

# Test for RuzImporter Parsers
add_executable(test_parsers
    test_parsers.cpp
)

target_include_directories(test_parsers PRIVATE
    ${CMAKE_SOURCE_DIR}/src/ruz_importer/include
    ${JSONCPP_INCLUDE_DIRS}
    ${CURL_INCLUDE_DIRS}
)

target_sources(test_parsers PRIVATE
    ${CMAKE_SOURCE_DIR}/src/ruz_importer/src/spbptu_parser.cpp
    ${CMAKE_SOURCE_DIR}/src/ruz_importer/src/spbgu_parser.cpp
    ${CMAKE_SOURCE_DIR}/src/ruz_importer/src/leti_parser.cpp
    ${CMAKE_SOURCE_DIR}/src/ruz_importer/src/http_client.cpp
)

target_link_libraries(test_parsers PRIVATE
    logger_lib
    ${JSONCPP_LIBRARIES}
    ${CURL_LIBRARIES}
    GTest::gtest_main
    ssl
    crypto
    dl
)

# Test for Scheduling Algorithm
add_executable(test_scheduling_algorithm
    test_scheduling_algorithm.cpp
)

target_link_libraries(test_scheduling_algorithm PRIVATE
    GTest::gtest_main
)

# Add all tests to ctest
add_test(NAME AvailabilityEngineTest COMMAND test_availability_engine)
add_test(NAME PostgreSQLAsyncClientTest COMMAND test_postgresql_async_client)
add_test(NAME PostgreSQLConnectionPoolTest COMMAND test_postgresql_connection_pool)
add_test(NAME RedisAsyncClientTest COMMAND test_redis_async_client)
add_test(NAME NATSClientTest COMMAND test_nats_client)
add_test(NAME RuzImporterTest COMMAND test_ruz_importer)
add_test(NAME RuzParsersTest COMMAND test_parsers)
add_test(NAME SchedulingAlgorithmTest COMMAND test_scheduling_algorithm)
