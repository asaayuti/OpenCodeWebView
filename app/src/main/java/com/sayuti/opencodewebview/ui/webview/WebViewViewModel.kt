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

/**
 * ViewModel responsible for managing the state of the WebView screen, including server URL preferences
 * and active network connectivity monitoring.
 */
class WebViewViewModel : ViewModel {

    private val serverPreferencesRepository: ServerPreferencesRepository
    private var connectivityManager: ConnectivityManager? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    private val _isLoading = MutableStateFlow(true)

    /** Emits whether the ViewModel is currently in a loading state. */
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isConnected = MutableStateFlow(true)

    /** Emits whether the device currently has an active network connection. */
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    /** Emits the current configured base URL for the server, or null if not set. */
    val baseUrl: StateFlow<String?>

    /**
     * Constructs a [WebViewViewModel] with the provided [ServerPreferencesRepository] and initial connection state.
     * Useful for testing or custom configurations.
     */
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

    /**
     * Constructs a [WebViewViewModel] with the application context and [ServerPreferencesRepository].
     * Automatically monitors network connectivity changes using [ConnectivityManager].
     */
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

    /**
     * Clears the current server base URL from preferences, allowing the user to select or configure a new server.
     *
     * @param onComplete Callback invoked when the server preference has been successfully cleared.
     */
    fun changeServer(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            serverPreferencesRepository.clearBaseUrl()
            onComplete()
        }
    }

    /**
     * Cleans up network callbacks when the ViewModel is cleared.
     */
    override fun onCleared() {
        try {
            if (connectivityManager != null && networkCallback != null) {
                connectivityManager?.unregisterNetworkCallback(networkCallback!!)
            }
        } catch (_: Exception) {
        }
    }

    /**
     * Factory for creating instances of [WebViewViewModel] with or without an [Application] context.
     */
    class Factory : ViewModelProvider.Factory {
        private var application: Application? = null
        private var repository: ServerPreferencesRepository

        /**
         * Creates a [Factory] with only the [ServerPreferencesRepository].
         */
        constructor(repository: ServerPreferencesRepository) {
            this.repository = repository
        }

        /**
         * Creates a [Factory] with an [Application] context and [ServerPreferencesRepository].
         */
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
