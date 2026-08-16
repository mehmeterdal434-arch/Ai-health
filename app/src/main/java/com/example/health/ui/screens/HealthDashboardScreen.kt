package com.example.health.ui.screens

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.health.data.HealthScenario
import com.example.health.model.MetricType
import com.example.health.ui.HealthUiState
import com.example.health.ui.components.*
import com.example.ui.theme.*

@Composable
fun HealthDashboardScreen(
    uiState: HealthUiState,
    onSelectMetric: (MetricType) -> Unit,
    onApplyScenario: (HealthScenario) -> Unit,
    onNavigateToAiSummary: () -> Unit,
    onNavigateToGuide: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onStartBreathing: () -> Unit,
    onAddWater: (Int) -> Unit,
    onGenerateReportText: () -> String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val summary = uiState.summary

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("health_dashboard_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
    ) {
        // Header & Status
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
                        text = "Klinik Kural Motoru + Akıllı Açıklayıcı",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.clickable { onNavigateToSettings() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Sync,
                            contentDescription = "Senkronize",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Health Connect",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }

        // Quick Action Hub Row (Breathing, Guide, Chat, Share)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickActionButton(
                    icon = "🧘",
                    title = "Nefes",
                    subtitle = "Gevşeme",
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier.weight(1f),
                    onClick = onStartBreathing
                )
                QuickActionButton(
                    icon = "💬",
                    title = "AI Koç",
                    subtitle = "Sohbet",
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToAiSummary
                )
                QuickActionButton(
                    icon = "📚",
                    title = "Rehber",
                    subtitle = "Ne İşe Yarar?",
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToGuide
                )
                QuickActionButton(
                    icon = "📋",
                    title = "Rapor",
                    subtitle = "Paylaş",
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
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

        // Scenario Selector Chips
        item {
            Column {
                Text(
                    text = "Kural Motoru Test Senaryosu:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HealthScenario.values().forEach { scenario ->
                        val isSelected = scenario == uiState.currentScenario
                        FilterChip(
                            selected = isSelected,
                            onClick = { onApplyScenario(scenario) },
                            label = {
                                Text(
                                    text = scenario.title,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
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
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
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
                                    text = "GÜNLÜK SAĞLIK SKORU",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "%${summary.overallScore} Hazırlık",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                StatusBadge(status = summary.overallStatus)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Kural motoru tüm parametreleri referans aralıklarına göre denetledi.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 16.sp
                                )
                            }

                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(76.dp)
                                    .clip(CircleShape)
                                    .background(summary.overallStatus.color.copy(alpha = 0.15f))
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

        // Daily Hydration Tracking Widget
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
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
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(ColorSpO2.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("💧", fontSize = 20.sp)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Günlük Hidrasyon",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${uiState.waterIntakeMl} / 2500 ml",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ColorSpO2
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { (uiState.waterIntakeMl / 2500f).coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth(0.9f)
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = ColorSpO2,
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

        // AI Daily Briefing Teaser Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
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
                                text = "Gemini AI Günlük Değerlendirmesi",
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
                                text = "Kural motoru çıktısı yapay zeka ile çevriliyor...",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else {
                        val brief = uiState.fullDayAiInsight?.explanationText ?: "Günün sağlık özetini ve AI tavsiyelerini incelemek için dokunun."
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

        // Section Title for Metrics
        item {
            Text(
                text = "Günün Sağlık Metrikleri (Detay için dokunun)",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
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
                    item {
                        MetricCardItem(
                            evaluation = eval,
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
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
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
