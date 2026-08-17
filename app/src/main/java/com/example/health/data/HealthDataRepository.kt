package com.example.health.data

import android.util.Log
import com.example.health.data.local.AiInsightEntity
import com.example.health.data.local.HealthDao
import com.example.health.data.local.HealthRecordEntity
import com.example.health.engine.RuleEngine
import com.example.health.engine.UserHealthProfile
import com.example.health.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

sealed class SyncState {
    object Idle : SyncState()
    object Syncing : SyncState()
    data class Success(val message: String, val syncedDateCount: Int, val timestamp: Long = System.currentTimeMillis()) : SyncState()
    data class Error(val message: String, val timestamp: Long = System.currentTimeMillis()) : SyncState()
}

class HealthDataRepository(
    private val healthDao: HealthDao,
    private val healthConnectManager: HealthConnectManager
) {
    private val tag = "HealthDataRepository"
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    fun getTodayDateString(): String = dateFormat.format(Date())

    val allDailyRecords: Flow<List<DailyHealthRecord>> = healthDao.getAllDailyRecords().map { entities ->
        entities.map { it.toDomain() }
    }

    val allAiInsights: Flow<List<AiGeneratedInsight>> = healthDao.getAllAiInsights().map { entities ->
        entities.map { it.toDomain() }
    }

    /**
     * Performs synchronized data ingestion from Health Connect (Samsung Health) for today and past 7 days.
     */
    suspend fun syncWithHealthConnect(profile: UserHealthProfile = UserHealthProfile()): SyncState {
        return try {
            val availability = healthConnectManager.checkHealthConnectAvailability()
            if (availability != HealthConnectAvailability.INSTALLED) {
                return SyncState.Error("Health Connect cihazda yüklü değil veya erişilemiyor.")
            }

            val permState = healthConnectManager.getPermissionState()
            if (!permState.anyGranted) {
                return SyncState.Error("Health Connect sağlık verisi okuma izinleri verilmedi.")
            }

            val today = LocalDate.now()
            var syncedCount = 0

            // 1. Fetch and store today's live data
            val liveTodayRecord = healthConnectManager.fetchRealHealthData(today)
            if (liveTodayRecord != null) {
                saveRecord(liveTodayRecord, profile)
                syncedCount++
            }

            // 2. Fetch past 6 days history
            for (i in 1..6) {
                val pastDate = today.minusDays(i.toLong())
                val pastRecord = healthConnectManager.fetchRealHealthData(pastDate)
                if (pastRecord != null && (pastRecord.hasStepsData || pastRecord.hasSleepData || pastRecord.hasHeartRateData)) {
                    saveRecord(pastRecord, profile)
                    syncedCount++
                }
            }

            SyncState.Success(
                message = "Samsung Health ve Health Connect verileri başarıyla güncellendi.",
                syncedDateCount = syncedCount
            )
        } catch (e: Exception) {
            Log.e(tag, "Sync failed", e)
            SyncState.Error("Eşitleme sırasında bir hata oluştu: ${e.localizedMessage ?: "Bağlantı kesildi"}")
        }
    }

    suspend fun getRecordForDate(dateStr: String, profile: UserHealthProfile = UserHealthProfile()): DailyHealthRecord {
        val entity = healthDao.getRecordByDate(dateStr)
        if (entity != null) {
            return entity.toDomain()
        }

        // If today or recent, try fetching directly from Health Connect
        try {
            val targetDate = LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE)
            val live = healthConnectManager.fetchRealHealthData(targetDate)
            if (live != null) {
                saveRecord(live, profile)
                return live
            }
        } catch (e: Exception) {
            Log.w(tag, "Date parsing / live fetch error for $dateStr", e)
        }

        // Return empty non-hallucinated record
        val blankRecord = DailyHealthRecord(
            date = dateStr,
            timestamp = System.currentTimeMillis(),
            steps = 0,
            hasStepsData = false,
            restingHeartRate = 0,
            hasHeartRateData = false,
            minHeartRate = 0,
            maxHeartRate = 0,
            sleepHours = 0.0,
            hasSleepData = false,
            deepSleepPercent = 0,
            spO2Percent = 0,
            hasSpO2Data = false,
            stressScore = 0,
            hasStressData = false,
            activeCaloriesKcal = 0,
            hasCaloriesData = false,
            distanceMeters = 0.0,
            activeMinutes = 0
        )
        saveRecord(blankRecord, profile)
        return blankRecord
    }

    suspend fun getTodayRecord(profile: UserHealthProfile = UserHealthProfile()): DailyHealthRecord {
        return getRecordForDate(getTodayDateString(), profile)
    }

    suspend fun saveRecord(record: DailyHealthRecord, profile: UserHealthProfile = UserHealthProfile()) {
        val summary = RuleEngine.evaluateAll(record, profile)
        val entity = HealthRecordEntity(
            date = record.date,
            timestamp = record.timestamp,
            steps = record.steps,
            hasStepsData = record.hasStepsData,
            restingHeartRate = record.restingHeartRate,
            hasHeartRateData = record.hasHeartRateData,
            minHeartRate = record.minHeartRate,
            maxHeartRate = record.maxHeartRate,
            sleepHours = record.sleepHours,
            hasSleepData = record.hasSleepData,
            deepSleepPercent = record.deepSleepPercent,
            spO2Percent = record.spO2Percent,
            hasSpO2Data = record.hasSpO2Data,
            stressScore = record.stressScore,
            hasStressData = record.hasStressData,
            activeCaloriesKcal = record.activeCaloriesKcal,
            hasCaloriesData = record.hasCaloriesData,
            distanceMeters = record.distanceMeters,
            activeMinutes = record.activeMinutes,
            overallScore = summary.overallScore,
            overallStatus = summary.overallStatus.tag
        )
        healthDao.insertOrUpdateDailyRecord(entity)
    }

    fun getHistoricalTrend(metricType: MetricType, daysCount: Int = 7): Flow<List<HistoricalTrendPoint>> {
        val queryFlow = if (daysCount <= 7) healthDao.getRecent7Days() else healthDao.getRecent30Days()
        return queryFlow.map { list ->
            list.reversed().map { entity ->
                val dayLabel = try {
                    val parsed = dateFormat.parse(entity.date)
                    val outFmt = if (daysCount <= 7) SimpleDateFormat("EEE", Locale("tr")) else SimpleDateFormat("d MMM", Locale("tr"))
                    outFmt.format(parsed ?: Date())
                } catch (e: Exception) {
                    entity.date.takeLast(5)
                }

                when (metricType) {
                    MetricType.STEPS -> HistoricalTrendPoint(
                        label = dayLabel,
                        value = entity.steps.toFloat(),
                        formattedValue = "${entity.steps}",
                        category = if (entity.steps >= 8000) HealthCategory.ACHIEVED else HealthCategory.MODERATE,
                        date = entity.date
                    )
                    MetricType.HEART_RATE -> HistoricalTrendPoint(
                        label = dayLabel,
                        value = entity.restingHeartRate.toFloat(),
                        formattedValue = if (entity.restingHeartRate > 0) "${entity.restingHeartRate} bpm" else "-",
                        category = if (entity.restingHeartRate in 60..100) HealthCategory.NORMAL else HealthCategory.LOW,
                        date = entity.date
                    )
                    MetricType.SLEEP -> HistoricalTrendPoint(
                        label = dayLabel,
                        value = entity.sleepHours.toFloat(),
                        formattedValue = if (entity.sleepHours > 0) String.format(Locale.US, "%.1f sa", entity.sleepHours) else "-",
                        category = if (entity.sleepHours >= 7.0) HealthCategory.OPTIMAL else HealthCategory.BELOW_AVERAGE,
                        date = entity.date
                    )
                    MetricType.SPO2 -> HistoricalTrendPoint(
                        label = dayLabel,
                        value = entity.spO2Percent.toFloat(),
                        formattedValue = if (entity.spO2Percent > 0) "%${entity.spO2Percent}" else "-",
                        category = if (entity.spO2Percent >= 95) HealthCategory.NORMAL else HealthCategory.ATTENTION,
                        date = entity.date
                    )
                    MetricType.STRESS -> HistoricalTrendPoint(
                        label = dayLabel,
                        value = entity.stressScore.toFloat(),
                        formattedValue = if (entity.stressScore > 0) "${entity.stressScore}" else "-",
                        category = if (entity.stressScore <= 50) HealthCategory.LOW else HealthCategory.MEDIUM,
                        date = entity.date
                    )
                    MetricType.CALORIES -> HistoricalTrendPoint(
                        label = dayLabel,
                        value = entity.activeCaloriesKcal.toFloat(),
                        formattedValue = "${entity.activeCaloriesKcal} kcal",
                        category = if (entity.activeCaloriesKcal >= 500) HealthCategory.ACHIEVED else HealthCategory.NORMAL,
                        date = entity.date
                    )
                }
            }
        }
    }

    suspend fun saveAiInsight(insight: AiGeneratedInsight): Long {
        val entity = AiInsightEntity(
            date = insight.date,
            timestamp = insight.timestamp,
            metricType = insight.metricType?.name,
            explanationText = insight.explanationText,
            practicalTip = insight.practicalTip,
            disclaimer = insight.disclaimer,
            criticalMedicalWarning = insight.criticalMedicalWarning
        )
        return healthDao.insertAiInsight(entity)
    }

    fun getLatestInsightForMetric(metricType: MetricType?, date: String): Flow<AiGeneratedInsight?> {
        return if (metricType == null) {
            healthDao.getLatestDayInsight(date).map { it?.toDomain() }
        } else {
            healthDao.getLatestMetricInsight(date, metricType.name).map { it?.toDomain() }
        }
    }

    suspend fun clearAllLocalCache() {
        healthDao.clearAllRecords()
        healthDao.clearAllAiInsights()
    }

    private fun HealthRecordEntity.toDomain() = DailyHealthRecord(
        date = date,
        timestamp = timestamp,
        steps = steps,
        hasStepsData = hasStepsData,
        restingHeartRate = restingHeartRate,
        hasHeartRateData = hasHeartRateData,
        minHeartRate = minHeartRate,
        maxHeartRate = maxHeartRate,
        sleepHours = sleepHours,
        hasSleepData = hasSleepData,
        deepSleepPercent = deepSleepPercent,
        spO2Percent = spO2Percent,
        hasSpO2Data = hasSpO2Data,
        stressScore = stressScore,
        hasStressData = hasStressData,
        activeCaloriesKcal = activeCaloriesKcal,
        hasCaloriesData = hasCaloriesData,
        distanceMeters = distanceMeters,
        activeMinutes = activeMinutes
    )

    private fun AiInsightEntity.toDomain() = AiGeneratedInsight(
        id = id,
        timestamp = timestamp,
        date = date,
        metricType = metricType?.let { runCatching { MetricType.valueOf(it) }.getOrNull() },
        explanationText = explanationText,
        practicalTip = practicalTip,
        disclaimer = disclaimer,
        criticalMedicalWarning = criticalMedicalWarning
    )
}
