package com.example.health.engine

import com.example.health.model.*
import java.util.Locale
import kotlin.math.roundToInt

data class UserHealthProfile(
    val stepGoal: Int = 10000,
    val sleepBaselineHours: Double = 8.0,
    val restingHeartRateBaselineBpm: Int = 65,
    val activeCalorieGoalKcal: Int = 500,
    val activeMinutesGoal: Int = 30,
    val waterGoalMl: Int = 2500
)

object HealthThresholds {
    const val HR_CRITICAL_BRADYCARDIA = 45
    const val HR_LOW_THRESHOLD = 55
    const val HR_NORMAL_MAX = 90
    const val HR_CRITICAL_TACHYCARDIA = 115

    const val SLEEP_VERY_LOW_HOURS = 4.5
    const val SLEEP_BELOW_AVG_RATIO = 0.8
    const val SLEEP_OPTIMAL_MIN_HOURS = 7.0
    const val SLEEP_OPTIMAL_MAX_HOURS = 9.0

    const val STEPS_LOW_RATIO = 0.4
    const val STEPS_MODERATE_RATIO = 0.75

    const val SPO2_CRITICAL = 90
    const val SPO2_NORMAL_MIN = 95

    const val STRESS_REST_MAX = 25
    const val STRESS_LOW_MAX = 50
    const val STRESS_MEDIUM_MAX = 75

    const val CALORIES_LOW_RATIO = 0.5
}

/**
 * Deterministic Rule Engine for Samsung Health & Health Connect telemetry.
 * Operates completely offline to guarantee medical boundaries and safety standards.
 */
object RuleEngine {

    fun evaluateAll(
        record: DailyHealthRecord,
        profile: UserHealthProfile = UserHealthProfile()
    ): StructuredHealthSummary {
        val hrEval = if (record.hasHeartRateData && record.restingHeartRate > 0) {
            evaluateHeartRate(record.restingHeartRate)
        } else {
            createUnmeasuredEvaluation(MetricType.HEART_RATE, "60 - 100 bpm", "Bugün için dinlenik nabız ölçümü henüz senkronize edilmedi.")
        }

        val sleepEval = if (record.hasSleepData && record.sleepHours > 0.0) {
            evaluateSleep(record.sleepHours, profile.sleepBaselineHours)
        } else {
            createUnmeasuredEvaluation(MetricType.SLEEP, "7.0 - 9.0 saat", "Bugün için uyku seansı kaydı bulunamadı.")
        }

        val stepsEval = if (record.hasStepsData || record.steps > 0) {
            evaluateSteps(record.steps, profile.stepGoal)
        } else {
            createUnmeasuredEvaluation(MetricType.STEPS, "${profile.stepGoal} adım hedefi", "Adım verisi henüz kaydedilmedi.")
        }

        val spO2Eval = if (record.hasSpO2Data && record.spO2Percent > 0) {
            evaluateSpO2(record.spO2Percent)
        } else {
            createUnmeasuredEvaluation(MetricType.SPO2, "%95 - %100", "Kandaki oksijen doygunluğu (SpO2) ölçümü bekleniyor.")
        }

        val stressEval = if (record.hasStressData && record.stressScore > 0) {
            evaluateStress(record.stressScore)
        } else {
            createUnmeasuredEvaluation(MetricType.STRESS, "0 - 50 (İdeal)", "Stres skoru henüz hesaplanmadı.")
        }

        val calEval = if (record.hasCaloriesData || record.activeCaloriesKcal > 0) {
            evaluateCalories(record.activeCaloriesKcal, profile.activeCalorieGoalKcal)
        } else {
            createUnmeasuredEvaluation(MetricType.CALORIES, "${profile.activeCalorieGoalKcal} kcal hedefi", "Aktif kalori verisi bekleniyor.")
        }

        val evalMap = mapOf(
            MetricType.HEART_RATE to hrEval,
            MetricType.SLEEP to sleepEval,
            MetricType.STEPS to stepsEval,
            MetricType.SPO2 to spO2Eval,
            MetricType.STRESS to stressEval,
            MetricType.CALORIES to calEval
        )

        val criticalAlerts = mutableListOf<String>()
        evalMap.values.forEach { eval ->
            if (eval.isCritical && eval.criticalAlertMessage != null) {
                criticalAlerts.add(eval.criticalAlertMessage)
            }
        }

        // Overall score calculation over measured metrics
        val measuredEvals = evalMap.values.filter { it.hasMeasuredData }
        val overallScore = if (measuredEvals.isNotEmpty()) {
            calculateOverallScore(measuredEvals)
        } else {
            0
        }

        val overallStatus = when {
            criticalAlerts.isNotEmpty() -> HealthStatusLevel.CRITICAL
            overallScore >= 80 -> HealthStatusLevel.OPTIMAL
            overallScore >= 65 -> HealthStatusLevel.GOOD
            overallScore >= 50 -> HealthStatusLevel.ATTENTION
            overallScore > 0 -> HealthStatusLevel.ATTENTION
            else -> HealthStatusLevel.LOW
        }

        return StructuredHealthSummary(
            date = record.date,
            timestamp = record.timestamp,
            evaluations = evalMap,
            overallScore = overallScore,
            overallStatus = overallStatus,
            hasCriticalConditions = criticalAlerts.isNotEmpty(),
            criticalAlerts = criticalAlerts
        )
    }

