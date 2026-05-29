package io.github.gdict.core

import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import kotlin.math.abs

class FsrsAlgorithmTest {

    @Before
    fun setUp() {
        FsrsAlgorithm.resetParameters()
    }

    @After
    fun tearDown() {
        FsrsAlgorithm.resetParameters()
    }

    @Test
    fun testScheduleNewReturnsAllRatings() {
        val result = FsrsAlgorithm.scheduleNew(now = 1000L)
        assertEquals(4, result.size)
        assertTrue(result.containsKey(Rating.Again))
        assertTrue(result.containsKey(Rating.Hard))
        assertTrue(result.containsKey(Rating.Good))
        assertTrue(result.containsKey(Rating.Easy))
    }

    @Test
    fun testScheduleNewDifficultyRange() {
        val result = FsrsAlgorithm.scheduleNew(now = 1000L)
        for (card in result.values) {
            assertTrue(
                "Difficulty ${card.difficulty} should be >= 1.0",
                card.difficulty >= 1.0
            )
            assertTrue(
                "Difficulty ${card.difficulty} should be <= 10.0",
                card.difficulty <= 10.0
            )
        }
    }

    @Test
    fun testScheduleNewStabilityPositive() {
        val result = FsrsAlgorithm.scheduleNew(now = 1000L)
        for (card in result.values) {
            assertTrue(
                "Stability ${card.stability} should be >= 0.1",
                card.stability >= 0.1
            )
        }
    }

    @Test
    fun testScheduleNewScheduledDaysAtLeastOne() {
        val result = FsrsAlgorithm.scheduleNew(now = 1000L)
        for (card in result.values) {
            assertTrue(
                "scheduledDays ${card.scheduledDays} should be >= 1",
                card.scheduledDays >= 1
            )
            assertTrue(
                "scheduledDays ${card.scheduledDays} should be <= 36500",
                card.scheduledDays <= 36500
            )
        }
    }

    @Test
    fun testScheduleNewRetrievabilityIsOne() {
        val result = FsrsAlgorithm.scheduleNew(now = 1000L)
        for (card in result.values) {
            assertEquals(
                "New card retrievability should be 1.0",
                1.0, card.retrievability, 0.001
            )
        }
    }

    @Test
    fun testScheduleNewNextReviewInFuture() {
        val now = 1000L
        val result = FsrsAlgorithm.scheduleNew(now = now)
        for (card in result.values) {
            assertTrue(
                "nextReview should be > now",
                card.nextReview > now
            )
        }
    }

    @Test
    fun testScheduleNewEasierRatingGivesLowerDifficulty() {
        val result = FsrsAlgorithm.scheduleNew(now = 1000L)
        val againD = result[Rating.Again]!!.difficulty
        val hardD = result[Rating.Hard]!!.difficulty
        val goodD = result[Rating.Good]!!.difficulty
        val easyD = result[Rating.Easy]!!.difficulty
        assertTrue(
            "Again ($againD) should be harder than Hard ($hardD)",
            againD >= hardD
        )
        assertTrue(
            "Hard ($hardD) should be harder than Good ($goodD)",
            hardD >= goodD
        )
        assertTrue(
            "Good ($goodD) should be harder than Easy ($easyD)",
            goodD >= easyD
        )
    }

    @Test
    fun testScheduleNewEasierRatingGivesHigherStability() {
        val result = FsrsAlgorithm.scheduleNew(now = 1000L)
        val againS = result[Rating.Again]!!.stability
        val hardS = result[Rating.Hard]!!.stability
        val goodS = result[Rating.Good]!!.stability
        val easyS = result[Rating.Easy]!!.stability
        assertTrue(
            "Again stability ($againS) should be <= Hard ($hardS)",
            againS <= hardS
        )
        assertTrue(
            "Hard stability ($hardS) should be <= Good ($goodS)",
            hardS <= goodS
        )
        assertTrue(
            "Good stability ($goodS) should be <= Easy ($easyS)",
            goodS <= easyS
        )
    }

