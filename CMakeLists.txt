cmake_minimum_required(VERSION 3.15)
project(spbstu_room_booking)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

include(FetchContent)
message(STATUS "Fetching NATS C client...")
FetchContent_Declare(
  cnats
  GIT_REPOSITORY https://github.com/nats-io/nats.c.git
  GIT_TAG        v3.8.2
)
set(NATS_BUILD_WITH_TLS OFF CACHE BOOL "" FORCE)
set(NATS_BUILD_STREAMING OFF CACHE BOOL "" FORCE)
set(NATS_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)

FetchContent_MakeAvailable(cnats)

add_subdirectory(log)
add_subdirectory(protos)
add_subdirectory(src)

set(CMAKE_RUNTIME_OUTPUT_DIRECTORY ${CMAKE_BINARY_DIR}/bin)
set(CMAKE_LIBRARY_OUTPUT_DIRECTORY ${CMAKE_BINARY_DIR}/lib)
set(CMAKE_ARCHIVE_OUTPUT_DIRECTORY ${CMAKE_BINARY_DIR}/lib)

find_package(GTest REQUIRED)
include_directories(${GTEST_INCLUDE_DIRS})

include_directories(${CMAKE_SOURCE_DIR}/include)
include_directories(${CMAKE_SOURCE_DIR}/src)
include_directories(${CMAKE_SOURCE_DIR}/log)

find_library(LIBPQ pq REQUIRED)

add_library(logger log/logger.cpp)

enable_testing()

add_executable(test_postgresql_connection_pool tests/test_postgresql_connection_pool.cpp)
target_link_libraries(test_postgresql_connection_pool PRIVATE common_lib ${GTEST_BOTH_LIBRARIES})
add_test(NAME PostgreSQLConnectionPoolTest COMMAND test_postgresql_connection_pool)
