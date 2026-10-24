# LanNotify

**A LAN-only notification server built for educational purposes.**

LanNotify is a learning project that demonstrates how to build a notification system using **C++, gRPC, and Android**. It consists of a C++ server that runs on a local network and an Android client that connects to the server to receive notifications.

The project explores client-server communication, gRPC streaming, Android foreground services, and real-time notification delivery over a local network.

> **Note:** LanNotify is an educational prototype intended for use on a trusted local area network (LAN). It is not designed or hardened for production use.

## Features

- **LAN-based communication:** Communicate between the server and Android device over the same local network.
- **gRPC:** Use gRPC and Protocol Buffers for communication between the C++ server and Android client.
- **Server-streaming RPC:** Maintain a subscription through which the server can send notifications to connected Android clients.
- **Terminal notification sender:** Send notifications from a command-line application.
- **Android notifications:** Display incoming messages using Android's notification system.
- **Connection management:** Track active client subscriptions and queue notifications in memory.
- **Automatic reconnection:** The Android connection service attempts to reconnect when the stream disconnects.

## Tech Stack

| Component | Technology |
|---|---|
| Server | C++ |
| Build system | CMake |
| RPC framework | gRPC |
| Serialization | Protocol Buffers |
| Android client | Kotlin |
| Android communication | gRPC |
| Android development | Android Studio |
| C++ development | CLion |
| Current storage | In-memory |

## Architecture

```text
                Local Area Network (LAN)
        ┌─────────────────────────────────┐
        │                                 │
        │  ┌───────────────────────────┐  │
        │  │     C++ gRPC Server       │  │
        │  │          CLion             │  │
        │  │                           │  │
        │  │  NotificationService      │  │
        │  │  ConnectionManager        │  │
        │  └─────────────┬─────────────┘  │
        │                │                │
        │          gRPC / HTTP/2          │
        │                │                │
        │  ┌─────────────▼─────────────┐  │
        │  │      Android Client      │  │
        │  │      Android Studio      │  │
        │  │                           │  │
        │  │  Connection Service      │  │
        │  │  Notification Handler   │  │
        │  └───────────────────────────┘  │
        │                                 │
        └─────────────────────────────────┘
```

The Android client connects to the server and subscribes to its notification stream. The terminal sender uses a separate gRPC client to submit notification requests to the server, which forwards them to an active device subscription.

## Project Structure

The project contains the following logical components:

- **Server:** C++ gRPC service, connection management, and server entry point.
- **Sender:** C++ command-line application for sending notifications.
- **Android:** Kotlin application that registers a device, subscribes to notifications, and displays them.
- **Proto:** Shared Protocol Buffers definitions used to generate the gRPC interfaces for both platforms.

## Getting Started

### Prerequisites

- [CLion](https://www.jetbrains.com/clion/) for the C++ server and sender.
- [Android Studio](https://developer.android.com/studio) for the Android application.
- A C++ compiler and CMake.
- Git for fetching dependencies.
- An Android device or emulator.
- A local network that allows communication between the server and Android device.

### 1. Run the C++ server

1. Open the server project in CLion.
2. Configure the CMake project and fetch its dependencies.
3. Build the `LanNotifyServer` target.
4. Run the server.

The server listens on the configured address and port. For example:

```text
0.0.0.0:50051
```

Ensure that the operating system firewall allows inbound connections on the chosen port from your trusted local network.

### 2. Run the Android client

1. Open the Android project in Android Studio.
2. Allow Gradle to synchronize and resolve dependencies.
3. Build and run the application on an Android device.
4. Configure the server's LAN IP address and port.
5. Register the device and establish the notification subscription.

For example, if the server's LAN address is `192.168.1.5` and its port is `50051`, configure the Android client to connect to that address.

**Important:** The example IP is specific to a local network configuration. Your server's actual address may differ.

### 3. Send a notification from the terminal

Build and run the `LanNotifySender` target from CLion or your build directory. The sender prompts for the server address, port, device ID, notification title, and message body.

Once the Android client is connected, the server can forward the notification through the active gRPC stream.

## How It Works

1. The Android client registers with the server and receives device credentials.
2. The Android connection service opens a server-streaming gRPC subscription.
3. The terminal sender submits a notification request using gRPC.
4. The server passes the notification to the active connection for the target device.
5. The Android client receives the message and displays a system notification.

The current implementation uses in-memory state. Notifications are not guaranteed to survive server restarts or to be delivered while a device is offline.

## Educational Goals

This project is intended to explore:

- C++ client-server application design.
- gRPC service definitions and Protocol Buffers.
- Unary and server-streaming RPCs.
- Concurrent connection and message-queue management.
- Kotlin coroutines and Android foreground services.
- Cross-platform communication between C++ and Android.
- Local-network application development.

## Limitations and Security

LanNotify is currently a prototype for learning and experimentation.

- It is intended for a trusted LAN, not direct exposure to the public internet.
- The current gRPC setup uses insecure channel credentials and does not provide transport encryption.
- The prototype does not yet provide durable notification storage or reliable offline delivery.
- In-memory registrations and queued notifications can be lost when the server stops.
- Authentication, authorization, and credential management require further hardening.

Do not use this prototype to transmit sensitive information or expose it to untrusted networks.

## Future Improvements

- Persistent device registration and notification storage using SQLite.
- Reliable delivery and message acknowledgements.
- Notification replay after reconnection.
- Improved authentication and secure transport using TLS.
- Better connection lifecycle management and graceful shutdown.
- Improved CLI experience and error reporting.

## License

Add a license if you intend to distribute or reuse this project publicly.

---

**LanNotify — learning how to build a local notification system with C++, gRPC, and Android.**