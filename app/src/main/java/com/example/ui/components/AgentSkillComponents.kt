package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Surface
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.skill.AgentSkill
import com.example.data.skill.InvocationStatus
import com.example.data.skill.SkillInvocationRecord
import com.example.ui.theme.AgnesCyan
import com.example.ui.theme.AgnesViolet
import com.example.ui.theme.AgnesVioletDark
import com.example.ui.theme.AppCardBg
import com.example.ui.theme.AppCardBorder
import com.example.ui.theme.AppSubtleBg
import com.example.ui.theme.AppSurface
import com.example.ui.theme.AppTextPrimary
import com.example.ui.theme.AppTextSecondary

/**
 * Top horizontal loaded skills bar in Agent Tab.
 */
@Composable
fun AgentLoadedSkillsBar(
    skills: List<AgentSkill>,
    onOpenSkillHub: () -> Unit,
    onSkillClick: (AgentSkill) -> Unit,
    modifier: Modifier = Modifier
) {
    val activeCount = skills.count { it.isEnabled }
    val scrollState = rememberScrollState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(AppCardBg)
            .border(1.dp, AppCardBorder, RoundedCornerShape(10.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Skill Hub trigger badge
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(Brush.horizontalGradient(listOf(AgnesVioletDark, AgnesViolet)))
                .clickable { onOpenSkillHub() }
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Extension,
                contentDescription = "已装载技能",
                tint = Color.White,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "技能库 ($activeCount/${skills.size})",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Horizontal scrollable skill chips
        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(scrollState),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            skills.forEach { skill ->
                val isEnabled = skill.isEnabled
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isEnabled) AgnesCyan.copy(alpha = 0.15f) else AppSubtleBg)
                        .border(
                            1.dp,
                            if (isEnabled) AgnesCyan.copy(alpha = 0.5f) else AppCardBorder,
                            RoundedCornerShape(6.dp)
                        )
                        .clickable { onSkillClick(skill) }
                        .padding(horizontal = 7.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = skill.iconEmoji,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = skill.name.take(6),
                        fontSize = 11.sp,
                        color = if (isEnabled) AppTextPrimary else AppTextSecondary,
                        fontWeight = if (isEnabled) FontWeight.Medium else FontWeight.Normal
                    )
                    if (isEnabled) {
                        Spacer(modifier = Modifier.width(3.dp))
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .background(AgnesCyan, CircleShape)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Banner shown when a Skill is currently executing in real-time.
 */
@Composable
fun ActiveSkillExecutingBanner(
    record: SkillInvocationRecord?,
    onCancel: () -> Unit = {},
    onDismiss: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (record == null || record.status != InvocationStatus.EXECUTING) return

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alphaAnim by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = AppCardBg),
        border = androidx.compose.foundation.BorderStroke(1.dp, AgnesCyan.copy(alpha = alphaAnim))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                color = AgnesCyan,
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${record.iconEmoji} 智能体正在执行技能: ",
                        fontSize = 12.sp,
                        color = AppTextSecondary
                    )
                    Text(
                        text = record.skillName,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = AgnesCyan
                    )
                }
                if (record.statusMessage.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = record.statusMessage,
                        fontSize = 11.sp,
                        color = AppTextSecondary,
                        maxLines = 1
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                onClick = onCancel,
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFFEF4444).copy(alpha = 0.2f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = "终止任务",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "终止",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFEF4444)
                    )
                }
            }
        }
    }
}

/**
 * Bottom Sheet for managing and viewing all loaded Skills.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgentSkillManagerSheet(
    skills: List<AgentSkill>,
    onToggleSkill: (String, Boolean) -> Unit,
    onUseSkillTemplate: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = AppSurface,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState())
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
                            .size(34.dp)
                            .background(Brush.linearGradient(listOf(AgnesViolet, AgnesCyan)), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Extension,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "智能体技能中心 (Skills)",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppTextPrimary
                        )
                        Text(
                            text = "当前已装载 ${skills.size} 个专业能力，可由智能体自主调度",
                            fontSize = 12.sp,
                            color = AppTextSecondary
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "关闭", tint = AppTextPrimary)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Skill items
            skills.forEach { skill ->
                SkillDetailCard(
                    skill = skill,
                    onToggle = { enabled -> onToggleSkill(skill.id, enabled) },
                    onUseTemplate = {
                        val prompt = when (skill.id) {
                            "image-generation" -> "请帮我画一张电影感赛博朋克雨夜街道概念图"
                            "video-generation" -> "请制作一部关于深海古城探索的多幕视频短片"
                            "prompt-enhancer" -> "帮我润色一段关于魔法图书馆的摄影级提示词"
                            "storyboard-director" -> "为一部科幻太空救援电影设计4幕标准导演分镜表"
                            else -> "调用技能 ${skill.name}"
                        }
                        onUseSkillTemplate(prompt)
                        onDismiss()
                    }
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/**
 * Individual card inside the Skill Manager.
 */
