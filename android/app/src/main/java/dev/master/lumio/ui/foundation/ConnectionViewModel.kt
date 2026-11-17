package dev.master.lumio.ui.foundation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.master.lumio.data.CatalogApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ConnectionState {
    data object Loading : ConnectionState
    data class Connected(val count: Int) : ConnectionState
    data object Error : ConnectionState
}
class ConnectionViewModel(private val api: CatalogApi) : ViewModel() {
    private val mutableState = MutableStateFlow<ConnectionState>(ConnectionState.Loading)
    val state = mutableState.asStateFlow()
    private var request: Job? = null
    init { retry() }
    fun retry() {
        request?.cancel()
        request = viewModelScope.launch {
            mutableState.value = ConnectionState.Loading
            try {
                val health = api.health()
                check(health.status == "ok" && health.movies >= 0)
                mutableState.value = ConnectionState.Connected(health.movies)
            } catch (error: CancellationException) { throw error
            } catch (_: Exception) { mutableState.value = ConnectionState.Error }
        }
    }
}
