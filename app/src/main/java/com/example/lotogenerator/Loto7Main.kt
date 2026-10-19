
package com.example.lotogenerator

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch


enum class Loto7OddEvenFilter(
    val label: String,
    val oddCount: Int?
) {
    ANY("Any", null),
    THREE_FOUR("3 Odd / 4 Even", 3),
    FOUR_THREE("4 Odd / 3 Even", 4),
    FIVE_TWO("5 Odd / 2 Even", 5),
    TWO_FIVE("2 Odd / 5 Even", 2),
    SIX_ONE("6 Odd / 1 Even", 6),
    ONE_SIX("1 Odd / 6 Even", 1)
}


enum class Loto7LowHighFilter(
    val label: String,
    val lowCount: Int?
) {
    ANY("Any", null),
    THREE_FOUR("3 Low / 4 High", 3),
    FOUR_THREE("4 Low / 3 High", 4),
    FIVE_TWO("5 Low / 2 High", 5),
    TWO_FIVE("2 Low / 5 High", 2),
    SIX_ONE("6 Low / 1 High", 6),
    ONE_SIX("1 Low / 6 High", 1)
}


enum class Loto7Method(
    val label: String
) {
    RANDOM("Random"),
    BALANCED("Balanced"),
    FREQUENCY("Frequency"),
    HOT("Hot"),
    COLD("Cold")
}


data class Loto7Combination(
    val numbers: List<Int>,
    val score: Int,
    val method: Loto7Method
)


class Loto7Engine {

    fun generateRandom(
        pool: List<Int>,
        oddEven: Loto7OddEvenFilter,
        lowHigh: Loto7LowHighFilter
    ): List<Int> {

        var attempts = 0

        while (attempts < 5000) {

            attempts++

            val numbers =
                pool
                    .shuffled()
                    .take(7)
                    .sorted()

            if (
                matchesOddEven(numbers, oddEven) &&
                matchesLowHigh(numbers, lowHigh)
            ) {
                return numbers
            }
        }

        return pool
            .shuffled()
            .take(7)
            .sorted()
    }


    fun generateBalanced(
        pool: List<Int>,
        oddEven: Loto7OddEvenFilter,
        lowHigh: Loto7LowHighFilter
    ): List<Int> {

        var attempts = 0

        while (attempts < 5000) {

            attempts++

            val numbers =
                pool
                    .shuffled()
                    .take(7)
                    .sorted()

            if (
                matchesOddEven(numbers, oddEven) &&
                matchesLowHigh(numbers, lowHigh) &&
                hasGoodBalance(numbers)
            ) {
                return numbers
            }
        }

        return generateRandom(
            pool,
            oddEven,
            lowHigh
        )
    }


    fun generateFrequency(
        pool: List<Int>,
        frequency: Map<Int, Int>,
        oddEven: Loto7OddEvenFilter,
        lowHigh: Loto7LowHighFilter
    ): List<Int> {

        if (frequency.isEmpty()) {

            return generateRandom(
                pool,
                oddEven,
                lowHigh
            )
        }

        val weightedPool =
            ArrayList<Int>()

        for (number in pool.distinct()) {

            val count =
                frequency[number] ?: 0

            val weight =
                (count + 1)
                    .coerceAtMost(20)

            repeat(weight) {
                weightedPool.add(number)
            }
        }

        var attempts = 0

        while (attempts < 5000) {

            attempts++

            val numbers =
                weightedPool
                    .shuffled()
                    .distinct()
                    .take(7)
                    .sorted()

            if (
                numbers.size == 7 &&
                matchesOddEven(numbers, oddEven) &&
                matchesLowHigh(numbers, lowHigh)
            ) {
                return numbers
            }
        }

        return generateRandom(
            pool,
            oddEven,
            lowHigh
        )
    }


    fun generateHot(
        pool: List<Int>,
        frequency: Map<Int, Int>,
        oddEven: Loto7OddEvenFilter,
        lowHigh: Loto7LowHighFilter
    ): List<Int> {

        val sorted =
            frequency.entries
                .filter {
                    it.key in pool
                }
                .sortedWith(
                    Comparator { first, second ->

                        if (first.value != second.value) {
                            second.value.compareTo(first.value)
                        } else {
                            first.key.compareTo(second.key)
                        }
                    }
                )

        val hotNumbers =
            ArrayList<Int>()

        val count =
            minOf(
                7,
                sorted.size
            )

        for (i in 0 until count) {
            hotNumbers.add(
                sorted[i].key
            )
        }

        var attempts = 0

        while (attempts < 5000) {

            attempts++

            val selected =
                ArrayList<Int>()

            selected.addAll(
                hotNumbers.shuffled()
            )

            for (number in pool.shuffled()) {

                if (!selected.contains(number)) {
                    selected.add(number)
                }

                if (selected.size == 7) {
                    break
                }
            }

            val numbers =
                selected
                    .take(7)
                    .sorted()

            if (
                matchesOddEven(numbers, oddEven) &&
                matchesLowHigh(numbers, lowHigh)
            ) {
                return numbers
            }
        }

        return generateRandom(
            pool,
            oddEven,
            lowHigh
        )
    }


    fun generateCold(
        pool: List<Int>,
        frequency: Map<Int, Int>,
        oddEven: Loto7OddEvenFilter,
        lowHigh: Loto7LowHighFilter
    ): List<Int> {

        val sorted =
            frequency.entries
                .filter {
                    it.key in pool
                }
                .sortedWith(
                    Comparator { first, second ->

                        if (first.value != second.value) {
                            first.value.compareTo(second.value)
                        } else {
                            first.key.compareTo(second.key)
                        }
                    }
                )

        val coldNumbers =
            ArrayList<Int>()

        val count =
            minOf(
                7,
                sorted.size
            )

        for (i in 0 until count) {
            coldNumbers.add(
                sorted[i].key
            )
        }

        var attempts = 0

        while (attempts < 5000) {

            attempts++

            val selected =
                ArrayList<Int>()

            selected.addAll(
                coldNumbers.shuffled()
            )

            for (number in pool.shuffled()) {

                if (!selected.contains(number)) {
                    selected.add(number)
                }

                if (selected.size == 7) {
                    break
                }
            }

            val numbers =
                selected
                    .take(7)
                    .sorted()

            if (
                matchesOddEven(numbers, oddEven) &&
                matchesLowHigh(numbers, lowHigh)
            ) {
                return numbers
            }
        }

        return generateRandom(
            pool,
            oddEven,
            lowHigh
        )
    }


    private fun matchesOddEven(
        numbers: List<Int>,
        filter: Loto7OddEvenFilter
    ): Boolean {

        if (filter == Loto7OddEvenFilter.ANY) {
            return true
        }

        val oddCount =
            numbers.count {
                it % 2 != 0
            }

        val evenCount =
            numbers.count {
                it % 2 == 0
            }

        val requiredOdd =
            filter.oddCount
                ?: return true

        val requiredEven =
            7 - requiredOdd

        return oddCount == requiredOdd &&
                evenCount == requiredEven
    }


