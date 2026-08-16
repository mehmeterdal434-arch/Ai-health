package com.example.health.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.health.model.HealthCategory
import com.example.health.model.HealthStatusLevel
import com.example.health.model.MetricEvaluation
import com.example.health.model.MetricType
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
                fontSize = 12.sp,
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
                contentDescription = "Kritik Uyarı",
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
                    text = "Lütfen vakit kaybetmeden bir hekime veya sağlık kuruluşuna danışınız.",
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
    disclaimerText: String = "Bu bilgiler tıbbi tavsiye yerine geçmez, endişelerin varsa doktoruna danış."
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
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
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
fun MetricCardItem(
    evaluation: MetricEvaluation,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accentColor = getMetricColor(evaluation.metricType)

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
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
                        text = evaluation.formattedValue,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = evaluation.differenceFromBaseline,
                        fontSize = 12.sp,
                        color = evaluation.statusLevel.color,
                        fontWeight = FontWeight.Medium
                    )
                }

                CategoryBadge(category = evaluation.category)
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
