package com.sayuti.opencodewebview.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WebViewUrlUtilTest {

    @Test
    fun testSameHostAllowed() {
        val baseUrl = "https://example.com/app"
        assertTrue(WebViewUrlUtil.isUrlAllowedInWebView("https://example.com/app/dashboard", baseUrl))
        assertTrue(WebViewUrlUtil.isUrlAllowedInWebView("https://example.com/", baseUrl))
    }

    @Test
    fun testSubdomainAllowed() {
        val baseUrl = "https://example.com"
        assertTrue(WebViewUrlUtil.isUrlAllowedInWebView("https://sub.example.com/page", baseUrl))
    }

    @Test
    fun testDifferentHostBlocked() {
        val baseUrl = "https://example.com"
        assertFalse(WebViewUrlUtil.isUrlAllowedInWebView("https://google.com", baseUrl))
    }

    @Test
    fun testNonHttpSchemesAllowed() {
        val baseUrl = "https://example.com"
        assertTrue(WebViewUrlUtil.isUrlAllowedInWebView("about:blank", baseUrl))
        assertTrue(WebViewUrlUtil.isUrlAllowedInWebView("data:text/html,...", baseUrl))
        assertTrue(WebViewUrlUtil.isUrlAllowedInWebView("blob:https://...", baseUrl))
    }
}
