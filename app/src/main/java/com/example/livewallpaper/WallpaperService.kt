package com.example.livewallpaper

import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Movie
import android.media.MediaPlayer
import android.os.Handler
import android.content.Context
import android.app.WallpaperManager
import android.graphics.Bitmap
import android.util.Log
import android.view.Surface
import android.os.Build
import androidx.annotation.RequiresApi

class WallpaperService : WallpaperService() {
    override fun onCreateEngine(): Engine {
        return LiveWallpaperEngine(this)
    }

    inner class LiveWallpaperEngine(private val context: Context) : Engine() {
        private var handler: Handler = Handler()
        private var isUnlocked = false
        private var videoPlayed = false
        private var mediaPlayer: MediaPlayer? = null
        private var surface: Surface? = null

        private val preImageRes = R.drawable.per
        private val postImageRes = R.drawable.post
        private val videoRes = R.raw.vid1

        private val drawRunnable = object : Runnable {
            override fun run() {
                draw()
                handler.postDelayed(this, 1000 / 30)
            }
        }

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            handler.post(drawRunnable)
        }

        override fun onDestroy() {
            handler.removeCallbacks(drawRunnable)
            mediaPlayer?.release()
            super.onDestroy()
        }

        private fun draw() {
            val canvas: Canvas? = surfaceHolder.lockCanvas()
            if (canvas != null) {
                if (!isUnlocked) {
                    val bitmap = BitmapFactory.decodeResource(context.resources, preImageRes)
                    canvas.drawBitmap(bitmap, 0f, 0f, null)
                } else if (!videoPlayed) {
                    playVideo()
                } else {
                    val bitmap = BitmapFactory.decodeResource(context.resources, postImageRes)
                    canvas.drawBitmap(bitmap, 0f, 0f, null)
                    setWallpaper(bitmap)
                }
                surfaceHolder.unlockCanvasAndPost(canvas)
            }
        }

        private fun playVideo() {
            if (mediaPlayer == null) {
                mediaPlayer = MediaPlayer.create(context, videoRes)
                surface = surfaceHolder.surface
                mediaPlayer?.setSurface(surface)
                mediaPlayer?.setOnCompletionListener {
                    videoPlayed = true
                }
                mediaPlayer?.start()
            }
        }

        private fun setWallpaper(bitmap: Bitmap) {
            try {
                val wallpaperManager = WallpaperManager.getInstance(context)
                wallpaperManager.setBitmap(bitmap)
            } catch (e: Exception) {
                Log.e("LiveWallpaper", "Failed to set wallpaper", e)
            }
        }

        // Simulate unlock event (replace with real logic)
        override fun onVisibilityChanged(visible: Boolean) {
            isUnlocked = visible
        }
    }
}
