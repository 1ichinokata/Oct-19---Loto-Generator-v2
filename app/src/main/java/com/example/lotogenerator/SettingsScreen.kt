package com.example.lotogenerator

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

@Composable
fun SettingsScreen(
    darkMode: Boolean,
    onDarkModeChange: (Boolean) -> Unit,
    onBack: () -> Unit,
    onRequireAccessCode: () -> Unit
) {
    val context = LocalContext.current

    val prefs = remember {
        context.getSharedPreferences(
            "combined_loto_settings",
            Context.MODE_PRIVATE
        )
    }

    var remainingMillis by remember {
        mutableLongStateOf(
            AccessManager.getRemainingTime(context)
        )
    }

    var trialRemainingMillis by remember {
        mutableLongStateOf(
            AccessManager.getTrialRemainingTime(context)
        )
    }

    val permanent =
        AccessManager.isGodMode(context)

    val paidAccess =
        AccessManager.isActivated(context)

    val trialActive =
        !paidAccess &&
                AccessManager.isTrialActive(context)

    // -----------------------------------------
    // PURCHASE STATE
    // -----------------------------------------

    var purchasing by remember {
        mutableStateOf(false)
    }

    val scope = rememberCoroutineScope()

    fun startPurchase(plan: String) {

        if (purchasing) return

        purchasing = true

        scope.launch {

            try {

                val deviceId =
                    Settings.Secure.getString(
                        context.contentResolver,
                        Settings.Secure.ANDROID_ID
                    ) ?: "unknown-device"

                val json =
                    JSONObject().apply {
                        put(
                            "device_id",
                            deviceId
                        )

                        put(
                            "plan",
                            plan
                        )
                    }

                val body =
                    json.toString()
                        .toRequestBody(
                            "application/json; charset=utf-8"
                                .toMediaType()
                        )

                val request =
                    Request.Builder()
                        .url(
                            "https://ghwtwpuyswthueqysvnx.supabase.co/" +
                                    "functions/v1/create-checkout-session"
                        )
                        .post(body)
                        .addHeader(
                            "Content-Type",
                            "application/json"
                        )
                        .addHeader(
                            "apikey",
                            "sb_publishable_go_fk2r0PMvz1vcr1QJ_Iw_UjnlzdD9"
                        )
                        .addHeader(
                            "Authorization",
                            "Bearer sb_publishable_go_fk2r0PMvz1vcr1QJ_Iw_UjnlzdD9"
                        )
                        .build()

                val checkoutUrl =
                    withContext(Dispatchers.IO) {

                        OkHttpClient()
                            .newCall(request)
                            .execute()
                            .use { response ->

                                val responseBody =
                                    response.body
                                        ?.string()
                                        .orEmpty()

                                if (!response.isSuccessful) {
                                    throw Exception(
                                        "Checkout request failed: HTTP ${response.code}"
                                    )
                                }

                                val result =
                                    JSONObject(
                                        responseBody
                                    )

                                if (
                                    !result.optBoolean(
                                        "success",
                                        false
                                    )
                                ) {
                                    throw Exception(
                                        result.optString(
                                            "message",
                                            "Could not start checkout."
                                        )
                                    )
                                }

                                result.optString(
                                    "checkout_url",
                                    ""
                                )
                            }
                    }

                if (checkoutUrl.isBlank()) {
                    throw Exception(
                        "Stripe checkout URL was not returned."
                    )
                }

                context.startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse(checkoutUrl)
                    )
                )

            } catch (e: Exception) {

                Toast.makeText(
                    context,
                    e.message
                        ?: "Unable to start purchase.",
                    Toast.LENGTH_LONG
                ).show()

            } finally {

                purchasing = false
            }
        }
    }

    // -----------------------------------------
    // APP UPDATE STATE
    // -----------------------------------------

    var checkingUpdate by remember {
        mutableStateOf(false)
    }

    var updateAvailable by remember {
        mutableStateOf(false)
    }

    var latestVersion by remember {
        mutableStateOf("")
    }

    var updateMessage by remember {
        mutableStateOf("")
    }

    // -----------------------------------------
    // CURRENT VERSION
    // -----------------------------------------

    val currentVersion = remember {

        try {

            context.packageManager
                .getPackageInfo(
                    context.packageName,
                    0
                )
                .versionName
                ?: "Unknown"

        } catch (_: Exception) {

            "Unknown"
        }
    }

    var versionTapCount by remember {
        mutableIntStateOf(0)
    }

    // -----------------------------------------
    // ACCESS COUNTDOWN
    // -----------------------------------------

    LaunchedEffect(
        permanent,
        trialActive
    ) {

        while (!permanent) {

            if (trialActive) {

                trialRemainingMillis =
                    AccessManager.getTrialRemainingTime(
                        context
                    )

            } else {

                remainingMillis =
                    AccessManager.getRemainingTime(
                        context
                    )
            }

            delay(1000L)
        }
    }

    // -----------------------------------------
    // UPDATE CHECK
    // -----------------------------------------

    fun checkForUpdates() {

        if (checkingUpdate) return

        checkingUpdate = true
        updateAvailable = false
        latestVersion = ""
        updateMessage = ""

        scope.launch {

            try {

                val result =
                    withContext(Dispatchers.IO) {

                        val client =
                            OkHttpClient()

                        val request =
                            Request.Builder()
                                .url(
                                    "https://api.github.com/repos/" +
                                            "1ichinokata/Oct-19---Loto-Generator-v2/releases/latest"
                                )
                                .header(
                                    "Accept",
                                    "application/vnd.github+json"
                                )
                                .header(
                                    "User-Agent",
                                    "Furan-Lotto-Generator"
                                )
                                .build()

                        client
                            .newCall(request)
                            .execute()
                            .use { response ->

                                if (!response.isSuccessful) {

                                    throw Exception(
                                        "GitHub returned HTTP ${response.code}"
                                    )
                                }

                                val body =
                                    response.body
                                        ?.string()
                                        ?: throw Exception(
                                            "Empty response from GitHub"
                                        )

                                val json =
                                    JSONObject(body)

                                val tagName =
                                    json
                                        .optString(
                                            "tag_name",
                                            ""
                                        )
                                        .removePrefix("v")
                                        .trim()

                                if (tagName.isBlank()) {

                                    throw Exception(
                                        "GitHub release version was not found"
                                    )
                                }

                                tagName
                            }
                    }

                latestVersion = result

                val currentParts =
                    currentVersion
                        .split(".")
                        .mapNotNull {
                            it.toIntOrNull()
                        }

                val latestParts =
                    latestVersion
                        .split(".")
                        .mapNotNull {
                            it.toIntOrNull()
                        }

                fun versionToNumber(
                    parts: List<Int>
                ): Long {

                    val major =
                        parts.getOrElse(0) {
                            0
                        }

                    val minor =
                        parts.getOrElse(1) {
                            0
                        }

                    val patch =
                        parts.getOrElse(2) {
                            0
                        }

                    return major * 1_000_000L +
                            minor * 1_000L +
                            patch
                }

                val currentNumber =
                    versionToNumber(
                        currentParts
                    )

                val latestNumber =
                    versionToNumber(
                        latestParts
                    )

                if (
                    latestNumber >
                    currentNumber
                ) {

                    updateAvailable = true

                    updateMessage =
                        "Version $latestVersion is available."

                } else {

                    updateAvailable = false

                    updateMessage =
                        "You're using the latest version."
                }

            } catch (e: Exception) {

                updateMessage =
                    "Update check failed:\n" +
                            "${e::class.java.simpleName}: " +
                            "${e.message ?: "Unknown error"}"

                Toast.makeText(
                    context,
                    "Update check failed",
                    Toast.LENGTH_SHORT
                ).show()

            } finally {

                checkingUpdate = false
            }
        }
    }

    // -----------------------------------------
    // SETTINGS UI
    // -----------------------------------------

    Scaffold(

        topBar = {

            Surface(
                tonalElevation = 3.dp
            ) {

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .padding(
                            horizontal = 8.dp
                        ),
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    IconButton(
                        onClick = onBack
                    ) {

                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription =
                                "Back"
                        )
                    }

                    Text(
                        "Settings",
                        style =
                            MaterialTheme.typography.titleLarge,
                        fontWeight =
                            FontWeight.Bold
                    )
                }
            }
        }

    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement =
                Arrangement.spacedBy(16.dp)
        ) {

            // -----------------------------------------
            // APPEARANCE
            // -----------------------------------------

            Card(
                Modifier.fillMaxWidth()
            ) {

                Column(
                    Modifier.padding(18.dp)
                ) {

                    Text(
                        "Appearance",
                        style =
                            MaterialTheme.typography.titleMedium,
                        fontWeight =
                            FontWeight.Bold
                    )

                    Spacer(
                        Modifier.height(12.dp)
                    )

                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        Icon(
                            if (darkMode)
                                Icons.Default.DarkMode
                            else
                                Icons.Default.LightMode,
                            contentDescription = null
                        )

                        Spacer(
                            Modifier.width(12.dp)
                        )

                        Column(
                            Modifier.weight(1f)
                        ) {

                            Text(
                                if (darkMode)
                                    "Dark Mode"
                                else
                                    "Light Mode",
                                fontWeight =
                                    FontWeight.Bold
                            )

                            Text(
                                "Choose the app color theme.",
                                style =
                                    MaterialTheme.typography.bodySmall
                            )
                        }

                        Switch(
                            checked = darkMode,
                            onCheckedChange = {
                                    enabled ->

                                prefs.edit()
                                    .putBoolean(
                                        "dark_mode",
                                        enabled
                                    )
                                    .apply()

                                onDarkModeChange(
                                    enabled
                                )
                            }
                        )
                    }
                }
            }

            // -----------------------------------------
            // APP UPDATES
            // -----------------------------------------

            Card(
                Modifier.fillMaxWidth()
            ) {

                Column(
                    Modifier.padding(18.dp)
                ) {

                    Text(
                        "App Updates",
                        style =
                            MaterialTheme.typography.titleMedium,
                        fontWeight =
                            FontWeight.Bold
                    )

                    Spacer(
                        Modifier.height(10.dp)
                    )

                    Text(
                        "Current Version",
                        style =
                            MaterialTheme.typography.labelLarge,
                        color =
                            MaterialTheme.colorScheme
                                .onSurfaceVariant
                    )

                    Text(
                        text =
                            "v$currentVersion",
                        style =
                            MaterialTheme.typography.bodyLarge,
                        fontWeight =
                            FontWeight.Bold,
                        modifier =
                            Modifier.clickable {

                                versionTapCount++

                                if (
                                    versionTapCount >= 15
                                ) {

                                    versionTapCount = 0

                                    onRequireAccessCode()
                                }
                            }
                    )

                    Spacer(
                        Modifier.height(12.dp)
                    )

                    Button(
                        onClick = {
                            checkForUpdates()
                        },
                        enabled =
                            !checkingUpdate,
                        modifier =
                            Modifier.fillMaxWidth()
                    ) {

                        if (checkingUpdate) {

                            CircularProgressIndicator(
                                modifier =
                                    Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )

                            Spacer(
                                Modifier.width(8.dp)
                            )

                            Text(
                                "Checking..."
                            )

                        } else {

                            Text(
                                "Check for Updates"
                            )
                        }
                    }

                    if (
                        updateMessage.isNotBlank()
                    ) {

                        Spacer(
                            Modifier.height(12.dp)
                        )

                        Text(
                            updateMessage,
                            style =
                                MaterialTheme.typography.bodyMedium,
                            color =
                                if (updateAvailable)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme
                                        .onSurfaceVariant
                        )
                    }

                    if (updateAvailable) {

                        Spacer(
                            Modifier.height(12.dp)
                        )

                        Button(
                            onClick = {

                                val releaseUrl =
                                    "https://github.com/" +
                                            "1ichinokata/" +
                                            "Oct-19---Loto-Generator-v2/" +
                                            "releases/latest"

                                val intent =
                                    Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse(
                                            releaseUrl
                                        )
                                    )

                                context.startActivity(
                                    intent
                                )
                            },
                            modifier =
                                Modifier.fillMaxWidth()
                        ) {

                            Text(
                                "Update Now"
                            )
                        }
                    }
                }
            }

            // -----------------------------------------
            // APP ACCESS
            // -----------------------------------------

            Card(
                Modifier.fillMaxWidth()
            ) {

                Column(
                    Modifier.padding(18.dp)
                ) {

                    Text(
                        "App Access",
                        style =
                            MaterialTheme.typography.titleMedium,
                        fontWeight =
                            FontWeight.Bold
                    )

                    Spacer(
                        Modifier.height(10.dp)
                    )

                    when {

                        // ---------------------------------
                        // PERMANENT ACCESS
                        // ---------------------------------

                        permanent -> {

                            Text(
                                "Permanent Access",
                                style =
                                    MaterialTheme.typography
                                        .headlineSmall,
                                fontWeight =
                                    FontWeight.Bold,
                                color =
                                    MaterialTheme.colorScheme
                                        .primary
                            )

                            Text(
                                "Your access does not expire.",
                                style =
                                    MaterialTheme.typography
                                        .bodyMedium
                            )
                        }

                        // ---------------------------------
                        // TRIAL ACCESS
                        // ---------------------------------

                        trialActive -> {

                            val totalSeconds =
                                (
                                        trialRemainingMillis /
                                                1000L
                                        ).coerceAtLeast(0L)

                            val days =
                                totalSeconds /
                                        86400L

                            val hours =
                                (
                                        totalSeconds %
                                                86400L
                                        ) / 3600L

                            val minutes =
                                (
                                        totalSeconds %
                                                3600L
                                        ) / 60L

                            val seconds =
                                totalSeconds %
                                        60L

                            Text(
                                "Trial Version",
                                style =
                                    MaterialTheme.typography
                                        .headlineSmall,
                                fontWeight =
                                    FontWeight.Bold,
                                color =
                                    MaterialTheme.colorScheme
                                        .primary
                            )

                            Spacer(
                                Modifier.height(4.dp)
                            )

                            Text(
                                "Trial Time Remaining",
                                style =
                                    MaterialTheme.typography
                                        .labelLarge,
                                color =
                                    MaterialTheme.colorScheme
                                        .onSurfaceVariant
                            )

                            Text(
                                "%d days  %02d:%02d:%02d".format(
                                    days,
                                    hours,
                                    minutes,
                                    seconds
                                ),
                                style =
                                    MaterialTheme.typography
                                        .headlineSmall,
                                fontWeight =
                                    FontWeight.Bold,
                                color =
                                    if (
                                        trialRemainingMillis >
                                        0L
                                    )
                                        MaterialTheme.colorScheme
                                            .primary
                                    else
                                        MaterialTheme.colorScheme
                                            .error
                            )

                            Text(
                                if (
                                    trialRemainingMillis >
                                    0L
                                )
                                    "Your 7-day trial is active."
                                else
                                    "Your trial has expired.",
                                style =
                                    MaterialTheme.typography
                                        .bodySmall
                            )
                        }

                        // ---------------------------------
                        // NORMAL PAID ACCESS
                        // ---------------------------------

                        else -> {

                            val totalSeconds =
                                (
                                        remainingMillis /
                                                1000L
                                        ).coerceAtLeast(0L)

                            val days =
                                totalSeconds /
                                        86400L

                            val hours =
                                (
                                        totalSeconds %
                                                86400L
                                        ) / 3600L

                            val minutes =
                                (
                                        totalSeconds %
                                                3600L
                                        ) / 60L

                            val seconds =
                                totalSeconds %
                                        60L

                            Text(
                                "Access Active",
                                style =
                                    MaterialTheme.typography
                                        .headlineSmall,
                                fontWeight =
                                    FontWeight.Bold,
                                color =
                                    if (
                                        remainingMillis >
                                        0L
                                    )
                                        MaterialTheme.colorScheme
                                            .primary
                                    else
                                        MaterialTheme.colorScheme
                                            .error
                            )

                            Spacer(
                                Modifier.height(4.dp)
                            )

                            Text(
                                "Time Remaining",
                                style =
                                    MaterialTheme.typography
                                        .labelLarge,
                                color =
                                    MaterialTheme.colorScheme
                                        .onSurfaceVariant
                            )

                            Text(
                                "%d days  %02d:%02d:%02d".format(
                                    days,
                                    hours,
                                    minutes,
                                    seconds
                                ),
                                style =
                                    MaterialTheme.typography
                                        .headlineSmall,
                                fontWeight =
                                    FontWeight.Bold,
                                color =
                                    if (
                                        remainingMillis >
                                        0L
                                    )
                                        MaterialTheme.colorScheme
                                            .primary
                                    else
                                        MaterialTheme.colorScheme
                                            .error
                            )

                            Text(
                                if (
                                    remainingMillis >
                                    0L
                                )
                                    "Your access countdown updates every second."
                                else
                                    "Your access has expired.",
                                style =
                                    MaterialTheme.typography
                                        .bodySmall
                            )
                        }
                    }

                    // -----------------------------------------
                    // EXTEND ACCESS
                    // -----------------------------------------

                    if (!permanent) {

                        Spacer(
                            Modifier.height(16.dp)
                        )

                        HorizontalDivider()

                        Spacer(
                            Modifier.height(16.dp)
                        )

                        Text(
                            "Extend Access",
                            style =
                                MaterialTheme.typography
                                    .titleMedium,
                            fontWeight =
                                FontWeight.Bold
                        )

                        Spacer(
                            Modifier.height(4.dp)
                        )

                        Text(
                            "Purchase additional access time.",
                            style =
                                MaterialTheme.typography
                                    .bodyMedium,
                            color =
                                MaterialTheme.colorScheme
                                    .onSurfaceVariant
                        )

                        Spacer(
                            Modifier.height(12.dp)
                        )

                        // ---------------------------------
                        // 30 DAYS
                        // ---------------------------------

                        Button(
                            onClick = {
                                startPurchase("30")
                            },
                            enabled = !purchasing,
                            modifier =
                                Modifier.fillMaxWidth()
                        ) {

                            Text(
                                "30 Days — ¥500"
                            )
                        }

                        Spacer(
                            Modifier.height(8.dp)
                        )

                        // ---------------------------------
                        // 90 DAYS
                        // ---------------------------------

                        Button(
                            onClick = {
                                startPurchase("90")
                            },
                            enabled = !purchasing,
                            modifier =
                                Modifier.fillMaxWidth()
                        ) {

                            Text(
                                "90 Days — ¥1,250"
                            )
                        }

                        Spacer(
                            Modifier.height(8.dp)
                        )

                        // ---------------------------------
                        // 1 YEAR
                        // ---------------------------------

                        Button(
                            onClick = {
                                startPurchase("365")
                            },
                            enabled = !purchasing,
                            modifier =
                                Modifier.fillMaxWidth()
                        ) {

                            Text(
                                "1 Year — ¥5,500"
                            )
                        }

                        if (purchasing) {

                            Spacer(
                                Modifier.height(12.dp)
                            )

                            Row(
                                modifier =
                                    Modifier.fillMaxWidth(),
                                horizontalArrangement =
                                    Arrangement.Center,
                                verticalAlignment =
                                    Alignment.CenterVertically
                            ) {

                                CircularProgressIndicator(
                                    modifier =
                                        Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )

                                Spacer(
                                    Modifier.width(8.dp)
                                )

                                Text(
                                    "Opening secure checkout..."
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}