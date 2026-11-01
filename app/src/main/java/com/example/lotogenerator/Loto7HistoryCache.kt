package com.example.lotogenerator

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class Loto7HistoryCache(context: Context) {
    companion object {
        private const val PREFS_NAME = "loto7_server_cache"
        private const val KEY_HISTORY = "history"
    }

    private val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun save(history: List<Loto7Draw>) {
        if (history.isEmpty()) return
        val array = JSONArray()
        history.sortedByDescending { it.drawNumber }.forEach { draw ->
            val item = JSONObject()
            item.put("drawNumber", draw.drawNumber)
            item.put("date", draw.date)

            val numbers = JSONArray()
            draw.numbers.sorted().forEach(numbers::put)
            item.put("numbers", numbers)

            val bonusNumbers = JSONArray()
            draw.bonusNumbers.distinct().sorted().take(2).forEach(bonusNumbers::put)
            item.put("bonusNumbers", bonusNumbers)

            val prizes = JSONArray()
            draw.prizes.forEach { prize ->
                prizes.put(JSONObject().apply {
                    put("division", prize.division)
                    put("prizePool", prize.prizePool ?: JSONObject.NULL)
                    put("winners", prize.winners ?: JSONObject.NULL)
                    put("prizeAmount", prize.prizeAmount ?: JSONObject.NULL)
                })
            }
            item.put("prizes", prizes)
            array.put(item)
        }
        preferences.edit().putString(KEY_HISTORY, array.toString()).apply()
    }

    fun load(): List<Loto7Draw> {
        val raw = preferences.getString(KEY_HISTORY, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            val result = ArrayList<Loto7Draw>()
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val drawNumber = item.optInt("drawNumber", -1)
                if (drawNumber < 1) continue
                val date = item.optString("date", "")
                val numbersArray = item.optJSONArray("numbers") ?: continue
                val numbers = ArrayList<Int>()
                for (i in 0 until numbersArray.length()) numbers += numbersArray.optInt(i, -1)
                val cleanNumbers = numbers.filter { it in 1..37 }.distinct().sorted()
                if (cleanNumbers.size != 7) continue

                val bonusNumbers = ArrayList<Int>()
                item.optJSONArray("bonusNumbers")?.let { bonuses ->
                    for (i in 0 until bonuses.length()) {
                        val number = bonuses.optInt(i, -1)
                        if (number in 1..37 && number !in bonusNumbers) bonusNumbers += number
                        if (bonusNumbers.size == 2) break
                    }
                }

                val prizes = ArrayList<Loto7Prize>()
                item.optJSONArray("prizes")?.let { prizeArray ->
                    for (i in 0 until prizeArray.length()) {
                        val prize = prizeArray.optJSONObject(i) ?: continue
                        val division = prize.optInt("division", -1)
                        if (division < 1) continue
                        val prizePool = if (prize.isNull("prizePool")) null else prize.optLong("prizePool")
                        val winners = if (prize.isNull("winners")) null else prize.optInt("winners")
                        val prizeAmount = if (prize.isNull("prizeAmount")) null else prize.optLong("prizeAmount")
                        prizes += Loto7Prize(division, prizePool, winners, prizeAmount)
                    }
                }

                result += Loto7Draw(
                    drawNumber = drawNumber,
                    date = date,
                    numbers = cleanNumbers,
                    bonusNumbers = bonusNumbers.sorted(),
                    prizes = prizes
                )
            }
            result.sortedByDescending { it.drawNumber }.distinctBy { it.drawNumber }
        } catch (_: Exception) {
            emptyList()
        }
    }
}
