package com.example.health.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.health.ai.GeminiExplainer
import com.example.health.data.*
import com.example.health.data.local.AppDatabase
import com.example.health.engine.RuleEngine
import com.example.health.engine.UserHealthProfile
import com.example.health.model.*
import com.example.ui.theme.AppThemeMode
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

enum class DateFilterMode(val title: String) {
    TODAY("Bugün"),
    YESTERDAY("Dün"),
    DAYS_7("Son 7 Gün"),
    DAYS_30("Son 30 Gün")
}

data class UserFeedbackMessage(
    val message: String,
    val isError: Boolean = false,
    val id: Long = System.currentTimeMillis()
)

data class HealthUiState(
    val currentRecord: DailyHealthRecord? = null,
    val summary: StructuredHealthSummary? = null,
    val profile: UserHealthProfile = UserHealthProfile(),
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val selectedDate: String = "",
    val selectedFilterMode: DateFilterMode = DateFilterMode.TODAY,
    val isAiGenerating: Boolean = false,
    val isSyncing: Boolean = false,
    val lastSyncTime: String = "Henüz eşitlenmedi",
    val syncSuccessMessage: String? = null,
    val syncErrorMessage: String? = null,
    val fullDayAiInsight: AiGeneratedInsight? = null,
    val metricAiInsights: Map<MetricType, AiGeneratedInsight> = emptyMap(),
    val historicalTrend: List<HistoricalTrendPoint> = emptyList(),
    val trendDaysCount: Int = 7,
    val selectedMetricForDetail: MetricType = MetricType.STEPS,
    val healthConnectAvailability: HealthConnectAvailability = HealthConnectAvailability.INSTALLED,
    val permissionState: HealthPermissionState = HealthPermissionState(),
    val isSamsungHealthInstalled: Boolean = true,
    val customApiKey: String = "",
    val activeTab: Int = 0, // 0: Dashboard, 1: Detail, 2: AI Coach/Summary, 3: Guide, 4: Settings
    val waterIntakeMl: Int = 1600,
    val waterGoalMl: Int = 2500,
    val chatMessages: List<ChatMessage> = listOf(
        ChatMessage(
            text = "Merhaba! Ben senin Samsung Health AI Sağlık Asistanınım. 🩺 Canlı Samsung Health & Health Connect verilerin ve klinik kural motorumuz ışığında uyku, nabız, antrenman veya toparlanma durumun hakkında merak ettiğin her şeyi sorabilirsin.",
            isUser = false
        )
    ),
    val isChatGenerating: Boolean = false,
    val isBreathingSheetOpen: Boolean = false,
    val userFeedback: UserFeedbackMessage? = null
)

class HealthViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    val preferences = AppPreferences(application)
    val healthConnectManager = HealthConnectManager(application)
    val repository = HealthDataRepository(db.healthDao(), healthConnectManager)
    val geminiExplainer = GeminiExplainer(application)

    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val isoDateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    private var trendJob: Job? = null
    private var syncJob: Job? = null
    private var aiInsightJob: Job? = null
    private var metricAiJob: Job? = null
    private var chatJob: Job? = null
    private var dateJob: Job? = null

    private val _uiState = MutableStateFlow(
        HealthUiState(
            themeMode = preferences.loadThemeMode(),
            profile = preferences.loadProfile(),
            selectedDate = LocalDate.now().format(isoDateFormatter)
        )
    )
    val uiState: StateFlow<HealthUiState> = _uiState.asStateFlow()

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            try {
                val availability = healthConnectManager.checkHealthConnectAvailability()
                val isSamsungInstalled = healthConnectManager.isSamsungHealthInstalled()
                val permState = healthConnectManager.getPermissionState()
                val currentApiKey = preferences.getCustomApiKey()
                val savedProfile = preferences.loadProfile()
                val savedTheme = preferences.loadThemeMode()

                _uiState.update {
                    it.copy(
                        healthConnectAvailability = availability,
                        isSamsungHealthInstalled = isSamsungInstalled,
                        permissionState = permState,
                        customApiKey = currentApiKey,
                        profile = savedProfile,
                        waterGoalMl = savedProfile.waterGoalMl,
                        themeMode = savedTheme
                    )
                }

                // Initial data load and sync
                syncHealthData()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        userFeedback = UserFeedbackMessage(
                            message = "Başlangıç verileri yüklenirken bir sorun oluştu",
                            isError = true
                        )
                    )
                }
            }
        }
    }

    fun setThemeMode(mode: AppThemeMode) {
        preferences.setThemeMode(mode)
        _uiState.update {
            it.copy(
                themeMode = mode,
                userFeedback = UserFeedbackMessage(message = "Tema değiştirildi: ${mode.title}")
            )
        }
    }

    fun syncHealthData(isManualTrigger: Boolean = false) {
        if (_uiState.value.isSyncing) return
        syncJob?.cancel()
        syncJob = viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true, syncErrorMessage = null, syncSuccessMessage = null) }
            try {
                val result = repository.syncWithHealthConnect(_uiState.value.profile)
                val permState = healthConnectManager.getPermissionState()
                val syncTime = timeFormat.format(Date())

                when (result) {
                    is SyncState.Success -> {
                        val todayRecord = repository.getTodayRecord(_uiState.value.profile)
                        val summary = RuleEngine.evaluateAll(todayRecord, _uiState.value.profile)
                        _uiState.update {
                            it.copy(
                                currentRecord = todayRecord,
                                summary = summary,
                                permissionState = permState,
                                isSyncing = false,
                                lastSyncTime = syncTime,
                                syncSuccessMessage = "✓ Veriler Samsung Health ile eşitlendi",
                                userFeedback = if (isManualTrigger) UserFeedbackMessage(message = "✓ Sağlık verileri güncellendi") else null
                            )
                        }
                        loadTrendForMetric(_uiState.value.selectedMetricForDetail, _uiState.value.trendDaysCount)
                        generateFullDayAiInsight(summary, forceRefresh = false)
                    }
                    is SyncState.Error -> {
                        // Fallback to local DB record
                        val localRecord = repository.getTodayRecord(_uiState.value.profile)
                        val summary = RuleEngine.evaluateAll(localRecord, _uiState.value.profile)
                        _uiState.update {
                            it.copy(
                                currentRecord = localRecord,
                                summary = summary,
                                permissionState = permState,
                                isSyncing = false,
                                syncErrorMessage = result.message,
                                userFeedback = if (isManualTrigger) UserFeedbackMessage(message = result.message, isError = true) else null
                            )
                        }
                        loadTrendForMetric(_uiState.value.selectedMetricForDetail, _uiState.value.trendDaysCount)
                    }
                    else -> {
                        _uiState.update { it.copy(isSyncing = false) }
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSyncing = false,
                        syncErrorMessage = "Eşitleme sırasında bir hata oluştu",
                        userFeedback = if (isManualTrigger) UserFeedbackMessage(message = "Eşitleme hatası", isError = true) else null
                    )
                }
            }
        }
    }

    fun setDateFilter(mode: DateFilterMode) {
        val targetDate = when (mode) {
            DateFilterMode.TODAY -> LocalDate.now()
            DateFilterMode.YESTERDAY -> LocalDate.now().minusDays(1)
            DateFilterMode.DAYS_7, DateFilterMode.DAYS_30 -> LocalDate.now()
        }
        val targetDateStr = targetDate.format(isoDateFormatter)
        val days = if (mode == DateFilterMode.DAYS_30) 30 else 7

        dateJob?.cancel()
        dateJob = viewModelScope.launch {
            try {
                val record = repository.getRecordForDate(targetDateStr, _uiState.value.profile)
                val summary = RuleEngine.evaluateAll(record, _uiState.value.profile)

                _uiState.update {
                    it.copy(
                        selectedFilterMode = mode,
                        selectedDate = targetDateStr,
                        currentRecord = record,
                        summary = summary,
                        trendDaysCount = days
                    )
                }
                loadTrendForMetric(_uiState.value.selectedMetricForDetail, days)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(userFeedback = UserFeedbackMessage("Tarih verisi yüklenemedi", isError = true))
                }
            }
        }
    }

    fun selectDate(dateStr: String) {
        dateJob?.cancel()
        dateJob = viewModelScope.launch {
            try {
                val record = repository.getRecordForDate(dateStr, _uiState.value.profile)
                val summary = RuleEngine.evaluateAll(record, _uiState.value.profile)
                _uiState.update {
                    it.copy(
                        selectedDate = dateStr,
                        currentRecord = record,
                        summary = summary
                    )
                }
                loadTrendForMetric(_uiState.value.selectedMetricForDetail, _uiState.value.trendDaysCount)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(userFeedback = UserFeedbackMessage("Seçilen günün verisi yüklenemedi", isError = true))
                }
            }
        }
    }

    fun selectTab(tabIndex: Int) {
        _uiState.update { it.copy(activeTab = tabIndex) }
    }

    fun setBreathingSheetOpen(open: Boolean) {
        _uiState.update { it.copy(isBreathingSheetOpen = open) }
    }

    fun addWater(amountMl: Int) {
        _uiState.update {
            val newAmount = (it.waterIntakeMl + amountMl).coerceIn(0, 8000)
            it.copy(waterIntakeMl = newAmount)
        }
    }

    fun resetWater() {
        _uiState.update { it.copy(waterIntakeMl = 0) }
    }

    fun sendChatMessage(userText: String) {
        val trimmed = userText.trim()
        if (trimmed.isBlank() || _uiState.value.isChatGenerating) return

        val userMessage = ChatMessage(text = trimmed, isUser = true)
        val currentList = _uiState.value.chatMessages + userMessage
        _uiState.update {
            it.copy(
                chatMessages = currentList,
                isChatGenerating = true
            )
        }

        chatJob?.cancel()
        chatJob = viewModelScope.launch {
            try {
                val summary = _uiState.value.summary ?: RuleEngine.evaluateAll(
                    DailyHealthRecord(
                        date = LocalDate.now().format(isoDateFormatter),
                        timestamp = System.currentTimeMillis()
                    ),
                    _uiState.value.profile
                )

                val responseText = geminiExplainer.chatWithHealthCoach(
                    userMessage = trimmed,
                    summary = summary,
                    conversationHistory = currentList
                )

                val aiMessage = ChatMessage(text = responseText, isUser = false)
                _uiState.update {
                    it.copy(
                        chatMessages = it.chatMessages + aiMessage,
                        isChatGenerating = false
                    )
                }
            } catch (e: Exception) {
                val fallbackMsg = ChatMessage(
                    text = "Yanıt üretilirken bir sorun oluştu. Lütfen tekrar deneyin.",
                    isUser = false
                )
                _uiState.update {
                    it.copy(
                        chatMessages = it.chatMessages + fallbackMsg,
                        isChatGenerating = false
                    )
                }
            }
        }
    }

    fun generateShareableHealthReport(): String {
        val summary = _uiState.value.summary ?: return "Henüz sağlık verisi bulunmamaktadır."
        val aiInsight = _uiState.value.fullDayAiInsight

        return buildString {
            appendLine("📋 SAMSUNG HEALTH & AI KLİNİK SAĞLIK RAPORU")
            appendLine("📅 Tarih: ${summary.date}")
            appendLine("🔄 Son Senkronizasyon: ${_uiState.value.lastSyncTime}")
            appendLine("📊 Günlük Hazırlık Skoru: %${summary.overallScore} (${summary.overallStatus.tag})")
            appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            appendLine("📈 BİYOMETRİK ÖLÇÜMLER:")
            summary.evaluations.forEach { (type, eval) ->
                val valStr = if (eval.hasMeasuredData) eval.formattedValue else "Ölçüm Yok"
                appendLine("• ${type.displayName}: $valStr")
                appendLine("  Durum: ${eval.category.label} (${eval.statusLevel.tag}) | Ref: ${eval.normalRange}")
            }
            appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            if (aiInsight != null) {
                appendLine("🤖 AI SAĞLIK ASİSTANI ANALİZİ:")
                appendLine(aiInsight.explanationText)
                appendLine("\n💡 TAVSİYE: ${aiInsight.practicalTip}")
            }
            appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            appendLine("⚠️ YASAL UYARI: ${GeminiExplainer.MANDATORY_DISCLAIMER}")
        }
    }

    fun selectMetricForDetail(metricType: MetricType) {
        _uiState.update {
            it.copy(
                selectedMetricForDetail = metricType,
                activeTab = 1 // Switch to Detail screen
            )
        }
        loadTrendForMetric(metricType, _uiState.value.trendDaysCount)
        generateMetricAiInsight(metricType, forceRefresh = false)
    }

    fun setTrendDaysCount(days: Int) {
        _uiState.update { it.copy(trendDaysCount = days) }
        loadTrendForMetric(_uiState.value.selectedMetricForDetail, days)
    }

    private fun loadTrendForMetric(metricType: MetricType, days: Int = 7) {
        trendJob?.cancel()
        trendJob = viewModelScope.launch {
            try {
                repository.getHistoricalTrend(metricType, days).collectLatest { trendList ->
                    _uiState.update { it.copy(historicalTrend = trendList) }
                }
            } catch (e: Exception) {
                // If query is cancelled or fails, silently keep current or empty list
            }
        }
    }

    fun generateFullDayAiInsight(summary: StructuredHealthSummary, forceRefresh: Boolean = true) {
        aiInsightJob?.cancel()
        aiInsightJob = viewModelScope.launch {
            try {
                _uiState.update { it.copy(isAiGenerating = true) }
                val insight = geminiExplainer.explainFullDay(summary)
                repository.saveAiInsight(insight)
                _uiState.update {
                    it.copy(
                        fullDayAiInsight = insight,
                        isAiGenerating = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isAiGenerating = false) }
            }
        }
    }

    fun generateMetricAiInsight(metricType: MetricType, forceRefresh: Boolean = true) {
        val currentSummary = _uiState.value.summary ?: return
        val eval = currentSummary.evaluations[metricType] ?: return

        metricAiJob?.cancel()
        metricAiJob = viewModelScope.launch {
            try {
                val insight = geminiExplainer.explainMetric(eval, currentSummary.date)
                repository.saveAiInsight(insight)
                _uiState.update { state ->
                    val newMap = state.metricAiInsights.toMutableMap()
                    newMap[metricType] = insight
                    state.copy(metricAiInsights = newMap)
                }
            } catch (e: Exception) {
                // Fallback handled inside GeminiExplainer
            }
        }
    }

    fun updateProfile(profile: UserHealthProfile) {
        preferences.saveProfile(profile)
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(profile = profile, waterGoalMl = profile.waterGoalMl) }
                val record = _uiState.value.currentRecord ?: return@launch
                val summary = RuleEngine.evaluateAll(record, profile)
                _uiState.update {
                    it.copy(
                        summary = summary,
                        userFeedback = UserFeedbackMessage(message = "Hedefler başarıyla kaydedildi")
                    )
                }
                repository.saveRecord(record, profile)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(userFeedback = UserFeedbackMessage("Hedefler kaydedilirken hata oluştu", isError = true))
                }
            }
        }
    }

    fun updateCustomApiKey(key: String) {
        preferences.saveCustomApiKey(key)
        geminiExplainer.saveCustomApiKey(key)
        _uiState.update {
            it.copy(
                customApiKey = key,
                userFeedback = UserFeedbackMessage(message = "Gemini API Anahtarı güncellendi")
            )
        }
    }

    fun refreshPermissionsState() {
        viewModelScope.launch {
            try {
                val permState = healthConnectManager.getPermissionState()
                _uiState.update { it.copy(permissionState = permState) }
                if (permState.anyGranted) {
                    syncHealthData()
                }
            } catch (e: Exception) {
                // Ignore permission refresh error
            }
        }
    }

    fun clearLocalData() {
        viewModelScope.launch {
            try {
                repository.clearAllLocalCache()
                val blankRecord = DailyHealthRecord(
                    date = LocalDate.now().format(isoDateFormatter),
                    timestamp = System.currentTimeMillis()
                )
                val summary = RuleEngine.evaluateAll(blankRecord, _uiState.value.profile)
                _uiState.update {
                    it.copy(
                        currentRecord = blankRecord,
                        summary = summary,
                        fullDayAiInsight = null,
                        metricAiInsights = emptyMap(),
                        historicalTrend = emptyList(),
                        userFeedback = UserFeedbackMessage(message = "Uygulama yerel verileri başarıyla temizlendi")
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(userFeedback = UserFeedbackMessage("Temizleme sırasında bir hata oluştu", isError = true))
                }
            }
        }
    }

    fun dismissFeedback() {
        _uiState.update { it.copy(userFeedback = null) }
    }
}
