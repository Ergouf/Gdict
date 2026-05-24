package io.github.gdict.core

import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

enum class Rating {
    Again,
    Hard,
    Good,
    Easy
}

data class SchedulingCard(
    val rating: Rating,
    val difficulty: Double,
    val stability: Double,
    val retrievability: Double,
    val scheduledDays: Int,
    val nextReview: Long
)

data class ReviewLog(
    val rating: Rating,
    val scheduledDays: Int,
    val elapsedDays: Int,
    val reviewTime: Long
)

object FsrsAlgorithm {
    private const val DECAY = -0.5
    private const val FACTOR = 19.0 / 81.0

    private val DEFAULT_PARAMS = doubleArrayOf(
        0.4072, 0.0066, 1.0018, 4.8959, 0.0566, 0.5032, 0.2763, 0.0239,
        1.5418, 0.1596, 1.0060, 1.7868, 0.0216, 0.7179, 0.2541, 0.1121,
        0.4297, 0.4777, 0.5381
    )

    private var parameters = DEFAULT_PARAMS.copyOf()

    fun setParameters(params: DoubleArray) {
        require(params.size == 19) { "FSRS requires exactly 19 parameters" }
        parameters = params.copyOf()
    }

    fun resetParameters() {
        parameters = DEFAULT_PARAMS.copyOf()
    }

    private fun initialDifficulty(rating: Rating): Double {
        val d0 = parameters[0]
        val d1 = parameters[1]
        val d2 = parameters[2]
        val d3 = parameters[3]
        val r = (rating.ordinal + 1).toDouble()
        return constrainDifficulty(
            d0 - d1 * ln(r) + d2 * r.pow(d3)
        )
    }

    private fun initialStability(rating: Rating): Double {
        val s0 = parameters[4]
        val s1 = parameters[5]
        val s2 = parameters[6]
        val s3 = parameters[7]
        val r = (rating.ordinal + 1).toDouble()
        return max(s0 + s1 * ln(r) + s2 * r.pow(s3), 0.1)
    }

    private fun nextDifficulty(d: Double, rating: Rating): Double {
        val d4 = parameters[8]
        val d5 = parameters[9]
        val d6 = parameters[10]
        val delta = d4 * (d5 * rating.ordinal - d6)
        return constrainDifficulty(meanReversion(d, d + delta))
    }

    private fun nextStability(d: Double, s: Double, r: Double, rating: Rating): Double {
        val s11 = parameters[11]
        val s12 = parameters[12]
        val s13 = parameters[13]
        val s14 = parameters[14]
        val s15 = parameters[15]
        val s16 = parameters[16]
        val s17 = parameters[17]
        val s18 = parameters[18]

        if (rating == Rating.Again) {
            return max(s * s11 * (d.pow(-s12) * ((s + 1).pow(s13) - 1) * exp(s14 * (1 - r))), 0.1)
        }

        val hardPenalty = if (rating == Rating.Hard) s15 else 1.0
        val easyBonus = if (rating == Rating.Easy) s16 else 1.0
        return max(
            s * (1 + exp(s17) * (11 - d) * s.pow(-s18) * (exp(s14 * (1 - r)) - 1) * hardPenalty * easyBonus),
            0.1
        )
    }

    private fun retrievability(elapsedDays: Int, stability: Double): Double {
        return (1 + FACTOR * elapsedDays / stability).pow(DECAY)
    }

    private fun constrainDifficulty(d: Double): Double {
        return min(max(d, 1.0), 10.0)
    }

    private fun meanReversion(init: Double, current: Double): Double {
        val w = 0.4
        return w * init + (1 - w) * current
    }

    private fun ln(x: Double): Double = kotlin.math.ln(x)

