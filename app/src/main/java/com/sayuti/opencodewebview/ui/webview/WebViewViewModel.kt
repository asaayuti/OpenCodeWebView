package com.sayuti.opencodewebview.ui.webview

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sayuti.opencodewebview.data.repository.ServerPreferencesRepository
import com.sayuti.opencodewebview.util.OpenCodeUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class WebViewViewModel : ViewModel {

    private val serverPreferencesRepository: ServerPreferencesRepository
    private var connectivityManager: ConnectivityManager? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isConnected = MutableStateFlow(true)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    val baseUrl: StateFlow<String?>

    constructor(repository: ServerPreferencesRepository, initialConnected: Boolean = true) {
        this.serverPreferencesRepository = repository
        _isConnected.value = initialConnected
        baseUrl = serverPreferencesRepository.baseUrlFlow
            .onEach { _isLoading.value = false }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = null
            )
    }

    constructor(application: Application, repository: ServerPreferencesRepository) {
        this.serverPreferencesRepository = repository
        _isConnected.value = OpenCodeUtils.isNetworkAvailable(application)

        baseUrl = serverPreferencesRepository.baseUrlFlow
            .onEach { _isLoading.value = false }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = null
            )

        connectivityManager = application.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                _isConnected.value = true
            }

            override fun onLost(network: Network) {
                _isConnected.value = false
            }

            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                val hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                _isConnected.value = hasInternet
            }
        }

        try {
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            connectivityManager?.registerNetworkCallback(request, networkCallback!!)
        } catch (_: Exception) {
        }
    }

    fun changeServer(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            serverPreferencesRepository.clearBaseUrl()
            onComplete()
        }
    }

    override fun onCleared() {
        try {
            if (connectivityManager != null && networkCallback != null) {
                connectivityManager?.unregisterNetworkCallback(networkCallback!!)
            }
        } catch (_: Exception) {
        }
    }

    class Factory : ViewModelProvider.Factory {
        private var application: Application? = null
        private var repository: ServerPreferencesRepository

        constructor(repository: ServerPreferencesRepository) {
            this.repository = repository
        }

        constructor(application: Application, repository: ServerPreferencesRepository) {
            this.application = application
            this.repository = repository
        }

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return if (application != null) {
                WebViewViewModel(application!!, repository) as T
            } else {
                WebViewViewModel(repository) as T
            }
        }
    }
}
