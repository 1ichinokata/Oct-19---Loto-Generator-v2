package com.example.lotogenerator

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

data class PhilippineLottoDraw(
    val drawNumber: Int,
    val date: LocalDate,
    val numbers: List<Int>,
    val jackpot: Double?,
    val winners: Int?
)

object PhilippineLottoApi {
    private const val URL = "https://www.pcso.gov.ph/SearchLottoResult.aspx"
    private const val TIMEOUT = 20_000
    private const val UA = "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 Chrome/154.0 Mobile Safari/537.36"

    suspend fun fetchRecentDraws(game: PhilippineLottoGame, limit: Int = 30): List<PhilippineLottoDraw> =
        withContext(Dispatchers.IO) {
            val today = LocalDate.now(ZoneId.of("Asia/Manila"))
            val html = submitSearch(today.minusDays(45), today, game)
            parse(html, game).sortedByDescending { it.date }.distinctBy { it.drawNumber }.take(limit)
        }

    private fun submitSearch(from: LocalDate, to: LocalDate, game: PhilippineLottoGame): String {
        val response = Jsoup.connect(URL)
            .method(org.jsoup.Connection.Method.GET)
            .timeout(TIMEOUT)
            .userAgent(UA)
            .execute()

        val document = response.parse()
        val form = document.select("form").firstOrNull() ?: return document.html()
        val data = linkedMapOf<String, String>()

        form.select("input[type=hidden]").forEach {
            val name = it.attr("name")
            if (name.isNotBlank()) data[name] = it.attr("value")
        }

        val selects = form.select("select")
        selects.forEach { select ->
            val name = select.attr("name")
            val option = select.select("option[selected]").firstOrNull() ?: select.select("option").firstOrNull()
            if (name.isNotBlank() && option != null) data[name] = option.attr("value")
        }

        val dateSelects = selects.filter { s ->
            val text = s.select("option").eachText()
            text.any { it.trim().matches(Regex("\\d{4}")) } ||
                text.any { it.trim().matches(Regex("(?i)jan(?:uary)?|feb(?:ruary)?|mar(?:ch)?|apr(?:il)?|may|jun(?:e)?|jul(?:y)?|aug(?:ust)?|sep(?:tember)?|oct(?:ober)?|nov(?:ember)?|dec(?:ember)?")) }
        }.take(6)

        applyDate(data, dateSelects.take(3), from)
        applyDate(data, dateSelects.drop(3).take(3), to)

        val gameSelect = selects.firstOrNull { s ->
            s.select("option").any { normalize(it.text()) == normalize(game.pcsoName) }
        }

        if (gameSelect != null) {
            val name = gameSelect.attr("name")
            val option = gameSelect.select("option").firstOrNull {
                normalize(it.text()) == normalize(game.pcsoName)
            }
            if (name.isNotBlank() && option != null) data[name] = option.attr("value")
        }

        val submit = form.select("input[type=submit],button").firstOrNull()
        if (submit != null && submit.attr("name").isNotBlank()) {
            data[submit.attr("name")] = submit.attr("value")
        }

        val action = form.absUrl("action").ifBlank { URL }

        return Jsoup.connect(action)
            .method(org.jsoup.Connection.Method.POST)
            .timeout(TIMEOUT)
            .userAgent(UA)
            .cookies(response.cookies())
            .data(data)
            .followRedirects(true)
            .execute()
            .body()
    }

    private fun applyDate(data: MutableMap<String, String>, selects: List<org.jsoup.nodes.Element>, date: LocalDate) {
        selects.forEach { select ->
            val name = select.attr("name")
            if (name.isBlank()) return@forEach

            val options = select.select("option")
            val option =
                options.firstOrNull { it.text().trim() == date.year.toString() }
                    ?: options.firstOrNull { it.text().trim() == date.dayOfMonth.toString() }
                    ?: options.firstOrNull { it.text().trim() == date.dayOfMonth.toString().padStart(2, '0') }
                    ?: options.firstOrNull {
                        val t = it.text().trim().lowercase(Locale.US)
                        t == date.monthValue.toString() ||
                            t == date.monthValue.toString().padStart(2, '0') ||
                            t == date.month.getDisplayName(java.time.format.TextStyle.FULL, Locale.US).lowercase(Locale.US) ||
                            t == date.month.getDisplayName(java.time.format.TextStyle.SHORT, Locale.US).lowercase(Locale.US)
                    }

            if (option != null) data[name] = option.attr("value")
        }
    }

    private fun parse(html: String, game: PhilippineLottoGame): List<PhilippineLottoDraw> {
        val document = Jsoup.parse(html)

        return document.select("tr").mapNotNull { row ->
            val cells = row.select("th,td")
            if (cells.size < 5) return@mapNotNull null
            if (normalize(cells[0].text()) != normalize(game.pcsoName)) return@mapNotNull null

            val numbers = Regex("\\d{1,2}")
                .findAll(cells[1].text())
                .map { it.value.toInt() }
                .filter { it in 1..game.maxNumber }
                .distinct()
                .take(6)
                .toList()

            if (numbers.size != 6) return@mapNotNull null

            val date = parseDate(cells[2].text()) ?: return@mapNotNull null
            val jackpot = cells[3].text().replace(",", "").replace("₱", "").trim().toDoubleOrNull()
            val winners = Regex("\\d[\\d,]*").find(cells[4].text())?.value?.replace(",", "")?.toIntOrNull()

            PhilippineLottoDraw(
                drawNumber = dateKey(date),
                date = date,
                numbers = numbers.sorted(),
                jackpot = jackpot,
                winners = winners
            )
        }
    }

    private fun parseDate(text: String): LocalDate? {
        for (pattern in listOf("M/d/yyyy", "MM/dd/yyyy", "M/d/yy")) {
            try {
                return LocalDate.parse(text.trim(), DateTimeFormatter.ofPattern(pattern, Locale.US))
            } catch (_: Exception) {
            }
        }
        return null
    }

    private fun dateKey(date: LocalDate): Int =
        date.year * 10000 + date.monthValue * 100 + date.dayOfMonth

    private fun normalize(value: String): String =
        value.lowercase(Locale.US).replace(" ", "").replace("-", "").replace("/", "")
}
