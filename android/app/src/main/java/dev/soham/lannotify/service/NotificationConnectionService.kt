package dev.soham.lannotify.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import dev.soham.lannotify.grpc.DeviceCredentials
import dev.soham.lannotify.grpc.NotificationClient
import dev.soham.lannotify.notifications.AndroidNotifier
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class NotificationConnectionService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var connectionJob: Job? = null
    private var client: NotificationClient? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val host = intent?.getStringExtra(EXTRA_HOST) ?: return START_NOT_STICKY
        val port = intent.getIntExtra(EXTRA_PORT, 50051)
        val deviceId = intent.getStringExtra(EXTRA_DEVICE_ID) ?: return START_NOT_STICKY
        val token = intent.getStringExtra(EXTRA_DEVICE_TOKEN) ?: return START_NOT_STICKY

        val notifier = AndroidNotifier(this)
        notifier.createChannels()
        startForeground(1001, notifier.connectionNotification())

        connectionJob?.cancel()
        connectionJob = scope.launch {
            var backoffMs = 1_000L

            while (isActive) {
                val nextClient =
                    NotificationClient(host, port, this@NotificationConnectionService)

                client = nextClient

                try {
                    nextClient.subscribe(DeviceCredentials(deviceId, token))

                    // A long-lived subscription normally shouldn't finish
                    // unless the server closes it or the stream is cancelled.
                    if (isActive) {
                        Log.w("NotifyConnection", "Subscription ended")
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (t: Throwable) {
                    Log.w("NotifyConnection", "Stream disconnected", t)
                } finally {
                    nextClient.close()
                    if (client === nextClient) {
                        client = null
                    }
                }

                delay(backoffMs.milliseconds)
                backoffMs = (backoffMs * 2).coerceAtMost(30_000L)
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        connectionJob?.cancel()
        client?.close()
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val EXTRA_HOST = "host"
        const val EXTRA_PORT = "port"
        const val EXTRA_DEVICE_ID = "device_id"
        const val EXTRA_DEVICE_TOKEN = "device_token"
    }
}
