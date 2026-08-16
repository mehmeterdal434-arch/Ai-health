package com.example.health.engine

/**
 * Customizable Medical and Lifestyle Thresholds for Rule Engine.
 * Easily adjustable for user age, gender, and personal doctor guidelines.
 */
data class UserHealthProfile(
    val age: Int = 30,
    val gender: String = "Genel",
    val stepGoal: Int = 8000,
    val sleepBaselineHours: Double = 7.5,
    val restingHeartRateBaselineBpm: Int = 65,
    val activeCalorieGoalKcal: Int = 500
)

object HealthThresholds {
    // Resting Heart Rate (BPM) (Adult reference intervals)
    const val HR_CRITICAL_BRADYCARDIA = 40 // <40 bpm requires immediate medical review
    const val HR_LOW_THRESHOLD = 60        // <60 bpm Bradycardia / Athlete low
    const val HR_NORMAL_MAX = 100          // 60-100 bpm Normal resting range
    const val HR_HIGH_THRESHOLD = 101      // 101-140 bpm Elevated / Tachycardia
    const val HR_CRITICAL_TACHYCARDIA = 145 // >145 bpm Resting tachycardia emergency

    // Oxygen Saturation SpO2 (%)
    const val SPO2_CRITICAL = 90           // <90% Severe Hypoxemia
    const val SPO2_ATTENTION = 94          // 90-94% Borderline / Attention
    const val SPO2_NORMAL_MIN = 95         // 95-100% Normal healthy range

    // Sleep Duration (Hours & % of Baseline)
    const val SLEEP_VERY_LOW_HOURS = 5.0   // <5.0 hours severe sleep deprivation
    const val SLEEP_BELOW_AVG_RATIO = 0.80 // <=80% of personal 7-day average
    const val SLEEP_OPTIMAL_MIN_HOURS = 7.0
    const val SLEEP_OPTIMAL_MAX_HOURS = 9.0

    // Stress Score (Samsung Health 0 - 100 Scale)
    const val STRESS_REST_MAX = 25         // 0-25 Rest / Calming state
    const val STRESS_LOW_MAX = 50          // 26-50 Low stress
    const val STRESS_MEDIUM_MAX = 75       // 51-75 Moderate stress
    const val STRESS_HIGH_MIN = 76         // 76-100 High stress tension

    // Active Steps Reference (% of target)
    const val STEPS_LOW_RATIO = 0.50       // <50% of daily goal (Sedentary)
    const val STEPS_MODERATE_RATIO = 0.90   // 50-89% of daily goal
    const val STEPS_ACHIEVED_RATIO = 1.00   // 100% of daily goal

    // Calories Reference (% of target)
    const val CALORIES_LOW_RATIO = 0.60
    const val CALORIES_ACHIEVED_RATIO = 1.00
}