    fun schedule(
        currentDifficulty: Double,
        currentStability: Double,
        lastReview: Long,
        now: Long = System.currentTimeMillis()
    ): Map<Rating, SchedulingCard> {
        val elapsedDays = max(((now - lastReview) / (1000.0 * 60 * 60 * 24)).roundToInt(), 0)
        val r = retrievability(elapsedDays, currentStability)

        val hardStability = nextStability(currentDifficulty, currentStability, r, Rating.Hard)
        val goodStability = nextStability(currentDifficulty, currentStability, r, Rating.Good)
        val easyStability = nextStability(currentDifficulty, currentStability, r, Rating.Easy)

        val hardDifficulty = nextDifficulty(currentDifficulty, Rating.Hard)
        val goodDifficulty = nextDifficulty(currentDifficulty, Rating.Good)
        val easyDifficulty = nextDifficulty(currentDifficulty, Rating.Easy)

        val hardDays = nextInterval(hardStability)
        val goodDays = nextInterval(goodStability)
        val easyDays = nextInterval(easyStability)

        val againStability = nextStability(currentDifficulty, currentStability, r, Rating.Again)
        val againDifficulty = nextDifficulty(currentDifficulty, Rating.Again)
        val againDays = nextInterval(againStability)

        return mapOf(
            Rating.Again to SchedulingCard(
                rating = Rating.Again,
                difficulty = againDifficulty,
                stability = againStability,
                retrievability = r,
                scheduledDays = againDays,
                nextReview = now + againDays * DAY_MS
            ),
            Rating.Hard to SchedulingCard(
                rating = Rating.Hard,
                difficulty = hardDifficulty,
                stability = hardStability,
                retrievability = r,
                scheduledDays = hardDays,
                nextReview = now + hardDays * DAY_MS
            ),
            Rating.Good to SchedulingCard(
                rating = Rating.Good,
                difficulty = goodDifficulty,
                stability = goodStability,
                retrievability = r,
                scheduledDays = goodDays,
                nextReview = now + goodDays * DAY_MS
            ),
            Rating.Easy to SchedulingCard(
                rating = Rating.Easy,
                difficulty = easyDifficulty,
                stability = easyStability,
                retrievability = r,
                scheduledDays = easyDays,
                nextReview = now + easyDays * DAY_MS
            )
        )
    }

    fun scheduleNew(now: Long = System.currentTimeMillis()): Map<Rating, SchedulingCard> {
        val d0 = initialDifficulty(Rating.Again)
        val s0 = initialStability(Rating.Again)
        val days0 = nextInterval(s0)

        val d1 = initialDifficulty(Rating.Hard)
        val s1 = initialStability(Rating.Hard)
        val days1 = nextInterval(s1)

        val d2 = initialDifficulty(Rating.Good)
        val s2 = initialStability(Rating.Good)
        val days2 = nextInterval(s2)

        val d3 = initialDifficulty(Rating.Easy)
        val s3 = initialStability(Rating.Easy)
        val days3 = nextInterval(s3)

        return mapOf(
            Rating.Again to SchedulingCard(
                rating = Rating.Again,
                difficulty = d0,
                stability = s0,
                retrievability = 1.0,
                scheduledDays = days0,
                nextReview = now + days0 * DAY_MS
            ),
            Rating.Hard to SchedulingCard(
                rating = Rating.Hard,
                difficulty = d1,
                stability = s1,
                retrievability = 1.0,
                scheduledDays = days1,
                nextReview = now + days1 * DAY_MS
            ),
            Rating.Good to SchedulingCard(
                rating = Rating.Good,
                difficulty = d2,
                stability = s2,
                retrievability = 1.0,
                scheduledDays = days2,
                nextReview = now + days2 * DAY_MS
            ),
            Rating.Easy to SchedulingCard(
                rating = Rating.Easy,
                difficulty = d3,
                stability = s3,
                retrievability = 1.0,
                scheduledDays = days3,
                nextReview = now + days3 * DAY_MS
            )
        )
    }

    private fun nextInterval(stability: Double): Int {
        val interval = stability / FACTOR * (0.9.pow(1.0 / DECAY) - 1)
        return max(min(interval.roundToInt(), MAX_INTERVAL), 1)
    }

    fun getRetrievability(elapsedDays: Int, stability: Double): Double {
        return retrievability(elapsedDays, stability)
    }

    const val DAY_MS = 1000L * 60 * 60 * 24
    private const val MAX_INTERVAL = 36500
}
