package dev.master.lumio

import android.app.Application
import android.content.Context
import coil3.ImageLoader
import coil3.SingletonImageLoader
import dev.master.lumio.data.ApiClient
import dev.master.lumio.data.CatalogRepository
import dev.master.lumio.data.OnlineExploreRepository
import dev.master.lumio.data.OnlineMovieRepository
import dev.master.lumio.data.ratings.LumioDatabase
import dev.master.lumio.data.ratings.RoomRatingsRepository

class AppContainer(context: Context) {
    val api = ApiClient.create(BuildConfig.API_BASE_URL)
    val catalogRepository = CatalogRepository(api)
    val exploreRepository = OnlineExploreRepository(api)
    val movieRepository = OnlineMovieRepository(api)
    // Ratings, undo history and rated-movie snapshots stay on the device.
    val ratingsRepository by lazy { RoomRatingsRepository(LumioDatabase.create(context)) }
}
class LumioApplication : Application(), SingletonImageLoader.Factory {
    // Images may be reused in memory, but are never stored for offline catalog use.
    override fun newImageLoader(context: Context): ImageLoader = ImageLoader.Builder(context)
        .diskCache(null)
        .build()

    val container by lazy { AppContainer(this) }
}
