package dev.master.lumio.data.settings

import androidx.datastore.preferences.core.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsRepositoryTest {
    @get:Rule val folder = TemporaryFolder()
    @Test fun defaultsAndIndependentEditsSurviveStoreRecreation() = runTest {
        val file = folder.root.resolve("settings.preferences_pb")
        var scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        var store = PreferenceDataStoreFactory.create(scope = scope) { file }
        var repository = DataStoreSettingsRepository(store)
        assertEquals(AppSettings(), repository.settings.first())
        repository.setSound(false)
        repository.setAppearance(Appearance.DARK)
        assertEquals(AppSettings(Appearance.DARK, false), repository.settings.first())
        scope.coroutineContext[Job]!!.cancelAndJoin()
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            store = PreferenceDataStoreFactory.create(scope = scope) { file }
            repository = DataStoreSettingsRepository(store)
            assertEquals(AppSettings(Appearance.DARK, false), repository.settings.first())
            repository.setAppearance(Appearance.LIGHT)
            assertFalse(repository.settings.first().sound)
            repository.setAppearance(Appearance.SYSTEM)
            assertEquals(Appearance.SYSTEM, repository.settings.first().appearance)
        } finally { scope.coroutineContext[Job]!!.cancelAndJoin() }
    }
    @Test fun unknownAppearanceFallsBackWithoutLosingSoundChoice() = runTest {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val store = PreferenceDataStoreFactory.create(scope = scope) { folder.root.resolve("unknown.preferences_pb") }
            store.edit { it[stringPreferencesKey("appearance")] = "FUTURE"; it[booleanPreferencesKey("game_sound")] = false }
            assertEquals(AppSettings(Appearance.SYSTEM, false), DataStoreSettingsRepository(store).settings.first())
        } finally { scope.coroutineContext[Job]!!.cancelAndJoin() }
    }
}
