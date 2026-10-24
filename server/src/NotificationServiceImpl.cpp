#include "NotificationServiceImpl.h"

#include <chrono>
#include <cstdlib>
#include <random>
#include <utility>

namespace Notify {
    namespace {
        std::string generateHexToken(std::size_t length = 32) {
            // Prototype token generator. Replace with a
            // cryptographically secure RNG before deployment.
            static thread_local std::random_device random_device;
            static thread_local std::mt19937 generator(random_device());

            static constexpr char hex[] = "0123456789abcdef";
            std::uniform_int_distribution<int> distribution(0, 15);

            std::string token;
            token.reserve(length);

            for (std::size_t i = 0; i < length; ++i) {
                token.push_back(hex[distribution(generator)]);
            }

            return token;
        }

        std::int64_t currentTimeUnixMs() {
            using namespace std::chrono;

            return duration_cast<milliseconds>(
                system_clock::now().time_since_epoch()
            ).count();
        }
    } // namespace

    NotificationServiceImpl::NotificationServiceImpl() {
        // Configure LAN_NOTIFY_ADMIN_TOKEN in the server environment.
        if (const char* token = std::getenv("LAN_NOTIFY_ADMIN_TOKEN")) {
            admin_token_ = token;
        }
    }

    bool NotificationServiceImpl::isValidDevice(
        const std::string& deviceId,
        const std::string& deviceToken
    ) const {
        std::lock_guard lock(registered_devices_mutex_);

        const auto it = registered_devices_.find(deviceId);

        return it != registered_devices_.end()
                && it->second.device_token == deviceToken;
    }

    grpc::Status NotificationServiceImpl::RegisterDevice(
        grpc::ServerContext* /*context*/,
        const notification::v1::RegisterDeviceRequest* request,
        notification::v1::RegisterDeviceResponse* response
    ) {
        if (request->device_name().empty()) {
            return {
                grpc::StatusCode::INVALID_ARGUMENT,
                "Device name must not be empty"
            };
        }

        RegisteredDevices device;
        device.device_id = generateHexToken(32);
        device.device_token = generateHexToken(64);
        device.device_name = request->device_name();

        {
            std::lock_guard lock(registered_devices_mutex_);

            // Avoid overwriting an existing device on a collision.
            while (registered_devices_.contains(device.device_id)) {
                device.device_id = generateHexToken(32);
            }

            registered_devices_.emplace(device.device_id, device);
        }

        response->set_device_id(device.device_id);
        response->set_device_token(device.device_token);

        return grpc::Status::OK;
    }

    std::vector<RegisteredDevices> NotificationServiceImpl::getRegisteredDevices() const {
        std::lock_guard lock(registered_devices_mutex_);
        std::vector<RegisteredDevices> devices;
        devices.reserve(registered_devices_.size());
        for (const auto& dev : registered_devices_ | std::views::values) {
            devices.push_back(dev);
        }
        return devices;
    }

    bool NotificationServiceImpl::sendNotificationDirect(
        const std::string& deviceId,
        const std::string& title,
        const std::string& body,
        std::string* outMessageId
    ) {
        {
            std::lock_guard lock(registered_devices_mutex_);
            if (!registered_devices_.contains(deviceId)) {
                return false;
            }
        }

        notification::v1::NotificationMessage notification;
        notification.set_message_id(generateHexToken(32));
        notification.set_device_id(deviceId);
        notification.set_title(title);
        notification.set_body(body);
        notification.set_created_unix_ms(currentTimeUnixMs());

        if (outMessageId) {
            *outMessageId = notification.message_id();
        }

        return connection_manager_.queueNotification(deviceId, notification);
    }

    grpc::Status NotificationServiceImpl::Subscribe(
        grpc::ServerContext* context,
        const notification::v1::SubscribeRequest* request,
        grpc::ServerWriter<notification::v1::NotificationMessage>* writer
    ) {
        if (!isValidDevice(request->device_id(), request->device_token())) {
            return {
                grpc::StatusCode::UNAUTHENTICATED,
                "Invalid device credentials"
            };
        }

        auto connection = connection_manager_.open(request->device_id());

        while (!context->IsCancelled()) {
            notification::v1::NotificationMessage notification;
            bool hasNotification = false;

            {
                std::unique_lock lock(connection->connection_mutex);

                // Timed waits allow us to notice RPC cancellation
                // even when no notification arrives.
                connection->condition.wait_for(
                    lock,
                    std::chrono::milliseconds(200),
                    [&] {
                        return connection->closed
                                || !connection->notifications.empty();
                    }
                );

                if (connection->notifications.empty()) {
                    if (connection->closed) {
                        break;
                    }

                    continue;
                }

                notification =
                        std::move(connection->notifications.front());

                connection->notifications.pop_front();
                hasNotification = true;
            }

            // Never hold the connection mutex during a network write.
            if (hasNotification && !writer->Write(notification)) {
                break;
            }
        }

        connection_manager_.close(request->device_id(), connection);
        return grpc::Status::OK;
    }

    grpc::Status NotificationServiceImpl::SendNotification(
        grpc::ServerContext* /*context*/,
        const notification::v1::SendNotificationRequest* request,
        notification::v1::SendNotificationResponse* response
    ) {
        // if (admin_token_.empty()) {
        //     return {
        //         grpc::StatusCode::FAILED_PRECONDITION,
        //         "Server admin token is not configured"
        //     };
        // }
        //
        // if (request->admin_token() != admin_token_) {
        //     return {
        //         grpc::StatusCode::UNAUTHENTICATED,
        //         "Invalid admin token"
        //     };
        // }

        if (request->device_id().empty() || request->title().empty()) {
            return {
                grpc::StatusCode::INVALID_ARGUMENT,
                "Device ID and notification title are required"
            };
        }

        {
            std::lock_guard lock(registered_devices_mutex_);

            if (!registered_devices_.contains(request->device_id())) {
                return {
                    grpc::StatusCode::NOT_FOUND,
                    "Device is not registered"
                };
            }
        }

        notification::v1::NotificationMessage notification;
        notification.set_message_id(generateHexToken(32));
        notification.set_device_id(request->device_id());
        notification.set_title(request->title());
        notification.set_body(request->body());
        notification.set_created_unix_ms(currentTimeUnixMs());

        for (const auto& [key, value] : request->data()) {
            (*notification.mutable_data())[key] = value;
        }

        const bool queued = connection_manager_.queueNotification(
            request->device_id(),
            notification
        );

        response->set_message_id(notification.message_id());
        response->set_accepted(queued);

        // This prototype accepts delivery only when an active
        // subscription can be found and its queue accepts the message.
        return grpc::Status::OK;
    }
} // namespace Notify
