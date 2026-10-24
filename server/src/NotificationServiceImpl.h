#pragma once

#include <mutex>
#include <string>
#include <unordered_map>

#include <grpcpp/grpcpp.h>

#include "notification.grpc.pb.h"

#include "ConnectionManager.h"

namespace Notify {
    struct RegisteredDevices {
        std::string device_id;
        std::string device_token;
        std::string device_name;
    };

    class NotificationServiceImpl final
            : public notification::v1::NotificationService::Service {
    public:
        NotificationServiceImpl();

        ~NotificationServiceImpl() override = default;

        grpc::Status SendNotification(
            grpc::ServerContext* context,
            const notification::v1::SendNotificationRequest* request,
            notification::v1::SendNotificationResponse* response
        ) override;

        grpc::Status Subscribe(
            grpc::ServerContext* context,
            const notification::v1::SubscribeRequest* request,
            grpc::ServerWriter<notification::v1::NotificationMessage>* writer
        ) override;

        grpc::Status RegisterDevice(
            grpc::ServerContext* context,
            const notification::v1::RegisterDeviceRequest* request,
            notification::v1::RegisterDeviceResponse* response
        ) override;

        std::vector<RegisteredDevices> getRegisteredDevices() const;

        bool sendNotificationDirect(
            const std::string& deviceId,
            const std::string& title,
            const std::string& body,
            std::string* outMessageId = nullptr
        );

    private:
        bool isValidDevice(
            const std::string& deviceId,
            const std::string& deviceToken
        ) const;

        std::string admin_token_;

        mutable std::mutex registered_devices_mutex_;
        std::unordered_map<std::string, RegisteredDevices>
        registered_devices_;

        ConnectionManager connection_manager_;
    };
} // namespace Notify
