package com.sayuti.opencodewebview.ui.webview

data class WebViewError(
    val message: String,
    val statusCode: Int? = null,
    val isAuthError: Boolean = false,
    val isNetworkError: Boolean = false
)
