
package com.example.lotogenerator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay


private enum class Lottery {
    LOTO6,
    LOTO7,
    MINI_LOTO,
    PHILIPPINE
}


private object SplashSession {
    var shown = false
}


class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AppWithSplash()
        }
    }
}


@Composable
private fun AppWithSplash() {

    val context = LocalContext.current

    val preferences = remember {
        context.getSharedPreferences(
            "combined_loto_settings",
            android.content.Context.MODE_PRIVATE
        )
    }

    var darkMode by remember {
        mutableStateOf(
            preferences.getBoolean(
                "dark_mode",
                false
            )
        )
    }

    var showSplash by remember {
        mutableStateOf(
            !SplashSession.shown
        )
    }

    LaunchedEffect(Unit) {

        if (showSplash) {

            delay(5000L)

            SplashSession.shown = true
            showSplash = false
        }
    }

    if (showSplash) {

        FuranSplashScreen()

    } else {

        MaterialTheme(
            colorScheme = if (darkMode) {
                darkColorScheme()
            } else {
                lightColorScheme()
            }
        ) {

            ProtectedLotoApp(
                darkMode = darkMode,
                onDarkModeChange = { enabled ->

                    darkMode = enabled

                    preferences
                        .edit()
                        .putBoolean(
                            "dark_mode",
                            enabled
                        )
                        .apply()
                }
            )
        }
    }
}


@Composable
private fun FuranSplashScreen() {

    val animation = rememberInfiniteTransition(
        label = "splashReferenceAnimation"
    )

    val phase by animation.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 5200,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "backgroundPhase"
    )

    val twinkle by animation.animateFloat(
        initialValue = 0.12f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(620),
            repeatMode = RepeatMode.Reverse
        ),
        label = "twinkleFast"
    )

    val twinkleSlow by animation.animateFloat(
        initialValue = 1f,
        targetValue = 0.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(1050),
            repeatMode = RepeatMode.Reverse
        ),
        label = "twinkleSlow"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                MaterialTheme.colorScheme.background
            )
    ) {

        // Fixed user-approved splash artwork.
        Image(
            painter = painterResource(
                R.drawable.furan_splash_reference
            ),
            contentDescription =
                "Furan's Loto Generator startup splash",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Animated decorative effects only.
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {

            val w = size.width
            val h = size.height

            val travel = phase * w * 1.25f

            for (i in 0..3) {

                val x =
                    ((travel + i * w * 0.34f) %
                            (w * 1.35f)) -
                            w * 0.2f

                val y =
                    h * (0.08f + i * 0.27f)

                drawOval(
                    color = if (i % 2 == 0) {
                        Color(0x33FFE36A)
                    } else {
                        Color(0x334DDCFF)
                    },
                    topLeft = Offset(x, y),
                    size = Size(
                        w * 0.38f,
                        8f
                    )
                )
            }

            fun sparkle(
                cx: Float,
                cy: Float,
                radius: Float,
                alpha: Float
            ) {

                val white =
                    Color.White.copy(
                        alpha = alpha
                    )

                val gold =
                    Color(0xFFFFE36A)
                        .copy(alpha = alpha)

                drawLine(
                    color = white,
                    start = Offset(
                        cx - radius,
                        cy
                    ),
                    end = Offset(
                        cx + radius,
                        cy
                    ),
                    strokeWidth = 4f
                )

                drawLine(
                    color = white,
                    start = Offset(
                        cx,
                        cy - radius
                    ),
                    end = Offset(
                        cx,
                        cy + radius
                    ),
                    strokeWidth = 4f
                )

                drawCircle(
                    color = gold,
                    radius = radius * 0.18f,
                    center = Offset(
                        cx,
                        cy
                    )
                )
            }

            sparkle(
                w * 0.20f,
                h * 0.22f,
                20f,
                twinkle
            )

            sparkle(
                w * 0.78f,
                h * 0.24f,
                27f,
                twinkleSlow
            )

            sparkle(
                w * 0.12f,
                h * 0.53f,
                18f,
                twinkleSlow
            )

            sparkle(
                w * 0.87f,
                h * 0.50f,
                22f,
                twinkle
            )

            sparkle(
                w * 0.68f,
                h * 0.12f,
                14f,
                twinkle
            )
        }
    }
}