    private fun createUnmeasuredEvaluation(
        type: MetricType,
        normalRange: String,
        summary: String
    ): MetricEvaluation {
        return MetricEvaluation(
            metricType = type,
            rawValue = 0.0,
            formattedValue = "Ölçüm Yok",
            category = HealthCategory.UNMEASURED,
            statusLevel = HealthStatusLevel.LOW,
            normalRange = normalRange,
            differenceFromBaseline = "Veri bekleniyor",
            clinicalSummary = summary,
            isCritical = false,
            criticalAlertMessage = null,
            hasMeasuredData = false
        )
    }

    /**
     * Evaluates resting heart rate according to clinical reference intervals.
     */
    fun evaluateHeartRate(restingBpm: Int): MetricEvaluation {
        return when {
            restingBpm <= HealthThresholds.HR_CRITICAL_BRADYCARDIA -> {
                MetricEvaluation(
                    metricType = MetricType.HEART_RATE,
                    rawValue = restingBpm.toDouble(),
                    formattedValue = "$restingBpm bpm",
                    category = HealthCategory.CRITICAL_LOW,
                    statusLevel = HealthStatusLevel.CRITICAL,
                    normalRange = "60 - 100 bpm",
                    differenceFromBaseline = "${60 - restingBpm} bpm normalin altında",
                    clinicalSummary = "Şiddetli bradikardi (aşırı düşük nabız) tespit edildi.",
                    isCritical = true,
                    criticalAlertMessage = "Kritik Düşük Kalp Hızı ($restingBpm bpm). Baş dönmesi veya baygınlık hissi varsa bir sağlık kuruluşuna danışın.",
                    hasMeasuredData = true
                )
            }
            restingBpm < HealthThresholds.HR_LOW_THRESHOLD -> {
                MetricEvaluation(
                    metricType = MetricType.HEART_RATE,
                    rawValue = restingBpm.toDouble(),
                    formattedValue = "$restingBpm bpm",
                    category = HealthCategory.LOW,
                    statusLevel = HealthStatusLevel.LOW,
                    normalRange = "60 - 100 bpm",
                    differenceFromBaseline = "${60 - restingBpm} bpm normal referansın altında",
                    clinicalSummary = "Hafif bradikardi / sporcu dinlenik kalp ritmi profili.",
                    isCritical = false,
                    hasMeasuredData = true
                )
            }
            restingBpm <= HealthThresholds.HR_NORMAL_MAX -> {
                MetricEvaluation(
                    metricType = MetricType.HEART_RATE,
                    rawValue = restingBpm.toDouble(),
                    formattedValue = "$restingBpm bpm",
                    category = HealthCategory.NORMAL,
                    statusLevel = HealthStatusLevel.GOOD,
                    normalRange = "60 - 100 bpm",
                    differenceFromBaseline = "Standart dinlenik referans aralığında",
                    clinicalSummary = "Sağlıklı dinlenik kalp atış hızı.",
                    isCritical = false,
                    hasMeasuredData = true
                )
            }
            restingBpm < HealthThresholds.HR_CRITICAL_TACHYCARDIA -> {
                MetricEvaluation(
                    metricType = MetricType.HEART_RATE,
                    rawValue = restingBpm.toDouble(),
                    formattedValue = "$restingBpm bpm",
                    category = HealthCategory.HIGH,
                    statusLevel = HealthStatusLevel.ATTENTION,
                    normalRange = "60 - 100 bpm",
                    differenceFromBaseline = "${restingBpm - 100} bpm normalin üstünde",
                    clinicalSummary = "Yükselmiş dinlenik nabız. Kafein, stres veya yorgunluk etkileyebilir.",
                    isCritical = false,
                    hasMeasuredData = true
                )
            }
            else -> {
                MetricEvaluation(
                    metricType = MetricType.HEART_RATE,
                    rawValue = restingBpm.toDouble(),
                    formattedValue = "$restingBpm bpm",
                    category = HealthCategory.CRITICAL_HIGH,
                    statusLevel = HealthStatusLevel.CRITICAL,
                    normalRange = "60 - 100 bpm",
                    differenceFromBaseline = "${restingBpm - 100} bpm yüksek",
                    clinicalSummary = "Kritik yüksek dinlenik nabız tespit edildi.",
                    isCritical = true,
                    criticalAlertMessage = "Kritik Yüksek Kalp Hızı ($restingBpm bpm). Dinlenme halindeyken bu değer acil tıbbi değerlendirme gerektirebilir.",
                    hasMeasuredData = true
                )
            }
        }
    }

