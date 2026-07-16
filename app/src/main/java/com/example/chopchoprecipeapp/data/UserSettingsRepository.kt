package com.example.chopchoprecipeapp.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_settings")

/**
 * Repository class that handles persisting and fetching user settings like
 * dark mode preference, user name, and profile image path.
 * @property context The application context to access DataStore
 * @author Amelie Dzierzawa
 */
class UserSettingsRepository(private val context: Context) {

    companion object {
        private val KEY_DARK_MODE = booleanPreferencesKey("dark_mode")
        private val KEY_USER_NAME = stringPreferencesKey("user_name")
        private val KEY_PROFILE_IMAGE_PATH = stringPreferencesKey("profile_image_path")
    }

    /**
     * Emits the current dark mode setting. Defaults to false if not set.
     */
    val isDarkMode: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_DARK_MODE] ?: false
    }

    /**
     * Emits the current user name. Defaults to an empty string.
     */
    val userName: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_USER_NAME] ?: ""
    }

    /**
     * Emits the stored profile image path, or null if none exists.
     */
    val profileImagePath: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[KEY_PROFILE_IMAGE_PATH]
    }

    /**
     * Saves the dark mode preference.
     * @param enabled Whether dark mode is enabled or not
     */
    suspend fun saveDarkMode(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_DARK_MODE] = enabled
        }
    }

    /**
     * Saves the user's name.
     * @param name The user's name
     */
    suspend fun saveUserName(name: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_USER_NAME] = name
        }
    }

    /**
     * Saves the local file path of the profile image.
     * @param path The local file path of the profile image
     */
    suspend fun saveProfileImagePath(path: String?) {
        context.dataStore.edit { preferences ->
            if (path != null) {
                preferences[KEY_PROFILE_IMAGE_PATH] = path
            } else {
                preferences.remove(KEY_PROFILE_IMAGE_PATH)
            }
        }
    }
}