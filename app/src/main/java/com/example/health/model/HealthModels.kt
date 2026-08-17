package com.example.health.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.*

enum class MetricType(val displayName: String, val unit: String) {
    STEPS("Adım Sayısı", "adım"),
    HEART_RATE("Kalp Atış Hızı (Dinlenik)", "bpm"),
    SLEEP("Uyku Süresi", "saat"),
    SPO2("Oksijen Satürasyonu (SpO2)", "%"),
    STRESS("Stres Düzeyi", "skor"),
    CALORIES("Yakılan Aktif Kalori", "kcal")
}

enum class HealthCategory(val label: String) {
    // Heart Rate
    CRITICAL_LOW("Kritik Düşük"),
    LOW("Düşük"),
    NORMAL("Normal"),
    HIGH("Yüksek"),
    CRITICAL_HIGH("Kritik Yüksek"),

    // Sleep
    VERY_LOW("Çok Yetersiz"),
    BELOW_AVERAGE("Ortalama Altı"),
    OPTIMAL("İdeal"),
    ABOVE_AVERAGE("Ortalama Üstü"),

    // Steps & Calories
    ACHIEVED("Hedefe Ulaşıldı"),
    MODERATE("Orta Düzey"),
    EXCELLENT("Mükemmel"),

    // Stress
    REST("Dinlenme / Rahat"),
    MEDIUM("Orta Stres"),

    // General & Unmeasured
    UNMEASURED("Ölçüm Bekleniyor"),
    GOOD("İyi"),
    ATTENTION("Dikkat Gerektirir"),
    CRITICAL("Kritik Risk")
}

enum class HealthStatusLevel(val color: Color, val tag: String) {
    OPTIMAL(StatusOptimalTeal, "İDEAL"),
    GOOD(StatusGoodGreen, "İYİ"),
    ATTENTION(StatusAttentionYellow, "DİKKAT"),
    CRITICAL(StatusCriticalRed, "KRİTİK"),
    LOW(StatusLowBlue, "DÜŞÜK")
}

/**
 * Output of the deterministic Rule Engine evaluation for a single metric.
 */
data class MetricEvaluation(
    val metricType: MetricType,
    val rawValue: Double,
    val formattedValue: String,
    val category: HealthCategory,
    val statusLevel: HealthStatusLevel,
    val normalRange: String,
    val differenceFromBaseline: String,
    val clinicalSummary: String,
    val isCritical: Boolean = false,
    val criticalAlertMessage: String? = null,
    val hasMeasuredData: Boolean = true
)

/**
 * Full day structured snapshot for rule engine and Gemini input.
 */
data class DailyHealthRecord(
    val date: String, // e.g. "2026-08-16"
    val timestamp: Long = System.currentTimeMillis(),
    val steps: Int = 0,
    val hasStepsData: Boolean = false,
    val restingHeartRate: Int = 0,
    val hasHeartRateData: Boolean = false,
    val minHeartRate: Int = 0,
    val maxHeartRate: Int = 0,
    val sleepHours: Double = 0.0,
    val hasSleepData: Boolean = false,
    val deepSleepPercent: Int = 0,
    val spO2Percent: Int = 0,
    val hasSpO2Data: Boolean = false,
    val stressScore: Int = 0, // 0-100
    val hasStressData: Boolean = false,
    val activeCaloriesKcal: Int = 0,
    val hasCaloriesData: Boolean = false,
    val distanceMeters: Double = 0.0,
    val activeMinutes: Int = 0
)

/**
 * Structured Evaluation output produced by RuleEngine.
 * This JSON representation is what gets passed to Gemini LLM (no raw telemetry).
 */
data class StructuredHealthSummary(
    val date: String,
    val timestamp: Long,
    val evaluations: Map<MetricType, MetricEvaluation>,
    val overallScore: Int, // 0-100
    val overallStatus: HealthStatusLevel,
    val hasCriticalConditions: Boolean,
    val criticalAlerts: List<String>
) {
    /**
     * Converts to sanitized, PII-free structured JSON for LLM prompt.
     */
    fun toSanitizedLlmJson(): String {
        val evalList = evaluations.map { (type, eval) ->
            """
            "${type.name}": {
              "name": "${type.displayName}",
              "value": "${eval.formattedValue}",
              "hasData": ${eval.hasMeasuredData},
              "category": "${eval.category.name} (${eval.category.label})",
              "status": "${eval.statusLevel.tag}",
              "normalRange": "${eval.normalRange}",
              "comparison": "${eval.differenceFromBaseline}",
              "clinicalRuleSummary": "${eval.clinicalSummary}",
              "isCritical": ${eval.isCritical}
            }
            """.trimIndent()
        }.joinToString(",\n")

        return """
        {
          "date": "$date",
          "overallScore": $overallScore,
          "hasCriticalConditions": $hasCriticalConditions,
          "metrics": {
            $evalList
          }
        }
        """.trimIndent()
    }
}

data class HistoricalTrendPoint(
    val label: String, // e.g., "Pzt", "10 Ağu"
    val value: Float,
    val formattedValue: String,
    val category: HealthCategory,
    val date: String = ""
)

data class AiGeneratedInsight(
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val date: String,
    val metricType: MetricType? = null, // null means whole day summary
    val explanationText: String,
    val practicalTip: String,
    val disclaimer: String = "Bu analizler tıbbi tavsiye yerine geçmez. Olağandışı durumlarda doktorunuza danışınız.",
    val criticalMedicalWarning: String? = null
)
