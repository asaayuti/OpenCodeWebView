package com.sayuti.opencodewebview.ui.webview

import android.webkit.CookieManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sayuti.opencodewebview.data.repository.ServerPreferencesRepository
import kotlinx.coroutines.launch

class WebViewViewModel(
    private val serverPreferencesRepository: ServerPreferencesRepository,
    private val cookieManagerProvider: () -> CookieManager = { CookieManager.getInstance() }
) : ViewModel() {

    fun changeServer(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            serverPreferencesRepository.clearBaseUrl()
            onComplete()
        }
    }

    fun logout(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                val cookieManager = cookieManagerProvider()
                cookieManager.removeAllCookies(null)
                cookieManager.flush()
            } catch (e: Exception) {
                // Ignore exceptions during unit tests or uninitialized webview environments
            }
            serverPreferencesRepository.clearBaseUrl()
            onComplete()
        }
    }

    class Factory(
        private val repository: ServerPreferencesRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return WebViewViewModel(repository) as T
        }
    }
}