    private fun matchesLowHigh(
        numbers: List<Int>,
        filter: Loto7LowHighFilter
    ): Boolean {

        if (filter == Loto7LowHighFilter.ANY) {
            return true
        }

        val lowCount =
            numbers.count {
                it in 1..18
            }

        val highCount =
            numbers.count {
                it in 19..37
            }

        val requiredLow =
            filter.lowCount
                ?: return true

        val requiredHigh =
            7 - requiredLow

        return lowCount == requiredLow &&
                highCount == requiredHigh
    }


    private fun hasGoodBalance(
        numbers: List<Int>
    ): Boolean {

        val odd =
            numbers.count {
                it % 2 != 0
            }

        val low =
            numbers.count {
                it in 1..18
            }

        val spread =
            numbers.last() -
                    numbers.first()

        return odd in 2..4 &&
                low in 2..4 &&
                spread >= 20
    }
}


class Loto7ViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val engine =
        Loto7Engine()

    private val _history =
        MutableStateFlow<List<Loto7Draw>>(
            emptyList()
        )

    val history:
            StateFlow<List<Loto7Draw>> =
        _history


    private val _latestDraw =
        MutableStateFlow<Loto7Draw?>(
            null
        )

    val latestDraw:
            StateFlow<Loto7Draw?> =
        _latestDraw


    private val _combinations =
        MutableStateFlow<List<Loto7Combination>>(
            emptyList()
        )

    val combinations:
            StateFlow<List<Loto7Combination>> =
        _combinations


    private val _oddEvenFilter =
        MutableStateFlow(
            Loto7OddEvenFilter.ANY
        )

    val oddEvenFilter:
            StateFlow<Loto7OddEvenFilter> =
        _oddEvenFilter


    private val _lowHighFilter =
        MutableStateFlow(
            Loto7LowHighFilter.ANY
        )

    val lowHighFilter:
            StateFlow<Loto7LowHighFilter> =
        _lowHighFilter


    private val _method =
        MutableStateFlow(
            Loto7Method.BALANCED
        )

    val method:
            StateFlow<Loto7Method> =
        _method


    private val _status =
        MutableStateFlow(
            "Ready"
        )

    val status:
            StateFlow<String> =
        _status


    private val _isUpdating =
        MutableStateFlow(false)

    val isUpdating:
            StateFlow<Boolean> =
        _isUpdating


    private val favoriteStore =
        FavoriteStore(application)


    private val _favorites =
        MutableStateFlow(
            favoriteStore.load("loto7")
        )

    val favorites:
            StateFlow<List<FavoriteItem>> =
        _favorites


    fun toggleFavorite(
        combination: Loto7Combination
    ) {

        val current =
            _favorites.value

        val exists =
            current.any {
                it.numbers ==
                        combination.numbers
            }

        if (exists) {

            favoriteStore.remove(
                "loto7",
                combination.numbers
            )

        } else {

            favoriteStore.save(
                "loto7",
                FavoriteItem(
                    numbers = combination.numbers,
                    score = combination.score,
                    method = combination.method.label,
                    savedAt = System.currentTimeMillis(),
                    targetDrawNumber =
                        _latestDraw.value?.drawNumber?.plus(1)
                )
            )
        }

        _favorites.value =
            favoriteStore.load("loto7")
    }


    /*
     * ========================================================
     * MANUAL FAVORITE
     * ========================================================
     *
     * Used by FavoriteListScreen.
     *
     * Loto 7 requires:
     * - exactly 7 numbers
     * - unique numbers
     * - numbers from 1..37
     *
     * The manual favorite is targeted at the next draw.
     */

    fun addManualFavorite(
        numbers: List<Int>
    ) {

        val sortedNumbers =
            numbers
                .distinct()
                .sorted()

        if (
            sortedNumbers.size != 7 ||
            sortedNumbers.any {
                it !in 1..37
            }
        ) {
            return
        }

        val targetDraw =
            _latestDraw.value
                ?.drawNumber
                ?.plus(1)

        favoriteStore.save(
            "loto7",

            FavoriteItem(
                numbers = sortedNumbers,
                score = 0,
                method = "Manual",
                savedAt = System.currentTimeMillis(),
                targetDrawNumber = targetDraw
            )
        )

        _favorites.value =
            favoriteStore.load("loto7")
    }


    init {
        updateFromLottolyzer()
    }


    fun setOddEvenFilter(
        value: Loto7OddEvenFilter
    ) {
        _oddEvenFilter.value =
            value
    }


    fun setLowHighFilter(
        value: Loto7LowHighFilter
    ) {
        _lowHighFilter.value =
            value
    }


    fun setMethod(
        value: Loto7Method
    ) {
        _method.value =
            value
    }


    fun clearCombinations() {

        _combinations.value =
            emptyList()

        _status.value =
            "Generated combinations cleared"
    }


    private fun getAnalysisDraws():
            List<Loto7Draw> {

        val source =
            _history.value

        val result =
            ArrayList<Loto7Draw>()

        var index = 1

        while (
            index < source.size &&
            result.size < 10
        ) {

            result.add(
                source[index]
            )

            index++
        }

        return result
    }


    private fun buildFrequency(
        draws: List<Loto7Draw>
    ): Map<Int, Int> {

        val frequency =
            HashMap<Int, Int>()

        for (number in 1..37) {
            frequency[number] = 0
        }

        for (draw in draws) {

            for (number in draw.numbers) {

                val old =
                    frequency[number]
                        ?: 0

                frequency[number] =
                    old + 1
            }
        }

        return frequency
    }


    fun generate() {

        val analysisDraws =
            getAnalysisDraws()


        if (analysisDraws.isEmpty()) {

            _combinations.value =
                emptyList()

            _status.value =
                "Please wait for Loto 7 results to load."

            return
        }


        val frequency =
            buildFrequency(
                analysisDraws
            )


        val latestNumbers =
            _latestDraw.value
                ?.numbers
                ?.toSet()
                ?: emptySet()


        val generationPool =
            analysisDraws
                .flatMap {
                    it.numbers
                }
                .filter {
                    it !in latestNumbers
                }
                .distinct()
                .sorted()


        if (generationPool.size < 7) {

            _combinations.value =
                emptyList()

            _status.value =
                "Not enough numbers in the previous 10 draws to generate 7-number combinations."

            return
        }


        val previousWinningCombinations =
            _history.value
                .map {
                        draw ->
                    draw.numbers.sorted()
                }
                .toSet()


        val selectedMethod =
            _method.value

        val oddEven =
            _oddEvenFilter.value

        val lowHigh =
            _lowHighFilter.value


        val result =
            ArrayList<Loto7Combination>()


        var attempts = 0


        while (
            result.size < 10 &&
            attempts < 10000
        ) {

            attempts++


            val numbers =
                when (selectedMethod) {

                    Loto7Method.RANDOM ->
                        engine.generateRandom(
                            generationPool,
                            oddEven,
                            lowHigh
                        )

                    Loto7Method.BALANCED ->
                        engine.generateBalanced(
                            generationPool,
                            oddEven,
                            lowHigh
                        )

                    Loto7Method.FREQUENCY ->
                        engine.generateFrequency(
                            generationPool,
                            frequency,
                            oddEven,
                            lowHigh
                        )

                    Loto7Method.HOT ->
                        engine.generateHot(
                            generationPool,
                            frequency,
                            oddEven,
                            lowHigh
                        )

                    Loto7Method.COLD ->
                        engine.generateCold(
                            generationPool,
                            frequency,
                            oddEven,
                            lowHigh
                        )
                }


            val validSize =
                numbers.size == 7

            val validUnique =
                numbers.distinct().size == 7

            val validPool =
                numbers.all {
                    it in generationPool
                }

            val validLatest =
                numbers.none {
                    it in latestNumbers
                }

            val validHistoricalCombination =
                numbers.sorted() !in
                        previousWinningCombinations


            val requiredOdd =
                oddEven.oddCount

            val actualOdd =
                numbers.count {
                    it % 2 != 0
                }

            val validOddEven =
                requiredOdd == null ||
                        actualOdd == requiredOdd


            val requiredLow =
                lowHigh.lowCount

            val actualLow =
                numbers.count {
                    it in 1..18
                }

            val validLowHigh =
                requiredLow == null ||
                        actualLow == requiredLow


            val validFilters =
                validOddEven &&
                        validLowHigh


            val duplicate =
                result.any { existing ->

                    existing.numbers.sorted() ==
                            numbers.sorted()
                }


            if (
                validSize &&
                validUnique &&
                validPool &&
                validLatest &&
                validHistoricalCombination &&
                validFilters &&
                !duplicate
            ) {

                val score =
                    calculateScore(
                        numbers,
                        frequency
                    )

                result.add(
                    Loto7Combination(
                        numbers =
                            numbers.sorted(),

                        score =
                            score,

                        method =
                            selectedMethod
                    )
                )
            }
        }


        _combinations.value =
            result


        _status.value =
            when {

                result.size == 10 ->
                    "Generated 10 valid combinations"

                result.isNotEmpty() ->
                    "Generated ${result.size} valid combinations. Try a less restrictive filter for 10."

                else ->
                    "No valid combinations found. Try a less restrictive filter."
            }
    }


    private fun calculateScore(
        numbers: List<Int>,
        frequency: Map<Int, Int>
    ): Int {

        var score = 50

        val odd =
            numbers.count {
                it % 2 != 0
            }

        val low =
            numbers.count {
                it in 1..18
            }

        if (odd in 2..4) {
            score += 10
        }

        if (low in 2..4) {
            score += 10
        }

        val totalFrequency =
            numbers.sumOf {
                frequency[it] ?: 0
            }

        score +=
            minOf(
                20,
                totalFrequency * 2
            )

        val spread =
            numbers.last() -
                    numbers.first()

        if (spread >= 20) {
            score += 10
        }

        return score.coerceIn(
            0,
            100
        )
    }


    fun updateFromLottolyzer() {

        if (_isUpdating.value) {
            return
        }

        viewModelScope.launch {

            _isUpdating.value =
                true

            _status.value =
                "Connecting to Lottolyzer......"

            try {

                val api =
                    Loto7LottolyzerApi(
                        getApplication<Application>()
                    )

                val draws =
                    api.fetchRecentDraws(30)

                if (draws.isEmpty()) {

                    _status.value =
                        "Lottolyzer page loaded, but no Loto 7 results could be read."

                    return@launch
                }

                val sorted =
                    draws.sortedByDescending {
                        it.drawNumber
                    }

                val latest =
                    sorted[0]

                _history.value =
                    sorted

                _latestDraw.value =
                    latest

                _status.value =
                    "Ready - tap Generate 10 Combinations"

            } catch (error: Exception) {

                _status.value =
                    "Lottolyzer update failed: ${
                        error.message
                            ?: "network or parsing error"
                    }"

            } finally {

                _isUpdating.value =
                    false
            }
        }
    }
}


