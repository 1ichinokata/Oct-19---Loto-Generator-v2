package com.example.lotogenerator

import android.app.Application
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Downloads Mini Loto history and prize details from Lottolyzer.
 *
 * History:
 * https://en.lottolyzer.com/history/japan/mini-loto/page/1/per-page/50/summary-view
 *
 * Individual result:
 * https://en.lottolyzer.com/result/japan/mini-loto/draw/{drawNumber}
 */
class MiniLotoLottolyzerApi(
    private val application: Application
) {

    companion object {

        const val HISTORY_URL =
            "https://en.lottolyzer.com/history/japan/mini-loto/page/1/per-page/50/summary-view"

        private const val HOME_SUMMARY_URL =
            "https://en.lottolyzer.com/home/japan/mini-loto/summary-view"

        private const val NUMBER_VIEW_URL =
            "https://en.lottolyzer.com/history/japan/mini-loto/page/1/per-page/50/number-view"

        private const val RESULT_URL =
            "https://en.lottolyzer.com/result/japan/mini-loto/draw/"

        private const val TIMEOUT_MS = 20_000
    }

    suspend fun fetchRecentDraws(
        limit: Int = 30
    ): List<MiniLotoDraw> =
        withContext(Dispatchers.IO) {

            require(limit > 0) {
                "limit must be greater than zero"
            }

            val basicDraws = try {
                parse(download(HISTORY_URL))
            } catch (_: Exception) {
                try {
                    parse(download(HOME_SUMMARY_URL))
                } catch (_: Exception) {
                    emptyList()
                }
            }
                .sortedByDescending { it.drawNumber }
                .distinctBy { it.drawNumber }
                .take(limit)

            if (basicDraws.isEmpty()) {
                return@withContext emptyList()
            }

            // FAST MODE: fetch prize details for the latest draw only.
            val latestDrawNumber = basicDraws.first().drawNumber
            val latestPrizes = try {
                parsePrizes(download(RESULT_URL + latestDrawNumber))
            } catch (_: Exception) {
                emptyList()
            }

            basicDraws.map { draw ->
                draw.copy(prizes = if (draw.drawNumber == latestDrawNumber) latestPrizes else emptyList())
            }
        }

    private fun download(
        urlString: String
    ): String = LottolyzerHttp.get(urlString)

    private fun parse(
        html: String
    ): List<MiniLotoDraw> {

        val document =
            Jsoup.parse(html)

        val draws =
            ArrayList<MiniLotoDraw>()

        for (row in document.select("tr")) {

            val cells =
                row.select("th, td")

            if (cells.size < 4) {
                continue
            }

            val drawNumber =
                cells[0]
                    .text()
                    .trim()
                    .toIntOrNull()
                    ?: continue

            if (drawNumber < 1_000) {
                continue
            }

            val date =
                cells[1]
                    .text()
                    .trim()

            val winningText =
                cells[2]
                    .text()
                    .trim()

            val bonusText =
                cells[3]
                    .text()
                    .trim()

            val numbers =
                extractFiveNumbers(
                    winningText
                )

            if (
                numbers.size != 5 ||
                numbers.distinct().size != 5
            ) {
                continue
            }

            val bonus =
                extractNumbers(bonusText)
                    .firstOrNull {
                        it in 1..31
                    }

            draws +=
                MiniLotoDraw(
                    drawNumber = drawNumber,
                    date = date,
                    numbers = numbers.sorted(),
                    bonus = bonus
                )
        }

        return draws
    }

    /**
     * Reads prize information from an individual
     * Lottolyzer result page.
     *
     * Mini Loto has four prize divisions.
     */
    private fun parsePrizes(
        html: String
    ): List<MiniLotoPrize> {

        val document =
            Jsoup.parse(html)

        val prizes =
            ArrayList<MiniLotoPrize>()

        for (row in document.select("tr")) {

            val cells =
                row.select("th, td")

            if (cells.size < 4) {
                continue
            }

            val rowText =
                row.text()
                    .trim()

            val division =
                extractDivision(rowText)
                    ?: continue

            if (division !in 1..4) {
                continue
            }

            val prizePool =
                parseYen(
                    cells[1].text()
                )

            val winners =
                parseNumber(
                    cells[2].text()
                )

            val prizeAmount =
                parseYen(
                    cells[3].text()
                )

            prizes +=
                MiniLotoPrize(
                    division = division,
                    prizePool = prizePool,
                    winners = winners,
                    prizeAmount = prizeAmount
                )
        }

        return prizes
            .distinctBy {
                it.division
            }
            .sortedBy {
                it.division
            }
    }

    private fun extractDivision(
        text: String
    ): Int? {

        val patterns =
            listOf(
                Regex(
                    "(?i)division\\s*([1-4])"
                ),
                Regex(
                    "(?i)([1-4])(?:st|nd|rd|th)\\s+prize"
                ),
                Regex(
                    "(?i)([1-4])\\s*(?:st|nd|rd|th)"
                )
            )

        for (pattern in patterns) {

            val match =
                pattern.find(text)

            if (match != null) {

                return match
                    .groupValues[1]
                    .toIntOrNull()
            }
        }

        return null
    }

    private fun parseYen(
        text: String
    ): Long? {

        val cleaned =
            text
                .replace(",", "")
                .replace("¥", "")
                .replace("円", "")
                .replace(" ", "")
                .trim()

        val match =
            Regex(
                "(\\d+)"
            ).find(cleaned)

        return match
            ?.groupValues
            ?.getOrNull(1)
            ?.toLongOrNull()
    }

    private fun parseNumber(
        text: String
    ): Int? {

        val match =
            Regex(
                "(\\d[\\d,]*)"
            ).find(text)

        return match
            ?.groupValues
            ?.getOrNull(1)
            ?.replace(",", "")
            ?.toIntOrNull()
    }

    private fun extractFiveNumbers(
        text: String
    ): List<Int> {

        val values =
            extractNumbers(text)
                .filter {
                    it in 1..31
                }

        val result =
            ArrayList<Int>(5)

        for (value in values) {

            if (value !in result) {
                result += value
            }

            if (result.size == 5) {
                break
            }
        }

        return result
    }

    private fun extractNumbers(
        text: String
    ): List<Int> =
        Regex(
            "(?<!\\d)(?:[1-9]|[12]\\d|3[01])(?!\\d)"
        )
            .findAll(text)
            .mapNotNull {
                it.value.toIntOrNull()
            }
            .toList()
}

data class MiniLotoPrize(
    val division: Int,
    val prizePool: Long?,
    val winners: Int?,
    val prizeAmount: Long?
)

data class MiniLotoDraw(
    val drawNumber: Int,
    val date: String,
    val numbers: List<Int>,
    val bonus: Int?,
    val prizes: List<MiniLotoPrize> = emptyList()
)