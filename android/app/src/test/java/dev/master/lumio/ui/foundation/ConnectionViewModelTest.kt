package dev.master.lumio.ui.foundation

import dev.master.lumio.data.Catalog
import dev.master.lumio.data.FakeCatalogApi
import dev.master.lumio.data.Health
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ConnectionViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    @Test fun successAndInvalidHealth() = runTest(dispatcher) {
        val responses = ArrayDeque(listOf(Health("ok", 123), Health("bad", 123), Health("ok", -1)))
        val viewModel = ConnectionViewModel(object : FakeCatalogApi() {
            override suspend fun health() = responses.removeFirst()
            override suspend fun catalog(): Catalog = error("unexpected catalog")
        })
        assertEquals(ConnectionState.Loading, viewModel.state.value)
        runCurrent()
        assertEquals(ConnectionState.Connected(123), viewModel.state.value)
        viewModel.retry()
        runCurrent()
        assertEquals(ConnectionState.Error, viewModel.state.value)
        viewModel.retry()
        runCurrent()
        assertEquals(ConnectionState.Error, viewModel.state.value)
    }

    @Test fun failureThenRetryAndCancellation() = runTest(dispatcher) {
        val pending = CompletableDeferred<Health>()
        var calls = 0
        val viewModel = ConnectionViewModel(object : FakeCatalogApi() {
            override suspend fun health(): Health = when (++calls) {
                1 -> throw IllegalStateException("offline")
                2 -> pending.await()
                else -> Health("ok", 7)
            }
            override suspend fun catalog(): Catalog = error("unexpected catalog")
        })
        runCurrent()
        assertEquals(ConnectionState.Error, viewModel.state.value)
        viewModel.retry()
        runCurrent()
        assertEquals(ConnectionState.Loading, viewModel.state.value)
        viewModel.retry()
        runCurrent()
        assertEquals(ConnectionState.Connected(7), viewModel.state.value)
        pending.complete(Health("ok", 999))
        runCurrent()
        assertEquals(ConnectionState.Connected(7), viewModel.state.value)
    }
}
