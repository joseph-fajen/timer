package com.igygtimer.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "settings"
)

data class UserSettings(
    val lastRatio: Float = 1.0f,
    val lastRounds: Int = 10
)

class SettingsRepository(private val context: Context) {

    private object PreferenceKeys {
        val LAST_RATIO = floatPreferencesKey("last_ratio")
        val LAST_ROUNDS = intPreferencesKey("last_rounds")
    }

    val settings: Flow<UserSettings> = context.settingsDataStore.data.map { prefs ->
        UserSettings(
            lastRatio = prefs[PreferenceKeys.LAST_RATIO] ?: 1.0f,
            lastRounds = prefs[PreferenceKeys.LAST_ROUNDS] ?: 10
        )
    }

    suspend fun saveLastWorkout(ratio: Float, rounds: Int) {
        context.settingsDataStore.edit { prefs ->
            prefs[PreferenceKeys.LAST_RATIO] = ratio
            prefs[PreferenceKeys.LAST_ROUNDS] = rounds
        }
    }
}