@Composable
fun Loto7App() {

    val vm:
            Loto7ViewModel =
        viewModel()

    val context =
        LocalContext.current

    val preferences =
        remember(context) {

            context.getSharedPreferences(
                "combined_loto_settings",
                Context.MODE_PRIVATE
            )
        }

    var darkMode by remember {

        mutableStateOf(
            preferences.getBoolean(
                "dark_mode",
                false
            )
        )
    }

    LaunchedEffect(darkMode) {

        preferences
            .edit()
            .putBoolean(
                "dark_mode",
                darkMode
            )
            .apply()
    }

    val history by
    vm.history.collectAsState()

    val latestDraw by
    vm.latestDraw.collectAsState()

    val combinations by
    vm.combinations.collectAsState()

    val oddEvenFilter by
    vm.oddEvenFilter.collectAsState()

    val lowHighFilter by
    vm.lowHighFilter.collectAsState()

    val method by
    vm.method.collectAsState()

    val status by
    vm.status.collectAsState()

    val isUpdating by
    vm.isUpdating.collectAsState()

    val favorites by
    vm.favorites.collectAsState()


    val lightScheme =
        lightColorScheme(
            primary =
                androidx.compose.ui.graphics.Color(
                    0xFF3F51B5
                ),
            secondary =
                androidx.compose.ui.graphics.Color(
                    0xFF00897B
                ),
            surface =
                androidx.compose.ui.graphics.Color(
                    0xFFF8F9FC
                ),
            background =
                androidx.compose.ui.graphics.Color(
                    0xFFF8F9FC
                )
        )


    val darkScheme =
        darkColorScheme(
            primary =
                androidx.compose.ui.graphics.Color(
                    0xFF9FA8FF
                ),
            secondary =
                androidx.compose.ui.graphics.Color(
                    0xFF80CBC4
                ),
            surface =
                androidx.compose.ui.graphics.Color(
                    0xFF12141A
                ),
            background =
                androidx.compose.ui.graphics.Color(
                    0xFF0E1015
                )
        )


    MaterialTheme(
        colorScheme =
            if (darkMode)
                darkScheme
            else
                lightScheme
    ) {

        Surface(
            modifier =
                Modifier.fillMaxSize(),

            color =
                MaterialTheme
                    .colorScheme
                    .background
        ) {

            Loto7Scaffold(
                darkMode = darkMode,

                onDarkModeChange = {
                    darkMode = it
                },

                latestDraw =
                    latestDraw,

                history =
                    history,

                combinations =
                    combinations,

                oddEvenFilter =
                    oddEvenFilter,

                lowHighFilter =
                    lowHighFilter,

                method =
                    method,

                status =
                    status,

                isUpdating =
                    isUpdating,

                favorites =
                    favorites,

                onUpdate = {
                    vm.updateFromLottolyzer()
                },

                onGenerate = {
                    vm.generate()
                },

                onClear = {
                    vm.clearCombinations()
                },

                onOddEvenChange = {
                    vm.setOddEvenFilter(it)
                },

                onLowHighChange = {
                    vm.setLowHighFilter(it)
                },

                onMethodChange = {
                    vm.setMethod(it)
                },

                onToggleFavorite = {
                    vm.toggleFavorite(it)
                },

                onAddManualFavorite = {
                        numbers ->
                    vm.addManualFavorite(numbers)
                }
            )
        }
    }
}


