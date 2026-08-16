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
    REST("Dinlenme / Çok Düşük"),
    MEDIUM("Orta"),

    // General
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
    val criticalAlertMessage: String? = null
)

/**
 * Full day structured snapshot for rule engine and Gemini input.
 */
data class DailyHealthRecord(
    val date: String, // e.g. "2026-08-16"
    val timestamp: Long = System.currentTimeMillis(),
    val steps: Int,
    val restingHeartRate: Int,
    val minHeartRate: Int,
    val maxHeartRate: Int,
    val sleepHours: Double,
    val deepSleepPercent: Int,
    val spO2Percent: Int,
    val stressScore: Int, // 0-100
    val activeCaloriesKcal: Int
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
    val category: HealthCategory
)

data class AiGeneratedInsight(
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val date: String,
    val metricType: MetricType? = null, // null means whole day summary
    val explanationText: String,
    val practicalTip: String,
    val disclaimer: String = "Bu bilgiler tıbbi tavsiye yerine geçmez, endişelerin varsa doktoruna danış.",
    val criticalMedicalWarning: String? = null
)
