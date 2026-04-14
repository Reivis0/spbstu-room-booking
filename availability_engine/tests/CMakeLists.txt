find_package(Threads REQUIRED)
find_package(CURL REQUIRED)
find_package(PkgConfig REQUIRED)
pkg_check_modules(JSONCPP REQUIRED jsoncpp)
pkg_check_modules(POSTGRESQL REQUIRED libpq)

add_executable(test_availability
    test_availability_engine.cpp
)

target_include_directories(test_availability PRIVATE
    ${CMAKE_SOURCE_DIR}/include
    ${CMAKE_SOURCE_DIR}/src/common
    ${CMAKE_SOURCE_DIR}/src/availability_engine/include
)

target_sources(test_availability PRIVATE
    ${CMAKE_SOURCE_DIR}/src/availability_engine/src/async_room_service.cpp
)

target_link_libraries(test_availability PRIVATE
    common_lib
    Threads::Threads
    ${LIBEVENT_LIBRARIES}
    gtest
    gtest_main
)

set_target_properties(test_availability PROPERTIES
    RUNTIME_OUTPUT_DIRECTORY ${CMAKE_BINARY_DIR}/bin
)

# Test for RuzImporter
add_executable(test_ruz_importer
    test_ruz_importer.cpp
)

target_include_directories(test_ruz_importer PRIVATE
    ${CMAKE_SOURCE_DIR}/include
    ${CMAKE_SOURCE_DIR}/src/common
    ${CMAKE_SOURCE_DIR}/src/ruz_importer/include
    ${CURL_INCLUDE_DIRS}
    ${JSONCPP_INCLUDE_DIRS}
    ${POSTGRESQL_INCLUDE_DIRS}
)

target_sources(test_ruz_importer PRIVATE
    ${CMAKE_SOURCE_DIR}/src/ruz_importer/src/ruz_importer.cpp
    ${CMAKE_SOURCE_DIR}/src/ruz_importer/src/config.cpp
    ${CMAKE_SOURCE_DIR}/src/ruz_importer/src/http_client.cpp
    ${CMAKE_SOURCE_DIR}/src/ruz_importer/src/data_processor.cpp
)

target_link_libraries(test_ruz_importer PRIVATE
    common_lib
    logger_lib
    Threads::Threads
    ${CURL_LIBRARIES}
    ${JSONCPP_LIBRARIES}
    ${POSTGRESQL_LIBRARIES}
    gtest
    gtest_main
    ssl
    crypto
    dl
)

set_target_properties(test_ruz_importer PROPERTIES
    RUNTIME_OUTPUT_DIRECTORY ${CMAKE_BINARY_DIR}/bin
)