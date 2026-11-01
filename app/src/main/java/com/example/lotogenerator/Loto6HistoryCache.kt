package com.example.lotogenerator

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class Loto6HistoryCache(context: Context) {
    companion object {
        private const val PREFS_NAME = "loto6_server_cache"
        private const val KEY_HISTORY = "history"
    }

    private val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun save(history: List<Loto6Draw>) {
        if (history.isEmpty()) return
        val array = JSONArray()
        history.sortedByDescending { it.drawNumber }.forEach { draw ->
            val item = JSONObject().apply {
                put("drawNumber", draw.drawNumber)
                put("date", draw.date)
                put("bonus", draw.bonus ?: JSONObject.NULL)
            }
            val numbers = JSONArray()
            draw.numbers.sorted().forEach(numbers::put)
            item.put("numbers", numbers)

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

    fun load(): List<Loto6Draw> {
        val raw = preferences.getString(KEY_HISTORY, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            val result = ArrayList<Loto6Draw>()
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val drawNumber = item.optInt("drawNumber", -1)
                if (drawNumber < 1) continue
                val date = item.optString("date", "")
                val numbersArray = item.optJSONArray("numbers") ?: continue
                val numbers = ArrayList<Int>()
                for (i in 0 until numbersArray.length()) numbers += numbersArray.optInt(i, -1)
                val cleanNumbers = numbers.filter { it in 1..43 }.distinct().sorted()
                if (cleanNumbers.size != 6) continue

                val bonus = if (item.isNull("bonus")) null else item.optInt("bonus", -1).takeIf { it in 1..43 }
                val prizes = ArrayList<Loto6Prize>()
                item.optJSONArray("prizes")?.let { prizeArray ->
                    for (i in 0 until prizeArray.length()) {
                        val prize = prizeArray.optJSONObject(i) ?: continue
                        val division = prize.optInt("division", -1)
                        if (division < 1) continue
                        val prizePool = if (prize.isNull("prizePool")) null else prize.optLong("prizePool")
                        val winners = if (prize.isNull("winners")) null else prize.optInt("winners")
                        val prizeAmount = if (prize.isNull("prizeAmount")) null else prize.optLong("prizeAmount")
                        prizes += Loto6Prize(division, prizePool, winners, prizeAmount)
                    }
                }

                result += Loto6Draw(
                    drawNumber = drawNumber,
                    date = date,
                    numbers = cleanNumbers,
                    bonus = bonus,
                    prizes = prizes
                )
            }
            result.sortedByDescending { it.drawNumber }.distinctBy { it.drawNumber }
        } catch (_: Exception) {
            emptyList()
        }
    }
}
