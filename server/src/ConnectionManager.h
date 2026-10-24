#pragma once

#include <condition_variable>
#include <deque>
#include <memory>
#include <mutex>
#include <string>
#include <unordered_map>
#include <utility>

#include "notification.pb.h"

namespace Notify {
    class ConnectionManager {
    public:
        using Notification = notification::v1::NotificationMessage;

        struct Connection {
            std::mutex connection_mutex;
            std::condition_variable condition;
            std::deque<Notification> notifications;
            bool closed = false;
        };

        using ConnectionPtr = std::shared_ptr<Connection>;

        ConnectionPtr open(const std::string& deviceId) {
            auto connection = std::make_shared<Connection>();

            std::lock_guard lock(mutex_);
            connections_[deviceId] = connection;

            return connection;
        }

        void close(
            const std::string& deviceId,
            const ConnectionPtr& connection
        ) {
            {
                std::lock_guard lock(mutex_);

                const auto it = connections_.find(deviceId);

                // Don't erase a newer connection after a reconnect.
                if (it != connections_.end() &&
                    it->second == connection) {
                    connections_.erase(it);
                }
            }

            {
                std::lock_guard lock(connection->connection_mutex);
                connection->closed = true;
            }

            connection->condition.notify_all();
        }

        bool queueNotification(
            const std::string& deviceId,
            const Notification& notification
        ) {
            ConnectionPtr connection;

            {
                std::lock_guard lock(mutex_);

                const auto it = connections_.find(deviceId);
                if (it == connections_.end()) {
                    return false;
                }

                connection = it->second;
            }

            {
                std::lock_guard lock(connection->connection_mutex);

                if (connection->closed) {
                    return false;
                }

                connection->notifications.push_back(notification);
            }

            connection->condition.notify_one();
            return true;
        }

    private:
        std::mutex mutex_;
        std::unordered_map<std::string, ConnectionPtr> connections_;
    };
} // namespace Notify