    /**
     * Evaluates sleep duration against user baseline and recommended recovery hours.
     */
    fun evaluateSleep(hours: Double, baselineHours: Double): MetricEvaluation {
        val diffHours = hours - baselineHours
        val diffStr = if (diffHours >= 0) {
            "+${String.format(Locale.US, "%.1f", diffHours)} sa hedefin üstünde"
        } else {
            "${String.format(Locale.US, "%.1f", diffHours)} sa hedefin altında"
        }

        return when {
            hours < HealthThresholds.SLEEP_VERY_LOW_HOURS -> {
                MetricEvaluation(
                    metricType = MetricType.SLEEP,
                    rawValue = hours,
                    formattedValue = String.format(Locale.US, "%.1f sa", hours),
                    category = HealthCategory.VERY_LOW,
                    statusLevel = HealthStatusLevel.CRITICAL,
                    normalRange = "7.0 - 9.0 saat",
                    differenceFromBaseline = diffStr,
                    clinicalSummary = "Ciddi uyku eksikliği. Zihinsel odaklanma ve bağışıklık toparlanması kısıtlı.",
                    isCritical = false,
                    hasMeasuredData = true
                )
            }
            hours <= (baselineHours * HealthThresholds.SLEEP_BELOW_AVG_RATIO) -> {
                MetricEvaluation(
                    metricType = MetricType.SLEEP,
                    rawValue = hours,
                    formattedValue = String.format(Locale.US, "%.1f sa", hours),
                    category = HealthCategory.BELOW_AVERAGE,
                    statusLevel = HealthStatusLevel.ATTENTION,
                    normalRange = "7.0 - 9.0 saat",
                    differenceFromBaseline = diffStr,
                    clinicalSummary = "Hedeflenen ortalamanın altında uyku süresi.",
                    isCritical = false,
                    hasMeasuredData = true
                )
            }
            hours in HealthThresholds.SLEEP_OPTIMAL_MIN_HOURS..HealthThresholds.SLEEP_OPTIMAL_MAX_HOURS -> {
                MetricEvaluation(
                    metricType = MetricType.SLEEP,
                    rawValue = hours,
                    formattedValue = String.format(Locale.US, "%.1f sa", hours),
                    category = HealthCategory.OPTIMAL,
                    statusLevel = HealthStatusLevel.OPTIMAL,
                    normalRange = "7.0 - 9.0 saat",
                    differenceFromBaseline = diffStr,
                    clinicalSummary = "Mükemmel dinlenme ve hücresel toparlanma süresi.",
                    isCritical = false,
                    hasMeasuredData = true
                )
            }
            hours > HealthThresholds.SLEEP_OPTIMAL_MAX_HOURS -> {
                MetricEvaluation(
                    metricType = MetricType.SLEEP,
                    rawValue = hours,
                    formattedValue = String.format(Locale.US, "%.1f sa", hours),
                    category = HealthCategory.ABOVE_AVERAGE,
                    statusLevel = HealthStatusLevel.GOOD,
                    normalRange = "7.0 - 9.0 saat",
                    differenceFromBaseline = diffStr,
                    clinicalSummary = "Uzun dinlenme periyodu.",
                    isCritical = false,
                    hasMeasuredData = true
                )
            }
            else -> {
                MetricEvaluation(
                    metricType = MetricType.SLEEP,
                    rawValue = hours,
                    formattedValue = String.format(Locale.US, "%.1f sa", hours),
                    category = HealthCategory.NORMAL,
                    statusLevel = HealthStatusLevel.GOOD,
                    normalRange = "7.0 - 9.0 saat",
                    differenceFromBaseline = diffStr,
                    clinicalSummary = "Yeterli uyku süresi.",
                    isCritical = false,
                    hasMeasuredData = true
                )
            }
        }
    }

