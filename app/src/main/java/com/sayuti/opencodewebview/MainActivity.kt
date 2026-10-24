package com.sayuti.opencodewebview

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sayuti.opencodewebview.data.repository.ServerPreferencesRepository
import com.sayuti.opencodewebview.ui.setup.SetupScreen
import com.sayuti.opencodewebview.ui.setup.SetupViewModel
import com.sayuti.opencodewebview.ui.theme.OpenCodeWebViewTheme
import com.sayuti.opencodewebview.ui.webview.WebViewScreen
import com.sayuti.opencodewebview.ui.webview.WebViewViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val repository = ServerPreferencesRepository(applicationContext)

        setContent {
            OpenCodeWebViewTheme {
                val baseUrl by repository.baseUrlFlow.collectAsStateWithLifecycle(initialValue = null)
                var currentPairUrl by rememberSaveable { mutableStateOf<String?>(null) }

                val webViewViewModel: WebViewViewModel = viewModel(
                    factory = WebViewViewModel.Factory(repository)
                )

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    if (baseUrl.isNullOrBlank() && currentPairUrl == null) {
                        val setupViewModel: SetupViewModel = viewModel(
                            factory = SetupViewModel.Factory(repository)
                        )
                        SetupScreen(
                            viewModel = setupViewModel,
                            onPairSuccess = { pairUrl, _ ->
                                currentPairUrl = pairUrl
                            },
                            modifier = Modifier.padding(innerPadding)
                        )
                    } else {
                        val targetUrl = currentPairUrl ?: baseUrl ?: ""
                        val effectiveBaseUrl = baseUrl ?: targetUrl
                        WebViewScreen(
                            initialUrl = targetUrl,
                            baseUrl = effectiveBaseUrl,
                            onChangeServer = {
                                currentPairUrl = null
                                webViewViewModel.changeServer()
                            },
                            onLogout = {
                                currentPairUrl = null
                                webViewViewModel.logout()
                            },
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                }
            }
        }
    }
}
