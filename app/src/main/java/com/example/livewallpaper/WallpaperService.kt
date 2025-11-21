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

        private var isVideoPlaying = false
        private val drawRunnable = object : Runnable {
            override fun run() {
                if (!isUnlocked) {
                    // Wallpaper not visible, do nothing
                } else if (!videoPlayed && !isVideoPlaying) {
                    // Show pre image before video
                    drawImage(preImageRes)
                } else if (videoPlayed && !isVideoPlaying) {
                    // Show post image after video
                    drawImage(postImageRes)
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
        private fun drawImage(resId: Int) {
            val canvas: Canvas? = surfaceHolder.lockCanvas()
            if (canvas != null) {
                try {
                    val bitmap = BitmapFactory.decodeResource(context.resources, resId)
                    if (bitmap != null) {
                        drawBitmapFillCrop(canvas, bitmap)
                    } else {
                        Log.e("LiveWallpaper", "Failed to decode resource: $resId")
                    }
                } catch (e: Exception) {
                    Log.e("LiveWallpaper", "Exception in drawImage()", e)
                } finally {
                    surfaceHolder.unlockCanvasAndPost(canvas)
                }
            }
        }

        // Center crop scaling for bitmap drawing
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
                    mediaPlayer?.setOnErrorListener { _, what, extra ->
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

        // Removed setWallpaperOnce, not needed for live wallpaper

        // Play video on any visibility (unlock or home)
        override fun onVisibilityChanged(visible: Boolean) {
            isUnlocked = visible
            if (visible) {
                // Reset state so video plays on every unlock
                videoPlayed = false
                isVideoPlaying = false
                handler.post(drawRunnable)
                playVideo()
            } else {
                // Optionally stop video if wallpaper is hidden
                if (isVideoPlaying) {
                    mediaPlayer?.stop()
                    mediaPlayer?.release()
                    mediaPlayer = null
                    isVideoPlaying = false
                }
            }
        }
    }
}
