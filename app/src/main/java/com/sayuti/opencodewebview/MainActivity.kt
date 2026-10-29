package com.sayuti.opencodewebview

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
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
                val webViewViewModel: WebViewViewModel = viewModel(
                    factory = WebViewViewModel.Factory(repository)
                )
                val baseUrl by webViewViewModel.baseUrl.collectAsStateWithLifecycle()
                val isLoading by webViewViewModel.isLoading.collectAsStateWithLifecycle()
                var currentPairUrl by rememberSaveable { mutableStateOf<String?>(null) }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    when {
                        isLoading -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding)
                            )
                        }
                        baseUrl.isNullOrBlank() && currentPairUrl == null -> {
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
                        }
                        else -> {
                            val targetUrl = currentPairUrl ?: baseUrl ?: ""
                            val effectiveBaseUrl = baseUrl ?: targetUrl
                            WebViewScreen(
                                initialUrl = targetUrl,
                                baseUrl = effectiveBaseUrl,
                                onChangeServer = {
                                    currentPairUrl = null
                                    webViewViewModel.changeServer()
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }
    }
}
