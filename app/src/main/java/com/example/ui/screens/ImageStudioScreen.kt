package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.GenerationProject
import com.example.data.model.ProjectType
import com.example.ui.components.ImagePickerBottomSheet
import com.example.ui.components.RateLimitBanner
import com.example.ui.theme.AgnesCyan
import com.example.ui.theme.AgnesEmerald
import com.example.ui.theme.AgnesViolet
import com.example.ui.theme.CyberCardBg
import com.example.ui.theme.CyberCardBorder
import com.example.ui.viewmodel.AgnesViewModel

val ART_STYLES = listOf(
    "Cinematic 3D" to "电影级 3D",
    "Cyberpunk" to "赛博朋克",
    "Anime Fantasy" to "二次元奇幻",
    "Futuristic Sci-Fi" to "科幻机甲",
    "Realistic Photography" to "真实摄影",
    "Oil Painting" to "油画艺术"
)

val ASPECT_RATIOS = listOf("16:9", "1:1", "9:16", "4:3")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageStudioScreen(
    viewModel: AgnesViewModel,
    onConvertToVideo: (String, String) -> Unit, // (imageUri, prompt)
    modifier: Modifier = Modifier
) {
    val rateLimitState by viewModel.rateLimitState.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val progressMessage by viewModel.progressMessage.collectAsState()
    val selectedProject by viewModel.selectedProject.collectAsState()

    var promptText by remember { mutableStateOf("夜幕中霓虹闪烁的未来都市，光子悬浮飞车穿梭其中，雨夜地面光影倒影，8k超清") }
    var selectedImageUri by remember { mutableStateOf<String?>(null) }
    var selectedStyle by remember { mutableStateOf("Cinematic 3D") }
    var selectedRatio by remember { mutableStateOf("16:9") }
    var comparisonSplit by remember { mutableFloatStateOf(0.5f) }
    var showImagePicker by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val lastImageProject = if (selectedProject?.type == ProjectType.IMAGE_TO_IMAGE) selectedProject else null

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0A0D14))
            .verticalScroll(scrollState)
            .padding(12.dp)
            .testTag("image_studio_screen")
    ) {
        // Rate Limit Monitor
        RateLimitBanner(rateLimitState = rateLimitState)

        Spacer(modifier = Modifier.height(12.dp))

        // Title Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        Brush.linearGradient(listOf(AgnesViolet, Color(0xFFEC4899))),
                        RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Palette,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "AI 图像重绘变奏工作台",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "基于 Agnes API 图生图算法与多风格重塑",
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Source Image Selection Area
        Surface(
            onClick = { showImagePicker = true },
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, if (selectedImageUri != null) AgnesCyan else CyberCardBorder, RoundedCornerShape(12.dp))
                .testTag("upload_image_card"),
            color = CyberCardBg,
            tonalElevation = 2.dp
        ) {
            if (selectedImageUri != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                ) {
                    AsyncImage(
                        model = selectedImageUri,
                        contentDescription = "Source Image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Black.copy(alpha = 0.5f), Color.Transparent, Color.Black.copy(alpha = 0.7f))
                                )
                            )
                    )
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                    ) {
                        IconButton(
                            onClick = { selectedImageUri = null },
                            modifier = Modifier
                                .size(28.dp)
                                .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                    Text(
                        text = "已选定参考图片 (点击可更换)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(10.dp)
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(AgnesCyan.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = null,
                            tint = AgnesCyan,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "点击上传参考图 / 选取精选样张",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Agnes AI 将根据参考图片进行构图重塑与变奏",
                        fontSize = 10.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Prompt Input
        Text(
            text = "重绘提示词描述:",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFFCBD5E1)
        )

        Spacer(modifier = Modifier.height(4.dp))

        OutlinedTextField(
            value = promptText,
            onValueChange = { promptText = it },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("image_prompt_input"),
            placeholder = { Text("输入你想生成的画面细节、光影、风格...", color = Color(0xFF64748B), fontSize = 12.sp) },
            minLines = 2,
            maxLines = 4,
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AgnesViolet,
                unfocusedBorderColor = CyberCardBorder,
                focusedContainerColor = CyberCardBg,
                unfocusedContainerColor = CyberCardBg,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Style selector
        Text(
            text = "艺术风格选择:",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFFCBD5E1)
        )

        Spacer(modifier = Modifier.height(6.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(ART_STYLES) { (key, name) ->
                val isSelected = selectedStyle == key
                Surface(
                    onClick = { selectedStyle = key },
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) AgnesViolet else Color(0xFF161E31),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isSelected) AgnesCyan else CyberCardBorder
                    ),
                    modifier = Modifier.testTag("style_$key")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                        }
                        Text(
                            text = name,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = Color.White
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Aspect Ratio Selector
        Text(
            text = "画面比例:",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFFCBD5E1)
        )

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ASPECT_RATIOS.forEach { ratio ->
                val isSelected = selectedRatio == ratio
                Surface(
                    onClick = { selectedRatio = ratio },
                    shape = RoundedCornerShape(6.dp),
                    color = if (isSelected) AgnesCyan.copy(alpha = 0.2f) else Color(0xFF161E31),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isSelected) AgnesCyan else CyberCardBorder
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = ratio,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) AgnesCyan else Color(0xFF94A3B8),
                        modifier = Modifier.padding(vertical = 6.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Generate Button
        Button(
            onClick = {
                viewModel.generateImage(
                    prompt = promptText,
                    stylePreset = selectedStyle,
                    aspectRatio = selectedRatio,
                    sourceImageUri = selectedImageUri
                )
            },
            enabled = !isGenerating,
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .testTag("generate_image_button"),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = AgnesViolet
            )
        ) {
            if (isGenerating) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (progressMessage.isNotBlank()) progressMessage else "排队调用 Agnes API 中...",
                    fontSize = 13.sp,
                    color = Color.White
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = AgnesCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (rateLimitState.isCoolingDown) "加入生成队列 (自动等待 ${rateLimitState.remainingSeconds}s 冷却)" else "立即生成 AI 新图像",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        // Result Card if generated
        if (lastImageProject?.resultImageUri != null) {
            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, AgnesViolet.copy(alpha = 0.5f), RoundedCornerShape(14.dp)),
                color = CyberCardBg,
                tonalElevation = 4.dp
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "生成结果 (Agnes AI)",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Box(
                            modifier = Modifier
                                .background(AgnesEmerald.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "重绘成功",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = AgnesEmerald
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Image Display
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(if (selectedRatio == "16:9") 16f / 9f else 1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black)
                    ) {
                        AsyncImage(
                            model = lastImageProject.resultImageUri,
                            contentDescription = "Generated Result",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Next step: Convert to video storyboard
                    Button(
                        onClick = {
                            onConvertToVideo(lastImageProject.resultImageUri ?: "", lastImageProject.prompt)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("convert_to_video_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AgnesCyan
                        )
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Movie,
                                contentDescription = null,
                                tint = Color(0xFF0F172A),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "以该图为首帧，自动拆解分镜并生成多段视频",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
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
                if (prompt != null && promptText.isBlank()) {
                    promptText = prompt
                }
            }
        )
    }
}
