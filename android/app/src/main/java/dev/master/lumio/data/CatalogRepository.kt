package dev.master.lumio.data

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** A lazy, process-memory catalog for Genie and Trivia. Never writes to disk. */
class CatalogRepository(private val api: CatalogApi) {
    private val mutex = Mutex()
    private var catalog: Catalog? = null
    suspend fun catalog(): Catalog = mutex.withLock {
        catalog ?: api.catalog().also { catalog = it }
    }
}