    /**
     * Evaluates daily steps relative to user target and WHO guidelines.
     */
    fun evaluateSteps(steps: Int, target: Int): MetricEvaluation {
        val percent = if (target > 0) ((steps.toDouble() / target.toDouble()) * 100).roundToInt() else 0
        val diff = steps - target
        val diffStr = if (diff >= 0) "+$diff adım (Hedefin %$percent'i)" else "$diff adım (Hedefin %$percent'i)"

        return when {
            steps < (target * HealthThresholds.STEPS_LOW_RATIO) -> {
                MetricEvaluation(
                    metricType = MetricType.STEPS,
                    rawValue = steps.toDouble(),
                    formattedValue = "$steps adım",
                    category = HealthCategory.LOW,
                    statusLevel = HealthStatusLevel.ATTENTION,
                    normalRange = "$target adım hedefi",
                    differenceFromBaseline = diffStr,
                    clinicalSummary = "Düşük günlük aktivite seviyesi.",
                    isCritical = false,
                    hasMeasuredData = true
                )
            }
            steps < (target * HealthThresholds.STEPS_MODERATE_RATIO) -> {
                MetricEvaluation(
                    metricType = MetricType.STEPS,
                    rawValue = steps.toDouble(),
                    formattedValue = "$steps adım",
                    category = HealthCategory.MODERATE,
                    statusLevel = HealthStatusLevel.GOOD,
                    normalRange = "$target adım hedefi",
                    differenceFromBaseline = diffStr,
                    clinicalSummary = "Orta seviye günlük hareketlilik.",
                    isCritical = false,
                    hasMeasuredData = true
                )
            }
            steps < (target * 1.5) -> {
                MetricEvaluation(
                    metricType = MetricType.STEPS,
                    rawValue = steps.toDouble(),
                    formattedValue = "$steps adım",
                    category = HealthCategory.ACHIEVED,
                    statusLevel = HealthStatusLevel.OPTIMAL,
                    normalRange = "$target adım hedefi",
                    differenceFromBaseline = diffStr,
                    clinicalSummary = "Günlük adım hedefine başarıyla ulaşıldı.",
                    isCritical = false,
                    hasMeasuredData = true
                )
            }
            else -> {
                MetricEvaluation(
                    metricType = MetricType.STEPS,
                    rawValue = steps.toDouble(),
                    formattedValue = "$steps adım",
                    category = HealthCategory.EXCELLENT,
                    statusLevel = HealthStatusLevel.OPTIMAL,
                    normalRange = "$target adım hedefi",
                    differenceFromBaseline = diffStr,
                    clinicalSummary = "Yüksek aktif performans seviyesi.",
                    isCritical = false,
                    hasMeasuredData = true
                )
            }
        }
    }

