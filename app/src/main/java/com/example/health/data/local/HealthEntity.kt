package com.example.health.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_health_records")
data class HealthRecordEntity(
    @PrimaryKey val date: String, // e.g. "2026-08-16"
    val timestamp: Long,
    val steps: Int,
    val restingHeartRate: Int,
    val minHeartRate: Int,
    val maxHeartRate: Int,
    val sleepHours: Double,
    val deepSleepPercent: Int,
    val spO2Percent: Int,
    val stressScore: Int,
    val activeCaloriesKcal: Int,
    val overallScore: Int,
    val overallStatus: String
)

@Entity(tableName = "ai_insights")
data class AiInsightEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val timestamp: Long,
    val metricType: String?, // null if whole day
    val explanationText: String,
    val practicalTip: String,
    val disclaimer: String,
    val criticalMedicalWarning: String?
)
