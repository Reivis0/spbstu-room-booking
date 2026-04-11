find_package(Threads REQUIRED)

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