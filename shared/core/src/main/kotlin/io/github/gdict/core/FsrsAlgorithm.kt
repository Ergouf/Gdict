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
    private const val REQUEST_RETENTION = 0.9
    private const val MAX_INTERVAL = 36500
    private const val MIN_STABILITY = 0.1

    // FSRS-5 default parameters (19), py-fsrs layout:
    //  [0..3]   initial stability for Again/Hard/Good/Easy
    //  [4..5]   initial difficulty base and exponent
    //  [6..7]   next difficulty change rate and mean-reversion weight
    //  [8..10]  recall stability: increase factor, S-exponent, R-exponent
    //  [11..14] forget stability: coefficient, D-exponent, S-exponent, R-exponent
    //  [15]     hard penalty
    //  [16]     easy bonus
    //  [17..18] short-term stability rate and offset
    private val DEFAULT_PARAMS = doubleArrayOf(
        0.4072, 1.1829, 3.1262, 15.4745,
        7.2102, 0.5316,
        1.0651, 0.0589,
        1.5331, 0.1544, 1.0347,
        1.9395, 0.11, 0.29605, 2.2698,
        0.2315,
        2.9898,
        0.5166, 0.6621
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
        val r = (rating.ordinal + 1).toDouble()
        return constrainDifficulty(parameters[4] - exp(parameters[5] * (r - 1)) + 1)
    }

    private fun initialStability(rating: Rating): Double {
        return max(parameters[rating.ordinal], MIN_STABILITY)
    }

    private fun nextDifficulty(d: Double, rating: Rating): Double {
        val r = (rating.ordinal + 1).toDouble()
        val delta = -parameters[6] * (r - 3)
        val dPrime = d + delta * (10.0 - d) / 9.0
        val w = parameters[7]
        return constrainDifficulty(w * initialDifficulty(Rating.Easy) + (1 - w) * dPrime)
    }

    private fun nextStability(d: Double, s: Double, r: Double, rating: Rating): Double {
        return if (rating == Rating.Again) {
            nextForgetStability(d, s, r)
        } else {
            val hardPenalty = if (rating == Rating.Hard) parameters[15] else 1.0
            val easyBonus = if (rating == Rating.Easy) parameters[16] else 1.0
            nextRecallStability(d, s, r, hardPenalty, easyBonus)
        }
    }

    private fun nextForgetStability(d: Double, s: Double, r: Double): Double {
        val forget = parameters[11] *
            d.pow(-parameters[12]) *
            ((s + 1).pow(parameters[13]) - 1) *
            exp(parameters[14] * (1 - r))
        val upperBound = s / exp(parameters[17] * parameters[18])
        return max(min(forget, upperBound), MIN_STABILITY)
    }

    private fun nextRecallStability(
        d: Double,
        s: Double,
        r: Double,
        hardPenalty: Double,
        easyBonus: Double
    ): Double {
        val stability = s * (1 +
            exp(parameters[8]) * (11 - d) *
            s.pow(-parameters[9]) *
            (exp(parameters[10] * (1 - r)) - 1) *
            hardPenalty * easyBonus)
        return max(stability, MIN_STABILITY)
    }

    private fun retrievability(elapsedDays: Int, stability: Double): Double {
        return (1 + FACTOR * elapsedDays / stability).pow(DECAY)
    }

    private fun constrainDifficulty(d: Double): Double {
        return min(max(d, 1.0), 10.0)
    }

    private fun nextInterval(stability: Double): Int {
        val interval = stability / FACTOR * (REQUEST_RETENTION.pow(1.0 / DECAY) - 1)
        return max(min(interval.roundToInt(), MAX_INTERVAL), 1)
    }

    fun schedule(
        currentDifficulty: Double,
        currentStability: Double,
        lastReview: Long,
        now: Long = System.currentTimeMillis()
    ): Map<Rating, SchedulingCard> {
        val elapsedDays = max(((now - lastReview) / (1000.0 * 60 * 60 * 24)).roundToInt(), 0)
        val r = retrievability(elapsedDays, currentStability)

        return Rating.values().associateWith { rating ->
            val difficulty = nextDifficulty(currentDifficulty, rating)
            val stability = nextStability(currentDifficulty, currentStability, r, rating)
            val days = nextInterval(stability)
            SchedulingCard(
                rating = rating,
                difficulty = difficulty,
                stability = stability,
                retrievability = r,
                scheduledDays = days,
                nextReview = now + days * DAY_MS
            )
        }
    }

    fun scheduleNew(now: Long = System.currentTimeMillis()): Map<Rating, SchedulingCard> {
        return Rating.values().associateWith { rating ->
            val difficulty = initialDifficulty(rating)
            val stability = initialStability(rating)
            val days = nextInterval(stability)
            SchedulingCard(
                rating = rating,
                difficulty = difficulty,
                stability = stability,
                retrievability = 1.0,
                scheduledDays = days,
                nextReview = now + days * DAY_MS
            )
        }
    }

    fun getRetrievability(elapsedDays: Int, stability: Double): Double {
        return retrievability(elapsedDays, stability)
    }

    const val DAY_MS = 1000L * 60 * 60 * 24
}
