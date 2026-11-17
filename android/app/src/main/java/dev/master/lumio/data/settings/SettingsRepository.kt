package dev.master.lumio.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class Appearance { SYSTEM, LIGHT, DARK }
data class AppSettings(val appearance: Appearance = Appearance.SYSTEM, val sound: Boolean = true)
interface SettingsRepository {
    val settings: Flow<AppSettings>
    suspend fun setAppearance(value: Appearance)
    suspend fun setSound(value: Boolean)
}
private val Context.settingsStore by preferencesDataStore("settings")
class DataStoreSettingsRepository(private val store: DataStore<Preferences>) : SettingsRepository {
    constructor(context: Context) : this(context.applicationContext.settingsStore)
    override val settings = store.data.map { prefs ->
        AppSettings(Appearance.entries.firstOrNull { it.name == prefs[APPEARANCE] } ?: Appearance.SYSTEM,
            prefs[SOUND] ?: true)
    }
    override suspend fun setAppearance(value: Appearance) { store.edit { it[APPEARANCE] = value.name } }
    override suspend fun setSound(value: Boolean) { store.edit { it[SOUND] = value } }
    private companion object {
        val APPEARANCE = stringPreferencesKey("appearance")
        val SOUND = booleanPreferencesKey("game_sound")
    }
}
