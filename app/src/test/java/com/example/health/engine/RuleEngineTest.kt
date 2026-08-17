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
        val eval = RuleEngine.evaluateSteps(11000, 10000)

        assertTrue(eval.hasMeasuredData)
        assertEquals(HealthCategory.ACHIEVED, eval.category)
        assertEquals(HealthStatusLevel.OPTIMAL, eval.statusLevel)
        assertEquals("11000 adım", eval.formattedValue)
    }

    @Test
    fun `test evaluateHeartRate critical bradycardia`() {
        val eval = RuleEngine.evaluateHeartRate(42)

        assertTrue(eval.isCritical)
        assertEquals(HealthCategory.CRITICAL_LOW, eval.category)
        assertEquals(HealthStatusLevel.CRITICAL, eval.statusLevel)
        assertNotNull(eval.criticalAlertMessage)
    }

    @Test
    fun `test evaluateAll readiness calculation with unmeasured data`() {
        val record = DailyHealthRecord(
            date = "2026-08-17",
            timestamp = System.currentTimeMillis()
        )
        val profile = UserHealthProfile()

        val summary = RuleEngine.evaluateAll(record, profile)

        assertEquals("2026-08-17", summary.date)
        assertEquals(0, summary.overallScore)
        assertFalse(summary.hasCriticalConditions)
        assertEquals(6, summary.evaluations.size)
        // All evaluations should report unmeasured
        summary.evaluations.values.forEach { eval ->
            assertFalse(eval.hasMeasuredData)
            assertEquals("Ölçüm Yok", eval.formattedValue)
        }
    }
}
