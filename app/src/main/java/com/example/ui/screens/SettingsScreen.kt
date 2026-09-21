package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.data.model.AIProvider
import com.example.data.model.AgnesApiConfig
import com.example.ui.theme.AgnesAmber
import com.example.ui.theme.AgnesCyan
import com.example.ui.theme.AgnesEmerald
import com.example.ui.theme.AgnesRose
import com.example.ui.theme.AgnesViolet
import com.example.ui.theme.AgnesVioletLight
import com.example.ui.theme.AppBackground
import com.example.ui.theme.AppCardBg
import com.example.ui.theme.AppCardBorder
import com.example.ui.theme.AppDivider
import com.example.ui.theme.AppInputBg
import com.example.ui.theme.AppSubtleBg
import com.example.ui.theme.AppSurface
import com.example.ui.theme.AppTextPrimary
import com.example.ui.theme.AppTextSecondary
import com.example.ui.viewmodel.AgnesViewModel
import java.util.UUID

sealed class SettingsSubPage {
    object Main : SettingsSubPage()
    object Providers : SettingsSubPage()
    object ModelMapping : SettingsSubPage()
    object RateLimitAndGeneration : SettingsSubPage()
    object Appearance : SettingsSubPage()
}

@Composable
fun SettingsScreen(
    viewModel: AgnesViewModel,
    modifier: Modifier = Modifier
) {
    val currentConfig by viewModel.config.collectAsState()
    val availableModels by viewModel.availableModels.collectAsState()
    val isFetchingModels by viewModel.isFetchingModels.collectAsState()

    var currentPage by remember { mutableStateOf<SettingsSubPage>(SettingsSubPage.Main) }

    // Intercept back button when inside a second-level subpage
    BackHandler(enabled = currentPage !is SettingsSubPage.Main) {
        currentPage = SettingsSubPage.Main
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(12.dp)
            .testTag("settings_screen")
    ) {
        AnimatedContent(
            targetState = currentPage,
            transitionSpec = {
                if (initialState is SettingsSubPage.Main) {
                    (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                        slideOutHorizontally { width -> -width } + fadeOut()
                    )
                } else {
                    (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
                        slideOutHorizontally { width -> width } + fadeOut()
                    )
                }
            },
            label = "settings_navigation_animation"
        ) { page ->
            when (page) {
                is SettingsSubPage.Main -> {
                    SettingsMainView(
                        config = currentConfig,
                        onNavigate = { subPage -> currentPage = subPage },
                        onToggleTheme = { isDark -> viewModel.toggleTheme(isDark) }
                    )
                }
                is SettingsSubPage.Providers -> {
                    ProvidersConfigSubPage(
                        viewModel = viewModel,
                        config = currentConfig,
                        onBack = { currentPage = SettingsSubPage.Main }
                    )
                }
                is SettingsSubPage.ModelMapping -> {
                    ModelMappingSubPage(
                        viewModel = viewModel,
                        config = currentConfig,
                        availableModels = availableModels,
                        isFetchingModels = isFetchingModels,
                        onBack = { currentPage = SettingsSubPage.Main }
                    )
                }
                is SettingsSubPage.RateLimitAndGeneration -> {
                    RateLimitAndGenerationSubPage(
                        viewModel = viewModel,
                        config = currentConfig,
                        onBack = { currentPage = SettingsSubPage.Main }
                    )
                }
                is SettingsSubPage.Appearance -> {
                    AppearanceSubPage(
                        viewModel = viewModel,
                        config = currentConfig,
                        onBack = { currentPage = SettingsSubPage.Main }
                    )
                }
            }
        }
    }
}

/**
 * 第一层：设置主菜单导航页
 */
