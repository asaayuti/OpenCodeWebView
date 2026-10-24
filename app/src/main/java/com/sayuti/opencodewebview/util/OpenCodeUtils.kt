package com.sayuti.opencodewebview.util

import android.util.Log

object OpenCodeUtils {
    const val AUTH_CONNECT_PATH_TEMPLATE = "/auth/connect/%s"
    const val TAG = "OpenCodePairing"

    fun buildAuthConnectPath(code: String): String {
        return String.format(AUTH_CONNECT_PATH_TEMPLATE, code.trim())
    }

    fun buildPairUrl(baseUrl: String, code: String): String {
        val cleanBase = baseUrl.trimEnd('/')
        return "$cleanBase${buildAuthConnectPath(code)}"
    }

    fun logDebug(tag: String = TAG, message: String) {
        try {
            Log.d(tag, message)
        } catch (e: Throwable) {
            println("[$tag] $message")
        }
    }
}
