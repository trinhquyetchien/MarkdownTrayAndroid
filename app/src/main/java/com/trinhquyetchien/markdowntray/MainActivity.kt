package com.trinhquyetchien.markdowntray

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.core.content.ContextCompat
import com.trinhquyetchien.markdowntray.alarm.CalendarAlarmScheduler
import com.trinhquyetchien.markdowntray.data.StorageManager
import com.trinhquyetchien.markdowntray.screens.MainScreen
import com.trinhquyetchien.markdowntray.ui.theme.MarkdownTrayTheme

class MainActivity : ComponentActivity() {
    private lateinit var storageManager: StorageManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        storageManager = StorageManager(applicationContext)

        // Initialize calendar notification channel and alarms
        CalendarAlarmScheduler.createNotificationChannel(applicationContext)
        CalendarAlarmScheduler.scheduleUpcomingAlarms(applicationContext, storageManager)

        setContent {
            MarkdownTrayTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppRoot(storageManager = storageManager)
                }
            }
        }
    }
}

@Composable
private fun AppRoot(storageManager: StorageManager) {
    val context = LocalContext.current
    var hasStoragePermission by remember { mutableStateOf(storageManager.hasStoragePermission()) }
    var showStorageDialog by remember { mutableStateOf(!hasStoragePermission) }

    // Android 13+ Notification Permission Launcher
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val isGranted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!isGranted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    MainScreen(storageManager = storageManager)

    // First Launch Storage Access Dialog
    if (showStorageDialog && !hasStoragePermission) {
        AlertDialog(
            onDismissRequest = { showStorageDialog = false },
            title = {
                Text(
                    text = "Quyền truy cập thư mục ghi chú",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Markdown Tray cần quyền 'Truy cập tất cả tệp' (All files access) để có thể đọc, ghi và đồng bộ các file Markdown và Calendar trực tiếp từ thư mục Syncthing trên thiết bị của bạn."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showStorageDialog = false
                        try {
                            context.startActivity(storageManager.getRequestStoragePermissionIntent())
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                ) {
                    Text("Cấp quyền ngay")
                }
            },
            dismissButton = {
                TextButton(onClick = { showStorageDialog = false }) {
                    Text("Để sau")
                }
            }
        )
    }
}
