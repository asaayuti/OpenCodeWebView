package com.sayuti.opencodewebview.ui.webview

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.PermissionRequest
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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sayuti.opencodewebview.ui.theme.OpenCodeWebViewTheme
import com.sayuti.opencodewebview.util.OpenCodeUtils
import com.sayuti.opencodewebview.util.WebViewUrlUtil

/**
 * A Composable screen that hosts a fully-featured [WebView] with support for JavaScript,
 * DOM storage, file choosing, camera/audio permissions, fullscreen video/HTML5 media,
 * back button handling, offline/network monitoring, and authentication error handling.
 *
 * @param initialUrl The initial URL to load in the WebView.
 * @param baseUrl The base URL used for validating whether navigation stays within the WebView or opens externally.
 * @param onChangeServer Callback invoked when the user requests re-pairing or server reconfiguration.
 * @param modifier Optional [Modifier] to be applied to the layout.
 * @param doubleBackToExitEnabled Whether pressing back twice within 2 seconds exits the activity. Defaults to true.
 * @param webViewViewModel Optional custom [WebViewViewModel] instance. If null, a default instance is created.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebViewScreen(
    initialUrl: String,
    baseUrl: String,
    onChangeServer: () -> Unit,
    modifier: Modifier = Modifier,
    doubleBackToExitEnabled: Boolean = true,
    webViewViewModel: WebViewViewModel? = null
) {
    val context = LocalContext.current
    val isPreview = LocalInspectionMode.current
    
    val application = context.applicationContext as? android.app.Application
    val viewModel = webViewViewModel ?: if (application != null) {
        androidx.lifecycle.viewmodel.compose.viewModel(
            factory = WebViewViewModel.Factory(application, com.sayuti.opencodewebview.data.repository.ServerPreferencesRepository(context))
        )
    } else {
        androidx.lifecycle.viewmodel.compose.viewModel(
            factory = WebViewViewModel.Factory(com.sayuti.opencodewebview.data.repository.ServerPreferencesRepository(context))
        )
    }
    val isConnected by viewModel.isConnected.collectAsStateWithLifecycle()

    var isLoading by remember { mutableStateOf(false) }
    var errorState by remember { mutableStateOf<WebViewError?>(null) }
    var pageTitle by remember { mutableStateOf("") }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var canGoBack by remember { mutableStateOf(false) }
    var pendingFilePathCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }
    var pendingFileChooserParams by remember { mutableStateOf<WebChromeClient.FileChooserParams?>(null) }
    var pendingPermissionRequest by remember { mutableStateOf<PermissionRequest?>(null) }
    var backPressedTime by remember { mutableLongStateOf(0L) }
    
    // Fullscreen custom view state for HTML5 video / fullscreen API
    var customView by remember { mutableStateOf<android.view.View?>(null) }
    var customViewCallback by remember { mutableStateOf<WebChromeClient.CustomViewCallback?>(null) }

    val toastContext = context.applicationContext
    val activity = context as? Activity

    // Real-time network monitoring effect
    if (!isPreview) {
        androidx.compose.runtime.LaunchedEffect(isConnected) {
            if (!isConnected) {
                if (errorState == null) {
                    errorState = WebViewError(
                        message = "No internet connection. Please check your network settings and try again.",
                        isNetworkError = true
                    )
                }
            } else {
                if (errorState?.isNetworkError == true) {
                    errorState = null
                    webViewRef?.reload()
                }
            }
        }
    }

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

        // Ensure WebView is properly destroyed on disposal to prevent memory leaks
        DisposableEffect(Unit) {
            onDispose {
                webViewRef?.let { webView ->
                    (webView.parent as? ViewGroup)?.removeView(webView)
                    webView.destroy()
                }
                webViewRef = null
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
                results = arrayOf(dataString.toUri())
            }
        }

        pendingFilePathCallback?.onReceiveValue(results)
        pendingFilePathCallback = null
        pendingFileChooserParams = null
    }

    // Camera Permission Launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            pendingPermissionRequest?.let { request ->
                request.grant(request.resources)
                pendingPermissionRequest = null
            }
            pendingFilePathCallback?.let { callback ->
                val params = pendingFileChooserParams
                val intent = try {
                    params?.createIntent()
                } catch (_: Exception) {
                    null
                }
                val finalIntent = intent ?: Intent(Intent.ACTION_GET_CONTENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "*/*"
                }
                try {
                    fileChooserLauncher.launch(finalIntent)
                } catch (_: Exception) {
                    callback.onReceiveValue(null)
                    pendingFilePathCallback = null
                    pendingFileChooserParams = null
                }
            }
        } else {
            Toast.makeText(toastContext, "Camera permission is required to access the camera", Toast.LENGTH_SHORT).show()
            pendingPermissionRequest?.deny()
            pendingPermissionRequest = null
            pendingFilePathCallback?.onReceiveValue(null)
            pendingFilePathCallback = null
            pendingFileChooserParams = null
        }
    }

    val launchFileChooserOrRequestPermission: (ValueCallback<Array<Uri>>?, WebChromeClient.FileChooserParams?) -> Boolean = { callback, params ->
        pendingFilePathCallback?.onReceiveValue(null)
        pendingFilePathCallback = callback
        pendingFileChooserParams = params

        val hasCameraPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (hasCameraPermission) {
            val intent = try {
                params?.createIntent()
            } catch (_: Exception) {
                null
            }
            val finalIntent = intent ?: Intent(Intent.ACTION_GET_CONTENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "*/*"
            }
            try {
                fileChooserLauncher.launch(finalIntent)
                true
            } catch (_: Exception) {
                pendingFilePathCallback?.onReceiveValue(null)
                pendingFilePathCallback = null
                pendingFileChooserParams = null
                false
            }
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            true
        }
    }

    // Back Button Handler
    BackHandler(enabled = true) {
        if (customView != null) {
            customViewCallback?.onCustomViewHidden()
            customView = null
            customViewCallback = null
            activity?.window?.decorView?.let { decorView ->
                WindowCompat.getInsetsController(activity.window, decorView).show(
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
                            allowFileAccess = true
                            allowContentAccess = true
                            mediaPlaybackRequiresUserGesture = false
                        }

                        webChromeClient = object : WebChromeClient() {
                            override fun onReceivedTitle(view: WebView?, title: String?) {
                                super.onReceivedTitle(view, title)
                                if (!title.isNullOrBlank()) {
                                    pageTitle = title
                                }
                            }

                            override fun onPermissionRequest(request: PermissionRequest?) {
                                if (request == null) return
                                val resources = request.resources
                                val needsCameraOrAudio = resources.contains(PermissionRequest.RESOURCE_VIDEO_CAPTURE) ||
                                        resources.contains(PermissionRequest.RESOURCE_AUDIO_CAPTURE)

                                if (needsCameraOrAudio) {
                                    val hasCameraPermission = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.CAMERA
                                    ) == PackageManager.PERMISSION_GRANTED

                                    val hasAudioPermission = !resources.contains(PermissionRequest.RESOURCE_AUDIO_CAPTURE) ||
                                            ContextCompat.checkSelfPermission(
                                                context,
                                                Manifest.permission.RECORD_AUDIO
                                            ) == PackageManager.PERMISSION_GRANTED

                                    if (hasCameraPermission && hasAudioPermission) {
                                        request.grant(resources)
                                    } else {
                                        pendingPermissionRequest = request
                                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                    }
                                } else {
                                    request.grant(resources)
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
                                    WindowCompat.getInsetsController(activity.window, decorView).hide(
                                        WindowInsetsCompat.Type.systemBars()
                                    )
                                }
                            }

                            override fun onHideCustomView() {
                                activity?.window?.decorView?.let { decorView ->
                                    WindowCompat.getInsetsController(activity.window, decorView).show(
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
                                return launchFileChooserOrRequestPermission(filePathCallback, fileChooserParams)
                            }
                        }

                        webViewClient = object : WebViewClient() {
                            override fun onRenderProcessGone(
                                view: WebView?,
                                detail: android.webkit.RenderProcessGoneDetail?
                            ): Boolean {
                                val reason = if (detail?.didCrash() == true) {
                                    "Web renderer crashed"
                                } else {
                                    "Web renderer killed by system (low memory)"
                                }

                                errorState = WebViewError(
                                    message = "$reason. Please tap Retry to reload.",
                                    isNetworkError = false
                                )
                                view?.let { webView ->
                                    (webView.parent as? ViewGroup)?.removeView(webView)
                                    webView.destroy()
                                }
                                webViewRef = null
                                return true
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                isLoading = false
                                canGoBack = view?.canGoBack() == true
                                val title = view?.title
                                if (!title.isNullOrBlank()) {
                                    pageTitle = title
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
                                val requestUrl = url ?: return false
                                return handleUrlOverride(requestUrl, baseUrl, ctx)
                            }

                            override fun onReceivedError(
                                view: WebView?,
                                request: WebResourceRequest?,
                                error: WebResourceError?
                            ) {
                                super.onReceivedError(view, request, error)
                                if (request?.isForMainFrame == true) {
                                    isLoading = false
                                    val errorCode = error?.errorCode ?: 0
                                    val description = error?.description?.toString() ?: "Failed to load page"

                                    val isNetworkDown = !OpenCodeUtils.isNetworkAvailable(ctx) ||
                                            errorCode == ERROR_HOST_LOOKUP ||
                                            errorCode == ERROR_CONNECT ||
                                            errorCode == ERROR_TIMEOUT ||
                                            errorCode == ERROR_IO ||
                                            errorCode == ERROR_FAILED_SSL_HANDSHAKE

                                    val message = if (isNetworkDown) {
                                        "No internet connection. Please check your network settings and try again."
                                    } else {
                                        description
                                    }

                                    errorState = WebViewError(
                                        message = message,
                                        statusCode = errorCode,
                                        isAuthError = false,
                                        isNetworkError = isNetworkDown
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

                        if (!OpenCodeUtils.isNetworkAvailable(ctx)) {
                            errorState = WebViewError(
                                message = "No internet connection. Please check your network settings and try again.",
                                isNetworkError = true
                            )
                        } else {
                            loadUrl(initialUrl)
                        }
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

        // Error Overlay Screen
        errorState?.let { currentError ->
            ErrorScreenOverlay(
                error = currentError,
                onRetry = {
                    if (currentError.isNetworkError && !OpenCodeUtils.isNetworkAvailable(context)) {
                        Toast.makeText(context, "Still no internet connection", Toast.LENGTH_SHORT).show()
                    } else {
                        errorState = null
                        webViewRef?.reload()
                    }
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

/**
 * Clears all WebView session data including cookies, local web storage, cache, and navigation history.
 *
 * @param webView The [WebView] instance to clear cache and history from, or null if only storage/cookies need clearing.
 */
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

/**
 * Determines whether a requested URL should be loaded internally within the WebView
 * or opened externally in the system browser based on whether it matches the allowed [baseUrl].
 *
 * @param url The target URL requested for loading.
 * @param baseUrl The allowed base URL for internal webview navigation.
 * @param context The [android.content.Context] used to launch external browser intents.
 * @return true if the URL was handled externally (or overridden), false to allow WebView to load it.
 */
private fun handleUrlOverride(url: String, baseUrl: String, context: android.content.Context): Boolean {
    if (WebViewUrlUtil.isUrlAllowedInWebView(url, baseUrl)) {
        return false // Allow WebView to load URL internally
    }

    // Host differs or external link -> open in system browser
    return try {
        val intent = Intent(Intent.ACTION_VIEW, url.toUri())
        context.startActivity(intent)
        true
    } catch (e: Exception) {
        OpenCodeUtils.logDebug(OpenCodeUtils.TAG, "Failed to open external URL ($url): ${e.message}")
        false
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
