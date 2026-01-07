cmake_minimum_required(VERSION 3.15)

project(ruz_importer)

find_package(PkgConfig REQUIRED)
find_package(Threads REQUIRED)

find_package(CURL REQUIRED)
pkg_check_modules(JSONCPP REQUIRED jsoncpp)
pkg_check_modules(POSTGRESQL REQUIRED libpq)

add_executable(ruz_importer
    src/main.cpp
    src/ruz_importer.cpp
    src/http_client.cpp
    src/data_processor.cpp
    src/config.cpp
)

target_compile_definitions(ruz_importer PRIVATE
    CONFIG_PATH="${CMAKE_SOURCE_DIR}/include/configs")

target_include_directories(ruz_importer PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}/include
    ${CMAKE_SOURCE_DIR}/include
    ${CMAKE_SOURCE_DIR}/log

    ${CMAKE_SOURCE_DIR}/src/common/database
    ${CMAKE_SOURCE_DIR}/src/common/messaging
    ${CMAKE_SOURCE_DIR}/src/common/cache

    ${CURL_INCLUDE_DIRS}
    ${JSONCPP_INCLUDE_DIRS}
    ${POSTGRESQL_INCLUDE_DIRS}
)


target_link_libraries(ruz_importer PRIVATE
    common_lib
    logger_lib
    ${CURL_LIBRARIES}
    ${JSONCPP_LIBRARIES}
    ${POSTGRESQL_LIBRARIES}
    Threads::Threads
    dl
    ssl
    crypto
)

set_target_properties(ruz_importer PROPERTIES
    RUNTIME_OUTPUT_DIRECTORY ${CMAKE_BINARY_DIR}/bin
)