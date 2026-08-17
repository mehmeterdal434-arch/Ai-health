package com.example.health.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface HealthDao {
    @Query("SELECT * FROM daily_health_records ORDER BY date DESC")
    fun getAllDailyRecords(): Flow<List<HealthRecordEntity>>

    @Query("SELECT * FROM daily_health_records WHERE date = :date LIMIT 1")
    suspend fun getRecordByDate(date: String): HealthRecordEntity?

    @Query("SELECT * FROM daily_health_records ORDER BY date DESC LIMIT 7")
    fun getRecent7Days(): Flow<List<HealthRecordEntity>>

    @Query("SELECT * FROM daily_health_records ORDER BY date DESC LIMIT 30")
    fun getRecent30Days(): Flow<List<HealthRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateDailyRecord(record: HealthRecordEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailyRecords(records: List<HealthRecordEntity>)

    @Query("SELECT * FROM ai_insights ORDER BY timestamp DESC")
    fun getAllAiInsights(): Flow<List<AiInsightEntity>>

    @Query("SELECT * FROM ai_insights WHERE date = :date AND metricType IS NULL ORDER BY timestamp DESC LIMIT 1")
    fun getLatestDayInsight(date: String): Flow<AiInsightEntity?>

    @Query("SELECT * FROM ai_insights WHERE date = :date AND metricType = :metricType ORDER BY timestamp DESC LIMIT 1")
    fun getLatestMetricInsight(date: String, metricType: String): Flow<AiInsightEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAiInsight(insight: AiInsightEntity): Long

    @Query("DELETE FROM daily_health_records WHERE date = :date")
    suspend fun deleteRecordByDate(date: String)

    @Query("DELETE FROM daily_health_records")
    suspend fun clearAllRecords()

    @Query("DELETE FROM ai_insights")
    suspend fun clearAllAiInsights()
}
