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
        private var isVideoPlaying = false
        private val drawRunnable = object : Runnable {
            override fun run() {
                if (!isUnlocked || (videoPlayed && !wallpaperSet) || (videoPlayed && wallpaperSet)) {
                    draw()
                }
                // Do not draw while video is playing
                if (!isVideoPlaying) {
                    handler.postDelayed(this, 1000 / 30)
                }
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
                                drawBitmapFillCrop(canvas, bitmap)
                            } else {
                                Log.e("LiveWallpaper", "Failed to decode preImageRes")
                            }
                        }
                        videoPlayed && !wallpaperSet -> {
                            val bitmap = BitmapFactory.decodeResource(context.resources, postImageRes)
                            if (bitmap != null) {
                                drawBitmapFillCrop(canvas, bitmap)
                                setWallpaperOnce(bitmap)
                            } else {
                                Log.e("LiveWallpaper", "Failed to decode postImageRes")
                            }
                        }
                        videoPlayed && wallpaperSet -> {
                            val bitmap = BitmapFactory.decodeResource(context.resources, postImageRes)
                            if (bitmap != null) {
                                drawBitmapFillCrop(canvas, bitmap)
                            } else {
                                Log.e("LiveWallpaper", "Failed to decode postImageRes")
                            }
                        }
                        // Do not draw while video is playing
                    }
                } catch (e: Exception) {
                    Log.e("LiveWallpaper", "Exception in draw()", e)
                } finally {
                    surfaceHolder.unlockCanvasAndPost(canvas)
                }
            }
        }

        // Helper to scale and crop bitmap to fill canvas (centerCrop)
        private fun drawBitmapFillCrop(canvas: Canvas, bitmap: Bitmap) {
            val canvasWidth = canvas.width.toFloat()
            val canvasHeight = canvas.height.toFloat()
            val bitmapWidth = bitmap.width.toFloat()
            val bitmapHeight = bitmap.height.toFloat()
            val scale = Math.max(canvasWidth / bitmapWidth, canvasHeight / bitmapHeight)
            val dx = (canvasWidth - bitmapWidth * scale) / 2f
            val dy = (canvasHeight - bitmapHeight * scale) / 2f
            val saveCount = canvas.save()
            canvas.translate(dx, dy)
            canvas.scale(scale, scale)
            canvas.drawBitmap(bitmap, 0f, 0f, null)
            canvas.restoreToCount(saveCount)
        }

        private fun playVideo() {
            if (mediaPlayer == null && !isVideoPlaying) {
                try {
                    isVideoPlaying = true
                    mediaPlayer = MediaPlayer()
                    val afd = context.resources.openRawResourceFd(videoRes)
                    mediaPlayer?.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                    afd.close()
                    surface = surfaceHolder.surface
                    mediaPlayer?.setSurface(surface)
                    mediaPlayer?.isLooping = false
                    mediaPlayer?.setOnCompletionListener {
                        videoPlayed = true
                        isVideoPlaying = false
                        mediaPlayer?.release()
                        mediaPlayer = null
                        handler.post(drawRunnable) // Resume drawing after video
                    }
                    mediaPlayer?.setOnErrorListener { mp, what, extra ->
                        Log.e("LiveWallpaper", "MediaPlayer error: what=$what, extra=$extra")
                        isVideoPlaying = false
                        mediaPlayer?.release()
                        mediaPlayer = null
                        handler.post(drawRunnable)
                        true
                    }
                    mediaPlayer?.prepare()
                    mediaPlayer?.start()
                } catch (e: Exception) {
                    Log.e("LiveWallpaper", "Exception in playVideo()", e)
                    isVideoPlaying = false
                    mediaPlayer?.release()
                    mediaPlayer = null
                    handler.post(drawRunnable)
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

        // Play video on any visibility (unlock or home)
        override fun onVisibilityChanged(visible: Boolean) {
            isUnlocked = visible
            if (visible && !videoPlayed && !isVideoPlaying) {
                playVideo()
            }
        }
    }
}
