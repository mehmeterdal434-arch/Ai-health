package com.example

import com.example.health.engine.RuleEngine
import com.example.health.engine.UserHealthProfile
import com.example.health.model.DailyHealthRecord
import com.example.health.model.HealthCategory
import com.example.health.model.HealthStatusLevel
import com.example.health.model.MetricType
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun `RuleEngine correctly categorizes resting heart rate`() {
        val normalEval = RuleEngine.evaluateHeartRate(72)
        assertEquals(HealthCategory.NORMAL, normalEval.category)
        assertEquals(HealthStatusLevel.GOOD, normalEval.statusLevel)
        assertFalse(normalEval.isCritical)

        val bradycardiaEval = RuleEngine.evaluateHeartRate(38)
        assertEquals(HealthCategory.CRITICAL_LOW, bradycardiaEval.category)
        assertTrue(bradycardiaEval.isCritical)
        assertNotNull(bradycardiaEval.criticalAlertMessage)

        val athleteEval = RuleEngine.evaluateHeartRate(54)
        assertEquals(HealthCategory.LOW, athleteEval.category)
        assertFalse(athleteEval.isCritical)
    }

    @Test
    fun `RuleEngine correctly categorizes sleep duration`() {
        val optimalEval = RuleEngine.evaluateSleep(7.8, 7.5)
        assertEquals(HealthCategory.OPTIMAL, optimalEval.category)
        assertEquals(HealthStatusLevel.OPTIMAL, optimalEval.statusLevel)

        val veryLowEval = RuleEngine.evaluateSleep(4.0, 7.5)
        assertEquals(HealthCategory.VERY_LOW, veryLowEval.category)
        assertEquals(HealthStatusLevel.CRITICAL, veryLowEval.statusLevel)

        val belowAvgEval = RuleEngine.evaluateSleep(5.6, 7.5)
        assertEquals(HealthCategory.BELOW_AVERAGE, belowAvgEval.category)
        assertEquals(HealthStatusLevel.ATTENTION, belowAvgEval.statusLevel)
    }

    @Test
    fun `RuleEngine correctly flags critical low SpO2`() {
        val normalSpO2 = RuleEngine.evaluateSpO2(98)
        assertEquals(HealthCategory.NORMAL, normalSpO2.category)
        assertFalse(normalSpO2.isCritical)

        val criticalSpO2 = RuleEngine.evaluateSpO2(88)
        assertEquals(HealthCategory.CRITICAL, criticalSpO2.category)
        assertTrue(criticalSpO2.isCritical)
        assertNotNull(criticalSpO2.criticalAlertMessage)
    }

    @Test
    fun `RuleEngine correctly evaluates stress levels`() {
        val restStress = RuleEngine.evaluateStress(15)
        assertEquals(HealthCategory.REST, restStress.category)
        assertEquals(HealthStatusLevel.OPTIMAL, restStress.statusLevel)

        val lowStress = RuleEngine.evaluateStress(40)
        assertEquals(HealthCategory.LOW, lowStress.category)
        assertEquals(HealthStatusLevel.GOOD, lowStress.statusLevel)

        val mediumStress = RuleEngine.evaluateStress(65)
        assertEquals(HealthCategory.MEDIUM, mediumStress.category)
        assertEquals(HealthStatusLevel.ATTENTION, mediumStress.statusLevel)

        val highStress = RuleEngine.evaluateStress(85)
        assertEquals(HealthCategory.HIGH, highStress.category)
        assertEquals(HealthStatusLevel.CRITICAL, highStress.statusLevel)
    }

    @Test
    fun `RuleEngine correctly evaluates steps and calories`() {
        val sedentarySteps = RuleEngine.evaluateSteps(2500, 8000)
        assertEquals(HealthCategory.LOW, sedentarySteps.category)
        assertEquals(HealthStatusLevel.ATTENTION, sedentarySteps.statusLevel)

        val achievedSteps = RuleEngine.evaluateSteps(9000, 8000)
        assertEquals(HealthCategory.ACHIEVED, achievedSteps.category)
        assertEquals(HealthStatusLevel.OPTIMAL, achievedSteps.statusLevel)

        val achievedCal = RuleEngine.evaluateCalories(550, 500)
        assertEquals(HealthCategory.ACHIEVED, achievedCal.category)
        assertEquals(HealthStatusLevel.OPTIMAL, achievedCal.statusLevel)
    }

    @Test
    fun `RuleEngine correctly detects critical tachycardia`() {
        val tachyEval = RuleEngine.evaluateHeartRate(150)
        assertEquals(HealthCategory.CRITICAL_HIGH, tachyEval.category)
        assertTrue(tachyEval.isCritical)
        assertNotNull(tachyEval.criticalAlertMessage)
    }

    @Test
    fun `RuleEngine evaluates full daily summary accurately`() {
        val record = DailyHealthRecord(
            date = "2026-08-16",
            steps = 9500,
            hasStepsData = true,
            restingHeartRate = 66,
            hasHeartRateData = true,
            minHeartRate = 58,
            maxHeartRate = 120,
            sleepHours = 8.0,
            hasSleepData = true,
            deepSleepPercent = 25,
            spO2Percent = 99,
            hasSpO2Data = true,
            stressScore = 20,
            hasStressData = true,
            activeCaloriesKcal = 550,
            hasCaloriesData = true
        )
        val summary = RuleEngine.evaluateAll(record, UserHealthProfile())
        assertEquals(HealthStatusLevel.OPTIMAL, summary.overallStatus)
        assertFalse(summary.hasCriticalConditions)
        assertTrue(summary.overallScore >= 80)

        // Verify JSON sanitization output
        val json = summary.toSanitizedLlmJson()
        assertTrue(json.contains("\"date\": \"2026-08-16\""))
        assertTrue(json.contains("\"overallScore\":"))
        assertTrue(json.contains("\"metrics\":"))
    }
}
