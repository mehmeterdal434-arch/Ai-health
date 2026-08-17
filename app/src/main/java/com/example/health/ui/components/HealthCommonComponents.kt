package com.example.health.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.health.model.*
import com.example.health.ui.DateFilterMode
import com.example.ui.theme.*

fun getMetricIcon(type: MetricType): ImageVector = when (type) {
    MetricType.STEPS -> Icons.Filled.DirectionsWalk
    MetricType.HEART_RATE -> Icons.Filled.Favorite
    MetricType.SLEEP -> Icons.Filled.Bedtime
    MetricType.SPO2 -> Icons.Filled.Air
    MetricType.STRESS -> Icons.Filled.SelfImprovement
    MetricType.CALORIES -> Icons.Filled.LocalFireDepartment
}

fun getMetricColor(type: MetricType): Color = when (type) {
    MetricType.STEPS -> ColorSteps
    MetricType.HEART_RATE -> ColorHeartRate
    MetricType.SLEEP -> ColorSleep
    MetricType.SPO2 -> ColorSpO2
    MetricType.STRESS -> ColorStress
    MetricType.CALORIES -> ColorCalories
}

@Composable
fun StatusBadge(status: HealthStatusLevel, modifier: Modifier = Modifier) {
    Surface(
        color = status.color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(status.color)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = status.tag,
                color = status.color,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun CategoryBadge(category: HealthCategory, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Text(
            text = category.label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun CriticalAlertBanner(
    alerts: List<String>,
    modifier: Modifier = Modifier
) {
    if (alerts.isEmpty()) return

    Card(
        colors = CardDefaults.cardColors(
            containerColor = StatusCriticalRed.copy(alpha = 0.12f)
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Filled.Warning,
                contentDescription = "Kritik Tıbbi Uyarı",
                tint = StatusCriticalRed,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Tıbbi Değerlendirme Uyarısı",
                    color = StatusCriticalRed,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                alerts.forEach { alert ->
                    Text(
                        text = "• $alert",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Lütfen vakit kaybetmeden bir hekime veya acil sağlık kuruluşuna danışınız.",
                    color = StatusCriticalRed,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun MedicalDisclaimerCard(
    modifier: Modifier = Modifier,
    disclaimerText: String = "Bu uygulama ve analizler tıbbi teşhis/tedavi yerine geçmez. Sağlık endişelerinizde daima doktorunuza danışınız."
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = "Bilgi",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = disclaimerText,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                lineHeight = 15.sp
            )
        }
    }
}

@Composable
fun DateSelectorBar(
    currentFilter: DateFilterMode,
    onFilterSelected: (DateFilterMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            DateFilterMode.values().forEach { mode ->
                val isSelected = mode == currentFilter
                Surface(
                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onFilterSelected(mode) }
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Text(
                            text = mode.title,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MetricCardItem(
    evaluation: MetricEvaluation,
    progressRatio: Float? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accentColor = getMetricColor(evaluation.metricType)

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getMetricIcon(evaluation.metricType),
                            contentDescription = evaluation.metricType.displayName,
                            tint = accentColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = evaluation.metricType.displayName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                StatusBadge(status = evaluation.statusLevel)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = if (evaluation.hasMeasuredData) evaluation.formattedValue else "Ölçüm Yok",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = evaluation.differenceFromBaseline,
                        fontSize = 12.sp,
                        color = if (evaluation.hasMeasuredData) evaluation.statusLevel.color else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }

                CategoryBadge(category = evaluation.category)
            }

            if (progressRatio != null && progressRatio > 0f) {
                Spacer(modifier = Modifier.height(10.dp))
                val animatedProgress by animateFloatAsState(
                    targetValue = progressRatio.coerceIn(0f, 1f),
                    animationSpec = tween(durationMillis = 600),
                    label = "metric_progress"
                )
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    color = accentColor,
                    trackColor = accentColor.copy(alpha = 0.15f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = evaluation.clinicalSummary,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 16.sp,
                maxLines = 2
            )
        }
    }
}

@Composable
fun InteractiveTrendChart(
    points: List<HistoricalTrendPoint>,
    metricType: MetricType,
    modifier: Modifier = Modifier,
    onPointSelected: ((HistoricalTrendPoint) -> Unit)? = null
) {
    val accentColor = getMetricColor(metricType)
    var selectedIndex by remember { mutableStateOf<Int?>(null) }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Geçmiş Eğilim Grafiği",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (selectedIndex != null && selectedIndex in points.indices) {
                    val p = points[selectedIndex!!]
                    Text(
                        text = "${p.label}: ${p.formattedValue}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                } else {
                    Text(
                        text = "${points.size} Günlük Veri",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (points.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Grafik için yeterli geçmiş veri bulunmuyor.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                }
            } else {
                val maxVal = points.maxOfOrNull { it.value }?.takeIf { it > 0f } ?: 100f
                val minVal = 0f

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(points) {
                                detectTapGestures { offset ->
                                    if (points.isNotEmpty()) {
                                        val barWidth = size.width / points.size.coerceAtLeast(1)
                                        val tappedIndex = if (barWidth > 0f) (offset.x / barWidth).toInt().coerceIn(0, points.size - 1) else 0
                                        selectedIndex = tappedIndex
                                        onPointSelected?.invoke(points[tappedIndex])
                                    }
                                }
                            }
                    ) {
                        val width = size.width
                        val height = size.height
                        val count = points.size.coerceAtLeast(1)
                        val barSpacing = width / count

                        // Draw Grid lines
                        val gridCount = 3
                        for (i in 0..gridCount) {
                            val y = height * (i.toFloat() / gridCount)
                            drawLine(
                                color = Color.Gray.copy(alpha = 0.12f),
                                start = Offset(0f, y),
                                end = Offset(width, y),
                                strokeWidth = 1.dp.toPx()
                            )
                        }

                        // Draw Bars or Curved Line
                        points.forEachIndexed { index, point ->
                            val normalized = if (maxVal > minVal) ((point.value - minVal) / (maxVal - minVal)).coerceIn(0f, 1f) else 0f
                            val barHeight = (height * 0.75f * normalized).coerceAtLeast(4.dp.toPx())
                            val x = index * barSpacing + (barSpacing * 0.2f)
                            val y = height - barHeight
                            val barW = (barSpacing * 0.6f).coerceAtLeast(2.dp.toPx())

                            val isSelected = selectedIndex == index
                            val barColor = if (isSelected) accentColor else accentColor.copy(alpha = 0.65f)

                            drawRoundRect(
                                color = barColor,
                                topLeft = Offset(x, y),
                                size = Size(barW, barHeight),
                                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // X-Axis Labels
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    points.forEachIndexed { index, point ->
                        if (points.size <= 7 || index % (points.size / 5).coerceAtLeast(1) == 0) {
                            Text(
                                text = point.label,
                                fontSize = 10.sp,
                                color = if (selectedIndex == index) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = if (selectedIndex == index) FontWeight.Bold else FontWeight.Normal,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AppPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    isLoading: Boolean = false
) {
    Button(
        onClick = onClick,
        enabled = enabled && !isLoading,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp
            )
        } else {
            if (icon != null) {
                Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(text = text, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        }
    }
}

@Composable
fun AppSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.primary
        ),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
    ) {
        if (icon != null) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(text = text, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    }
}

@Composable
fun SectionHeader(title: String, subtitle: String? = null, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (subtitle != null) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
