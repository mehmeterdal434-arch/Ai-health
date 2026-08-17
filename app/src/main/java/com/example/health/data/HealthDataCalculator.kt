package com.example.health.data

import java.time.Duration
import java.time.Instant
import kotlin.math.roundToInt

data class RawStepRecord(
    val startTime: Instant,
    val endTime: Instant,
    val count: Long,
    val packageName: String = ""
)

data class RawDistanceRecord(
    val startTime: Instant,
    val endTime: Instant,
    val distanceMeters: Double,
    val packageName: String = ""
)

data class RawCalorieRecord(
    val startTime: Instant,
    val endTime: Instant,
    val energyKcal: Double,
    val packageName: String = ""
)

data class RawSleepStage(
    val startTime: Instant,
    val endTime: Instant,
    val stageType: Int // 1: Awaking, 2: Sleeping/Light, 3: Deep, 4: REM, 5: Out of bed
)

data class RawSleepSession(
    val startTime: Instant,
    val endTime: Instant,
    val stages: List<RawSleepStage> = emptyList(),
    val packageName: String = ""
)

data class RawExerciseSession(
    val startTime: Instant,
    val endTime: Instant,
    val title: String? = null,
    val exerciseType: Int = 0,
    val packageName: String = ""
)

data class StepResult(
    val totalSteps: Int,
    val hasData: Boolean
)

data class DistanceResult(
    val distanceMeters: Double,
    val hasData: Boolean
)

data class CalorieResult(
    val activeCaloriesKcal: Int,
    val hasData: Boolean
)

data class HeartRateResult(
    val restingHeartRate: Int,
    val minHeartRate: Int,
    val maxHeartRate: Int,
    val averageHeartRate: Int,
    val sampleCount: Int,
    val hasData: Boolean
)

data class SleepResult(
    val sleepHours: Double,
    val totalSleepMinutes: Int,
    val deepSleepPercent: Int,
    val hasData: Boolean
)

data class SpO2Result(
    val spo2Percent: Int,
    val hasData: Boolean
)

data class ExerciseResult(
    val totalMinutes: Int,
    val sessionCount: Int,
    val hasData: Boolean
)

/**
 * Pure, deterministic Health Data Calculator and Deduplication Engine.
 * Formulates zero-hallucination metrics strictly from Health Connect telemetry.
 */
object HealthDataCalculator {

    const val SAMSUNG_HEALTH_PACKAGE = "com.sec.android.app.shealth"

    /**
     * Deduplicates and aggregates step records from multiple sources (Wearable, Samsung Health, Phone).
     * Prevents double-counting overlapping intervals.
     */
    fun calculateSteps(
        aggregateCount: Long?,
        records: List<RawStepRecord>
    ): StepResult {
        if (aggregateCount != null && aggregateCount >= 0) {
            return StepResult(
                totalSteps = aggregateCount.toInt(),
                hasData = aggregateCount > 0 || records.isNotEmpty()
            )
        }

        if (records.isEmpty()) {
            return StepResult(totalSteps = 0, hasData = false)
        }

        // If records come from multiple sources, check if Samsung Health has primary records
        val shealthRecords = records.filter { it.packageName.contains("shealth", ignoreCase = true) }
        val targetRecords = if (shealthRecords.isNotEmpty()) shealthRecords else records

        // Deduplicate overlapping time windows
        val sorted = targetRecords.sortedBy { it.startTime }
        var totalSteps = 0L
        var lastEnd = Instant.MIN

        for (record in sorted) {
            if (record.count <= 0) continue
            // If identical or fully contained within previous record window, skip duplicate
            if (record.startTime >= lastEnd) {
                totalSteps += record.count
                lastEnd = record.endTime
            } else if (record.endTime > lastEnd) {
                // Partial overlap: calculate proportion of new interval
                val totalDuration = Duration.between(record.startTime, record.endTime).toMillis()
                val nonOverlapDuration = Duration.between(lastEnd, record.endTime).toMillis()
                if (totalDuration > 0) {
                    val proportionalSteps = (record.count * nonOverlapDuration / totalDuration).coerceAtLeast(0)
                    totalSteps += proportionalSteps
                }
                lastEnd = record.endTime
            }
        }

        return StepResult(
            totalSteps = totalSteps.toInt(),
            hasData = true
        )
    }

