package com.example.lotogenerator

import android.content.Context
import android.provider.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

object AccessManager {

    private const val SUPABASE_URL =
        "https://ghwtwpuyswthueqysvnx.supabase.co"

    private const val SUPABASE_PUBLISHABLE_KEY =
        "sb_publishable_go_fk2r0PMvz1vcr1QJ_Iw_UjnlzdD9"

    private const val FUNCTION_URL =
        "$SUPABASE_URL/functions/v1/validate-access-code"

    private const val PREFS_NAME =
        "loto_access"

    private const val ACCESS_GRANTED =
        "access_granted"

    private const val EXPIRES_AT =
        "expires_at"

    private const val PERMANENT_ACCESS =
        "permanent_access"

    private const val ACCESS_CODE =
        "access_code"

    private val client =
        OkHttpClient()

    /*
     * Get this Android device's ID.
     */
    private fun getDeviceId(
        context: Context
    ): String {

        return Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        ) ?: "unknown-device"
    }

    /*
     * Check locally stored access.
     *
     * The server will also be checked when
     * the application starts.
     */
    fun isActivated(
        context: Context
    ): Boolean {

        val prefs =
            context.getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )

        /*
         * Permanent access
         */
        if (
            prefs.getBoolean(
                PERMANENT_ACCESS,
                false
            )
        ) {
            return true
        }

        /*
         * Normal access
         */
        if (
            !prefs.getBoolean(
                ACCESS_GRANTED,
                false
            )
        ) {
            return false
        }

        val expiresAt =
            prefs.getLong(
                EXPIRES_AT,
                0L
            )

        return expiresAt >
                System.currentTimeMillis()
    }

    /*
     * Check whether this device has permanent access.
     */
    fun isGodMode(
        context: Context
    ): Boolean {

        return context
            .getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )
            .getBoolean(
                PERMANENT_ACCESS,
                false
            )
    }

    /*
     * Activate an access code.
     */
    suspend fun activate(
        context: Context,
        code: String
    ): ActivationResult {

        return callServer(
            context = context,
            code = code,
            action = "activate"
        )
    }

    /*
     * Re-check the currently activated
     * access code against Supabase.
     */
    suspend fun validateCurrentAccess(
        context: Context
    ): ActivationResult {

        val prefs =
            context.getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )

        val savedCode =
            prefs.getString(
                ACCESS_CODE,
                null
            )

        /*
         * Nothing to validate.
         */
        if (savedCode.isNullOrEmpty()) {

            return ActivationResult(
                success = false,
                message =
                    "No access code is stored."
            )
        }

        return callServer(
            context = context,
            code = savedCode,
            action = "validate"
        )
    }

    /*
     * Communicate with Supabase.
     */
    private suspend fun callServer(
        context: Context,
        code: String,
        action: String
    ): ActivationResult {

        return withContext(Dispatchers.IO) {

            try {

                val cleanCode =
                    code.trim().uppercase()

                if (cleanCode.isEmpty()) {

                    return@withContext ActivationResult(
                        success = false,
                        message =
                            "Please enter an access code."
                    )
                }

                val deviceId =
                    getDeviceId(context)

                val json =
                    JSONObject()

                json.put(
                    "code",
                    cleanCode
                )

                json.put(
                    "device_id",
                    deviceId
                )

                json.put(
                    "action",
                    action
                )

                val body =
                    json.toString()
                        .toRequestBody(
                            "application/json; charset=utf-8"
                                .toMediaType()
                        )

                val request =
                    Request.Builder()
                        .url(FUNCTION_URL)
                        .post(body)
                        .addHeader(
                            "Content-Type",
                            "application/json"
                        )
                        .addHeader(
                            "apikey",
                            SUPABASE_PUBLISHABLE_KEY
                        )
                        .addHeader(
                            "Authorization",
                            "Bearer $SUPABASE_PUBLISHABLE_KEY"
                        )
                        .build()

                client
                    .newCall(request)
                    .execute()
                    .use { response ->

                        val responseBody =
                            response.body
                                ?.string()
                                ?: ""

                        val result =
                            if (
                                responseBody.isNotEmpty()
                            ) {
                                JSONObject(
                                    responseBody
                                )
                            } else {
                                JSONObject()
                            }

                        val success =
                            result.optBoolean(
                                "success",
                                false
                            )

                        /*
                         * Server rejected access.
                         */
                        if (!success) {

                            /*
                             * Remove local access.
                             */
                            clearAccess(context)

                            return@withContext ActivationResult(
                                success = false,
                                message =
                                    result.optString(
                                        "message",
                                        "Access denied."
                                    )
                            )
                        }

                        val permanent =
                            result.optBoolean(
                                "permanent",
                                false
                            )

                        /*
                         * Permanent access.
                         */
                        if (permanent) {

                            context
                                .getSharedPreferences(
                                    PREFS_NAME,
                                    Context.MODE_PRIVATE
                                )
                                .edit()
                                .putBoolean(
                                    ACCESS_GRANTED,
                                    true
                                )
                                .putBoolean(
                                    PERMANENT_ACCESS,
                                    true
                                )
                                .putString(
                                    ACCESS_CODE,
                                    cleanCode
                                )
                                .remove(EXPIRES_AT)
                                .apply()

                            return@withContext ActivationResult(
                                success = true,
                                permanent = true,
                                message =
                                    "Permanent access is valid."
                            )
                        }

                        /*
                         * Normal access.
                         */
                        val expiresAtString =
                            result.optString(
                                "expires_at",
                                ""
                            )

                        val expiresAtMillis =
                            try {

                                java.time.Instant
                                    .parse(
                                        expiresAtString
                                    )
                                    .toEpochMilli()

                            } catch (
                                e: Exception
                            ) {

                                0L
                            }

                        if (
                            expiresAtMillis <= 0L
                        ) {

                            clearAccess(context)

                            return@withContext ActivationResult(
                                success = false,
                                message =
                                    "Invalid expiration date."
                            )
                        }

                        /*
                         * Save the current valid access.
                         */
                        context
                            .getSharedPreferences(
                                PREFS_NAME,
                                Context.MODE_PRIVATE
                            )
                            .edit()
                            .putBoolean(
                                ACCESS_GRANTED,
                                true
                            )
                            .putBoolean(
                                PERMANENT_ACCESS,
                                false
                            )
                            .putString(
                                ACCESS_CODE,
                                cleanCode
                            )
                            .putLong(
                                EXPIRES_AT,
                                expiresAtMillis
                            )
                            .apply()

                        ActivationResult(
                            success = true,
                            permanent = false,
                            expiresAt =
                                expiresAtMillis,
                            message =
                                if (
                                    action == "validate"
                                ) {
                                    "Access is still valid."
                                } else {
                                    "Access activated successfully."
                                }
                        )
                    }

            } catch (e: Exception) {

                /*
                 * If the server cannot be reached,
                 * don't immediately erase valid access.
                 *
                 * This allows temporary offline use.
                 */
                ActivationResult(
                    success = false,
                    connectionError = true,
                    message =
                        "Unable to connect to the access server."
                )
            }
        }
    }

    /*
     * Remove local access information.
     */
    private fun clearAccess(
        context: Context
    ) {

        context
            .getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )
            .edit()
            .clear()
            .apply()
    }

    /*
     * Remaining access time.
     */
    fun getRemainingTime(
        context: Context
    ): Long {

        if (isGodMode(context)) {
            return Long.MAX_VALUE
        }

        val expiresAt =
            context
                .getSharedPreferences(
                    PREFS_NAME,
                    Context.MODE_PRIVATE
                )
                .getLong(
                    EXPIRES_AT,
                    0L
                )

        return maxOf(
            0L,
            expiresAt -
                    System.currentTimeMillis()
        )
    }

    /*
     * Remaining days.
     */
    fun getRemainingDays(
        context: Context
    ): Long {

        if (isGodMode(context)) {
            return Long.MAX_VALUE
        }

        return getRemainingTime(context) /
                (
                        24L *
                                60L *
                                60L *
                                1000L
                        )
    }

    data class ActivationResult(
        val success: Boolean,
        val permanent: Boolean = false,
        val expiresAt: Long = 0L,
        val connectionError: Boolean = false,
        val message: String
    )
}