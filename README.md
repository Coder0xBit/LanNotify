# LanNotify

**LanNotify** is a LAN-based notification system that lets a C++ server send notifications to registered Android devices over gRPC.

The project is built as a learning project to explore gRPC, Protocol Buffers, C++, Kotlin, and Android notification delivery.

## Features

- **C++ gRPC server** for device registration and notification delivery.
- **Interactive server shell** for managing registered devices and sending notifications.
- **Kotlin Android client** that receives notifications through a gRPC streaming connection.
- **Protocol Buffers** for communication between the server and clients.
- **LAN communication** without relying on Firebase Cloud Messaging (FCM).
- **CMake and FetchContent** for managing the C++ build and dependencies.

## Architecture

```text
                  Local Area Network
                          |
             +------------+------------+
             |                         |
       +-----v------+           +------v------+
       | LanNotify  |           |   Android   |
       |   Server   |<----------|    Client   |
       |   (C++)    |   gRPC    |   (Kotlin)  |
       +------------+           +-------------+
             |
       Interactive Shell
             |
       +-----+------+
       |            |
    devices        send
```

The server exposes a gRPC service, registers Android devices, and maintains active notification connections in memory. The interactive shell allows the operator to inspect registered devices and send notifications directly from the server terminal.

## Tech Stack

| Component | Technology |
|---|---|
| Server | C++ |
| RPC framework | gRPC |
| Interface definition | Protocol Buffers |
| Build system | CMake |
| Dependency management | FetchContent |
| Android client | Kotlin |
| Android notifications | Android Notification APIs |
| IDEs | CLion and Android Studio |

## Project Structure

```text
LanNotify/
├── android/                 # Android client
├── server/
│   ├── CMakeLists.txt
│   ├── grpc/                # gRPC dependency setup
│   └── src/
│       ├── main.cpp
│       ├── NotificationServiceImpl.h
│       └── NotificationServiceImpl.cpp
├── proto/                   # Protocol Buffer definitions
├── .gitignore
└── README.md
```

*The structure above is representative; adjust the paths to match your actual source tree.*

## Prerequisites

- A C++ compiler with C++20 or later support.
- CMake.
- Git.
- An internet connection for the initial FetchContent dependency download.
- CLion for the C++ server.
- Android Studio and the Android SDK for the Android client.

## Running the Server

1. Open the project in CLion.
2. Configure the CMake project and allow dependencies to download.
3. Build the server target.
4. Run the server executable.

The server listens on the configured address:

```text
192.168.1.5:50051
```

Change the `server_address` value in `main.cpp` to match the host computer's LAN IPv4 address.

For Android devices on the same network to connect, allow inbound TCP traffic on port `50051` in the host computer's firewall.

## Interactive Server Shell

After the server starts, an interactive prompt is available:

```text
LanNotify server listening on 192.168.1.5:50051

lan-notify>
```

### Available commands

| Command | Description |
|---|---|
| `help` | Display available commands. |
| `devices` | List registered devices and their IDs and names. |
| `send` | Send a notification to a registered device. |
| `exit` | Shut down the server. |
| `quit` | Shut down the server. |

### List registered devices

```text
lan-notify> devices
Registered Devices (1):
  - ID: <DEVICE_ID> | Name: My Android Phone
```

If no devices have registered, the shell reports that no devices are available.

### Send a notification

Enter `send` and follow the prompts:

```text
lan-notify> send
Device ID: <DEVICE_ID>
Title: Build completed
Body: Your Android build completed successfully.

[SUCCESS] Notification queued! Message ID: <MESSAGE_ID>
```

The shell sends the notification through the server's `sendNotificationDirect()` method. The target device must be registered and connected for the notification to be accepted.

### Stop the server

```text
lan-notify> exit
Shutting down server...
```

## Running the Android Client

1. Open the `android/` project in Android Studio.
2. Configure the server host to use the computer's LAN IP address and port `50051`.
3. Build and install the application on an Android device connected to the same network.
4. Register the device with the server.
5. Keep the application connected to receive notifications.

On Android 13 and later, grant the notification permission when prompted. Android's notification-channel settings also affect whether notifications appear as heads-up pop-ups.

## Current Limitations

- **LAN development only:** the server uses insecure gRPC credentials without TLS.
- **In-memory state:** registered devices and active connections are not persisted across server restarts.
- **No offline delivery:** notifications are not guaranteed to reach devices that are disconnected.
- **No admin authentication:** anyone able to reach the server may attempt to send notifications.
- **Android background restrictions:** Android may interrupt the connection depending on device settings and operating-system policies.

Do not expose this server to the public internet or use it to deliver sensitive information without adding appropriate security, authentication, and persistence.

## Future Improvements

- Add TLS and device authentication.
- Persist device registrations and notifications.
- Implement notification acknowledgements and offline delivery.
- Add delivery status and reconnection handling.
- Improve Android background reliability.

## Purpose

LanNotify is a hands-on project for learning C++, gRPC, Protocol Buffers, network communication, and Android development through a practical client-server application.
