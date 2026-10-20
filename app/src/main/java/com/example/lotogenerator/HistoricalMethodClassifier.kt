package com.example.lotogenerator

/**
 * Classifies a historical winning combination using only draws that occurred before it.
 * A draw can match more than one method because Balanced is structural while
 * Frequency/Hot/Cold are frequency-based.
 */
object HistoricalMethodClassifier {
    fun classify(
        numbers: List<Int>,
        previousDraws: List<List<Int>>,
        maxNumber: Int
    ): List<String> {
        if (numbers.isEmpty() || previousDraws.isEmpty()) return emptyList()

        val result = mutableListOf<String>()
        val count = numbers.size
        val odd = numbers.count { it % 2 != 0 }
        val lowBoundary = maxNumber / 2
        val low = numbers.count { it <= lowBoundary }
        val spread = (numbers.maxOrNull() ?: 0) - (numbers.minOrNull() ?: 0)

        val balancedOddRange = if (count == 5) 2..3 else 2..4
        val balancedLowRange = if (count == 5) 2..3 else 2..4
        if (odd in balancedOddRange && low in balancedLowRange && spread >= maxNumber / 2) {
            result += "Balanced"
        }

        val frequency = (1..maxNumber).associateWith { n ->
            previousDraws.count { n in it }
        }
        val ranked = frequency.entries.sortedByDescending { it.value }
        val third = (maxNumber / 3).coerceAtLeast(1)
        val hotSet = ranked.take(third).map { it.key }.toSet()
        val coldSet = ranked.takeLast(third).map { it.key }.toSet()

        val hotCount = numbers.count { it in hotSet }
        val coldCount = numbers.count { it in coldSet }
        val selectedAverage = numbers.map { frequency[it] ?: 0 }.average()
        val allAverage = frequency.values.average()

        if (selectedAverage >= allAverage) result += "Frequency"
        if (hotCount >= (count + 1) / 2) result += "Hot"
        if (coldCount >= (count + 1) / 2) result += "Cold"

        return result.distinct()
    }
}
