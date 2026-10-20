package com.trinhquyetchien.markdowntray.screens

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.trinhquyetchien.markdowntray.data.StorageManager
import com.trinhquyetchien.markdowntray.data.SyncStatusInfo
import java.io.File
import org.json.JSONArray
import org.json.JSONObject

class CalendarWebBridge(
    private val context: Context,
    private val storageManager: StorageManager,
    private val onDataSaved: () -> Unit
) {
    private val mainHandler = Handler(Looper.getMainLooper())

    @Suppress("DEPRECATION")
    @JavascriptInterface
    fun vibrate(durationMs: Long) {
        mainHandler.post {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                    vibratorManager?.defaultVibrator?.vibrate(
                        VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE)
                    )
                } else {
                    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator?.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
                    } else {
                        vibrator?.vibrate(durationMs)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    @JavascriptInterface
    fun getEventsJson(): String {
        return try {
            val file = storageManager.getCalendarFile()
            if (file.exists()) {
                val content = file.readText(Charsets.UTF_8).trim()
                if (content.isNotEmpty()) content else "[]"
            } else {
                "[]"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            "[]"
        }
    }

    @JavascriptInterface
    fun postMessage(message: String) {
        try {
            val obj = JSONObject(message)
            val action = obj.optString("action")
            if (action == "save_events") {
                val data = obj.get("data")
                val formattedJson = when (data) {
                    is JSONArray -> data.toString(2)
                    is JSONObject -> data.toString(2)
                    else -> data.toString()
                }
                val file = storageManager.getCalendarFile()
                storageManager.writeText(file, formattedJson)
                mainHandler.post {
                    onDataSaved()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun CalendarScreen(
    storageManager: StorageManager,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var syncInfo by remember { mutableStateOf<SyncStatusInfo?>(null) }

    fun buildInjectedHtml(): String {
        val baseHtml = try {
            context.assets.open("calendar.html").bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            Log.e("CalendarScreen", "Could not load calendar.html from assets", e)
            ""
        }
        val file = storageManager.getCalendarFile()
        val json = if (file.exists()) file.readText(Charsets.UTF_8).trim() else "[]"
        val jsSafeJson = JSONObject.quote(json)
        val injection = "try { loadData($jsSafeJson); } catch(e){ console.error('Initial loadData error', e); }"
        return baseHtml.replace("/* __INITIAL_EVENTS_DATA__ */", injection)
    }

    fun refreshCalendar() {
        val file = storageManager.getCalendarFile()
        syncInfo = storageManager.getFileSyncInfo(file)
        val json = if (file.exists()) file.readText(Charsets.UTF_8).trim() else "[]"
        val jsSafeJson = JSONObject.quote(json)
        webViewRef?.evaluateJavascript("if (typeof loadData === 'function') { loadData($jsSafeJson); }", null)
    }

    LaunchedEffect(storageManager.calendarFilePath) {
        syncInfo = storageManager.getFileSyncInfo(storageManager.getCalendarFile())
    }

    // Auto-reload via DirectoryObserver when Syncthing writes calendar.json
    DisposableEffect(storageManager.calendarFilePath) {
        val file = storageManager.getCalendarFile()
        val parentDir = file.parentFile ?: File(StorageManager.DEFAULT_STORAGE_ROOT)
        val observer = storageManager.createDirectoryObserver(parentDir) { changedPath ->
            if (changedPath == null || changedPath == file.name) {
                refreshCalendar()
            }
        }
        observer.start()
        onDispose {
            observer.stop()
        }
    }

    val bridge = remember {
        CalendarWebBridge(context, storageManager) {
            syncInfo = storageManager.getFileSyncInfo(storageManager.getCalendarFile())
            com.trinhquyetchien.markdowntray.alarm.CalendarAlarmScheduler.scheduleUpcomingAlarms(context, storageManager)
            Toast.makeText(context, "Đã lưu lịch biểu", Toast.LENGTH_SHORT).show()
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = "Lịch biểu",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )

                    // Sync Status pill
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { refreshCalendar() }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(
                                    if (syncInfo?.isSynced == true) Color(0xFF4CAF50)
                                    else Color(0xFFFF9800)
                                )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = syncInfo?.statusText ?: "Đang kiểm tra...",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            actions = {
                IconButton(onClick = {
                    refreshCalendar()
                    Toast.makeText(context, "Đã làm mới lịch", Toast.LENGTH_SHORT).show()
                }) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Làm mới",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )

        Box(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        setBackgroundColor(android.graphics.Color.parseColor("#1c1c1e"))

                        isHapticFeedbackEnabled = true
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.allowFileAccess = true
                        settings.allowContentAccess = true
                        @Suppress("DEPRECATION")
                        settings.allowFileAccessFromFileURLs = true
                        @Suppress("DEPRECATION")
                        settings.allowUniversalAccessFromFileURLs = true
                        settings.useWideViewPort = true
                        settings.loadWithOverviewMode = true
                        settings.builtInZoomControls = true
                        settings.displayZoomControls = false
                        settings.cacheMode = WebSettings.LOAD_NO_CACHE

                        webChromeClient = object : WebChromeClient() {
                            override fun onConsoleMessage(cm: ConsoleMessage?): Boolean {
                                Log.d("CalendarWebView", "${cm?.message()} -- line ${cm?.lineNumber()}")
                                return super.onConsoleMessage(cm)
                            }
                        }

                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                val file = storageManager.getCalendarFile()
                                syncInfo = storageManager.getFileSyncInfo(file)
                                val json = if (file.exists()) file.readText(Charsets.UTF_8).trim() else "[]"
                                val jsSafeJson = JSONObject.quote(json)
                                view?.evaluateJavascript("if (typeof loadData === 'function') { loadData($jsSafeJson); }", null)
                            }
                        }

                        addJavascriptInterface(bridge, "AndroidBridge")
                        val injectedHtml = buildInjectedHtml()
                        loadDataWithBaseURL("file:///android_asset/", injectedHtml, "text/html", "UTF-8", null)
                        webViewRef = this
                    }
                },
                update = { webView ->
                    webViewRef = webView
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
