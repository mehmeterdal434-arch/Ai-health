package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.health.ui.HealthViewModel
import com.example.health.ui.screens.*
import com.example.ui.theme.HealthAITheme

class MainActivity : ComponentActivity() {

    private val viewModel: HealthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val snackbarHostState = remember { SnackbarHostState() }

            // Health Connect permission launcher contract
            val requestPermissionLauncher = rememberLauncherForActivityResult(
                contract = PermissionController.createRequestPermissionResultContract()
            ) { grantedPermissions ->
                viewModel.refreshPermissionsState()
            }

            fun launchHealthConnectPermissions() {
                try {
                    if (viewModel.healthConnectManager.isHealthConnectAvailable()) {
                        requestPermissionLauncher.launch(viewModel.healthConnectManager.requiredPermissions)
                    } else {
                        try {
                            val settingsIntent = viewModel.healthConnectManager.getHealthConnectSettingsIntent()
                            startActivity(settingsIntent)
                        } catch (e2: Throwable) {
                            try {
                                startActivity(viewModel.healthConnectManager.getPlayStoreIntentForHealthConnect())
                            } catch (e3: Throwable) {
                                // No play store available in preview/emulator
                            }
                        }
                    }
                } catch (e: Throwable) {
                    try {
                        val settingsIntent = viewModel.healthConnectManager.getHealthConnectSettingsIntent()
                        startActivity(settingsIntent)
                    } catch (e2: Throwable) {
                        try {
                            startActivity(viewModel.healthConnectManager.getPlayStoreIntentForHealthConnect())
                        } catch (e3: Throwable) {
                            // Ignored
                        }
                    }
                }
            }

            LaunchedEffect(uiState.userFeedback) {
                uiState.userFeedback?.let { fb ->
                    snackbarHostState.showSnackbar(
                        message = fb.message,
                        duration = SnackbarDuration.Short
                    )
                    viewModel.dismissFeedback()
                }
            }

            HealthAITheme(themeMode = uiState.themeMode) {
                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("main_scaffold"),
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    bottomBar = {
                        NavigationBar(
                            modifier = Modifier
                                .windowInsetsPadding(WindowInsets.navigationBars)
                                .testTag("bottom_nav_bar")
                        ) {
                            NavigationBarItem(
                                selected = uiState.activeTab == 0,
                                onClick = { viewModel.selectTab(0) },
                                icon = {
                                    Icon(
                                        imageVector = if (uiState.activeTab == 0) Icons.Filled.Dashboard else Icons.Outlined.Dashboard,
                                        contentDescription = "Panel"
                                    )
                                },
                                label = { Text("Panel", fontSize = 10.sp) },
                                modifier = Modifier.testTag("nav_dashboard")
                            )
                            NavigationBarItem(
                                selected = uiState.activeTab == 1,
                                onClick = { viewModel.selectTab(1) },
                                icon = {
                                    Icon(
                                        imageVector = if (uiState.activeTab == 1) Icons.Filled.Insights else Icons.Outlined.Insights,
                                        contentDescription = "Metrikler"
                                    )
                                },
                                label = { Text("Metrikler", fontSize = 10.sp) },
                                modifier = Modifier.testTag("nav_detail")
                            )
                            NavigationBarItem(
                                selected = uiState.activeTab == 2,
                                onClick = { viewModel.selectTab(2) },
                                icon = {
                                    Icon(
                                        imageVector = if (uiState.activeTab == 2) Icons.Filled.AutoAwesome else Icons.Outlined.AutoAwesome,
                                        contentDescription = "AI Koç"
                                    )
                                },
                                label = { Text("AI Koç", fontSize = 10.sp) },
                                modifier = Modifier.testTag("nav_ai_summary")
                            )
                            NavigationBarItem(
                                selected = uiState.activeTab == 3,
                                onClick = { viewModel.selectTab(3) },
                                icon = {
                                    Icon(
                                        imageVector = if (uiState.activeTab == 3) Icons.Filled.MenuBook else Icons.Outlined.MenuBook,
                                        contentDescription = "Rehber"
                                    )
                                },
                                label = { Text("Rehber", fontSize = 10.sp) },
                                modifier = Modifier.testTag("nav_guide")
                            )
                            NavigationBarItem(
                                selected = uiState.activeTab == 4,
                                onClick = { viewModel.selectTab(4) },
                                icon = {
                                    Icon(
                                        imageVector = if (uiState.activeTab == 4) Icons.Filled.Settings else Icons.Outlined.Settings,
                                        contentDescription = "Ayarlar"
                                    )
                                },
                                label = { Text("Ayarlar", fontSize = 10.sp) },
                                modifier = Modifier.testTag("nav_settings")
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .windowInsetsPadding(WindowInsets.statusBars)
                    ) {
                        when (uiState.activeTab) {
                            0 -> HealthDashboardScreen(
                                uiState = uiState,
                                onSelectMetric = { viewModel.selectMetricForDetail(it) },
                                onSyncLiveHealthData = { viewModel.syncHealthData(isManualTrigger = true) },
                                onRequestPermissions = { launchHealthConnectPermissions() },
                                onNavigateToAiSummary = { viewModel.selectTab(2) },
                                onNavigateToGuide = { viewModel.selectTab(3) },
                                onNavigateToSettings = { viewModel.selectTab(4) },
                                onStartBreathing = { viewModel.setBreathingSheetOpen(true) },
                                onAddWater = { viewModel.addWater(it) },
                                onSetDateFilter = { viewModel.setDateFilter(it) },
                                onGenerateReportText = { viewModel.generateShareableHealthReport() }
                            )
                            1 -> MetricDetailScreen(
                                uiState = uiState,
                                onSelectMetric = { viewModel.selectMetricForDetail(it) },
                                onRefreshAiExplanation = { viewModel.generateMetricAiInsight(it, forceRefresh = true) },
                                onSetTrendDays = { viewModel.setTrendDaysCount(it) }
                            )
                            2 -> AiSummaryScreen(
                                uiState = uiState,
                                onRegenerateSummary = {
                                    uiState.summary?.let {
                                        viewModel.generateFullDayAiInsight(it, forceRefresh = true)
                                    }
                                },
                                onSendMessage = { viewModel.sendChatMessage(it) },
                                onGenerateReportText = { viewModel.generateShareableHealthReport() }
                            )
                            3 -> HealthGuideScreen(
                                uiState = uiState,
                                onNavigateToMetric = { viewModel.selectMetricForDetail(it) },
                                onStartBreathing = { viewModel.setBreathingSheetOpen(true) }
                            )
                            4 -> SettingsScreen(
                                uiState = uiState,
                                onUpdateProfile = { viewModel.updateProfile(it) },
                                onUpdateTheme = { viewModel.setThemeMode(it) },
                                onUpdateApiKey = { viewModel.updateCustomApiKey(it) },
                                onRequestPermissions = { launchHealthConnectPermissions() },
                                onSyncNow = { viewModel.syncHealthData(isManualTrigger = true) },
                                onClearLocalData = { viewModel.clearLocalData() }
                            )
                        }

                        // Modal Breathing Recovery Sheet
                        AnimatedVisibility(
                            visible = uiState.isBreathingSheetOpen,
                            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                        ) {
                            BreathingRecoveryScreen(
                                onClose = { viewModel.setBreathingSheetOpen(false) }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshPermissionsState()
    }
}
