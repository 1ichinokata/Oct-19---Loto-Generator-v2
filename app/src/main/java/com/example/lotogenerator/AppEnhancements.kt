package com.example.lotogenerator

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.text.Collator
import java.time.LocalDate
import java.util.Locale

data class AppLanguage(val tag: String, val label: String)

object AppLanguageManager {
    private const val PREFS = "combined_loto_settings"
    private const val KEY = "app_language"

    fun currentTag(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY, Locale.getDefault().toLanguageTag())
            ?: "en"

    fun available(): List<AppLanguage> {
        val seen = linkedMapOf<String, AppLanguage>()
        Locale.getAvailableLocales().forEach { locale ->
            val tag = locale.toLanguageTag()
            if (tag.isBlank() || tag == "und" || locale.language.isBlank()) return@forEach
            val native = locale.getDisplayName(locale).replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(locale) else it.toString()
            }
            val english = locale.getDisplayName(Locale.ENGLISH)
            val label = if (native.equals(english, true)) native else "$native — $english"
            seen.putIfAbsent(tag, AppLanguage(tag, label))
        }
        val collator = Collator.getInstance(Locale.getDefault())
        return seen.values.sortedWith { a, b -> collator.compare(a.label, b.label) }
    }

    fun apply(context: Context, tag: String, recreate: Boolean = true) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY, tag).apply()
        val locale = Locale.forLanguageTag(tag)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        context.resources.updateConfiguration(config, context.resources.displayMetrics)
        if (recreate) (context as? Activity)?.recreate()
    }

    fun applySaved(context: Context) = apply(context, currentTag(context), false)
}

@Composable
fun LanguagePickerButton(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var open by remember { mutableStateOf(false) }
    val currentTag = remember { AppLanguageManager.currentTag(context) }
    val current = remember(currentTag) {
        Locale.forLanguageTag(currentTag).getDisplayName(Locale.forLanguageTag(currentTag))
    }

    OutlinedButton(onClick = { open = true }, modifier = modifier) {
        Text("Language: $current")
    }

    if (open) {
        val languages = remember { AppLanguageManager.available() }
        AlertDialog(
            onDismissRequest = { open = false },
            title = { Text("Choose language") },
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 430.dp)) {
                    items(languages, key = { it.tag }) { lang ->
                        Text(
                            text = lang.label,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    open = false
                                    AppLanguageManager.apply(context, lang.tag)
                                }
                                .padding(vertical = 10.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { open = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun AccessExpiryWarning() {
    val context = LocalContext.current
    var remaining by remember { mutableLongStateOf(AccessManager.getRemainingTime(context)) }
    val permanent = remember { AccessManager.isGodMode(context) }

    LaunchedEffect(permanent) {
        while (!permanent) {
            remaining = AccessManager.getRemainingTime(context)
            delay(1000)
        }
    }

    val threeDays = 3L * 24 * 60 * 60 * 1000
    if (!permanent && remaining in 1 until threeDays) {
        val totalSeconds = remaining / 1000
        val days = totalSeconds / 86400
        val hours = (totalSeconds % 86400) / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.errorContainer
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("⚠ Access expires soon", fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer)
                Text("${days}d %02d:%02d:%02d remaining".format(hours, minutes, seconds),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer)
            }
        }
    }
}

@Composable
fun DailyUpdatePrompt() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("app_update_prompt", Context.MODE_PRIVATE) }
    var latest by remember { mutableStateOf<String?>(null) }
    var show by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val today = LocalDate.now().toString()
        if (prefs.getString("dismissed_day", "") == today) return@LaunchedEffect
        try {
            val info = withContext(Dispatchers.IO) {
                val request = Request.Builder()
                    .url("https://api.github.com/repos/1ichinokata/Oct-19---Loto-Generator-v2/releases/latest")
                    .header("Accept", "application/vnd.github+json")
                    .header("User-Agent", "Furan-Lotto-Generator")
                    .build()
                OkHttpClient().newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@use null
                    val json = JSONObject(response.body?.string().orEmpty())
                    json.optString("tag_name", "").removePrefix("v").trim().takeIf { it.isNotBlank() }
                }
            } ?: return@LaunchedEffect

            val current = try {
                context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "0"
            } catch (_: Exception) { "0" }

            fun parts(v: String) = v.split(".").map { it.toIntOrNull() ?: 0 }
            fun newer(a: String, b: String): Boolean {
                val x = parts(a); val y = parts(b)
                for (i in 0 until maxOf(x.size, y.size)) {
                    val xi = x.getOrElse(i) { 0 }; val yi = y.getOrElse(i) { 0 }
                    if (xi != yi) return xi > yi
                }
                return false
            }
            if (newer(info, current)) {
                latest = info
                show = true
            }
        } catch (_: Exception) { }
    }

    if (show && latest != null) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Update Available") },
            text = { Text("Version ${latest} is available. You can update now or cancel and continue using this version.") },
            confirmButton = {
                Button(onClick = {
                    show = false
                    context.startActivity(Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://github.com/1ichinokata/Oct-19---Loto-Generator-v2/releases/latest")))
                }) { Text("Download / Update") }
            },
            dismissButton = {
                TextButton(onClick = {
                    prefs.edit().putString("dismissed_day", LocalDate.now().toString()).apply()
                    show = false
                }) { Text("Cancel") }
            }
        )
    }
}
