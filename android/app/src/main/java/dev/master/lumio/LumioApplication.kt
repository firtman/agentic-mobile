package dev.master.lumio

import android.app.Application
import dev.master.lumio.data.ApiClient
import dev.master.lumio.data.CatalogRepository

class AppContainer {
    val api = ApiClient.create(BuildConfig.API_BASE_URL)
    val catalogRepository = CatalogRepository(api)
}
class LumioApplication : Application() {
    val container by lazy { AppContainer() }
}
