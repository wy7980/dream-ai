package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.model.GenerationProject
import com.example.data.model.GenerationStatus
import com.example.data.model.ProjectType
import com.example.data.model.SceneClip
import com.example.ui.components.VideoTimelinePlayer
import com.example.ui.theme.AgnesAmber
import com.example.ui.theme.AgnesCyan
import com.example.ui.theme.AgnesEmerald
import com.example.ui.theme.AgnesViolet
import com.example.ui.theme.AgnesVioletLight
import com.example.ui.theme.CyberCardBg
import com.example.ui.theme.CyberCardBorder
import com.example.ui.viewmodel.AgnesViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Helper to safely parse local File paths and remote URLs for Coil
fun safeImageModel(uri: String?): Any? {
    if (uri.isNullOrBlank()) return null
    return if (uri.startsWith("/")) File(uri) else uri
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    viewModel: AgnesViewModel,
    onOpenProjectInStudio: (GenerationProject) -> Unit,
    onConvertToVideo: (String, String) -> Unit = { _, _ -> },
    onNavigateToCreate: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val projects by viewModel.projects.collectAsState()
    val selectedClips by viewModel.selectedProjectClips.collectAsState()

    var selectedFilter by remember { mutableStateOf("ALL") }
    var isGridView by remember { mutableStateOf(true) }

    // Direct preview state (Image or Video)
    var activePreviewProject by remember { mutableStateOf<GenerationProject?>(null) }

    val filteredProjects = when (selectedFilter) {
        "IMAGE" -> projects.filter { it.type == ProjectType.IMAGE_TO_IMAGE }
        "VIDEO" -> projects.filter { it.type == ProjectType.VIDEO_SCRIPT_AND_STITCH }
        else -> projects
    }

    // Load clips when opening a video project for direct preview
    LaunchedEffect(activePreviewProject) {
        if (activePreviewProject != null) {
            viewModel.selectProject(activePreviewProject)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0A0D14))
            .padding(12.dp)
            .testTag("gallery_screen")
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            Brush.linearGradient(listOf(AgnesViolet, AgnesCyan)),
                            RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "创作作品集与历史档案",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "共 ${projects.size} 个作品 (点击即可直接大图/播放查看)",
                        fontSize = 10.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            }

            // View toggle (Grid / List)
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF161E31),
                border = androidx.compose.foundation.BorderStroke(1.dp, CyberCardBorder)
            ) {
                Row(modifier = Modifier.padding(2.dp)) {
                    IconButton(
                        onClick = { isGridView = true },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.GridView,
                            contentDescription = "Grid View",
                            tint = if (isGridView) AgnesCyan else Color(0xFF64748B),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(
                        onClick = { isGridView = false },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ViewList,
                            contentDescription = "List View",
                            tint = if (!isGridView) AgnesCyan else Color(0xFF64748B),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Filter Tabs
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChip(
                selected = selectedFilter == "ALL",
                onClick = { selectedFilter = "ALL" },
                label = { Text("全部 (${projects.size})", fontSize = 11.sp) },
                shape = RoundedCornerShape(8.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = AgnesViolet,
                    selectedLabelColor = Color.White,
                    containerColor = Color(0xFF161E31),
                    labelColor = Color(0xFFCBD5E1)
                )
            )
            FilterChip(
                selected = selectedFilter == "IMAGE",
                onClick = { selectedFilter = "IMAGE" },
                label = { Text("🎨 图片 (${projects.count { it.type == ProjectType.IMAGE_TO_IMAGE }})", fontSize = 11.sp) },
                shape = RoundedCornerShape(8.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = AgnesViolet,
                    selectedLabelColor = Color.White,
                    containerColor = Color(0xFF161E31),
                    labelColor = Color(0xFFCBD5E1)
                )
            )
            FilterChip(
                selected = selectedFilter == "VIDEO",
                onClick = { selectedFilter = "VIDEO" },
                label = { Text("🎬 视频 (${projects.count { it.type == ProjectType.VIDEO_SCRIPT_AND_STITCH }})", fontSize = 11.sp) },
                shape = RoundedCornerShape(8.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = AgnesCyan,
                    selectedLabelColor = Color(0xFF0F172A),
                    containerColor = Color(0xFF161E31),
                    labelColor = Color(0xFFCBD5E1)
                )
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Gallery Content
        if (filteredProjects.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .background(Color(0xFF161E31), CircleShape)
                            .border(1.dp, CyberCardBorder, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = AgnesVioletLight,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "暂无对应作品",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "可通过助手对话或工作台生成高清图片与多段拼接视频",
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { onNavigateToCreate(false) },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AgnesViolet)
                        ) {
                            Icon(imageVector = Icons.Default.Image, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("创作图片", fontSize = 11.sp)
                        }
                        Button(
                            onClick = { onNavigateToCreate(true) },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF161E31))
                        ) {
                            Icon(imageVector = Icons.Default.Movie, contentDescription = null, tint = AgnesCyan, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("生成视频", fontSize = 11.sp, color = AgnesCyan)
                        }
                    }
                }
            }
        } else {
            if (isGridView) {
                // Grid View
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 20.dp)
                ) {
                    items(filteredProjects, key = { it.id }) { project ->
                        ProjectGridCard(
                            project = project,
                            onClick = {
                                activePreviewProject = project
                            },
                            onDelete = { viewModel.deleteProject(project) }
                        )
                    }
                }
            } else {
                // List View
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 20.dp)
                ) {
                    items(filteredProjects, key = { it.id }) { project ->
                        ProjectDetailedListCard(
                            project = project,
                            onClick = {
                                activePreviewProject = project
                            },
                            onOpenStudio = {
                                viewModel.selectProject(project)
                                onOpenProjectInStudio(project)
                            },
                            onDelete = { viewModel.deleteProject(project) }
                        )
                    }
                }
            }
        }
    }

    // Direct Full View / Player Dialog
    activePreviewProject?.let { project ->
        if (project.type == ProjectType.VIDEO_SCRIPT_AND_STITCH) {
            DirectVideoViewerDialog(
                project = project,
                clips = selectedClips,
                onDismiss = { activePreviewProject = null },
                onOpenInStudio = {
                    val p = project
                    activePreviewProject = null
                    viewModel.selectProject(p)
                    onOpenProjectInStudio(p)
                },
                onExport = {
                    viewModel.showToast("🎬 完整多段拼接长视频已成功保存至本地媒体库！")
                },
                onDelete = {
                    viewModel.deleteProject(project)
                    activePreviewProject = null
                }
            )
        } else {
            DirectImageViewerDialog(
                project = project,
                onDismiss = { activePreviewProject = null },
                onConvertToVideo = {
                    val p = project
                    activePreviewProject = null
                    val uri = p.resultImageUri ?: p.sourceImageUri ?: ""
                    onConvertToVideo(uri, p.prompt)
                },
                onOpenInStudio = {
                    val p = project
                    activePreviewProject = null
                    viewModel.selectProject(p)
                    onOpenProjectInStudio(p)
                },
                onSave = {
                    viewModel.showToast("🎨 高清图像已成功保存至相册！")
                },
                onDelete = {
                    viewModel.deleteProject(project)
                    activePreviewProject = null
                }
            )
        }
    }
}

