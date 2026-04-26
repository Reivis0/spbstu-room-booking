find_package(GTest REQUIRED)
include_directories(${GTEST_INCLUDE_DIRS})

find_package(PkgConfig REQUIRED)
pkg_check_modules(JSONCPP REQUIRED jsoncpp)
pkg_check_modules(POSTGRESQL REQUIRED libpq)
find_package(CURL REQUIRED)

pkg_check_modules(CNATS cnats)
if (NOT CNATS_FOUND)
    find_path(CNATS_INCLUDE_DIR nats.h PATHS /usr/local/include /usr/include)
    if (CNATS_INCLUDE_DIR)
        include_directories(${CNATS_INCLUDE_DIR})
        message(STATUS "Found NATS headers manually at: ${CNATS_INCLUDE_DIR}")
    else()
        message(WARNING "NATS headers (nats.h) not found! Tests may fail to compile.")
    endif()
else()
    include_directories(${CNATS_INCLUDE_DIRS})
    link_directories(${CNATS_LIBRARY_DIRS})
endif()

include_directories(
    ${CMAKE_SOURCE_DIR}/include
    ${CMAKE_SOURCE_DIR}/include/genproto
    ${CMAKE_SOURCE_DIR}/src/ruz_importer/include
    ${CMAKE_SOURCE_DIR}/src/availability_engine/include
    ${CMAKE_SOURCE_DIR}/log            
    ${CMAKE_SOURCE_DIR}/src/common/database
    ${CMAKE_SOURCE_DIR}/src/common/messaging
    ${CMAKE_SOURCE_DIR}/src/common/cache
    
    ${JSONCPP_INCLUDE_DIRS}
    ${POSTGRESQL_INCLUDE_DIRS}
    ${CURL_INCLUDE_DIRS}
)


add_executable(ruz_importer_tests
    test_ruz_importer.cpp
    ${CMAKE_SOURCE_DIR}/src/ruz_importer/src/data_processor.cpp
    ${CMAKE_SOURCE_DIR}/src/ruz_importer/src/ruz_importer.cpp
    ${CMAKE_SOURCE_DIR}/src/ruz_importer/src/http_client.cpp
    ${CMAKE_SOURCE_DIR}/src/ruz_importer/src/config.cpp
    ${CMAKE_SOURCE_DIR}/log/logger.cpp 
    ${CMAKE_SOURCE_DIR}/src/common/database/postgreSQL_async_client.cpp
    ${CMAKE_SOURCE_DIR}/src/common/messaging/nats_async_client.cpp
)

target_link_libraries(ruz_importer_tests
    GTest::gtest_main
    GTest::gmock
    ${POSTGRESQL_LIBRARIES} 
    ${JSONCPP_LIBRARIES}   
    ${CURL_LIBRARIES}       
    pthread
    nats
    ssl crypto
)

add_test(NAME RuzImporterTests COMMAND ruz_importer_tests)


add_executable(availability_engine_tests
    test_availability_engine.cpp
    ${CMAKE_SOURCE_DIR}/src/availability_engine/src/async_room_service.cpp
    ${CMAKE_SOURCE_DIR}/log/logger.cpp
    ${CMAKE_SOURCE_DIR}/src/common/database/postgreSQL_async_client.cpp
    ${CMAKE_SOURCE_DIR}/src/common/cache/redis_async_client.cpp
    ${CMAKE_SOURCE_DIR}/src/common/messaging/nats_async_client.cpp
    ${CMAKE_SOURCE_DIR}/src/ruz_importer/src/config.cpp
)

target_link_libraries(availability_engine_tests
    GTest::gtest_main
    GTest::gmock
    ${POSTGRESQL_LIBRARIES}
    ${REDIS_LIBRARIES}
    pthread
    nats_static 
    ssl crypto
    common_lib
    event
)

add_test(NAME AvailabilityEngineTests COMMAND availability_engine_tests)
