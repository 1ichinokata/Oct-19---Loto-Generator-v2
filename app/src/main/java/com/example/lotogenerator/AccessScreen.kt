package com.example.lotogenerator

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

private const val SUPABASE_URL =
    "https://ghwtwpuyswthueqysvnx.supabase.co"

private const val SUPABASE_PUBLISHABLE_KEY =
    "sb_publishable_go_fk2r0PMvz1vcr1QJ_Iw_UjnlzdD9"

private const val CREATE_CHECKOUT_FUNCTION =
    "$SUPABASE_URL/functions/v1/create-checkout-session"


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

    var purchaseLoading by remember {
        mutableStateOf(false)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),

        horizontalAlignment =
            Alignment.CenterHorizontally,

        verticalArrangement =
            Arrangement.Center
    ) {

        Text(
            text = "🎟️",
            style =
                MaterialTheme.typography.displayLarge
        )

        Text(
            text = "Loto Generator",
            style =
                MaterialTheme.typography.headlineMedium,

            modifier =
                Modifier.padding(
                    top = 12.dp
                )
        )

        Text(
            text = "Enter your access code or purchase premium access.",
            style =
                MaterialTheme.typography.bodyLarge,

            textAlign =
                TextAlign.Center,

            modifier =
                Modifier.padding(
                    top = 8.dp,
                    bottom = 24.dp
                )
        )


        /*
         * =================================================
         * ACCESS CODE
         * =================================================
         */

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
                Text(
                    "XXXX-XXXXX-XXX",
                    color = Color.Gray
                )
            },

            singleLine = true,

            enabled =
                !isLoading &&
                        !purchaseLoading,

            modifier =
                Modifier.fillMaxWidth()
        )


        /*
         * =================================================
         * ERROR MESSAGE
         * =================================================
         */

        if (errorMessage.isNotEmpty()) {

            Text(
                text = errorMessage,

                color =
                    MaterialTheme.colorScheme.error,

                textAlign =
                    TextAlign.Center,

                modifier =
                    Modifier.padding(
                        top = 8.dp
                    )
            )
        }


        /*
         * =================================================
         * ACTIVATE EXISTING ACCESS CODE
         * =================================================
         */

        Button(

            onClick = {

                if (
                    accessCode
                        .trim()
                        .isEmpty()
                ) {

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

            enabled =
                !isLoading &&
                        !purchaseLoading,

            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        top = 20.dp
                    )
        ) {

            if (isLoading) {

                CircularProgressIndicator(
                    modifier =
                        Modifier.padding(
                            2.dp
                        ),

                    strokeWidth = 2.dp
                )

            } else {

                Text("ACTIVATE")
            }
        }


        /*
         * =================================================
         * DIVIDER / PURCHASE SECTION
         * =================================================
         */

        Spacer(
            modifier =
                Modifier.height(28.dp)
        )


        Text(
            text = "Purchase Premium Access",

            style =
                MaterialTheme.typography.titleMedium,

            textAlign =
                TextAlign.Center
        )


        Text(
            text =
                "Payment is securely processed by Stripe.",

            style =
                MaterialTheme.typography.bodySmall,

            textAlign =
                TextAlign.Center,

            modifier =
                Modifier.padding(
                    top = 4.dp,
                    bottom = 12.dp
                )
        )


        /*
         * =================================================
         * 30 DAYS
         * =================================================
         */

        Button(

            onClick = {

                purchaseLoading = true
                errorMessage = ""


                scope.launch {

                    val result =
                        createCheckoutSession(
                            context = context,
                            plan = "30"
                        )


                    purchaseLoading = false


                    if (
                        result.success &&
                        !result.checkoutUrl.isNullOrEmpty()
                    ) {

                        openCheckout(
                            context,
                            result.checkoutUrl
                        )

                    } else {

                        errorMessage =
                            result.message
                    }
                }
            },

            enabled =
                !isLoading &&
                        !purchaseLoading,

            modifier =
                Modifier.fillMaxWidth()
        ) {

            Text(
                "BUY 30 DAYS — ¥500"
            )
        }


        Spacer(
            modifier =
                Modifier.height(8.dp)
        )


        /*
         * =================================================
         * 90 DAYS
         * =================================================
         */

        Button(

            onClick = {

                purchaseLoading = true
                errorMessage = ""


                scope.launch {

                    val result =
                        createCheckoutSession(
                            context = context,
                            plan = "90"
                        )


                    purchaseLoading = false


                    if (
                        result.success &&
                        !result.checkoutUrl.isNullOrEmpty()
                    ) {

                        openCheckout(
                            context,
                            result.checkoutUrl
                        )

                    } else {

                        errorMessage =
                            result.message
                    }
                }
            },

            enabled =
                !isLoading &&
                        !purchaseLoading,

            modifier =
                Modifier.fillMaxWidth()
        ) {

            Text(
                "BUY 90 DAYS — ¥1,250"
            )
        }


        Spacer(
            modifier =
                Modifier.height(8.dp)
        )


        /*
         * =================================================
         * 1 YEAR
         * =================================================
         */

        Button(

            onClick = {

                purchaseLoading = true
                errorMessage = ""


                scope.launch {

                    val result =
                        createCheckoutSession(
                            context = context,
                            plan = "365"
                        )


                    purchaseLoading = false


                    if (
                        result.success &&
                        !result.checkoutUrl.isNullOrEmpty()
                    ) {

                        openCheckout(
                            context,
                            result.checkoutUrl
                        )

                    } else {

                        errorMessage =
                            result.message
                    }
                }
            },

            enabled =
                !isLoading &&
                        !purchaseLoading,

            modifier =
                Modifier.fillMaxWidth()
        ) {

            Text(
                "BUY 1 YEAR — ¥5,500"
            )
        }


        if (purchaseLoading) {

            Spacer(
                modifier =
                    Modifier.height(12.dp)
            )

            CircularProgressIndicator(
                strokeWidth = 2.dp
            )

            Text(
                text =
                    "Opening secure Stripe Checkout...",

                style =
                    MaterialTheme.typography.bodySmall,

                modifier =
                    Modifier.padding(
                        top = 6.dp
                    )
            )
        }


        Spacer(
            modifier =
                Modifier.height(16.dp)
        )


        Text(
            text =
                "Enter a valid access code to continue.",

            style =
                MaterialTheme.typography.bodySmall,

            textAlign =
                TextAlign.Center
        )
    }
}


