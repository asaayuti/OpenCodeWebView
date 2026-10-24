package com.sayuti.opencodewebview.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ServerPreferencesRepositoryTest {

    @get:Rule
    val tmpFolder = TemporaryFolder()

    @Test
    fun testSaveAndClearBaseUrl() = runTest {
        val testFile = tmpFolder.newFile("test_server_preferences.preferences_pb")
        val testDataStore: DataStore<Preferences> = PreferenceDataStoreFactory.create(
            produceFile = { testFile }
        )

        val repository = ServerPreferencesRepository.createForTest(testDataStore)

        // Initial state should be null
        assertNull(repository.baseUrlFlow.first())

        // Save base URL
        val testUrl = "https://192.168.1.100:8080"
        repository.saveBaseUrl(testUrl)
        assertEquals(testUrl, repository.baseUrlFlow.first())

        // Clear base URL
        repository.clearBaseUrl()
        assertNull(repository.baseUrlFlow.first())
    }
}
