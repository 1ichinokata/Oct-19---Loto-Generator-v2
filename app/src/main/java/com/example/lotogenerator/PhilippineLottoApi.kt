package com.example.lotogenerator

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

data class PhilippineLottoDraw(
    val drawNumber: Int,
    val date: LocalDate,
    val numbers: List<Int>,
    val jackpot: Double? = null,
    val winners: Int? = null
)

object PhilippineLottoApi {
    private const val TIMEOUT = 20_000
    private const val UA = "Mozilla/5.0"

    private fun historyUrl(game: PhilippineLottoGame, page: Int = 1): String {
        val slug = when (game) {
            PhilippineLottoGame.LOTTO_642 -> "lotto-6_slash_42"
            PhilippineLottoGame.MEGA_645 -> "mega-lotto-6_slash_45"
            PhilippineLottoGame.SUPER_649 -> "super-lotto-6_slash_49"
            PhilippineLottoGame.GRAND_655 -> "grand-lotto-6_slash_55"
            PhilippineLottoGame.ULTRA_658 -> "ultra-lotto-6_slash_58"
        }
        return "https://en.lottolyzer.com/history/philippines/$slug/page/$page/per-page/50/summary-view"
    }

    suspend fun fetchRecentDraws(game: PhilippineLottoGame, limit: Int = 30): List<PhilippineLottoDraw> =
        withContext(Dispatchers.IO) {
            val result = mutableListOf<PhilippineLottoDraw>()
            for (page in 1..3) {
                if (result.size >= limit) break
                val html = Jsoup.connect(historyUrl(game, page))
                    .timeout(TIMEOUT)
                    .userAgent(UA)
                    .get()
                    .html()
                result += parse(html, game)
            }
            result.sortedByDescending { it.date }.distinctBy { it.date }.take(limit)
        }

    private fun parse(html: String, game: PhilippineLottoGame): List<PhilippineLottoDraw> {
        val doc = Jsoup.parse(html)
        return doc.select("tr").mapNotNull { row ->
            val cells = row.select("td,th")
            if (cells.size < 3) return@mapNotNull null

            val date = cells.firstNotNullOfOrNull { parseDate(it.text()) } ?: return@mapNotNull null
            val text = cells.joinToString(" ") { it.text() }
            val numberText = Regex("""\b\d{1,2}(?:\s*,\s*\d{1,2}){5}\b""")
                .find(text)?.value ?: return@mapNotNull null
            val numbers = numberText.split(",").mapNotNull { it.trim().toIntOrNull() }
                .filter { it in 1..game.maxNumber }.distinct()
            if (numbers.size != 6) return@mapNotNull null

            PhilippineLottoDraw(
                drawNumber = dateKey(date),
                date = date,
                numbers = numbers.sorted()
            )
        }
    }

    private fun parseDate(text: String): LocalDate? {
        val value = text.trim()
        for (pattern in listOf("yyyy-MM-dd", "M/d/yyyy", "MM/dd/yyyy", "M/d/yy")) {
            try { return LocalDate.parse(value, DateTimeFormatter.ofPattern(pattern, Locale.US)) }
            catch (_: Exception) {}
        }
        return Regex("""\b\d{1,2}/\d{1,2}/\d{4}\b""").find(value)?.value?.let {
            try { LocalDate.parse(it, DateTimeFormatter.ofPattern("M/d/yyyy", Locale.US)) } catch (_: Exception) { null }
        }
    }

    private fun dateKey(date: LocalDate): Int =
        date.year * 10000 + date.monthValue * 100 + date.dayOfMonth
}
