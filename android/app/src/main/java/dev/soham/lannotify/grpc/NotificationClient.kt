package dev.soham.lannotify.grpc

import android.content.Context
import dev.soham.lannotify.notifications.AndroidNotifier
import dev.soham.lannotify.proto.AcknowledgeRequest
import dev.soham.lannotify.proto.RegisterDeviceRequest
import dev.soham.lannotify.proto.SubscribeRequest
import dev.soham.lannotify.proto.NotificationServiceGrpc
import io.grpc.ManagedChannel
import io.grpc.okhttp.OkHttpChannelBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class DeviceCredentials(val deviceId: String, val deviceToken: String)

class NotificationClient(
    private val host: String,
    private val port: Int,
    private val context: Context
) {
    private val channel: ManagedChannel =
        OkHttpChannelBuilder.forAddress(host, port).usePlaintext().build()
    private val stub = NotificationServiceGrpc.newBlockingStub(channel)
    private val notifier = AndroidNotifier(context)

    suspend fun register(deviceName: String): DeviceCredentials = withContext(Dispatchers.IO) {
        val response = stub.registerDevice(RegisterDeviceRequest.newBuilder().setDeviceName(deviceName).build())
        DeviceCredentials(response.deviceId, response.deviceToken)
    }

    suspend fun subscribe(credentials: DeviceCredentials) = withContext(Dispatchers.IO) {
        notifier.createChannels()
        val request = SubscribeRequest.newBuilder()
            .setDeviceId(credentials.deviceId)
            .setDeviceToken(credentials.deviceToken)
            .build()

        // Blocking iterator runs on IO dispatcher; cancellation/stop closes the channel.
        val iterator = stub.subscribe(request)
        while (iterator.hasNext()) {
            val message = iterator.next()
            notifier.show(message.messageId, message.title, message.body)
            stub.acknowledge(
                AcknowledgeRequest.newBuilder()
                    .setDeviceId(credentials.deviceId)
                    .setDeviceToken(credentials.deviceToken)
                    .setMessageId(message.messageId)
                    .build()
            )
        }
    }

    fun close() {
        channel.shutdownNow()
    }
}
