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
    ${CMAKE_SOURCE_DIR}/src/ruz_importer/include
)

# Test for AvailabilityEngine
add_executable(test_availability_engine
    test_availability_engine.cpp
)

target_link_libraries(test_availability_engine PRIVATE
    GTest::gtest_main
)

# Test for PostgreSQL Async Client
add_executable(test_postgresql_async_client
    test_postgresql_async_client.cpp
)

target_link_libraries(test_postgresql_async_client PRIVATE
    GTest::gtest_main
    common_lib
)

# Test for PostgreSQL Connection Pool
add_executable(test_postgresql_connection_pool
    test_postgresql_connection_pool.cpp
)

target_link_libraries(test_postgresql_connection_pool PRIVATE
    GTest::gtest_main
    common_lib
)

# Test for Redis Async Client
add_executable(test_redis_async_client
    test_redis_async_client.cpp
)

target_link_libraries(test_redis_async_client PRIVATE
    GTest::gtest_main
    common_lib
    ${LIBEVENT_LIBRARIES}
)

# Test for NATS Client
add_executable(test_nats_client
    test_nats_client.cpp
)

target_link_libraries(test_nats_client PRIVATE
    GTest::gtest_main
    common_lib
)

# Test for RUZ Importer
add_executable(test_ruz_importer
    test_ruz_importer.cpp
)

target_link_libraries(test_ruz_importer PRIVATE
    GTest::gtest_main
    common_lib
)

# Test for RuzImporter Parsers
add_executable(test_parsers
    test_parsers.cpp
)

target_include_directories(test_parsers PRIVATE
    ${CMAKE_SOURCE_DIR}/src/ruz_importer/include
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
