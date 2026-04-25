package com.awper.lightscore.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "ls_settings")

class SettingsStore(context: Context) {
    val appContext: Context = context.applicationContext
    private val ds = appContext.dataStore
    private val AUTO_CELL = booleanPreferencesKey("auto_cell")
    private val BG = booleanPreferencesKey("bg")
    private val FAV_BG = booleanPreferencesKey("fav_bg")
    private val AUTO_FAV_CELL = booleanPreferencesKey("auto_fav_cell")
    private val KEEP_AWAKE = booleanPreferencesKey("keep_awake")
    private val LOW_DATA = booleanPreferencesKey("low_data")
    private val PIN_FAVORITES = booleanPreferencesKey("pin_favorites")
    private val FAVS = stringPreferencesKey("favs")

    val autoUpdateCellular: Flow<Boolean> = ds.data.map { it[AUTO_CELL] ?: true }
    val updateBackground: Flow<Boolean> = ds.data.map { it[BG] ?: true }
    val updateFavoritesBackground: Flow<Boolean> = ds.data.map { it[FAV_BG] ?: false }
    val autoUpdateFavoritesCellular: Flow<Boolean> = ds.data.map { it[AUTO_FAV_CELL] ?: true }
    val keepScreenAwake: Flow<Boolean> = ds.data.map { it[KEEP_AWAKE] ?: false }
    val lowDataMode: Flow<Boolean> = ds.data.map { it[LOW_DATA] ?: false }
    val pinFavorites: Flow<Boolean> = ds.data.map { it[PIN_FAVORITES] ?: true }
    val favorites: Flow<List<String>> = ds.data.map { it[FAVS]?.split(",")?.filter { s -> s.isNotBlank() } ?: emptyList() }

    suspend fun setAutoUpdateCellular(v: Boolean) {
        ds.updateData { prefs ->
            val m = prefs.toMutablePreferences()
            m[AUTO_CELL] = v
            m
        }
    }
    suspend fun setUpdateBackground(v: Boolean) {
        ds.updateData { prefs ->
            val m = prefs.toMutablePreferences()
            m[BG] = v
            m
        }
    }
    suspend fun setUpdateFavoritesBackground(v: Boolean) {
        ds.updateData { prefs ->
            val m = prefs.toMutablePreferences()
            m[FAV_BG] = v
            m
        }
    }
    suspend fun setAutoUpdateFavoritesCellular(v: Boolean) {
        ds.updateData { prefs ->
            val m = prefs.toMutablePreferences()
            m[AUTO_FAV_CELL] = v
            m
        }
    }
    suspend fun setKeepScreenAwake(v: Boolean) {
        ds.updateData { prefs ->
            val m = prefs.toMutablePreferences()
            m[KEEP_AWAKE] = v
            m
        }
    }
    suspend fun setLowDataMode(v: Boolean) {
        ds.updateData { prefs ->
            val m = prefs.toMutablePreferences()
            m[LOW_DATA] = v
            m
        }
    }
    suspend fun setPinFavorites(v: Boolean) {
        ds.updateData { prefs ->
            val m = prefs.toMutablePreferences()
            m[PIN_FAVORITES] = v
            m
        }
    }
    suspend fun setFavorites(v: List<String>) {
        ds.updateData { prefs ->
            val m = prefs.toMutablePreferences()
            m[FAVS] = v.joinToString(",")
            m
        }
    }
}
