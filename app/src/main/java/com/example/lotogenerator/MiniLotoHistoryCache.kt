package com.example.lotogenerator

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class MiniLotoHistoryCache(context: Context) {
    companion object {
        private const val PREFS_NAME = "miniloto_server_cache"
        private const val KEY_HISTORY = "history"
    }

    private val preferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun save(history: List<MiniLotoDraw>) {
        if (history.isEmpty()) return

        val array = JSONArray()
        history.sortedByDescending { it.drawNumber }.forEach { draw ->
            val item = JSONObject()
            item.put("drawNumber", draw.drawNumber)
            item.put("date", draw.date)

            val numbers = JSONArray()
            draw.numbers.sorted().forEach { numbers.put(it) }
            item.put("numbers", numbers)

            if (draw.bonus == null) item.put("bonus", JSONObject.NULL)
            else item.put("bonus", draw.bonus)

            val prizes = JSONArray()
            draw.prizes.forEach { prize ->
                val prizeObject = JSONObject()
                prizeObject.put("division", prize.division)
                prizeObject.put("prizePool", prize.prizePool ?: JSONObject.NULL)
                prizeObject.put("winners", prize.winners ?: JSONObject.NULL)
                prizeObject.put("prizeAmount", prize.prizeAmount ?: JSONObject.NULL)
                prizes.put(prizeObject)
            }
            item.put("prizes", prizes)
            array.put(item)
        }

        preferences.edit().putString(KEY_HISTORY, array.toString()).apply()
    }

    fun load(): List<MiniLotoDraw> {
        val raw = preferences.getString(KEY_HISTORY, null) ?: return emptyList()

        return try {
            val array = JSONArray(raw)
            val result = ArrayList<MiniLotoDraw>()

            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val drawNumber = item.optInt("drawNumber", -1)
                if (drawNumber < 1) continue

                val date = item.optString("date", "")
                val numbersArray = item.optJSONArray("numbers") ?: continue
                val numbers = ArrayList<Int>()

                for (numberIndex in 0 until numbersArray.length()) {
                    numbers += numbersArray.optInt(numberIndex, -1)
                }

                val cleanNumbers = numbers
                    .filter { it in 1..31 }
                    .distinct()
                    .sorted()

                if (cleanNumbers.size != 5) continue

                val bonus = if (item.isNull("bonus")) {
                    null
                } else {
                    item.optInt("bonus", -1).takeIf { it in 1..31 }
                }

                val prizesArray = item.optJSONArray("prizes")
                val prizes = ArrayList<MiniLotoPrize>()

                if (prizesArray != null) {
                    for (prizeIndex in 0 until prizesArray.length()) {
                        val prizeObject = prizesArray.optJSONObject(prizeIndex) ?: continue
                        val division = prizeObject.optInt("division", -1)
                        if (division < 1) continue

                        val prizePool = if (prizeObject.isNull("prizePool")) null
                        else prizeObject.optLong("prizePool")
                        val winners = if (prizeObject.isNull("winners")) null
                        else prizeObject.optInt("winners")
                        val prizeAmount = if (prizeObject.isNull("prizeAmount")) null
                        else prizeObject.optLong("prizeAmount")

                        prizes += MiniLotoPrize(
                            division = division,
                            prizePool = prizePool,
                            winners = winners,
                            prizeAmount = prizeAmount
                        )
                    }
                }

                result += MiniLotoDraw(
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
