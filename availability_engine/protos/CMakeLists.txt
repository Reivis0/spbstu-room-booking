find_package(Protobuf REQUIRED MODULE)
find_package(PkgConfig REQUIRED)
pkg_check_modules(gRPC REQUIRED grpc++ grpc)

# Find the gRPC C++ plugin executable
find_program(gRPC_CPP_PLUGIN_EXECUTABLE grpc_cpp_plugin)

if(NOT gRPC_CPP_PLUGIN_EXECUTABLE)
    message(FATAL_ERROR "gRPC C++ plugin not found")
endif()

set(PROTO_FILES
  ${CMAKE_CURRENT_SOURCE_DIR}/room_service/message.proto
)

set(PROTO_GEN_DIR ${CMAKE_SOURCE_DIR}/include/genproto)
file(MAKE_DIRECTORY "${PROTO_GEN_DIR}")

add_library(protos_lib STATIC)

# Ensure C++17 is used
target_compile_features(protos_lib PUBLIC cxx_std_17)

protobuf_generate(
  TARGET protos_lib
  LANGUAGE cpp
  PROTOC_OUT_DIR "${PROTO_GEN_DIR}"
  IMPORT_DIRS "${CMAKE_CURRENT_SOURCE_DIR}"
  APPEND_PATH
  PROTOS ${PROTO_FILES}
)

protobuf_generate(
  TARGET protos_lib
  LANGUAGE grpc
  PROTOC_OUT_DIR "${PROTO_GEN_DIR}"
  IMPORT_DIRS "${CMAKE_CURRENT_SOURCE_DIR}"
  APPEND_PATH
  GENERATE_EXTENSIONS .grpc.pb.h .grpc.pb.cc
  PLUGIN "protoc-gen-grpc=${gRPC_CPP_PLUGIN_EXECUTABLE}"
  PROTOS ${PROTO_FILES}
)

# Link against the libraries found by PkgConfig and Protobuf module
target_link_libraries(protos_lib PUBLIC ${Protobuf_LIBRARIES} ${gRPC_LIBRARIES})
target_include_directories(protos_lib PUBLIC "${PROTO_GEN_DIR}" ${Protobuf_INCLUDE_DIRS} ${gRPC_INCLUDE_DIRS})