/*
 * =========================================================
 * CHECKOUT RESULT
 * =========================================================
 */

private data class CheckoutResult(

    val success: Boolean,

    val checkoutUrl: String? = null,

    val message: String
)


/*
 * =========================================================
 * CREATE STRIPE CHECKOUT SESSION
 * =========================================================
 */

private suspend fun createCheckoutSession(
    context: android.content.Context,
    plan: String
): CheckoutResult {

    return withContext(
        Dispatchers.IO
    ) {

        try {

            /*
             * Android device ID.
             */

            val deviceId =
                Settings.Secure.getString(
                    context.contentResolver,
                    Settings.Secure.ANDROID_ID
                )


            if (deviceId.isNullOrBlank()) {

                return@withContext CheckoutResult(
                    success = false,
                    message =
                        "Could not identify this device."
                )
            }


            /*
             * Request body.
             */

            val requestBody =
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


            /*
             * Connect to Supabase Edge Function.
             */

            val connection =
                URL(
                    CREATE_CHECKOUT_FUNCTION
                )
                    .openConnection()
                        as HttpURLConnection


            connection.requestMethod =
                "POST"

            connection.connectTimeout =
                15000

            connection.readTimeout =
                30000

            connection.doOutput =
                true


            connection.setRequestProperty(
                "Content-Type",
                "application/json"
            )


            connection.setRequestProperty(
                "apikey",
                SUPABASE_PUBLISHABLE_KEY
            )


            connection.setRequestProperty(
                "Authorization",
                "Bearer $SUPABASE_PUBLISHABLE_KEY"
            )


            /*
             * Send request.
             */

            connection.outputStream
                .use { output ->

                    output.write(
                        requestBody
                            .toString()
                            .toByteArray(
                                Charsets.UTF_8
                            )
                    )
                }


            val responseCode =
                connection.responseCode


            val responseText =

                if (
                    responseCode in
                    200..299
                ) {

                    connection.inputStream
                        .bufferedReader()
                        .use {
                            it.readText()
                        }

                } else {

                    connection.errorStream
                        ?.bufferedReader()
                        ?.use {
                            it.readText()
                        }
                        ?: ""
                }


            connection.disconnect()


            /*
             * Parse response.
             */

            val json =
                if (
                    responseText.isNotBlank()
                ) {

                    JSONObject(
                        responseText
                    )

                } else {

                    JSONObject()
                }


            val success =
                json.optBoolean(
                    "success",
                    false
                )


            val checkoutUrl =
                json.optString(
                    "checkout_url",
                    ""
                )
                    .takeIf {
                        it.isNotBlank()
                    }


            val message =
                json.optString(
                    "message",
                    ""
                )


            if (
                responseCode in
                200..299 &&
                success &&
                checkoutUrl != null
            ) {

                CheckoutResult(
                    success = true,
                    checkoutUrl =
                        checkoutUrl,
                    message =
                        "Checkout created."
                )

            } else {

                CheckoutResult(
                    success = false,
                    message =
                        if (
                            message.isNotBlank()
                        ) {
                            message
                        } else {
                            "Could not create checkout."
                        }
                )
            }

        } catch (
            exception: Exception
        ) {

            CheckoutResult(
                success = false,

                message =
                    "Connection error: ${
                        exception.message
                            ?: "Unknown error"
                    }"
            )
        }
    }
}


/*
 * =========================================================
 * OPEN STRIPE CHECKOUT
 * =========================================================
 */

private fun openCheckout(
    context: android.content.Context,
    checkoutUrl: String
) {

    val intent =
        Intent(
            Intent.ACTION_VIEW,
            Uri.parse(
                checkoutUrl
            )
        )


    context.startActivity(
        intent
    )
}