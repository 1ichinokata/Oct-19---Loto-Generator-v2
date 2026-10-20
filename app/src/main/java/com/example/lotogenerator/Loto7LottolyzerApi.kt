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
 * Downloads Loto 7 history and prize information from Lottolyzer.
 *
 * History:
 * https://en.lottolyzer.com/history/japan/lotto-7/page/1/per-page/50/summary-view
 *
 * Individual result:
 * https://en.lottolyzer.com/result/japan/lotto-7/draw/{drawNumber}
 */
class Loto7LottolyzerApi(
    private val application: Application
) {

    companion object {

        const val HISTORY_URL =
            "https://en.lottolyzer.com/history/japan/lotto-7/page/1/per-page/50/summary-view"

        private const val RESULT_URL =
            "https://en.lottolyzer.com/result/japan/lotto-7/draw/"

        private const val TIMEOUT_MS = 20_000
    }

    /**
     * Fetch recent Loto 7 draws.
     *
     * The history table is retrieved first.
     * Prize information is then retrieved separately
     * from each individual result page.
     *
     * If prize information fails for a draw,
     * the draw itself is still returned.
     */
    suspend fun fetchRecentDraws(
        limit: Int = 30
    ): List<Loto7Draw> =
        withContext(Dispatchers.IO) {

            require(limit > 0) {
                "limit must be greater than zero"
            }

            // Retrieve Loto 7 history.
            val historyHtml =
                download(HISTORY_URL)

            val draws =
                parseHistory(historyHtml)
                    .sortedByDescending {
                        it.drawNumber
                    }
                    .distinctBy {
                        it.drawNumber
                    }
                    .take(limit)

            if (draws.isEmpty()) {
                return@withContext emptyList()
            }

            /*
             * Retrieve prize information separately.
             *
             * Failure to retrieve prizes does not
             * prevent the draw from being returned.
             */
            coroutineScope {

                val results =
                    draws.map { draw ->

                        async(Dispatchers.IO) {

                            val prizes =
                                try {

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

    /**
     * Fetch prize information for one draw.
     */
    private fun fetchPrizesForDraw(
        drawNumber: Int
    ): List<Loto7Prize> {

        val url =
            RESULT_URL + drawNumber

        val html =
            download(url)

        return parsePrizes(html)
    }

    /**
     * Download a web page.
     */
    private fun download(
        urlString: String
    ): String {

        val connection =
            (URL(urlString).openConnection()
                    as HttpURLConnection).apply {

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
                .use {
                    it.readText()
                }

        } finally {

            connection.disconnect()
        }
    }

    /**
     * Parse Loto 7 history.
     *
     * Loto 7 has:
     *
     * 7 main winning numbers
     * 2 bonus numbers
     */
    private fun parseHistory(
        html: String
    ): List<Loto7Draw> {

        val document =
            Jsoup.parse(html)

        val draws =
            ArrayList<Loto7Draw>()

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

            if (drawNumber < 1) {
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

            /*
             * Extract exactly seven unique
             * main winning numbers.
             */
            val numbers =
                extractSevenNumbers(
                    winningText
                )

            if (
                numbers.size != 7 ||
                numbers.distinct().size != 7
            ) {
                continue
            }

            /*
             * Loto 7 has TWO bonus numbers.
             *
             * Extract all valid Loto 7 numbers
             * from the bonus column and keep
             * the first two unique numbers.
             */
            val bonusNumbers =
                extractNumbers(
                    bonusText
                )
                    .filter {
                        it in 1..37
                    }
                    .distinct()
                    .take(2)

            draws += Loto7Draw(
                drawNumber = drawNumber,
                date = date,
                numbers = numbers.sorted(),
                bonusNumbers = bonusNumbers.sorted(),
                prizes = emptyList()
            )
        }

        return draws
    }

    /**
     * Parse the prize table from an individual
     * Loto 7 result page.
     *
     * Expected:
     *
     * Division | Prize Pool | Winners | Prize Amount
     */
    private fun parsePrizes(
        html: String
    ): List<Loto7Prize> {

        val document =
            Jsoup.parse(html)

        val prizes =
            ArrayList<Loto7Prize>()

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
             * Loto 7 has seven prize divisions.
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

                    first.contains("6th") ||
                            first.contains("division 6") ||
                            first.contains("div ision 6") ->
                        6

                    first.contains("7th") ||
                            first.contains("division 7") ||
                            first.contains("div ision 7") ->
                        7

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

            prizes += Loto7Prize(
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

    /**
     * Convert a prize amount such as:
     *
     * ¥1,000,000
     * 1,000,000
     * ￥1,000,000
     *
     * into Long.
     */
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

    /**
     * Convert winner text such as:
     *
     * 10
     * 1,234
     * 10 winners
     *
     * into Int.
     */
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

    /**
     * Extract exactly seven unique
     * Loto 7 winning numbers.
     */
    private fun extractSevenNumbers(
        text: String
    ): List<Int> {

        val values =
            extractNumbers(text)
                .filter {
                    it in 1..37
                }

        val result =
            ArrayList<Int>(7)

        for (value in values) {

            if (value !in result) {
                result += value
            }

            if (result.size == 7) {
                break
            }
        }

        return result
    }

    /**
     * Extract valid Loto 7 numbers.
     *
     * Valid range: 1-37.
     */
    private fun extractNumbers(
        text: String
    ): List<Int> {

        return Regex(
            "(?<!\\d)(?:[1-9]|[1-2]\\d|3[0-7])(?!\\d)"
        )
            .findAll(text)
            .mapNotNull {
                it.value.toIntOrNull()
            }
            .toList()
    }
}

/**
 * Loto 7 prize information.
 */
data class Loto7Prize(
    val division: Int,
    val prizePool: Long?,
    val winners: Int?,
    val prizeAmount: Long?
)

/**
 * Loto 7 draw.
 *
 * Loto 7 has seven main numbers
 * and two bonus numbers.
 */
data class Loto7Draw(
    val drawNumber: Int,
    val date: String,
    val numbers: List<Int>,
    val bonusNumbers: List<Int>,
    val prizes: List<Loto7Prize> = emptyList()
)