    /**
     * Evaluates Blood Oxygen Saturation SpO2 (%).
     */
    fun evaluateSpO2(spO2: Int): MetricEvaluation {
        return when {
            spO2 < HealthThresholds.SPO2_CRITICAL -> {
                MetricEvaluation(
                    metricType = MetricType.SPO2,
                    rawValue = spO2.toDouble(),
                    formattedValue = "%$spO2",
                    category = HealthCategory.CRITICAL,
                    statusLevel = HealthStatusLevel.CRITICAL,
                    normalRange = "%95 - %100",
                    differenceFromBaseline = "%${95 - spO2} düşük",
                    clinicalSummary = "Hipoksemi (kritik düşük kan oksijeni) göstergesi.",
                    isCritical = true,
                    criticalAlertMessage = "Kritik Düşük Oksijen Satürasyonu (%$spO2). Nefes darlığı veya göğüs baskısı varsa lütfen vakit kaybetmeden tıbbi destek alın.",
                    hasMeasuredData = true
                )
            }
            spO2 < HealthThresholds.SPO2_NORMAL_MIN -> {
                MetricEvaluation(
                    metricType = MetricType.SPO2,
                    rawValue = spO2.toDouble(),
                    formattedValue = "%$spO2",
                    category = HealthCategory.ATTENTION,
                    statusLevel = HealthStatusLevel.ATTENTION,
                    normalRange = "%95 - %100",
                    differenceFromBaseline = "%${95 - spO2} sınırda düşük",
                    clinicalSummary = "Sınırda oksijen satürasyonu. Derin nefes egzersizi ve havalandırma önerilir.",
                    isCritical = false,
                    hasMeasuredData = true
                )
            }
            else -> {
                MetricEvaluation(
                    metricType = MetricType.SPO2,
                    rawValue = spO2.toDouble(),
                    formattedValue = "%$spO2",
                    category = HealthCategory.NORMAL,
                    statusLevel = HealthStatusLevel.OPTIMAL,
                    normalRange = "%95 - %100",
                    differenceFromBaseline = "Optimal solunum doygunluğu",
                    clinicalSummary = "Kandaki oksijen doygunluğu mükemmel seviyede.",
                    isCritical = false,
                    hasMeasuredData = true
                )
            }
        }
    }

    /**
     * Evaluates Stress Score (0-100).
     */
    fun evaluateStress(stressScore: Int): MetricEvaluation {
        return when {
            stressScore <= HealthThresholds.STRESS_REST_MAX -> {
                MetricEvaluation(
                    metricType = MetricType.STRESS,
                    rawValue = stressScore.toDouble(),
                    formattedValue = "$stressScore / 100",
                    category = HealthCategory.REST,
                    statusLevel = HealthStatusLevel.OPTIMAL,
                    normalRange = "0 - 50 (Düşük/Dinlenme)",
                    differenceFromBaseline = "Parasempatik dinlenme modu aktif",
                    clinicalSummary = "Beden ve zihin yüksek düzeyde sakin ve toparlanma durumunda.",
                    isCritical = false,
                    hasMeasuredData = true
                )
            }
            stressScore <= HealthThresholds.STRESS_LOW_MAX -> {
                MetricEvaluation(
                    metricType = MetricType.STRESS,
                    rawValue = stressScore.toDouble(),
                    formattedValue = "$stressScore / 100",
                    category = HealthCategory.LOW,
                    statusLevel = HealthStatusLevel.GOOD,
                    normalRange = "0 - 50 (Düşük)",
                    differenceFromBaseline = "Dengeli otonom sinir sistemi",
                    clinicalSummary = "Rutin günlük stres düzeyi kontrol altında.",
                    isCritical = false,
                    hasMeasuredData = true
                )
            }
            stressScore <= HealthThresholds.STRESS_MEDIUM_MAX -> {
                MetricEvaluation(
                    metricType = MetricType.STRESS,
                    rawValue = stressScore.toDouble(),
                    formattedValue = "$stressScore / 100",
                    category = HealthCategory.MEDIUM,
                    statusLevel = HealthStatusLevel.ATTENTION,
                    normalRange = "0 - 50 (İdeal)",
                    differenceFromBaseline = "Hafif yükselmiş sempatik aktivite",
                    clinicalSummary = "Orta seviye stres. Kısa molalar ve gevşeme önerilir.",
                    isCritical = false,
                    hasMeasuredData = true
                )
            }
            else -> {
                MetricEvaluation(
                    metricType = MetricType.STRESS,
                    rawValue = stressScore.toDouble(),
                    formattedValue = "$stressScore / 100",
                    category = HealthCategory.HIGH,
                    statusLevel = HealthStatusLevel.CRITICAL,
                    normalRange = "0 - 50 (İdeal)",
                    differenceFromBaseline = "Belirgin yüksek stres yükü",
                    clinicalSummary = "Yüksek sempatik uyarılma. Nefes çalışması ve gevşeme molası tavsiye edilir.",
                    isCritical = false,
                    hasMeasuredData = true
                )
            }
        }
    }

