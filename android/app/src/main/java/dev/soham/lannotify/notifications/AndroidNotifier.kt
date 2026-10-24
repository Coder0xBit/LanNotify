package dev.soham.lannotify.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dev.soham.lannotify.R

class AndroidNotifier(private val context: Context) {
    companion object {
        const val CHANNEL_ID = "remote_messages"
        const val CONNECTION_CHANNEL_ID = "connection_status"
    }

    fun createChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Remote messages", NotificationManager.IMPORTANCE_DEFAULT)
            )
            manager.createNotificationChannel(
                NotificationChannel(CONNECTION_CHANNEL_ID, "Connection status", NotificationManager.IMPORTANCE_MAX)
            )
        }
    }

    fun show(messageId: String, title: String, body: String) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
        ) return

        // Stable per-message ID prevents duplicate retries from creating duplicate notifications.
        val id = messageId.hashCode() and 0x7fffffff
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(id, notification)
    }

    fun connectionNotification() =
        NotificationCompat.Builder(context, CONNECTION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Notification server connection")
            .setContentText("Connecting to your LAN notification server")
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
}
