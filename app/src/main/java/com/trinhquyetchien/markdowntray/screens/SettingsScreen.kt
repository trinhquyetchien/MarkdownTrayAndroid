package com.trinhquyetchien.markdowntray.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trinhquyetchien.markdowntray.data.StorageManager
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    storageManager: StorageManager,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(storageManager.hasStoragePermission()) }

    var dailyDir by remember { mutableStateOf(storageManager.dailyNotesDir) }
    var dailyAlias by remember { mutableStateOf(storageManager.dailyAlias) }

    var deadlineFile by remember { mutableStateOf(storageManager.deadlineFilePath) }
    var deadlineAlias by remember { mutableStateOf(storageManager.deadlineAlias) }

    var calendarFile by remember { mutableStateOf(storageManager.calendarFilePath) }

    // Re-check permission on composition
    LaunchedEffect(Unit) {
        hasPermission = storageManager.hasStoragePermission()
    }

    val scrollState = rememberScrollState()

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Text(
                    text = "Settings",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Storage Permission Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (hasPermission) {
                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                    } else {
                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                    }
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = if (hasPermission) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (hasPermission) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (hasPermission) "Storage Access Active" else "Storage Permission Required",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = if (hasPermission) {
                                    "App can read and write files directly for Syncthing / Obsidian sync."
                                } else {
                                    "Please allow 'All files access' so MarkdownTray can modify your synced notes."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (!hasPermission) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                try {
                                    context.startActivity(storageManager.getRequestStoragePermissionIntent())
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Could not open settings: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Grant All Files Access")
                        }
                    }
                }
            }

            // Daily Notes Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Daily Notes Configuration",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    OutlinedTextField(
                        value = dailyAlias,
                        onValueChange = { dailyAlias = it },
                        label = { Text("Daily Title Alias") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = dailyDir,
                        onValueChange = { dailyDir = it },
                        label = { Text("Daily Notes Directory") },
                        singleLine = false,
                        modifier = Modifier.fillMaxWidth()
                    )

                    val dirExists = File(dailyDir).isDirectory
                    Text(
                        text = if (dirExists) "✓ Directory exists" else "⚠ Directory does not exist yet (will be created)",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (dirExists) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                    )
                }
            }

            // Deadline Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Deadline Note Configuration",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    OutlinedTextField(
                        value = deadlineAlias,
                        onValueChange = { deadlineAlias = it },
                        label = { Text("Deadline Title Alias") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = deadlineFile,
                        onValueChange = { deadlineFile = it },
                        label = { Text("Deadline File Path") },
                        singleLine = false,
                        modifier = Modifier.fillMaxWidth()
                    )

                    val fileExists = File(deadlineFile).isFile
                    Text(
                        text = if (fileExists) "✓ File exists" else "⚠ File does not exist yet (will be created)",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (fileExists) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                    )
                }
            }

            // Calendar Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Weekly Calendar Configuration",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    OutlinedTextField(
                        value = calendarFile,
                        onValueChange = { calendarFile = it },
                        label = { Text("Calendar Data JSON Path") },
                        singleLine = false,
                        modifier = Modifier.fillMaxWidth()
                    )

                    val calExists = File(calendarFile).isFile
                    Text(
                        text = if (calExists) "✓ Calendar JSON exists" else "⚠ Calendar JSON does not exist yet",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (calExists) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                    )
                }
            }

            // Save & Reset Actions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        dailyDir = StorageManager.DEFAULT_DAILY_DIR
                        dailyAlias = "Daily List"
                        deadlineFile = StorageManager.DEFAULT_DEADLINE_FILE
                        deadlineAlias = "Deadline"
                        calendarFile = StorageManager.DEFAULT_CALENDAR_FILE

                        storageManager.dailyNotesDir = dailyDir
                        storageManager.dailyAlias = dailyAlias
                        storageManager.deadlineFilePath = deadlineFile
                        storageManager.deadlineAlias = deadlineAlias
                        storageManager.calendarFilePath = calendarFile

                        Toast.makeText(context, "Reset to defaults", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.RestartAlt, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Defaults")
                }

                Button(
                    onClick = {
                        storageManager.dailyNotesDir = dailyDir.trim()
                        storageManager.dailyAlias = dailyAlias.trim().ifEmpty { "Daily List" }
                        storageManager.deadlineFilePath = deadlineFile.trim()
                        storageManager.deadlineAlias = deadlineAlias.trim().ifEmpty { "Deadline" }
                        storageManager.calendarFilePath = calendarFile.trim()

                        Toast.makeText(context, "Settings saved successfully", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
