cmake_minimum_required(VERSION 3.29.2)
project(LanNotify LANGUAGES C CXX)

file(GLOB_RECURSE NOTIFY_SOURCE src/*.cpp src/*.h)

set(CMAKE_CXX_STANDARD 23)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

include(grpc/fetch.cmake)
include(grpc/proto.cmake)

add_executable(NotifyServer
        src/NotificationServer.cpp
        src/NotificationServiceImpl.cpp
        ${NOTIFICATION_PROTO_SRCS}
        ${NOTIFICATION_PROTO_GRPC_SRCS}
)

target_include_directories(NotifyServer PRIVATE
        include
        "${CMAKE_CURRENT_BINARY_DIR}"
)

target_link_libraries(NotifyServer PRIVATE
        NOTIFICATION_GRPC_PROTO
        absl::check
        absl::flags
        absl::flags_parse
        absl::log
        absl::log_initialize
)
