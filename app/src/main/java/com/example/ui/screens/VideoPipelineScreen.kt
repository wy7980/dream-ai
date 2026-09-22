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
import androidx.compose.material.icons.filled.Tune
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
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import kotlin.math.roundToInt
import coil.compose.AsyncImage
import com.example.data.model.GenerationProject
import com.example.data.model.GenerationStatus
import com.example.data.model.ProjectType
import com.example.data.model.VideoDurationLimits
import com.example.data.model.VideoSceneLimits
import com.example.ui.components.ImagePickerBottomSheet
import com.example.ui.components.RateLimitBanner
import com.example.ui.components.SceneCard
import com.example.ui.components.StudioHistoryDrawer
import com.example.ui.components.StudioTopBar
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
    val projects by viewModel.projects.collectAsState()

    var themePrompt by remember {
        mutableStateOf(
            initialPrompt ?: "赛博朋克探险家在雨夜潜入霓虹高塔，解开量子能量核心，引发城市能量觉醒的史诗冒险"
        )
    }
    var sourceImageUri by remember { mutableStateOf(initialImageUri) }
    var selectedModel by remember { mutableStateOf("agnes-video-2.5-flash") }
    var selectedRatio by remember { mutableStateOf("16:9") }
    var sceneDuration by remember { mutableIntStateOf(VideoDurationLimits.DEFAULT) }
    var sceneCount by remember { mutableIntStateOf(VideoSceneLimits.DEFAULT) }
    // When true the director model sizes the film (scene count + per-scene duration) from the
    // material instead of the user guessing. Sliders are disabled and shown as "AI 规划".
    var autoPlan by remember { mutableStateOf(true) }
    var selectedStyle by remember { mutableStateOf("Cinematic 3D") }
    var showImagePicker by remember { mutableStateOf(false) }
    var showHistoryDrawer by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val activeVideoProject = if (selectedProject?.type == ProjectType.VIDEO_SCRIPT_AND_STITCH) selectedProject else null
    val videoHistoryCount = projects.count { it.type == ProjectType.VIDEO_SCRIPT_AND_STITCH }

    // Keep the workspace inputs in sync with the selected task. This covers selection from the
    // history drawer, the gallery, the agent, AND re-entry into this tab (where the screen's
    // `remember` state is recreated because AnimatedContent disposes it on switch). Without it
    // the prompt box silently falls back to the default prompt while a history task is shown.
    LaunchedEffect(selectedProject?.id) {
        val project = selectedProject ?: return@LaunchedEffect
        themePrompt = project.prompt
        sourceImageUri = project.sourceImageUri
        if (project.totalClips in VideoSceneLimits.MIN..VideoSceneLimits.MAX) {
            sceneCount = project.totalClips
        }
        // Recover the per-scene duration from the project total (total = scenes * perScene).
        if (project.totalClips > 0 && project.durationSeconds > 0) {
            sceneDuration = VideoDurationLimits.clamp(project.durationSeconds / project.totalClips)
        }
        if (project.stylePreset.isNotBlank()) selectedStyle = project.stylePreset
        if (project.aspectRatio.isNotBlank()) selectedRatio = project.aspectRatio
    }

    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Minimalist native top bar: history lives in a drawer, not on the workspace screen.
            StudioTopBar(
                title = "AI 视频分镜与拼接",
                subtitle = "分镜规划 ➔ 逐段生成 ➔ 一键拼接",
                icon = Icons.Default.Movie,
                gradient = listOf(AgnesCyan, AgnesViolet),
                onOpenHistory = { showHistoryDrawer = true },
                historyBadgeCount = videoHistoryCount
            )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
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
                        text = "电影短片创作工作台",
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

                        Column(horizontalAlignment = Alignment.End) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "分镜幕数:",
                                    fontSize = 11.sp,
                                    color = AppTextSecondary,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (autoPlan) "AI 自动规划" else "$sceneCount 幕",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (autoPlan) AgnesViolet else AgnesCyan
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (autoPlan) {
                                    "由 AI 按素材自然单元分幕（每幕 1 次限速请求）"
                                } else {
                                    "可选 ${VideoSceneLimits.MIN}-${VideoSceneLimits.MAX} 幕 (每幕 1 次限速请求)"
                                },
                                fontSize = 9.sp,
                                color = AppTextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Scene count slider (1..20). Disabled while AI auto-planning is on.
                    Slider(
                        value = sceneCount.toFloat(),
                        onValueChange = { sceneCount = it.roundToInt().coerceIn(VideoSceneLimits.MIN, VideoSceneLimits.MAX) },
                        enabled = !autoPlan,
                        valueRange = VideoSceneLimits.MIN.toFloat()..VideoSceneLimits.MAX.toFloat(),
                        steps = VideoSceneLimits.MAX - VideoSceneLimits.MIN - 1,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("scene_count_slider"),
                        colors = SliderDefaults.colors(
                            thumbColor = AgnesCyan,
                            activeTrackColor = AgnesCyan,
                            inactiveTrackColor = AppSubtleBg
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Per-scene duration (4..12s). Disabled while AI auto-planning is on.
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "单幕时长:",
                            fontSize = 11.sp,
                            color = AppTextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = if (autoPlan) "AI 按旁白长度" else "$sceneDuration 秒",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = AgnesViolet
                        )
                    }

                    Slider(
                        value = sceneDuration.toFloat(),
                        onValueChange = { sceneDuration = it.roundToInt().coerceIn(VideoDurationLimits.MIN, VideoDurationLimits.MAX) },
                        enabled = !autoPlan,
                        valueRange = VideoDurationLimits.MIN.toFloat()..VideoDurationLimits.MAX.toFloat(),
                        steps = VideoDurationLimits.MAX - VideoDurationLimits.MIN - 1,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("scene_duration_slider"),
                        colors = SliderDefaults.colors(
                            thumbColor = AgnesViolet,
                            activeTrackColor = AgnesViolet,
                            inactiveTrackColor = AppSubtleBg
                        )
                    )

                    Text(
                        text = if (autoPlan) {
                            "AI 将根据素材自动决定幕数与每幕时长，确保内容不遗漏"
                        } else {
                            "可选 ${VideoDurationLimits.MIN}-${VideoDurationLimits.MAX} 秒/幕 · 成片总时长约 ${sceneCount * sceneDuration} 秒"
                        },
                        fontSize = 9.sp,
                        color = AppTextSecondary
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Auto-plan toggle: on by default. Turning it off hands the two sliders back to
                    // the user for precise control (and pins both numbers for the whole pipeline).
                    Surface(
                        onClick = { autoPlan = !autoPlan },
                        shape = RoundedCornerShape(10.dp),
                        color = if (autoPlan) AgnesViolet.copy(alpha = 0.14f) else AppSubtleBg,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (autoPlan) AgnesViolet else AppCardBorder
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auto_plan_toggle")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (autoPlan) Icons.Default.AutoAwesome else Icons.Default.Tune,
                                contentDescription = null,
                                tint = if (autoPlan) AgnesViolet else AppTextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "AI 自动规划时长与分镜数",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AppTextPrimary
                                )
                                Text(
                                    text = if (autoPlan) "已开启 · 按素材自然单元分幕，内容不遗漏" else "已关闭 · 使用上方滑块手动指定",
                                    fontSize = 9.sp,
                                    color = AppTextSecondary
                                )
                            }
                            Switch(
                                checked = autoPlan,
                                onCheckedChange = { autoPlan = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = AgnesViolet,
                                    checkedTrackColor = AgnesViolet.copy(alpha = 0.5f)
                                )
                            )
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
                                    sceneCount = if (autoPlan) VideoSceneLimits.AUTO else sceneCount,
                                    stylePreset = selectedStyle,
                                    videoModel = selectedModel,
                                    aspectRatio = selectedRatio,
                                    durationPerScene = if (autoPlan) VideoDurationLimits.AUTO else sceneDuration
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
                                    text = if (autoPlan) "一键开启：AI 规划分镜 ➔ 生成多段视频 ➔ 拼接长视频" else "一键开启：规划分镜 ➔ 生成多段视频 ➔ 拼接长视频",
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
                    },
                    onSavePrompt = { title, visualPrompt, cameraMovement, narration ->
                        viewModel.updateScenePrompt(
                            clipId = clip.id,
                            title = title,
                            visualPrompt = visualPrompt,
                            cameraMovement = cameraMovement,
                            narration = narration
                        )
                    }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
        }

        // Studio history drawer: only video projects, so history is no longer dumped inline.
        StudioHistoryDrawer(
            visible = showHistoryDrawer,
            title = "视频拼接历史",
            projects = projects,
            filterType = ProjectType.VIDEO_SCRIPT_AND_STITCH,
            selectedProjectId = selectedProject?.id,
            onDismiss = { showHistoryDrawer = false },
            onSelectProject = { project ->
                viewModel.selectProject(project)
                showHistoryDrawer = false
            },
            onNewSession = {
                showHistoryDrawer = false
                viewModel.selectProject(null)
                themePrompt = ""
                sourceImageUri = null
                sceneCount = VideoSceneLimits.DEFAULT
            },
            onDeleteProject = { project -> viewModel.deleteProject(project) }
        )
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
