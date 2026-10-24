package com.sayuti.opencodewebview.util

import java.net.URI

object WebViewUrlUtil {

    /**
     * Checks if requested URL should be loaded inside the WebView or opened in an external browser.
     * Returns true if request host matches or is a subdomain of base host, or if scheme is non-HTTP(S) (e.g. data, blob, about).
     * Returns false if request host differs from base host (for HTTP/HTTPS requests).
     */
    fun isUrlAllowedInWebView(requestUrl: String?, baseUrl: String?): Boolean {
        if (requestUrl.isNullOrBlank()) return true
        if (baseUrl.isNullOrBlank()) return true

        val requestUri = try {
            URI.create(requestUrl)
        } catch (e: Exception) {
            return true
        }

        val scheme = requestUri.scheme?.lowercase() ?: return true

        // Allow internal or custom schemes (e.g., about:blank, blob:, data:) inside WebView
        if (scheme != "http" && scheme != "https") {
            return true
        }

        val requestHost = requestUri.host?.lowercase() ?: return true

        val baseUri = try {
            URI.create(baseUrl)
        } catch (e: Exception) {
            return false
        }

        val baseHost = baseUri.host?.lowercase() ?: return false

        // Check if host matches base host directly or as a subdomain
        return requestHost == baseHost || requestHost.endsWith(".$baseHost")
    }
}
