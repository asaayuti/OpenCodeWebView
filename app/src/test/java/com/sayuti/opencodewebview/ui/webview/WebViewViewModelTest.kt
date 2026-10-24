package com.sayuti.opencodewebview.ui.webview

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
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class WebViewViewModelTest {

    @get:Rule
    val tmpFolder = TemporaryFolder()

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher + Job())

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testChangeServerClearsBaseUrl() = testScope.runTest {
        val testFile = tmpFolder.newFile("test_prefs.preferences_pb")
        val testDataStore: DataStore<Preferences> = PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { testFile }
        )
        val repository = ServerPreferencesRepository.createForTest(testDataStore)
        repository.saveBaseUrl("https://example.com")

        val viewModel = WebViewViewModel(repository)
        viewModel.changeServer()
        advanceUntilIdle()

        assertNull(repository.baseUrlFlow.first())
    }

    @Test
    fun testInitialLoadingStateAndBaseUrlEmission() = testScope.runTest {
        val testFile = tmpFolder.newFile("test_loading_prefs.preferences_pb")
        val testDataStore: DataStore<Preferences> = PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { testFile }
        )
        val repository = ServerPreferencesRepository.createForTest(testDataStore)
        repository.saveBaseUrl("https://test.com")

        val viewModel = WebViewViewModel(repository)
        advanceUntilIdle()

        org.junit.Assert.assertFalse(viewModel.isLoading.value)
        org.junit.Assert.assertEquals("https://test.com", viewModel.baseUrl.value)
    }
}
