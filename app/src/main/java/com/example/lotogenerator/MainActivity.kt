
package com.example.lotogenerator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private enum class Lottery {
    LOTO6,
    LOTO7,
    MINI_LOTO
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ProtectedLotoApp()
        }
    }
}

@Composable
private fun ProtectedLotoApp() {

    val context = LocalContext.current

    val localAccess =
        remember {
            AccessManager.isActivated(context)
        }

    var activated by remember {
        mutableStateOf(localAccess)
    }

    var checkingAccess by remember {
        mutableStateOf(localAccess)
    }

    /*
     * If the device already has a locally stored access code,
     * check it against the Supabase server.
     */
    LaunchedEffect(Unit) {

        if (localAccess) {

            checkingAccess = true

            val result =
                AccessManager.validateCurrentAccess(
                    context
                )

            /*
             * If the server confirms the code,
             * keep the user inside the app.
             */
            if (result.success) {

                activated = true

            } else if (!result.connectionError) {

                /*
                 * Server explicitly rejected the code.
                 * AccessManager has already cleared
                 * the saved access information.
                 */
                activated = false
            }

            /*
             * If connectionError == true,
             * keep the existing local access.
             *
             * This gives the user offline grace instead
             * of locking them out because of a temporary
             * internet/server problem.
             */

            checkingAccess = false
        }
    }

    /*
     * Show a small loading screen while validating
     * an already activated account.
     */
    if (checkingAccess) {

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }

    } else if (!activated) {

        AccessScreen(
            onActivated = {
                activated = true
            }
        )

    } else {

        CombinedLotoApp()
    }
}

@Composable
private fun CombinedLotoApp() {

    var lottery by remember {
        mutableStateOf(Lottery.LOTO6)
    }

    var lotteryMenuExpanded by remember {
        mutableStateOf(false)
    }

    val selectedLotteryName = when (lottery) {
        Lottery.LOTO6 -> "Loto 6"
        Lottery.LOTO7 -> "Loto 7"
        Lottery.MINI_LOTO -> "Mini Loto"
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {

        when (lottery) {
            Lottery.LOTO6 -> Loto6App()
            Lottery.LOTO7 -> Loto7App()
            Lottery.MINI_LOTO -> MiniLotoApp()
        }

        Box(
            modifier = Modifier
                .padding(
                    start = 16.dp,
                    top = 16.dp
                )
        ) {
            OutlinedButton(
                onClick = {
                    lotteryMenuExpanded = true
                }
            ) {
                Text(
                    text = "🇯🇵 $selectedLotteryName"
                )

                Text(
                    text = " ▼"
                )
            }

            DropdownMenu(
                expanded = lotteryMenuExpanded,
                onDismissRequest = {
                    lotteryMenuExpanded = false
                }
            ) {

                DropdownMenuItem(
                    text = {
                        Text("🇯🇵 Loto 6")
                    },
                    onClick = {
                        lottery = Lottery.LOTO6
                        lotteryMenuExpanded = false
                    }
                )

                DropdownMenuItem(
                    text = {
                        Text("🇯🇵 Loto 7")
                    },
                    onClick = {
                        lottery = Lottery.LOTO7
                        lotteryMenuExpanded = false
                    }
                )

                DropdownMenuItem(
                    text = {
                        Text("🇯🇵 Mini Loto")
                    },
                    onClick = {
                        lottery = Lottery.MINI_LOTO
                        lotteryMenuExpanded = false
                    }
                )
            }
        }
    }
}
