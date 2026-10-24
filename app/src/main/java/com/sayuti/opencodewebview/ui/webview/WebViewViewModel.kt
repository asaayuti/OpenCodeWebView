package com.sayuti.opencodewebview.ui.webview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sayuti.opencodewebview.data.repository.ServerPreferencesRepository
import kotlinx.coroutines.launch

class WebViewViewModel(
    private val serverPreferencesRepository: ServerPreferencesRepository
) : ViewModel() {

    fun changeServer(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
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
