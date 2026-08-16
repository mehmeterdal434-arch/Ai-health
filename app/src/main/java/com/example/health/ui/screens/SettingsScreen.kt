package com.example.health.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.health.data.HealthConnectAvailability
import com.example.health.engine.UserHealthProfile
import com.example.health.ui.HealthUiState
import com.example.health.ui.components.MedicalDisclaimerCard
import com.example.ui.theme.*

@Composable
fun SettingsScreen(
    uiState: HealthUiState,
    onUpdateProfile: (UserHealthProfile) -> Unit,
    onUpdateApiKey: (String) -> Unit,
    onRequestPermissions: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var stepGoalText by remember(uiState.profile.stepGoal) { mutableStateOf(uiState.profile.stepGoal.toString()) }
    var sleepGoalText by remember(uiState.profile.sleepBaselineHours) { mutableStateOf(uiState.profile.sleepBaselineHours.toString()) }
    var hrBaselineText by remember(uiState.profile.restingHeartRateBaselineBpm) { mutableStateOf(uiState.profile.restingHeartRateBaselineBpm.toString()) }
    var calorieGoalText by remember(uiState.profile.activeCalorieGoalKcal) { mutableStateOf(uiState.profile.activeCalorieGoalKcal.toString()) }

    var apiKeyText by remember(uiState.customApiKey) { mutableStateOf(uiState.customApiKey) }
    var showApiKey by remember { mutableStateOf(false) }
    var saveSuccessMessage by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
    ) {
        // Header
        item {
            Column {
                Text(
                    text = "Ayarlar & Entegrasyon",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Samsung Health, Health Connect ve Kural Eşikleri",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Save confirmation toast/banner
        if (saveSuccessMessage != null) {
            item {
                Surface(
                    color = StatusGoodGreen.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = StatusGoodGreen
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = saveSuccessMessage ?: "",
                            color = StatusGoodGreen,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // Samsung Health & Health Connect Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Sync,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Samsung Health / Health Connect",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Status rows
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Health Connect Durumu", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                        val hcStatus = when (uiState.healthConnectAvailability) {
                            HealthConnectAvailability.INSTALLED -> "Kurulu ve Hazır"
                            HealthConnectAvailability.NOT_INSTALLED -> "Yüklü Değil"
                            HealthConnectAvailability.NOT_SUPPORTED -> "Desteklenmiyor"
                        }
                        Text(hcStatus, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = StatusGoodGreen)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Veri İzinleri", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                        Text(
                            if (uiState.permissionState.allGranted) "Tam Yetki Verildi" else "Kısmi İzin",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (uiState.permissionState.allGranted) StatusGoodGreen else StatusAttentionYellow
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                onRequestPermissions()
                                saveSuccessMessage = "Health Connect izinleri başarıyla güncellendi."
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("İzinleri Onayla", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                try {
                                    val intent = Intent("androidx.health.ACTION_HEALTH_CONNECT_SETTINGS").apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    val playStoreIntent = Intent(Intent.ACTION_VIEW).apply {
                                        data = Uri.parse("https://play.google.com/store/apps/details?id=com.google.android.apps.healthdata")
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(playStoreIntent)
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Sistem Ayarları", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Rule Engine Thresholds Customizer Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Tune,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Kişisel Hedefler ve Eşik Değerleri",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Kural motoru hesaplamalarında referans alınacak kişisel değerlerinizi özelleştirin.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = stepGoalText,
                        onValueChange = { stepGoalText = it },
                        label = { Text("Günlük Adım Hedefi") },
                        leadingIcon = { Icon(Icons.Default.DirectionsWalk, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = sleepGoalText,
                        onValueChange = { sleepGoalText = it },
                        label = { Text("Hedef Uyku Süresi (Saat)") },
                        leadingIcon = { Icon(Icons.Default.Bedtime, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = hrBaselineText,
                        onValueChange = { hrBaselineText = it },
                        label = { Text("Dinlenik Nabız Tabanı (BPM)") },
                        leadingIcon = { Icon(Icons.Default.Favorite, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = calorieGoalText,
                        onValueChange = { calorieGoalText = it },
                        label = { Text("Aktif Kalori Hedefi (kcal)") },
                        leadingIcon = { Icon(Icons.Default.LocalFireDepartment, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            val newProfile = UserHealthProfile(
                                stepGoal = stepGoalText.toIntOrNull() ?: 8000,
                                sleepBaselineHours = sleepGoalText.toDoubleOrNull() ?: 7.5,
                                restingHeartRateBaselineBpm = hrBaselineText.toIntOrNull() ?: 65,
                                activeCalorieGoalKcal = calorieGoalText.toIntOrNull() ?: 500
                            )
                            onUpdateProfile(newProfile)
                            saveSuccessMessage = "Kişisel eşikler ve hedefler başarıyla güncellendi."
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Eşikleri Kaydet ve Kural Motorunu Güncelle")
                    }
                }
            }
        }

        // Gemini API Key Management Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Key,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Gemini API Anahtarı Yönetimi",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Doğal dilde AI açıklamaları için Gemini API anahtarı kullanılır. Anahtar girilmezse kural motoru yerel offline açıklayıcı ile çalışır.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = apiKeyText,
                        onValueChange = { apiKeyText = it },
                        label = { Text("Gemini API Key") },
                        visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showApiKey = !showApiKey }) {
                                Icon(
                                    imageVector = if (showApiKey) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = null
                                )
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            onUpdateApiKey(apiKeyText)
                            saveSuccessMessage = "API Anahtarı başarıyla kaydedildi."
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("API Anahtarını Kaydet")
                    }
                }
            }
        }

        // Step-by-Step Test Guide Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.HelpOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Nasıl Test Edilir?",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val steps = listOf(
                        "1. Ana ekrandaki senaryo butonlarına (Sporcu, Uykusuz, Kritik) basarak kural motorunun farklı eşiklerdeki tepkisini anında görün.",
                        "2. Herhangi bir metrik kartına (Adım, Nabız, Uyku vb.) tıklayarak 7 günlük geçmiş trend grafiğini ve kural motoru eşik analizini inceleyin.",
                        "3. 'AI Günlük Özeti' sekmesinden Gemini AI'ın tüm kural kategorilerini sentezlediği genel sağlık raporunu okuyun.",
                        "4. 'Kritik Sağlık Uyarısı' senaryosunu seçerek tehlikeli eşiklerdeki otomatik hekim yönlendirme uyarısını test edin."
                    )

                    steps.forEach { stepText ->
                        Text(
                            text = stepText,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 17.sp,
                            modifier = Modifier.padding(vertical = 3.dp)
                        )
                    }
                }
            }
        }

        // Medical Disclaimer
        item {
            MedicalDisclaimerCard()
        }
    }
}
