package org.noirofficial.presenter.activities

import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import org.noirofficial.R
import org.noirofficial.databinding.ActivityAssetImageBinding
import org.noirofficial.databinding.ActivityAssetVideoPlayerBinding
import org.noirofficial.presenter.activities.base.BRActivity
import org.noirofficial.presenter.activities.callbacks.ActivityAssetImageCallback
import org.noirofficial.presenter.activities.utils.createPlayer
import org.noirofficial.presenter.activities.utils.initialize
import org.noirofficial.tools.animation.BRAnimator
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import java.util.concurrent.TimeUnit

class AssetImageActivity : BRActivity() {
    private lateinit var background: ViewGroup

    private var videoBinding: ActivityAssetVideoPlayerBinding? = null

    private val callback = ActivityAssetImageCallback { this.remove() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        when (intent.getStringExtra(MIME)) {
            "image" -> {
                val binding: ActivityAssetImageBinding = DataBindingUtil.setContentView(this, R.layout.activity_asset_image)
                background = binding.background
                binding.callback = callback
                val contentUrl = intent.getStringExtra(CONTENT_URL)
                Glide.with(this).load(contentUrl).diskCacheStrategy(DiskCacheStrategy.ALL).listener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(e: GlideException?, model: Any, target: Target<Drawable>, isFirstResource: Boolean): Boolean {
                        finish()
                        return false
                    }

                    override fun onResourceReady(resource: Drawable, model: Any, target: Target<Drawable>, dataSource: DataSource, isFirstResource: Boolean): Boolean {
                        fadeInBackground()
                        return false
                    }
                }).into(binding.image)
            }
            "video" -> {
                fadeInBackground()
                val contentUrl = intent.getStringExtra(CONTENT_URL)
                videoBinding = DataBindingUtil.setContentView(this, R.layout.activity_asset_video_player)
                background = videoBinding!!.background
                videoBinding!!.callback = callback
                videoBinding!!.progress.visibility = View.VISIBLE
                videoBinding!!.videoView.player = createPlayer(this).initialize(
                        context = this,
                        url = contentUrl.replace("ecom", "com"),
                        onPlaybackStarted = {
                            videoBinding?.progress?.visibility = View.GONE
                        },
                        finishPlayback = {
                            remove()
                        },
                        errorPlayback = {
                            Toast.makeText(this, R.string.error_playing_asset_video, Toast.LENGTH_SHORT).show()
                            remove()
                        }
                )
            }
        }
    }

    private fun fadeInBackground() {
        Completable.fromRunnable {
            val colorFade = BRAnimator.animateBackgroundDim(background, false, null)
            colorFade.startDelay = 350
            colorFade.duration = 500
            colorFade.start()
        }.subscribeOn(AndroidSchedulers.mainThread()).delaySubscription(250, TimeUnit.MILLISECONDS).subscribe()
    }

    override fun onBackPressed() {
        remove()
    }

    override fun finish() {
        super.finish()
        videoBinding?.videoView?.player?.release()
        overridePendingTransition(R.anim.enter_from_left, R.anim.exit_to_left)
    }

    private fun remove() {
        BRAnimator.animateBackgroundDim(background, true) { this.finish() }.start()
    }

    companion object {

        private const val CONTENT_URL = "AssetImageActivity:AssetContentUrl"
        private const val MIME = "AssetImageActivity:Mime"

        fun show(activity: AppCompatActivity, view: View, assetContentUrl: String, mime: String) {
            val intent = Intent(activity, AssetImageActivity::class.java)
            intent.putExtra(CONTENT_URL, assetContentUrl)
            intent.putExtra(MIME, mime)
            activity.startActivity(intent)
        }
    }
}
