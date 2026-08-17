package com.example.health.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
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
    onUpdateTheme: (AppThemeMode) -> Unit,
    onUpdateApiKey: (String) -> Unit,
    onRequestPermissions: () -> Unit,
    onSyncNow: () -> Unit,
    onClearLocalData: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var stepGoalText by remember(uiState.profile.stepGoal) { mutableStateOf(uiState.profile.stepGoal.toString()) }
    var sleepGoalText by remember(uiState.profile.sleepBaselineHours) { mutableStateOf(uiState.profile.sleepBaselineHours.toString()) }
    var hrBaselineText by remember(uiState.profile.restingHeartRateBaselineBpm) { mutableStateOf(uiState.profile.restingHeartRateBaselineBpm.toString()) }
    var calorieGoalText by remember(uiState.profile.activeCalorieGoalKcal) { mutableStateOf(uiState.profile.activeCalorieGoalKcal.toString()) }
    var waterGoalText by remember(uiState.profile.waterGoalMl) { mutableStateOf(uiState.profile.waterGoalMl.toString()) }

    var apiKeyText by remember(uiState.customApiKey) { mutableStateOf(uiState.customApiKey) }
    var showApiKey by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }
    var saveSuccessMessage by remember { mutableStateOf<String?>(null) }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            icon = { Icon(Icons.Default.DeleteForever, contentDescription = null, tint = StatusCriticalRed) },
            title = { Text("Yerel Önbelleği Temizle", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Uygulamanın yerel veritabanındaki günlük kayıtlar ve AI özetleri temizlenecektir.\n\n" +
                    "Not: Samsung Health ve Health Connect'teki orijinal sağlık verileriniz SİLİNMEZ. Dilediğiniz zaman 'Eşitle' butonuna basarak tekrar yükleyebilirsiniz.",
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onClearLocalData()
                        showClearDialog = false
                        saveSuccessMessage = "Yerel veritabanı temizlendi."
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusCriticalRed)
                ) {
                    Text("Temizle")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("İptal")
                }
            }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("settings_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
    ) {
        // Header
        item {
            Column {
                Text(
                    text = "Ayarlar & Yapılandırma",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Görünüm, Kişisel Hedefler ve Sağlık Entegrasyonu",
                    fontSize = 12.sp,
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

        // 1. APPEARANCE & THEME MODE CARD
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.DarkMode,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Görünüm & Tema",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        AppThemeMode.values().forEach { mode ->
                            val isSelected = uiState.themeMode == mode
                            Surface(
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { onUpdateTheme(mode) }
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                    Text(
                                        text = mode.title,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. SAMSUNG HEALTH & HEALTH CONNECT INTEGRATION CARD
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Sync,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Samsung Health & Health Connect",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        FilledTonalButton(
                            onClick = {
                                onSyncNow()
                                saveSuccessMessage = "Sağlık verileri eşitlendi."
                            },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Şimdi Eşitle", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Health Connect Platformu", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
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
                        Text("Samsung Health Durumu", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                        Text(
                            if (uiState.isSamsungHealthInstalled) "Cihazda Yüklü" else "Yüklü Değil",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (uiState.isSamsungHealthInstalled) StatusGoodGreen else StatusAttentionYellow
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Biyometrik İzinler", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                        Text(
                            if (uiState.permissionState.allGranted) "Tam Yetkili (${uiState.permissionState.grantedCount}/${uiState.permissionState.totalCount})"
                            else if (uiState.permissionState.anyGranted) "Kısmi İzinli (${uiState.permissionState.grantedCount}/${uiState.permissionState.totalCount})"
                            else "İzin Bekleniyor",
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
                            onClick = { onRequestPermissions() },
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
                            Text("Health Connect", fontSize = 12.sp)
                        }
                    }

                    if (uiState.isSamsungHealthInstalled) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = {
                                try {
                                    val launchIntent = context.packageManager.getLaunchIntentForPackage("com.sec.android.app.shealth")
                                    if (launchIntent != null) {
                                        context.startActivity(launchIntent)
                                    }
                                } catch (e: Exception) {
                                    // ignore
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Launch, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Samsung Health Uygulamasını Aç", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // 3. PERSONAL TARGETS & RULE ENGINE THRESHOLDS
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
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
                            text = "Kişisel Hedefler & Klinik Eşikler",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Sağlık ve hazırlık skorunuzu hesaplayan kural motoru bu değerleri baz alır.",
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
                        value = waterGoalText,
                        onValueChange = { waterGoalText = it },
                        label = { Text("Günlük Su Hedefi (ml)") },
                        leadingIcon = { Text("💧", modifier = Modifier.padding(start = 12.dp)) },
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
                                activeCalorieGoalKcal = calorieGoalText.toIntOrNull() ?: 500,
                                waterGoalMl = waterGoalText.toIntOrNull() ?: 2500
                            )
                            onUpdateProfile(newProfile)
                            saveSuccessMessage = "Kişisel hedefler başarıyla güncellendi."
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Hedefleri Kaydet & Yeniden Hesapla")
                    }
                }
            }
        }

        // 4. GEMINI AI API CONFIGURATION
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
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
                            text = "Gemini API Yapılandırması",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Sağlık asistanı analizleri Gemini 2.5 Flash ile üretilir. Özel API anahtarınız varsa buradan kaydedebilirsiniz.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = apiKeyText,
                        onValueChange = { apiKeyText = it },
                        label = { Text("Gemini API Anahtarı") },
                        visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showApiKey = !showApiKey }) {
                                Icon(
                                    imageVector = if (showApiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (showApiKey) "Gizle" else "Göster"
                                )
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            onUpdateApiKey(apiKeyText.trim())
                            saveSuccessMessage = "Gemini API Anahtarı kaydedildi."
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("API Anahtarını Kaydet")
                    }
                }
            }
        }

        // 5. LOCAL DATA STORAGE MANAGEMENT
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Storage,
                            contentDescription = null,
                            tint = StatusAttentionYellow,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Veri Yönetimi & Gizlilik",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Uygulama yerel SQLite veritabanındaki kayıtları temizleyebilirsiniz. Samsung Health verileriniz etkilenmez.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedButton(
                        onClick = { showClearDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusCriticalRed),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Yerel Önbelleği ve Kayıtları Temizle")
                    }
                }
            }
        }

        // Disclaimer
        item {
            MedicalDisclaimerCard()
        }
    }
}
