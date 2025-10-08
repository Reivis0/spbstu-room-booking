find_package(Protobuf REQUIRED)
find_package(gRPC CONFIG REQUIRED)

set(PROTO_FILES
  ${CMAKE_CURRENT_SOURCE_DIR}/room_service/message.proto
)

set(PROTO_GEN_DIR ${CMAKE_SOURCE_DIR}/include/genproto)
file(MAKE_DIRECTORY "${PROTO_GEN_DIR}") 

add_library(protos_lib STATIC)

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
  PLUGIN protoc-gen-grpc=$<TARGET_FILE:gRPC::grpc_cpp_plugin>
  PROTOS ${PROTO_FILES}
)

target_link_libraries(protos_lib PUBLIC protobuf::libprotobuf gRPC::grpc++)
target_include_directories(protos_lib PUBLIC "${PROTO_GEN_DIR}")