@Composable
fun Loto7Scaffold(
    darkMode: Boolean,
    onDarkModeChange: (Boolean) -> Unit,
    latestDraw: Loto7Draw?,
    history: List<Loto7Draw>,
    combinations: List<Loto7Combination>,
    oddEvenFilter: Loto7OddEvenFilter,
    lowHighFilter: Loto7LowHighFilter,
    method: Loto7Method,
    status: String,
    isUpdating: Boolean,
    favorites: List<FavoriteItem>,
    onUpdate: () -> Unit,
    onGenerate: () -> Unit,
    onClear: () -> Unit,
    onOddEvenChange: (Loto7OddEvenFilter) -> Unit,
    onLowHighChange: (Loto7LowHighFilter) -> Unit,
    onMethodChange: (Loto7Method) -> Unit,
    onToggleFavorite: (Loto7Combination) -> Unit,
    onAddManualFavorite: (List<Int>) -> Unit
) {

    var selectedTab by remember {
        mutableStateOf(0)
    }

    val tabs =
        listOf(
            "Generator",
            "History",
            "Frequency",
            "Favorites"
        )

    val icons =
        listOf(
            Icons.Default.Casino,
            Icons.Default.History,
            Icons.Default.BarChart,
            Icons.Default.Favorite
        )


    Scaffold(

        topBar = {

            Surface(
                modifier =
                    Modifier.fillMaxWidth(),

                tonalElevation = 3.dp,

                shadowElevation = 1.dp
            ) {

                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(72.dp)
                            .padding(
                                start = 16.dp,
                                end = 8.dp
                            ),

                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Column(
                        modifier =
                            Modifier.weight(1f)
                    ) {
                    }


                    IconButton(
                        onClick = {
                            onDarkModeChange(
                                !darkMode
                            )
                        }
                    ) {

                        Icon(
                            if (darkMode)
                                Icons.Default.LightMode
                            else
                                Icons.Default.DarkMode,

                            contentDescription =
                                if (darkMode)
                                    "Light mode"
                                else
                                    "Dark mode"
                        )
                    }


                    IconButton(
                        onClick =
                            onUpdate,

                        enabled =
                            !isUpdating
                    ) {

                        Icon(
                            Icons.Default.Refresh,
                            contentDescription =
                                "Refresh results"
                        )
                    }
                }
            }
        },


        bottomBar = {

            NavigationBar {

                tabs.forEachIndexed {
                        index,
                        title ->

                    NavigationBarItem(
                        selected =
                            selectedTab ==
                                    index,

                        onClick = {
                            selectedTab =
                                index
                        },

                        icon = {

                            Icon(
                                icons[index],
                                contentDescription =
                                    title
                            )
                        },

                        label = {
                            Text(title)
                        }
                    )
                }
            }
        }

    ) { padding ->


        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
        ) {


            Loto7LatestResultCard(
                draw =
                    latestDraw,

                isUpdating =
                    isUpdating,

                onUpdate =
                    onUpdate
            )


            when (selectedTab) {

                0 ->

                    Loto7GeneratorScreen(
                        combinations =
                            combinations,

                        oddEvenFilter =
                            oddEvenFilter,

                        lowHighFilter =
                            lowHighFilter,

                        method =
                            method,

                        status =
                            status,

                        onGenerate =
                            onGenerate,

                        onClear =
                            onClear,

                        onOddEvenChange =
                            onOddEvenChange,

                        onLowHighChange =
                            onLowHighChange,

                        onMethodChange =
                            onMethodChange,

                        favorites =
                            favorites,

                        onToggleFavorite =
                            onToggleFavorite
                    )


                1 ->

                    Loto7HistoryScreen(
                        history =
                            history
                    )


                2 ->

                    Loto7FrequencyScreen(
                        history =
                            history
                    )


                3 ->

                    FavoriteListScreen(
                        lotteryName =
                            "Loto 7",

                        favorites =
                            favorites,

                        latestDrawNumbers =
                            latestDraw
                                ?.numbers
                                ?.toSet()
                                ?: emptySet(),

                        latestDrawNumber =
                            latestDraw
                                ?.drawNumber
                                ?: 0,

                        onRemove = { item ->

                            onToggleFavorite(
                                Loto7Combination(
                                    item.numbers,
                                    item.score,

                                    Loto7Method.entries
                                        .firstOrNull {
                                            it.label ==
                                                    item.method
                                        }
                                        ?: Loto7Method.BALANCED
                                )
                            )
                        },

                        onAddManual = {
                                numbers ->
                            onAddManualFavorite(numbers)
                        }
                    )
            }
        }
    }
}


