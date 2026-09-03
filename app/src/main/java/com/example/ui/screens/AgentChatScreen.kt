package com.example.ui.screens

import android.net.Uri
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Videocam
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
import com.example.ui.components.ImagePickerBottomSheet
import com.example.ui.components.RateLimitBanner
import com.example.ui.theme.AgnesCyan
import com.example.ui.theme.AgnesEmerald
import com.example.ui.theme.AgnesViolet
import com.example.ui.theme.AgnesVioletDark
import com.example.ui.theme.AgnesVioletLight
import com.example.ui.theme.CyberCardBg
import com.example.ui.theme.CyberCardBorder
import com.example.ui.viewmodel.AgnesViewModel
import kotlinx.coroutines.launch

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

    var inputText by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<String?>(null) }
    var showImagePicker by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Auto-scroll on new message
    LaunchedEffect(chatMessages.size) {
        if (chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(chatMessages.size - 1)
        }
    }

    val quickPrompts = listOf(
        "🎬 根据此图片生成4幕电影短片并拼接",
        "🎨 将图片重绘为赛博朋克霓虹风格",
        "🚀 构思星际科幻分镜并生成完整视频",
        "✨ 动漫二次元艺术变奏重绘"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0A0D14))
            .testTag("agent_chat_screen")
    ) {
        // Top Rate Limit Guard
        RateLimitBanner(
            rateLimitState = rateLimitState,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )

        // Chat Header with Clear action
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
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
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Agnes 智能体助手",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "多模态图像重绘 • 分镜规划 • 视频拼接",
                        fontSize = 10.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            }

            IconButton(
                onClick = { viewModel.clearChat() },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Clear Chat",
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // Chat Messages List
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
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
                    }
                )
            }

            if (isGenerating) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = AgnesCyan,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (progressMessage.isNotBlank()) progressMessage else "Agnes 智能体正在调度执行...",
                            fontSize = 11.sp,
                            color = AgnesCyan,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Quick Prompt Chips
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(quickPrompts) { prompt ->
                Surface(
                    onClick = {
                        inputText = prompt.substringAfter(" ")
                    },
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF161E31),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyberCardBorder)
                ) {
                    Text(
                        text = prompt,
                        fontSize = 11.sp,
                        color = Color(0xFFCBD5E1),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
        }

        // Attached Image Preview if selected
        AnimatedVisibility(visible = selectedImageUri != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
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
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = AgnesCyan
                    )
                    Text(
                        text = "可用于图片重绘变奏或生成多段分镜短片",
                        fontSize = 9.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
                IconButton(
                    onClick = { selectedImageUri = null },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove",
                        tint = Color.Gray,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        // Input Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { showImagePicker = true },
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(0xFF161E31), CircleShape)
                    .border(1.dp, CyberCardBorder, CircleShape)
                    .testTag("attach_image_button")
            ) {
                Icon(
                    imageVector = Icons.Default.AddPhotoAlternate,
                    contentDescription = "Attach Image",
                    tint = AgnesCyan,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier
                    .weight(1f)
                    .testTag("chat_input_field"),
                placeholder = {
                    Text("向 Agnes 提问、输入构思或生成指令...", fontSize = 12.sp, color = Color(0xFF64748B))
                },
                maxLines = 3,
                shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AgnesViolet,
                    unfocusedBorderColor = CyberCardBorder,
                    focusedContainerColor = CyberCardBg,
                    unfocusedContainerColor = CyberCardBg,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            Spacer(modifier = Modifier.width(6.dp))

            IconButton(
                onClick = {
                    if (inputText.isNotBlank() || selectedImageUri != null) {
                        viewModel.sendUserMessage(inputText, selectedImageUri)
                        inputText = ""
                        selectedImageUri = null
                    }
                },
                enabled = (inputText.isNotBlank() || selectedImageUri != null) && !isGenerating,
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        if (inputText.isNotBlank() || selectedImageUri != null) AgnesViolet else Color(0xFF334155),
                        CircleShape
                    )
                    .testTag("send_message_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
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
}

@Composable
fun ChatMessageItem(
    message: ChatMessage,
    project: com.example.data.model.GenerationProject? = null,
    onOpenVideoStudio: () -> Unit,
    onOpenImageStudio: () -> Unit
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
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
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

                Text(
                    text = message.content,
                    fontSize = 13.sp,
                    color = Color.White,
                    lineHeight = 18.sp
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
            }
        }
    }
}
