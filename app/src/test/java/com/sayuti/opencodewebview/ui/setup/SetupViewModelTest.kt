package com.sayuti.opencodewebview.ui.setup

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.sayuti.opencodewebview.data.repository.ServerPreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class SetupViewModelTest {

    @get:Rule
    val tmpFolder = TemporaryFolder()

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher + Job())
    private lateinit var repository: ServerPreferencesRepository
    private lateinit var viewModel: SetupViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        val testFile = tmpFolder.newFile("test_setup_vm.preferences_pb")
        val testDataStore: DataStore<Preferences> = PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { testFile }
        )
        repository = ServerPreferencesRepository.createForTest(testDataStore)
        viewModel = SetupViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is empty`() {
        val state = viewModel.uiState.value
        assertEquals("", state.inputText)
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
        assertNull(state.successPairUrl)
    }

    @Test
    fun `onInputTextChanged updates input and clears error`() {
        viewModel.onScanFailed(RuntimeException("Scan failed"))
        assertNotNull(viewModel.uiState.value.errorMessage)

        viewModel.onInputTextChanged("https://server.com/auth/connect/123")
        val state = viewModel.uiState.value
        assertEquals("https://server.com/auth/connect/123", state.inputText)
        assertNull(state.errorMessage)
    }

    @Test
    fun `onConnectClicked with valid URL saves base URL and calls onSuccess`() = runTest(testDispatcher) {
        viewModel.onInputTextChanged("https://server.opentunnel.xyz/auth/connect/myCode123")

        var successPairUrl: String? = null
        var successBaseUrl: String? = null

        viewModel.onConnectClicked { pairUrl, baseUrl ->
            successPairUrl = pairUrl
            successBaseUrl = baseUrl
        }

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
        assertEquals("https://server.opentunnel.xyz/auth/connect/myCode123", state.successPairUrl)

        assertEquals("https://server.opentunnel.xyz/auth/connect/myCode123", successPairUrl)
        assertEquals("https://server.opentunnel.xyz", successBaseUrl)

        val savedBaseUrl = repository.baseUrlFlow.first()
        assertEquals("https://server.opentunnel.xyz", savedBaseUrl)
    }

    @Test
    fun `onConnectClicked with invalid URL shows error`() = runTest(testDispatcher) {
        viewModel.onInputTextChanged("invalid string")

        var callbackCalled = false
        viewModel.onConnectClicked { _, _ -> callbackCalled = true }

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertFalse(callbackCalled)
        assertNotNull(state.errorMessage)
        assertEquals("Invalid link or QR code format", state.errorMessage)
    }

    @Test
    fun `onQrCodeScanned with valid QR JSON payload saves base URL and calls onSuccess`() = runTest(testDispatcher) {
        val qrJson = """{"code":"qrCode99","urls":["https://qr.opentunnel.xyz"]}"""

        var successPairUrl: String? = null
        var successBaseUrl: String? = null

        viewModel.onQrCodeScanned(qrJson) { pairUrl, baseUrl ->
            successPairUrl = pairUrl
            successBaseUrl = baseUrl
        }

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
        assertEquals("https://qr.opentunnel.xyz/auth/connect/qrCode99", state.successPairUrl)

        assertEquals("https://qr.opentunnel.xyz/auth/connect/qrCode99", successPairUrl)
        assertEquals("https://qr.opentunnel.xyz", successBaseUrl)

        val savedBaseUrl = repository.baseUrlFlow.first()
        assertEquals("https://qr.opentunnel.xyz", savedBaseUrl)
    }

    @Test
    fun `onScanFailed sets error message`() {
        viewModel.onScanFailed(RuntimeException("Camera permission denied"))

        val state = viewModel.uiState.value
        assertNotNull(state.errorMessage)
        assertTrue(state.errorMessage!!.contains("Camera permission denied"))
    }
}
