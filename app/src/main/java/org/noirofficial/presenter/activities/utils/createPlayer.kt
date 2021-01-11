package org.noirofficial.presenter.activities.utils

import android.content.Context
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.database.ExoDatabaseProvider
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.upstream.*
import com.google.android.exoplayer2.upstream.cache.*
import java.io.File

fun createPlayer(context: Context?): SimpleExoPlayer {
    val loadController = DefaultLoadControl.Builder()
            .setAllocator(DefaultAllocator(true, 16))
            .setBufferDurationsMs(
                    4000,
                    150000,
                    4000,
                    2000
            ).setTargetBufferBytes(-1)
            .setPrioritizeTimeOverSizeThresholds(true)
            .createDefaultLoadControl()
    return ExoPlayerFactory.newSimpleInstance(
            context,
            DefaultRenderersFactory(context),
            DefaultTrackSelector(),
            loadController
    )
}

object AssetVideos {
    lateinit var cache: LocalCacheDataSourceFactory

    fun init(context: Context) {
        cache = LocalCacheDataSourceFactory(context)
    }
}

class LocalCacheDataSourceFactory(context: Context) : DataSource.Factory {

    private val cache: Cache = SimpleCache(
            File(context.externalCacheDir, "assetVideoCache"),
            LeastRecentlyUsedCacheEvictor(10 * 1024 * 1024),
            ExoDatabaseProvider(context)
    )

    private val defaultDataSourceFactory: DefaultDataSourceFactory
    private val cacheDataSink: CacheDataSink = CacheDataSink(cache, 1000)
    private val fileDataSource: FileDataSource = FileDataSource()

    init {
        val userAgent = "movie-player"
        val bandwidthMeter = DefaultBandwidthMeter()
        defaultDataSourceFactory = DefaultDataSourceFactory(
                context,
                bandwidthMeter,
                DefaultHttpDataSourceFactory(userAgent)
        )
    }

    override fun createDataSource(): DataSource {
        return CacheDataSource(
                cache, defaultDataSourceFactory.createDataSource(),
                fileDataSource, cacheDataSink,
                CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR, null
        )
    }
}