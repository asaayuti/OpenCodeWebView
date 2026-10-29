package com.sayuti.opencodewebview.ui.webview

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
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sayuti.opencodewebview.ui.theme.OpenCodeWebViewTheme

/**
 * A Composable overlay screen displayed when an error (such as network loss, HTTP 401/403, or page load failure) occurs.
 *
 * @param error The [WebViewError] object containing error details, messages, and state flags.
 * @param onRetry Callback invoked when the user clicks the Retry button.
 * @param onRePair Callback invoked when the user clicks the Re-pair button (for authentication errors).
 * @param modifier Optional [Modifier] to be applied to the overlay layout.
 */
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
                    imageVector = if (error.isNetworkError) Icons.Rounded.WifiOff else Icons.Rounded.ErrorOutline,
                    contentDescription = "Error",
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.error
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = when {
                        error.isAuthError -> "Authentication Required"
                        error.isNetworkError -> "No Internet Connection"
                        else -> "Page Load Error"
                    },
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
                    if (!error.isNetworkError) {
                        OutlinedButton(
                            onClick = onRePair,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Re-pair")
                        }
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
