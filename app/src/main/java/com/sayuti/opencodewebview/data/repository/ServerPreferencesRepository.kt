package com.sayuti.opencodewebview.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Extension property to provide a [DataStore] instance for server preferences.
 */
private val Context.serverPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(name = "server_preferences")

/**
 * Repository responsible for managing server configuration preferences (such as the base URL)
 * using Jetpack Preferences DataStore.
 *
 * @property dataStore The underlying [DataStore] instance used to persist preferences.
 */
class ServerPreferencesRepository private constructor(
    private val dataStore: DataStore<Preferences>
) {
    /**
     * Secondary constructor that initializes the repository using the provided [Context]
     * and the default [serverPreferencesDataStore].
     *
     * @param context The application context.
     */
    constructor(context: Context) : this(context.serverPreferencesDataStore)

    companion object {
        /** Preference key for storing the server base URL. */
        private val KEY_BASE_URL = stringPreferencesKey("base_url")

        /**
         * Creates a [ServerPreferencesRepository] instance with a custom [DataStore] for testing purposes.
         *
         * @param dataStore The custom [DataStore] instance to use.
         * @return A new [ServerPreferencesRepository] instance.
         */
        fun createForTest(dataStore: DataStore<Preferences>): ServerPreferencesRepository {
            return ServerPreferencesRepository(dataStore)
        }
    }

    /**
     * A [Flow] that emits the current server base URL whenever it changes,
     * or `null` if no base URL has been configured.
     */
    val baseUrlFlow: Flow<String?> = dataStore.data.map { preferences ->
        preferences[KEY_BASE_URL]
    }

    /**
     * Saves or updates the server base URL in the DataStore.
     *
     * @param url The base URL string to save.
     */
    suspend fun saveBaseUrl(url: String) {
        dataStore.edit { preferences ->
            preferences[KEY_BASE_URL] = url
        }
    }

    /**
     * Removes the stored server base URL from the DataStore.
     */
    suspend fun clearBaseUrl() {
        dataStore.edit { preferences ->
            preferences.remove(KEY_BASE_URL)
        }
    }
}

/**
 * Type alias for [ServerPreferencesRepository] to maintain backward compatibility or concise naming.
 */
typealias DataStoreManager = ServerPreferencesRepository
