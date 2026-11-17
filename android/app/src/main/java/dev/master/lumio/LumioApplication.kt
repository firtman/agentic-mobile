package dev.master.lumio

import android.app.Application
import android.content.Context
import coil3.ImageLoader
import coil3.SingletonImageLoader
import dev.master.lumio.data.ApiClient
import dev.master.lumio.data.CatalogRepository
import dev.master.lumio.data.OnlineExploreRepository

class AppContainer {
    val api = ApiClient.create(BuildConfig.API_BASE_URL)
    val catalogRepository = CatalogRepository(api)
    val exploreRepository = OnlineExploreRepository(api)
}
class LumioApplication : Application(), SingletonImageLoader.Factory {
    // Images may be reused in memory, but are never stored for offline catalog use.
    override fun newImageLoader(context: Context): ImageLoader = ImageLoader.Builder(context)
        .diskCache(null)
        .build()

    val container by lazy { AppContainer() }
}
