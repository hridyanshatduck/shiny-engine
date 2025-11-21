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

        private var wallpaperSet = false
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
                try {
                    when {
                        !isUnlocked -> {
                            val bitmap = BitmapFactory.decodeResource(context.resources, preImageRes)
                            if (bitmap != null) {
                                canvas.drawBitmap(bitmap, 0f, 0f, null)
                            } else {
                                Log.e("LiveWallpaper", "Failed to decode preImageRes")
                            }
                        }
                        !videoPlayed -> {
                            playVideo()
                            // Optionally draw a frame or a placeholder while video is playing
                        }
                        videoPlayed && !wallpaperSet -> {
                            val bitmap = BitmapFactory.decodeResource(context.resources, postImageRes)
                            if (bitmap != null) {
                                canvas.drawBitmap(bitmap, 0f, 0f, null)
                                setWallpaperOnce(bitmap)
                            } else {
                                Log.e("LiveWallpaper", "Failed to decode postImageRes")
                            }
                        }
                        else -> {
                            val bitmap = BitmapFactory.decodeResource(context.resources, postImageRes)
                            if (bitmap != null) {
                                canvas.drawBitmap(bitmap, 0f, 0f, null)
                            } else {
                                Log.e("LiveWallpaper", "Failed to decode postImageRes")
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("LiveWallpaper", "Exception in draw()", e)
                } finally {
                    surfaceHolder.unlockCanvasAndPost(canvas)
                }
            }
        }

        private fun playVideo() {
            if (mediaPlayer == null) {
                try {
                    mediaPlayer = MediaPlayer.create(context, videoRes)
                    surface = surfaceHolder.surface
                    mediaPlayer?.setSurface(surface)
                    mediaPlayer?.setOnCompletionListener {
                        videoPlayed = true
                        mediaPlayer?.release()
                        mediaPlayer = null
                    }
                    mediaPlayer?.setOnErrorListener { mp, what, extra ->
                        Log.e("LiveWallpaper", "MediaPlayer error: what=$what, extra=$extra")
                        mediaPlayer?.release()
                        mediaPlayer = null
                        true
                    }
                    mediaPlayer?.start()
                } catch (e: Exception) {
                    Log.e("LiveWallpaper", "Exception in playVideo()", e)
                    mediaPlayer?.release()
                    mediaPlayer = null
                }
            }
        }

        private fun setWallpaperOnce(bitmap: Bitmap) {
            if (!wallpaperSet) {
                try {
                    val wallpaperManager = WallpaperManager.getInstance(context)
                    wallpaperManager.setBitmap(bitmap)
                    wallpaperSet = true
                } catch (e: Exception) {
                    Log.e("LiveWallpaper", "Failed to set wallpaper", e)
                }
            }
        }

        // Simulate unlock event (replace with real logic)
        override fun onVisibilityChanged(visible: Boolean) {
            isUnlocked = visible
        }
    }
}
