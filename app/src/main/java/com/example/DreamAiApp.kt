package com.example

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.VideoFrameDecoder

/**
 * Application entry point.
 *
 * Registers a Coil [ImageLoader] that can decode the first frame of video files
 * (`coil-video`'s [VideoFrameDecoder]). Without this, `AsyncImage` requests pointing at
 * `.mp4` clips resolve to nothing and the UI falls back to a placeholder.
 */
class DreamAiApp : Application(), ImageLoaderFactory {
    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .components { add(VideoFrameDecoder.Factory()) }
            .crossfade(true)
            .build()
}
