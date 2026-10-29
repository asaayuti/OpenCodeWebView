package com.sayuti.opencodewebview.ui.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sayuti.opencodewebview.data.repository.ServerPreferencesRepository
import com.sayuti.opencodewebview.util.OpenCodeUtils
import com.sayuti.opencodewebview.util.PairingParseResult
import com.sayuti.opencodewebview.util.PairingParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel responsible for managing the state and business logic of the server setup and pairing screen.
 * Handles text input, QR code scanning results, URL parsing, and persisting server base URLs.
 *
 * @property serverPreferencesRepository Repository for managing saved server preferences and base URLs.
 */
class SetupViewModel(
    private val serverPreferencesRepository: ServerPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SetupUiState())

    /**
     * Observable stream of UI state for the setup screen.
     */
    val uiState: StateFlow<SetupUiState> = _uiState.asStateFlow()

    /**
     * Updates the input text field value in the UI state and clears any existing error messages.
     *
     * @param text The current string entered by the user.
     */
    fun onInputTextChanged(text: String) {
        _uiState.update { it.copy(inputText = text, errorMessage = null) }
    }

    /**
     * Clears any active error message in the UI state.
     */
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    /**
     * Handles the connect button click event by processing the current input text.
     *
     * @param onSuccess Callback invoked with pairUrl and baseUrl upon successful connection parsing and persistence.
     */
    fun onConnectClicked(onSuccess: (pairUrl: String, baseUrl: String) -> Unit) {
        val input = _uiState.value.inputText
        processInput(input, onSuccess)
    }

    /**
     * Handles the result of a successful QR code scan.
     *
     * @param rawValue The raw string scanned from the QR code.
     * @param onSuccess Callback invoked with pairUrl and baseUrl upon successful parsing and persistence.
     */
    fun onQrCodeScanned(rawValue: String?, onSuccess: (pairUrl: String, baseUrl: String) -> Unit) {
        if (rawValue.isNullOrBlank()) {
            _uiState.update { it.copy(errorMessage = "Scanned QR code is empty") }
            return
        }
        processInput(rawValue, onSuccess)
    }

    /**
     * Handles errors encountered during QR code scanning.
     *
     * @param exception The exception thrown by the scanner.
     */
    fun onScanFailed(exception: Exception) {
        val msg = exception.localizedMessage ?: "Failed to scan QR code"
        _uiState.update { it.copy(errorMessage = "QR Scanner error: $msg") }
    }

    /**
     * Handles cancellation of the QR code scan dialog by the user.
     */
    fun onScanCanceled() {
        // User canceled scan dialog
    }

    /**
     * Parses the pairing input string, validates it, persists the server base URL,
     * and triggers the success callback if valid.
     *
     * @param input The raw pairing URL or input string to parse.
     * @param onSuccess Callback invoked with pairUrl and baseUrl upon successful persistence.
     */
    private fun processInput(
        input: String,
        onSuccess: (pairUrl: String, baseUrl: String) -> Unit
    ) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        when (val result = PairingParser.parse(input)) {
            is PairingParseResult.Error -> {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = result.message
                    )
                }
            }
            is PairingParseResult.Success -> {
                val info = result.pairingInfo
                OpenCodeUtils.logDebug(OpenCodeUtils.TAG, "Connecting to pair URL: ${info.pairUrl} (Base: ${info.baseUrl})")

                viewModelScope.launch {
                    try {
                        serverPreferencesRepository.saveBaseUrl(info.baseUrl)
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                successPairUrl = info.pairUrl
                            )
                        }
                        onSuccess(info.pairUrl, info.baseUrl)
                    } catch (e: Exception) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = "Failed to save server URL: ${e.localizedMessage ?: "Error"}"
                            )
                        }
                    }
                }
            }
        }
    }

    /**
     * ViewModelProvider.Factory for instantiating [SetupViewModel] with required dependencies.
     *
     * @property repository The [ServerPreferencesRepository] required by the ViewModel.
     */
    class Factory(
        private val repository: ServerPreferencesRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SetupViewModel(repository) as T
        }
    }
}
