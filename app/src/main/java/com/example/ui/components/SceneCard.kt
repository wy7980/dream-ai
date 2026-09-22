package com.example.ui.components

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import java.io.File
import com.example.data.model.GenerationStatus
import com.example.data.model.SceneClip
import com.example.ui.theme.*
import com.example.ui.theme.AgnesAmber
import com.example.ui.theme.AgnesCyan
import com.example.ui.theme.AgnesEmerald
import com.example.ui.theme.AgnesRose
import com.example.ui.theme.AgnesViolet
import com.example.ui.theme.AppCardBg
import com.example.ui.theme.AppCardBorder
import com.example.ui.theme.AppInputBg
import com.example.ui.theme.AppSubtleBg
import com.example.ui.theme.AppTextPrimary
import com.example.ui.theme.AppTextSecondary

@Composable
fun SceneCard(
    clip: SceneClip,
    onClick: () -> Unit = {},
    isRerunning: Boolean = false,
    onRerun: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    onSavePrompt: ((title: String, visualPrompt: String, cameraMovement: String, narration: String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var showEditDialog by remember { mutableStateOf(false) }
    // A planned-but-unrendered scene: no video request has been spent yet, so it can still be
    // freely edited or removed before phase 2 runs.
    val isDraft = clip.isDraft && clip.status != GenerationStatus.COMPLETED
    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(
                1.dp,
                if (clip.status == GenerationStatus.COMPLETED) AgnesEmerald.copy(alpha = 0.5f)
                else if (clip.status == GenerationStatus.GENERATING_CLIPS) AgnesCyan
                else if (isDraft) AgnesViolet.copy(alpha = 0.5f)
                else AppCardBorder,
                RoundedCornerShape(12.dp)
            )
            .testTag("scene_card_${clip.sceneNumber}"),
        color = AppCardBg,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            // Header Row: Scene Number & Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(AgnesViolet, RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "分镜 0${clip.sceneNumber}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = clip.sceneTitle,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AppTextPrimary
                    )
                }

                // Status Badge
                when (clip.status) {
                    GenerationStatus.COMPLETED -> {
                        Box(
                            modifier = Modifier
                                .background(AgnesEmerald.copy(alpha = 0.15f), CircleShape)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = AgnesEmerald,
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "就绪",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AgnesEmerald
                                )
                            }
                        }
                    }
                    GenerationStatus.GENERATING_CLIPS -> {
                        Box(
                            modifier = Modifier
                                .background(AgnesCyan.copy(alpha = 0.15f), CircleShape)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(9.dp),
                                    strokeWidth = 1.5.dp,
                                    color = AgnesCyan
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "生成中",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AgnesCyan
                                )
                            }
                        }
                    }
                    GenerationStatus.WAITING_RATE_LIMIT -> {
                        Box(
                            modifier = Modifier
                                .background(AgnesAmber.copy(alpha = 0.15f), CircleShape)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.HourglassBottom,
                                    contentDescription = null,
                                    tint = AgnesAmber,
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "限速排队",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AgnesAmber
                                )
                            }
                        }
                    }
                    GenerationStatus.FAILED -> {
                        Text(
                            text = "生成异常",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = AgnesRose
                        )
                    }
                    GenerationStatus.AWAITING_REVIEW -> {
                        Box(
                            modifier = Modifier
                                .background(AgnesViolet.copy(alpha = 0.15f), CircleShape)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = null,
                                    tint = AgnesVioletLight,
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "待确认",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AgnesVioletLight
                                )
                            }
                        }
                    }
                    else -> {
                        if (isDraft) {
                            Box(
                                modifier = Modifier
                                    .background(AgnesViolet.copy(alpha = 0.15f), CircleShape)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "待生成",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AgnesVioletLight
                                )
                            }
                        } else {
                            Text(
                                text = "等待调度",
                                fontSize = 9.sp,
                                color = AppTextSecondary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Body: Video / Frame Preview & Details
            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Video Frame Thumbnail
                Box(
                    modifier = Modifier
                        .size(width = 96.dp, height = 58.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(AppSubtleBg),
                    contentAlignment = Alignment.Center
                ) {
                    if (clip.status == GenerationStatus.COMPLETED && !clip.videoUrl.isNullOrBlank()) {
                        val modelPath = clip.videoUrl!!
                        val imageModel = if (modelPath.startsWith("/")) File(modelPath) else modelPath
                        AsyncImage(
                            model = imageModel,
                            contentDescription = clip.sceneTitle,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        Icon(
                            imageVector = Icons.Default.PlayCircleFilled,
                            contentDescription = "Play",
                            tint = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.size(20.dp)
                        )
                    } else if (clip.status == GenerationStatus.GENERATING_CLIPS) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = AgnesCyan
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "轮询生成中",
                                fontSize = 8.sp,
                                color = AgnesCyan,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Videocam,
                                contentDescription = null,
                                tint = AgnesViolet.copy(alpha = 0.6f),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "${clip.durationSeconds}s",
                                fontSize = 9.sp,
                                color = AppTextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Scene Prompts & Camera Motion
                Column(modifier = Modifier.weight(1f)) {
                    // Camera Pill
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = AgnesCyan,
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = clip.cameraMovement,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = AgnesCyan,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(3.dp))

                    // Narration Script
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = null,
                            tint = AppTextSecondary,
                            modifier = Modifier.size(10.dp).padding(top = 2.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = clip.narration,
                            fontSize = 10.sp,
                            color = AppTextSecondary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 14.sp
                        )
                    }

                    if (!clip.taskId.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(3.dp))
                        Box(
                            modifier = Modifier
                                .background(AgnesViolet.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                .border(0.5.dp, AgnesViolet.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "🆔 Task ID: ${clip.taskId}",
                                fontSize = 9.sp,
                                color = AgnesVioletLight,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    if (!clip.statusMessage.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "💬 Status: ${clip.statusMessage}",
                            fontSize = 9.sp,
                            color = if (clip.status == GenerationStatus.FAILED) AgnesRose else AgnesCyan,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 12.sp
                        )
                    }
                }
            }

            // Per-scene actions: edit the storyboard script, delete a not-yet-rendered scene, and
            // re-run just this clip. Editing is always available (even while rendering); re-run is
            // disabled only while this very clip is actively generating.
            if (onRerun != null || onSavePrompt != null || onDelete != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onSavePrompt != null) {
                        Surface(
                            onClick = { showEditDialog = true },
                            shape = RoundedCornerShape(8.dp),
                            color = AgnesViolet.copy(alpha = 0.12f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, AgnesViolet.copy(alpha = 0.5f)),
                            modifier = Modifier.testTag("edit_scene_${clip.sceneNumber}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = null,
                                    tint = AgnesVioletLight,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "编辑提示词",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = AgnesVioletLight
                                )
                            }
                        }
                        if (onRerun != null) {
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                    }

                    if (onDelete != null) {
                        val canDelete = !isRerunning && clip.status != GenerationStatus.GENERATING_CLIPS
                        Surface(
                            onClick = { if (canDelete) onDelete() },
                            enabled = canDelete,
                            shape = RoundedCornerShape(8.dp),
                            color = AgnesRose.copy(alpha = 0.12f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, AgnesRose.copy(alpha = 0.5f)),
                            modifier = Modifier.testTag("delete_scene_${clip.sceneNumber}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = null,
                                    tint = AgnesRose,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "删除",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = AgnesRose
                                )
                            }
                        }
                        if (onRerun != null) {
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                    }

                    if (onRerun != null) {
                    val isGenerating = clip.status == GenerationStatus.GENERATING_CLIPS
                    val enabled = !isRerunning && !isGenerating
                    val tint = when {
                        isRerunning || isGenerating -> AgnesAmber
                        clip.status == GenerationStatus.FAILED -> AgnesRose
                        else -> AgnesCyan
                    }
                    Surface(
                        onClick = { if (enabled) onRerun() },
                        enabled = enabled,
                        shape = RoundedCornerShape(8.dp),
                        color = tint.copy(alpha = 0.12f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, tint.copy(alpha = 0.5f)),
                        modifier = Modifier.testTag("rerun_scene_${clip.sceneNumber}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isRerunning) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(12.dp),
                                    strokeWidth = 1.5.dp,
                                    color = tint
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Sync,
                                    contentDescription = null,
                                    tint = tint,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isRerunning) "重跑中..." else "重跑本分镜",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = tint
                            )
                        }
                    }
                    }
                }
            }
        }
    }

    if (showEditDialog && onSavePrompt != null) {
        ScenePromptEditDialog(
            clip = clip,
            onDismiss = { showEditDialog = false },
            onSave = { title, visualPrompt, cameraMovement, narration ->
                onSavePrompt(title, visualPrompt, cameraMovement, narration)
                showEditDialog = false
            }
        )
    }
}

