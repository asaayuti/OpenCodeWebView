package com.sayuti.opencodewebview.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.serverPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(name = "server_preferences")

class ServerPreferencesRepository private constructor(
    private val dataStore: DataStore<Preferences>
) {
    constructor(context: Context) : this(context.serverPreferencesDataStore)

    companion object {
        private val KEY_BASE_URL = stringPreferencesKey("base_url")

        fun createForTest(dataStore: DataStore<Preferences>): ServerPreferencesRepository {
            return ServerPreferencesRepository(dataStore)
        }
    }

    val baseUrlFlow: Flow<String?> = dataStore.data.map { preferences ->
        preferences[KEY_BASE_URL]
    }

    suspend fun saveBaseUrl(url: String) {
        dataStore.edit { preferences ->
            preferences[KEY_BASE_URL] = url
        }
    }

    suspend fun clearBaseUrl() {
        dataStore.edit { preferences ->
            preferences.remove(KEY_BASE_URL)
        }
    }
}

typealias DataStoreManager = ServerPreferencesRepository
