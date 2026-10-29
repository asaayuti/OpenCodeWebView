package com.sayuti.opencodewebview.util

import java.net.URI

/**
 * Utility object for evaluating WebView URL navigation policies and determining whether
 * a requested URL should be loaded within the WebView or handled externally (e.g., in a browser).
 */
object WebViewUrlUtil {

    /**
     * Determines whether the specified [requestUrl] is allowed to be loaded within the WebView
     * based on its domain relationship to the [baseUrl] and its URI scheme.
     *
     * Navigation rules:
     * - If [requestUrl] or [baseUrl] is null or blank, `true` is returned by default.
     * - If [requestUrl] cannot be parsed as a valid [URI], `true` is returned as a fallback.
     * - Non-HTTP/HTTPS schemes (such as `about:blank`, `data:`, `blob:`) are always allowed inside the WebView (`true`).
     * - HTTP and HTTPS requests are allowed (`true`) if the request host matches the base host
     *   directly or is a valid subdomain (e.g., `sub.example.com` matches `example.com`).
     * - HTTP and HTTPS requests are disallowed (`false`) if the request host differs from the base host.
     *
     * @param requestUrl The URL string requested during WebView navigation.
     * @param baseUrl The baseline URL string representing the authorized domain scope.
     * @return `true` if the navigation should proceed inside the WebView; `false` otherwise.
     */
    fun isUrlAllowedInWebView(requestUrl: String?, baseUrl: String?): Boolean {
        if (requestUrl.isNullOrBlank()) return true
        if (baseUrl.isNullOrBlank()) return true

        val requestUri = try {
            URI.create(requestUrl)
        } catch (_: Exception) {
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
        } catch (_: Exception) {
            return false
        }

        val baseHost = baseUri.host?.lowercase() ?: return false

        // Check if host matches base host directly or as a subdomain
        return requestHost == baseHost || requestHost.endsWith(".$baseHost")
    }
}
