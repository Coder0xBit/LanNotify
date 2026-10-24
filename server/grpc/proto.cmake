cmake_minimum_required(VERSION 3.29.2)

get_filename_component(NOTIFICATION_PROTO  "${CMAKE_CURRENT_SOURCE_DIR}/../proto/notification/v1/notification.proto" ABSOLUTE)
get_filename_component(NOTIFICATION_PROTO_PATH "${NOTIFICATION_PROTO}" PATH)

# Generated sources
set(NOTIFICATION_PROTO_SRCS "${CMAKE_CURRENT_BINARY_DIR}/notification.pb.cc")
set(NOTIFICATION_PROTO_HDRS "${CMAKE_CURRENT_BINARY_DIR}/notification.pb.h")
set(NOTIFICATION_PROTO_GRPC_SRCS "${CMAKE_CURRENT_BINARY_DIR}/notification.grpc.pb.cc")
set(NOTIFICATION_PROTO_GRPC_HDRS "${CMAKE_CURRENT_BINARY_DIR}/notification.grpc.pb.h")
add_custom_command(
        OUTPUT "${NOTIFICATION_PROTO_SRCS}" "${NOTIFICATION_PROTO_HDRS}" "${NOTIFICATION_PROTO_GRPC_SRCS}" "${NOTIFICATION_PROTO_GRPC_HDRS}"
        COMMAND ${_PROTOBUF_PROTOC}
        ARGS --grpc_out "${CMAKE_CURRENT_BINARY_DIR}"
        --cpp_out "${CMAKE_CURRENT_BINARY_DIR}"
        -I "${NOTIFICATION_PROTO_PATH}"
        --plugin=protoc-gen-grpc="${_GRPC_CPP_PLUGIN_EXECUTABLE}"
        "${NOTIFICATION_PROTO}"
        DEPENDS "${NOTIFICATION_PROTO}")

include_directories("${CMAKE_CURRENT_BINARY_DIR}")

add_library(NOTIFICATION_GRPC_PROTO
        ${NOTIFICATION_PROTO_SRCS}
        ${NOTIFICATION_PROTO_HDRS}
        ${NOTIFICATION_PROTO_GRPC_SRCS}
        ${NOTIFICATION_PROTO_GRPC_HDRS})

target_link_libraries(NOTIFICATION_GRPC_PROTO
        absl::check
        ${_REFLECTION}
        ${_GRPC_GRPCPP}
        ${_PROTOBUF_LIBPROTOBUF})