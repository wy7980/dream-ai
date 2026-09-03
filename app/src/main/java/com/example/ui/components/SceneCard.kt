package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import java.io.File
import com.example.data.model.GenerationStatus
import com.example.data.model.SceneClip
import com.example.ui.theme.AgnesAmber
import com.example.ui.theme.AgnesCyan
import com.example.ui.theme.AgnesEmerald
import com.example.ui.theme.AgnesRose
import com.example.ui.theme.AgnesViolet
import com.example.ui.theme.CyberCardBg
import com.example.ui.theme.CyberCardBorder

@Composable
fun SceneCard(
    clip: SceneClip,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(
                1.dp,
                if (clip.status == GenerationStatus.COMPLETED) AgnesEmerald.copy(alpha = 0.5f)
                else if (clip.status == GenerationStatus.GENERATING_CLIPS) AgnesCyan
                else CyberCardBorder,
                RoundedCornerShape(12.dp)
            )
            .testTag("scene_card_${clip.sceneNumber}"),
        color = CyberCardBg,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            // Header Row: Scene Number & Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(AgnesViolet, RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "分镜 0${clip.sceneNumber}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = clip.sceneTitle,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }

                // Status Badge
                when (clip.status) {
                    GenerationStatus.COMPLETED -> {
                        Box(
                            modifier = Modifier
                                .background(AgnesEmerald.copy(alpha = 0.15f), CircleShape)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = AgnesEmerald,
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "就绪",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AgnesEmerald
                                )
                            }
                        }
                    }
                    GenerationStatus.GENERATING_CLIPS -> {
                        Box(
                            modifier = Modifier
                                .background(AgnesCyan.copy(alpha = 0.15f), CircleShape)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(9.dp),
                                    strokeWidth = 1.5.dp,
                                    color = AgnesCyan
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "生成中",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AgnesCyan
                                )
                            }
                        }
                    }
                    GenerationStatus.WAITING_RATE_LIMIT -> {
                        Box(
                            modifier = Modifier
                                .background(AgnesAmber.copy(alpha = 0.15f), CircleShape)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.HourglassBottom,
                                    contentDescription = null,
                                    tint = AgnesAmber,
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "限速排队",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AgnesAmber
                                )
                            }
                        }
                    }
                    GenerationStatus.FAILED -> {
                        Text(
                            text = "生成异常",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = AgnesRose
                        )
                    }
                    else -> {
                        Text(
                            text = "等待调度",
                            fontSize = 9.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Body: Video / Frame Preview & Details
            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Video Frame Thumbnail
                Box(
                    modifier = Modifier
                        .size(width = 96.dp, height = 58.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF161E31)),
                    contentAlignment = Alignment.Center
                ) {
                    if (clip.videoUrl != null || clip.previewThumbnailUrl != null) {
                        val modelPath = clip.videoUrl ?: clip.previewThumbnailUrl
                        val imageModel = if (modelPath != null && modelPath.startsWith("/")) File(modelPath) else modelPath
                        AsyncImage(
                            model = imageModel,
                            contentDescription = clip.sceneTitle,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        Icon(
                            imageVector = Icons.Default.PlayCircleFilled,
                            contentDescription = "Play",
                            tint = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Videocam,
                                contentDescription = null,
                                tint = AgnesViolet.copy(alpha = 0.6f),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "${clip.durationSeconds}s",
                                fontSize = 9.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Scene Prompts & Camera Motion
                Column(modifier = Modifier.weight(1f)) {
                    // Camera Pill
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = AgnesCyan,
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = clip.cameraMovement,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = AgnesCyan,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(3.dp))

                    // Narration Script
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(10.dp).padding(top = 2.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = clip.narration,
                            fontSize = 10.sp,
                            color = Color(0xFFCBD5E1),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 14.sp
                        )
                    }
                }
            }
        }
    }
}
