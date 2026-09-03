package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ProjectType
import com.example.ui.screens.AgentChatScreen
import com.example.ui.screens.GalleryScreen
import com.example.ui.screens.ImageStudioScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.VideoPipelineScreen
import com.example.ui.theme.AgnesCyan
import com.example.ui.theme.AgnesViolet
import com.example.ui.theme.CyberCardBg
import com.example.ui.theme.CyberCardBorder
import com.example.ui.theme.CyberObsidian
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AgnesViewModel

enum class AppNavTab(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val tag: String
) {
    AGENT("智能体", Icons.Filled.SmartToy, Icons.Outlined.SmartToy, "tab_agent"),
    IMAGE("图片重绘", Icons.Filled.Palette, Icons.Outlined.Palette, "tab_image"),
    VIDEO("视频拼接", Icons.Filled.Movie, Icons.Outlined.VideoLibrary, "tab_video"),
    GALLERY("作品集", Icons.Filled.FolderOpen, Icons.Outlined.FolderOpen, "tab_gallery"),
    SETTINGS("设置", Icons.Filled.Settings, Icons.Outlined.Settings, "tab_settings")
}

class MainActivity : ComponentActivity() {

    private val viewModel: AgnesViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                MainAppContent(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainAppContent(viewModel: AgnesViewModel) {
    var selectedTab by remember { mutableStateOf(AppNavTab.AGENT) }
    var pipelineInitialImage by remember { mutableStateOf<String?>(null) }
    var pipelineInitialPrompt by remember { mutableStateOf<String?>(null) }

    val toastMessage by viewModel.toastMessage.collectAsState()

    LaunchedEffect(toastMessage) {
        toastMessage?.let { msg ->
            // Clear toast after reading
            viewModel.clearToast()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .navigationBarsPadding()
                    .testTag("main_bottom_nav"),
                containerColor = Color(0xFF0F172A),
                tonalElevation = 2.dp
            ) {
                AppNavTab.values().forEach { tab ->
                    val isSelected = selectedTab == tab
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { selectedTab = tab },
                        icon = {
                            Icon(
                                imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                contentDescription = tab.title,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        label = {
                            Text(
                                text = tab.title,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = AgnesCyan,
                            selectedTextColor = AgnesCyan,
                            unselectedIconColor = Color(0xFF94A3B8),
                            unselectedTextColor = Color(0xFF94A3B8),
                            indicatorColor = AgnesViolet.copy(alpha = 0.25f)
                        ),
                        modifier = Modifier.testTag(tab.tag)
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(CyberObsidian)
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "TabContent"
            ) { tab ->
                when (tab) {
                    AppNavTab.AGENT -> AgentChatScreen(
                        viewModel = viewModel,
                        onNavigateToVideo = { selectedTab = AppNavTab.VIDEO },
                        onNavigateToImage = { selectedTab = AppNavTab.IMAGE }
                    )
                    AppNavTab.IMAGE -> ImageStudioScreen(
                        viewModel = viewModel,
                        onConvertToVideo = { imgUri, prompt ->
                            pipelineInitialImage = imgUri
                            pipelineInitialPrompt = prompt
                            selectedTab = AppNavTab.VIDEO
                        }
                    )
                    AppNavTab.VIDEO -> VideoPipelineScreen(
                        viewModel = viewModel,
                        initialImageUri = pipelineInitialImage,
                        initialPrompt = pipelineInitialPrompt
                    )
                    AppNavTab.GALLERY -> GalleryScreen(
                        viewModel = viewModel,
                        onOpenProjectInStudio = { project ->
                            if (project.type == ProjectType.VIDEO_SCRIPT_AND_STITCH) {
                                selectedTab = AppNavTab.VIDEO
                            } else {
                                selectedTab = AppNavTab.IMAGE
                            }
                        }
                    )
                    AppNavTab.SETTINGS -> SettingsScreen(
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}
