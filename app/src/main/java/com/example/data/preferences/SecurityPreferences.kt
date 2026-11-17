package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.securityDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "shieldcore_prefs"
)

data class UserSecurityPrefs(
    val realTimeShieldEnabled: Boolean = true,
    val lowBatterySafetyMode: Boolean = true,
    val autoScanIntervalHours: Int = 12,
    val lastBackgroundScanTime: Long = 0L,
    val lastBackgroundScanStatus: String = "IDLE",
    val lastBackgroundScanSummary: String = "",
    val darkThemeEnabled: Boolean = true,
    val bengaliLanguage: Boolean = false,
    val signatureDbVersion: String = "2026.11.17-v1",
    val lastSelectedFolderLabel: String? = null,
    val lastSelectedFolderUri: String? = null
)

class SecurityPreferences(private val context: Context) {
    private object Keys {
        val REAL_TIME_SHIELD = booleanPreferencesKey("real_time_shield")
        val LOW_BATTERY_SAFETY_MODE = booleanPreferencesKey("low_battery_safety_mode")
        val AUTO_SCAN_INTERVAL_HOURS = intPreferencesKey("auto_scan_interval_hours")
        val LAST_BG_SCAN_TIME = longPreferencesKey("last_bg_scan_time")
        val LAST_BG_SCAN_STATUS = stringPreferencesKey("last_bg_scan_status")
        val LAST_BG_SCAN_SUMMARY = stringPreferencesKey("last_bg_scan_summary")
        val DARK_THEME = booleanPreferencesKey("dark_theme")
        val BENGALI_LANGUAGE = booleanPreferencesKey("bengali_language")
        val SIGNATURE_DB_VERSION = stringPreferencesKey("signature_db_version")
        val LAST_FOLDER_LABEL = stringPreferencesKey("last_folder_label")
        val LAST_FOLDER_URI = stringPreferencesKey("last_folder_uri")
    }

    val prefsFlow: Flow<UserSecurityPrefs> = context.securityDataStore.data.map { prefs ->
        UserSecurityPrefs(
            realTimeShieldEnabled = prefs[Keys.REAL_TIME_SHIELD] ?: true,
            lowBatterySafetyMode = prefs[Keys.LOW_BATTERY_SAFETY_MODE] ?: true,
            autoScanIntervalHours = (prefs[Keys.AUTO_SCAN_INTERVAL_HOURS] ?: 12).coerceIn(6, 24),
            lastBackgroundScanTime = prefs[Keys.LAST_BG_SCAN_TIME] ?: 0L,
            lastBackgroundScanStatus = prefs[Keys.LAST_BG_SCAN_STATUS] ?: "IDLE",
            lastBackgroundScanSummary = prefs[Keys.LAST_BG_SCAN_SUMMARY] ?: "",
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

    suspend fun setLowBatterySafetyMode(enabled: Boolean) {
        context.securityDataStore.edit { it[Keys.LOW_BATTERY_SAFETY_MODE] = enabled }
    }

    suspend fun setAutoScanIntervalHours(hours: Int) {
        context.securityDataStore.edit {
            it[Keys.AUTO_SCAN_INTERVAL_HOURS] = hours.coerceIn(6, 24)
        }
    }

    suspend fun recordBackgroundScanResult(
        timestamp: Long,
        status: String,
        summary: String
    ) {
        context.securityDataStore.edit {
            it[Keys.LAST_BG_SCAN_TIME] = timestamp
            it[Keys.LAST_BG_SCAN_STATUS] = status
            it[Keys.LAST_BG_SCAN_SUMMARY] = summary
        }
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