@Composable
fun Loto7LatestResultCard(
    draw: Loto7Draw?,
    isUpdating: Boolean,
    onUpdate: () -> Unit
) {

    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 16.dp,
                    vertical = 8.dp
                ),

        shape =
            RoundedCornerShape(22.dp),

        colors =
            androidx.compose.material3.CardDefaults
                .cardColors(
                    containerColor =
                        MaterialTheme
                            .colorScheme
                            .primaryContainer
                )
    ) {

        Column(
            Modifier.padding(18.dp)
        ) {

            Row(
                Modifier.fillMaxWidth(),

                verticalAlignment =
                    Alignment.CenterVertically,

                horizontalArrangement =
                    Arrangement.SpaceBetween
            ) {

                Column(
                    Modifier.weight(1f)
                ) {

                    Row(
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        Icon(
                            Icons.Default.EmojiEvents,
                            contentDescription = null,
                            modifier =
                                Modifier.size(20.dp)
                        )

                        Spacer(
                            Modifier.size(7.dp)
                        )

                        Text(
                            "LATEST RESULT",
                            fontWeight =
                                FontWeight.Bold
                        )
                    }


                    Text(
                        "Lottolyzer • ${
                            draw?.let {
                                "Loto7Draw ${it.drawNumber}"
                            } ?: "Updating…"
                        }",

                        style =
                            MaterialTheme
                                .typography
                                .labelMedium,

                        color =
                            MaterialTheme
                                .colorScheme
                                .onPrimaryContainer
                                .copy(alpha = .75f)
                    )
                }


                if (isUpdating) {

                    androidx.compose.material3
                        .CircularProgressIndicator(
                            Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )

                } else {

                    IconButton(
                        onClick = onUpdate
                    ) {

                        Icon(
                            Icons.Default.Refresh,
                            contentDescription =
                                "Refresh"
                        )
                    }
                }
            }


            Spacer(
                Modifier.height(14.dp)
            )


            if (draw == null) {

                Text(
                    "Loading Loto 7 results…"
                )

            } else {

                Row(
                    Modifier.fillMaxWidth(),

                    horizontalArrangement =
                        Arrangement.SpaceBetween
                ) {

                    draw.numbers.forEach {

                        Loto7NumberBubble(
                            it,
                            prominent = true
                        )
                    }
                }


                Spacer(
                    Modifier.height(10.dp)
                )


                Row(
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Text(
                        draw.date,

                        style =
                            MaterialTheme
                                .typography
                                .labelMedium
                    )


                    if (!draw.bonusNumbers.isNullOrEmpty()) {

                        Spacer(
                            Modifier.weight(1f)
                        )

                        Text(
                            "Bonus ${
                                draw.bonusNumbers.joinToString(" - ") {
                                    it.toString().padStart(2, '0')
                                }
                            }",

                            style =
                                MaterialTheme
                                    .typography
                                    .labelMedium,

                            fontWeight =
                                FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun Loto7GeneratorScreen(
    combinations: List<Loto7Combination>,
    oddEvenFilter: Loto7OddEvenFilter,
    lowHighFilter: Loto7LowHighFilter,
    method: Loto7Method,
    status: String,
    onGenerate: () -> Unit,
    onClear: () -> Unit,
    favorites: List<FavoriteItem> = emptyList(),
    onToggleFavorite: (Loto7Combination) -> Unit = {},
    onOddEvenChange: (Loto7OddEvenFilter) -> Unit,
    onLowHighChange: (Loto7LowHighFilter) -> Unit,
    onMethodChange: (Loto7Method) -> Unit
) {

    var showGenerateConfirmation by remember {
        mutableStateOf(false)
    }

    var showClearConfirmation by remember {
        mutableStateOf(false)
    }


    if (showGenerateConfirmation) {

        AlertDialog(
            onDismissRequest = {
                showGenerateConfirmation =
                    false
            },

            title = {
                Text(
                    "Generate combinations?"
                )
            },

            text = {

                Column {

                    Text(
                        "Generate 10 new Loto 7 combinations using these settings?"
                    )

                    Spacer(
                        Modifier.height(12.dp)
                    )

                    Text(
                        "Strategy: ${method.label}",
                        fontWeight =
                            FontWeight.Bold
                    )

                    Text(
                        "Odd / Even: ${oddEvenFilter.label}"
                    )

                    Text(
                        "Low / High: ${lowHighFilter.label}"
                    )

                    if (
                        combinations.isNotEmpty()
                    ) {

                        Spacer(
                            Modifier.height(12.dp)
                        )

                        Text(
                            "Your current generated combinations will be replaced."
                        )
                    }
                }
            },

            confirmButton = {

                Button(
                    onClick = {

                        showGenerateConfirmation =
                            false

                        onGenerate()
                    }
                ) {

                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null
                    )

                    Spacer(
                        Modifier.size(6.dp)
                    )

                    Text("Generate")
                }
            },

            dismissButton = {

                TextButton(
                    onClick = {
                        showGenerateConfirmation =
                            false
                    }
                ) {

                    Text("Cancel")
                }
            }
        )
    }


    if (showClearConfirmation) {

        AlertDialog(
            onDismissRequest = {
                showClearConfirmation =
                    false
            },

            title = {
                Text(
                    "Clear combinations?"
                )
            },

            text = {

                Text(
                    "Are you sure you want to clear all generated Loto 7 combinations?"
                )
            },

            confirmButton = {

                Button(
                    onClick = {

                        showClearConfirmation =
                            false

                        onClear()
                    }
                ) {

                    Text("Clear")
                }
            },

            dismissButton = {

                TextButton(
                    onClick = {
                        showClearConfirmation =
                            false
                    }
                ) {

                    Text("Cancel")
                }
            }
        )
    }


    LazyColumn(
        modifier =
            Modifier.fillMaxSize(),

        contentPadding =
            androidx.compose.foundation.layout.PaddingValues(
                horizontal = 16.dp,
                vertical = 8.dp
            ),

        verticalArrangement =
            Arrangement.spacedBy(10.dp)
    ) {

        item {

            Loto7SectionHeader(
                title =
                    "Generate combinations",

                subtitle =
                    "Numbers are selected only from the previous 10 draws."
            )
        }


        item {

            Loto7FilterCard(
                oddEvenFilter,
                lowHighFilter,
                method,
                onOddEvenChange,
                onLowHighChange,
                onMethodChange
            )
        }


        item {

            Row(
                modifier =
                    Modifier.fillMaxWidth(),

                horizontalArrangement =
                    Arrangement.spacedBy(10.dp)
            ) {

                Button(
                    onClick = {
                        showGenerateConfirmation =
                            true
                    },

                    modifier =
                        Modifier
                            .weight(1f)
                            .height(54.dp),

                    shape =
                        RoundedCornerShape(16.dp)
                ) {

                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null
                    )

                    Spacer(
                        Modifier.size(6.dp)
                    )

                    Text(
                        "Generate",
                        fontWeight =
                            FontWeight.Bold
                    )
                }


                androidx.compose.material3
                    .OutlinedButton(
                        onClick = {
                            showClearConfirmation =
                                true
                        },

                        enabled =
                            combinations.isNotEmpty(),

                        modifier =
                            Modifier
                                .weight(1f)
                                .height(54.dp),

                        shape =
                            RoundedCornerShape(
                                16.dp
                            )
                    ) {

                        Text(
                            "Clear",
                            fontWeight =
                                FontWeight.Bold
                        )
                    }
            }
        }


        item {

            Loto7StatusCard(
                status
            )
        }


        items(combinations) {
                combination ->

            Loto7CombinationCard(
                combination =
                    combination,

                isFavorite =
                    favorites.any {
                        it.numbers ==
                                combination.numbers
                    },

                onFavorite = {

                    onToggleFavorite(
                        combination
                    )
                }
            )
        }
    }
}


@Composable
fun Loto7SectionHeader(
    title: String,
    subtitle: String
) {

    Column(
        Modifier.padding(
            vertical = 2.dp
        )
    ) {

        Text(
            title,
            style =
                MaterialTheme
                    .typography
                    .titleLarge,
            fontWeight =
                FontWeight.Bold
        )

        Text(
            subtitle,
            style =
                MaterialTheme
                    .typography
                    .bodySmall,
            color =
                MaterialTheme
                    .colorScheme
                    .onSurfaceVariant
        )
    }
}


@Composable
fun Loto7StatusCard(
    status: String
) {

    Surface(
        shape =
            RoundedCornerShape(14.dp),

        color =
            MaterialTheme
                .colorScheme
                .surfaceVariant
    ) {

        Row(
            Modifier
                .fillMaxWidth()
                .padding(12.dp),

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Icon(
                Icons.Default.Info,
                contentDescription = null,
                modifier =
                    Modifier.size(18.dp)
            )

            Spacer(
                Modifier.size(8.dp)
            )

            Text(
                status,
                style =
                    MaterialTheme
                        .typography
                        .labelMedium
            )
        }
    }
}


@Composable
fun Loto7FilterCard(
    oddEvenFilter: Loto7OddEvenFilter,
    lowHighFilter: Loto7LowHighFilter,
    method: Loto7Method,
    onOddEvenChange: (Loto7OddEvenFilter) -> Unit,
    onLowHighChange: (Loto7LowHighFilter) -> Unit,
    onMethodChange: (Loto7Method) -> Unit
) {

    Card(
        modifier =
            Modifier.fillMaxWidth(),

        shape =
            RoundedCornerShape(20.dp)
    ) {

        Column(
            Modifier.padding(16.dp)
        ) {

            Row(
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Icon(
                    Icons.Default.Tune,
                    contentDescription = null,
                    modifier =
                        Modifier.size(20.dp)
                )

                Spacer(
                    Modifier.size(8.dp)
                )

                Text(
                    "Generation settings",
                    style =
                        MaterialTheme
                            .typography
                            .titleMedium,
                    fontWeight =
                        FontWeight.Bold
                )
            }


            Spacer(
                Modifier.height(12.dp)
            )


            Loto7SimpleDropdown(
                "Odd / Even",
                oddEvenFilter.label,
                Loto7OddEvenFilter.entries
                    .map {
                        it.label
                    }
            ) { selected ->

                Loto7OddEvenFilter.entries
                    .firstOrNull {
                        it.label ==
                                selected
                    }
                    ?.let(
                        onOddEvenChange
                    )
            }


            Loto7SimpleDropdown(
                "Low / High",
                lowHighFilter.label,
                Loto7LowHighFilter.entries
                    .map {
                        it.label
                    }
            ) { selected ->

                Loto7LowHighFilter.entries
                    .firstOrNull {
                        it.label ==
                                selected
                    }
                    ?.let(
                        onLowHighChange
                    )
            }


            Loto7SimpleDropdown(
                "Strategy",
                method.label,
                Loto7Method.entries
                    .map {
                        it.label
                    }
            ) { selected ->

                Loto7Method.entries
                    .firstOrNull {
                        it.label ==
                                selected
                    }
                    ?.let(
                        onMethodChange
                    )
            }
        }
    }
}


@Composable
fun Loto7SimpleDropdown(
    label: String,
    selected: String,
    options: List<String>,
    onSelected: (String) -> Unit
) {

    var expanded by remember {
        mutableStateOf(false)
    }


    Column(
        Modifier.padding(
            bottom = 8.dp
        )
    ) {

        Text(
            label,
            style =
                MaterialTheme
                    .typography
                    .labelMedium,
            color =
                MaterialTheme
                    .colorScheme
                    .onSurfaceVariant
        )


        Spacer(
            Modifier.height(5.dp)
        )


        Box(
            Modifier.fillMaxWidth()
        ) {

            androidx.compose.material3
                .OutlinedButton(
                    onClick = {
                        expanded = true
                    },

                    modifier =
                        Modifier.fillMaxWidth(),

                    shape =
                        RoundedCornerShape(
                            13.dp
                        )
                ) {

                    Text(
                        selected,
                        Modifier.weight(1f)
                    )

                    Icon(
                        Icons.Default.ExpandMore,
                        contentDescription = null
                    )
                }


            DropdownMenu(
                expanded =
                    expanded,

                onDismissRequest = {
                    expanded = false
                }
            ) {

                options.forEach {
                        option ->

                    DropdownMenuItem(
                        text = {
                            Text(option)
                        },

                        onClick = {

                            expanded =
                                false

                            onSelected(
                                option
                            )
                        }
                    )
                }
            }
        }
    }
}


@Composable
fun Loto7CombinationCard(
    combination: Loto7Combination,
    isFavorite: Boolean = false,
    onFavorite: () -> Unit = {}
) {

    val context =
        LocalContext.current


    Card(
        modifier =
            Modifier.fillMaxWidth(),

        shape =
            RoundedCornerShape(20.dp)
    ) {

        Column(
            Modifier.padding(15.dp)
        ) {

            Row(
                Modifier.fillMaxWidth(),

                verticalAlignment =
                    Alignment.CenterVertically,

                horizontalArrangement =
                    Arrangement.SpaceBetween
            ) {

                Surface(
                    shape =
                        RoundedCornerShape(8.dp),

                    color =
                        MaterialTheme
                            .colorScheme
                            .secondaryContainer
                ) {

                    Text(
                        combination
                            .method
                            .label
                            .uppercase(),

                        Modifier.padding(
                            horizontal = 8.dp,
                            vertical = 4.dp
                        ),

                        style =
                            MaterialTheme
                                .typography
                                .labelSmall,

                        fontWeight =
                            FontWeight.Bold
                    )
                }


                Row(
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Text(
                        "${combination.score}/100",
                        fontWeight =
                            FontWeight.Bold
                    )


                    IconButton(
                        onClick =
                            onFavorite
                    ) {

                        Icon(
                            if (isFavorite)
                                Icons.Default.Favorite
                            else
                                Icons.Default.FavoriteBorder,

                            contentDescription =
                                if (isFavorite)
                                    "Remove from favorites"
                                else
                                    "Save to favorites"
                        )
                    }


                    IconButton(
                        onClick = {

                            val clipboard =
                                context.getSystemService(
                                    Context.CLIPBOARD_SERVICE
                                ) as ClipboardManager


                            clipboard.setPrimaryClip(
                                ClipData.newPlainText(
                                    "Loto 7",

                                    combination
                                        .numbers
                                        .joinToString("-")
                                )
                            )
                        }
                    ) {

                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription =
                                "Copy combination"
                        )
                    }
                }
            }


            Spacer(
                Modifier.height(12.dp)
            )


            Row(
                Modifier.fillMaxWidth(),

                horizontalArrangement =
                    Arrangement.SpaceBetween
            ) {

                combination
                    .numbers
                    .forEach {

                        Loto7NumberBubble(
                            it
                        )
                    }
            }
        }
    }
}


@Composable
fun Loto7NumberBubble(
    number: Int,
    prominent: Boolean = false
) {

    Surface(
        modifier =
            Modifier.size(
                if (prominent)
                    43.dp
                else
                    40.dp
            ),

        shape =
            RoundedCornerShape(50),

        color =
            if (prominent)
                MaterialTheme
                    .colorScheme
                    .primary
            else
                MaterialTheme
                    .colorScheme
                    .surfaceVariant,

        contentColor =
            if (prominent)
                MaterialTheme
                    .colorScheme
                    .onPrimary
            else
                MaterialTheme
                    .colorScheme
                    .onSurfaceVariant,

        tonalElevation = 2.dp
    ) {

        Box(
            contentAlignment =
                Alignment.Center
        ) {

            Text(
                number
                    .toString()
                    .padStart(
                        2,
                        '0'
                    ),

                fontWeight =
                    FontWeight.Bold
            )
        }
    }
}


/*
 * ============================================================
 * LOTO 7 HISTORY
 * ============================================================
 *
 * Previous-10 appearance rule:
 *
 * For draw N:
 *
 *     N-11 through N-2
 *
 * Example:
 *
 * Draw 2144
 * analyzes:
 *
 * 2133, 2134, ... 2142
 *
 * Draw 2143 is intentionally excluded.
 */

@Composable
fun Loto7HistoryScreen(
    history: List<Loto7Draw>
) {

    var expandedDrawNumber by remember {
        mutableStateOf<Int?>(null)
    }

    LazyColumn(
        modifier =
            Modifier.fillMaxSize(),

        contentPadding =
            androidx.compose.foundation.layout.PaddingValues(
                horizontal = 12.dp,
                vertical = 8.dp
            ),

        verticalArrangement =
            Arrangement.spacedBy(4.dp)
    ) {

        item {

            Surface(
                modifier = Modifier.fillMaxWidth(),

                color =
                    MaterialTheme
                        .colorScheme
                        .surfaceVariant,

                shape =
                    RoundedCornerShape(8.dp)
            ) {

                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = 8.dp,
                                vertical = 7.dp
                            ),

                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Text(
                        "DRAW",
                        modifier =
                            Modifier.weight(0.85f),

                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,

                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurfaceVariant
                    )

                    Text(
                        "DATE",
                        modifier =
                            Modifier.weight(1.05f),

                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,

                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurfaceVariant
                    )

                    Text(
                        "WINNING NUMBERS",
                        modifier =
                            Modifier.weight(2.7f),

                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,

                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurfaceVariant
                    )

                    Text(
                        "BONUS",
                        modifier =
                            Modifier.weight(1.15f),

                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,

                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurfaceVariant,

                        textAlign =
                            TextAlign.End
                    )
                }
            }
        }


        items(
            history,
            key = {
                it.drawNumber
            }
        ) { draw ->

            val isLatest =
                draw == history.firstOrNull()

            val isExpanded =
                expandedDrawNumber ==
                        draw.drawNumber


            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(
                            RoundedCornerShape(8.dp)
                        )
                        .background(
                            if (isLatest) {
                                MaterialTheme
                                    .colorScheme
                                    .primaryContainer
                            } else {
                                MaterialTheme
                                    .colorScheme
                                    .surface
                            }
                        )
                        .clickable {

                            expandedDrawNumber =
                                if (isExpanded) {
                                    null
                                } else {
                                    draw.drawNumber
                                }
                        }
                        .padding(
                            horizontal = 8.dp,
                            vertical = 7.dp
                        )
            ) {

                Row(
                    modifier =
                        Modifier.fillMaxWidth(),

                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Text(
                        text =
                            draw.drawNumber
                                .toString(),

                        modifier =
                            Modifier.weight(0.85f),

                        fontSize = 11.sp,

                        fontWeight =
                            if (isLatest)
                                FontWeight.Bold
                            else
                                FontWeight.Medium,

                        color =
                            if (isLatest)
                                MaterialTheme
                                    .colorScheme
                                    .primary
                            else
                                MaterialTheme
                                    .colorScheme
                                    .onSurface
                    )


                    Text(
                        text = draw.date,

                        modifier =
                            Modifier.weight(1.05f),

                        fontSize = 9.sp,

                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurfaceVariant
                    )


                    Row(
                        modifier =
                            Modifier.weight(2.7f),

                        horizontalArrangement =
                            Arrangement.spacedBy(3.dp),

                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        draw.numbers
                            .take(7)
                            .forEach { number ->

                                Loto7HistoryNumber(
                                    number = number,

                                    latest =
                                        isLatest,

                                    modifier =
                                        Modifier.size(22.dp)
                                )
                            }
                    }


                    Text(
                        text =
                            draw.bonusNumbers
                                ?.joinToString(" ") {
                                    it.toString()
                                        .padStart(2, '0')
                                }
                                ?: "—",

                        modifier =
                            Modifier.weight(1.15f),

                        fontSize = 9.sp,

                        fontWeight =
                            FontWeight.Bold,

                        textAlign =
                            TextAlign.End,

                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurfaceVariant
                    )
                }


                if (isExpanded) {

                    Spacer(
                        Modifier.height(8.dp)
                    )

                    androidx.compose.material3
                        .HorizontalDivider()

                    Spacer(
                        Modifier.height(8.dp)
                    )


                    val oddCount =
                        draw.numbers.count {
                            it % 2 != 0
                        }

                    val evenCount =
                        draw.numbers.count {
                            it % 2 == 0
                        }


                    val lowCount =
                        draw.numbers.count {
                            it in 1..18
                        }

                    val highCount =
                        draw.numbers.count {
                            it in 19..37
                        }


                    Row(
                        modifier =
                            Modifier.fillMaxWidth(),

                        horizontalArrangement =
                            Arrangement.spacedBy(6.dp)
                    ) {

                        Surface(
                            modifier =
                                Modifier.weight(1f),

                            shape =
                                RoundedCornerShape(8.dp),

                            color =
                                MaterialTheme
                                    .colorScheme
                                    .surfaceVariant
                        ) {

                            Column(
                                modifier =
                                    Modifier.padding(
                                        8.dp
                                    )
                            ) {

                                Text(
                                    "Odd / Even",

                                    fontSize = 9.sp,

                                    fontWeight =
                                        FontWeight.Bold
                                )

                                Spacer(
                                    Modifier.height(3.dp)
                                )

                                Text(
                                    "$oddCount Odd  •  $evenCount Even",

                                    fontSize = 10.sp
                                )
                            }
                        }


                        Surface(
                            modifier =
                                Modifier.weight(1f),

                            shape =
                                RoundedCornerShape(8.dp),

                            color =
                                MaterialTheme
                                    .colorScheme
                                    .surfaceVariant
                        ) {

                            Column(
                                modifier =
                                    Modifier.padding(
                                        8.dp
                                    )
                            ) {

                                Text(
                                    "Low / High",

                                    fontSize = 9.sp,

                                    fontWeight =
                                        FontWeight.Bold
                                )

                                Spacer(
                                    Modifier.height(3.dp)
                                )

                                Text(
                                    "$lowCount Low  •  $highCount High",

                                    fontSize = 10.sp
                                )
                            }
                        }
                    }


                    Spacer(
                        Modifier.height(10.dp)
                    )


                    Text(
                        "Prize Details",

                        fontSize = 11.sp,

                        fontWeight =
                            FontWeight.Bold
                    )

                    Spacer(
                        Modifier.height(5.dp)
                    )

                    if (draw.prizes.isEmpty()) {

                        Text(
                            "Prize information unavailable.",

                            fontSize = 9.sp,

                            color =
                                MaterialTheme
                                    .colorScheme
                                    .onSurfaceVariant
                        )

                    } else {

                        draw.prizes
                            .sortedBy {
                                it.division
                            }
                            .forEach { prize ->

                                Loto7PrizeRow(
                                    prize
                                )
                            }
                    }


                    Spacer(
                        Modifier.height(10.dp)
                    )


                                                val historicalMethodMatches = HistoricalMethodClassifier.classify(
                                numbers = draw.numbers,
                                previousDraws = history.filter { it.drawNumber in (draw.drawNumber - 11)..(draw.drawNumber - 2) }.map { it.numbers },
                                maxNumber = 37
                            )

                            Text(
                                "Method Match",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                if (historicalMethodMatches.isEmpty()) "No strong method match"
                                else historicalMethodMatches.joinToString(" • "),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "Based only on draws before this result.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(12.dp))

Text(
                        "Numbers Appeared in Previous 10 Draws",

                        fontSize = 11.sp,

                        fontWeight =
                            FontWeight.Bold
                    )

                    Text(
                        "Draws #${draw.drawNumber - 11}–#${draw.drawNumber - 2}",

                        style =
                            MaterialTheme
                                .typography
                                .labelSmall,

                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurfaceVariant
                    )


                    Spacer(
                        Modifier.height(5.dp)
                    )


                    val appearanceDraws =
                        history.filter {

                            it.drawNumber in
                                    (draw.drawNumber - 11)..
                                    (draw.drawNumber - 2)
                        }


                    val previous10Numbers =
                        appearanceDraws
                            .flatMap {
                                it.numbers
                            }
                            .toSet()


                    val matchedNumbers =
                        draw.numbers.filter {
                            it in previous10Numbers
                        }


                    if (matchedNumbers.isEmpty()) {

                        Text(
                            "None",

                            fontSize = 10.sp,

                            color =
                                MaterialTheme
                                    .colorScheme
                                    .onSurfaceVariant
                        )

                    } else {

                        Row(
                            modifier =
                                Modifier.fillMaxWidth(),

                            horizontalArrangement =
                                Arrangement.spacedBy(5.dp),

                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {

                            matchedNumbers
                                .forEach { number ->

                                    Loto7HistoryNumber(
                                        number = number,

                                        latest = false,

                                        modifier =
                                            Modifier.size(
                                                26.dp
                                            )
                                    )
                                }
                        }
                    }
                }
            }
        }
    }
}