    @Test
    fun testScheduleNewEasierRatingGivesLongerInterval() {
        val result = FsrsAlgorithm.scheduleNew(now = 1000L)
        val againDays = result[Rating.Again]!!.scheduledDays
        val hardDays = result[Rating.Hard]!!.scheduledDays
        val goodDays = result[Rating.Good]!!.scheduledDays
        val easyDays = result[Rating.Easy]!!.scheduledDays
        assertTrue(
            "Again days ($againDays) should be <= Hard ($hardDays)",
            againDays <= hardDays
        )
        assertTrue(
            "Hard days ($hardDays) should be <= Good ($goodDays)",
            hardDays <= goodDays
        )
        assertTrue(
            "Good days ($goodDays) should be <= Easy ($easyDays)",
            goodDays <= easyDays
        )
    }

    @Test
    fun testScheduleReturnsAllRatings() {
        val now = System.currentTimeMillis()
        val result = FsrsAlgorithm.schedule(
            currentDifficulty = 5.0,
            currentStability = 10.0,
            lastReview = now - 86400000L,
            now = now
        )
        assertEquals(4, result.size)
        assertTrue(result.containsKey(Rating.Again))
        assertTrue(result.containsKey(Rating.Hard))
        assertTrue(result.containsKey(Rating.Good))
        assertTrue(result.containsKey(Rating.Easy))
    }

    @Test
    fun testScheduleDifficultyRange() {
        val now = System.currentTimeMillis()
        val result = FsrsAlgorithm.schedule(
            currentDifficulty = 5.0,
            currentStability = 10.0,
            lastReview = now - 86400000L,
            now = now
        )
        for (card in result.values) {
            assertTrue(
                "Difficulty ${card.difficulty} should be >= 1.0",
                card.difficulty >= 1.0
            )
            assertTrue(
                "Difficulty ${card.difficulty} should be <= 10.0",
                card.difficulty <= 10.0
            )
        }
    }

    @Test
    fun testScheduleStabilityPositive() {
        val now = System.currentTimeMillis()
        val result = FsrsAlgorithm.schedule(
            currentDifficulty = 5.0,
            currentStability = 10.0,
            lastReview = now - 86400000L,
            now = now
        )
        for (card in result.values) {
            assertTrue(
                "Stability ${card.stability} should be >= 0.1",
                card.stability >= 0.1
            )
        }
    }

    @Test
    fun testScheduleAgainReducesStability() {
        val now = System.currentTimeMillis()
        val currentStability = 10.0
        val result = FsrsAlgorithm.schedule(
            currentDifficulty = 5.0,
            currentStability = currentStability,
            lastReview = now - 86400000L,
            now = now
        )
        val againStability = result[Rating.Again]!!.stability
        assertTrue(
            "Again stability ($againStability) should be less than current ($currentStability)",
            againStability < currentStability
        )
    }

    @Test
    fun testScheduleGoodOrEasyIncreasesStability() {
        val now = System.currentTimeMillis()
        val currentStability = 10.0
        val elapsedMs = 86400000L
        val result = FsrsAlgorithm.schedule(
            currentDifficulty = 5.0,
            currentStability = currentStability,
            lastReview = now - elapsedMs,
            now = now
        )
        val easyStability = result[Rating.Easy]!!.stability
        assertTrue(
            "Easy stability ($easyStability) should be greater than current ($currentStability)",
            easyStability > currentStability
        )
    }

    @Test
    fun testScheduleAgainIncreasesDifficulty() {
        val now = System.currentTimeMillis()
        val currentDifficulty = 5.0
        val result = FsrsAlgorithm.schedule(
            currentDifficulty = currentDifficulty,
            currentStability = 10.0,
            lastReview = now - 86400000L,
            now = now
        )
        val againDifficulty = result[Rating.Again]!!.difficulty
        assertTrue(
            "Again difficulty ($againDifficulty) should be >= current ($currentDifficulty)",
            againDifficulty >= currentDifficulty
        )
    }

    @Test
    fun testScheduleEasyDecreasesDifficulty() {
        val now = System.currentTimeMillis()
        val currentDifficulty = 5.0
        val result = FsrsAlgorithm.schedule(
            currentDifficulty = currentDifficulty,
            currentStability = 10.0,
            lastReview = now - 86400000L,
            now = now
        )
        val easyDifficulty = result[Rating.Easy]!!.difficulty
        assertTrue(
            "Easy difficulty ($easyDifficulty) should be <= current ($currentDifficulty)",
            easyDifficulty <= currentDifficulty
        )
    }

