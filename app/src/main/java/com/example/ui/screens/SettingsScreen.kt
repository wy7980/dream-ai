package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AgnesApiConfig
import com.example.ui.components.RateLimitBanner
import com.example.ui.theme.AgnesAmber
import com.example.ui.theme.AgnesCyan
import com.example.ui.theme.AgnesEmerald
import com.example.ui.theme.AgnesRose
import com.example.ui.theme.AgnesViolet
import com.example.ui.theme.AgnesVioletLight
import com.example.ui.theme.CyberCardBg
import com.example.ui.theme.CyberCardBorder
import com.example.ui.viewmodel.AgnesViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    viewModel: AgnesViewModel,
    modifier: Modifier = Modifier
) {
    val currentConfig by viewModel.config.collectAsState()
    val rateLimitState by viewModel.rateLimitState.collectAsState()
    val availableModels by viewModel.availableModels.collectAsState()
    val isFetchingModels by viewModel.isFetchingModels.collectAsState()

    var apiKey by remember(currentConfig) { mutableStateOf(currentConfig.apiKey) }
    var endpointUrl by remember(currentConfig) { mutableStateOf(currentConfig.endpointUrl) }
    var chatModelName by remember(currentConfig) { mutableStateOf(currentConfig.chatModelName) }
    var modelName by remember(currentConfig) { mutableStateOf(currentConfig.modelName) }
    var videoModelName by remember(currentConfig) { mutableStateOf(currentConfig.videoModelName) }
    var rateLimitSeconds by remember(currentConfig) { mutableStateOf(currentConfig.rateLimitSeconds.toString()) }
    var autoStitch by remember(currentConfig) { mutableStateOf(currentConfig.autoStitchVideos) }

    var isPasswordVisible by remember { mutableStateOf(false) }
    var testResultText by remember { mutableStateOf<String?>(null) }
    var isTesting by remember { mutableStateOf(false) }
    var isTestSuccess by remember { mutableStateOf(false) }
    var fetchStatusText by remember { mutableStateOf<String?>(null) }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0A0D14))
            .verticalScroll(scrollState)
            .padding(12.dp)
            .testTag("settings_screen")
    ) {
        // Rate limit banner
        RateLimitBanner(rateLimitState = rateLimitState)

        Spacer(modifier = Modifier.height(10.dp))

        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .background(Color(0xFF161E31), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    tint = AgnesViolet,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "Agnes API 与多模型分流配置",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "支持对话、生图、生视频模型独立选择与速率分流控制",
                    fontSize = 10.sp,
                    color = Color(0xFF94A3B8)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Card 1: API Endpoint & Key
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .border(1.dp, CyberCardBorder, RoundedCornerShape(10.dp)),
            color = CyberCardBg,
            tonalElevation = 2.dp
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = null,
                        tint = AgnesCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Agnes API 密钥与服务地址",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "服务端点 (Base URL):",
                    fontSize = 11.sp,
                    color = Color(0xFFCBD5E1)
                )
                Spacer(modifier = Modifier.height(3.dp))
                OutlinedTextField(
                    value = endpointUrl,
                    onValueChange = { endpointUrl = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("endpoint_input"),
                    placeholder = { Text("https://api.agnes.ai/v1", color = Color(0xFF64748B), fontSize = 12.sp) },
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AgnesViolet,
                        unfocusedBorderColor = CyberCardBorder,
                        focusedContainerColor = Color(0xFF0E1422),
                        unfocusedContainerColor = Color(0xFF0E1422),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "API Key 密钥:",
                    fontSize = 11.sp,
                    color = Color(0xFFCBD5E1)
                )
                Spacer(modifier = Modifier.height(3.dp))
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("api_key_input"),
                    placeholder = { Text("在此粘贴你的 Agnes/OpenAI 兼容 API Key", color = Color(0xFF64748B), fontSize = 12.sp) },
                    visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                            Icon(
                                imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Toggle Visibility",
                                tint = Color.Gray,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AgnesCyan,
                        unfocusedBorderColor = CyberCardBorder,
                        focusedContainerColor = Color(0xFF0E1422),
                        unfocusedContainerColor = Color(0xFF0E1422),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Fetch Remote Models Button
                Button(
                    onClick = {
                        val tempCfg = AgnesApiConfig(
                            apiKey = apiKey,
                            endpointUrl = endpointUrl,
                            chatModelName = chatModelName,
                            modelName = modelName,
                            videoModelName = videoModelName
                        )
                        viewModel.updateConfig(tempCfg)
                        viewModel.fetchModelsFromEndpoint { success, count, msg ->
                            fetchStatusText = if (success) "✅ 已从服务地址拉取到 $count 个可用模型" else "ℹ️ $msg"
                        }
                    },
                    enabled = !isFetchingModels,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                        .testTag("fetch_models_button"),
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF161E31)
                    )
                ) {
                    if (isFetchingModels) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            color = AgnesCyan,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("正在从端点拉取模型列表...", fontSize = 11.sp, color = AgnesCyan)
                    } else {
                        Icon(
                            imageVector = Icons.Default.CloudDownload,
                            contentDescription = null,
                            tint = AgnesCyan,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("根据模型地址自动拉取模型列表 (${availableModels.size} 个可用)", fontSize = 11.sp, color = AgnesCyan, fontWeight = FontWeight.SemiBold)
                    }
                }

                if (fetchStatusText != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = fetchStatusText ?: "",
                        fontSize = 10.sp,
                        color = AgnesCyan
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Card 2: Model Configuration (Chat, Image, Video)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .border(1.dp, AgnesViolet.copy(alpha = 0.4f), RoundedCornerShape(10.dp)),
            color = CyberCardBg,
            tonalElevation = 2.dp
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = AgnesVioletLight,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "专属模型选择与自动匹配",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "支持从列表选择或直接手动输入。若列表中未匹配到，将直接使用您手动输入的模型名称。",
                    fontSize = 10.sp,
                    color = Color(0xFF94A3B8),
                    lineHeight = 14.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 1. Chat Model
                ModelInputFieldWithSuggestions(
                    label = "💬 对话模型 (Chat Model)",
                    badge = "⚡ 高速率畅聊",
                    badgeColor = AgnesEmerald,
                    tooltip = "对话模型拥有极高吞吐速率，日常闲聊、脚本策划、问答不占用 1 分钟生图/生视频冷却排队。",
                    currentValue = chatModelName,
                    onValueChange = { chatModelName = it },
                    availableList = availableModels,
                    filterKeywords = listOf("chat", "gpt", "claude", "deepseek", "qwen", "gemini", "agent"),
                    placeholder = "agnes-chat-pro",
                    testTag = "chat_model_input"
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 2. Image Model
                ModelInputFieldWithSuggestions(
                    label = "🎨 图像重绘/生成模型 (Image Model)",
                    badge = "⏱️ 1分钟限速保护",
                    badgeColor = AgnesAmber,
                    tooltip = "只在执行生图或图片变奏重绘时调用，严格受 1 分钟限速保护。",
                    currentValue = modelName,
                    onValueChange = { modelName = it },
                    availableList = availableModels,
                    filterKeywords = listOf("vision", "sd", "flux", "dall", "midjourney", "diffusion", "image"),
                    placeholder = "agnes-vision-ultra",
                    testTag = "image_model_input"
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 3. Video Model
                ModelInputFieldWithSuggestions(
                    label = "🎬 视频生成模型 (Video Model)",
                    badge = "⏱️ 1分钟限速保护",
                    badgeColor = AgnesAmber,
                    tooltip = "只在生成多段视频片段时调用，严格每分钟生成 1 段并排队拼接。",
                    currentValue = videoModelName,
                    onValueChange = { videoModelName = it },
                    availableList = availableModels,
                    filterKeywords = listOf("video", "kling", "luma", "sora", "runway", "cog"),
                    placeholder = "agnes-video-gen-v2",
                    testTag = "video_model_input"
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Card 3: Rate Limit Policy Configuration
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .border(1.dp, AgnesAmber.copy(alpha = 0.3f), RoundedCornerShape(10.dp)),
            color = CyberCardBg,
            tonalElevation = 2.dp
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        tint = AgnesAmber,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "生图 & 生视频限速保护策略",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AgnesAmber.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                        .padding(8.dp)
                ) {
                    Text(
                        text = "💡 策略说明：只有生图和生视频模型会触发 1 分钟严格限速冷却；对话模型走高速通道实时响应。APP 内置秒级倒计时调度器，保证多段生图和生视频稳定执行不超频。",
                        fontSize = 11.sp,
                        color = Color(0xFFFDE68A),
                        lineHeight = 15.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "生图与生视频冷却周期 (秒):",
                    fontSize = 11.sp,
                    color = Color(0xFFCBD5E1)
                )

                Spacer(modifier = Modifier.height(4.dp))

                OutlinedTextField(
                    value = rateLimitSeconds,
                    onValueChange = { rateLimitSeconds = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("rate_limit_input"),
                    placeholder = { Text("60", color = Color(0xFF64748B), fontSize = 12.sp) },
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AgnesAmber,
                        unfocusedBorderColor = CyberCardBorder,
                        focusedContainerColor = Color(0xFF0E1422),
                        unfocusedContainerColor = Color(0xFF0E1422),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Card 4: Multi-Video Stitching Switch
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .border(1.dp, CyberCardBorder, RoundedCornerShape(10.dp)),
            color = CyberCardBg,
            tonalElevation = 2.dp
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "多段分镜视频自动拼接",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                        Text(
                            text = "生成全部电影片段后，自动合成无缝长视频",
                            fontSize = 10.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                    Switch(
                        checked = autoStitch,
                        onCheckedChange = { autoStitch = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = AgnesCyan
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Actions: Test Connection & Save Config
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    isTesting = true
                    testResultText = null
                    val testConfig = AgnesApiConfig(
                        apiKey = apiKey,
                        endpointUrl = endpointUrl,
                        chatModelName = chatModelName,
                        modelName = modelName,
                        videoModelName = videoModelName,
                        rateLimitSeconds = rateLimitSeconds.toIntOrNull() ?: 60,
                        autoStitchVideos = autoStitch
                    )
                    viewModel.updateConfig(testConfig)
                    viewModel.testApiConnection { success, msg ->
                        isTesting = false
                        isTestSuccess = success
                        testResultText = msg
                    }
                },
                enabled = !isTesting,
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp)
                    .testTag("test_api_button"),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF161E31)
                )
            ) {
                if (isTesting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = AgnesCyan,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.CloudSync,
                        contentDescription = null,
                        tint = AgnesCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "测试 API 连通性",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AgnesCyan
                    )
                }
            }

            Button(
                onClick = {
                    val updated = AgnesApiConfig(
                        apiKey = apiKey,
                        endpointUrl = endpointUrl,
                        chatModelName = chatModelName,
                        modelName = modelName,
                        videoModelName = videoModelName,
                        rateLimitSeconds = rateLimitSeconds.toIntOrNull() ?: 60,
                        autoStitchVideos = autoStitch
                    )
                    viewModel.updateConfig(updated)
                },
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp)
                    .testTag("save_config_button"),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AgnesViolet
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Save,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "保存所有配置",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        // Test Result Card
        if (testResultText != null) {
            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(if (isTestSuccess) AgnesEmerald.copy(alpha = 0.15f) else AgnesRose.copy(alpha = 0.15f))
                .border(
                    1.dp,
                    if (isTestSuccess) AgnesEmerald.copy(alpha = 0.5f) else AgnesRose.copy(alpha = 0.5f),
                    RoundedCornerShape(8.dp)
                )
                .padding(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isTestSuccess) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                        contentDescription = null,
                        tint = if (isTestSuccess) AgnesEmerald else AgnesRose,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = testResultText ?: "",
                        fontSize = 11.sp,
                        color = if (isTestSuccess) AgnesEmerald else AgnesRose,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ModelInputFieldWithSuggestions(
    label: String,
    badge: String,
    badgeColor: Color,
    tooltip: String,
    currentValue: String,
    onValueChange: (String) -> Unit,
    availableList: List<String>,
    filterKeywords: List<String>,
    placeholder: String,
    testTag: String
) {
    var expanded by remember { mutableStateOf(false) }

    // Matching suggestions based on user input
    val matchedSuggestions = remember(currentValue, availableList) {
        if (currentValue.isBlank()) {
            availableList.filter { model -> filterKeywords.any { k -> model.contains(k, ignoreCase = true) } }
                .ifEmpty { availableList.take(6) }
        } else {
            availableList.filter { it.contains(currentValue.trim(), ignoreCase = true) }
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFCBD5E1)
            )
            Box(
                modifier = Modifier
                    .background(badgeColor.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                    .border(0.5.dp, badgeColor.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = badge,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = badgeColor
                )
            }
        }

        Text(
            text = tooltip,
            fontSize = 9.sp,
            color = Color(0xFF64748B),
            lineHeight = 12.sp,
            modifier = Modifier.padding(top = 2.dp, bottom = 4.dp)
        )

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it }
        ) {
            OutlinedTextField(
                value = currentValue,
                onValueChange = {
                    onValueChange(it)
                    expanded = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(MenuAnchorType.PrimaryEditable, enabled = true)
                    .testTag(testTag),
                placeholder = { Text(placeholder, color = Color(0xFF64748B), fontSize = 12.sp) },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AgnesViolet,
                    unfocusedBorderColor = CyberCardBorder,
                    focusedContainerColor = Color(0xFF0E1422),
                    unfocusedContainerColor = Color(0xFF0E1422),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            if (matchedSuggestions.isNotEmpty()) {
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.background(Color(0xFF161E31))
                ) {
                    matchedSuggestions.take(8).forEach { suggestion ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = suggestion,
                                    fontSize = 12.sp,
                                    color = if (suggestion.equals(currentValue, ignoreCase = true)) AgnesCyan else Color.White
                                )
                            },
                            onClick = {
                                onValueChange(suggestion)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }

        // Quick Suggestion Chips below input
        if (matchedSuggestions.isNotEmpty() && matchedSuggestions.size <= 5) {
            Spacer(modifier = Modifier.height(4.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                matchedSuggestions.take(4).forEach { sug ->
                    Surface(
                        onClick = { onValueChange(sug) },
                        shape = RoundedCornerShape(4.dp),
                        color = if (sug.equals(currentValue, ignoreCase = true)) AgnesViolet.copy(alpha = 0.3f) else Color(0xFF161E31),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, if (sug.equals(currentValue, ignoreCase = true)) AgnesViolet else CyberCardBorder)
                    ) {
                        Text(
                            text = sug,
                            fontSize = 9.sp,
                            color = if (sug.equals(currentValue, ignoreCase = true)) AgnesVioletLight else Color(0xFF94A3B8),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

