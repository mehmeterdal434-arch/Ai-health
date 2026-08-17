package com.example.health.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.*
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.example.health.model.DailyHealthRecord
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

enum class HealthConnectAvailability {
    INSTALLED,
    NOT_INSTALLED,
    NOT_SUPPORTED
}

data class HealthPermissionState(
    val stepsGranted: Boolean = false,
    val heartRateGranted: Boolean = false,
    val sleepGranted: Boolean = false,
    val spO2Granted: Boolean = false,
    val caloriesGranted: Boolean = false,
    val distanceGranted: Boolean = false,
    val exerciseGranted: Boolean = false
) {
    val allGranted: Boolean
        get() = stepsGranted && heartRateGranted && sleepGranted && spO2Granted && caloriesGranted && distanceGranted && exerciseGranted
    
    val anyGranted: Boolean
        get() = stepsGranted || heartRateGranted || sleepGranted || spO2Granted || caloriesGranted || distanceGranted || exerciseGranted

    val grantedCount: Int
        get() = listOf(stepsGranted, heartRateGranted, sleepGranted, spO2Granted, caloriesGranted, distanceGranted, exerciseGranted).count { it }

    val totalCount: Int = 7
}

/**
 * Health Connect Client & Direct Data Syncer for Samsung Health and Health Connect.
 * Performs accurate aggregation, interval deduplication, and zero-hallucination metric extraction.
 */
class HealthConnectManager(private val context: Context) {

    private val tag = "HealthConnectManager"
    val healthConnectPackage = "com.google.android.apps.healthdata"
    val samsungHealthPackage = "com.sec.android.app.shealth"

