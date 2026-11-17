package dev.master.lumio.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.master.lumio.data.settings.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class SettingsState(val settings: AppSettings = AppSettings(), val loading: Boolean = true,
    val initialized: Boolean = false, val readError: Boolean = false, val saving: Boolean = false, val writeError: Boolean = false)
class SettingsViewModel(private val repository: SettingsRepository) : ViewModel() {
    private val mutableState = MutableStateFlow(SettingsState())
    val state = mutableState.asStateFlow()
    private var readJob: Job? = null
    init { retry() }
    fun retry() {
        readJob?.cancel()
        mutableState.update { it.copy(loading = true, readError = false) }
        readJob = viewModelScope.launch {
            try {
                repository.settings.collect { settings ->
                    mutableState.update { it.copy(settings = settings, loading = false, initialized = true, readError = false) }
                }
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { mutableState.update { it.copy(loading = false, initialized = true, readError = true) } }
        }
    }
    fun appearance(value: Appearance) = save { repository.setAppearance(value) }
    fun sound(value: Boolean) = save { repository.setSound(value) }
    private fun save(action: suspend () -> Unit) {
        if (state.value.loading || state.value.readError || state.value.saving) return
        mutableState.update { it.copy(saving = true, writeError = false) }
        viewModelScope.launch {
            try { action() }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { mutableState.update { it.copy(writeError = true) } }
            finally { mutableState.update { it.copy(saving = false) } }
        }
    }
}
