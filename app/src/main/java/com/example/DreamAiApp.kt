package com.example

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.VideoFrameDecoder
import com.arthenica.ffmpegkit.FFmpegKitConfig
import com.arthenica.ffmpegkit.Level

/**
 * Application entry point.
 *
 * Registers a Coil [ImageLoader] that can decode the first frame of video files
 * (`coil-video`'s [VideoFrameDecoder]). Without this, `AsyncImage` requests pointing at
 * `.mp4` clips resolve to nothing and the UI falls back to a placeholder.
 *
 * Also configures FFmpegKit, which is used to add cross-fade transitions when stitching
 * the per-scene clips into the master video.
 */
class DreamAiApp : Application(), ImageLoaderFactory {
    override fun onCreate() {
        super.onCreate()
        // Keep FFmpeg quiet in release; only surface real errors to logcat.
        FFmpegKitConfig.setLogLevel(Level.AV_LOG_ERROR)
    }

    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .components { add(VideoFrameDecoder.Factory()) }
            .crossfade(true)
            .build()
}
