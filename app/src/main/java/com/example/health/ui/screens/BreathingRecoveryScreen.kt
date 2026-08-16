package com.example.health.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

enum class BreathingTechnique(
    val title: String,
    val description: String,
    val stages: List<BreathingStage>
) {
    RELAX_4_7_8(
        "4-7-8 Derin Gevşeme",
        "Vagus sinirini aktive ederek kalp atış hızını düşürür ve uykuya hazırlar.",
        listOf(
            BreathingStage("Nefes Al (Burundan)", 4, StatusLowBlue),
            BreathingStage("Nefesini Tut", 7, StatusAttentionYellow),
            BreathingStage("Nefes Ver (Ağızdan)", 8, StatusOptimalTeal)
        )
    ),
    BOX_BREATHING(
        "Kutu Nefesi (Box 4-4-4-4)",
        "Zihinsel netliği artırır ve akut iş/günlük stresi dengeler.",
        listOf(
            BreathingStage("Nefes Al", 4, StatusGoodGreen),
            BreathingStage("Nefesini Tut", 4, StatusAttentionYellow),
            BreathingStage("Nefes Ver", 4, StatusOptimalTeal),
            BreathingStage("Boşlukta Bekle", 4, StatusLowBlue)
        )
    )
}

data class BreathingStage(
    val label: String,
    val durationSeconds: Int,
    val color: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BreathingRecoveryScreen(
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTechnique by remember { mutableStateOf(BreathingTechnique.RELAX_4_7_8) }
    var isRunning by remember { mutableStateOf(false) }
    var currentStageIndex by remember { mutableStateOf(0) }
    var secondsLeftInStage by remember { mutableStateOf(4) }
    var completedCycles by remember { mutableStateOf(0) }

    val currentStage = selectedTechnique.stages[currentStageIndex]

    // Timer loop
    LaunchedEffect(isRunning, selectedTechnique, currentStageIndex) {
        if (!isRunning) return@LaunchedEffect
        secondsLeftInStage = selectedTechnique.stages[currentStageIndex].durationSeconds

        while (isActive && isRunning) {
            delay(1000L)
            if (secondsLeftInStage > 1) {
                secondsLeftInStage--
            } else {
                // Move to next stage
                val nextIndex = (currentStageIndex + 1) % selectedTechnique.stages.size
                if (nextIndex == 0) {
                    completedCycles++
                }
                currentStageIndex = nextIndex
                secondsLeftInStage = selectedTechnique.stages[nextIndex].durationSeconds
            }
        }
    }

    // Animation scale for expanding/contracting circle
    val targetScale = when {
        !isRunning -> 1.0f
        currentStage.label.contains("Nefes Al") -> 1.35f
        currentStage.label.contains("Tut") -> 1.35f
        currentStage.label.contains("Nefes Ver") -> 0.75f
        else -> 0.75f
    }

    val animatedScale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = tween(
            durationMillis = if (isRunning) currentStage.durationSeconds * 1000 else 600,
            easing = LinearOutSlowInEasing
        ),
        label = "breathing_scale"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Biyolojik Toparlanma & Nefes",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Kapat")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 12.dp)
                .testTag("breathing_recovery_screen"),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Technique Selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                BreathingTechnique.values().forEach { tech ->
                    val isSelected = tech == selectedTechnique
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable {
                                if (selectedTechnique != tech) {
                                    selectedTechnique = tech
                                    isRunning = false
                                    currentStageIndex = 0
                                    completedCycles = 0
                                    secondsLeftInStage = tech.stages[0].durationSeconds
                                }
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (tech == BreathingTechnique.RELAX_4_7_8) "4-7-8 Rahatlama" else "Kutu Nefesi",
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Text(
                text = selectedTechnique.description,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            // Center Interactive Breathing Visualizer
            Box(
                modifier = Modifier
                    .size(260.dp)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                // Outer ambient glow
                Box(
                    modifier = Modifier
                        .size(240.dp)
                        .scale(animatedScale)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    currentStage.color.copy(alpha = 0.35f),
                                    currentStage.color.copy(alpha = 0.05f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                // Inner Main Circle
                Box(
                    modifier = Modifier
                        .size(170.dp)
                        .scale(animatedScale)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    currentStage.color,
                                    currentStage.color.copy(alpha = 0.7f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = if (isRunning) "$secondsLeftInStage" else "▶",
                            fontSize = if (isRunning) 44.sp else 36.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Text(
                            text = if (isRunning) currentStage.label else "Başlamak İçin Dokun",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.9f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }
                }
            }

            // Stats / Cycle counter
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Tamamlanan Döngü", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("$completedCycles", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    VerticalDivider(modifier = Modifier.height(30.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Hedeflenen Süre", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("3 Dakika", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }

            // Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        isRunning = false
                        currentStageIndex = 0
                        completedCycles = 0
                        secondsLeftInStage = selectedTechnique.stages[0].durationSeconds
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Sıfırla")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Sıfırla")
                }

                Button(
                    onClick = { isRunning = !isRunning },
                    modifier = Modifier
                        .weight(2f)
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRunning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isRunning) "Duraklat" else "Başlat"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isRunning) "Duraklat" else "Nefes Egzersizini Başlat",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