@Composable
private fun Loto7HistoryNumber(
    number: Int,
    latest: Boolean,
    modifier: Modifier = Modifier
) {

    Box(
        modifier =
            modifier
                .aspectRatio(1f)
                .background(
                    color =
                        if (latest) {

                            MaterialTheme
                                .colorScheme
                                .primary

                        } else {

                            MaterialTheme
                                .colorScheme
                                .surfaceVariant
                        },

                    shape =
                        CircleShape
                ),

        contentAlignment =
            Alignment.Center
    ) {

        Text(
            text =
                number
                    .toString()
                    .padStart(
                        2,
                        '0'
                    ),

            color =
                if (latest) {

                    MaterialTheme
                        .colorScheme
                        .onPrimary

                } else {

                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant
                },

            fontSize = 8.sp,

            fontWeight =
                FontWeight.Bold,

            textAlign =
                TextAlign.Center,

            maxLines = 1
        )
    }
}


@Composable
private fun Loto7PreviousNumberBall(
    number: Int
) {

    Box(
        modifier =
            Modifier
                .size(30.dp)
                .background(
                    color =
                        MaterialTheme
                            .colorScheme
                            .primary,

                    shape =
                        CircleShape
                ),

        contentAlignment =
            Alignment.Center
    ) {

        Text(
            text =
                number
                    .toString()
                    .padStart(
                        2,
                        '0'
                    ),

            color =
                MaterialTheme
                    .colorScheme
                    .onPrimary,

            fontSize = 9.sp,

            fontWeight =
                FontWeight.Bold,

            textAlign =
                TextAlign.Center
        )
    }
}


