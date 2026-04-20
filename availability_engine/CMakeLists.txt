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

message(STATUS "Fetching Prometheus-cpp...")
FetchContent_Declare(
  prometheus-cpp
  GIT_REPOSITORY https://github.com/jupp0r/prometheus-cpp.git
  GIT_TAG        v1.2.4
)
set(ENABLE_PUSH OFF CACHE BOOL "" FORCE)
set(ENABLE_COMPRESSION OFF CACHE BOOL "" FORCE)
set(ENABLE_TESTING OFF CACHE BOOL "" FORCE)
FetchContent_MakeAvailable(prometheus-cpp)

add_subdirectory(log)
add_subdirectory(protos)
add_subdirectory(src)
# add_subdirectory(tests)

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