    /**
     * Aggregates distance accurately without synthesizing from step count.
     */
    fun calculateDistance(
        aggregateMeters: Double?,
        records: List<RawDistanceRecord>
    ): DistanceResult {
        if (aggregateMeters != null && aggregateMeters >= 0.0) {
            return DistanceResult(
                distanceMeters = aggregateMeters,
                hasData = aggregateMeters > 0.0 || records.isNotEmpty()
            )
        }

        if (records.isEmpty()) {
            return DistanceResult(distanceMeters = 0.0, hasData = false)
        }

        val shealthRecords = records.filter { it.packageName.contains("shealth", ignoreCase = true) }
        val targetRecords = if (shealthRecords.isNotEmpty()) shealthRecords else records

        val sorted = targetRecords.sortedBy { it.startTime }
        var totalDistance = 0.0
        var lastEnd = Instant.MIN

        for (record in sorted) {
            if (record.distanceMeters <= 0.0) continue
            if (record.startTime >= lastEnd) {
                totalDistance += record.distanceMeters
                lastEnd = record.endTime
            } else if (record.endTime > lastEnd) {
                val totalDuration = Duration.between(record.startTime, record.endTime).toMillis()
                val nonOverlapDuration = Duration.between(lastEnd, record.endTime).toMillis()
                if (totalDuration > 0) {
                    val propDist = record.distanceMeters * nonOverlapDuration / totalDuration
                    totalDistance += propDist
                }
                lastEnd = record.endTime
            }
        }

        return DistanceResult(
            distanceMeters = totalDistance,
            hasData = totalDistance > 0.0 || records.isNotEmpty()
        )
    }

    /**
     * Aggregates active calories burned without synthesizing from steps.
     */
    fun calculateActiveCalories(
        aggregateActiveKcal: Double?,
        records: List<RawCalorieRecord>
    ): CalorieResult {
        if (aggregateActiveKcal != null && aggregateActiveKcal >= 0.0) {
            return CalorieResult(
                activeCaloriesKcal = aggregateActiveKcal.roundToInt(),
                hasData = aggregateActiveKcal > 0.0 || records.isNotEmpty()
            )
        }

        if (records.isEmpty()) {
            return CalorieResult(activeCaloriesKcal = 0, hasData = false)
        }

        val shealthRecords = records.filter { it.packageName.contains("shealth", ignoreCase = true) }
        val targetRecords = if (shealthRecords.isNotEmpty()) shealthRecords else records

        val sorted = targetRecords.sortedBy { it.startTime }
        var totalCalories = 0.0
        var lastEnd = Instant.MIN

        for (record in sorted) {
            if (record.energyKcal <= 0.0) continue
            if (record.startTime >= lastEnd) {
                totalCalories += record.energyKcal
                lastEnd = record.endTime
            } else if (record.endTime > lastEnd) {
                val totalDuration = Duration.between(record.startTime, record.endTime).toMillis()
                val nonOverlapDuration = Duration.between(lastEnd, record.endTime).toMillis()
                if (totalDuration > 0) {
                    val propCal = record.energyKcal * nonOverlapDuration / totalDuration
                    totalCalories += propCal
                }
                lastEnd = record.endTime
            }
        }

        return CalorieResult(
            activeCaloriesKcal = totalCalories.roundToInt(),
            hasData = totalCalories > 0.0 || records.isNotEmpty()
        )
    }

    /**
     * Calculates heart rate boundaries and resting baseline with noise rejection.
     */
    fun calculateHeartRate(
        restingRecords: List<Int>,
        rawSamples: List<Int>
    ): HeartRateResult {
        val validSamples = rawSamples.filter { it in 30..240 }
        val validRestingRecords = restingRecords.filter { it in 35..150 }

        if (validSamples.isEmpty() && validRestingRecords.isEmpty()) {
            return HeartRateResult(
                restingHeartRate = 0,
                minHeartRate = 0,
                maxHeartRate = 0,
                averageHeartRate = 0,
                sampleCount = 0,
                hasData = false
            )
        }

        val minHr = if (validSamples.isNotEmpty()) validSamples.minOrNull() ?: 0 else validRestingRecords.first()
        val maxHr = if (validSamples.isNotEmpty()) validSamples.maxOrNull() ?: 0 else validRestingRecords.first()
        val avgHr = if (validSamples.isNotEmpty()) validSamples.average().roundToInt() else validRestingRecords.first()

        val restingHr = when {
            validRestingRecords.isNotEmpty() -> validRestingRecords.last()
            validSamples.isNotEmpty() -> {
                // Approximate resting from lowest 20th percentile of daytime/night samples
                val sorted = validSamples.sorted()
                val lowCount = (sorted.size * 0.20).roundToInt().coerceAtLeast(1)
                sorted.take(lowCount).average().roundToInt()
            }
            else -> 0
        }

        return HeartRateResult(
            restingHeartRate = restingHr,
            minHeartRate = minHr,
            maxHeartRate = maxHr,
            averageHeartRate = avgHr,
            sampleCount = validSamples.size,
            hasData = true
        )
    }

