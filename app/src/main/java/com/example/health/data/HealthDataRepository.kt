package com.example.health.data

import com.example.health.data.local.AiInsightEntity
import com.example.health.data.local.HealthDao
import com.example.health.data.local.HealthRecordEntity
import com.example.health.engine.RuleEngine
import com.example.health.engine.UserHealthProfile
import com.example.health.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.*

enum class HealthScenario(val title: String, val description: String) {
    BALANCED_HEALTHY(
        "Dengeli ve Sağlıklı Gün",
        "İdeal adım, mükemmel 7.8 saat uyku, 64 bpm dinlenik nabız, %98 SpO2 ve düşük stres."
    ),
    ATHLETE_RECOVERY(
        "Sporcu & Yüksek Aktivite",
        "14,800 adım, 8.2 saat uyku, 52 bpm düşük dinlenik nabız, 850 kcal aktif kalori."
    ),
    TIRED_LOW_SLEEP(
        "Yorgun & Uykusuz Gün",
        "3,100 adım, 4.8 saat yetersiz uyku (ortalama altı), 78 bpm nabız, 65 orta stres."
    ),
    HIGH_STRESS_WORK(
        "Yoğun & Yüksek Stres",
        "5,200 adım, 5.5 saat uyku, 88 bpm yükselmiş nabız, 84 yüksek stres yükü."
    ),
    CRITICAL_ALERT_TEST(
        "Kritik Sağlık Uyarısı (Tıbbi Test)",
        "Düşük SpO2 (%88 Hipoksemi) ve Düşük Nabız (38 bpm) - Doğrudan doktor başvuru alarmı tetikler."
    )
}

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

    suspend fun getTodayRecord(): DailyHealthRecord {
        val todayStr = getTodayDateString()
        val entity = healthDao.getRecordByDate(todayStr)
        return if (entity != null) {
            entity.toDomain()
        } else {
            val defaultRecord = generateScenarioRecord(HealthScenario.BALANCED_HEALTHY, todayStr)
            saveRecord(defaultRecord)
            defaultRecord
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

    suspend fun applyScenario(scenario: HealthScenario, profile: UserHealthProfile = UserHealthProfile()): DailyHealthRecord {
        val todayStr = getTodayDateString()
        val record = generateScenarioRecord(scenario, todayStr)
        saveRecord(record, profile)
        return record
    }

    suspend fun seedMockHistoryIfEmpty(profile: UserHealthProfile = UserHealthProfile()) {
        val today = Calendar.getInstance()
        val list = mutableListOf<HealthRecordEntity>()

        for (i in 6 downTo 1) {
            val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -i) }
            val dateStr = dateFormat.format(cal.time)
            val existing = healthDao.getRecordByDate(dateStr)
            if (existing == null) {
                val mockSteps = (6000..11000).random()
                val mockSleep = (6.0 + (0..25).random() / 10.0)
                val mockHr = (58..74).random()
                val mockSpO2 = (96..99).random()
                val mockStress = (15..55).random()
                val mockCal = (380..650).random()

                val rec = DailyHealthRecord(
                    date = dateStr,
                    timestamp = cal.timeInMillis,
                    steps = mockSteps,
                    restingHeartRate = mockHr,
                    minHeartRate = mockHr - 10,
                    maxHeartRate = mockHr + 45,
                    sleepHours = mockSleep,
                    deepSleepPercent = (15..28).random(),
                    spO2Percent = mockSpO2,
                    stressScore = mockStress,
                    activeCaloriesKcal = mockCal
                )
                val summary = RuleEngine.evaluateAll(rec, profile)
                list.add(
                    HealthRecordEntity(
                        date = rec.date,
                        timestamp = rec.timestamp,
                        steps = rec.steps,
                        restingHeartRate = rec.restingHeartRate,
                        minHeartRate = rec.minHeartRate,
                        maxHeartRate = rec.maxHeartRate,
                        sleepHours = rec.sleepHours,
                        deepSleepPercent = rec.deepSleepPercent,
                        spO2Percent = rec.spO2Percent,
                        stressScore = rec.stressScore,
                        activeCaloriesKcal = rec.activeCaloriesKcal,
                        overallScore = summary.overallScore,
                        overallStatus = summary.overallStatus.tag
                    )
                )
            }
        }
        if (list.isNotEmpty()) {
            healthDao.insertDailyRecords(list)
        }
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

    private fun generateScenarioRecord(scenario: HealthScenario, dateStr: String): DailyHealthRecord {
        return when (scenario) {
            HealthScenario.BALANCED_HEALTHY -> DailyHealthRecord(
                date = dateStr,
                steps = 8640,
                restingHeartRate = 64,
                minHeartRate = 56,
                maxHeartRate = 118,
                sleepHours = 7.8,
                deepSleepPercent = 24,
                spO2Percent = 98,
                stressScore = 22,
                activeCaloriesKcal = 540
            )
            HealthScenario.ATHLETE_RECOVERY -> DailyHealthRecord(
                date = dateStr,
                steps = 14850,
                restingHeartRate = 52,
                minHeartRate = 48,
                maxHeartRate = 165,
                sleepHours = 8.2,
                deepSleepPercent = 30,
                spO2Percent = 99,
                stressScore = 18,
                activeCaloriesKcal = 850
            )
            HealthScenario.TIRED_LOW_SLEEP -> DailyHealthRecord(
                date = dateStr,
                steps = 3200,
                restingHeartRate = 78,
                minHeartRate = 66,
                maxHeartRate = 125,
                sleepHours = 4.8,
                deepSleepPercent = 12,
                spO2Percent = 96,
                stressScore = 65,
                activeCaloriesKcal = 280
            )
            HealthScenario.HIGH_STRESS_WORK -> DailyHealthRecord(
                date = dateStr,
                steps = 5200,
                restingHeartRate = 88,
                minHeartRate = 72,
                maxHeartRate = 135,
                sleepHours = 5.5,
                deepSleepPercent = 14,
                spO2Percent = 95,
                stressScore = 84,
                activeCaloriesKcal = 390
            )
            HealthScenario.CRITICAL_ALERT_TEST -> DailyHealthRecord(
                date = dateStr,
                steps = 1200,
                restingHeartRate = 38, // Bradycardia critical alarm
                minHeartRate = 34,
                maxHeartRate = 95,
                sleepHours = 4.1,
                deepSleepPercent = 8,
                spO2Percent = 88, // Hypoxemia critical alarm
                stressScore = 92,
                activeCaloriesKcal = 160
            )
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
