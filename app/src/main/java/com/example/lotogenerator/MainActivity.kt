package com.example.lotogenerator

import android.app.Activity
import android.content.Intent
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
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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

        AppLanguageManager.applySaved(this)

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

        Image(
            painter = painterResource(
                R.drawable.furan_splash_reference
            ),
            contentDescription =
                "Furan's Loto Generator startup splash",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

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

    DailyUpdatePrompt()

    var activated by remember {
        mutableStateOf(false)
    }

    var checkingAccess by remember {
        mutableStateOf(true)
    }

    /*
     * This is separate from activated.
     *
     * A user who already has valid access can manually
     * open AccessScreen from Settings to extend/replace
     * their access code without losing current access.
     */
    var manuallyEnteringAccessCode by remember {
        mutableStateOf(false)
    }

    var paymentResultMessage by remember {
        mutableStateOf<String?>(null)
    }

    /*
     * This should NOT increase on every normal resume.
     *
     * It is only increased when the app returns from
     * Stripe/payment so normal minimizing/backgrounding
     * does not trigger an access reload.
     */
    var accessSession by remember {
        mutableStateOf(0)
    }

    val lifecycleOwner =
        LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {

        val observer =
            LifecycleEventObserver { _, event ->

                if (event == Lifecycle.Event.ON_RESUME) {

                    val activity =
                        context as? Activity

                    val data =
                        activity?.intent?.data

                    val paymentReturn =
                        (
                                data?.scheme == "https" &&
                                        data.host ==
                                        "1ichinokata.github.io" &&
                                        data.path?.startsWith(
                                            "/payment-success"
                                        ) == true
                                ) ||
                                (
                                        data?.scheme ==
                                                "furanlotto" &&
                                                data.host ==
                                                "payment-cancel"
                                        )

                    /*
                     * Only payment return causes another
                     * access validation.
                     *
                     * Normal background -> foreground
                     * does nothing here.
                     */
                    if (paymentReturn) {
                        accessSession++
                    }
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


    LaunchedEffect(accessSession) {

        checkingAccess = true

        try {

            /*
             * If this is a Stripe return, allow a little
             * time for the webhook/Supabase update.
             */
            if (accessSession > 0) {
                delay(2000L)
            }

            val activity =
                context as? Activity

            val returnUri =
                activity?.intent?.data

            val isPaymentSuccess =
                returnUri?.scheme == "https" &&
                        returnUri.host ==
                        "1ichinokata.github.io" &&
                        returnUri.path?.startsWith(
                            "/payment-success"
                        ) == true

            val isPaymentCancelled =
                returnUri?.scheme ==
                        "furanlotto" &&
                        returnUri.host ==
                        "payment-cancel"


            if (isPaymentCancelled) {

                paymentResultMessage =
                    "Purchase Cancelled"

                activity?.let {

                    it.intent =
                        Intent(it.intent)
                            .setData(null)
                }
            }


            if (isPaymentSuccess) {

                var purchaseRestored = false

                for (attempt in 1..5) {

                    val restoreResult =
                        AccessManager.restoreAccess(
                            context
                        )

                    if (restoreResult.success) {

                        purchaseRestored = true
                        break
                    }

                    if (attempt < 5) {
                        delay(2000L)
                    }
                }

                activity?.let {

                    it.intent =
                        Intent(it.intent)
                            .setData(null)
                }

                if (purchaseRestored) {

                    activated = true
                    manuallyEnteringAccessCode = false

                    paymentResultMessage =
                        "Purchase Successful"

                    return@LaunchedEffect

                } else {

                    paymentResultMessage =
                        "Purchase Failed. Payment could not be verified. Please try Restore Access or contact support if you were charged."
                }
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
                    AccessManager
                        .validateCurrentAccess(
                            context
                        )

                if (result.success) {

                    activated = true
                    return@LaunchedEffect

                } else if (
                    result.connectionError
                ) {

                    /*
                     * Keep locally valid access during
                     * a temporary network problem.
                     */
                    activated = true
                    return@LaunchedEffect
                }
            }


            /*
             * No valid local access.
             *
             * Try restoring paid access from server.
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

                if (attempt < 5) {
                    delay(2000L)
                }
            }


            if (restored) {

                activated = true
                return@LaunchedEffect
            }


            /*
             * No paid access.
             *
             * Check the device's existing trial.
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
                modifier =
                    Modifier.fillMaxSize(),
                contentAlignment =
                    Alignment.Center
            ) {

                CircularProgressIndicator()
            }
        }


        /*
         * Show AccessScreen when:
         *
         * 1. User has no valid access, OR
         * 2. User manually selected Enter Access Code
         *    from Settings.
         *
         * Existing paid access remains active while
         * manually entering an extension code.
         */
        !activated ||
                manuallyEnteringAccessCode -> {

            AccessScreen(
                onActivated = {

                    AccessManager.setManualLock(
                        context,
                        false
                    )

                    manuallyEnteringAccessCode =
                        false

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
                     * IMPORTANT:
                     *
                     * Do not set activated=false.
                     * Do not increase accessSession.
                     * Do not enable manual_lock.
                     *
                     * The user still has valid access.
                     * We are simply opening AccessScreen
                     * so they can extend their time.
                     */
                    manuallyEnteringAccessCode =
                        true
                }
            )
        }
    }


    paymentResultMessage?.let { message ->

        AlertDialog(
            onDismissRequest = {
                paymentResultMessage = null
            },
            title = {
                Text(message)
            },
            text = {

                Text(
                    when {

                        message ==
                                "Purchase Successful" ->

                            "Your purchase has been verified and your access has been updated."

                        message ==
                                "Purchase Cancelled" ->

                            "The purchase was cancelled. No access change was made."

                        else -> message
                    }
                )
            },
            confirmButton = {

                TextButton(
                    onClick = {
                        paymentResultMessage = null
                    }
                ) {

                    Text("OK")
                }
            }
        )
    }
}


@Composable
private fun CombinedLotoApp(
    darkMode: Boolean,
    onDarkModeChange: (Boolean) -> Unit,
    onRequireAccessCode: () -> Unit
) {

    var lottery by rememberSaveable {
        mutableStateOf(
            Lottery.LOTO6
        )
    }

    var philippineGame by rememberSaveable {
        mutableStateOf(
            PhilippineLottoGame.LOTTO_642
        )
    }

    var countryExpanded by remember {
        mutableStateOf(false)
    }

    var gameExpanded by remember {
        mutableStateOf(false)
    }

    var showSettings by rememberSaveable {
        mutableStateOf(false)
    }

    var showHome by rememberSaveable {
        mutableStateOf(true)
    }


    val isJapan =
        lottery != Lottery.PHILIPPINE

    val countryName =
        if (isJapan) {
            "🇯🇵 Japan Loto"
        } else {
            "🇵🇭 Philippine Loto"
        }

    val gameName =
        when (lottery) {

            Lottery.LOTO6 ->
                "Loto 6"

            Lottery.LOTO7 ->
                "Loto 7"

            Lottery.MINI_LOTO ->
                "Mini Loto"

            Lottery.PHILIPPINE ->
                philippineGame.title
        }


    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                MaterialTheme
                    .colorScheme
                    .background
            )
            .then(
                if (showSettings) {
                    Modifier.lotoSettingsSwipe {
                        showSettings = false
                    }
                } else {
                    Modifier
                }
            )
    ) {

        when {

            showSettings -> {

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
            }


            showHome -> {

                LottoHomeScreen(
                    onSwipeRight = {
                        // Keep the currently selected lottery and open its Generator tab.
                        showHome = false
                    },
                    onJapan = {

                        lottery =
                            Lottery.LOTO6

                        showHome = false
                    },

                    onPhilippines = {

                        lottery =
                            Lottery.PHILIPPINE

                        philippineGame =
                            PhilippineLottoGame
                                .LOTTO_642

                        showHome = false
                    },

                    onSettings = {
                        showSettings = true
                    }
                )
            }


            else -> {

                when (lottery) {

                    Lottery.LOTO6 ->

                        Loto6App(
                            onHome = {
                                showHome = true
                            }
                        )

                    Lottery.LOTO7 ->

                        Loto7App(
                            onHome = {
                                showHome = true
                            }
                        )

                    Lottery.MINI_LOTO ->

                        MiniLotoApp(
                            onHome = {
                                showHome = true
                            }
                        )

                    Lottery.PHILIPPINE ->

                        PhilippineLottoApp(
                            selectedGame =
                                philippineGame,

                            onHome = {
                                showHome = true
                            }
                        )
                }


                /*
                 * Country selector
                 */
                Box(
                    modifier =
                        Modifier.padding(
                            start = 12.dp,
                            top = 16.dp
                        )
                ) {

                    OutlinedButton(
                        onClick = {
                            countryExpanded = true
                        }
                    ) {

                        Text(
                            "$countryName ▼"
                        )
                    }

                    DropdownMenu(
                        expanded =
                            countryExpanded,

                        onDismissRequest = {
                            countryExpanded =
                                false
                        }
                    ) {

                        DropdownMenuItem(
                            text = {
                                Text(
                                    "🇯🇵 Japan Loto"
                                )
                            },

                            onClick = {

                                lottery =
                                    Lottery.LOTO6

                                countryExpanded =
                                    false
                            }
                        )

                        DropdownMenuItem(
                            text = {
                                Text(
                                    "🇵🇭 Philippine Loto"
                                )
                            },

                            onClick = {

                                lottery =
                                    Lottery.PHILIPPINE

                                countryExpanded =
                                    false
                            }
                        )
                    }
                }


                /*
                 * Game selector
                 */
                Box(
                    modifier = Modifier
                        .align(
                            Alignment.TopEnd
                        )
                        .padding(
                            top = 16.dp,
                            end = 56.dp
                        )
                ) {

                    OutlinedButton(
                        onClick = {
                            gameExpanded = true
                        }
                    ) {

                        Text(
                            "$gameName ▼"
                        )
                    }

                    DropdownMenu(
                        expanded =
                            gameExpanded,

                        onDismissRequest = {
                            gameExpanded =
                                false
                        }
                    ) {

                        if (isJapan) {

                            DropdownMenuItem(
                                text = {
                                    Text("Loto 6")
                                },
                                onClick = {

                                    lottery =
                                        Lottery.LOTO6

                                    gameExpanded =
                                        false
                                }
                            )

                            DropdownMenuItem(
                                text = {
                                    Text("Loto 7")
                                },
                                onClick = {

                                    lottery =
                                        Lottery.LOTO7

                                    gameExpanded =
                                        false
                                }
                            )

                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "Mini Loto"
                                    )
                                },
                                onClick = {

                                    lottery =
                                        Lottery.MINI_LOTO

                                    gameExpanded =
                                        false
                                }
                            )

                        } else {

                            PhilippineLottoGame
                                .entries
                                .forEach { game ->

                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                game.title
                                            )
                                        },

                                        onClick = {

                                            philippineGame =
                                                game

                                            gameExpanded =
                                                false
                                        }
                                    )
                                }
                        }
                    }
                }


                /*
                 * Settings button
                 */
                IconButton(
                    onClick = {
                        showSettings = true
                    },

                    modifier = Modifier
                        .align(
                            Alignment.TopEnd
                        )
                        .padding(
                            top = 16.dp,
                            end = 8.dp
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
}


@Composable
private fun LottoHomeScreen(
    onSwipeRight: () -> Unit,
    onJapan: () -> Unit,
    onPhilippines: () -> Unit,
    onSettings: () -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .lotoTabSwipe(selectedTab = 0) { nextTab ->
                if (nextTab == 1) onSwipeRight()
            }
            .padding(
                horizontal = 24.dp,
                vertical = 28.dp
            ),
        horizontalAlignment =
            Alignment.CenterHorizontally,
        verticalArrangement =
            Arrangement.Center
    ) {

        Text(
            text =
                "Furan Lotto Generator",
            style =
                MaterialTheme
                    .typography
                    .headlineMedium
        )

        Spacer(
            Modifier.height(8.dp)
        )

        Text(
            text =
                "Choose Your Lotto",
            style =
                MaterialTheme
                    .typography
                    .titleMedium,
            color =
                MaterialTheme
                    .colorScheme
                    .onSurfaceVariant
        )

        Spacer(
            Modifier.height(28.dp)
        )


        Card(
            modifier =
                Modifier.fillMaxWidth()
        ) {

            Column(
                Modifier.padding(20.dp)
            ) {

                Text(
                    "🇯🇵  JAPAN LOTO",
                    style =
                        MaterialTheme
                            .typography
                            .titleLarge
                )

                Spacer(
                    Modifier.height(6.dp)
                )

                Text(
                    "Loto 6 • Loto 7 • Mini Loto"
                )

                Spacer(
                    Modifier.height(14.dp)
                )

                Button(
                    onClick = onJapan,
                    modifier =
                        Modifier.fillMaxWidth()
                ) {

                    Text(
                        "Open Japan Loto"
                    )
                }
            }
        }


        Spacer(
            Modifier.height(16.dp)
        )


        Card(
            modifier =
                Modifier.fillMaxWidth()
        ) {

            Column(
                Modifier.padding(20.dp)
            ) {

                Text(
                    "🇵🇭  PHILIPPINE LOTTO",
                    style =
                        MaterialTheme
                            .typography
                            .titleLarge
                )

                Spacer(
                    Modifier.height(6.dp)
                )

                Text(
                    "6/42 • 6/45 • 6/49 • 6/55 • 6/58"
                )

                Spacer(
                    Modifier.height(14.dp)
                )

                Button(
                    onClick =
                        onPhilippines,
                    modifier =
                        Modifier.fillMaxWidth()
                ) {

                    Text(
                        "Open Philippine Lotto"
                    )
                }
            }
        }


        Spacer(
            Modifier.height(20.dp)
        )


        LanguagePickerButton(
            modifier =
                Modifier.fillMaxWidth()
        )


        Spacer(
            Modifier.height(10.dp)
        )


        OutlinedButton(
            onClick = onSettings
        ) {

            Icon(
                Icons.Default.Settings,
                contentDescription = null
            )

            Spacer(
                Modifier.width(8.dp)
            )

            Text("Settings")
        }
    }
}