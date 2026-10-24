#include <iostream>
#include <memory>

#include <grpcpp/grpcpp.h>

#include "NotificationServiceImpl.h"

void runInteractiveShell(Notify::NotificationServiceImpl& service, grpc::Server* server) {
    std::string command;
    std::cout << "\nType 'help' for commands, or 'exit' to stop the server.\n";

    while (true) {
        std::cout << "lan-notify> ";
        if (!std::getline(std::cin, command))
            break;

        if (command == "exit" || command == "quit") {
            std::cout << "Shutting down server...\n";
            server->Shutdown();
            break;
        } else if (command == "help") {
            std::cout << "Commands:\n"
                    << "  send         - Send a notification to a device\n"
                    << "  devices      - List registered devices\n"
                    << "  exit / quit  - Stop the server\n";
        } else if (command == "devices") {
            auto devices = service.getRegisteredDevices();
            if (devices.empty()) {
                std::cout << "No devices registered yet.\n";
            } else {
                std::cout << "Registered Devices (" << devices.size() << "):\n";
                for (const auto& dev : devices) {
                    std::cout << "  - ID: " << dev.device_id
                            << " | Name: " << dev.device_name << "\n";
                }
            }
        } else if (command == "send") {
            std::string device_id, title, body;
            std::cout << "Device ID: ";
            std::getline(std::cin, device_id);
            std::cout << "Title: ";
            std::getline(std::cin, title);
            std::cout << "Body: ";
            std::getline(std::cin, body);

            if (device_id.empty() || title.empty()) {
                std::cout << "Error: Device ID and Title cannot be empty.\n";
                continue;
            }

            std::string msg_id;
            bool ok = service.sendNotificationDirect(device_id, title, body, &msg_id);
            if (ok) {
                std::cout << "[SUCCESS] Notification queued! Message ID: " << msg_id << "\n";
            } else {
                std::cout << "[FAILED] Device not found or not connected.\n";
            }
        } else if (!command.empty()) {
            std::cout << "Unknown command: '" << command << "'. Type 'help' for usage.\n";
        }
    }
}


int main() {
    constexpr const char* server_address = "192.168.1.5:50051";

    Notify::NotificationServiceImpl service;

    grpc::ServerBuilder builder;
    builder.AddListeningPort(
        server_address,
        grpc::InsecureServerCredentials()
    );
    builder.RegisterService(&service);

    std::unique_ptr<grpc::Server> server = builder.BuildAndStart();

    if (!server) {
        std::cerr << "Failed to start LanNotify server\n";
        return 1;
    }

    std::cout << "LanNotify server listening on "
            << server_address << '\n';

    runInteractiveShell(service, server.get());

    server->Wait();
    return 0;
}
