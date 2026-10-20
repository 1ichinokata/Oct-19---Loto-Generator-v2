package com.example.lotogenerator

import kotlin.math.roundToInt

data class PatternPrediction(
    val odd: Int,
    val even: Int,
    val oddEvenScore: Int,
    val low: Int,
    val high: Int,
    val lowHighScore: Int,
    val alternativeOdd: Int,
    val alternativeEven: Int,
    val alternativeLow: Int,
    val alternativeHigh: Int
)

object NextDrawPatternPredictor {
    fun predict(draws: List<List<Int>>, numberCount: Int, lowBoundary: Int): PatternPrediction? {
        val recent = draws.filter { it.size >= numberCount }.take(30)
        if (recent.size < 3) return null

        fun weightedBest(selector: (List<Int>) -> Int): Triple<Int, Int, Int> {
            val scores = DoubleArray(numberCount + 1)
            recent.forEachIndexed { index, draw ->
                val value = selector(draw).coerceIn(0, numberCount)
                val weight = 1.0 + (recent.size - index).toDouble() / recent.size
                scores[value] += weight
            }
            val ranked = scores.indices.sortedByDescending { scores[it] }
            val best = ranked[0]
            val alt = ranked.getOrElse(1) { best }
            val total = scores.sum().coerceAtLeast(1.0)
            val confidence = ((scores[best] / total) * 100.0).roundToInt().coerceIn(0, 99)
            return Triple(best, alt, confidence)
        }

        val (odd, altOdd, oddScore) = weightedBest { draw -> draw.take(numberCount).count { it % 2 != 0 } }
        val (low, altLow, lowScore) = weightedBest { draw -> draw.take(numberCount).count { it <= lowBoundary } }

        return PatternPrediction(
            odd = odd, even = numberCount - odd, oddEvenScore = oddScore,
            low = low, high = numberCount - low, lowHighScore = lowScore,
            alternativeOdd = altOdd, alternativeEven = numberCount - altOdd,
            alternativeLow = altLow, alternativeHigh = numberCount - altLow
        )
    }
}