@Composable
fun SettingsMainView(
    config: AgnesApiConfig,
    onNavigate: (SettingsSubPage) -> Unit,
    onToggleTheme: (Boolean) -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "系统与模型设置",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "分层配置 Provider、多模态专属模型与调度策略",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Quick Overview Card
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, AgnesViolet.copy(alpha = 0.35f), RoundedCornerShape(12.dp)),
            color = AppCardBg,
            tonalElevation = 2.dp
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "⚡ 当前已激活配置概览",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppTextPrimary
                    )
                    Box(
                        modifier = Modifier
                            .background(AgnesEmerald.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                            .border(0.5.dp, AgnesEmerald.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${config.providers.size} 个 Providers 渠道",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = AgnesEmerald
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                val chatProviderName = config.providers.find { it.id == config.chatProviderId }?.name ?: "默认渠道"
                val imageProviderName = config.providers.find { it.id == config.imageProviderId }?.name ?: "默认渠道"
                val videoProviderName = config.providers.find { it.id == config.videoProviderId }?.name ?: "默认渠道"

                ConfigOverviewItem(
                    label = "💬 对话任务",
                    providerName = chatProviderName,
                    modelName = config.chatModelName,
                    color = AgnesEmerald
                )
                Spacer(modifier = Modifier.height(4.dp))
                ConfigOverviewItem(
                    label = "🎨 生图任务",
                    providerName = imageProviderName,
                    modelName = config.modelName,
                    color = AgnesVioletLight
                )
                Spacer(modifier = Modifier.height(4.dp))
                ConfigOverviewItem(
                    label = "🎬 视频任务",
                    providerName = videoProviderName,
                    modelName = config.videoModelName,
                    color = AgnesCyan
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Level 1 Navigation Menu Items
        Text(
            text = "功能配置分类",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
        )

        // Item 1: Provider 管理
        SettingsNavigationCard(
            icon = Icons.Default.Hub,
            iconTint = AgnesCyan,
            iconBg = AgnesCyan.copy(alpha = 0.15f),
            title = "第一层：API Provider 渠道配置",
            subtitle = "管理多个服务商 (Dream AI、OpenAI、硅基流动、DeepSeek 或自定义端点与密钥)",
            badge = "${config.providers.size} 已配置",
            badgeColor = AgnesCyan,
            testTag = "nav_providers_button",
            onClick = { onNavigate(SettingsSubPage.Providers) }
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Item 2: 模型分层配置 (先选 Provider 再选 Model)
        SettingsNavigationCard(
            icon = Icons.Default.AutoAwesome,
            iconTint = AgnesVioletLight,
            iconBg = AgnesViolet.copy(alpha = 0.2f),
            title = "第二层：分任务专属模型配置",
            subtitle = "针对对话、生图、视频生成分别指定 Provider 与对应模型名称，支持一键拉取",
            badge = "分层匹配",
            badgeColor = AgnesVioletLight,
            testTag = "nav_model_mapping_button",
            onClick = { onNavigate(SettingsSubPage.ModelMapping) }
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Item 3: 限速与流水线控制
        SettingsNavigationCard(
            icon = Icons.Default.Speed,
            iconTint = AgnesAmber,
            iconBg = AgnesAmber.copy(alpha = 0.15f),
            title = "生图 & 视频限速与拼接设置",
            subtitle = "配置 60s 安全冷却保护周期与多段视频全自动合成开关",
            badge = "${config.rateLimitSeconds}s 冷却",
            badgeColor = AgnesAmber,
            testTag = "nav_ratelimit_button",
            onClick = { onNavigate(SettingsSubPage.RateLimitAndGeneration) }
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Item 4: 外观与主题
        SettingsNavigationCard(
            icon = if (config.isDarkTheme) Icons.Default.DarkMode else Icons.Default.LightMode,
            iconTint = if (config.isDarkTheme) AgnesVioletLight else AgnesAmber,
            iconBg = if (config.isDarkTheme) AgnesViolet.copy(alpha = 0.15f) else AgnesAmber.copy(alpha = 0.15f),
            title = "界面主题与偏好",
            subtitle = if (config.isDarkTheme) "当前为经典极客暗黑主题" else "当前为清新明亮白主题",
            badge = if (config.isDarkTheme) "Dark" else "Light",
            badgeColor = if (config.isDarkTheme) AgnesVioletLight else AgnesAmber,
            testTag = "nav_appearance_button",
            onClick = { onNavigate(SettingsSubPage.Appearance) }
        )

        Spacer(modifier = Modifier.height(18.dp))

        // App Version Info Card
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp)),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Dream AI Studio",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "自动构建时间戳版本（支持平滑覆盖安装）",
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Box(
                    modifier = Modifier
                        .background(AgnesViolet.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                        .border(0.5.dp, AgnesViolet.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "v${BuildConfig.VERSION_NAME}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = AgnesVioletLight
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun ConfigOverviewItem(
    label: String,
    providerName: String,
    modelName: String,
    color: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppSubtleBg, RoundedCornerShape(6.dp))
            .border(0.5.dp, AppCardBorder, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = AppTextSecondary,
            fontWeight = FontWeight.Medium
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "[$providerName]",
                fontSize = 10.sp,
                color = color,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = modelName,
                fontSize = 11.sp,
                color = AppTextPrimary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun SettingsNavigationCard(
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    title: String,
    subtitle: String,
    badge: String? = null,
    badgeColor: Color = AgnesCyan,
    testTag: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .testTag(testTag),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(iconBg, RoundedCornerShape(10.dp))
                    .border(0.5.dp, iconTint.copy(alpha = 0.4f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (badge != null) {
                        Box(
                            modifier = Modifier
                                .background(badgeColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                .border(0.5.dp, badgeColor.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
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
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 14.sp
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

/**
 * 第二层子页面 1: Provider 渠道配置
 */
@Composable
fun ProvidersConfigSubPage(
    viewModel: AgnesViewModel,
    config: AgnesApiConfig,
    onBack: () -> Unit
) {
    val scrollState = rememberScrollState()
    var editingProvider by remember { mutableStateOf<AIProvider?>(null) }
    var isAddingNew by remember { mutableStateOf(false) }

    var testStatusMap by remember { mutableStateOf<Map<String, Pair<Boolean, String>>>(emptyMap()) }
    var testingProviderId by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        // Subpage Header
        SubPageHeader(
            title = "API Providers 渠道管理",
            subtitle = "添加与配置各主流 AI 服务商端点 (Base URL) 与 API 密钥",
            onBack = onBack
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Add Provider Button
        Button(
            onClick = {
                isAddingNew = true
                editingProvider = AIProvider(
                    id = UUID.randomUUID().toString(),
                    name = "自定义 Provider",
                    endpointUrl = "https://api.openai.com/v1",
                    apiKey = "",
                    authHeader = "Bearer",
                    description = "自定义 OpenAI 兼容接口"
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp)
                .testTag("add_provider_button"),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AgnesCyan)
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = Color(0xFF0B0F19), modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = "添加新的 Provider 渠道", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0B0F19))
        }

        Spacer(modifier = Modifier.height(12.dp))

        // List of configured Providers
        config.providers.forEach { provider ->
            val isTestingThis = testingProviderId == provider.id
            val testStatus = testStatusMap[provider.id]

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .border(
                        1.dp,
                        if (provider.isDefault) AgnesCyan.copy(alpha = 0.5f) else AppCardBorder,
                        RoundedCornerShape(10.dp)
                    ),
                color = AppCardBg,
                tonalElevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = provider.name,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = AppTextPrimary
                            )
                            if (provider.isDefault) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .background(AgnesCyan.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                ) {
                                    Text(text = "默认推荐", fontSize = 8.sp, color = AgnesCyan, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    isAddingNew = false
                                    editingProvider = provider
                                },
                                modifier = Modifier.size(28.dp).testTag("edit_provider_${provider.id}")
                            ) {
                                Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit", tint = AgnesVioletLight, modifier = Modifier.size(16.dp))
                            }
                            if (!provider.isDefault && config.providers.size > 1) {
                                IconButton(
                                    onClick = { viewModel.deleteProvider(provider.id) },
                                    modifier = Modifier.size(28.dp).testTag("delete_provider_${provider.id}")
                                ) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = AgnesRose, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "端点: ${provider.endpointUrl}",
                        fontSize = 10.sp,
                        color = AppTextSecondary
                    )

                    Text(
                        text = if (provider.apiKey.isNotBlank()) "密钥: ••••••••${provider.apiKey.takeLast(4)}" else "密钥: (未设置，请点击编辑配置)",
                        fontSize = 10.sp,
                        color = if (provider.apiKey.isNotBlank()) AgnesEmerald else AgnesAmber
                    )

                    if (provider.description.isNotBlank()) {
                        Text(
                            text = provider.description,
                            fontSize = 9.sp,
                            color = AppTextSecondary,
                            lineHeight = 12.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                testingProviderId = provider.id
                                viewModel.testProviderConnection(provider) { success, msg ->
                                    testingProviderId = null
                                    testStatusMap = testStatusMap + (provider.id to Pair(success, msg))
                                }
                            },
                            enabled = !isTestingThis,
                            modifier = Modifier.weight(1f).height(34.dp).testTag("test_provider_${provider.id}"),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            if (isTestingThis) {
                                CircularProgressIndicator(modifier = Modifier.size(12.dp), color = AgnesCyan, strokeWidth = 1.5.dp)
                            } else {
                                Icon(imageVector = Icons.Default.CloudSync, contentDescription = null, tint = AgnesCyan, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "测试连通性", fontSize = 10.sp, color = AgnesCyan)
                            }
                        }

                        Button(
                            onClick = {
                                isAddingNew = false
                                editingProvider = provider
                            },
                            modifier = Modifier.weight(1f).height(34.dp),
                            shape = RoundedCornerShape(6.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AppSubtleBg)
                        ) {
                            Icon(imageVector = Icons.Default.Tune, contentDescription = null, tint = AppTextPrimary, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "配置参数 / 密钥", fontSize = 10.sp, color = AppTextPrimary)
                        }
                    }

                    if (testStatus != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        val (success, msg) = testStatus
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (success) AgnesEmerald.copy(alpha = 0.15f) else AgnesRose.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                .padding(6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (success) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = if (success) AgnesEmerald else AgnesRose,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = msg, fontSize = 10.sp, color = if (success) AgnesEmerald else AgnesRose)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    // Provider Edit / Add Dialog
    if (editingProvider != null) {
        val prov = editingProvider!!
        var editName by remember { mutableStateOf(prov.name) }
        var editEndpoint by remember { mutableStateOf(prov.endpointUrl) }
        var editApiKey by remember { mutableStateOf(prov.apiKey) }
        var editAuthHeader by remember { mutableStateOf(prov.authHeader) }
        var editDescription by remember { mutableStateOf(prov.description) }
        var isPwdVisible by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { editingProvider = null },
            title = {
                Text(
                    text = if (isAddingNew) "添加新的 Provider" else "编辑 Provider: ${prov.name}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppTextPrimary
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(text = "Provider 名称:", fontSize = 11.sp, color = AppTextSecondary)
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        modifier = Modifier.fillMaxWidth().testTag("edit_provider_name"),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(text = "API Base URL (端点地址):", fontSize = 11.sp, color = AppTextSecondary)
                    OutlinedTextField(
                        value = editEndpoint,
                        onValueChange = { editEndpoint = it },
                        modifier = Modifier.fillMaxWidth().testTag("edit_provider_endpoint"),
                        placeholder = { Text("https://api.openai.com/v1", fontSize = 11.sp) },
                        shape = RoundedCornerShape(8.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(text = "API Key (密钥):", fontSize = 11.sp, color = AppTextSecondary)
                    OutlinedTextField(
                        value = editApiKey,
                        onValueChange = { editApiKey = it },
                        modifier = Modifier.fillMaxWidth().testTag("edit_provider_key"),
                        placeholder = { Text("sk-...", fontSize = 11.sp) },
                        visualTransformation = if (isPwdVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { isPwdVisible = !isPwdVisible }) {
                                Icon(
                                    imageVector = if (isPwdVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        },
                        shape = RoundedCornerShape(8.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(text = "授权请求头 (Auth Header, 默认 Bearer):", fontSize = 11.sp, color = AppTextSecondary)
                    OutlinedTextField(
                        value = editAuthHeader,
                        onValueChange = { editAuthHeader = it },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(text = "备注说明:", fontSize = 11.sp, color = AppTextSecondary)
                    OutlinedTextField(
                        value = editDescription,
                        onValueChange = { editDescription = it },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val updated = prov.copy(
                            name = editName.trim().ifBlank { "自定义 Provider" },
                            endpointUrl = editEndpoint.trim().ifBlank { "https://api.agnes-ai.cn/v1" },
                            apiKey = editApiKey.trim(),
                            authHeader = editAuthHeader.trim().ifBlank { "Bearer" },
                            description = editDescription.trim()
                        )
                        viewModel.addOrUpdateProvider(updated)
                        editingProvider = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AgnesViolet),
                    modifier = Modifier.testTag("confirm_provider_button")
                ) {
                    Text("保存", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingProvider = null }) {
                    Text("取消", color = AppTextSecondary)
                }
            },
            containerColor = AppCardBg
        )
    }
}

/**
 * 第二层子页面 2: 分任务模型配置 (先选 Provider 再选 Model)
 */
@Composable
fun ModelMappingSubPage(
    viewModel: AgnesViewModel,
    config: AgnesApiConfig,
    availableModels: List<String>,
    isFetchingModels: Boolean,
    onBack: () -> Unit
) {
    val scrollState = rememberScrollState()

    var chatProviderId by remember(config) { mutableStateOf(config.chatProviderId) }
    var chatModelName by remember(config) { mutableStateOf(config.chatModelName) }

    var imageProviderId by remember(config) { mutableStateOf(config.imageProviderId) }
    var modelName by remember(config) { mutableStateOf(config.modelName) }

    var videoProviderId by remember(config) { mutableStateOf(config.videoProviderId) }
    var videoModelName by remember(config) { mutableStateOf(config.videoModelName) }

    var fetchStatusText by remember { mutableStateOf<String?>(null) }
    var activeFetchingProviderId by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        SubPageHeader(
            title = "分任务专属模型配置",
            subtitle = "分层模式：第一层选择任务适用的 Provider，第二层选择/输入对应模型名称",
            onBack = onBack
        )

        Spacer(modifier = Modifier.height(10.dp))

        // 1. 对话任务配置
        TaskModelConfigurationCard(
            taskTitle = "💬 对话任务 (Chat & Copilot)",
            taskBadge = "⚡ 高速实时通道",
            badgeColor = AgnesEmerald,
            taskDescription = "用于日常智能对话、影视剧本策划、提示词润色。不计入 1 分钟生图限速队列。",
            providers = config.providers,
            selectedProviderId = chatProviderId,
            onProviderSelected = { chatProviderId = it },
            modelName = chatModelName,
            onModelNameChange = { chatModelName = it },
            availableModels = availableModels,
            filterKeywords = listOf("chat", "gpt", "claude", "deepseek", "qwen", "gemini", "agent"),
            placeholder = "gpt-4o",
            providerDropdownTag = "chat_provider_dropdown",
            modelInputTag = "chat_model_input",
            onFetchModels = { prov ->
                activeFetchingProviderId = prov.id
                viewModel.fetchModelsForProvider(prov) { success, list, msg ->
                    activeFetchingProviderId = null
                    fetchStatusText = if (success) "✅ [${prov.name}] 拉取成功: ${list.size} 个模型" else "⚠️ [${prov.name}] $msg"
                }
            },
            isFetching = isFetchingModels && activeFetchingProviderId == chatProviderId
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 2. 生图任务配置
        TaskModelConfigurationCard(
            taskTitle = "🎨 图像生成与重绘 (Image Generation)",
            taskBadge = "⏱️ 1分钟限速保护",
            badgeColor = AgnesAmber,
            taskDescription = "只在生图或艺术风格重绘时触发，严格执行 1 分钟排队防超频机制。",
            providers = config.providers,
            selectedProviderId = imageProviderId,
            onProviderSelected = { imageProviderId = it },
            modelName = modelName,
            onModelNameChange = { modelName = it },
            availableModels = availableModels,
            filterKeywords = listOf("flux", "sd", "dall", "vision", "diffusion", "image"),
            placeholder = "flux-1-dev",
            providerDropdownTag = "image_provider_dropdown",
            modelInputTag = "image_model_input",
            onFetchModels = { prov ->
                activeFetchingProviderId = prov.id
                viewModel.fetchModelsForProvider(prov) { success, list, msg ->
                    activeFetchingProviderId = null
                    fetchStatusText = if (success) "✅ [${prov.name}] 拉取成功: ${list.size} 个模型" else "⚠️ [${prov.name}] $msg"
                }
            },
            isFetching = isFetchingModels && activeFetchingProviderId == imageProviderId
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 3. 视频生成任务配置
        TaskModelConfigurationCard(
            taskTitle = "🎬 视频生成与分镜流水线 (Video Generation)",
            taskBadge = "⏱️ 1分钟限速保护",
            badgeColor = AgnesCyan,
            taskDescription = "用于电影分镜逐幕视频生成，支持每分钟生成一段短片并自动多段拼接合成。",
            providers = config.providers,
            selectedProviderId = videoProviderId,
            onProviderSelected = { videoProviderId = it },
            modelName = videoModelName,
            onModelNameChange = { videoModelName = it },
            availableModels = availableModels,
            filterKeywords = listOf("kling", "video", "sora", "luma", "runway", "cog"),
            placeholder = "kling-v1",
            providerDropdownTag = "video_provider_dropdown",
            modelInputTag = "video_model_input",
            onFetchModels = { prov ->
                activeFetchingProviderId = prov.id
                viewModel.fetchModelsForProvider(prov) { success, list, msg ->
                    activeFetchingProviderId = null
                    fetchStatusText = if (success) "✅ [${prov.name}] 拉取成功: ${list.size} 个模型" else "⚠️ [${prov.name}] $msg"
                }
            },
            isFetching = isFetchingModels && activeFetchingProviderId == videoProviderId
        )

        if (fetchStatusText != null) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = fetchStatusText ?: "",
                fontSize = 11.sp,
                color = AgnesCyan,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Save Button
        Button(
            onClick = {
                val updated = config.copy(
                    chatProviderId = chatProviderId,
                    chatModelName = chatModelName.trim().ifBlank { "gpt-4o" },
                    imageProviderId = imageProviderId,
                    modelName = modelName.trim().ifBlank { "flux-1-dev" },
                    videoProviderId = videoProviderId,
                    videoModelName = videoModelName.trim().ifBlank { "kling-v1" }
                )
                viewModel.updateConfig(updated)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .testTag("save_model_mapping_button"),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AgnesViolet)
        ) {
            Icon(imageVector = Icons.Default.Save, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = "保存所有分层模型映射", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

/**
 * 分层配置卡片：Step 1 选 Provider -> Step 2 选/输入 Model
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TaskModelConfigurationCard(
    taskTitle: String,
    taskBadge: String,
    badgeColor: Color,
    taskDescription: String,
    providers: List<AIProvider>,
    selectedProviderId: String,
    onProviderSelected: (String) -> Unit,
    modelName: String,
    onModelNameChange: (String) -> Unit,
    availableModels: List<String>,
    filterKeywords: List<String>,
    placeholder: String,
    providerDropdownTag: String,
    modelInputTag: String,
    onFetchModels: (AIProvider) -> Unit,
    isFetching: Boolean
) {
    var isProviderDropdownExpanded by remember { mutableStateOf(false) }
    val currentSelectedProvider = providers.find { it.id == selectedProviderId } ?: providers.firstOrNull()

    // Model list combines global available models + provider-specific custom models
    val combinedModels = remember(currentSelectedProvider, availableModels) {
        val custom = currentSelectedProvider?.customModels ?: emptyList()
        (custom + availableModels).distinct()
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, AppCardBorder, RoundedCornerShape(10.dp)),
        color = AppCardBg,
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = taskTitle,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppTextPrimary
                )
                Box(
                    modifier = Modifier
                        .background(badgeColor.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                        .border(0.5.dp, badgeColor.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(text = taskBadge, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = badgeColor)
                }
            }

            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = taskDescription,
                fontSize = 9.sp,
                color = AppTextSecondary,
                lineHeight = 12.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Step 1: Select Provider
            Text(
                text = "第 1 步：选择服务商 (Provider)",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = AgnesCyan
            )
            Spacer(modifier = Modifier.height(4.dp))

            ExposedDropdownMenuBox(
                expanded = isProviderDropdownExpanded,
                onExpandedChange = { isProviderDropdownExpanded = it }
            ) {
                OutlinedTextField(
                    value = currentSelectedProvider?.name ?: "请选择 Provider",
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                        .testTag(providerDropdownTag),
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = isProviderDropdownExpanded)
                    },
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

                ExposedDropdownMenu(
                    expanded = isProviderDropdownExpanded,
                    onDismissRequest = { isProviderDropdownExpanded = false },
                    modifier = Modifier.background(AppCardBg)
                ) {
                    providers.forEach { prov ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(
                                        text = prov.name,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (prov.id == selectedProviderId) AgnesCyan else AppTextPrimary
                                    )
                                    Text(
                                        text = prov.endpointUrl,
                                        fontSize = 9.sp,
                                        color = AppTextSecondary
                                    )
                                }
                            },
                            onClick = {
                                onProviderSelected(prov.id)
                                isProviderDropdownExpanded = false
                            },
                            trailingIcon = {
                                if (prov.id == selectedProviderId) {
                                    Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = AgnesCyan, modifier = Modifier.size(16.dp))
                                }
                            }
                        )
                    }
                }
            }

            // Quick fetch button for currently selected provider
            if (currentSelectedProvider != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = { onFetchModels(currentSelectedProvider) },
                        enabled = !isFetching
                    ) {
                        if (isFetching) {
                            CircularProgressIndicator(modifier = Modifier.size(12.dp), color = AgnesCyan, strokeWidth = 1.5.dp)
                            Spacer(modifier = Modifier.width(4.dp))
                        } else {
                            Icon(imageVector = Icons.Default.CloudDownload, contentDescription = null, tint = AgnesCyan, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(
                            text = "从此 Provider 拉取模型 (${currentSelectedProvider.name})",
                            fontSize = 10.sp,
                            color = AgnesCyan
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Step 2: Select or Input Model
            Text(
                text = "第 2 步：选择或输入模型名称 (Model)",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = AgnesVioletLight
            )
            Spacer(modifier = Modifier.height(4.dp))

            ModelInputFieldWithSuggestions(
                label = "",
                badge = if (combinedModels.contains(modelName)) "官方推荐/已同步" else "自定义输入",
                badgeColor = if (combinedModels.contains(modelName)) AgnesEmerald else AgnesAmber,
                tooltip = "支持从下拉列表中选择或直接手动输入。若列表中没有，将以您输入的内容为准。",
                currentValue = modelName,
                onValueChange = onModelNameChange,
                availableList = combinedModels,
                filterKeywords = filterKeywords,
                placeholder = placeholder,
                testTag = modelInputTag
            )
        }
    }
}

/**
 * 第二层子页面 3: 限速策略与视频拼接配置
 */
@Composable
fun RateLimitAndGenerationSubPage(
    viewModel: AgnesViewModel,
    config: AgnesApiConfig,
    onBack: () -> Unit
) {
    val scrollState = rememberScrollState()
    var rateLimitSeconds by remember(config) { mutableStateOf(config.rateLimitSeconds.toString()) }
    var autoStitch by remember(config) { mutableStateOf(config.autoStitchVideos) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        SubPageHeader(
            title = "生图 & 视频限速与流水线控制",
            subtitle = "精准设置 1 分钟安全冷却周期与全自动多段视频合成",
            onBack = onBack
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Rate limit explanation card
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .border(1.dp, AgnesAmber.copy(alpha = 0.4f), RoundedCornerShape(10.dp)),
            color = AppCardBg,
            tonalElevation = 2.dp
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Speed, contentDescription = null, tint = AgnesAmber, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "严格限速策略 (Rate Limiting)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AppTextPrimary)
                }

                Spacer(modifier = Modifier.height(6.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AgnesAmber.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                        .padding(8.dp)
                ) {
                    Text(
                        text = "💡 说明：对话任务走高速通道实时响应；生图与生视频任务严格受安全周期保护（默认 60 秒），防止 API 返回 429 报错，保证分镜流水线平稳渲染。",
                        fontSize = 11.sp,
                        color = if (MaterialTheme.colorScheme.background == com.example.ui.theme.LightBackground) Color(0xFFB45309) else Color(0xFFFDE68A),
                        lineHeight = 15.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(text = "生图与生视频冷却间隔 (秒):", fontSize = 11.sp, color = AppTextSecondary)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = rateLimitSeconds,
                    onValueChange = { rateLimitSeconds = it },
                    modifier = Modifier.fillMaxWidth().testTag("rate_limit_input"),
                    placeholder = { Text("60", color = AppTextSecondary, fontSize = 12.sp) },
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AgnesAmber,
                        unfocusedBorderColor = AppCardBorder,
                        focusedContainerColor = AppInputBg,
                        unfocusedContainerColor = AppInputBg,
                        focusedTextColor = AppTextPrimary,
                        unfocusedTextColor = AppTextPrimary
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Auto stitch card
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .border(1.dp, AppCardBorder, RoundedCornerShape(10.dp)),
            color = AppCardBg,
            tonalElevation = 2.dp
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "多段分镜视频自动拼接合成", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = AppTextPrimary)
                        Text(text = "在各分镜片段逐个生成完毕后，自动拼合成无缝电影短片", fontSize = 10.sp, color = AppTextSecondary)
                    }
                    Switch(
                        checked = autoStitch,
                        onCheckedChange = { autoStitch = it },
                        modifier = Modifier.testTag("auto_stitch_switch"),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = AgnesCyan
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                val updated = config.copy(
                    rateLimitSeconds = rateLimitSeconds.toIntOrNull() ?: 60,
                    autoStitchVideos = autoStitch
                )
                viewModel.updateConfig(updated)
            },
            modifier = Modifier.fillMaxWidth().height(42.dp).testTag("save_ratelimit_button"),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AgnesViolet)
        ) {
            Icon(imageVector = Icons.Default.Save, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = "保存限速与流水线策略", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

/**
 * 第二层子页面 4: 主题与外观偏好
 */
@Composable
fun AppearanceSubPage(
    viewModel: AgnesViewModel,
    config: AgnesApiConfig,
    onBack: () -> Unit
) {
    val scrollState = rememberScrollState()
    var isDarkTheme by remember(config) { mutableStateOf(config.isDarkTheme) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        SubPageHeader(
            title = "界面主题与偏好",
            subtitle = "切换应用明亮白与极客暗黑主题配色",
            onBack = onBack
        )

        Spacer(modifier = Modifier.height(10.dp))

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                if (isDarkTheme) AgnesViolet.copy(alpha = 0.2f) else AgnesAmber.copy(alpha = 0.2f),
                                RoundedCornerShape(8.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isDarkTheme) Icons.Default.DarkMode else Icons.Default.LightMode,
                            contentDescription = null,
                            tint = if (isDarkTheme) AgnesVioletLight else AgnesAmber,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = if (isDarkTheme) "暗黑模式 (Dark Mode)" else "明亮模式 (Light Mode)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isDarkTheme) "沉浸式深空黑，适合夜间与极客创作" else "高对比清新白，适合日间清晰阅读",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Switch(
                    checked = !isDarkTheme,
                    onCheckedChange = { isLight ->
                        val newDark = !isLight
                        isDarkTheme = newDark
                        viewModel.toggleTheme(newDark)
                    },
                    modifier = Modifier.testTag("theme_switch"),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = AgnesCyan,
                        uncheckedThumbColor = AgnesVioletLight,
                        uncheckedTrackColor = AppCardBorder
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

/**
 * 通用二级页面头部组件（带返回主菜单按键）
 */
@Composable
fun SubPageHeader(
    title: String,
    subtitle: String,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .size(34.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                .testTag("subpage_back_button")
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
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

    val matchedSuggestions = remember(currentValue, availableList) {
        if (currentValue.isBlank()) {
            availableList.filter { model -> filterKeywords.any { k -> model.contains(k, ignoreCase = true) } }
                .ifEmpty { availableList.take(6) }
        } else {
            availableList.filter { it.contains(currentValue.trim(), ignoreCase = true) }
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        if (label.isNotBlank() || badge.isNotBlank()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (label.isNotBlank()) {
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFCBD5E1)
                    )
                }
                if (badge.isNotBlank()) {
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
            }
        }

        if (tooltip.isNotBlank()) {
            Text(
                text = tooltip,
                fontSize = 9.sp,
                color = Color(0xFF64748B),
                lineHeight = 12.sp,
                modifier = Modifier.padding(top = 2.dp, bottom = 4.dp)
            )
        }

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
                    unfocusedBorderColor = AppCardBorder,
                    focusedContainerColor = AppInputBg,
                    unfocusedContainerColor = AppInputBg,
                    focusedTextColor = AppTextPrimary,
                    unfocusedTextColor = AppTextPrimary
                )
            )

            if (matchedSuggestions.isNotEmpty()) {
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.background(AppCardBg)
                ) {
                    matchedSuggestions.take(10).forEach { suggestion ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = suggestion,
                                    fontSize = 12.sp,
                                    color = if (suggestion.equals(currentValue, ignoreCase = true)) AgnesCyan else AppTextPrimary
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
                        color = if (sug.equals(currentValue, ignoreCase = true)) AgnesViolet.copy(alpha = 0.3f) else AppSubtleBg,
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, if (sug.equals(currentValue, ignoreCase = true)) AgnesViolet else AppCardBorder)
                    ) {
                        Text(
                            text = sug,
                            fontSize = 9.sp,
                            color = if (sug.equals(currentValue, ignoreCase = true)) AgnesVioletLight else AppTextSecondary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}
