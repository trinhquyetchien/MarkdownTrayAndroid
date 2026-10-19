package com.trinhquyetchien.markdowntray.screens

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.viewinterop.AndroidView
import com.trinhquyetchien.markdowntray.data.StorageManager
import org.json.JSONArray
import org.json.JSONObject

class CalendarWebBridge(
    private val storageManager: StorageManager,
    private val onDataSaved: () -> Unit
) {
    private val mainHandler = Handler(Looper.getMainLooper())

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

    fun refreshCalendar() {
        val file = storageManager.getCalendarFile()
        val json = if (file.exists()) file.readText(Charsets.UTF_8).trim() else "[]"
        val jsSafeJson = JSONObject.quote(json)
        webViewRef?.evaluateJavascript("loadData($jsSafeJson);", null)
    }

    val bridge = remember {
        CalendarWebBridge(storageManager) {
            Toast.makeText(context, "Calendar saved", Toast.LENGTH_SHORT).show()
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Text(
                    text = "Calendar",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
            },
            actions = {
                IconButton(onClick = {
                    refreshCalendar()
                    Toast.makeText(context, "Calendar refreshed", Toast.LENGTH_SHORT).show()
                }) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh calendar",
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
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.allowFileAccess = true
                        settings.allowContentAccess = true
                        settings.useWideViewPort = true
                        settings.loadWithOverviewMode = true
                        settings.builtInZoomControls = true
                        settings.displayZoomControls = false

                        webChromeClient = WebChromeClient()
                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                val file = storageManager.getCalendarFile()
                                val json = if (file.exists()) file.readText(Charsets.UTF_8).trim() else "[]"
                                val jsSafeJson = JSONObject.quote(json)
                                view?.evaluateJavascript("loadData($jsSafeJson);", null)
                            }
                        }

                        addJavascriptInterface(bridge, "AndroidBridge")
                        loadUrl("file:///android_asset/calendar.html")
                        webViewRef = this
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
