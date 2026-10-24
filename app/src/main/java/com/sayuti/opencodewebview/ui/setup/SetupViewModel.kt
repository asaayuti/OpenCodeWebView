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

class SetupViewModel(
    private val serverPreferencesRepository: ServerPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SetupUiState())
    val uiState: StateFlow<SetupUiState> = _uiState.asStateFlow()

    fun onInputTextChanged(text: String) {
        _uiState.update { it.copy(inputText = text, errorMessage = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun onConnectClicked(onSuccess: (pairUrl: String, baseUrl: String) -> Unit) {
        val input = _uiState.value.inputText
        processInput(input, onSuccess)
    }

    fun onQrCodeScanned(rawValue: String?, onSuccess: (pairUrl: String, baseUrl: String) -> Unit) {
        if (rawValue.isNullOrBlank()) {
            _uiState.update { it.copy(errorMessage = "Scanned QR code is empty") }
            return
        }
        processInput(rawValue, onSuccess)
    }

    fun onScanFailed(exception: Exception) {
        val msg = exception.localizedMessage ?: "Failed to scan QR code"
        _uiState.update { it.copy(errorMessage = "QR Scanner error: $msg") }
    }

    fun onScanCanceled() {
        // User canceled scan dialog
    }

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

    class Factory(
        private val repository: ServerPreferencesRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SetupViewModel(repository) as T
        }
    }
}
