package com.example.health.engine

import com.example.health.model.DailyHealthRecord
import com.example.health.model.HealthCategory
import com.example.health.model.HealthStatusLevel
import com.example.health.model.MetricType
import org.junit.Assert.*
import org.junit.Test

class RuleEngineTest {

    @Test
    fun `test evaluateSteps optimal case`() {
        val record = DailyHealthRecord(
            date = "2026-08-17",
            timestamp = System.currentTimeMillis(),
            steps = 11000,
            hasStepsData = true
        )
        val profile = UserHealthProfile(stepGoal = 10000)

        val eval = RuleEngine.evaluateSteps(record, profile)

        assertTrue(eval.hasMeasuredData)
        assertEquals(HealthCategory.ACHIEVED, eval.category)
        assertEquals(HealthStatusLevel.EXCELLENT, eval.statusLevel)
        assertEquals("11000 adım", eval.formattedValue)
    }

    @Test
    fun `test evaluateHeartRate critical bradycardia`() {
        val record = DailyHealthRecord(
            date = "2026-08-17",
            timestamp = System.currentTimeMillis(),
            restingHeartRate = 42,
            hasHeartRateData = true
        )
        val profile = UserHealthProfile(restingHeartRateBaselineBpm = 65)

        val eval = RuleEngine.evaluateHeartRate(record, profile)

        assertTrue(eval.isCritical)
        assertEquals(HealthCategory.CRITICAL_LOW, eval.category)
        assertEquals(HealthStatusLevel.CRITICAL, eval.statusLevel)
        assertNotNull(eval.criticalAlertMessage)
    }

    @Test
    fun `test evaluateAll readiness calculation with empty data`() {
        val record = DailyHealthRecord(
            date = "2026-08-17",
            timestamp = System.currentTimeMillis()
        )
        val profile = UserHealthProfile()

        val summary = RuleEngine.evaluateAll(record, profile)

        assertEquals("2026-08-17", summary.date)
        assertEquals(50, summary.overallScore)
        assertFalse(summary.hasCriticalConditions)
        assertEquals(6, summary.evaluations.size)
    }
}