@Composable
private fun ProtectedLotoApp(
    darkMode: Boolean,
    onDarkModeChange: (Boolean) -> Unit
) {

    val context = LocalContext.current

    var activated by remember {
        mutableStateOf(false)
    }

    var checkingAccess by remember {
        mutableStateOf(true)
    }

    /*
     * Changes every time the Activity resumes.
     *
     * Stripe flow:
     *
     * 1. User leaves the app.
     * 2. Stripe Checkout opens.
     * 3. User completes payment.
     * 4. Stripe webhook grants access.
     * 5. User returns to the app.
     * 6. ON_RESUME increments accessSession.
     * 7. LaunchedEffect runs again.
     * 8. restoreAccess() checks the new paid access.
     */
    var accessSession by remember {
        mutableStateOf(0)
    }

    val lifecycleOwner =
        LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {

        val observer =
            LifecycleEventObserver { _, event ->

                if (
                    event ==
                    Lifecycle.Event.ON_RESUME
                ) {

                    accessSession++
                }
            }

        lifecycleOwner.lifecycle.addObserver(
            observer
        )

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(
                observer
            )
        }
    }

    /*
     * Check access:
     *
     * - on first startup
     * - whenever the Activity resumes
     * - after Settings deliberately locks access
     *
     * For Stripe purchases we retry restore several
     * times because the Stripe webhook may take a
     * moment to create/update access_codes.
     */
    LaunchedEffect(accessSession) {

        checkingAccess = true

        try {

            /*
             * Give Stripe/Supabase time to finish
             * processing the payment.
             */
            if (accessSession > 0) {
                delay(2000L)
            }



            /*
             * First check locally saved paid access.
             */
            if (
                AccessManager.isActivated(
                    context
                )
            ) {

                val result =
                    AccessManager.validateCurrentAccess(
                        context
                    )

                if (result.success) {

                    activated = true
                    return@LaunchedEffect

                } else if (
                    result.connectionError
                ) {

                    /*
                     * Temporary connection failure.
                     * Keep valid local access.
                     */
                    activated = true
                    return@LaunchedEffect
                }
            }

            /*
             * No valid local access.
             *
             * Ask the server to restore paid access.
             *
             * Retry because the Stripe webhook can
             * sometimes finish after the app resumes.
             */
            var restored = false

            for (attempt in 1..5) {

                val restoreResult =
                    AccessManager.restoreAccess(
                        context
                    )

                if (restoreResult.success) {

                    restored = true
                    break
                }

                /*
                 * Wait before trying again.
                 *
                 * Do not wait after the final attempt.
                 */
                if (attempt < 5) {
                    delay(2000L)
                }
            }

            if (restored) {

                activated = true
                return@LaunchedEffect
            }

            /*
             * No paid access found.
             *
             * Check the device's 7-day trial.
             */
            val trialResult =
                AccessManager.checkTrial(
                    context
                )

            activated =
                trialResult.success &&
                        !trialResult.expired

        } catch (e: Exception) {

            e.printStackTrace()

        } finally {

            checkingAccess = false
        }
    }

    when {

        checkingAccess -> {

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment =
                    Alignment.Center
            ) {

                CircularProgressIndicator()
            }
        }

        !activated -> {

            AccessScreen(
                onActivated = {

                    /*
                     * User successfully entered
                     * an Access Key.
                     */
                    AccessManager.setManualLock(
                        context,
                        false
                    )

                    activated = true
                }
            )
        }

        else -> {

            CombinedLotoApp(
                darkMode = darkMode,
                onDarkModeChange =
                    onDarkModeChange,

                onRequireAccessCode = {

                    /*
                     * Hidden Settings action.
                     */
                    AccessManager.setManualLock(
                        context,
                        true
                    )

                    activated = false

                    accessSession++
                }
            )
        }
    }
}


@Composable
private fun CombinedLotoApp(
    darkMode: Boolean,
    onDarkModeChange: (Boolean) -> Unit,
    onRequireAccessCode: () -> Unit
) {

    var lottery by remember {
        mutableStateOf(
            Lottery.LOTO6
        )
    }

    var expanded by remember {
        mutableStateOf(false)
    }

    var showSettings by remember {
        mutableStateOf(false)
    }

    val name = when (lottery) {

        Lottery.LOTO6 ->
            "🇯🇵 Loto 6"

        Lottery.LOTO7 ->
            "🇯🇵 Loto 7"

        Lottery.MINI_LOTO ->
            "🇯🇵 Mini Loto"

        Lottery.PHILIPPINE ->
            "🇵🇭 Philippine Lotto"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                MaterialTheme.colorScheme.background
            )
    ) {

        if (showSettings) {

            SettingsScreen(
                darkMode = darkMode,
                onDarkModeChange =
                    onDarkModeChange,

                onBack = {
                    showSettings = false
                },

                onRequireAccessCode = {

                    showSettings = false

                    onRequireAccessCode()
                }
            )

        } else {

            when (lottery) {

                Lottery.LOTO6 -> {

                    Loto6App()
                }

                Lottery.LOTO7 -> {

                    Loto7App()
                }

                Lottery.MINI_LOTO -> {

                    MiniLotoApp()
                }

                Lottery.PHILIPPINE -> {

                    PhilippineLottoApp()
                }
            }
        }

        if (!showSettings) {

            Box(
                modifier = Modifier
                    .padding(
                        start = 16.dp,
                        top = 16.dp
                    )
            ) {

                OutlinedButton(
                    onClick = {
                        expanded = true
                    }
                ) {

                    Text(
                        "$name ▼"
                    )
                }

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = {
                        expanded = false
                    }
                ) {

                    DropdownMenuItem(
                        text = {
                            Text("🇯🇵 Loto 6")
                        },
                        onClick = {

                            lottery =
                                Lottery.LOTO6

                            expanded = false
                        }
                    )

                    DropdownMenuItem(
                        text = {
                            Text("🇯🇵 Loto 7")
                        },
                        onClick = {

                            lottery =
                                Lottery.LOTO7

                            expanded = false
                        }
                    )

                    DropdownMenuItem(
                        text = {
                            Text("🇯🇵 Mini Loto")
                        },
                        onClick = {

                            lottery =
                                Lottery.MINI_LOTO

                            expanded = false
                        }
                    )

                    DropdownMenuItem(
                        text = {
                            Text(
                                "🇵🇭 Philippines — " +
                                        "6/42 • 6/45 • 6/49 • " +
                                        "6/55 • 6/58"
                            )
                        },
                        onClick = {

                            lottery =
                                Lottery.PHILIPPINE

                            expanded = false
                        }
                    )
                }
            }

            IconButton(
                onClick = {
                    showSettings = true
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(
                        top = 16.dp,
                        end = 12.dp
                    )
            ) {

                Icon(
                    imageVector =
                        Icons.Default.Settings,
                    contentDescription =
                        "Settings",
                    tint =
                        MaterialTheme
                            .colorScheme
                            .onBackground
                )
            }
        }
    }
}