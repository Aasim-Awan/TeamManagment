package com.teamfund.manager.core.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "teamfund_preferences")

class UserPreferencesRepository(private val context: Context) {

    private val KEY_ACTIVE_TEAM_ID = longPreferencesKey("active_team_id")
    private val KEY_NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
    private val KEY_THEME_MODE = stringPreferencesKey("theme_mode") // "LIGHT", "DARK", "SYSTEM"

    val activeTeamIdFlow: Flow<Long?> = context.dataStore.data.map { prefs ->
        prefs[KEY_ACTIVE_TEAM_ID]
    }

    val notificationsEnabledFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_NOTIFICATIONS_ENABLED] ?: false
    }

    val themeModeFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_THEME_MODE] ?: "LIGHT"
    }

    suspend fun setActiveTeamId(teamId: Long) {
        context.dataStore.edit { prefs ->
            prefs[KEY_ACTIVE_TEAM_ID] = teamId
        }
    }

    suspend fun clearActiveTeamId() {
        context.dataStore.edit { prefs ->
            prefs.remove(KEY_ACTIVE_TEAM_ID)
        }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_NOTIFICATIONS_ENABLED] = enabled
        }
    }

    suspend fun setThemeMode(themeMode: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_THEME_MODE] = themeMode
        }
    }
}