/**
 * Modern Grid Visual Card with full media preview
 */
@Composable
fun ProjectGridCard(
    project: GenerationProject,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val isVideo = project.type == ProjectType.VIDEO_SCRIPT_AND_STITCH
    val dateFormat = remember { SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()) }
    val timeStr = dateFormat.format(Date(project.createdAt))
    val previewUri = project.resultImageUri ?: project.resultVideoUri ?: project.sourceImageUri

    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, CyberCardBorder, RoundedCornerShape(12.dp)),
        color = CyberCardBg,
        tonalElevation = 3.dp
    ) {
        Column {
            // Visual Preview Aspect Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(if (isVideo) 16f / 10f else 1f)
                    .background(Color(0xFF161E31)),
                contentAlignment = Alignment.Center
            ) {
                if (previewUri != null) {
                    AsyncImage(
                        model = safeImageModel(previewUri),
                        contentDescription = project.prompt,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = if (isVideo) Icons.Default.Movie else Icons.Default.Image,
                        contentDescription = null,
                        tint = if (isVideo) AgnesCyan else AgnesViolet,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Gradient Overlay for Readability
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f)),
                                startY = 80f
                            )
                        )
                )

                // Top Badges
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(6.dp)
                        .align(Alignment.TopCenter),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                if (isVideo) AgnesCyan.copy(alpha = 0.85f) else AgnesViolet.copy(alpha = 0.85f),
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (isVideo) "🎬 视频" else "🎨 图片",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isVideo) Color(0xFF0F172A) else Color.White
                        )
                    }

                    if (isVideo && project.totalClips > 0) {
                        Box(
                            modifier = Modifier
                                .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${project.completedClips}/${project.totalClips}幕",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = AgnesEmerald
                            )
                        }
                    }
                }

                // Center Play or Zoom Indicator
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                        .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                        .align(Alignment.Center),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isVideo) Icons.Default.PlayArrow else Icons.Default.ZoomIn,
                        contentDescription = if (isVideo) "播放视频" else "查看大图",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Bottom text overlay inside image
                Text(
                    text = "点击直接${if (isVideo) "播放" else "查看大图"}",
                    fontSize = 9.sp,
                    color = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 6.dp)
                )
            }

            // Text Info & Actions
            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = project.prompt,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 15.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$timeStr • ${project.stylePreset}",
                        fontSize = 9.sp,
                        color = Color(0xFF94A3B8),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete",
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Detailed List Card with high information density and direct preview buttons
 */
