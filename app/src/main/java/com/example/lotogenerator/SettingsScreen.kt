package com.example.lotogenerator

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun SettingsScreen(darkMode: Boolean, onDarkModeChange: (Boolean) -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("combined_loto_settings", Context.MODE_PRIVATE) }
    var remainingMillis by remember { mutableLongStateOf(AccessManager.getRemainingTime(context)) }
    val permanent = remember { AccessManager.isGodMode(context) }

    LaunchedEffect(permanent) {
        while (!permanent) {
            remainingMillis = AccessManager.getRemainingTime(context)
            delay(1000L)
        }
    }

    Scaffold(
        topBar = {
            Surface(tonalElevation = 3.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") }
                    Text("Settings", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp)) {
                    Text("Appearance", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (darkMode) Icons.Default.DarkMode else Icons.Default.LightMode, null)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(if (darkMode) "Dark Mode" else "Light Mode", fontWeight = FontWeight.Bold)
                            Text("Choose the app color theme.", style = MaterialTheme.typography.bodySmall)
                        }
                        Switch(
                            checked = darkMode,
                            onCheckedChange = { enabled ->
                                prefs.edit().putBoolean("dark_mode", enabled).apply()
                                onDarkModeChange(enabled)
                            }
                        )
                    }
                }
            }

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp)) {
                    Text("App Access", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(10.dp))
                    if (permanent) {
                        Text("Permanent Access", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text("Your access does not expire.", style = MaterialTheme.typography.bodyMedium)
                    } else {
                        val totalSeconds = (remainingMillis / 1000L).coerceAtLeast(0L)
                        val days = totalSeconds / 86400L
                        val hours = (totalSeconds % 86400L) / 3600L
                        val minutes = (totalSeconds % 3600L) / 60L
                        val seconds = totalSeconds % 60L
                        Text("Time Remaining", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            "%d days  %02d:%02d:%02d".format(days, hours, minutes, seconds),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (remainingMillis > 0L) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                        Text(
                            if (remainingMillis > 0L) "Your access countdown updates every second." else "Your access has expired.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}
