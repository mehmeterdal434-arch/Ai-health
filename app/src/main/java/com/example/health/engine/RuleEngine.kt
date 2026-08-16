package com.example.health.engine

import com.example.health.model.*
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Deterministic Rule Engine for Samsung Health telemetry.
 * Operates completely offline to guarantee medical boundaries and safety standards.
 * The outputs of this engine are the ONLY inputs permitted for LLM natural language generation.
 */
object RuleEngine {

    fun evaluateAll(
        record: DailyHealthRecord,
        profile: UserHealthProfile = UserHealthProfile()
    ): StructuredHealthSummary {
        val hrEval = evaluateHeartRate(record.restingHeartRate)
        val sleepEval = evaluateSleep(record.sleepHours, profile.sleepBaselineHours)
        val stepsEval = evaluateSteps(record.steps, profile.stepGoal)
        val spO2Eval = evaluateSpO2(record.spO2Percent)
        val stressEval = evaluateStress(record.stressScore)
        val calEval = evaluateCalories(record.activeCaloriesKcal, profile.activeCalorieGoalKcal)

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

        // Overall readiness / score calculation
        val overallScore = calculateOverallScore(evalMap)
        val overallStatus = when {
            criticalAlerts.isNotEmpty() -> HealthStatusLevel.CRITICAL
            overallScore >= 80 -> HealthStatusLevel.OPTIMAL
            overallScore >= 65 -> HealthStatusLevel.GOOD
            overallScore >= 50 -> HealthStatusLevel.ATTENTION
            else -> HealthStatusLevel.CRITICAL
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
                    criticalAlertMessage = "Kritik Düşük Kalp Hızı ($restingBpm bpm). Baş dönmesi veya baygınlık hissi varsa derhal bir sağlık kuruluşuna başvurun."
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
                    clinicalSummary = "Hafif bradikardi / sporcu kalp ritmi seviyesi.",
                    isCritical = false
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
                    isCritical = false
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
                    clinicalSummary = "Yükselmiş dinlenik nabız (Taşikardi eğilimi). Kafein, stres veya yorgunluk etkileyebilir.",
                    isCritical = false
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
                    criticalAlertMessage = "Kritik Yüksek Kalp Hızı ($restingBpm bpm). Dinlenme halindeyken bu değer acil tıbbi değerlendirme gerektirebilir."
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
            "+${String.format(Locale.US, "%.1f", diffHours)} sa ortalamanın üstünde"
        } else {
            "${String.format(Locale.US, "%.1f", diffHours)} sa ortalamanın altında"
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
                    isCritical = false
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
                    clinicalSummary = "Kişisel ortalamanızın %20'den fazla altında uyku süresi.",
                    isCritical = false
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
                    clinicalSummary = "Mükemmel dinlenme ve biyolojik onarım süresi.",
                    isCritical = false
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
                    isCritical = false
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
                    clinicalSummary = "Kabul edilebilir uyku süresi.",
                    isCritical = false
                )
            }
        }
    }

    /**
     * Evaluates daily steps relative to user target and WHO guidelines.
     */
    fun evaluateSteps(steps: Int, target: Int): MetricEvaluation {
        val percent = ((steps.toDouble() / target.toDouble()) * 100).roundToInt()
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
                    clinicalSummary = "Hareketsiz / sedanter gün profili.",
                    isCritical = false
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
                    isCritical = false
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
                    clinicalSummary = "Günlük kardiyo ve adım hedefine başarıyla ulaşıldı.",
                    isCritical = false
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
                    isCritical = false
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
                    criticalAlertMessage = "Kritik Düşük Oksijen Satürasyonu (%$spO2). Nefes darlığı veya göğüs baskısı varsa lütfen vakit kaybetmeden acil tıbbi destek alın."
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
                    clinicalSummary = "Sınırda oksijen satürasyonu. Derin nefes egzersizi ve ortam havalandırması önerilir.",
                    isCritical = false
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
                    differenceFromBaseline = "Optimal solunum oksijenasyonu",
                    clinicalSummary = "Kandaki oksijen doygunluğu mükemmel seviyede.",
                    isCritical = false
                )
            }
        }
    }

    /**
     * Evaluates Samsung Health Stress Score (0-100).
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
                    isCritical = false
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
                    isCritical = false
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
                    clinicalSummary = "Orta seviye zihinsel/fiziksel stres. Kısa molalar faydalı olabilir.",
                    isCritical = false
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
                    isCritical = false
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
                    isCritical = false
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
                    isCritical = false
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
                    isCritical = false
                )
            }
        }
    }

    private fun calculateOverallScore(evaluations: Map<MetricType, MetricEvaluation>): Int {
        var score = 0
        evaluations.values.forEach { eval ->
            val metricScore = when (eval.statusLevel) {
                HealthStatusLevel.OPTIMAL -> 100
                HealthStatusLevel.GOOD -> 85
                HealthStatusLevel.LOW -> 70
                HealthStatusLevel.ATTENTION -> 55
                HealthStatusLevel.CRITICAL -> 25
            }
            score += metricScore
        }
        return (score / evaluations.size.coerceAtLeast(1))
    }
}
