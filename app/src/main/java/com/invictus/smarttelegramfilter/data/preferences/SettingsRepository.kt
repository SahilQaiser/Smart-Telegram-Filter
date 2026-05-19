package com.invictus.smarttelegramfilter.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(private val dataStore: DataStore<Preferences>) {

    companion object {
        val QUIET_ENABLED = booleanPreferencesKey("quiet_enabled")
        val QUIET_START   = intPreferencesKey("quiet_start")
        val QUIET_END     = intPreferencesKey("quiet_end")
    }

    val quietEnabled: Flow<Boolean> = dataStore.data.map { it[QUIET_ENABLED] ?: false }
    val quietStart:   Flow<Int>     = dataStore.data.map { it[QUIET_START] ?: 22 }
    val quietEnd:     Flow<Int>     = dataStore.data.map { it[QUIET_END] ?: 8 }

    suspend fun setQuietEnabled(v: Boolean) = dataStore.edit { it[QUIET_ENABLED] = v }
    suspend fun setQuietStart(h: Int)       = dataStore.edit { it[QUIET_START] = h }
    suspend fun setQuietEnd(h: Int)         = dataStore.edit { it[QUIET_END] = h }

    suspend fun isQuietNow(): Boolean {
        val prefs = dataStore.data.first()
        if (!(prefs[QUIET_ENABLED] ?: false)) return false
        val start = prefs[QUIET_START] ?: 22
        val end   = prefs[QUIET_END]   ?: 8
        val hour  = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return if (start <= end) hour in start..end else hour >= start || hour < end
    }
}
