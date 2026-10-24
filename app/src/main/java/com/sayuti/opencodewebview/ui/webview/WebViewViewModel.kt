package com.sayuti.opencodewebview.ui.webview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sayuti.opencodewebview.data.repository.ServerPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class WebViewViewModel(
    private val serverPreferencesRepository: ServerPreferencesRepository
) : ViewModel() {

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    val baseUrl: StateFlow<String?> = serverPreferencesRepository.baseUrlFlow
        .onEach { _isLoading.value = false }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = null
        )

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