/**
 * Edit dialog for a single storyboard scene. Lets the user fix the visual prompt (and the
 * auxiliary title / camera movement / narration) before re-rendering the clip.
 */
@Composable
private fun ScenePromptEditDialog(
    clip: SceneClip,
    onDismiss: () -> Unit,
    onSave: (title: String, visualPrompt: String, cameraMovement: String, narration: String) -> Unit
) {
    var title by remember { mutableStateOf(clip.sceneTitle) }
    var visualPrompt by remember { mutableStateOf(clip.visualPrompt) }
    var cameraMovement by remember { mutableStateOf(clip.cameraMovement) }
    var narration by remember { mutableStateOf(clip.narration) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, AgnesViolet.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                .testTag("scene_prompt_edit_dialog"),
            color = AppCardBg,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(AgnesViolet, RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "分镜 0${clip.sceneNumber}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "编辑分镜脚本",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppTextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "修改后点击保存；如需重新生成画面，请再点「重跑本分镜」。",
                    fontSize = 10.sp,
                    color = AppTextSecondary
                )

                Spacer(modifier = Modifier.height(12.dp))

                EditField(label = "标题", value = title, onValueChange = { title = it }, minLines = 1, maxLines = 2, tag = "edit_scene_title")
                Spacer(modifier = Modifier.height(10.dp))
                EditField(label = "画面提示词 (Visual Prompt)", value = visualPrompt, onValueChange = { visualPrompt = it }, minLines = 4, maxLines = 8, tag = "edit_scene_prompt")
                Spacer(modifier = Modifier.height(10.dp))
                EditField(label = "运镜方式 (Camera Movement)", value = cameraMovement, onValueChange = { cameraMovement = it }, minLines = 1, maxLines = 2, tag = "edit_scene_camera")
                Spacer(modifier = Modifier.height(10.dp))
                EditField(label = "旁白 / 台词 (Narration)", value = narration, onValueChange = { narration = it }, minLines = 2, maxLines = 4, tag = "edit_scene_narration")

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("取消", color = AppTextSecondary, fontSize = 13.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onSave(title, visualPrompt, cameraMovement, narration) },
                        enabled = visualPrompt.isNotBlank(),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AgnesViolet),
                        modifier = Modifier.testTag("save_scene_prompt_button")
                    ) {
                        Text("保存脚本", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun EditField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    minLines: Int,
    maxLines: Int,
    tag: String
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = AgnesCyan
        )
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(tag),
            minLines = minLines,
            maxLines = maxLines,
            shape = RoundedCornerShape(8.dp),
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AgnesCyan,
                unfocusedBorderColor = AppCardBorder,
                focusedContainerColor = AppInputBg,
                unfocusedContainerColor = AppInputBg,
                focusedTextColor = AppTextPrimary,
                unfocusedTextColor = AppTextPrimary
            )
        )
    }
}
