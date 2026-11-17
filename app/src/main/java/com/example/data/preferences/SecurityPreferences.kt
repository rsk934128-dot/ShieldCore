package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.securityDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "shieldcore_prefs"
)

data class UserSecurityPrefs(
    val realTimeShieldEnabled: Boolean = true,
    val darkThemeEnabled: Boolean = true,
    val bengaliLanguage: Boolean = false,
    val signatureDbVersion: String = "2026.11.17-v1",
    val lastSelectedFolderLabel: String? = null,
    val lastSelectedFolderUri: String? = null
)

class SecurityPreferences(private val context: Context) {
    private object Keys {
        val REAL_TIME_SHIELD = booleanPreferencesKey("real_time_shield")
        val DARK_THEME = booleanPreferencesKey("dark_theme")
        val BENGALI_LANGUAGE = booleanPreferencesKey("bengali_language")
        val SIGNATURE_DB_VERSION = stringPreferencesKey("signature_db_version")
        val LAST_FOLDER_LABEL = stringPreferencesKey("last_folder_label")
        val LAST_FOLDER_URI = stringPreferencesKey("last_folder_uri")
    }

    val prefsFlow: Flow<UserSecurityPrefs> = context.securityDataStore.data.map { prefs ->
        UserSecurityPrefs(
            realTimeShieldEnabled = prefs[Keys.REAL_TIME_SHIELD] ?: true,
            darkThemeEnabled = prefs[Keys.DARK_THEME] ?: true,
            bengaliLanguage = prefs[Keys.BENGALI_LANGUAGE] ?: false,
            signatureDbVersion = prefs[Keys.SIGNATURE_DB_VERSION] ?: "2026.11.17-v1",
            lastSelectedFolderLabel = prefs[Keys.LAST_FOLDER_LABEL],
            lastSelectedFolderUri = prefs[Keys.LAST_FOLDER_URI]
        )
    }

    suspend fun setRealTimeShield(enabled: Boolean) {
        context.securityDataStore.edit { it[Keys.REAL_TIME_SHIELD] = enabled }
    }

    suspend fun setDarkTheme(enabled: Boolean) {
        context.securityDataStore.edit { it[Keys.DARK_THEME] = enabled }
    }

    suspend fun setBengaliLanguage(enabled: Boolean) {
        context.securityDataStore.edit { it[Keys.BENGALI_LANGUAGE] = enabled }
    }

    suspend fun setSignatureDbVersion(version: String) {
        context.securityDataStore.edit { it[Keys.SIGNATURE_DB_VERSION] = version }
    }

    suspend fun setSelectedFolder(uriString: String, label: String) {
        context.securityDataStore.edit {
            it[Keys.LAST_FOLDER_URI] = uriString
            it[Keys.LAST_FOLDER_LABEL] = label
        }
    }
}