    @Test
    fun testScheduleIntervalOrdering() {
        val now = System.currentTimeMillis()
        val result = FsrsAlgorithm.schedule(
            currentDifficulty = 5.0,
            currentStability = 10.0,
            lastReview = now - 86400000L,
            now = now
        )
        val againDays = result[Rating.Again]!!.scheduledDays
        val hardDays = result[Rating.Hard]!!.scheduledDays
        val goodDays = result[Rating.Good]!!.scheduledDays
        val easyDays = result[Rating.Easy]!!.scheduledDays
        assertTrue("Again ($againDays) <= Hard ($hardDays)", againDays <= hardDays)
        assertTrue("Hard ($hardDays) <= Good ($goodDays)", hardDays <= goodDays)
        assertTrue("Good ($goodDays) <= Easy ($easyDays)", goodDays <= easyDays)
    }

    @Test
    fun testRetrievabilityAtDayZero() {
        val r = FsrsAlgorithm.getRetrievability(0, 10.0)
        assertEquals("Retrievability at day 0 should be 1.0", 1.0, r, 0.001)
    }

    @Test
    fun testRetrievabilityDecreasesOverTime() {
        val r1 = FsrsAlgorithm.getRetrievability(1, 10.0)
        val r5 = FsrsAlgorithm.getRetrievability(5, 10.0)
        val r10 = FsrsAlgorithm.getRetrievability(10, 10.0)
        assertTrue("r1 ($r1) should be > r5 ($r5)", r1 > r5)
        assertTrue("r5 ($r5) should be > r10 ($r10)", r5 > r10)
    }

    @Test
    fun testRetrievabilityRange() {
        for (days in listOf(0, 1, 5, 10, 30, 100, 365)) {
            val r = FsrsAlgorithm.getRetrievability(days, 10.0)
            assertTrue(
                "Retrievability $r for $days days should be in (0, 1]",
                r > 0.0 && r <= 1.0
            )
        }
    }

    @Test
    fun testRetrievabilityHigherStabilitySlowerDecay() {
        val r1s5 = FsrsAlgorithm.getRetrievability(10, 5.0)
        val r1s20 = FsrsAlgorithm.getRetrievability(10, 20.0)
        assertTrue(
            "Higher stability should give higher retrievability: s20=$r1s20 vs s5=$r1s5",
            r1s20 > r1s5
        )
    }