@Composable
fun SkillDetailCard(
    skill: AgentSkill,
    onToggle: (Boolean) -> Unit,
    onUseTemplate: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = AppCardBg),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (skill.isEnabled) AgnesCyan.copy(alpha = 0.4f) else AppCardBorder
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = skill.iconEmoji,
                        fontSize = 24.sp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = skill.name,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = AppTextPrimary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            // Category Tag
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(AgnesViolet.copy(alpha = 0.2f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = skill.id,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = AgnesViolet
                                )
                            }
                        }
                    }
                }

                // Switch
                Switch(
                    checked = skill.isEnabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = AgnesCyan,
                        uncheckedThumbColor = AppTextSecondary,
                        uncheckedTrackColor = AppCardBorder
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Description
            Text(
                text = skill.description,
                fontSize = 13.sp,
                color = AppTextSecondary,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Expandable details (Parameters & Triggers)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (expanded) "收起参数契约与触发词" else "查看参数规格 (${skill.parameters.size} 个入参)",
                    fontSize = 12.sp,
                    color = AgnesCyan
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = AgnesCyan,
                    modifier = Modifier.size(18.dp)
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .background(AppSubtleBg, RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        text = "参数契约 (Schema):",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppTextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    skill.parameters.forEach { p ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                        ) {
                            Text(
                                text = "• ${p.name} [${p.type}]",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = AgnesCyan,
                                modifier = Modifier.width(130.dp)
                            )
                            Text(
                                text = "${p.description}${if (p.required) " (必填)" else ""}",
                                fontSize = 11.sp,
                                color = AppTextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "自然语言触发词: ${skill.triggerKeywords.take(6).joinToString(", ")}...",
                        fontSize = 11.sp,
                        color = AppTextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Quick Use Button
            OutlinedButton(
                onClick = onUseTemplate,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, AgnesViolet)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = AgnesCyan,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "快速填入该技能指令",
                    fontSize = 12.sp,
                    color = AppTextPrimary
                )
            }
        }
    }
}

/**
 * High-craft interactive Document Card rendered inside Chat Stream for Word, PDF and Excel results.
 */
@Composable
fun AgentDocumentCard(
    documentName: String,
    documentType: String, // "WORD", "PDF", "EXCEL"
    documentSize: String?,
    documentUri: String,
    onOpenDocument: () -> Unit,
    onShareDocument: () -> Unit,
    onSaveToDownloads: () -> Unit,
    modifier: Modifier = Modifier
) {
    val upperType = documentType.uppercase()

    val (badgeBg, badgeBorder, badgeText, extLabel, typeIconEmoji) = when (upperType) {
        "WORD" -> Quintuple(
            Color(0xFF1E3A8A),
            Color(0xFF3B82F6),
            Color(0xFF93C5FD),
            "DOCX",
            "📝"
        )
        "PDF" -> Quintuple(
            Color(0xFF7F1D1D),
            Color(0xFFEF4444),
            Color(0xFFFCA5A5),
            "PDF",
            "📄"
        )
        "EXCEL" -> Quintuple(
            Color(0xFF064E3B),
            Color(0xFF10B981),
            Color(0xFF6EE7B7),
            "XLSX",
            "📊"
        )
        else -> Quintuple(
            Color(0xFF1E293B),
            Color(0xFF64748B),
            Color(0xFFCBD5E1),
            "DOC",
            "📁"
        )
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = AppCardBg
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, badgeBorder.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Document Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Type Icon Badge
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(badgeBg)
                        .border(1.dp, badgeBorder, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = extLabel,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = badgeText,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = typeIconEmoji,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = documentName,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppTextPrimary,
                            maxLines = 1
                        )
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Tag: Formatted Size
                        if (!documentSize.isNullOrBlank()) {
                            Text(
                                text = documentSize,
                                fontSize = 10.sp,
                                color = AppTextSecondary
                            )
                            Text(text = "•", fontSize = 10.sp, color = AppTextSecondary)
                        }
                        // Tag: Standard Sandbox Ready
                        Text(
                            text = "已生成编译完成",
                            fontSize = 10.sp,
                            color = AgnesCyan,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons Row: Open, Share, Download
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Button 1: Open Document
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(badgeBg.copy(alpha = 0.8f))
                        .border(1.dp, badgeBorder, RoundedCornerShape(8.dp))
                        .clickable { onOpenDocument() }
                        .padding(vertical = 7.dp, horizontal = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.OpenInNew,
                            contentDescription = "打开",
                            tint = Color.White,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "打开预览",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }

                // Button 2: Share Document
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(AppSubtleBg)
                        .border(1.dp, AppCardBorder, RoundedCornerShape(8.dp))
                        .clickable { onShareDocument() }
                        .padding(vertical = 7.dp, horizontal = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "分享",
                            tint = AgnesCyan,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "分享发送",
                            fontSize = 11.sp,
                            color = AgnesCyan
                        )
                    }
                }

                // Button 3: Save to Downloads
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(AppSubtleBg)
                        .border(1.dp, AppCardBorder, RoundedCornerShape(8.dp))
                        .clickable { onSaveToDownloads() }
                        .padding(vertical = 7.dp, horizontal = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "保存",
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "存入系统",
                            fontSize = 11.sp,
                            color = Color(0xFF10B981)
                        )
                    }
                }
            }
        }
    }
}

private data class Quintuple<A, B, C, D, E>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E
)

