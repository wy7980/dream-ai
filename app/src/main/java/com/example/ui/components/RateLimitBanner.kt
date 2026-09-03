package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.RateLimitState
import com.example.ui.theme.AgnesAmber
import com.example.ui.theme.AgnesCyan
import com.example.ui.theme.AgnesEmerald
import com.example.ui.theme.AgnesViolet
import com.example.ui.theme.CyberCardBg
import com.example.ui.theme.CyberCardBorder

@Composable
fun RateLimitBanner(
    rateLimitState: RateLimitState,
    modifier: Modifier = Modifier
) {
    val isCooling = rateLimitState.isCoolingDown
    val remaining = rateLimitState.remainingSeconds
    val total = rateLimitState.totalCooldownSeconds.coerceAtLeast(1)
    val progress = (total - remaining).toFloat() / total.toFloat()

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val borderColor by animateColorAsState(
        targetValue = if (isCooling) AgnesAmber else AgnesEmerald.copy(alpha = 0.4f),
        label = "borderColor"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, borderColor.copy(alpha = if (isCooling) pulseAlpha else 0.4f), RoundedCornerShape(12.dp))
            .testTag("rate_limit_banner"),
        color = CyberCardBg,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .background(
                                if (isCooling) AgnesAmber.copy(alpha = 0.15f) else AgnesEmerald.copy(alpha = 0.15f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isCooling) {
                            CircularProgressIndicator(
                                progress = { progress },
                                modifier = Modifier.size(20.dp),
                                color = AgnesAmber,
                                trackColor = AgnesAmber.copy(alpha = 0.2f),
                                strokeWidth = 2.dp
                            )
                            Text(
                                text = "${remaining}s",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = AgnesAmber
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Ready",
                                tint = AgnesEmerald,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (isCooling) "限速冷却中 (${remaining}s)" else "Dream AI 调度就绪",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isCooling) AgnesAmber else AgnesEmerald
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFF1E293B), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "1次/分",
                                    fontSize = 9.sp,
                                    color = AgnesCyan,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Text(
                            text = if (isCooling) "下一次 API 调用冷却归零后自动发起" else "支持图生图与多段分镜自动化拼接",
                            fontSize = 10.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                if (rateLimitState.pendingQueueCount > 0) {
                    Box(
                        modifier = Modifier
                            .background(
                                Brush.horizontalGradient(listOf(AgnesViolet, AgnesCyan)),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.HourglassTop,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "排队: ${rateLimitState.pendingQueueCount}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = rateLimitState.currentExecutingTask != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column(modifier = Modifier.padding(top = 6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = "Processing",
                            tint = AgnesCyan,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = rateLimitState.currentExecutingTask ?: "",
                            fontSize = 11.sp,
                            color = AgnesCyan,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1
                        )
                    }
                    if (isCooling) {
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .clip(RoundedCornerShape(1.5.dp)),
                            color = AgnesAmber,
                            trackColor = Color(0xFF1E293B)
                        )
                    }
                }
            }
        }
    }
}
