package com.example.ui.screens

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.example.ui.theme.CyberCardBg
import com.example.ui.theme.CyberCardBorder
import com.example.ui.viewmodel.AgnesViewModel

@Composable
fun SettingsScreen(
    viewModel: AgnesViewModel,
    modifier: Modifier = Modifier
) {
    val currentConfig by viewModel.config.collectAsState()
    val rateLimitState by viewModel.rateLimitState.collectAsState()

    var apiKey by remember(currentConfig) { mutableStateOf(currentConfig.apiKey) }
    var endpointUrl by remember(currentConfig) { mutableStateOf(currentConfig.endpointUrl) }
    var modelName by remember(currentConfig) { mutableStateOf(currentConfig.modelName) }
    var videoModelName by remember(currentConfig) { mutableStateOf(currentConfig.videoModelName) }
    var rateLimitSeconds by remember(currentConfig) { mutableStateOf(currentConfig.rateLimitSeconds.toString()) }
    var autoStitch by remember(currentConfig) { mutableStateOf(currentConfig.autoStitchVideos) }

    var isPasswordVisible by remember { mutableStateOf(false) }
    var testResultText by remember { mutableStateOf<String?>(null) }
    var isTesting by remember { mutableStateOf(false) }
    var isTestSuccess by remember { mutableStateOf(false) }

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
                    text = "Agnes API 密钥与限速调度配置",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "配置官方/第三方 Agnes 访问参数与限速安全保护",
                    fontSize = 10.sp,
                    color = Color(0xFF94A3B8)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Card 1: API Credentials
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
                        text = "Agnes API 密钥 (API Key)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("api_key_input"),
                    placeholder = { Text("在此粘贴你的 Agnes API Key (例如 agnes_sk_...)", color = Color(0xFF64748B), fontSize = 12.sp) },
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

                Text(
                    text = "Agnes API 基础端点 (Base URL):",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFCBD5E1)
                )

                Spacer(modifier = Modifier.height(4.dp))

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
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Card 2: Rate Limit Policy Configuration
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
                        text = "访问限速策略 (Strict Rate Limiting)",
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
                        text = "💡 重要限速保护：Agnes API 限制每分钟最多调用 1 次。APP 已内置精确到秒级的全自动调度队列，每次调用成功后将自动锁定 60 秒冷却，防止被服务端 429 封禁或报错。",
                        fontSize = 11.sp,
                        color = Color(0xFFFDE68A),
                        lineHeight = 15.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "调用冷却时间间隔 (秒):",
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

        // Card 3: Model Parameters & Switches
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .border(1.dp, CyberCardBorder, RoundedCornerShape(10.dp)),
            color = CyberCardBg,
            tonalElevation = 2.dp
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "模型与自动化开关",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "多段视频自动拼接",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                        Text(
                            text = "所有分镜片段生成完毕后，自动合成无缝长视频",
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

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "图像重绘模型名称:",
                    fontSize = 11.sp,
                    color = Color(0xFFCBD5E1)
                )
                Spacer(modifier = Modifier.height(3.dp))
                OutlinedTextField(
                    value = modelName,
                    onValueChange = { modelName = it },
                    modifier = Modifier.fillMaxWidth(),
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
                    text = "视频生成模型名称:",
                    fontSize = 11.sp,
                    color = Color(0xFFCBD5E1)
                )
                Spacer(modifier = Modifier.height(3.dp))
                OutlinedTextField(
                    value = videoModelName,
                    onValueChange = { videoModelName = it },
                    modifier = Modifier.fillMaxWidth(),
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
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Test API Connection Button & Feedback
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
                    text = "保存配置",
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
