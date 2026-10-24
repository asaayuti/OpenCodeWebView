package com.sayuti.opencodewebview.ui.setup

data class SetupUiState(
    val inputText: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successPairUrl: String? = null
)
