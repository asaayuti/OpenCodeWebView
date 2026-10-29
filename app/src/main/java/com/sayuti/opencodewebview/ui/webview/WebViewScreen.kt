package com.sayuti.opencodewebview.ui.webview

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.sayuti.opencodewebview.ui.theme.OpenCodeWebViewTheme
import com.sayuti.opencodewebview.util.OpenCodeUtils
import com.sayuti.opencodewebview.util.WebViewUrlUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebViewScreen(
    initialUrl: String,
    baseUrl: String,
    onChangeServer: () -> Unit,
    modifier: Modifier = Modifier,
    doubleBackToExitEnabled: Boolean = true
) {
    val context = LocalContext.current
    var isRefreshing by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorState by remember { mutableStateOf<WebViewError?>(null) }
    var pageTitle by remember { mutableStateOf("") }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var canGoBack by remember { mutableStateOf(false) }
    var pendingFilePathCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }
    var backPressedTime by remember { mutableStateOf(0L) }
    
    // Fullscreen custom view state for HTML5 video / fullscreen API
    var customView by remember { mutableStateOf<android.view.View?>(null) }
    var customViewCallback by remember { mutableStateOf<WebChromeClient.CustomViewCallback?>(null) }

    val toastContext = context.applicationContext
    val isPreview = LocalInspectionMode.current
    val activity = context as? Activity

    // Ensure cookies are flushed to disk on lifecycle events (e.g. app paused or backgrounded)
    if (!isPreview) {
        val lifecycleOwner = LocalLifecycleOwner.current
        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                    CookieManager.getInstance().flush()
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
                CookieManager.getInstance().flush()
            }
        }
    }

    // File Chooser Launcher
    val fileChooserLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data
        val resultCode = result.resultCode
        var results: Array<Uri>? = null

        if (resultCode == Activity.RESULT_OK && data != null) {
            val dataString = data.dataString
            val clipData = data.clipData
            if (clipData != null && clipData.itemCount > 0) {
                results = Array(clipData.itemCount) { i -> clipData.getItemAt(i).uri }
            } else if (dataString != null) {
                results = arrayOf(Uri.parse(dataString))
            }
        }

        pendingFilePathCallback?.onReceiveValue(results)
        pendingFilePathCallback = null
    }

    // Back Button Handler
    BackHandler(enabled = true) {
        if (customView != null) {
            customViewCallback?.onCustomViewHidden()
            customView = null
            customViewCallback = null
            activity?.window?.decorView?.let { decorView ->
                WindowCompat.getInsetsController(activity.window, decorView)?.show(
                    WindowInsetsCompat.Type.systemBars()
                )
            }
        } else if (errorState != null) {
            errorState = null
            if (webViewRef?.canGoBack() == true) {
                webViewRef?.goBack()
                canGoBack = webViewRef?.canGoBack() == true
            }
        } else if (webViewRef?.canGoBack() == true) {
            webViewRef?.goBack()
            canGoBack = webViewRef?.canGoBack() == true
        } else if (doubleBackToExitEnabled) {
            val currentTime = System.currentTimeMillis()
            if (currentTime - backPressedTime < 2000L) {
                activity?.finish()
            } else {
                backPressedTime = currentTime
                Toast.makeText(toastContext, "Tekan sekali lagi untuk keluar", Toast.LENGTH_SHORT).show()
            }
        } else {
            activity?.finish()
        }
    }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                isRefreshing = true
                errorState = null
                webViewRef?.reload()
            },
            modifier = Modifier.fillMaxSize()
        ) {
            if (isPreview) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "WebView Preview\n$initialUrl",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )

                            // Configure CookieManager for persistent session storage
                            val cookieManager = CookieManager.getInstance()
                            cookieManager.setAcceptCookie(true)
                            cookieManager.setAcceptThirdPartyCookies(this, true)

                            // WebSettings configuration
                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                cacheMode = WebSettings.LOAD_DEFAULT
                                allowFileAccess = false
                                allowContentAccess = false
                            }

                            webChromeClient = object : WebChromeClient() {
                                override fun onReceivedTitle(view: WebView?, title: String?) {
                                    super.onReceivedTitle(view, title)
                                    if (!title.isNullOrBlank()) {
                                        pageTitle = title
                                    }
                                }

                                override fun onShowCustomView(
                                    view: android.view.View?,
                                    callback: CustomViewCallback?
                                ) {
                                    if (customView != null) {
                                        callback?.onCustomViewHidden()
                                        return
                                    }
                                    customView = view
                                    customViewCallback = callback
                                    activity?.window?.decorView?.let { decorView ->
                                        WindowCompat.getInsetsController(activity.window, decorView)?.hide(
                                            WindowInsetsCompat.Type.systemBars()
                                        )
                                    }
                                }

                                override fun onHideCustomView() {
                                    activity?.window?.decorView?.let { decorView ->
                                        WindowCompat.getInsetsController(activity.window, decorView)?.show(
                                            WindowInsetsCompat.Type.systemBars()
                                        )
                                    }
                                    customViewCallback?.onCustomViewHidden()
                                    customView = null
                                    customViewCallback = null
                                }

                                override fun onShowFileChooser(
                                    webView: WebView?,
                                    filePathCallback: ValueCallback<Array<Uri>>?,
                                    fileChooserParams: FileChooserParams?
                                ): Boolean {
                                    // Reset any previously pending callback
                                    pendingFilePathCallback?.onReceiveValue(null)
                                    pendingFilePathCallback = filePathCallback

                                    val intent = try {
                                        fileChooserParams?.createIntent()
                                    } catch (e: Exception) {
                                        null
                                    }

                                    val finalIntent = intent ?: Intent(Intent.ACTION_GET_CONTENT).apply {
                                        addCategory(Intent.CATEGORY_OPENABLE)
                                        type = "*/*"
                                    }

                                    return try {
                                        fileChooserLauncher.launch(finalIntent)
                                        true
                                    } catch (e: Exception) {
                                        pendingFilePathCallback?.onReceiveValue(null)
                                        pendingFilePathCallback = null
                                        false
                                    }
                                }
                            }

                            webViewClient = object : WebViewClient() {
                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    isRefreshing = false
                                    isLoading = false
                                    canGoBack = view?.canGoBack() == true
                                    if (!view?.title.isNullOrBlank()) {
                                        pageTitle = view?.title ?: ""
                                    }
                                    CookieManager.getInstance().flush()
                                }

                                override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                                    super.onPageStarted(view, url, favicon)
                                    isLoading = true
                                    canGoBack = view?.canGoBack() == true
                                }

                                override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                                    super.doUpdateVisitedHistory(view, url, isReload)
                                    canGoBack = view?.canGoBack() == true
                                }

                                override fun shouldOverrideUrlLoading(
                                    view: WebView?,
                                    request: WebResourceRequest?
                                ): Boolean {
                                    val requestUrl = request?.url?.toString() ?: return false
                                    return handleUrlOverride(requestUrl, baseUrl, ctx)
                                }

                                @Deprecated("Deprecated in Java")
                                override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                                    if (url == null) return false
                                    return handleUrlOverride(url, baseUrl, ctx)
                                }

                                override fun onReceivedError(
                                    view: WebView?,
                                    request: WebResourceRequest?,
                                    error: WebResourceError?
                                ) {
                                    super.onReceivedError(view, request, error)
                                    if (request?.isForMainFrame == true) {
                                        isRefreshing = false
                                        isLoading = false
                                        val description = error?.description?.toString() ?: "Failed to load page"
                                        errorState = WebViewError(
                                            message = description,
                                            statusCode = error?.errorCode,
                                            isAuthError = false
                                        )
                                    }
                                }

                                override fun onReceivedHttpError(
                                    view: WebView?,
                                    request: WebResourceRequest?,
                                    errorResponse: WebResourceResponse?
                                ) {
                                    super.onReceivedHttpError(view, request, errorResponse)
                                    if (request?.isForMainFrame == true) {
                                        val statusCode = errorResponse?.statusCode ?: 0
                                        if (statusCode == 401 || statusCode == 403) {
                                            isRefreshing = false
                                            isLoading = false
                                            val message = when (statusCode) {
                                                401 -> "Authentication required (HTTP 401). Please re-pair your server."
                                                403 -> "Access forbidden (HTTP 403). Please re-pair your server."
                                                else -> "HTTP Error $statusCode"
                                            }
                                            errorState = WebViewError(
                                                message = message,
                                                statusCode = statusCode,
                                                isAuthError = true
                                            )
                                        }
                                    }
                                }
                            }

                            loadUrl(initialUrl)
                            webViewRef = this
                        }
                    },
                    update = { webView ->
                        webViewRef = webView
                        canGoBack = webView.canGoBack()
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Error Overlay Screen
        errorState?.let { currentError ->
            ErrorScreenOverlay(
                error = currentError,
                onRetry = {
                    errorState = null
                    isRefreshing = true
                    webViewRef?.reload()
                },
                onRePair = {
                    errorState = null
                    clearWebViewData(webViewRef)
                    onChangeServer()
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        if (isLoading) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
            )
        }

        // Fullscreen Custom View Overlay (for HTML5 video / fullscreen API)
        customView?.let { view ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                AndroidView(
                    factory = { _ -> view },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

private fun clearWebViewData(webView: WebView? = null) {
    try {
        val cookieManager = CookieManager.getInstance()
        cookieManager.removeAllCookies(null)
        cookieManager.flush()
        WebStorage.getInstance().deleteAllData()
        webView?.clearCache(true)
        webView?.clearHistory()
    } catch (e: Exception) {
        OpenCodeUtils.logDebug(OpenCodeUtils.TAG, "Failed to clear web view data: ${e.message}")
    }
}

private fun handleUrlOverride(url: String, baseUrl: String, context: android.content.Context): Boolean {
    if (WebViewUrlUtil.isUrlAllowedInWebView(url, baseUrl)) {
        return false // Allow WebView to load URL internally
    }

    // Host differs or external link -> open in system browser
    return try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
        true
    } catch (e: Exception) {
        OpenCodeUtils.logDebug(OpenCodeUtils.TAG, "Failed to open external URL ($url): ${e.message}")
        false
    }
}

@Composable
fun ErrorScreenOverlay(
    error: WebViewError,
    onRetry: () -> Unit,
    onRePair: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.ErrorOutline,
                    contentDescription = "Error",
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.error
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = if (error.isAuthError) "Authentication Required" else "Page Load Error",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = error.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onRePair,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Re-pair")
                    }

                    Button(
                        onClick = onRetry,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Retry")
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ErrorScreenOverlayPreview() {
    OpenCodeWebViewTheme {
        ErrorScreenOverlay(
            error = WebViewError(
                message = "Access forbidden (HTTP 403). Please re-pair your server.",
                statusCode = 403,
                isAuthError = true
            ),
            onRetry = {},
            onRePair = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun WebViewScreenPreview() {
    OpenCodeWebViewTheme {
        WebViewScreen(
            initialUrl = "https://example.com",
            baseUrl = "https://example.com",
            onChangeServer = {}
        )
    }
}
