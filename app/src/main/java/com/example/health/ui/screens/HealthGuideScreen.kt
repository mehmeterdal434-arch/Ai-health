package com.example.health.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.health.model.HealthKnowledgeBase
import com.example.health.model.HealthTopicGuide
import com.example.health.model.MetricType
import com.example.health.ui.HealthUiState
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthGuideScreen(
    uiState: HealthUiState,
    onNavigateToMetric: (MetricType) -> Unit,
    onStartBreathing: () -> Unit,
    modifier: Modifier = Modifier
) {
    val categories = listOf("Tümü", "Kardiyovasküler", "Toparlanma", "Zihin & Beden", "Solunum & Vital", "Aktivite", "Metabolizma")
    var selectedCategory by remember { mutableStateOf("Tümü") }
    var expandedTopicId by remember { mutableStateOf<String?>("heart_rate") }

    val filteredTopics = remember(selectedCategory) {
        if (selectedCategory == "Tümü") {
            HealthKnowledgeBase.topics
        } else {
            HealthKnowledgeBase.topics.filter { it.category == selectedCategory }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("health_guide_screen"),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Header
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_health_hero_1786913676242),
                    contentDescription = "Sağlık Rehberi Başlık",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    MaterialTheme.colorScheme.background.copy(alpha = 0.85f),
                                    MaterialTheme.colorScheme.background
                                )
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "📚 SAĞLIK ANSİKLOPEDİSİ & PRO İPUÇLARI",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Ne, Ne İşe Yarar?",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Samsung Health metriklerinin biyolojik anlamı ve pratik yaşam rehberi",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Quick Breathing Action Banner
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f)
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .clickable { onStartBreathing() }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.tertiaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🧘", fontSize = 24.sp)
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Hızlı Biyolojik Toparlanma",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Text(
                            text = "4-7-8 veya Kutu Nefesi ile nabzını ve stresini 3 dakikada dengele",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Başlat",
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
        }

        // Category Filter Chips
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { cat ->
                    val isSelected = cat == selectedCategory
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        leadingIcon = if (isSelected) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                        } else null
                    )
                }
            }
        }

        // Knowledge Topic Cards
        items(filteredTopics, key = { it.id }) { topic ->
            val isExpanded = expandedTopicId == topic.id
            HealthTopicCard(
                topic = topic,
                isExpanded = isExpanded,
                onToggleExpand = {
                    expandedTopicId = if (isExpanded) null else topic.id
                },
                onViewMetricDetail = {
                    when (topic.id) {
                        "heart_rate" -> onNavigateToMetric(MetricType.HEART_RATE)
                        "sleep_stages" -> onNavigateToMetric(MetricType.SLEEP)
                        "blood_oxygen" -> onNavigateToMetric(MetricType.SPO2)
                        "stress_hrv" -> onNavigateToMetric(MetricType.STRESS)
                        "daily_steps" -> onNavigateToMetric(MetricType.STEPS)
                        "active_calories" -> onNavigateToMetric(MetricType.CALORIES)
                    }
                },
                modifier = Modifier.padding(horizontal = 20.dp)
            )
        }
    }
}

@Composable
fun HealthTopicCard(
    topic: HealthTopicGuide,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onViewMetricDetail: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("topic_card_${topic.id}")
            .clickable { onToggleExpand() }
    ) {
        Column {
            // Visual Image Top for each topic
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
            ) {
                Image(
                    painter = painterResource(id = topic.imageRes),
                    contentDescription = topic.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                                )
                            )
                        )
                )
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(topic.emoji, fontSize = 28.sp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = topic.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = topic.subtitle,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = onToggleExpand,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f), CircleShape)
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Genişlet/Daralt",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Column(modifier = Modifier.padding(16.dp)) {
                // Short punchy summary
                Text(
                    text = topic.shortSummary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                AnimatedVisibility(
                    visible = isExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        modifier = Modifier.padding(top = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        // Biological role
                        DetailSection(
                            title = "🧬 Biyolojik Mekanizma ve Vücuttaki Rolü",
                            content = topic.biologicalRole,
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                            textColor = MaterialTheme.colorScheme.onSurface
                        )

                        // Ideal reference ranges
                        DetailSection(
                            title = "🎯 İdeal Klinik Değerler & Eşikler",
                            content = topic.idealRanges,
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f),
                            textColor = MaterialTheme.colorScheme.onSurface
                        )

                        // Pro Actionable Tips
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "💡 Nasıl İyileştirilir? (Pro Tavsiyeler)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            topic.tips.forEach { tip ->
                                Row(
                                    modifier = Modifier.padding(vertical = 2.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Text("• ", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    Text(tip, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }

                        // Myth buster
                        DetailSection(
                            title = "🧐 Doğru Bilinen Yanlışlar (Mythbuster)",
                            content = topic.mythBuster,
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f),
                            textColor = MaterialTheme.colorScheme.onSurface
                        )

                        // Samsung health info
                        DetailSection(
                            title = "⌚ Samsung Health'te Nasıl Takip Edilir?",
                            content = topic.whyItMattersInSamsungHealth,
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.35f),
                            textColor = MaterialTheme.colorScheme.onSurface
                        )

                        // View Metric Detail Action
                        Button(
                            onClick = onViewMetricDetail,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.QueryStats, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Bugünkü Verimi ve Grafikleri İncele", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailSection(
    title: String,
    content: String,
    containerColor: Color,
    textColor: Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(containerColor, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Text(
            text = title,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = content,
            fontSize = 12.sp,
            lineHeight = 17.sp,
            color = textColor
        )
    }
}
