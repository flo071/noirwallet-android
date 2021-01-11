package org.noirofficial.presenter.activities.utils

import com.crashlytics.android.Crashlytics
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.Player

class PlayerListener(
        val onPlaybackStarted: () -> Unit,
        val finishPlayback: () -> Unit,
        val errorPlayback: () -> Unit
) : Player.EventListener {

    var readyAchieved = false
    var endingAchieved = false
    override fun onPlayerError(error: ExoPlaybackException?) {
        error?.let {
            errorPlayback()
            Crashlytics.logException(it)
            it.printStackTrace()
        }
    }

    override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
        when (playbackState) {
            Player.STATE_ENDED -> {
                if (!endingAchieved) {
                    endingAchieved = true
                    finishPlayback()
                }
            }
            Player.STATE_READY -> {
                if (!readyAchieved) {
                    readyAchieved = true
                    onPlaybackStarted()
                }
            }
        }
    }
}