@Composable
fun Loto7PrizeRow(
    prize: Loto7Prize
) {

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    vertical = 2.dp
                ),

        verticalAlignment =
            Alignment.CenterVertically
    ) {

        Text(
            text = "Division ${prize.division}",

            modifier =
                Modifier.weight(1.4f),

            fontSize = 10.sp,

            fontWeight =
                FontWeight.Medium,

            maxLines = 1
        )


        Text(
            text =
                if (prize.winners != null) {
                    prize.winners.toString()
                } else {
                    "—"
                },

            modifier =
                Modifier.weight(0.8f),

            fontSize = 10.sp,

            textAlign =
                TextAlign.Center,

            color =
                MaterialTheme
                    .colorScheme
                    .onSurfaceVariant,

            maxLines = 1
        )


        Text(
            text =
                formatLoto7Yen(
                    prize.prizeAmount
                ),

            modifier =
                Modifier.weight(1.4f),

            fontSize = 10.sp,

            fontWeight =
                FontWeight.Bold,

            textAlign =
                TextAlign.End,

            maxLines = 1
        )
    }
}


fun formatLoto7Yen(
    amount: Long?
): String {

    return if (amount == null) {

        "—"

    } else {

        "¥%,d".format(
            amount
        )
    }
}


@Composable
fun Loto7FrequencyScreen(
    history: List<Loto7Draw>
) {

    val analysisDraws =
        history
            .drop(1)
            .take(10)


    val frequency =
        (1..37)
            .associateWith {
                    number ->

                analysisDraws.count {
                    number in it.numbers
                }
            }


    val sortedFrequency =
        frequency.entries
            .sortedWith(
                compareByDescending<
                        Map.Entry<Int, Int>
                        > {
                    it.value
                }.thenBy {
                    it.key
                }
            )


    val maxFrequency =
        sortedFrequency
            .firstOrNull()
            ?.value
            ?.coerceAtLeast(1)
            ?: 1


    LazyColumn(
        modifier =
            Modifier.fillMaxSize(),

        contentPadding =
            androidx.compose.foundation.layout.PaddingValues(
                16.dp
            ),

        verticalArrangement =
            Arrangement.spacedBy(8.dp)
    ) {

        item {

            Loto7SectionHeader(
                "Frequency analysis",
                "Previous 10 draws • latest result excluded"
            )
        }


        items(sortedFrequency) {
                entry ->

            Loto7FrequencyRow(
                entry.key,
                entry.value,
                maxFrequency
            )
        }
    }
}


