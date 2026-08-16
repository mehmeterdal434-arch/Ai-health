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

class HealthDataRepository(
    private val healthDao: HealthDao,
    private val healthConnectManager: HealthConnectManager
) {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    fun getTodayDateString(): String = dateFormat.format(Date())

    val allDailyRecords: Flow<List<DailyHealthRecord>> = healthDao.getAllDailyRecords().map { entities ->
        entities.map { it.toDomain() }
    }

    val allAiInsights: Flow<List<AiGeneratedInsight>> = healthDao.getAllAiInsights().map { entities ->
        entities.map { it.toDomain() }
    }

    /**
     * Attempts to read live synchronized data from Health Connect (Samsung Health) for today and the past 7 days.
     */
    suspend fun syncWithHealthConnect(profile: UserHealthProfile = UserHealthProfile()): DailyHealthRecord? {
        val today = LocalDate.now()
        val todayStr = today.format(DateTimeFormatter.ISO_LOCAL_DATE)

        // 1. Fetch today's live data
        val liveTodayRecord = healthConnectManager.fetchRealHealthData(today)
        if (liveTodayRecord != null) {
            saveRecord(liveTodayRecord, profile)
        }

        // 2. Fetch past 6 days history if missing or to update
        for (i in 1..6) {
            val pastDate = today.minusDays(i.toLong())
            val pastDateStr = pastDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
            val pastRecord = healthConnectManager.fetchRealHealthData(pastDate)
            if (pastRecord != null && (pastRecord.steps > 0 || pastRecord.sleepHours > 0 || pastRecord.restingHeartRate > 0)) {
                saveRecord(pastRecord, profile)
            }
        }

        return liveTodayRecord ?: getTodayRecord()
    }

    suspend fun getTodayRecord(): DailyHealthRecord {
        val todayStr = getTodayDateString()
        val entity = healthDao.getRecordByDate(todayStr)
        return if (entity != null) {
            entity.toDomain()
        } else {
            // Check Health Connect directly first
            val live = healthConnectManager.fetchRealHealthData(LocalDate.now())
            if (live != null) {
                saveRecord(live)
                live
            } else {
                // Initialize clean empty record for today
                val blankRecord = DailyHealthRecord(
                    date = todayStr,
                    timestamp = System.currentTimeMillis(),
                    steps = 0,
                    restingHeartRate = 0,
                    minHeartRate = 0,
                    maxHeartRate = 0,
                    sleepHours = 0.0,
                    deepSleepPercent = 0,
                    spO2Percent = 0,
                    stressScore = 0,
                    activeCaloriesKcal = 0
                )
                saveRecord(blankRecord)
                blankRecord
            }
        }
    }

    suspend fun saveRecord(record: DailyHealthRecord, profile: UserHealthProfile = UserHealthProfile()) {
        val summary = RuleEngine.evaluateAll(record, profile)
        val entity = HealthRecordEntity(
            date = record.date,
            timestamp = record.timestamp,
            steps = record.steps,
            restingHeartRate = record.restingHeartRate,
            minHeartRate = record.minHeartRate,
            maxHeartRate = record.maxHeartRate,
            sleepHours = record.sleepHours,
            deepSleepPercent = record.deepSleepPercent,
            spO2Percent = record.spO2Percent,
            stressScore = record.stressScore,
            activeCaloriesKcal = record.activeCaloriesKcal,
            overallScore = summary.overallScore,
            overallStatus = summary.overallStatus.tag
        )
        healthDao.insertOrUpdateDailyRecord(entity)
    }

    fun getHistoricalTrend(metricType: MetricType): Flow<List<HistoricalTrendPoint>> {
        return healthDao.getRecent7Days().map { list ->
            list.reversed().map { entity ->
                val dayLabel = try {
                    val parsed = dateFormat.parse(entity.date)
                    val outFmt = SimpleDateFormat("EEE", Locale("tr"))
                    outFmt.format(parsed ?: Date())
                } catch (e: Exception) {
                    entity.date.takeLast(5)
                }

                when (metricType) {
                    MetricType.STEPS -> HistoricalTrendPoint(
                        label = dayLabel,
                        value = entity.steps.toFloat(),
                        formattedValue = "${entity.steps}",
                        category = if (entity.steps >= 8000) HealthCategory.ACHIEVED else HealthCategory.MODERATE
                    )
                    MetricType.HEART_RATE -> HistoricalTrendPoint(
                        label = dayLabel,
                        value = entity.restingHeartRate.toFloat(),
                        formattedValue = "${entity.restingHeartRate} bpm",
                        category = if (entity.restingHeartRate in 60..100) HealthCategory.NORMAL else HealthCategory.LOW
                    )
                    MetricType.SLEEP -> HistoricalTrendPoint(
                        label = dayLabel,
                        value = entity.sleepHours.toFloat(),
                        formattedValue = String.format(Locale.US, "%.1f sa", entity.sleepHours),
                        category = if (entity.sleepHours >= 7.0) HealthCategory.OPTIMAL else HealthCategory.BELOW_AVERAGE
                    )
                    MetricType.SPO2 -> HistoricalTrendPoint(
                        label = dayLabel,
                        value = entity.spO2Percent.toFloat(),
                        formattedValue = "%${entity.spO2Percent}",
                        category = if (entity.spO2Percent >= 95) HealthCategory.NORMAL else HealthCategory.ATTENTION
                    )
                    MetricType.STRESS -> HistoricalTrendPoint(
                        label = dayLabel,
                        value = entity.stressScore.toFloat(),
                        formattedValue = "${entity.stressScore}",
                        category = if (entity.stressScore <= 50) HealthCategory.LOW else HealthCategory.MEDIUM
                    )
                    MetricType.CALORIES -> HistoricalTrendPoint(
                        label = dayLabel,
                        value = entity.activeCaloriesKcal.toFloat(),
                        formattedValue = "${entity.activeCaloriesKcal} kcal",
                        category = if (entity.activeCaloriesKcal >= 500) HealthCategory.ACHIEVED else HealthCategory.NORMAL
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

    private fun HealthRecordEntity.toDomain() = DailyHealthRecord(
        date = date,
        timestamp = timestamp,
        steps = steps,
        restingHeartRate = restingHeartRate,
        minHeartRate = minHeartRate,
        maxHeartRate = maxHeartRate,
        sleepHours = sleepHours,
        deepSleepPercent = deepSleepPercent,
        spO2Percent = spO2Percent,
        stressScore = stressScore,
        activeCaloriesKcal = activeCaloriesKcal
    )

    private fun AiInsightEntity.toDomain() = AiGeneratedInsight(
        id = id,
        timestamp = timestamp,
        date = date,
        metricType = metricType?.let { MetricType.valueOf(it) },
        explanationText = explanationText,
        practicalTip = practicalTip,
        disclaimer = disclaimer,
        criticalMedicalWarning = criticalMedicalWarning
    )
}
