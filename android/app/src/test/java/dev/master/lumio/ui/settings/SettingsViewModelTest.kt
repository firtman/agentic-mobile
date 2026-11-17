package dev.master.lumio.ui.settings

import dev.master.lumio.data.settings.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun setup() = Dispatchers.setMain(dispatcher)
    @After fun teardown() = Dispatchers.resetMain()
    private class Repository : SettingsRepository {
        val data = MutableStateFlow(AppSettings())
        var failRead = false
        var failWrite = false
        var writes = 0
        var gate: CompletableDeferred<Unit>? = null
        override val settings: Flow<AppSettings> get() = flow {
            if (failRead) throw IOException()
            emitAll(data)
        }
        override suspend fun setAppearance(value: Appearance) {
            writes++; gate?.await()
            if (failWrite) throw IOException()
            data.value = data.value.copy(appearance = value)
        }
        override suspend fun setSound(value: Boolean) {
            if (failWrite) throw IOException()
            data.value = data.value.copy(sound = value)
        }
    }
    @Test fun loadsAndAppliesBothPreferences() = runTest(dispatcher) {
        val repo = Repository(); repo.data.value = AppSettings(Appearance.DARK, false)
        val vm = SettingsViewModel(repo); runCurrent()
        assertEquals(repo.data.value, vm.state.value.settings)
        vm.appearance(Appearance.LIGHT); runCurrent()
        vm.sound(true); runCurrent()
        assertEquals(AppSettings(Appearance.LIGHT, true), vm.state.value.settings)
    }
    @Test fun readFailureDisablesWritesAndRetryRecovers() = runTest(dispatcher) {
        val repo = Repository(); repo.failRead = true
        val vm = SettingsViewModel(repo); runCurrent()
        assertTrue(vm.state.value.readError)
        vm.appearance(Appearance.DARK); runCurrent(); assertEquals(0, repo.writes)
        repo.failRead = false; vm.retry(); runCurrent()
        assertFalse(vm.state.value.readError)
        vm.appearance(Appearance.DARK); runCurrent()
        assertEquals(Appearance.DARK, vm.state.value.settings.appearance)
    }
    @Test fun writeFailureRetainsSavedValueAndCanBeRetried() = runTest(dispatcher) {
        val repo = Repository(); val vm = SettingsViewModel(repo); runCurrent()
        repo.failWrite = true; vm.sound(false); runCurrent()
        assertTrue(vm.state.value.writeError)
        assertTrue(vm.state.value.settings.sound)
        repo.failWrite = false; vm.sound(false); runCurrent()
        assertFalse(vm.state.value.writeError)
        assertFalse(vm.state.value.settings.sound)
    }
    @Test fun overlappingWritesAreBlockedUntilSaveFinishes() = runTest(dispatcher) {
        val repo = Repository(); val vm = SettingsViewModel(repo); runCurrent()
        repo.gate = CompletableDeferred()
        vm.appearance(Appearance.DARK); vm.appearance(Appearance.LIGHT); runCurrent()
        assertEquals(1, repo.writes); assertTrue(vm.state.value.saving)
        repo.gate!!.complete(Unit); runCurrent()
        assertFalse(vm.state.value.saving)
        assertEquals(Appearance.DARK, vm.state.value.settings.appearance)
    }
}