@Composable
fun Loto7FrequencyRow(
    number: Int,
    frequency: Int,
    maxFrequency: Int
) {

    val percentage =
        if (maxFrequency == 0)
            0f
        else
            frequency.toFloat() /
                    maxFrequency.toFloat()


    Card(
        modifier =
            Modifier.fillMaxWidth(),

        shape =
            RoundedCornerShape(14.dp)
    ) {

        Column(
            Modifier.padding(
                horizontal = 14.dp,
                vertical = 10.dp
            )
        ) {

            Row(
                Modifier.fillMaxWidth(),

                horizontalArrangement =
                    Arrangement.SpaceBetween
            ) {

                Row(
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Loto7NumberBubble(
                        number
                    )


                    Spacer(
                        Modifier.size(10.dp)
                    )


                    Text(
                        "Number ${
                            number
                                .toString()
                                .padStart(
                                    2,
                                    '0'
                                )
                        }",

                        fontWeight =
                            FontWeight.SemiBold
                    )
                }


                Text(
                    "$frequency / 10",

                    fontWeight =
                        FontWeight.Bold
                )
            }


            Spacer(
                Modifier.height(8.dp)
            )


            androidx.compose.material3
                .LinearProgressIndicator(
                    progress = {
                        percentage
                            .coerceIn(
                                0f,
                                1f
                            )
                    },

                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(7.dp)
                            .clip(
                                RoundedCornerShape(
                                    50
                                )
                            )
                )
        }
    }
}