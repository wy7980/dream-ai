package com.example.ui.components

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import java.io.File

/**
 * Resolves a clip source string into a playable [Uri].
 *
 * Supports remote http(s) URLs, `file://` URIs, raw absolute paths, and `content://` URIs.
 */
private fun toPlayableUri(source: String): Uri {
    val trimmed = source.trim()
    return when {
        trimmed.startsWith("content://") -> Uri.parse(trimmed)
        trimmed.startsWith("file://") -> Uri.fromFile(File(trimmed.removePrefix("file://")))
        trimmed.startsWith("http://") || trimmed.startsWith("https://") -> Uri.parse(trimmed)
        else -> Uri.fromFile(File(trimmed))
    }
}

/**
 * A minimal ExoPlayer-backed video surface for Compose.
 *
 * The player is created once and released on dispose. Changing [videoSource] swaps the media item
 * and re-prepares; [playWhenReady] toggles playback without recreating the player.
 */
@Composable
fun VideoPlayerView(
    videoSource: String?,
    playWhenReady: Boolean,
    modifier: Modifier = Modifier,
    looping: Boolean = true,
    onError: (String) -> Unit = {}
) {
    val context = LocalContext.current

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = false
        }
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                onError(error.message ?: "视频播放失败")
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    // Swap media when the source changes.
    LaunchedEffect(videoSource) {
        val src = videoSource?.takeIf { it.isNotBlank() }
        if (src == null) {
            exoPlayer.clearMediaItems()
        } else {
            exoPlayer.setMediaItem(MediaItem.fromUri(toPlayableUri(src)))
            exoPlayer.repeatMode = if (looping) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
            exoPlayer.prepare()
        }
    }

    // Toggle playback.
    LaunchedEffect(playWhenReady, videoSource) {
        exoPlayer.playWhenReady = playWhenReady
    }

    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = exoPlayer
                useController = false
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                setShutterBackgroundColor(android.graphics.Color.BLACK)
            }
        },
        modifier = modifier,
        update = { it.player = exoPlayer }
    )
}
