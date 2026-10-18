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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private enum class Lottery { LOTO6, LOTO7, MINI_LOTO, PHILIPPINE }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ProtectedLotoApp() }
    }
}

@Composable
private fun ProtectedLotoApp() {
    val context = LocalContext.current
    val localAccess = remember { AccessManager.isActivated(context) }
    var activated by remember { mutableStateOf(localAccess) }
    var checkingAccess by remember { mutableStateOf(localAccess) }

    LaunchedEffect(Unit) {
        if (localAccess) {
            checkingAccess = true
            val result = AccessManager.validateCurrentAccess(context)
            if (result.success) activated = true
            else if (!result.connectionError) activated = false
            checkingAccess = false
        }
    }

    when {
        checkingAccess -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        !activated -> AccessScreen(onActivated = { activated = true })
        else -> CombinedLotoApp()
    }
}

@Composable
private fun CombinedLotoApp() {
    var lottery by remember { mutableStateOf(Lottery.LOTO6) }
    var expanded by remember { mutableStateOf(false) }

    val name = when (lottery) {
        Lottery.LOTO6 -> "🇯🇵 Loto 6"
        Lottery.LOTO7 -> "🇯🇵 Loto 7"
        Lottery.MINI_LOTO -> "🇯🇵 Mini Loto"
        Lottery.PHILIPPINE -> "🇵🇭 Philippine Lotto"
    }

    Box(Modifier.fillMaxSize()) {
        when (lottery) {
            Lottery.LOTO6 -> Loto6App()
            Lottery.LOTO7 -> Loto7App()
            Lottery.MINI_LOTO -> MiniLotoApp()
            Lottery.PHILIPPINE -> PhilippineLottoApp()
        }

        Box(Modifier.padding(start = 16.dp, top = 16.dp)) {
            OutlinedButton(onClick = { expanded = true }) {
                Text("$name ▼")
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("🇯🇵 Loto 6") },
                    onClick = { lottery = Lottery.LOTO6; expanded = false }
                )
                DropdownMenuItem(
                    text = { Text("🇯🇵 Loto 7") },
                    onClick = { lottery = Lottery.LOTO7; expanded = false }
                )
                DropdownMenuItem(
                    text = { Text("🇯🇵 Mini Loto") },
                    onClick = { lottery = Lottery.MINI_LOTO; expanded = false }
                )
                DropdownMenuItem(
                    text = {
                        Text("🇵🇭 Philippines — 6/42 • 6/45 • 6/49 • 6/55 • 6/58")
                    },
                    onClick = {
                        lottery = Lottery.PHILIPPINE
                        expanded = false
                    }
                )
            }
        }
    }
}
