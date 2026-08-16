package com.example.health.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.health.ai.GeminiExplainer
import com.example.health.data.HealthConnectAvailability
import com.example.health.data.HealthConnectManager
import com.example.health.data.HealthDataRepository
import com.example.health.data.HealthPermissionState
import com.example.health.data.local.AppDatabase
import com.example.health.engine.RuleEngine
import com.example.health.engine.UserHealthProfile
import com.example.health.model.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class HealthUiState(
    val currentRecord: DailyHealthRecord? = null,
    val summary: StructuredHealthSummary? = null,
    val profile: UserHealthProfile = UserHealthProfile(),
    val isAiGenerating: Boolean = false,
    val isSyncing: Boolean = false,
    val lastSyncTime: String = "Henüz eşitlenmedi",
    val syncErrorMessage: String? = null,
    val fullDayAiInsight: AiGeneratedInsight? = null,
    val metricAiInsights: Map<MetricType, AiGeneratedInsight> = emptyMap(),
    val historicalTrend: List<HistoricalTrendPoint> = emptyList(),
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
    val isBreathingSheetOpen: Boolean = false
)

class HealthViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    val healthConnectManager = HealthConnectManager(application)
    val repository = HealthDataRepository(db.healthDao(), healthConnectManager)
    val geminiExplainer = GeminiExplainer(application)

    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    private val _uiState = MutableStateFlow(HealthUiState())
    val uiState: StateFlow<HealthUiState> = _uiState.asStateFlow()

    init {
        loadInitialData()
    }

    fun loadInitialData() {
        viewModelScope.launch {
            // Check health connect availability & permissions
            val availability = healthConnectManager.checkHealthConnectAvailability()
            val isSamsungInstalled = healthConnectManager.isSamsungHealthInstalled()
            val permState = healthConnectManager.getPermissionState()
            val currentApiKey = geminiExplainer.getEffectiveApiKey()

            _uiState.update {
                it.copy(
                    healthConnectAvailability = availability,
                    isSamsungHealthInstalled = isSamsungInstalled,
                    permissionState = permState,
                    customApiKey = currentApiKey
                )
            }

            // Sync or Load today's record
            syncHealthData()
        }
    }

    fun syncHealthData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true, syncErrorMessage = null) }
            try {
                // Refresh permission status
                val permState = healthConnectManager.getPermissionState()
                
                // Fetch real data
                val record = repository.syncWithHealthConnect(_uiState.value.profile) ?: repository.getTodayRecord()
                val summary = RuleEngine.evaluateAll(record, _uiState.value.profile)
                val syncTime = timeFormat.format(Date())

                _uiState.update {
                    it.copy(
                        currentRecord = record,
                        summary = summary,
                        permissionState = permState,
                        isSyncing = false,
                        lastSyncTime = syncTime
                    )
                }

                // Refresh trend for active metric
                loadTrendForMetric(_uiState.value.selectedMetricForDetail)

                // Generate AI insight based on real live data
                generateFullDayAiInsight(summary, forceRefresh = false)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSyncing = false,
                        syncErrorMessage = "Senkronizasyon hatası: ${e.localizedMessage}"
                    )
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
            val newAmount = (it.waterIntakeMl + amountMl).coerceIn(0, 6000)
            it.copy(waterIntakeMl = newAmount)
        }
    }

    fun sendChatMessage(userText: String) {
        val trimmed = userText.trim()
        if (trimmed.isBlank()) return

        val userMessage = ChatMessage(text = trimmed, isUser = true)
        val currentList = _uiState.value.chatMessages + userMessage
        _uiState.update {
            it.copy(
                chatMessages = currentList,
                isChatGenerating = true
            )
        }

        viewModelScope.launch {
            val summary = _uiState.value.summary ?: return@launch
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
        }
    }

    fun generateShareableHealthReport(): String {
        val summary = _uiState.value.summary ?: return "Henüz sağlık verisi bulunmamaktadır."
        val aiInsight = _uiState.value.fullDayAiInsight

        return buildString {
            appendLine("📋 SAMSUNG HEALTH & AI KLİNİK ÖZET RAPORU")
            appendLine("📅 Tarih: ${summary.date}")
            appendLine("🔄 Son Senkronizasyon: ${_uiState.value.lastSyncTime}")
            appendLine("📊 Genel Hazırlık Skoru: %${summary.overallScore} (${summary.overallStatus.tag})")
            appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            appendLine("📈 GÜNCEL METRİK DETAYLARI (Canlı Veri):")
            summary.evaluations.forEach { (type, eval) ->
                appendLine("• ${type.displayName}: ${eval.formattedValue}")
                appendLine("  Durum: ${eval.category.label} (${eval.statusLevel.tag}) | Ref: ${eval.normalRange}")
            }
            appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            if (aiInsight != null) {
                appendLine("🤖 AI SAĞLIK ASİSTANI SENTEZİ:")
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
        loadTrendForMetric(metricType)
        generateMetricAiInsight(metricType, forceRefresh = false)
    }

    private fun loadTrendForMetric(metricType: MetricType) {
        viewModelScope.launch {
            repository.getHistoricalTrend(metricType).collectLatest { trendList ->
                _uiState.update { it.copy(historicalTrend = trendList) }
            }
        }
    }

    fun generateFullDayAiInsight(summary: StructuredHealthSummary, forceRefresh: Boolean = true) {
        viewModelScope.launch {
            _uiState.update { it.copy(isAiGenerating = true) }
            val insight = geminiExplainer.explainFullDay(summary)
            repository.saveAiInsight(insight)
            _uiState.update {
                it.copy(
                    fullDayAiInsight = insight,
                    isAiGenerating = false
                )
            }
        }
    }

    fun generateMetricAiInsight(metricType: MetricType, forceRefresh: Boolean = true) {
        val currentSummary = _uiState.value.summary ?: return
        val eval = currentSummary.evaluations[metricType] ?: return

        viewModelScope.launch {
            val insight = geminiExplainer.explainMetric(eval, currentSummary.date)
            repository.saveAiInsight(insight)
            _uiState.update { state ->
                val newMap = state.metricAiInsights.toMutableMap()
                newMap[metricType] = insight
                state.copy(metricAiInsights = newMap)
            }
        }
    }

    fun updateProfile(profile: UserHealthProfile) {
        viewModelScope.launch {
            _uiState.update { it.copy(profile = profile) }
            val record = _uiState.value.currentRecord ?: return@launch
            val summary = RuleEngine.evaluateAll(record, profile)
            _uiState.update { it.copy(summary = summary) }
            repository.saveRecord(record, profile)
        }
    }

    fun updateCustomApiKey(key: String) {
        geminiExplainer.saveCustomApiKey(key)
        _uiState.update { it.copy(customApiKey = key) }
    }

    fun refreshPermissionsState() {
        viewModelScope.launch {
            val permState = healthConnectManager.getPermissionState()
            _uiState.update { it.copy(permissionState = permState) }
            if (permState.anyGranted) {
                syncHealthData()
            }
        }
    }
}