    @Test
    fun testSetParametersRejectsWrongSize() {
        try {
            FsrsAlgorithm.setParameters(DoubleArray(10))
            fail("Should throw IllegalArgumentException for wrong parameter count")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("19"))
        }
    }

    @Test
    fun testSetParametersAcceptsCorrectSize() {
        val customParams = DoubleArray(19) { 0.5 }
        FsrsAlgorithm.setParameters(customParams)
        val result = FsrsAlgorithm.scheduleNew(now = 1000L)
        assertNotNull(result)
        assertTrue(result.size == 4)
    }

    @Test
    fun testResetParametersRestoresDefaults() {
        val before = FsrsAlgorithm.scheduleNew(now = 1000L)
        FsrsAlgorithm.setParameters(DoubleArray(19) { 0.5 })
        val afterCustom = FsrsAlgorithm.scheduleNew(now = 1000L)
        FsrsAlgorithm.resetParameters()
        val afterReset = FsrsAlgorithm.scheduleNew(now = 1000L)

        assertEquals(
            before[Rating.Good]!!.stability,
            afterReset[Rating.Good]!!.stability,
            0.0001
        )
        assertNotEquals(
            before[Rating.Good]!!.stability,
            afterCustom[Rating.Good]!!.stability,
            0.0001
        )
    }

    @Test
    fun testScheduleWithZeroElapsed() {
        val now = 1000L
        val result = FsrsAlgorithm.schedule(
            currentDifficulty = 5.0,
            currentStability = 10.0,
            lastReview = now,
            now = now
        )
        for (card in result.values) {
            assertTrue("scheduledDays should be >= 1", card.scheduledDays >= 1)
        }
    }

    @Test
    fun testScheduleWithLargeElapsed() {
        val now = System.currentTimeMillis()
        val oneYearAgo = now - 365L * 86400000L
        val result = FsrsAlgorithm.schedule(
            currentDifficulty = 5.0,
            currentStability = 10.0,
            lastReview = oneYearAgo,
            now = now
        )
        val againStability = result[Rating.Again]!!.stability
        assertTrue("Again stability should still be >= 0.1", againStability >= 0.1)
    }

    @Test
    fun testScheduleDeterministic() {
        val now = 1000000L
        val result1 = FsrsAlgorithm.schedule(
            currentDifficulty = 5.0,
            currentStability = 10.0,
            lastReview = now - 86400000L,
            now = now
        )
        val result2 = FsrsAlgorithm.schedule(
            currentDifficulty = 5.0,
            currentStability = 10.0,
            lastReview = now - 86400000L,
            now = now
        )
        for (rating in Rating.values()) {
            assertEquals(
                result1[rating]!!.difficulty,
                result2[rating]!!.difficulty,
                0.0001
            )
            assertEquals(
                result1[rating]!!.stability,
                result2[rating]!!.stability,
                0.0001
            )
            assertEquals(
                result1[rating]!!.scheduledDays,
                result2[rating]!!.scheduledDays
            )
        }
    }

    @Test
    fun testConsecutiveReviewsIncreaseInterval() {
        val now = 1000L
        val newResult = FsrsAlgorithm.scheduleNew(now = now)
        val goodCard = newResult[Rating.Good]!!

        val secondResult = FsrsAlgorithm.schedule(
            currentDifficulty = goodCard.difficulty,
            currentStability = goodCard.stability,
            lastReview = now,
            now = goodCard.nextReview
        )
        val secondGoodDays = secondResult[Rating.Good]!!.scheduledDays
        assertTrue(
            "Second review Good interval ($secondGoodDays) should be >= first (${goodCard.scheduledDays})",
            secondGoodDays >= goodCard.scheduledDays
        )
    }

    @Test
    fun testHighDifficultyShortensInterval() {
        val now = System.currentTimeMillis()
        val lowDiffResult = FsrsAlgorithm.schedule(
            currentDifficulty = 2.0,
            currentStability = 10.0,
            lastReview = now - 86400000L,
            now = now
        )
        val highDiffResult = FsrsAlgorithm.schedule(
            currentDifficulty = 8.0,
            currentStability = 10.0,
            lastReview = now - 86400000L,
            now = now
        )
        val lowGoodDays = lowDiffResult[Rating.Good]!!.scheduledDays
        val highGoodDays = highDiffResult[Rating.Good]!!.scheduledDays
        assertTrue(
            "Low diff Good interval ($lowGoodDays) should be >= high diff ($highGoodDays)",
            lowGoodDays >= highGoodDays
        )
    }

    @Test
    fun testDayMsConstant() {
        assertEquals(
            "DAY_MS should be 86400000",
            86400000L,
            FsrsAlgorithm.DAY_MS
        )
    }

    @Test
    fun testScheduleNewNextReviewCalculation() {
        val now = 1000L
        val result = FsrsAlgorithm.scheduleNew(now = now)
        for (card in result.values) {
            val expected = now + card.scheduledDays * FsrsAlgorithm.DAY_MS
            assertEquals(
                "nextReview should be now + scheduledDays * DAY_MS",
                expected, card.nextReview
            )
        }
    }

    @Test
    fun testVeryLowStabilityClamp() {
        val now = 1000L
        val result = FsrsAlgorithm.schedule(
            currentDifficulty = 10.0,
            currentStability = 0.1,
            lastReview = now - 86400000L * 30,
            now = now
        )
        for (card in result.values) {
            assertTrue(
                "Stability should be >= 0.1 even for extreme inputs, got ${card.stability}",
                card.stability >= 0.1
            )
        }
    }

    @Test
    fun testRatingEnumOrdering() {
        assertEquals(0, Rating.Again.ordinal)
        assertEquals(1, Rating.Hard.ordinal)
        assertEquals(2, Rating.Good.ordinal)
        assertEquals(3, Rating.Easy.ordinal)
    }
}
