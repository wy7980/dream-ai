package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.GenerationProject
import com.example.data.model.GenerationStatus
import com.example.data.model.ProjectType
import com.example.ui.components.ImagePickerBottomSheet
import com.example.ui.components.RateLimitBanner
import com.example.ui.components.SceneCard
import com.example.ui.components.VideoTimelinePlayer
import com.example.ui.theme.AgnesAmber
import com.example.ui.theme.AgnesCyan
import com.example.ui.theme.AgnesEmerald
import com.example.ui.theme.AgnesViolet
import com.example.ui.theme.AppCardBg
import com.example.ui.theme.AppCardBorder
import com.example.ui.theme.AppInputBg
import com.example.ui.theme.AppSubtleBg
import com.example.ui.theme.AppSurface
import com.example.ui.theme.AppTextPrimary
import com.example.ui.theme.AppTextSecondary
import androidx.compose.material3.MaterialTheme
import com.example.ui.viewmodel.AgnesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoPipelineScreen(
    viewModel: AgnesViewModel,
    initialImageUri: String? = null,
    initialPrompt: String? = null,
    modifier: Modifier = Modifier
) {
    val rateLimitState by viewModel.rateLimitState.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val isVideoGenerating by viewModel.isVideoGenerating.collectAsState()
    val progressMessage by viewModel.progressMessage.collectAsState()
    val videoProgressMessage by viewModel.videoProgressMessage.collectAsState()
    val selectedProject by viewModel.selectedProject.collectAsState()
    val selectedClips by viewModel.selectedProjectClips.collectAsState()
    val rerunningClipId by viewModel.rerunningClipId.collectAsState()

    var themePrompt by remember {
        mutableStateOf(
            initialPrompt ?: "赛博朋克探险家在雨夜潜入霓虹高塔，解开量子能量核心，引发城市能量觉醒的史诗冒险"
        )
    }
    var sourceImageUri by remember { mutableStateOf(initialImageUri) }
    var selectedModel by remember { mutableStateOf("agnes-video-2.5-flash") }
    var selectedRatio by remember { mutableStateOf("16:9") }
    var sceneDuration by remember { mutableIntStateOf(5) }
    var sceneCount by remember { mutableIntStateOf(4) }
    var selectedStyle by remember { mutableStateOf("Cinematic 3D") }
    var showImagePicker by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val activeVideoProject = if (selectedProject?.type == ProjectType.VIDEO_SCRIPT_AND_STITCH) selectedProject else null

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 12.dp)
            .testTag("video_pipeline_screen"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(2.dp))
            // Rate limit header
            RateLimitBanner(rateLimitState = rateLimitState)
        }

        // Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            Brush.linearGradient(listOf(AgnesCyan, AgnesViolet)),
                            RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Movie,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "AI 视频分镜拆解与无缝拼接流水线",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppTextPrimary
                    )
                    Text(
                        text = "智能分镜规划 ➔ 顺序生成多段视频 (1次/分) ➔ 一键拼接合成",
                        fontSize = 10.sp,
                        color = AppTextSecondary
                    )
                }
            }
        }

        // Input Card: Reference Image & Story Idea
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, AppCardBorder, RoundedCornerShape(12.dp)),
                color = AppCardBg,
                tonalElevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "1. 上传起始参考图 / 关键帧 (可选):",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppTextPrimary
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    if (sourceImageUri != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Black)
                        ) {
                            AsyncImage(
                                model = sourceImageUri,
                                contentDescription = "Reference Keyframe",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            IconButton(
                                onClick = { sourceImageUri = null },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(4.dp)
                                    .size(24.dp)
                                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    } else {
                        Button(
                            onClick = { showImagePicker = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(38.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AppSubtleBg
                            )
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AddPhotoAlternate,
                                    contentDescription = null,
                                    tint = AgnesCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "选择参考图片 (将作为分镜第 1 幕的首帧基底)",
                                    fontSize = 11.sp,
                                    color = AppTextSecondary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "2. 电影短片主题与剧情构思:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppTextPrimary
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    OutlinedTextField(
                        value = themePrompt,
                        onValueChange = { themePrompt = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("video_theme_input"),
                        placeholder = { Text("描述故事主线、人物动作与视觉高潮...", color = AppTextSecondary, fontSize = 12.sp) },
                        minLines = 2,
                        maxLines = 4,
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AgnesCyan,
                            unfocusedBorderColor = AppCardBorder,
                            focusedContainerColor = AppInputBg,
                            unfocusedContainerColor = AppInputBg,
                            focusedTextColor = AppTextPrimary,
                            unfocusedTextColor = AppTextPrimary
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Model Selection: 2.5-flash vs v2.0
                    Text(
                        text = "生成模型与画质引擎:",
                        fontSize = 11.sp,
                        color = AppTextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val models = listOf(
                            "agnes-video-2.5-flash" to "2.5-Flash (快速高清 720P)",
                            "agnes-video-v2.0" to "2.0 (多分辨率支持)"
                        )
                        models.forEach { (modelId, label) ->
                            val isSelected = selectedModel == modelId
                            Surface(
                                onClick = { selectedModel = modelId },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) AgnesViolet.copy(alpha = 0.2f) else AppSubtleBg,
                                border = BorderStroke(1.dp, if (isSelected) AgnesViolet else Color.Transparent),
                                modifier = Modifier.weight(1f).height(34.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 4.dp)) {
                                    Text(
                                        text = label,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) AgnesCyan else AppTextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Aspect Ratio and Scene count
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "画面比例:",
                                fontSize = 11.sp,
                                color = AppTextSecondary,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                listOf("16:9", "9:16", "1:1").forEach { ratio ->
                                    val isSelected = selectedRatio == ratio
                                    Surface(
                                        onClick = { selectedRatio = ratio },
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (isSelected) AgnesCyan else AppSubtleBg,
                                        modifier = Modifier.size(width = 46.dp, height = 28.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = ratio,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) Color(0xFF0A0D14) else AppTextPrimary
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Column {
                            Text(
                                text = "分镜幕数:",
                                fontSize = 11.sp,
                                color = AppTextSecondary,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                listOf(3, 4, 5).forEach { count ->
                                    val isSelected = sceneCount == count
                                    Surface(
                                        onClick = { sceneCount = count },
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (isSelected) AgnesCyan else AppSubtleBg,
                                        modifier = Modifier.size(width = 40.dp, height = 28.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = "${count}幕",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) Color(0xFF0A0D14) else AppTextPrimary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Launch / Cancel Pipeline Button
                    val isRunning = isVideoGenerating || isGenerating
                    if (isRunning) {
                        Button(
                            onClick = { viewModel.cancelVideoTask() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("cancel_pipeline_button"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFEF4444)
                            )
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Stop,
                                    contentDescription = "终止视频流水线",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "终止视频生成流水线 (${if (videoProgressMessage.isNotBlank()) videoProgressMessage else if (progressMessage.isNotBlank()) progressMessage else "正在运行"})",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    } else {
                        Button(
                            onClick = {
                                viewModel.startVideoPipeline(
                                    themePrompt = themePrompt,
                                    sourceImageUri = sourceImageUri,
                                    sceneCount = sceneCount,
                                    stylePreset = selectedStyle,
                                    videoModel = selectedModel,
                                    aspectRatio = selectedRatio,
                                    durationPerScene = sceneDuration
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("start_pipeline_button"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AgnesCyan
                            )
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = Color(0xFF0F172A),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "一键开启：规划分镜 ➔ 生成多段视频 ➔ 拼接长视频",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Active Stitched Video Player if pipeline finished or has clips
        if (activeVideoProject != null && selectedClips.isNotEmpty()) {
            item {
                Text(
                    text = "合成视频预览与分镜时间轴:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppTextPrimary
                )
            }

            item {
                VideoTimelinePlayer(
                    project = activeVideoProject,
                    clips = selectedClips,
                    onExportVideo = {
                        viewModel.exportVideoProject(activeVideoProject, selectedClips)
                    }
                )
            }

            // Storyboard Scene breakdown list
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "各分镜详细脚本与生成状态 (${selectedClips.size}幕):",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppTextPrimary
                    )

                    Box(
                        modifier = Modifier
                            .background(AppSubtleBg, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "每段间隔 60s 冷却",
                            fontSize = 10.sp,
                            color = AgnesAmber,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            items(selectedClips, key = { it.id }) { clip ->
                SceneCard(
                    clip = clip,
                    isRerunning = rerunningClipId == clip.id,
                    onRerun = {
                        activeVideoProject?.let { proj ->
                            viewModel.rerunSceneClip(proj.id, clip.id)
                        }
                    }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showImagePicker) {
        ImagePickerBottomSheet(
            sheetState = sheetState,
            onDismiss = { showImagePicker = false },
            onImageSelected = { uri, prompt ->
                sourceImageUri = uri
                if (prompt != null && themePrompt.isBlank()) {
                    themePrompt = prompt
                }
            }
        )
    }
}
