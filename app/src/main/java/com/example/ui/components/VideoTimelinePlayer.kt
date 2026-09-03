package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.GenerationProject
import com.example.data.model.SceneClip
import com.example.ui.theme.AgnesCyan
import com.example.ui.theme.AgnesEmerald
import com.example.ui.theme.AgnesViolet
import com.example.ui.theme.CyberCardBg
import com.example.ui.theme.CyberCardBorder
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.delay
import java.io.File

private fun safeVideoModel(uri: String?): Any? {
    if (uri.isNullOrBlank()) return null
    return if (uri.startsWith("/")) File(uri) else uri
}

@Composable
fun VideoTimelinePlayer(
    project: GenerationProject,
    clips: List<SceneClip>,
    onExportVideo: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var isPlaying by remember { mutableStateOf(false) }
    var currentClipIndex by remember { mutableIntStateOf(0) }
    var playbackProgress by remember { mutableFloatStateOf(0f) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
    var isLooping by remember { mutableStateOf(true) }

    val totalDurationSeconds = clips.sumOf { it.durationSeconds }.coerceAtLeast(1)
    val activeClip = clips.getOrNull(currentClipIndex)

    // Playback loop simulation with multi-scene auto progression
    LaunchedEffect(isPlaying, currentClipIndex, playbackSpeed, clips) {
        if (isPlaying && clips.isNotEmpty()) {
            val clipDuration = (activeClip?.durationSeconds ?: 4)
            val stepTime = 100L
            val totalSteps = (clipDuration * 1000L / (stepTime * playbackSpeed)).toLong()
            var step = (playbackProgress * totalSteps).toLong()

            while (isPlaying && step < totalSteps) {
                delay(stepTime)
                step++
                playbackProgress = (step.toFloat() / totalSteps.toFloat()).coerceIn(0f, 1f)
            }

            if (isPlaying) {
                playbackProgress = 0f
                if (currentClipIndex < clips.size - 1) {
                    currentClipIndex++
                } else if (isLooping) {
                    currentClipIndex = 0
                } else {
                    isPlaying = false
                }
            }
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, AgnesViolet.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
            .testTag("video_timeline_player"),
        color = CyberCardBg,
        tonalElevation = 3.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            // Header: Title & Project Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Movie,
                        contentDescription = null,
                        tint = AgnesCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "拼接完整长视频 (${totalDurationSeconds}s)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Box(
                    modifier = Modifier
                        .background(AgnesEmerald.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "${clips.size}段分镜已合成",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AgnesEmerald
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Video Player Screen Viewport
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.Black)
                    .border(1.dp, CyberCardBorder, RoundedCornerShape(10.dp))
                    .clickable { isPlaying = !isPlaying },
                contentAlignment = Alignment.Center
            ) {
                if (activeClip?.videoUrl != null || activeClip?.previewThumbnailUrl != null) {
                    val camLower = activeClip.cameraMovement.lowercase()
                    val motionScale = if (isPlaying) {
                        if (camLower.contains("zoom") || camLower.contains("推") || camLower.contains("close")) {
                            1.0f + (playbackProgress * 0.18f)
                        } else if (camLower.contains("crane") || camLower.contains("远") || camLower.contains("俯瞰")) {
                            1.18f - (playbackProgress * 0.14f)
                        } else {
                            1.05f + (playbackProgress * 0.06f)
                        }
                    } else 1.05f

                    val motionPanX = if (isPlaying && (camLower.contains("track") || camLower.contains("pan") || camLower.contains("跟"))) {
                        (playbackProgress - 0.5f) * 45f
                    } else 0f

                    AsyncImage(
                        model = safeVideoModel(activeClip.videoUrl ?: activeClip.previewThumbnailUrl),
                        contentDescription = activeClip.sceneTitle,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = motionScale
                                scaleY = motionScale
                                translationX = motionPanX
                            },
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color(0xFF1E1B4B), Color(0xFF0F172A))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Videocam,
                                contentDescription = null,
                                tint = AgnesViolet,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "正在缓冲分镜片段...",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                // Cinematic vignette overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Black.copy(alpha = 0.4f),
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.75f)
                                )
                            )
                        )
                )

                // Top Tag: Active Scene Info
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "分镜 0${(activeClip?.sceneNumber ?: (currentClipIndex + 1))} • ${activeClip?.sceneTitle ?: "场景"}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = AgnesCyan
                    )
                }

                // Center Play/Pause button overlay if paused
                if (!isPlaying) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(AgnesViolet.copy(alpha = 0.85f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play",
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                // Bottom Narration Subtitles
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (!activeClip?.narration.isNullOrBlank()) {
                        Box(
                            modifier = Modifier
                                .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = activeClip?.narration ?: "",
                                fontSize = 11.sp,
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Progress bar inside video
                    LinearProgressIndicator(
                        progress = { playbackProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.5.dp)
                            .clip(RoundedCornerShape(1.dp)),
                        color = AgnesCyan,
                        trackColor = Color.White.copy(alpha = 0.2f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Multi-segment timeline strip
            Text(
                text = "分镜序列时间轴 (点击可跳至指定片段):",
                fontSize = 11.sp,
                color = Color(0xFF94A3B8),
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(4.dp))

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                itemsIndexed(clips) { index, clip ->
                    val isSelected = currentClipIndex == index
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) AgnesViolet.copy(alpha = 0.25f) else Color(0xFF161E31))
                            .border(
                                1.dp,
                                if (isSelected) AgnesCyan else Color(0xFF1E2A3E),
                                RoundedCornerShape(8.dp)
                            )
                            .clickable {
                                currentClipIndex = index
                                playbackProgress = 0f
                                isPlaying = true
                            }
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(if (isSelected) AgnesCyan else Color.Gray, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "片段 ${clip.sceneNumber}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) AgnesCyan else Color.White
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "${clip.durationSeconds}s",
                                    fontSize = 9.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                            Text(
                                text = clip.sceneTitle,
                                fontSize = 9.sp,
                                color = Color.White.copy(alpha = 0.8f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.width(80.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Player Action Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { isPlaying = !isPlaying },
                        modifier = Modifier
                            .size(34.dp)
                            .background(AgnesViolet, CircleShape)
                            .testTag("play_pause_button")
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    IconButton(
                        onClick = {
                            currentClipIndex = 0
                            playbackProgress = 0f
                            isPlaying = true
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Replay,
                            contentDescription = "Restart",
                            tint = Color(0xFFCBD5E1),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Speed Toggle
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF161E31))
                            .border(1.dp, Color(0xFF1E2A3E), RoundedCornerShape(6.dp))
                            .clickable {
                                playbackSpeed = when (playbackSpeed) {
                                    1.0f -> 1.5f
                                    1.5f -> 2.0f
                                    2.0f -> 0.75f
                                    else -> 1.0f
                                }
                            }
                            .padding(horizontal = 7.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${playbackSpeed}x",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = AgnesCyan
                        )
                    }
                }

                // Action Buttons: Export / Save
                Row {
                    Surface(
                        onClick = onExportVideo,
                        shape = RoundedCornerShape(8.dp),
                        color = AgnesCyan.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AgnesCyan.copy(alpha = 0.4f)),
                        modifier = Modifier.testTag("export_video_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = "Export",
                                tint = AgnesCyan,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "导出长视频",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = AgnesCyan
                            )
                        }
                    }
                }
            }
        }
    }
}
