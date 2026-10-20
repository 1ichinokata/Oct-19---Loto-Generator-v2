package com.example.lotogenerator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun AccessScreen(
    onActivated: () -> Unit
) {

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var accessCode by remember {
        mutableStateOf("")
    }

    var errorMessage by remember {
        mutableStateOf("")
    }

    var isLoading by remember {
        mutableStateOf(false)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Text(
            text = "🎟️",
            style = MaterialTheme.typography.displayLarge
        )

        Text(
            text = "Loto Generator",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(top = 12.dp)
        )

        Text(
            text = "Enter your access code",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(
                top = 8.dp,
                bottom = 24.dp
            )
        )

        OutlinedTextField(
            value = accessCode,
            onValueChange = {
                accessCode = it
                errorMessage = ""
            },
            label = {
                Text("Access Code")
            },
            placeholder = {
                Text("XXXX-XXXXX-XXX")
            },
            singleLine = true,
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth()
        )

        if (errorMessage.isNotEmpty()) {

            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Button(
            onClick = {

                if (accessCode.trim().isEmpty()) {

                    errorMessage =
                        "Please enter your access code."

                    return@Button
                }

                isLoading = true
                errorMessage = ""

                scope.launch {

                    val result =
                        AccessManager.activate(
                            context = context,
                            code = accessCode
                        )

                    isLoading = false

                    if (result.success) {

                        onActivated()

                    } else {

                        errorMessage =
                            result.message
                    }
                }
            },
            enabled = !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp)
        ) {

            if (isLoading) {

                CircularProgressIndicator(
                    modifier = Modifier.padding(2.dp),
                    strokeWidth = 2.dp
                )

            } else {

                Text("ACTIVATE")
            }
        }

        Text(
            text = "Enter a valid access code to continue.",
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 16.dp)
        )
    }
}