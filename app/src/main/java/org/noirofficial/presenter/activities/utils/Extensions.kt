package org.noirofficial.presenter.activities.utils

import android.content.Context
import android.net.Uri
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.ProgressiveMediaSource

/**
 * Initialize a SimpleExoPlayer, provide a UI callback for when playback begins and ends.
 */
fun SimpleExoPlayer.initialize(
        context: Context,
        url: String,
        onPlaybackStarted: () -> Unit,
        finishPlayback: () -> Unit,
        errorPlayback: () -> Unit
): SimpleExoPlayer {
    prepare(
            ProgressiveMediaSource
                    .Factory(AssetVideos.cache)
                    .createMediaSource(Uri.parse(url))
    )
    playWhenReady = true
    addListener(
            PlayerListener(
                    onPlaybackStarted = onPlaybackStarted,
                    finishPlayback = finishPlayback,
                    errorPlayback = errorPlayback
            )
    )
    return this
}