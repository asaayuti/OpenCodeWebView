package com.sayuti.opencodewebview.util

import android.util.Log

/**
 * Utility object providing helper functions for network connectivity checks,
 * pairing URL construction, and fallback-safe logging.
 */
object OpenCodeUtils {
    /** Template for authentication connection path containing a placeholder for the pairing code. */
    const val AUTH_CONNECT_PATH_TEMPLATE = "/auth/connect/%s"

    /** Default logging tag used across OpenCode utility functions. */
    const val TAG = "OpenCodePairing"

    /**
     * Builds the authentication connection path using the provided pairing code.
     *
     * @param code The pairing code to embed in the path.
     * @return The formatted authentication connect path.
     */
    fun buildAuthConnectPath(code: String): String {
        return String.format(AUTH_CONNECT_PATH_TEMPLATE, code.trim())
    }

    /**
     * Constructs a full pairing URL combining the base URL and authentication path.
     *
     * @param baseUrl The base server URL.
     * @param code The pairing code.
     * @return The complete pairing URL string.
     */
    fun buildPairUrl(baseUrl: String, code: String): String {
        val cleanBase = baseUrl.trimEnd('/')
        return "$cleanBase${buildAuthConnectPath(code)}"
    }

    /**
     * Logs a debug message using [Log.d] with fallback to [println] if Android Log is unavailable
     * (such as during local unit tests).
     *
     * @param tag The log tag. Defaults to [TAG].
     * @param message The debug message to log.
     */
    fun logDebug(tag: String = TAG, message: String) {
        try {
            Log.d(tag, message)
        } catch (_: Throwable) {
            println("[$tag] $message")
        }
    }

    /**
     * Checks whether an active network connection with internet capability is currently available.
     *
     * @param context The application or activity context.
     * @return `true` if an internet-capable active network exists, `false` otherwise.
     */
    fun isNetworkAvailable(context: android.content.Context): Boolean {
        return try {
            val connectivityManager = context.getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager ?: return false
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            capabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (_: Exception) {
            false
        }
    }
}
