package dev.soham.lannotify

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startForegroundService
import dev.soham.lannotify.grpc.DeviceCredentials
import dev.soham.lannotify.grpc.NotificationClient
import dev.soham.lannotify.notifications.AndroidNotifier
import dev.soham.lannotify.service.NotificationConnectionService
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { NotificationHome() } }
    }
}

@Composable
private fun NotificationHome() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Replace with your computer's LAN IPv4 address.
    var host by remember { mutableStateOf("192.168.1.5") }
    var port by remember { mutableStateOf("50051") }
    var deviceName by remember { mutableStateOf("My Android Phone") }
    var status by remember { mutableStateOf("Not connected") }
    var credentials by remember { mutableStateOf<DeviceCredentials?>(null) }
    var busy by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        status = if (granted) "Notification permission granted" else "Notifications are disabled in Android settings"
    }

    LaunchedEffect(Unit) {
        AndroidNotifier(context).createChannels()
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("LAN Notifications", style = MaterialTheme.typography.headlineMedium)
            Text("Your own gRPC notification client")
            OutlinedTextField(host, { host = it }, label = { Text("Server LAN IP") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(port, { port = it }, label = { Text("Port") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(deviceName, { deviceName = it }, label = { Text("Device name") }, modifier = Modifier.fillMaxWidth())

            Button(
                enabled = !busy && credentials == null,
                onClick = {
                    busy = true
                    status = "Registering…"
                    scope.launch {
                        try {
                            val client = NotificationClient(host, port.toIntOrNull() ?: 50051, context)
                            val registered = client.register(deviceName)
                            client.close()
                            credentials = registered
                            status = "Registered. Tap Start connection."
                        } catch (t: Throwable) {
                            status = "Registration failed: ${t.message}"
                        } finally {
                            busy = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Register device") }

            credentials?.let {
                Text("Device ID: ${it.deviceId}")
                Text("Device token: ${it.deviceToken}")
                Text("Save these credentials; this starter keeps them only in memory.")

                Log.w("NotifyConnection", "Device ID: ${it.deviceId}")
                Log.w("NotifyConnection", "Device token: ${it.deviceToken}")
                Button(
                    onClick = {
                        val intent = Intent(context, NotificationConnectionService::class.java).apply {
                            putExtra(NotificationConnectionService.EXTRA_HOST, host)
                            putExtra(NotificationConnectionService.EXTRA_PORT, port.toIntOrNull() ?: 50051)
                            putExtra(NotificationConnectionService.EXTRA_DEVICE_ID, it.deviceId)
                            putExtra(NotificationConnectionService.EXTRA_DEVICE_TOKEN, it.deviceToken)
                        }
                        startForegroundService(context, intent)
                        status = "Connection service started"
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Start connection") }

                Button(
                    onClick = {
                        context.stopService(Intent(context, NotificationConnectionService::class.java))
                        status = "Connection stopped"
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Stop connection") }
            }
            Spacer(Modifier.height(4.dp))
            Text("Status: $status")
        }
    }
}
