package com.example.lotogenerator

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

class PhilippineLottoHistoryCache(context: Context) {
    companion object {
        private const val PREFS_NAME = "philippine_lotto_server_cache"
        private const val KEY_PREFIX = "history_"
    }

    private val preferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun save(game: PhilippineLottoGame, history: List<PhilippineLottoDraw>) {
        if (history.isEmpty()) return

        val array = JSONArray()
        history.sortedByDescending { it.date }.forEach { draw ->
            val item = JSONObject()
            item.put("drawNumber", draw.drawNumber)
            item.put("date", draw.date.toString())

            val numbers = JSONArray()
            draw.numbers.sorted().forEach { numbers.put(it) }
            item.put("numbers", numbers)

            if (draw.jackpot == null) item.put("jackpot", JSONObject.NULL)
            else item.put("jackpot", draw.jackpot)

            if (draw.winners == null) item.put("winners", JSONObject.NULL)
            else item.put("winners", draw.winners)

            array.put(item)
        }

        preferences.edit()
            .putString(KEY_PREFIX + game.key, array.toString())
            .apply()
    }

    fun load(game: PhilippineLottoGame): List<PhilippineLottoDraw> {
        val raw = preferences.getString(KEY_PREFIX + game.key, null) ?: return emptyList()

        return try {
            val array = JSONArray(raw)
            val result = ArrayList<PhilippineLottoDraw>()

            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val drawNumber = item.optInt("drawNumber", -1)
                if (drawNumber < 1) continue

                val date = try {
                    LocalDate.parse(item.optString("date"))
                } catch (_: Exception) {
                    continue
                }

                val numbersArray = item.optJSONArray("numbers") ?: continue
                val numbers = ArrayList<Int>()
                for (numberIndex in 0 until numbersArray.length()) {
                    numbers += numbersArray.optInt(numberIndex, -1)
                }

                val cleanNumbers = numbers
                    .filter { it in 1..game.maxNumber }
                    .distinct()
                    .sorted()

                if (cleanNumbers.size != 6) continue

                val jackpot = if (item.isNull("jackpot")) null
                else item.optDouble("jackpot", Double.NaN).takeUnless { it.isNaN() }

                val winners = if (item.isNull("winners")) null
                else item.optInt("winners", -1).takeIf { it >= 0 }

                result += PhilippineLottoDraw(
                    drawNumber = drawNumber,
                    date = date,
                    numbers = cleanNumbers,
                    jackpot = jackpot,
                    winners = winners
                )
            }

            result.sortedByDescending { it.date }.distinctBy { it.date }
        } catch (_: Exception) {
            emptyList()
        }
    }
}