    /**
     * Evaluates active calorie burn.
     */
    fun evaluateCalories(caloriesKcal: Int, targetKcal: Int): MetricEvaluation {
        val diff = caloriesKcal - targetKcal
        val diffStr = if (diff >= 0) "+$diff kcal hedef üstü" else "$diff kcal hedef altı"

        return when {
            caloriesKcal < (targetKcal * HealthThresholds.CALORIES_LOW_RATIO) -> {
                MetricEvaluation(
                    metricType = MetricType.CALORIES,
                    rawValue = caloriesKcal.toDouble(),
                    formattedValue = "$caloriesKcal kcal",
                    category = HealthCategory.LOW,
                    statusLevel = HealthStatusLevel.ATTENTION,
                    normalRange = "$targetKcal kcal hedefi",
                    differenceFromBaseline = diffStr,
                    clinicalSummary = "Düşük aktif enerji harcaması.",
                    isCritical = false,
                    hasMeasuredData = true
                )
            }
            caloriesKcal < targetKcal -> {
                MetricEvaluation(
                    metricType = MetricType.CALORIES,
                    rawValue = caloriesKcal.toDouble(),
                    formattedValue = "$caloriesKcal kcal",
                    category = HealthCategory.NORMAL,
                    statusLevel = HealthStatusLevel.GOOD,
                    normalRange = "$targetKcal kcal hedefi",
                    differenceFromBaseline = diffStr,
                    clinicalSummary = "Hedefe yakın aktif kalori yakımı.",
                    isCritical = false,
                    hasMeasuredData = true
                )
            }
            else -> {
                MetricEvaluation(
                    metricType = MetricType.CALORIES,
                    rawValue = caloriesKcal.toDouble(),
                    formattedValue = "$caloriesKcal kcal",
                    category = HealthCategory.ACHIEVED,
                    statusLevel = HealthStatusLevel.OPTIMAL,
                    normalRange = "$targetKcal kcal hedefi",
                    differenceFromBaseline = diffStr,
                    clinicalSummary = "Günlük aktif kalori hedefi başarıyla tamamlandı.",
                    isCritical = false,
                    hasMeasuredData = true
                )
            }
        }
    }

    private fun calculateOverallScore(evaluations: Collection<MetricEvaluation>): Int {
        if (evaluations.isEmpty()) return 0
        var score = 0
        evaluations.forEach { eval ->
            val metricScore = when (eval.statusLevel) {
                HealthStatusLevel.OPTIMAL -> 100
                HealthStatusLevel.GOOD -> 85
                HealthStatusLevel.LOW -> 70
                HealthStatusLevel.ATTENTION -> 55
                HealthStatusLevel.CRITICAL -> 25
            }
            score += metricScore
        }
        return (score / evaluations.size)
    }
}
