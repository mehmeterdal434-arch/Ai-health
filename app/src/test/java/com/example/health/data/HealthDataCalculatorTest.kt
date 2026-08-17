package com.example.health.data

import org.junit.Assert.*
import org.junit.Test
import java.time.Instant

class HealthDataCalculatorTest {

    @Test
    fun `test steps deduplication with overlapping intervals`() {
        val baseTime = Instant.parse("2026-08-17T08:00:00Z")

        // Wearable recorded 2000 steps from 08:00 to 09:00
        val record1 = RawStepRecord(
            startTime = baseTime,
            endTime = baseTime.plusSeconds(3600),
            count = 2000,
            packageName = HealthDataCalculator.SAMSUNG_HEALTH_PACKAGE
        )

        // Duplicate/Phone record covering exact same interval with 1800 steps
        val record2 = RawStepRecord(
            startTime = baseTime,
            endTime = baseTime.plusSeconds(3600),
            count = 1800,
            packageName = "com.google.android.apps.fitness"
        )

        // Next non-overlapping interval from 09:00 to 10:00 with 1500 steps
        val record3 = RawStepRecord(
            startTime = baseTime.plusSeconds(3600),
            endTime = baseTime.plusSeconds(7200),
            count = 1500,
            packageName = HealthDataCalculator.SAMSUNG_HEALTH_PACKAGE
        )

        val result = HealthDataCalculator.calculateSteps(
            aggregateCount = null,
            records = listOf(record1, record2, record3)
        )

        assertTrue(result.hasData)
        // Since Samsung Health records exist, it takes Samsung Health records: 2000 + 1500 = 3500
        assertEquals(3500, result.totalSteps)
    }

    @Test
    fun `test steps with aggregate count prioritization`() {
        val result = HealthDataCalculator.calculateSteps(
            aggregateCount = 8450L,
            records = emptyList()
        )

        assertTrue(result.hasData)
        assertEquals(8450, result.totalSteps)
    }

    @Test
    fun `test distance does not estimate from steps when missing`() {
        val result = HealthDataCalculator.calculateDistance(
            aggregateMeters = null,
            records = emptyList()
        )

        assertFalse(result.hasData)
        assertEquals(0.0, result.distanceMeters, 0.001)
    }

    @Test
    fun `test active calories does not estimate from steps when missing`() {
        val result = HealthDataCalculator.calculateActiveCalories(
            aggregateActiveKcal = null,
            records = emptyList()
        )

        assertFalse(result.hasData)
        assertEquals(0, result.activeCaloriesKcal)
    }

    @Test
    fun `test heart rate noise rejection and resting calculation`() {
        val restingRecords = listOf(62)
        // Include physiological noise like 20 bpm or 280 bpm
        val samples = listOf(20, 60, 65, 72, 85, 120, 280)

        val result = HealthDataCalculator.calculateHeartRate(restingRecords, samples)

        assertTrue(result.hasData)
        assertEquals(62, result.restingHeartRate)
        assertEquals(60, result.minHeartRate) // 20 was filtered out
        assertEquals(120, result.maxHeartRate) // 280 was filtered out
        assertEquals(5, result.sampleCount) // 5 valid samples
    }

    @Test
    fun `test sleep calculation across midnight with stage calculation`() {
        val start = Instant.parse("2026-08-16T23:30:00Z")
        val deepStart = Instant.parse("2026-08-17T01:00:00Z")
        val deepEnd = Instant.parse("2026-08-17T02:30:00Z") // 90 min deep sleep
        val end = Instant.parse("2026-08-17T07:30:00Z") // 8 hours total

        val stages = listOf(
            RawSleepStage(startTime = start, endTime = deepStart, stageType = 2), // Light
            RawSleepStage(startTime = deepStart, endTime = deepEnd, stageType = 3), // Deep
            RawSleepStage(startTime = deepEnd, endTime = end, stageType = 4) // REM
        )

        val session = RawSleepSession(
            startTime = start,
            endTime = end,
            stages = stages,
            packageName = HealthDataCalculator.SAMSUNG_HEALTH_PACKAGE
        )

        val result = HealthDataCalculator.calculateSleep(listOf(session))

        assertTrue(result.hasData)
        assertEquals(8.0, result.sleepHours, 0.1)
        assertEquals(480, result.totalSleepMinutes)
        // 90 min / 480 min = 18.75% -> 19%
        assertEquals(19, result.deepSleepPercent)
    }

    @Test
    fun `test sleep overlapping duplicate sessions merging`() {
        val start1 = Instant.parse("2026-08-16T23:00:00Z")
        val end1 = Instant.parse("2026-08-17T07:00:00Z") // 8 hours

        // Duplicate session overlapping 23:30 to 07:30
        val start2 = Instant.parse("2026-08-16T23:30:00Z")
        val end2 = Instant.parse("2026-08-17T07:30:00Z")

        val session1 = RawSleepSession(startTime = start1, endTime = end1)
        val session2 = RawSleepSession(startTime = start2, endTime = end2)

        val result = HealthDataCalculator.calculateSleep(listOf(session1, session2))

        assertTrue(result.hasData)
        // Merged interval: 23:00 to 07:30 = 8.5 hours (510 minutes)
        assertEquals(8.5, result.sleepHours, 0.1)
        assertEquals(510, result.totalSleepMinutes)
    }

    @Test
    fun `test exercise sessions deduplication`() {
        val start1 = Instant.parse("2026-08-17T10:00:00Z")
        val end1 = Instant.parse("2026-08-17T10:45:00Z") // 45 mins

        // Overlapping record from another tracker
        val start2 = Instant.parse("2026-08-17T10:15:00Z")
        val end2 = Instant.parse("2026-08-17T11:00:00Z") // extends to 11:00 (total 60 mins)

        val ex1 = RawExerciseSession(startTime = start1, endTime = end1)
        val ex2 = RawExerciseSession(startTime = start2, endTime = end2)

        val result = HealthDataCalculator.calculateExercise(listOf(ex1, ex2))

        assertTrue(result.hasData)
        assertEquals(60, result.totalMinutes)
    }
}
