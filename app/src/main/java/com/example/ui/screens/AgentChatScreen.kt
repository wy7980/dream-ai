package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.example.data.model.ChatMessage
import com.example.ui.components.MarkdownText
import com.example.data.model.ChatIntentMode
import com.example.data.skill.InvocationStatus
import com.example.ui.components.ActiveSkillExecutingBanner
import com.example.ui.components.AgentDocumentCard
import com.example.ui.components.AgentLoadedSkillsBar
import com.example.ui.components.AgentSkillManagerSheet
import com.example.ui.components.ImagePickerBottomSheet
import com.example.ui.components.RateLimitBanner
import com.example.ui.theme.AgnesAmber
import com.example.ui.theme.AgnesCyan
import com.example.ui.theme.AgnesEmerald
import com.example.ui.theme.AgnesViolet
import com.example.ui.theme.AgnesVioletDark
import com.example.ui.theme.AgnesVioletLight
import com.example.ui.theme.CyberCardBg
import com.example.ui.theme.CyberCardBorder
import com.example.ui.viewmodel.AgnesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgentChatScreen(
    viewModel: AgnesViewModel,
    onNavigateToVideo: () -> Unit,
    onNavigateToImage: () -> Unit,
    modifier: Modifier = Modifier
) {
    val chatMessages by viewModel.chatMessages.collectAsState()
    val projects by viewModel.projects.collectAsState()
    val rateLimitState by viewModel.rateLimitState.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val progressMessage by viewModel.progressMessage.collectAsState()
    val currentConfig by viewModel.config.collectAsState()
    val currentIntentMode by viewModel.chatIntentMode.collectAsState()
    val skills by viewModel.skills.collectAsState()
    val currentExecutingSkill by viewModel.currentExecutingSkill.collectAsState()

    var inputText by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<String?>(null) }
    var showImagePicker by remember { mutableStateOf(false) }
    var showHistoryDrawer by remember { mutableStateOf(false) }
    var showSkillManagerSheet by remember { mutableStateOf(false) }
    var showQuickPrompts by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Auto-scroll on new message
    LaunchedEffect(chatMessages.size) {
        if (chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(chatMessages.size - 1)
        }
    }

    val quickPrompts = listOf(
        "📝 撰写一份AI智能体商业合作策划案并导出Word",
        "📄 导出A4格式的智能影视制作技术白皮书PDF",
        "📊 制作一份2026年Q4短片制作预算明细Excel表",
        "💬 帮我构思一个赛博朋克雨夜侦探的微电影故事剧本",
        "🎨 根据此图片重绘为电影级霓虹光影概念艺术图",
        "🎬 将此概念生成4幕连续电影视频并自动拼接成片",
        "💡 推荐几个适合制作科幻短片的镜头提示词与运镜方式"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0A0D14))
            .testTag("agent_chat_screen")
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top Rate Limit Banner (Auto-hides when idle, slides in smoothly when cooling/queued)
            RateLimitBanner(
                rateLimitState = rateLimitState,
                autoHideWhenIdle = true,
                compact = true,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
            )

            // Clean Single-Row Header (Plan 1: Minimalist Native Top Bar)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Top Left: Menu Icon to Open History Drawer
                IconButton(
                    onClick = { showHistoryDrawer = true },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "历史对话列表",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Center: Pure Single-Row Title with AI Status Indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(
                                Brush.linearGradient(listOf(AgnesViolet, AgnesCyan)),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SmartToy,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(7.dp))
                    Text(
                        text = "Dream AI",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(AgnesEmerald, CircleShape)
                    )
                }

                // Top Right: Skills Hub & New Chat
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { showSkillManagerSheet = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.TopEnd) {
                            Icon(
                                imageVector = Icons.Default.Extension,
                                contentDescription = "智能体技能中心",
                                tint = AgnesCyan,
                                modifier = Modifier.size(20.dp)
                            )
                            val enabledCount = skills.count { it.isEnabled }
                            if (enabledCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(AgnesEmerald, CircleShape)
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = {
                            viewModel.clearChat()
                            viewModel.showToast("已开启新对话")
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "新建对话",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // Chat Messages List
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(chatMessages, key = { it.id }) { message ->
                    val relatedProject = message.relatedProjectId?.let { id -> projects.find { it.id == id } }
                    ChatMessageItem(
                        message = message,
                        project = relatedProject,
                        onOpenVideoStudio = onNavigateToVideo,
                        onOpenImageStudio = {
                            if (relatedProject != null) {
                                viewModel.selectProject(relatedProject)
                            }
                            onNavigateToImage()
                        },
                        onSaveImage = {
                            val imgUri = relatedProject?.resultImageUri ?: message.attachedImageUri
                            viewModel.saveImageToGallery(imgUri, relatedProject?.prompt ?: message.content)
                        },
                        onShareImage = {
                            val imgUri = relatedProject?.resultImageUri ?: message.attachedImageUri
                            viewModel.shareMedia(imgUri, isVideo = false)
                        },
                        onOpenDocument = { uri, type ->
                            viewModel.openDocument(uri, type)
                        },
                        onShareDocument = { uri, name, type ->
                            viewModel.shareDocument(uri, name, type)
                        },
                        onSaveDocument = { uri, name, type ->
                            viewModel.saveDocumentToDownloads(uri, name, type)
                        }
                    )
                }

                // Streaming Execution Status Banner (Displayed seamlessly at bottom of conversation)
                if (currentExecutingSkill != null && currentExecutingSkill?.status == InvocationStatus.EXECUTING) {
                    item {
                        ActiveSkillExecutingBanner(
                            record = currentExecutingSkill,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                } else if (isGenerating) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF161E31), RoundedCornerShape(8.dp))
                                .border(1.dp, AgnesCyan.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = AgnesCyan,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (progressMessage.isNotBlank()) progressMessage else "Dream AI 正在思考并执行调度...",
                                fontSize = 11.sp,
                                color = AgnesCyan,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

        // Quick Prompt Chips (Only shown when conversation is empty OR when user taps the magic wand button)
        AnimatedVisibility(
            visible = (chatMessages.isEmpty() || showQuickPrompts) && !isGenerating
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                if (chatMessages.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "💡 快捷指令与灵感提示词",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = AgnesCyan
                        )
                        IconButton(
                            onClick = { showQuickPrompts = false },
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.Gray,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(quickPrompts) { prompt ->
                        Surface(
                            onClick = {
                                inputText = prompt.substringAfter(" ")
                                showQuickPrompts = false
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF161E31),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CyberCardBorder)
                        ) {
                            Text(
                                text = prompt,
                                fontSize = 10.sp,
                                color = Color(0xFFCBD5E1),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }
        }

        // Modern Integrated Pill Input Bar (Plan A: ChatGPT-style all-in-one capsule)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .clip(RoundedCornerShape(26.dp))
                .border(
                    width = 1.dp,
                    brush = if (inputText.isNotBlank()) Brush.horizontalGradient(listOf(AgnesViolet, AgnesCyan)) else Brush.linearGradient(listOf(CyberCardBorder, CyberCardBorder)),
                    shape = RoundedCornerShape(26.dp)
                )
                .testTag("chat_input_pill_container"),
            color = Color(0xFF111726),
            tonalElevation = 2.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                // Attached Image Micro-Preview (Docked tightly inside the pill if present)
                AnimatedVisibility(visible = selectedImageUri != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 6.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF161E31))
                                .border(1.dp, AgnesCyan, RoundedCornerShape(6.dp))
                        ) {
                            AsyncImage(
                                model = selectedImageUri,
                                contentDescription = "Attached",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "已附加参考图片",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = AgnesCyan
                            )
                            Text(
                                text = "将作为图生图重绘或分镜视频故事底模",
                                fontSize = 8.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                        IconButton(
                            onClick = { selectedImageUri = null },
                            modifier = Modifier.size(22.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remove Image",
                                tint = Color.Gray,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }

                // Core Input Row: [ 📎 Image ] + [ 🪄 Prompts ] + [ BasicTextField ] + [ 🚀 Send ]
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Inset Action 1: Add Image
                    IconButton(
                        onClick = { showImagePicker = true },
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .testTag("attach_image_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = "Attach Image",
                            tint = if (selectedImageUri != null) AgnesCyan else Color(0xFF94A3B8),
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    // Left Inset Action 2: Inspiration / Quick Prompts toggle
                    IconButton(
                        onClick = { showQuickPrompts = !showQuickPrompts },
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .testTag("quick_prompts_toggle")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Quick Prompts",
                            tint = if (showQuickPrompts) AgnesAmber else Color(0xFF64748B),
                            modifier = Modifier.size(17.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    val placeholderHint = when (currentIntentMode) {
                        ChatIntentMode.CHAT -> "探讨交流（高速不排队）..."
                        ChatIntentMode.IMAGE_GEN -> "输入生图描述或参考图变奏..."
                        ChatIntentMode.VIDEO_GEN -> "输入视频构思，自动规划4段分镜..."
                        ChatIntentMode.AUTO -> "向 Dream AI 提问或调度技能..."
                    }

                    // Flexible Center Text Input Field
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 4.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (inputText.isEmpty()) {
                            Text(
                                text = placeholderHint,
                                fontSize = 12.sp,
                                color = Color(0xFF64748B),
                                maxLines = 1
                            )
                        }
                        BasicTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("chat_input_field"),
                            textStyle = TextStyle(
                                fontSize = 13.sp,
                                color = Color.White
                            ),
                            cursorBrush = SolidColor(AgnesCyan),
                            maxLines = 4
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Right Inset Action: Send Pill Button
                    val canSend = (inputText.isNotBlank() || selectedImageUri != null) && !isGenerating
                    IconButton(
                        onClick = {
                            if (canSend) {
                                viewModel.sendUserMessage(inputText, selectedImageUri)
                                inputText = ""
                                selectedImageUri = null
                                showQuickPrompts = false
                            }
                        },
                        enabled = canSend,
                        modifier = Modifier
                            .size(34.dp)
                            .background(
                                brush = if (canSend) {
                                    Brush.linearGradient(listOf(AgnesViolet, AgnesCyan))
                                } else {
                                    Brush.linearGradient(listOf(Color(0xFF1E293B), Color(0xFF1E293B)))
                                },
                                shape = CircleShape
                            )
                            .testTag("send_message_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = if (canSend) Color.White else Color(0xFF475569),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }

    // Left History Navigation Drawer Overlay (Qwen / DeepSeek Style)
    AnimatedVisibility(
        visible = showHistoryDrawer,
        enter = androidx.compose.animation.fadeIn() + slideInHorizontally(initialOffsetX = { -it }),
        exit = androidx.compose.animation.fadeOut() + slideOutHorizontally(targetOffsetX = { -it })
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.65f))
                .clickable { showHistoryDrawer = false }
        ) {
            Column(
                modifier = Modifier
                    .width(280.dp)
                    .fillMaxHeight()
                    .background(Color(0xFF0F172A))
                    .clickable(enabled = false) {}
                    .padding(16.dp)
            ) {
                // Drawer Header
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
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SmartToy,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Dream AI 历史对话",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    IconButton(onClick = { showHistoryDrawer = false }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "关闭",
                            tint = Color(0xFF94A3B8)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // New Chat Action Button
                Surface(
                    onClick = {
                        viewModel.clearChat()
                        showHistoryDrawer = false
                        viewModel.showToast("已开启新对话")
                    },
                    shape = RoundedCornerShape(12.dp),
                    color = AgnesVioletDark,
                    border = androidx.compose.foundation.BorderStroke(1.dp, AgnesViolet),
                    modifier = Modifier.fillMaxWidth()
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
                            text = "新建对话",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "历史创作项目与对话",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF94A3B8)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Project History List
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (projects.isEmpty() && chatMessages.size <= 1) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "暂无历史对话记录",
                                    fontSize = 12.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }
                    }

                    items(projects) { project ->
                        Surface(
                            onClick = {
                                viewModel.selectProject(project)
                                showHistoryDrawer = false
                                if (project.type == com.example.data.model.ProjectType.VIDEO_SCRIPT_AND_STITCH) {
                                    onNavigateToVideo()
                                } else {
                                    onNavigateToImage()
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = CyberCardBg,
                            border = androidx.compose.foundation.BorderStroke(1.dp, CyberCardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (project.type == com.example.data.model.ProjectType.VIDEO_SCRIPT_AND_STITCH) Icons.Default.Movie else Icons.Default.Image,
                                    contentDescription = null,
                                    tint = if (project.type == com.example.data.model.ProjectType.VIDEO_SCRIPT_AND_STITCH) AgnesCyan else AgnesVioletLight,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = project.title,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.White,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = project.statusMessage,
                                        fontSize = 10.sp,
                                        color = Color(0xFF94A3B8),
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Clear All Chat History
                Surface(
                    onClick = {
                        viewModel.clearChat()
                        showHistoryDrawer = false
                    },
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF1E293B),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = null,
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "清空对话历史",
                            fontSize = 12.sp,
                            color = Color(0xFFEF4444)
                        )
                    }
                }
            }
        }
    }
}

    if (showImagePicker) {
        ImagePickerBottomSheet(
            sheetState = sheetState,
            onDismiss = { showImagePicker = false },
            onImageSelected = { uri, prompt ->
                selectedImageUri = uri
                if (prompt != null && inputText.isBlank()) {
                    inputText = prompt
                }
            }
        )
    }

    if (showSkillManagerSheet) {
        AgentSkillManagerSheet(
            skills = skills,
            onToggleSkill = { id, enabled -> viewModel.toggleSkill(id, enabled) },
            onUseSkillTemplate = { template ->
                inputText = template
            },
            onDismiss = { showSkillManagerSheet = false }
        )
    }
}

@Composable
fun ChatMessageItem(
    message: ChatMessage,
    project: com.example.data.model.GenerationProject? = null,
    onOpenVideoStudio: () -> Unit,
    onOpenImageStudio: () -> Unit,
    onSaveImage: () -> Unit = {},
    onShareImage: () -> Unit = {},
    onOpenDocument: (uri: String?, type: String?) -> Unit = { _, _ -> },
    onShareDocument: (uri: String?, name: String?, type: String?) -> Unit = { _, _, _ -> },
    onSaveDocument: (uri: String?, name: String?, type: String?) -> Unit = { _, _, _ -> }
) {
    val isUser = message.sender == "user"

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(AgnesViolet, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.SmartToy,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            color = if (isUser) AgnesVioletDark else CyberCardBg,
            border = androidx.compose.foundation.BorderStroke(1.dp, if (isUser) AgnesViolet else CyberCardBorder),
            modifier = if (isUser) Modifier.widthIn(max = 300.dp) else Modifier.fillMaxWidth(0.92f)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                if (message.actionType == "SKILL_CALL") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF1E1738))
                            .border(1.dp, AgnesViolet, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Extension,
                            contentDescription = null,
                            tint = AgnesCyan,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "智能体技能调度 (Skill Invocation)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = AgnesCyan
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (message.attachedImageUri != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black)
                    ) {
                        AsyncImage(
                            model = message.attachedImageUri,
                            contentDescription = "Image",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                MarkdownText(
                    markdown = message.content,
                    fontSize = 13.sp,
                    textColor = Color.White,
                    lineHeight = 19.sp
                )

                // If this message is linked to a completed image generation project, render result preview!
                if (project?.resultImageUri != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black)
                            .border(1.dp, AgnesViolet, RoundedCornerShape(8.dp))
                            .clickable { onOpenImageStudio() }
                    ) {
                        AsyncImage(
                            model = project.resultImageUri,
                            contentDescription = "Generated Image",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(6.dp)
                                .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "✨ Agnes AI 生成新图 (${project.stylePreset})",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = AgnesCyan
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            onClick = onSaveImage,
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFF161E31)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, tint = AgnesEmerald, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("保存", fontSize = 10.sp, color = AgnesEmerald)
                            }
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            onClick = onShareImage,
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFF161E31)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, tint = AgnesCyan, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("分享", fontSize = 10.sp, color = AgnesCyan)
                            }
                        }
                    }
                }

                // If this message is linked to a completed video generation project, render video entry!
                if (project?.resultVideoUri != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        onClick = onOpenVideoStudio,
                        shape = RoundedCornerShape(8.dp),
                        color = Color.Black,
                        border = androidx.compose.foundation.BorderStroke(1.dp, AgnesCyan)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayCircle,
                                contentDescription = null,
                                tint = AgnesCyan,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "🎬 多段拼接电影已就绪 (${project.totalClips}幕)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "点击前往播放与分镜工作台",
                                    fontSize = 9.sp,
                                    color = AgnesCyan
                                )
                            }
                        }
                    }
                }

                if (message.actionType == "VIDEO_SCRIPT") {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        onClick = onOpenVideoStudio,
                        shape = RoundedCornerShape(8.dp),
                        color = AgnesCyan.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AgnesCyan.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Movie,
                                contentDescription = null,
                                tint = AgnesCyan,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "打开视频流水线工作室",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = AgnesCyan
                            )
                        }
                    }
                } else if (message.actionType == "IMAGE_RESULT") {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        onClick = onOpenImageStudio,
                        shape = RoundedCornerShape(8.dp),
                        color = AgnesViolet.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AgnesViolet.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = null,
                                tint = AgnesVioletLight,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "查看图片变奏工作台",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = AgnesVioletLight
                            )
                        }
                    }
                }

                // Render Interactive Document Card (Word, PDF, Excel)
                if (!message.documentUri.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    AgentDocumentCard(
                        documentName = message.documentName ?: "已导出文档",
                        documentType = message.documentType ?: "WORD",
                        documentSize = message.documentSize,
                        documentUri = message.documentUri,
                        onOpenDocument = { onOpenDocument(message.documentUri, message.documentType) },
                        onShareDocument = { onShareDocument(message.documentUri, message.documentName, message.documentType) },
                        onSaveToDownloads = { onSaveDocument(message.documentUri, message.documentName, message.documentType) }
                    )
                }
            }
        }
    }
}

