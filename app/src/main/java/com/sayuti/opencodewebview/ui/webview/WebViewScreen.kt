package com.sayuti.opencodewebview.ui.webview

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.sayuti.opencodewebview.ui.theme.OpenCodeWebViewTheme
import com.sayuti.opencodewebview.util.OpenCodeUtils
import com.sayuti.opencodewebview.util.WebViewUrlUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebViewScreen(
    initialUrl: String,
    baseUrl: String,
    onChangeServer: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isRefreshing by remember { mutableStateOf(false) }
    var errorState by remember { mutableStateOf<WebViewError?>(null) }
    var menuExpanded by remember { mutableStateOf(false) }
    var pageTitle by remember { mutableStateOf("") }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var pendingFilePathCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }

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
    val canGoBack = webViewRef?.canGoBack() == true
    BackHandler(enabled = errorState != null || canGoBack) {
        if (errorState != null) {
            errorState = null
            if (canGoBack) {
                webViewRef?.goBack()
            }
        } else if (canGoBack) {
            webViewRef?.goBack()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize()
    ) { _ ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(PaddingValues(0.dp))
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
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )

                            // WebSettings configuration
                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
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
                                    if (!view?.title.isNullOrBlank()) {
                                        pageTitle = view?.title ?: ""
                                    }
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
                    },
                    modifier = Modifier.fillMaxSize()
                )
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
                        onChangeServer()
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Floating menu button (always present in fullscreen mode)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.TopEnd
            ) {
                Box {
                    FloatingActionButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.size(44.dp),
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Menu options"
                        )
                    }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        WebViewMenuContent(
                            onChangeServer = onChangeServer,
                            onDismiss = { menuExpanded = false }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WebViewMenuContent(
    onChangeServer: () -> Unit,
    onDismiss: () -> Unit
) {
    DropdownMenuItem(
        text = { Text("Change server") },
        leadingIcon = {
            Icon(Icons.Rounded.Dns, contentDescription = null)
        },
        onClick = {
            onDismiss()
            onChangeServer()
        }
    )
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