@Composable
fun ProjectDetailedListCard(
    project: GenerationProject,
    onClick: () -> Unit,
    onOpenStudio: () -> Unit,
    onDelete: () -> Unit
) {
    val isVideo = project.type == ProjectType.VIDEO_SCRIPT_AND_STITCH
    val dateFormat = remember { SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()) }
    val timeStr = dateFormat.format(Date(project.createdAt))
    val previewUri = project.resultImageUri ?: project.resultVideoUri ?: project.sourceImageUri

    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, CyberCardBorder, RoundedCornerShape(12.dp)),
        color = CyberCardBg,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Big Thumbnail
            Box(
                modifier = Modifier
                    .size(width = 110.dp, height = 76.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF161E31))
                    .border(1.dp, if (isVideo) AgnesCyan.copy(alpha = 0.4f) else AgnesViolet.copy(alpha = 0.4f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (previewUri != null) {
                    AsyncImage(
                        model = safeImageModel(previewUri),
                        contentDescription = project.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = if (isVideo) Icons.Default.Movie else Icons.Default.Image,
                        contentDescription = null,
                        tint = if (isVideo) AgnesCyan else AgnesViolet,
                        modifier = Modifier.size(26.dp)
                    )
                }

                if (isVideo) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(Color.Black.copy(alpha = 0.65f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            .align(Alignment.BottomEnd)
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ZoomIn,
                            contentDescription = "Zoom",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(
                                if (isVideo) AgnesCyan.copy(alpha = 0.2f) else AgnesViolet.copy(alpha = 0.2f),
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (isVideo) "多段拼接视频" else "AI 图像重绘",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isVideo) AgnesCyan else AgnesViolet
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    if (isVideo && project.totalClips > 0) {
                        Text(
                            text = "${project.completedClips}/${project.totalClips} 幕已合成",
                            fontSize = 10.sp,
                            color = AgnesEmerald,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = project.prompt,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$timeStr • ${project.stylePreset}",
                        fontSize = 10.sp,
                        color = Color(0xFF94A3B8)
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            onClick = onClick,
                            shape = RoundedCornerShape(4.dp),
                            color = if (isVideo) AgnesCyan.copy(alpha = 0.2f) else AgnesViolet.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = if (isVideo) "▶ 播放" else "🔍 查看",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isVideo) AgnesCyan else AgnesVioletLight,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Delete",
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Direct High-Definition Image Preview Dialog (大图查看与对比交互弹窗)
 */
@Composable
fun DirectImageViewerDialog(
    project: GenerationProject,
    onDismiss: () -> Unit,
    onConvertToVideo: () -> Unit,
    onOpenInStudio: () -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit
) {
    var showComparison by remember { mutableStateOf(false) }
    var splitPosition by remember { mutableFloatStateOf(0.5f) }
    val resultImage = project.resultImageUri ?: project.sourceImageUri
    val sourceImage = project.sourceImageUri

    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }
    val timeStr = dateFormat.format(Date(project.createdAt))

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF070A10))
                .padding(12.dp)
                .testTag("direct_image_viewer_dialog"),
            color = Color(0xFF070A10)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // Top Bar with Close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(AgnesViolet.copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = null,
                                tint = AgnesViolet,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "🎨 高清图像查看与变奏",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "$timeStr • 比例 ${project.aspectRatio} • ${project.stylePreset}",
                                fontSize = 10.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .background(Color(0xFF161E31), CircleShape)
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Image Showcase Viewport
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(if (project.aspectRatio == "16:9") 16f / 9f else if (project.aspectRatio == "9:16") 9f / 16f else 1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black)
                        .border(1.dp, AgnesViolet.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (showComparison && sourceImage != null && resultImage != null) {
                        // Interactive Split Comparison
                        Row(modifier = Modifier.fillMaxSize()) {
                            Box(
                                modifier = Modifier
                                    .weight(splitPosition)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp))
                            ) {
                                AsyncImage(
                                    model = safeImageModel(sourceImage),
                                    contentDescription = "原图",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                Box(
                                    modifier = Modifier
                                        .padding(6.dp)
                                        .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                        .align(Alignment.TopStart)
                                ) {
                                    Text("原参考图", color = Color.White, fontSize = 9.sp)
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .width(2.dp)
                                    .fillMaxHeight()
                                    .background(AgnesCyan)
                            )

                            Box(
                                modifier = Modifier
                                    .weight(1f - splitPosition)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp))
                            ) {
                                AsyncImage(
                                    model = safeImageModel(resultImage),
                                    contentDescription = "重绘图",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                Box(
                                    modifier = Modifier
                                        .padding(6.dp)
                                        .background(AgnesViolet, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                        .align(Alignment.TopEnd)
                                ) {
                                    Text("AI 重绘结果", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    } else {
                        // Single Image Full View
                        if (resultImage != null) {
                            AsyncImage(
                                model = safeImageModel(resultImage),
                                contentDescription = project.prompt,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        } else {
                            Icon(imageVector = Icons.Default.Image, contentDescription = null, tint = AgnesViolet, modifier = Modifier.size(48.dp))
                        }
                    }
                }

                // Comparison Slider if source image is available
                if (sourceImage != null && resultImage != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            onClick = { showComparison = !showComparison },
                            shape = RoundedCornerShape(6.dp),
                            color = if (showComparison) AgnesCyan.copy(alpha = 0.2f) else Color(0xFF161E31),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (showComparison) AgnesCyan else CyberCardBorder)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(imageVector = Icons.Default.Compare, contentDescription = null, tint = if (showComparison) AgnesCyan else Color.White, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (showComparison) "退出原图对比" else "对比原参考图", fontSize = 10.sp, color = if (showComparison) AgnesCyan else Color.White)
                            }
                        }

                        if (showComparison) {
                            Slider(
                                value = splitPosition,
                                onValueChange = { splitPosition = it },
                                valueRange = 0.1f..0.9f,
                                modifier = Modifier.width(180.dp),
                                colors = SliderDefaults.colors(
                                    thumbColor = AgnesCyan,
                                    activeTrackColor = AgnesCyan,
                                    inactiveTrackColor = Color(0xFF1E293B)
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Prompt & Generation Details Card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = CyberCardBg,
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyberCardBorder)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "提示词构想 (Prompt):",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = AgnesCyan
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = project.prompt,
                            fontSize = 12.sp,
                            color = Color.White,
                            lineHeight = 17.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFF161E31), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("风格: ${project.stylePreset}", fontSize = 9.sp, color = Color(0xFFCBD5E1))
                            }
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFF161E31), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("画幅: ${project.aspectRatio}", fontSize = 9.sp, color = Color(0xFFCBD5E1))
                            }
                            Box(
                                modifier = Modifier
                                    .background(AgnesEmerald.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("8K 高清渲染", fontSize = 9.sp, color = AgnesEmerald, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onConvertToVideo,
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AgnesCyan)
                    ) {
                        Icon(imageVector = Icons.Default.Movie, contentDescription = null, tint = Color(0xFF0F172A), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("🎬 转为视频分镜", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                    }

                    Button(
                        onClick = onOpenInStudio,
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AgnesViolet)
                    ) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("🎨 在工作台编辑", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onSave,
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF161E31))
                    ) {
                        Icon(imageVector = Icons.Default.Download, contentDescription = null, tint = AgnesEmerald, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("保存到相册", fontSize = 11.sp, color = AgnesEmerald)
                    }

                    Button(
                        onClick = onDelete,
                        modifier = Modifier
                            .weight(0.7f)
                            .height(38.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A1215))
                    ) {
                        Icon(imageVector = Icons.Default.DeleteOutline, contentDescription = null, tint = Color(0xFFF87171), modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("删除作品", fontSize = 11.sp, color = Color(0xFFF87171))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

/**
 * Direct Video Player & Storyboard Script Dialog (视频全屏播放与分镜剧本弹窗)
 */
@Composable
fun DirectVideoViewerDialog(
    project: GenerationProject,
    clips: List<SceneClip>,
    onDismiss: () -> Unit,
    onOpenInStudio: () -> Unit,
    onExport: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }
    val timeStr = dateFormat.format(Date(project.createdAt))

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF070A10))
                .padding(12.dp)
                .testTag("direct_video_viewer_dialog"),
            color = Color(0xFF070A10)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // Top Bar with Close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(AgnesCyan.copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Movie,
                                contentDescription = null,
                                tint = AgnesCyan,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "🎬 多段拼接长视频播放器",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "$timeStr • ${project.completedClips}/${project.totalClips} 幕已无缝合成",
                                fontSize = 10.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .background(Color(0xFF161E31), CircleShape)
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Embedded Video Timeline Player Component
                VideoTimelinePlayer(
                    project = project,
                    clips = clips,
                    onExportVideo = onExport
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Multi-Scene Storyboard Breakdown
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = CyberCardBg,
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyberCardBorder)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "📋 电影分镜脚本清单 (${clips.size} 幕)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = AgnesCyan
                            )
                            Text(
                                text = "总时长: ${clips.sumOf { it.durationSeconds }}s",
                                fontSize = 10.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        clips.forEach { clip ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .background(Color(0xFF161E31), RoundedCornerShape(6.dp))
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .background(AgnesViolet, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${clip.sceneNumber}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = clip.sceneTitle,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Text(
                                            text = "${clip.durationSeconds}s • ${clip.cameraMovement}",
                                            fontSize = 9.sp,
                                            color = AgnesCyan
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(2.dp))

                                    Text(
                                        text = clip.visualPrompt,
                                        fontSize = 10.sp,
                                        color = Color(0xFF94A3B8),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onOpenInStudio,
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AgnesCyan)
                    ) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = null, tint = Color(0xFF0F172A), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("🎬 在视频工作台重制", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                    }

                    Button(
                        onClick = onExport,
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AgnesEmerald)
                    ) {
                        Icon(imageVector = Icons.Default.Download, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("💾 导出长视频", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = onDelete,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A1215))
                ) {
                    Icon(imageVector = Icons.Default.DeleteOutline, contentDescription = null, tint = Color(0xFFF87171), modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("删除此视频项目", fontSize = 11.sp, color = Color(0xFFF87171))
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