    val requiredPermissions = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getReadPermission(RestingHeartRateRecord::class),
        HealthPermission.getReadPermission(SleepSessionRecord::class),
        HealthPermission.getReadPermission(OxygenSaturationRecord::class),
        HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(DistanceRecord::class),
        HealthPermission.getReadPermission(ExerciseSessionRecord::class)
    )

    fun isHealthConnectAvailable(): Boolean {
        return try {
            val status = HealthConnectClient.getSdkStatus(context)
            status == HealthConnectClient.SDK_AVAILABLE
        } catch (e: Throwable) {
            false
        }
    }

    fun checkHealthConnectAvailability(): HealthConnectAvailability {
        return try {
            val status = HealthConnectClient.getSdkStatus(context)
            when (status) {
                HealthConnectClient.SDK_AVAILABLE -> HealthConnectAvailability.INSTALLED
                HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> HealthConnectAvailability.NOT_INSTALLED
                else -> {
                    try {
                        context.packageManager.getPackageInfo(healthConnectPackage, 0)
                        HealthConnectAvailability.INSTALLED
                    } catch (e: Throwable) {
                        HealthConnectAvailability.NOT_INSTALLED
                    }
                }
            }
        } catch (e: Throwable) {
            HealthConnectAvailability.NOT_SUPPORTED
        }
    }

    fun isSamsungHealthInstalled(): Boolean {
        return try {
            context.packageManager.getPackageInfo(samsungHealthPackage, 0)
            true
        } catch (e: Throwable) {
            false
        }
    }

    private fun getClient(): HealthConnectClient? {
        return try {
            if (isHealthConnectAvailable()) {
                HealthConnectClient.getOrCreate(context)
            } else {
                null
            }
        } catch (e: Throwable) {
            Log.e(tag, "HealthConnectClient error", e)
            null
        }
    }

    suspend fun getPermissionState(): HealthPermissionState {
        val client = getClient() ?: return HealthPermissionState()
        return try {
            val granted = client.permissionController.getGrantedPermissions()
            HealthPermissionState(
                stepsGranted = granted.contains(HealthPermission.getReadPermission(StepsRecord::class)),
                heartRateGranted = granted.contains(HealthPermission.getReadPermission(HeartRateRecord::class)) ||
                        granted.contains(HealthPermission.getReadPermission(RestingHeartRateRecord::class)),
                sleepGranted = granted.contains(HealthPermission.getReadPermission(SleepSessionRecord::class)),
                spO2Granted = granted.contains(HealthPermission.getReadPermission(OxygenSaturationRecord::class)),
                caloriesGranted = granted.contains(HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class)) ||
                        granted.contains(HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class)),
                distanceGranted = granted.contains(HealthPermission.getReadPermission(DistanceRecord::class)),
                exerciseGranted = granted.contains(HealthPermission.getReadPermission(ExerciseSessionRecord::class))
            )
        } catch (e: Throwable) {
            Log.e(tag, "Error reading permissions", e)
            HealthPermissionState()
        }
    }

    /**
     * Reads real health records from Health Connect (synced by Samsung Health / Wearable) for the given date.
     * Uses Health Connect native deduplicated aggregation and interval normalization.
     */
    suspend fun fetchRealHealthData(localDate: LocalDate): DailyHealthRecord? {
        val client = getClient() ?: return null
        val zone = ZoneId.systemDefault()
        val startOfDay = localDate.atStartOfDay(zone).toInstant()
        val endOfDay = localDate.plusDays(1).atStartOfDay(zone).toInstant()
        val now = Instant.now()
        val effectiveEnd = if (endOfDay.isAfter(now)) now else endOfDay

        val dateStr = localDate.format(DateTimeFormatter.ISO_LOCAL_DATE)

        return try {
            // 1. Read & Deduplicate Steps
            var stepResult = StepResult(totalSteps = 0, hasData = false)
            try {
                // Primary: Health Connect aggregate with COUNT_TOTAL automatically deduplicates overlapping time spans
                val stepsAggregate = client.aggregate(
                    AggregateRequest(
                        metrics = setOf(StepsRecord.COUNT_TOTAL),
                        timeRangeFilter = TimeRangeFilter.between(startOfDay, effectiveEnd)
                    )
                )
                val count = stepsAggregate[StepsRecord.COUNT_TOTAL]

                val stepsResponse = client.readRecords(
                    ReadRecordsRequest(
                        recordType = StepsRecord::class,
                        timeRangeFilter = TimeRangeFilter.between(startOfDay, effectiveEnd)
                    )
                )
                val rawStepRecords = stepsResponse.records.map {
                    RawStepRecord(
                        startTime = it.startTime,
                        endTime = it.endTime,
                        count = it.count,
                        packageName = it.metadata.dataOrigin.packageName
                    )
                }

                stepResult = HealthDataCalculator.calculateSteps(count, rawStepRecords)
            } catch (e: Exception) {
                Log.w(tag, "Steps read/aggregation error", e)
            }

            // 2. Read Distance
            var distanceResult = DistanceResult(distanceMeters = 0.0, hasData = false)
            try {
                val distanceAggregate = client.aggregate(
                    AggregateRequest(
                        metrics = setOf(DistanceRecord.DISTANCE_TOTAL),
                        timeRangeFilter = TimeRangeFilter.between(startOfDay, effectiveEnd)
                    )
                )
                val dist = distanceAggregate[DistanceRecord.DISTANCE_TOTAL]

                val distResponse = client.readRecords(
                    ReadRecordsRequest(
                        recordType = DistanceRecord::class,
                        timeRangeFilter = TimeRangeFilter.between(startOfDay, effectiveEnd)
                    )
                )
                val rawDistRecords = distResponse.records.map {
                    RawDistanceRecord(
                        startTime = it.startTime,
                        endTime = it.endTime,
                        distanceMeters = it.distance.inMeters,
                        packageName = it.metadata.dataOrigin.packageName
                    )
                }

                distanceResult = HealthDataCalculator.calculateDistance(dist?.inMeters, rawDistRecords)
            } catch (e: Exception) {
                Log.w(tag, "Distance read error", e)
            }

            // 3. Read Resting Heart Rate & Continuous Heart Rate
            var hrResult = HeartRateResult(0, 0, 0, 0, 0, false)
            try {
                val restingHrResponse = client.readRecords(
                    ReadRecordsRequest(
                        recordType = RestingHeartRateRecord::class,
                        timeRangeFilter = TimeRangeFilter.between(startOfDay, effectiveEnd)
                    )
                )
                val restingList = restingHrResponse.records.map { it.beatsPerMinute.toInt() }

                val hrResponse = client.readRecords(
                    ReadRecordsRequest(
                        recordType = HeartRateRecord::class,
                        timeRangeFilter = TimeRangeFilter.between(startOfDay, effectiveEnd)
                    )
                )
                val allSamples = hrResponse.records.flatMap { it.samples }.map { it.beatsPerMinute.toInt() }

                hrResult = HealthDataCalculator.calculateHeartRate(restingList, allSamples)
            } catch (e: Exception) {
                Log.w(tag, "Heart rate read error", e)
            }

            // 4. Read Sleep (Look from previous evening 18:00 to current day 18:00 across midnight)
            var sleepResult = SleepResult(0.0, 0, 0, false)
            try {
                val sleepStart = localDate.minusDays(1).atTime(18, 0).atZone(zone).toInstant()
                val sleepEnd = localDate.atTime(18, 0).atZone(zone).toInstant()
                val sleepResponse = client.readRecords(
                    ReadRecordsRequest(
                        recordType = SleepSessionRecord::class,
                        timeRangeFilter = TimeRangeFilter.between(sleepStart, sleepEnd)
                    )
                )
                val rawSleepSessions = sleepResponse.records.map { session ->
                    val stages = session.stages.map { stage ->
                        RawSleepStage(
                            startTime = stage.startTime,
                            endTime = stage.endTime,
                            stageType = stage.stage
                        )
                    }
                    RawSleepSession(
                        startTime = session.startTime,
                        endTime = session.endTime,
                        stages = stages,
                        packageName = session.metadata.dataOrigin.packageName
                    )
                }

                sleepResult = HealthDataCalculator.calculateSleep(rawSleepSessions)
            } catch (e: Exception) {
                Log.w(tag, "Sleep read error", e)
            }

            // 5. Read SpO2 (Oxygen Saturation)
            var spO2Result = SpO2Result(0, false)
            try {
                val spo2Response = client.readRecords(
                    ReadRecordsRequest(
                        recordType = OxygenSaturationRecord::class,
                        timeRangeFilter = TimeRangeFilter.between(startOfDay, effectiveEnd)
                    )
                )
                val samples = spo2Response.records.map { it.percentage.value.toInt() }
                spO2Result = HealthDataCalculator.calculateSpO2(samples)
            } catch (e: Exception) {
                Log.w(tag, "SpO2 read error", e)
            }

            // 6. Read Active Calories Burned (Zero synthetic guessing!)
            var calResult = CalorieResult(0, false)
            try {
                val activeCalAggregate = client.aggregate(
                    AggregateRequest(
                        metrics = setOf(ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL),
                        timeRangeFilter = TimeRangeFilter.between(startOfDay, effectiveEnd)
                    )
                )
                val activeEnergy = activeCalAggregate[ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL]

                val activeRecordsResponse = client.readRecords(
                    ReadRecordsRequest(
                        recordType = ActiveCaloriesBurnedRecord::class,
                        timeRangeFilter = TimeRangeFilter.between(startOfDay, effectiveEnd)
                    )
                )
                val rawCalRecords = activeRecordsResponse.records.map {
                    RawCalorieRecord(
                        startTime = it.startTime,
                        endTime = it.endTime,
                        energyKcal = it.energy.inKilocalories,
                        packageName = it.metadata.dataOrigin.packageName
                    )
                }

                calResult = HealthDataCalculator.calculateActiveCalories(
                    activeEnergy?.inKilocalories,
                    rawCalRecords
                )
            } catch (e: Exception) {
                Log.w(tag, "Calories read error", e)
            }

            // 7. Read Real Exercise Sessions & Active Minutes (Zero synthetic guessing!)
            var exerciseResult = ExerciseResult(0, 0, false)
            try {
                val exerciseResponse = client.readRecords(
                    ReadRecordsRequest(
                        recordType = ExerciseSessionRecord::class,
                        timeRangeFilter = TimeRangeFilter.between(startOfDay, effectiveEnd)
                    )
                )
                val rawExercises = exerciseResponse.records.map {
                    RawExerciseSession(
                        startTime = it.startTime,
                        endTime = it.endTime,
                        title = it.title,
                        exerciseType = it.exerciseType,
                        packageName = it.metadata.dataOrigin.packageName
                    )
                }
                exerciseResult = HealthDataCalculator.calculateExercise(rawExercises)
            } catch (e: Exception) {
                Log.w(tag, "Exercise read error", e)
            }

            // 8. Stress Score (Only if resting HR is measured)
            var stressScore = 0
            var hasStress = false
            if (hrResult.hasData && hrResult.restingHeartRate > 0) {
                val rhr = hrResult.restingHeartRate
                stressScore = when {
                    rhr > 85 -> ((rhr - 85) * 3 + 60).coerceIn(10, 95)
                    rhr < 60 -> ((rhr - 50) * 1.5 + 15).toInt().coerceIn(10, 40)
                    else -> ((rhr - 60) * 1.2 + 25).toInt().coerceIn(15, 60)
                }
                hasStress = true
            }

            DailyHealthRecord(
                date = dateStr,
                timestamp = System.currentTimeMillis(),
                steps = stepResult.totalSteps,
                hasStepsData = stepResult.hasData,
                restingHeartRate = hrResult.restingHeartRate,
                hasHeartRateData = hrResult.hasData,
                minHeartRate = hrResult.minHeartRate,
                maxHeartRate = hrResult.maxHeartRate,
                sleepHours = sleepResult.sleepHours,
                hasSleepData = sleepResult.hasData,
                deepSleepPercent = sleepResult.deepSleepPercent,
                spO2Percent = spO2Result.spo2Percent,
                hasSpO2Data = spO2Result.hasData,
                stressScore = stressScore,
                hasStressData = hasStress,
                activeCaloriesKcal = calResult.activeCaloriesKcal,
                hasCaloriesData = calResult.hasData,
                distanceMeters = distanceResult.distanceMeters,
                activeMinutes = exerciseResult.totalMinutes
            )
        } catch (e: Exception) {
            Log.e(tag, "Error fetching live data from Health Connect", e)
            null
        }
    }

    fun getPlayStoreIntentForHealthConnect(): Intent {
        return Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("https://play.google.com/store/apps/details?id=$healthConnectPackage")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    fun getPlayStoreIntentForSamsungHealth(): Intent {
        return Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("https://play.google.com/store/apps/details?id=$samsungHealthPackage")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    fun getSamsungHealthLaunchIntent(): Intent? {
        return context.packageManager.getLaunchIntentForPackage(samsungHealthPackage)
    }

    fun getHealthConnectSettingsIntent(): Intent {
        return Intent("androidx.health.ACTION_HEALTH_CONNECT_SETTINGS").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
}
