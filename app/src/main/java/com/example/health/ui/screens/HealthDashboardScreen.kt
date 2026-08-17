package com.example.health.ui.screens

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.health.data.HealthConnectAvailability
import com.example.health.model.MetricType
import com.example.health.ui.DateFilterMode
import com.example.health.ui.HealthUiState
import com.example.health.ui.components.*
import com.example.ui.theme.*
import java.util.Locale

@Composable
fun HealthDashboardScreen(
    uiState: HealthUiState,
    onSelectMetric: (MetricType) -> Unit,
    onSyncLiveHealthData: () -> Unit,
    onRequestPermissions: () -> Unit,
    onNavigateToAiSummary: () -> Unit,
    onNavigateToGuide: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onStartBreathing: () -> Unit,
    onAddWater: (Int) -> Unit,
    onSetDateFilter: (DateFilterMode) -> Unit,
    onGenerateReportText: () -> String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val summary = uiState.summary
    val record = uiState.currentRecord

    // Rotation animation for sync icon when syncing
    val infiniteTransition = rememberInfiniteTransition(label = "sync_rotation")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sync_angle"
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("health_dashboard_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 14.dp, bottom = 28.dp)
    ) {
        // Top App Bar & Sync Trigger
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Samsung Health AI",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Sağlık Takip & Akıllı Analiz Paneli",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Sync button
                FilledTonalButton(
                    onClick = { onSyncLiveHealthData() },
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("btn_sync_health")
                ) {
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = "Eşitle",
                        modifier = Modifier
                            .size(16.dp)
                            .rotate(if (uiState.isSyncing) rotationAngle else 0f)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (uiState.isSyncing) "Eşitleniyor..." else "Şimdi Eşitle",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Date Filter Switcher (Bugün | Dün | 7 Gün | 30 Gün)
        item {
            DateSelectorBar(
                currentFilter = uiState.selectedFilterMode,
                onFilterSelected = onSetDateFilter
            )
        }

        // Health Connect & Source Status Banner
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (uiState.permissionState.anyGranted)
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                    else
                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(
                                    if (uiState.permissionState.anyGranted) StatusGoodGreen.copy(alpha = 0.15f)
                                    else MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (uiState.permissionState.anyGranted) Icons.Default.CheckCircle else Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (uiState.permissionState.anyGranted) StatusGoodGreen else MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (uiState.permissionState.allGranted) "Health Connect: Bağlı (Samsung Health)"
                                else if (uiState.permissionState.anyGranted) "Health Connect: Kısmi İzin (${uiState.permissionState.grantedCount}/${uiState.permissionState.totalCount})"
                                else "Health Connect: İzin Bekleniyor",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Son Eşitleme: ${uiState.lastSyncTime}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (!uiState.permissionState.allGranted) {
                        Button(
                            onClick = { onRequestPermissions() },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("btn_grant_permissions")
                        ) {
                            Text("İzin Ver", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Critical Alerts Banner if any
        if (summary != null && summary.hasCriticalConditions) {
            item {
                CriticalAlertBanner(alerts = summary.criticalAlerts)
            }
        }

        // Overall Health Readiness Hero Card
        if (summary != null) {
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                        MaterialTheme.colorScheme.surface
                                    )
                                )
                            )
                            .padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "GÜNLÜK SAĞLIK VE HAZIRLIK SKORU",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.8.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "%${summary.overallScore} Hazırlık",
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                StatusBadge(status = summary.overallStatus)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Biyometrik verilerin klinik algoritma puanı.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(76.dp)
                                    .clip(CircleShape)
                                    .background(summary.overallStatus.color.copy(alpha = 0.12f))
                            ) {
                                CircularProgressIndicator(
                                    progress = { summary.overallScore / 100f },
                                    modifier = Modifier.size(68.dp),
                                    color = summary.overallStatus.color,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                    strokeWidth = 6.dp
                                )
                                Text(
                                    text = "${summary.overallScore}",
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }

        // Daily Activity Summary Highlights (Distance, Active Minutes, Calories, Steps)
        if (record != null) {
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        ActivityHighlightItem(
                            icon = "👟",
                            value = "${record.steps}",
                            label = "Adım",
                            color = ColorSteps
                        )
                        ActivityHighlightItem(
                            icon = "📍",
                            value = String.format(Locale.US, "%.1f km", record.distanceMeters / 1000.0),
                            label = "Mesafe",
                            color = ColorDistance
                        )
                        ActivityHighlightItem(
                            icon = "⏱",
                            value = "${record.activeMinutes} dk",
                            label = "Aktif Süre",
                            color = ColorStress
                        )
                        ActivityHighlightItem(
                            icon = "🔥",
                            value = "${record.activeCaloriesKcal} kcal",
                            label = "Kalori",
                            color = ColorCalories
                        )
                    }
                }
            }
        }

        // Daily Hydration Tracking Widget
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(ColorWater.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("💧", fontSize = 18.sp)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Günlük Su Tüketimi",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${uiState.waterIntakeMl} / ${uiState.waterGoalMl} ml",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ColorWater
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { (uiState.waterIntakeMl.toFloat() / uiState.waterGoalMl.toFloat()).coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth(0.9f)
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = ColorWater,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilledTonalButton(
                            onClick = { onAddWater(250) },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("+250ml", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        FilledTonalButton(
                            onClick = { onAddWater(500) },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("+500ml", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // AI Briefing Teaser Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToAiSummary() }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.AutoAwesome,
                                contentDescription = "AI",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Gemini AI Günlük Analizi",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Icon(
                            imageVector = Icons.Filled.ChevronRight,
                            contentDescription = "Detay",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (uiState.isAiGenerating) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Sağlık verileri analiz ediliyor...",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else {
                        val brief = uiState.fullDayAiInsight?.explanationText ?: "Günün sağlık sentezini ve AI önerilerini incelemek için dokunun."
                        Text(
                            text = brief,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }

        // Quick Hub Row (Nefes, AI Koç, Rehber, Rapor)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickActionButton(
                    icon = "🧘",
                    title = "Nefes",
                    subtitle = "Gevşeme",
                    containerColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.weight(1f),
                    onClick = onStartBreathing
                )
                QuickActionButton(
                    icon = "💬",
                    title = "AI Koç",
                    subtitle = "Sohbet",
                    containerColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToAiSummary
                )
                QuickActionButton(
                    icon = "📚",
                    title = "Rehber",
                    subtitle = "Metrikler",
                    containerColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToGuide
                )
                QuickActionButton(
                    icon = "📋",
                    title = "Rapor",
                    subtitle = "Paylaş",
                    containerColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        val report = onGenerateReportText()
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, report)
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Sağlık Raporu"))
                    }
                )
            }
        }

        // Section Title for Metrics
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Biyometrik Metrikler",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Grafik & Detay İçin Dokun",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 6 Metric Cards
        if (summary != null) {
            val metrics = listOf(
                MetricType.STEPS,
                MetricType.HEART_RATE,
                MetricType.SLEEP,
                MetricType.SPO2,
                MetricType.STRESS,
                MetricType.CALORIES
            )

            metrics.forEach { metricType ->
                val eval = summary.evaluations[metricType]
                if (eval != null) {
                    val progressRatio: Float? = when (metricType) {
                        MetricType.STEPS -> (record?.steps?.toFloat() ?: 0f) / uiState.profile.stepGoal.toFloat()
                        MetricType.SLEEP -> ((record?.sleepHours?.toFloat() ?: 0f) / uiState.profile.sleepBaselineHours.toFloat())
                        MetricType.CALORIES -> ((record?.activeCaloriesKcal?.toFloat() ?: 0f) / uiState.profile.activeCalorieGoalKcal.toFloat())
                        else -> null
                    }

                    item {
                        MetricCardItem(
                            evaluation = eval,
                            progressRatio = progressRatio,
                            onClick = { onSelectMetric(metricType) }
                        )
                    }
                }
            }
        }

        // Medical Disclaimer Card
        item {
            MedicalDisclaimerCard()
        }
    }
}

@Composable
fun ActivityHighlightItem(
    icon: String,
    value: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = icon, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun QuickActionButton(
    icon: String,
    title: String,
    subtitle: String,
    containerColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = containerColor,
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 1.dp,
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(icon, fontSize = 20.sp)
            Spacer(modifier = Modifier.height(2.dp))
            Text(title, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
