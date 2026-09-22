package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.GenerationProject
import com.example.data.model.ProjectType
import com.example.ui.theme.AgnesCyan
import com.example.ui.theme.AgnesViolet
import com.example.ui.theme.AgnesVioletLight
import com.example.ui.theme.AppCardBg
import com.example.ui.theme.AppCardBorder
import com.example.ui.theme.AppSubtleBg
import com.example.ui.theme.AppTextPrimary
import com.example.ui.theme.AppTextSecondary

/**
 * Minimalist native top bar for the studio screens (image / video).
 *
 * Mirrors the agent screen's header: a single row with a left history button, a centered
 * brand title, and an optional right-side action. It replaces the old "header row inside
 * the scroll content" pattern so history lives in a dedicated drawer instead of being
 * dumped onto the same screen as the workspace.
 */
@Composable
fun StudioTopBar(
    title: String,
    subtitle: String,
    icon: ImageVector,
    gradient: List<Color>,
    onOpenHistory: () -> Unit,
    historyBadgeCount: Int = 0,
    trailing: @Composable (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Top Left: history drawer trigger
        IconButton(
            onClick = onOpenHistory,
            modifier = Modifier
                .size(36.dp)
                .testTag("open_history_button")
        ) {
            Box(contentAlignment = Alignment.TopEnd) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "历史记录",
                    tint = AppTextPrimary,
                    modifier = Modifier.size(22.dp)
                )
                if (historyBadgeCount > 0) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(AgnesCyan, CircleShape)
                    )
                }
            }
        }

        // Center: brand block
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(Brush.linearGradient(gradient), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    fontSize = 10.sp,
                    color = AppTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        trailing?.invoke()
    }
}

/**
 * Left-side sliding history drawer for the studio screens.
 *
 * Only shows the projects that belong to this studio ([filterType]) — the image studio
 * shows image projects, the video studio shows video projects — so the two workspaces are
 * no longer crammed into one screen.
 */
@Composable
fun StudioHistoryDrawer(
    visible: Boolean,
    title: String,
    projects: List<GenerationProject>,
    filterType: ProjectType,
    selectedProjectId: String?,
    onDismiss: () -> Unit,
    onSelectProject: (GenerationProject) -> Unit,
    onNewSession: () -> Unit,
    onDeleteProject: (GenerationProject) -> Unit
) {
    val historyItems = projects.filter { it.type == filterType }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInHorizontally(initialOffsetX = { -it }),
        exit = fadeOut() + slideOutHorizontally(targetOffsetX = { -it })
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.65f))
                .clickable { onDismiss() }
                .testTag("studio_history_drawer")
        ) {
            Column(
                modifier = Modifier
                    .width(288.dp)
                    .fillMaxHeight()
                    .background(AppCardBg)
                    .clickable(enabled = false) {}
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .background(AgnesViolet.copy(alpha = 0.18f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = AgnesVioletLight,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = title,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppTextPrimary
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "关闭",
                            tint = AppTextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Surface(
                    onClick = onNewSession,
                    shape = RoundedCornerShape(12.dp),
                    color = AgnesViolet.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, AgnesViolet),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_session_button")
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = AgnesCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "新建创作",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppTextPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "历史记录 (${historyItems.size})",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppTextSecondary
                )

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (historyItems.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "暂无历史记录",
                                    fontSize = 12.sp,
                                    color = AppTextSecondary
                                )
                            }
                        }
                    }

                    items(historyItems, key = { it.id }) { project ->
                        val isSelected = project.id == selectedProjectId
                        Surface(
                            onClick = { onSelectProject(project) },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) AgnesViolet.copy(alpha = 0.18f) else AppSubtleBg,
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) AgnesViolet else AppCardBorder
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("history_item_${project.id}")
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (filterType == ProjectType.VIDEO_SCRIPT_AND_STITCH) Icons.Default.Movie else Icons.Default.Image,
                                    contentDescription = null,
                                    tint = if (filterType == ProjectType.VIDEO_SCRIPT_AND_STITCH) AgnesCyan else AgnesVioletLight,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = project.title,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = AppTextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = project.statusMessage.ifBlank { project.prompt },
                                        fontSize = 10.sp,
                                        color = AppTextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                IconButton(
                                    onClick = { onDeleteProject(project) },
                                    modifier = Modifier.size(26.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = "删除",
                                        tint = AppTextSecondary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
