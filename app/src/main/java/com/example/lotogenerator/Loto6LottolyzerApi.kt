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

class Loto6LottolyzerApi(
    private val application: Application
) {

    companion object {
        const val HISTORY_URL =
            "https://en.lottolyzer.com/history/japan/lotto-6/page/1/per-page/50/summary-view"

        private const val RESULT_URL =
            "https://en.lottolyzer.com/result/japan/lotto-6/draw/"

        private const val TIMEOUT_MS = 20_000
    }

    suspend fun fetchRecentDraws(
        limit: Int = 30
    ): List<Loto6Draw> =
        withContext(Dispatchers.IO) {

            require(limit > 0) {
                "limit must be greater than zero"
            }

            // Keep the original working history retrieval.
            val historyHtml = download(HISTORY_URL)

            val draws = parseHistory(historyHtml)
                .sortedByDescending { it.drawNumber }
                .distinctBy { it.drawNumber }
                .take(limit)

            if (draws.isEmpty()) {
                return@withContext emptyList()
            }

            /*
             * Retrieve prize information separately.
             *
             * This does NOT affect the history parser.
             * If prize information fails, the draw itself
             * still remains available.
             */
            coroutineScope {

                val results = draws.map { draw ->

                    async(Dispatchers.IO) {

                        val prizes = try {
                            fetchPrizesForDraw(
                                draw.drawNumber
                            )
                        } catch (_: Exception) {
                            emptyList()
                        }

                        draw.copy(
                            prizes = prizes
                        )
                    }
                }

                results.awaitAll()
            }
        }

    private fun fetchPrizesForDraw(
        drawNumber: Int
    ): List<Loto6Prize> {

        val url =
            RESULT_URL + drawNumber

        val html = download(url)

        return parsePrizes(html)
    }

    private fun download(
        urlString: String
    ): String {

        val connection =
            (URL(urlString).openConnection() as HttpURLConnection).apply {

                requestMethod = "GET"

                connectTimeout =
                    TIMEOUT_MS

                readTimeout =
                    TIMEOUT_MS

                useCaches = false
                doInput = true

                setRequestProperty(
                    "User-Agent",
                    "Mozilla/5.0 (Android) AppleWebKit/537.36 " +
                            "Chrome/154.0 Mobile Safari/537.36"
                )

                setRequestProperty(
                    "Accept",
                    "text/html,application/xhtml+xml"
                )

                setRequestProperty(
                    "Accept-Language",
                    "en-US,en;q=0.9"
                )
            }

        try {

            val response =
                connection.responseCode

            if (response !in 200..299) {
                throw IOException(
                    "Lottolyzer HTTP $response"
                )
            }

            return connection.inputStream
                .bufferedReader(Charsets.UTF_8)
                .use { it.readText() }

        } finally {
            connection.disconnect()
        }
    }

    /**
     * Original working history parser.
     */
    private fun parseHistory(
        html: String
    ): List<Loto6Draw> {

        val document =
            Jsoup.parse(html)

        val draws =
            ArrayList<Loto6Draw>()

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
                extractSixNumbers(
                    winningText
                )

            if (
                numbers.size != 6 ||
                numbers.distinct().size != 6
            ) {
                continue
            }

            val bonus =
                extractNumbers(bonusText)
                    .firstOrNull {
                        it in 1..43
                    }

            draws += Loto6Draw(
                drawNumber = drawNumber,
                date = date,
                numbers = numbers.sorted(),
                bonus = bonus,
                prizes = emptyList()
            )
        }

        return draws
    }

    /**
     * Parses the prize table from an individual
     * Loto 6 result page.
     *
     * Expected:
     *
     * Division | Prize Pool | Winners | Prize Amount
     */
    private fun parsePrizes(
        html: String
    ): List<Loto6Prize> {

        val document =
            Jsoup.parse(html)

        val prizes =
            ArrayList<Loto6Prize>()

        for (row in document.select("tr")) {

            val cells =
                row.select("th, td")

            if (cells.size < 4) {
                continue
            }

            val first =
                cells[0]
                    .text()
                    .trim()
                    .lowercase()

            /*
             * Only accept actual division rows.
             */
            val division =
                when {

                    first.contains("1st") ||
                            first.contains("division 1") ||
                            first.contains("div ision 1") ->
                        1

                    first.contains("2nd") ||
                            first.contains("division 2") ||
                            first.contains("div ision 2") ->
                        2

                    first.contains("3rd") ||
                            first.contains("division 3") ||
                            first.contains("div ision 3") ->
                        3

                    first.contains("4th") ||
                            first.contains("division 4") ||
                            first.contains("div ision 4") ->
                        4

                    first.contains("5th") ||
                            first.contains("division 5") ||
                            first.contains("div ision 5") ->
                        5

                    else ->
                        null
                }

            if (division == null) {
                continue
            }

            val prizePool =
                parseYenAmount(
                    cells[1].text()
                )

            val winners =
                parseWinnerCount(
                    cells[2].text()
                )

            val prizeAmount =
                parseYenAmount(
                    cells[3].text()
                )

            prizes += Loto6Prize(
                division = division,
                prizePool = prizePool,
                winners = winners,
                prizeAmount = prizeAmount
            )
        }

        return prizes
            .distinctBy { it.division }
            .sortedBy { it.division }
    }

    private fun parseYenAmount(
        text: String
    ): Long? {

        val cleaned =
            text
                .replace(",", "")
                .replace("¥", "")
                .replace("￥", "")
                .trim()

        if (
            cleaned.isEmpty() ||
            cleaned == "-"
        ) {
            return null
        }

        return cleaned.toLongOrNull()
    }

    private fun parseWinnerCount(
        text: String
    ): Int? {

        val cleaned =
            text.trim()

        if (
            cleaned.isEmpty() ||
            cleaned == "-"
        ) {
            return null
        }

        return Regex(
            "\\d[\\d,]*"
        )
            .find(cleaned)
            ?.value
            ?.replace(",", "")
            ?.toIntOrNull()
    }

    private fun extractSixNumbers(
        text: String
    ): List<Int> {

        val values =
            extractNumbers(text)
                .filter {
                    it in 1..43
                }

        val result =
            ArrayList<Int>(6)

        for (value in values) {

            if (value !in result) {
                result += value
            }

            if (result.size == 6) {
                break
            }
        }

        return result
    }

    private fun extractNumbers(
        text: String
    ): List<Int> {

        return Regex(
            "(?<!\\d)(?:[1-9]|[1-3]\\d|4[0-3])(?!\\d)"
        )
            .findAll(text)
            .mapNotNull {
                it.value.toIntOrNull()
            }
            .toList()
    }
}

data class Loto6Prize(
    val division: Int,
    val prizePool: Long?,
    val winners: Int?,
    val prizeAmount: Long?
)

data class Loto6Draw(
    val drawNumber: Int,
    val date: String,
    val numbers: List<Int>,
    val bonus: Int?,
    val prizes: List<Loto6Prize> = emptyList()
)