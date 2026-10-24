package com.sayuti.opencodewebview.ui.webview

import android.webkit.CookieManager
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
import org.junit.Assert.assertTrue
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
    fun testLogoutClearsCookiesAndBaseUrl() = testScope.runTest {
        val testFile = tmpFolder.newFile("test_prefs_logout.preferences_pb")
        val testDataStore: DataStore<Preferences> = PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { testFile }
        )
        val repository = ServerPreferencesRepository.createForTest(testDataStore)
        repository.saveBaseUrl("https://example.com")

        var removeCookiesCalled = false
        var flushCalled = false

        @Suppress("DEPRECATION")
        val fakeCookieManager = object : CookieManager() {
            override fun setAcceptCookie(accept: Boolean) {}
            override fun acceptCookie(): Boolean = true
            override fun setAcceptThirdPartyCookies(webview: android.webkit.WebView, accept: Boolean) {}
            override fun acceptThirdPartyCookies(webview: android.webkit.WebView): Boolean = true
            override fun setCookie(url: String, value: String) {}
            override fun setCookie(url: String, value: String, callback: android.webkit.ValueCallback<Boolean>?) {}
            override fun getCookie(url: String): String? = null
            @Suppress("DEPRECATION")
            override fun removeSessionCookie() {}
            override fun removeSessionCookies(callback: android.webkit.ValueCallback<Boolean>?) {}
            @Suppress("DEPRECATION")
            override fun removeAllCookie() {}
            override fun removeAllCookies(callback: android.webkit.ValueCallback<Boolean>?) {
                removeCookiesCalled = true
                callback?.onReceiveValue(true)
            }
            override fun hasCookies(): Boolean = false
            override fun removeExpiredCookie() {}
            override fun flush() {
                flushCalled = true
            }
        }

        val viewModel = WebViewViewModel(repository) { fakeCookieManager }

        viewModel.logout()
        advanceUntilIdle()

        assertTrue(removeCookiesCalled)
        assertTrue(flushCalled)
        assertNull(repository.baseUrlFlow.first())
    }
}
