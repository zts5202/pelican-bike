package com.zts.pelicanbike.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.highScoreDataStore by preferencesDataStore(name = "pelican_bike_prefs")

class HighScoreStore(private val context: Context) {
    private val key = intPreferencesKey("high_score")

    val highScore: Flow<Int> = context.highScoreDataStore.data.map { prefs ->
        prefs[key] ?: 0
    }

    suspend fun updateIfHigher(score: Int): Int {
        var result = 0
        context.highScoreDataStore.edit { prefs ->
            val current = prefs[key] ?: 0
            val next = maxOf(current, score)
            prefs[key] = next
            result = next
        }
        return result
    }
}
