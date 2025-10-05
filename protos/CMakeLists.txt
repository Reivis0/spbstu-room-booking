find_package(Protobuf REQUIRED)                           

set(PROTO_FILES
  ${CMAKE_CURRENT_SOURCE_DIR}/room_service/message.proto     
)

set(PROTO_GEN_DIR ${CMAKE_SOURCE_DIR}/protos/genproto)       
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

target_link_libraries(protos_lib PUBLIC protobuf::libprotobuf)
target_include_directories(protos_lib PUBLIC "${PROTO_GEN_DIR}")