    /**
     * Calculates sleep duration across midnight, merges duplicate sessions, and computes real deep sleep.
     */
    fun calculateSleep(
        sessions: List<RawSleepSession>
    ): SleepResult {
        if (sessions.isEmpty()) {
            return SleepResult(
                sleepHours = 0.0,
                totalSleepMinutes = 0,
                deepSleepPercent = 0,
                hasData = false
            )
        }

        // Merge overlapping session intervals [startTime, endTime]
        val intervals = sessions.map { it.startTime.toEpochMilli() to it.endTime.toEpochMilli() }
            .filter { it.second > it.first }
            .sortedBy { it.first }

        if (intervals.isEmpty()) {
            return SleepResult(sleepHours = 0.0, totalSleepMinutes = 0, deepSleepPercent = 0, hasData = false)
        }

        var totalSleepMillis = 0L
        var currentStart = intervals[0].first
        var currentEnd = intervals[0].second

        for (i in 1 until intervals.size) {
            val (start, end) = intervals[i]
            if (start <= currentEnd) {
                currentEnd = maxOf(currentEnd, end)
            } else {
                totalSleepMillis += (currentEnd - currentStart)
                currentStart = start
                currentEnd = end
            }
        }
        totalSleepMillis += (currentEnd - currentStart)

        val totalMinutes = (totalSleepMillis / (1000 * 60)).toInt()
        val hours = (totalMinutes / 6.0).roundToInt() / 10.0 // 1 decimal place e.g. 7.5

        // Extract deep sleep from real stages if available (STAGE_TYPE_DEEP = 3 or 5 depending on mapping)
        var deepSleepMillis = 0L
        var totalStageMillis = 0L
        for (session in sessions) {
            for (stage in session.stages) {
                val stageDuration = Duration.between(stage.startTime, stage.endTime).toMillis()
                if (stageDuration > 0) {
                    totalStageMillis += stageDuration
                    if (stage.stageType == 3) { // 3: Deep Sleep in Health Connect SleepSessionRecord.Stage
                        deepSleepMillis += stageDuration
                    }
                }
            }
        }

        val deepSleepPercent = if (totalStageMillis > 0) {
            ((deepSleepMillis.toDouble() / totalStageMillis.toDouble()) * 100).roundToInt().coerceIn(0, 100)
        } else {
            0 // No fake guessing
        }

        return SleepResult(
            sleepHours = hours,
            totalSleepMinutes = totalMinutes,
            deepSleepPercent = deepSleepPercent,
            hasData = totalMinutes >= 15
        )
    }

    /**
     * Calculates SpO2 percentage with physiological validation.
     */
    fun calculateSpO2(rawSamples: List<Int>): SpO2Result {
        val valid = rawSamples.filter { it in 60..100 }
        if (valid.isEmpty()) {
            return SpO2Result(spo2Percent = 0, hasData = false)
        }
        return SpO2Result(
            spo2Percent = valid.last(),
            hasData = true
        )
    }

    /**
     * Calculates exercise sessions and active minutes from real workouts.
     */
    fun calculateExercise(sessions: List<RawExerciseSession>): ExerciseResult {
        if (sessions.isEmpty()) {
            return ExerciseResult(totalMinutes = 0, sessionCount = 0, hasData = false)
        }

        val intervals = sessions.map { it.startTime.toEpochMilli() to it.endTime.toEpochMilli() }
            .filter { it.second > it.first }
            .sortedBy { it.first }

        var totalMillis = 0L
        var currentStart = intervals[0].first
        var currentEnd = intervals[0].second

        for (i in 1 until intervals.size) {
            val (start, end) = intervals[i]
            if (start <= currentEnd) {
                currentEnd = maxOf(currentEnd, end)
            } else {
                totalMillis += (currentEnd - currentStart)
                currentStart = start
                currentEnd = end
            }
        }
        totalMillis += (currentEnd - currentStart)

        val totalMins = (totalMillis / (1000 * 60)).toInt()
        return ExerciseResult(
            totalMinutes = totalMins,
            sessionCount = sessions.size,
            hasData = totalMins > 0 || sessions.isNotEmpty()
        )
    }
}
