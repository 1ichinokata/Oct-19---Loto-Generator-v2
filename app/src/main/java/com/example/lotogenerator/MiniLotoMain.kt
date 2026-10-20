package com.example.lotogenerator

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
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


// ============================================================
// MINI LOTO FILTERS
// ============================================================

enum class MiniLotoOddEvenFilter(
    val label: String,
    val oddCount: Int?
) {
    ANY("Any", null),
    TWO_THREE("2 Odd / 3 Even", 2),
    THREE_TWO("3 Odd / 2 Even", 3),
    ONE_FOUR("1 Odd / 4 Even", 1),
    FOUR_ONE("4 Odd / 1 Even", 4),
    ZERO_FIVE("0 Odd / 5 Even", 0),
    FIVE_ZERO("5 Odd / 0 Even", 5)
}


enum class MiniLotoLowHighFilter(
    val label: String,
    val lowCount: Int?
) {
    ANY("Any", null),
    TWO_THREE("2 Low / 3 High", 2),
    THREE_TWO("3 Low / 2 High", 3),
    ONE_FOUR("1 Low / 4 High", 1),
    FOUR_ONE("4 Low / 1 High", 4),
    ZERO_FIVE("0 Low / 5 High", 0),
    FIVE_ZERO("5 Low / 0 High", 5)
}


enum class MiniLotoMethod(
    val label: String
) {
    RANDOM("Random"),
    BALANCED("Balanced"),
    FREQUENCY("Frequency"),
    HOT("Hot"),
    COLD("Cold")
}


data class MiniLotoCombination(
    val numbers: List<Int>,
    val score: Int,
    val method: MiniLotoMethod
)


// ============================================================
// MINI LOTO ENGINE
// ============================================================

class MiniLotoEngine {

    fun generateRandom(
        pool: List<Int>,
        oddEven: MiniLotoOddEvenFilter,
        lowHigh: MiniLotoLowHighFilter
    ): List<Int> {

        var attempts = 0

        while (attempts < 5000) {

            attempts++

            val numbers =
                pool.shuffled()
                    .take(5)
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
            .take(5)
            .sorted()
    }


    fun generateBalanced(
        pool: List<Int>,
        oddEven: MiniLotoOddEvenFilter,
        lowHigh: MiniLotoLowHighFilter
    ): List<Int> {

        var attempts = 0

        while (attempts < 5000) {

            attempts++

            val numbers =
                pool.shuffled()
                    .take(5)
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
        oddEven: MiniLotoOddEvenFilter,
        lowHigh: MiniLotoLowHighFilter
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
                (count + 1).coerceAtMost(20)

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
                    .take(5)
                    .sorted()

            if (numbers.size == 5) {

                if (
                    matchesOddEven(numbers, oddEven) &&
                    matchesLowHigh(numbers, lowHigh)
                ) {
                    return numbers
                }
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
        oddEven: MiniLotoOddEvenFilter,
        lowHigh: MiniLotoLowHighFilter
    ): List<Int> {

        val sorted =
            frequency.entries
                .filter {
                    it.key in pool
                }
                .sortedWith(
                    Comparator { first, second ->

                        if (first.value != second.value) {
                            second.value.compareTo(
                                first.value
                            )
                        } else {
                            first.key.compareTo(
                                second.key
                            )
                        }
                    }
                )

        val hotNumbers =
            ArrayList<Int>()

        val count =
            minOf(
                5,
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

                if (selected.size == 5) {
                    break
                }
            }

            val numbers =
                selected
                    .take(5)
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
        oddEven: MiniLotoOddEvenFilter,
        lowHigh: MiniLotoLowHighFilter
    ): List<Int> {

        val sorted =
            frequency.entries
                .filter {
                    it.key in pool
                }
                .sortedWith(
                    Comparator { first, second ->

                        if (first.value != second.value) {
                            first.value.compareTo(
                                second.value
                            )
                        } else {
                            first.key.compareTo(
                                second.key
                            )
                        }
                    }
                )

        val coldNumbers =
            ArrayList<Int>()

        val count =
            minOf(
                5,
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

                if (selected.size == 5) {
                    break
                }
            }

            val numbers =
                selected
                    .take(5)
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
        filter: MiniLotoOddEvenFilter
    ): Boolean {

        if (
            filter ==
            MiniLotoOddEvenFilter.ANY
        ) {
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
            5 - requiredOdd

        return oddCount == requiredOdd &&
                evenCount == requiredEven
    }


    private fun matchesLowHigh(
        numbers: List<Int>,
        filter: MiniLotoLowHighFilter
    ): Boolean {

        if (
            filter ==
            MiniLotoLowHighFilter.ANY
        ) {
            return true
        }

        val lowCount =
            numbers.count {
                it in 1..15
            }

        val highCount =
            numbers.count {
                it in 16..31
            }

        val requiredLow =
            filter.lowCount
                ?: return true

        val requiredHigh =
            5 - requiredLow

        return lowCount == requiredLow &&
                highCount == requiredHigh
    }


    private fun hasGoodBalance(
        numbers: List<Int>
    ): Boolean {

        if (numbers.size != 5) {
            return false
        }

        val odd =
            numbers.count {
                it % 2 != 0
            }

        val low =
            numbers.count {
                it in 1..15
            }

        val spread =
            numbers.last() -
                    numbers.first()

        return odd in 2..3 &&
                low in 2..3 &&
                spread >= 15
    }
}


// ============================================================
// MINI LOTO VIEW MODEL
// ============================================================

class MiniLotoViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val engine =
        MiniLotoEngine()


    private val _history =
        MutableStateFlow<List<MiniLotoDraw>>(
            emptyList()
        )

    val history:
            StateFlow<List<MiniLotoDraw>> =
        _history


    private val _latestDraw =
        MutableStateFlow<MiniLotoDraw?>(null)

    val latestDraw:
            StateFlow<MiniLotoDraw?> =
        _latestDraw


    private val _combinations =
        MutableStateFlow<List<MiniLotoCombination>>(
            emptyList()
        )

    val combinations:
            StateFlow<List<MiniLotoCombination>> =
        _combinations


    private val _oddEvenFilter =
        MutableStateFlow(
            MiniLotoOddEvenFilter.ANY
        )

    val oddEvenFilter:
            StateFlow<MiniLotoOddEvenFilter> =
        _oddEvenFilter


    private val _lowHighFilter =
        MutableStateFlow(
            MiniLotoLowHighFilter.ANY
        )

    val lowHighFilter:
            StateFlow<MiniLotoLowHighFilter> =
        _lowHighFilter


    private val _method =
        MutableStateFlow(
            MiniLotoMethod.BALANCED
        )

    val method:
            StateFlow<MiniLotoMethod> =
        _method


    private val _status =
        MutableStateFlow("Ready")

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
            favoriteStore.load("miniloto")
        )

    val favorites:
            StateFlow<List<FavoriteItem>> =
        _favorites


    // --------------------------------------------------------
    // FAVORITES
    // --------------------------------------------------------

    fun toggleFavorite(
        combination: MiniLotoCombination
    ) {

        val current =
            _favorites.value

        val exists =
            current.any {
                it.numbers == combination.numbers
            }

        if (exists) {

            favoriteStore.remove(
                "miniloto",
                combination.numbers
            )

        } else {

            favoriteStore.save(
                "miniloto",
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
            favoriteStore.load("miniloto")
    }


    fun addManualFavorite(
        numbers: List<Int>
    ) {

        val sortedNumbers =
            numbers
                .distinct()
                .sorted()

        if (
            sortedNumbers.size != 5 ||
            sortedNumbers.any {
                it !in 1..31
            }
        ) {
            return
        }

        val targetDraw =
            _latestDraw.value
                ?.drawNumber
                ?.plus(1)

        favoriteStore.save(
            "miniloto",
            FavoriteItem(
                numbers =
                    sortedNumbers,

                score =
                    0,

                method =
                    "Manual",

                savedAt =
                    System.currentTimeMillis(),

                targetDrawNumber =
                    targetDraw
            )
        )

        _favorites.value =
            favoriteStore.load("miniloto")
    }


    // --------------------------------------------------------
    // INITIAL LOAD
    // --------------------------------------------------------

    init {
        updateFromLottolyzer()
    }


    // --------------------------------------------------------
    // FILTER SETTINGS
    // --------------------------------------------------------

    fun setOddEvenFilter(
        value: MiniLotoOddEvenFilter
    ) {
        _oddEvenFilter.value =
            value
    }


    fun setLowHighFilter(
        value: MiniLotoLowHighFilter
    ) {
        _lowHighFilter.value =
            value
    }


    fun setMethod(
        value: MiniLotoMethod
    ) {
        _method.value =
            value
    }


    // --------------------------------------------------------
    // CLEAR
    // --------------------------------------------------------

    fun clearCombinations() {

        _combinations.value =
            emptyList()

        _status.value =
            "Generated combinations cleared"
    }


    // --------------------------------------------------------
    // ANALYSIS DRAWS
    // --------------------------------------------------------

    private fun getAnalysisDraws():
            List<MiniLotoDraw> {

        val source =
            _history.value

        val result =
            ArrayList<MiniLotoDraw>()

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


    // --------------------------------------------------------
    // FREQUENCY
    // --------------------------------------------------------

    private fun buildFrequency(
        draws: List<MiniLotoDraw>
    ): Map<Int, Int> {

        val frequency =
            HashMap<Int, Int>()

        for (number in 1..31) {
            frequency[number] = 0
        }

        for (draw in draws) {

            for (number in draw.numbers) {

                val old =
                    frequency[number] ?: 0

                frequency[number] =
                    old + 1
            }
        }

        return frequency
    }


    // --------------------------------------------------------
    // GENERATE
    // --------------------------------------------------------

    fun generate() {

        val analysisDraws =
            getAnalysisDraws()


        if (analysisDraws.isEmpty()) {

            _combinations.value =
                emptyList()

            _status.value =
                "Please wait for Mini Loto results to load."

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


        if (generationPool.size < 5) {

            _combinations.value =
                emptyList()

            _status.value =
                "Not enough numbers in the previous 10 draws to generate combinations."

            return
        }


        val previousWinningCombinations =
            _history.value
                .map {
                    it.numbers.sorted()
                }
                .toSet()


        val selectedMethod =
            _method.value

        val oddEven =
            _oddEvenFilter.value

        val lowHigh =
            _lowHighFilter.value


        val result =
            ArrayList<MiniLotoCombination>()


        var attempts = 0


        while (
            result.size < 10 &&
            attempts < 10000
        ) {

            attempts++


            val numbers =
                when (selectedMethod) {

                    MiniLotoMethod.RANDOM ->
                        engine.generateRandom(
                            generationPool,
                            oddEven,
                            lowHigh
                        )

                    MiniLotoMethod.BALANCED ->
                        engine.generateBalanced(
                            generationPool,
                            oddEven,
                            lowHigh
                        )

                    MiniLotoMethod.FREQUENCY ->
                        engine.generateFrequency(
                            generationPool,
                            frequency,
                            oddEven,
                            lowHigh
                        )

                    MiniLotoMethod.HOT ->
                        engine.generateHot(
                            generationPool,
                            frequency,
                            oddEven,
                            lowHigh
                        )

                    MiniLotoMethod.COLD ->
                        engine.generateCold(
                            generationPool,
                            frequency,
                            oddEven,
                            lowHigh
                        )
                }


            val validSize =
                numbers.size == 5

            val validUnique =
                numbers.distinct().size == 5

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
                    it in 1..15
                }

            val validLowHigh =
                requiredLow == null ||
                        actualLow == requiredLow

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
                validOddEven &&
                validLowHigh &&
                !duplicate
            ) {

                val score =
                    calculateScore(
                        numbers,
                        frequency
                    )

                result.add(
                    MiniLotoCombination(
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


    // --------------------------------------------------------
    // SCORE
    // --------------------------------------------------------

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
                it in 1..15
            }

        if (odd in 2..3) {
            score += 10
        }

        if (low in 2..3) {
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

        if (spread >= 15) {
            score += 10
        }

        return score.coerceIn(
            0,
            100
        )
    }


    // --------------------------------------------------------
    // LOTTOLYZER UPDATE
    // --------------------------------------------------------

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
                    MiniLotoLottolyzerApi(
                        getApplication<Application>()
                    )

                val draws =
                    api.fetchRecentDraws(30)

                if (draws.isEmpty()) {

                    _status.value =
                        "Lottolyzer page loaded, but no Mini Loto results could be read."

                    return@launch
                }

                val sorted =
                    draws.sortedByDescending {
                        it.drawNumber
                    }

                val latest =
                    sorted.first()

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


// ============================================================
// MINI LOTO APP
// ============================================================

@Composable
fun MiniLotoApp() {

    val vm:
            MiniLotoViewModel =
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
            if (darkMode) {
                darkScheme
            } else {
                lightScheme
            }
    ) {

        Surface(
            modifier =
                Modifier.fillMaxSize(),

            color =
                MaterialTheme
                    .colorScheme
                    .background
        ) {

            MiniLotoScaffold(

                darkMode =
                    darkMode,

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

                // FIX:
                // Connect manual favorites to ViewModel.
                onAddManualFavorite = {
                    vm.addManualFavorite(it)
                }
            )
        }
    }
}


// ============================================================
// MAIN SCAFFOLD
// ============================================================

@Composable
fun MiniLotoScaffold(
    darkMode: Boolean,
    onDarkModeChange: (Boolean) -> Unit,
    latestDraw: MiniLotoDraw?,
    history: List<MiniLotoDraw>,
    combinations: List<MiniLotoCombination>,
    oddEvenFilter: MiniLotoOddEvenFilter,
    lowHighFilter: MiniLotoLowHighFilter,
    method: MiniLotoMethod,
    status: String,
    isUpdating: Boolean,
    favorites: List<FavoriteItem>,
    onUpdate: () -> Unit,
    onGenerate: () -> Unit,
    onClear: () -> Unit,
    onOddEvenChange:
        (MiniLotoOddEvenFilter) -> Unit,
    onLowHighChange:
        (MiniLotoLowHighFilter) -> Unit,
    onMethodChange:
        (MiniLotoMethod) -> Unit,
    onToggleFavorite:
        (MiniLotoCombination) -> Unit,

    // FIX:
    onAddManualFavorite:
        (List<Int>) -> Unit
) {

    var selectedTab by
    remember {
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

                tonalElevation =
                    3.dp,

                shadowElevation =
                    1.dp
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
                            selectedTab == index,

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

            MiniLotoLatestResultCard(
                draw =
                    latestDraw,

                isUpdating =
                    isUpdating,

                onUpdate =
                    onUpdate
            )


            when (selectedTab) {

                0 -> MiniLotoGeneratorScreen(
                    history = history,

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
                        onToggleFavorite,

                    // FIX:
                    onAddManualFavorite =
                        onAddManualFavorite
                )


                1 -> MiniLotoHistoryScreen(
                    history =
                        history
                )


                2 -> MiniLotoFrequencyScreen(
                    history =
                        history
                )


                3 -> FavoriteListScreen(

                    lotteryName =
                        "Mini Loto",

                    favorites =
                        favorites,

                    latestDrawNumbers =
                        latestDraw
                            ?.numbers
                            ?.toSet()
                            ?: emptySet(),

                    // FIX:
                    latestDrawNumber =
                        latestDraw?.drawNumber ?: 0,

                    onRemove = { item ->

                        onToggleFavorite(

                            MiniLotoCombination(

                                numbers =
                                    item.numbers,

                                score =
                                    item.score,

                                method =
                                    MiniLotoMethod.entries
                                        .firstOrNull {
                                            it.label ==
                                                    item.method
                                        }
                                        ?: MiniLotoMethod.BALANCED
                            )
                        )
                    },

                    // FIX:
                    onAddManual = { numbers ->
                        onAddManualFavorite(numbers)
                    },
                    winningAmountFor = { item ->
                        val targetDraw = history.firstOrNull { draw -> draw.drawNumber == item.targetDrawNumber }
                        if (targetDraw == null) null else {
                            val matches = item.numbers.count { number -> targetDraw.numbers.contains(number) }
                            val bonusHit = targetDraw.bonus != null && item.numbers.contains(targetDraw.bonus)
                            val division = if (matches == 5) 1 else if (matches == 4 && bonusHit) 2 else if (matches == 4) 3 else if (matches == 3) 4 else 0
                            if (division == 0) null else targetDraw.prizes.firstOrNull { prize -> prize.division == division }?.prizeAmount
                        }
                    }
                )
            }
        }
    }
}


// ============================================================
// LATEST RESULT
// ============================================================

@Composable
fun MiniLotoLatestResultCard(
    draw: MiniLotoDraw?,
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
            CardDefaults.cardColors(
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
                            contentDescription =
                                null,

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
                                "Draw ${it.drawNumber}"
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

                            strokeWidth =
                                2.dp
                        )

                } else {

                    IconButton(
                        onClick =
                            onUpdate
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
                    "Loading Mini Loto results…"
                )

            } else {

                Row(

                    Modifier.fillMaxWidth(),

                    horizontalArrangement =
                        Arrangement.SpaceBetween
                ) {

                    draw.numbers.forEach {
                        MiniLotoNumberBubble(
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


                    if (draw.bonus != null) {

                        Spacer(
                            Modifier.weight(1f)
                        )


                        Text(

                            "Bonus ${
                                draw.bonus
                                    .toString()
                                    .padStart(2, '0')
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


// ============================================================
// GENERATOR SCREEN
// ============================================================

@Composable
fun MiniLotoGeneratorScreen(
    history: List<MiniLotoDraw>,
    combinations: List<MiniLotoCombination>,
    oddEvenFilter: MiniLotoOddEvenFilter,
    lowHighFilter: MiniLotoLowHighFilter,
    method: MiniLotoMethod,
    status: String,
    onGenerate: () -> Unit,
    onClear: () -> Unit,
    favorites: List<FavoriteItem> =
        emptyList(),
    onToggleFavorite:
        (MiniLotoCombination) -> Unit =
        {},
    onOddEvenChange:
        (MiniLotoOddEvenFilter) -> Unit,
    onLowHighChange:
        (MiniLotoLowHighFilter) -> Unit,
    onMethodChange:
        (MiniLotoMethod) -> Unit,
    onAddManualFavorite:
        (List<Int>) -> Unit
) {

    var showGenerateConfirmation by
    remember {
        mutableStateOf(false)
    }


    var showClearConfirmation by
    remember {
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
                        "Generate 10 new Mini Loto combinations using these settings?"
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


                    if (combinations.isNotEmpty()) {

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
                        contentDescription =
                            null
                    )

                    Spacer(
                        Modifier.size(6.dp)
                    )

                    Text(
                        "Generate"
                    )
                }
            },


            dismissButton = {

                TextButton(

                    onClick = {

                        showGenerateConfirmation =
                            false
                    }
                ) {

                    Text(
                        "Cancel"
                    )
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
                    "Are you sure you want to clear all generated Mini Loto combinations?"
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

                    Text(
                        "Clear"
                    )
                }
            },


            dismissButton = {

                TextButton(

                    onClick = {

                        showClearConfirmation =
                            false
                    }
                ) {

                    Text(
                        "Cancel"
                    )
                }
            }
        )
    }


    LazyColumn(

        modifier =
            Modifier.fillMaxSize(),

        contentPadding =
            PaddingValues(
                horizontal = 16.dp,
                vertical = 8.dp
            ),

        verticalArrangement =
            Arrangement.spacedBy(10.dp)
    ) {

        item {

            MiniLotoSectionHeader(

                title =
                    "Generate combinations",

                subtitle =
                    "Numbers are selected only from the previous 10 draws, excluding the latest draw numbers."
            )
        }


        item {
            val prediction = NextDrawPatternPredictor.predict(
                draws = history.map { it.numbers },
                numberCount = 5,
                lowBoundary = 15
            )
            NextDrawPatternCard(prediction)
        }

        item {

            MiniLotoFilterCard(

                oddEvenFilter =
                    oddEvenFilter,

                lowHighFilter =
                    lowHighFilter,

                method =
                    method,

                onOddEvenChange =
                    onOddEvenChange,

                onLowHighChange =
                    onLowHighChange,

                onMethodChange =
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
                        contentDescription =
                            null
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


                OutlinedButton(

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
                        RoundedCornerShape(16.dp)
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

            MiniLotoStatusCard(
                status
            )
        }


        items(combinations) {
                combination ->

            MiniLotoCombinationCard(

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


// ============================================================
// SECTION HEADER
// ============================================================

@Composable
fun MiniLotoSectionHeader(
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


// ============================================================
// STATUS
// ============================================================

@Composable
fun MiniLotoStatusCard(
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
                contentDescription =
                    null,

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


// ============================================================
// FILTER CARD
// ============================================================

@Composable
fun MiniLotoFilterCard(

    oddEvenFilter:
    MiniLotoOddEvenFilter,

    lowHighFilter:
    MiniLotoLowHighFilter,

    method:
    MiniLotoMethod,

    onOddEvenChange:
        (MiniLotoOddEvenFilter) -> Unit,

    onLowHighChange:
        (MiniLotoLowHighFilter) -> Unit,

    onMethodChange:
        (MiniLotoMethod) -> Unit
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
                    contentDescription =
                        null,

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


            MiniLotoSimpleDropdown(

                label =
                    "Odd / Even",

                selected =
                    oddEvenFilter.label,

                options =
                    MiniLotoOddEvenFilter.entries
                        .map {
                            it.label
                        },

                onSelected = { selected ->

                    MiniLotoOddEvenFilter
                        .entries
                        .firstOrNull {
                            it.label ==
                                    selected
                        }
                        ?.let(
                            onOddEvenChange
                        )
                }
            )


            MiniLotoSimpleDropdown(

                label =
                    "Low / High",

                selected =
                    lowHighFilter.label,

                options =
                    MiniLotoLowHighFilter.entries
                        .map {
                            it.label
                        },

                onSelected = { selected ->

                    MiniLotoLowHighFilter
                        .entries
                        .firstOrNull {
                            it.label ==
                                    selected
                        }
                        ?.let(
                            onLowHighChange
                        )
                }
            )


            MiniLotoSimpleDropdown(

                label =
                    "Strategy",

                selected =
                    method.label,

                options =
                    MiniLotoMethod.entries
                        .map {
                            it.label
                        },

                onSelected = { selected ->

                    MiniLotoMethod
                        .entries
                        .firstOrNull {
                            it.label ==
                                    selected
                        }
                        ?.let(
                            onMethodChange
                        )
                }
            )
        }
    }
}


// ============================================================
// DROPDOWN
// ============================================================

@Composable
fun MiniLotoSimpleDropdown(

    label: String,

    selected: String,

    options: List<String>,

    onSelected:
        (String) -> Unit
) {

    var expanded by
    remember {
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

            OutlinedButton(

                onClick = {
                    expanded = true
                },

                modifier =
                    Modifier.fillMaxWidth(),

                shape =
                    RoundedCornerShape(13.dp)
            ) {

                Text(
                    selected,

                    Modifier.weight(1f)
                )


                Icon(
                    Icons.Default.ExpandMore,
                    contentDescription =
                        null
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


// ============================================================
// COMBINATION CARD
// ============================================================

@Composable
fun MiniLotoCombinationCard(

    combination:
    MiniLotoCombination,

    isFavorite:
    Boolean = false,

    onFavorite:
        () -> Unit = {}
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

                            if (isFavorite) {
                                Icons.Default.Favorite
                            } else {
                                Icons.Default.FavoriteBorder
                            },

                            contentDescription =
                                if (isFavorite) {
                                    "Remove from favorites"
                                } else {
                                    "Save to favorites"
                                }
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

                                    "Mini Loto",

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

                combination.numbers.forEach {
                    MiniLotoNumberBubble(it)
                }
            }
        }
    }
}


// ============================================================
// NUMBER BUBBLE
// ============================================================

@Composable
fun MiniLotoNumberBubble(

    number: Int,

    prominent: Boolean = false
) {

    Surface(

        modifier =
            Modifier.size(
                if (prominent) {
                    43.dp
                } else {
                    40.dp
                }
            ),

        shape =
            RoundedCornerShape(50),

        color =
            if (prominent) {
                MaterialTheme
                    .colorScheme
                    .primary
            } else {
                MaterialTheme
                    .colorScheme
                    .surfaceVariant
            },

        contentColor =
            if (prominent) {
                MaterialTheme
                    .colorScheme
                    .onPrimary
            } else {
                MaterialTheme
                    .colorScheme
                    .onSurfaceVariant
            },

        tonalElevation =
            2.dp
    ) {

        Box(
            contentAlignment =
                Alignment.Center
        ) {

            Text(

                number
                    .toString()
                    .padStart(2, '0'),

                fontWeight =
                    FontWeight.Bold
            )
        }
    }
}


// ============================================================
// HISTORY SCREEN
// ============================================================

@Composable
fun MiniLotoHistoryScreen(
    history: List<MiniLotoDraw>
) {

    var expandedDrawNumber by
    remember {
        mutableStateOf<Int?>(null)
    }


    LazyColumn(

        modifier =
            Modifier.fillMaxSize(),

        contentPadding =
            PaddingValues(
                horizontal = 16.dp,
                vertical = 8.dp
            ),

        verticalArrangement =
            Arrangement.spacedBy(4.dp)
    ) {

        item {

            MiniLotoSectionHeader(

                title =
                    "Mini Loto Draw History",

                subtitle =
                    "30 latest Mini Loto draws from Lottolyzer"
            )


            Spacer(
                Modifier.height(8.dp)
            )


            Row(

                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 4.dp,
                            vertical = 3.dp
                        ),

                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Text(
                    "DRAW",

                    modifier =
                        Modifier.weight(0.9f),

                    fontSize =
                        8.sp,

                    fontWeight =
                        FontWeight.Bold,

                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                )


                Text(
                    "DATE",

                    modifier =
                        Modifier.weight(1.25f),

                    fontSize =
                        8.sp,

                    fontWeight =
                        FontWeight.Bold,

                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                )


                Text(
                    "WINNING NUMBERS",

                    modifier =
                        Modifier.weight(3.3f),

                    fontSize =
                        8.sp,

                    fontWeight =
                        FontWeight.Bold,

                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                )


                Text(
                    "BONUS",

                    modifier =
                        Modifier.weight(0.9f),

                    fontSize =
                        8.sp,

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
        }


        items(history) { draw ->

            val expanded =
                expandedDrawNumber ==
                        draw.drawNumber


            val isLatest =
                history
                    .firstOrNull()
                    ?.drawNumber ==
                        draw.drawNumber


            val appearanceDraws =
                history.filter {

                    it.drawNumber in
                            (draw.drawNumber - 10)..
                            (draw.drawNumber - 1)
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


            Card(

                modifier =
                    Modifier.fillMaxWidth(),

                shape =
                    RoundedCornerShape(10.dp),

                colors =
                    CardDefaults.cardColors(

                        containerColor =
                            if (isLatest) {

                                MaterialTheme
                                    .colorScheme
                                    .primaryContainer

                            } else {

                                MaterialTheme
                                    .colorScheme
                                    .surface
                            }
                    ),

                onClick = {

                    expandedDrawNumber =

                        if (expanded) {
                            null
                        } else {
                            draw.drawNumber
                        }
                }
            ) {

                Column(

                    modifier =
                        Modifier
                            .fillMaxWidth()
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
                                draw.drawNumber.toString(),

                            modifier =
                                Modifier.weight(0.9f),

                            fontSize =
                                9.sp,

                            fontWeight =
                                if (isLatest) {
                                    FontWeight.Bold
                                } else {
                                    FontWeight.Medium
                                },

                            maxLines =
                                1
                        )


                        Text(

                            text =
                                draw.date,

                            modifier =
                                Modifier.weight(1.25f),

                            fontSize =
                                8.sp,

                            color =
                                MaterialTheme
                                    .colorScheme
                                    .onSurfaceVariant,

                            maxLines =
                                1
                        )


                        Row(

                            modifier =
                                Modifier.weight(3.3f),

                            horizontalArrangement =
                                Arrangement.spacedBy(3.dp)
                        ) {

                            draw.numbers.forEach {
                                    number ->

                                MiniLotoHistoryNumber(

                                    number =
                                        number,

                                    latest =
                                        isLatest,

                                    modifier =
                                        Modifier.weight(1f)
                                )
                            }
                        }


                        Box(

                            modifier =
                                Modifier
                                    .weight(0.9f)
                                    .padding(
                                        start = 4.dp
                                    ),

                            contentAlignment =
                                Alignment.CenterEnd
                        ) {

                            if (draw.bonus != null) {

                                Text(

                                    text =
                                        draw.bonus
                                            .toString()
                                            .padStart(
                                                2,
                                                '0'
                                            ),

                                    fontSize =
                                        8.sp,

                                    fontWeight =
                                        FontWeight.Bold,

                                    color =
                                        MaterialTheme
                                            .colorScheme
                                            .secondary
                                )

                            } else {

                                Text(

                                    "—",

                                    fontSize =
                                        8.sp,

                                    color =
                                        MaterialTheme
                                            .colorScheme
                                            .onSurfaceVariant
                                )
                            }
                        }
                    }


                    if (expanded) {

                        Spacer(
                            Modifier.height(6.dp)
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


                        Row(

                            modifier =
                                Modifier.fillMaxWidth(),

                            horizontalArrangement =
                                Arrangement.SpaceBetween
                        ) {

                            Text(

                                "Odd / Even",

                                fontSize =
                                    9.sp,

                                color =
                                    MaterialTheme
                                        .colorScheme
                                        .onSurfaceVariant
                            )


                            Text(

                                "$oddCount Odd / $evenCount Even",

                                fontSize =
                                    9.sp,

                                fontWeight =
                                    FontWeight.Medium
                            )
                        }


                        Spacer(
                            Modifier.height(4.dp)
                        )


                        val lowCount =
                            draw.numbers.count {
                                it in 1..15
                            }


                        val highCount =
                            draw.numbers.count {
                                it in 16..31
                            }


                        Row(

                            modifier =
                                Modifier.fillMaxWidth(),

                            horizontalArrangement =
                                Arrangement.SpaceBetween
                        ) {

                            Text(

                                "Low / High",

                                fontSize =
                                    9.sp,

                                color =
                                    MaterialTheme
                                        .colorScheme
                                        .onSurfaceVariant
                            )


                            Text(

                                "$lowCount Low / $highCount High",

                                fontSize =
                                    9.sp,

                                fontWeight =
                                    FontWeight.Medium
                            )
                        }


                        Spacer(
                            Modifier.height(9.dp)
                        )


                        Text(

                            "Prize Details",

                            fontSize =
                                11.sp,

                            fontWeight =
                                FontWeight.Bold
                        )


                        Spacer(
                            Modifier.height(4.dp)
                        )


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

                                "Division",

                                modifier =
                                    Modifier.weight(1.4f),

                                fontSize =
                                    9.sp,

                                fontWeight =
                                    FontWeight.Bold,

                                color =
                                    MaterialTheme
                                        .colorScheme
                                        .onSurfaceVariant
                            )


                            Text(

                                "Winners",

                                modifier =
                                    Modifier.weight(0.8f),

                                fontSize =
                                    9.sp,

                                fontWeight =
                                    FontWeight.Bold,

                                textAlign =
                                    TextAlign.Center,

                                color =
                                    MaterialTheme
                                        .colorScheme
                                        .onSurfaceVariant
                            )


                            Text(

                                "Amount",

                                modifier =
                                    Modifier.weight(1.4f),

                                fontSize =
                                    9.sp,

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


                        if (draw.prizes.isEmpty()) {

                            Text(

                                "Prize information unavailable.",

                                fontSize =
                                    9.sp,

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
                                .forEach {
                                        prize ->

                                    MiniLotoPrizeRow(
                                        prize
                                    )
                                }
                        }


                        Spacer(
                            Modifier.height(9.dp)
                        )


                                                    val historicalMethodMatches = HistoricalMethodClassifier.classify(
                                numbers = draw.numbers,
                                previousDraws = history.filter { it.drawNumber in (draw.drawNumber - 10)..(draw.drawNumber - 1) }.map { it.numbers },
                                maxNumber = 31
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

                            fontSize =
                                11.sp,

                            fontWeight =
                                FontWeight.Bold
                        )


                        Text(

                            "Draws #${
                                draw.drawNumber - 10
                            }–#${
                                draw.drawNumber - 1
                            }",

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
                            Modifier.height(4.dp)
                        )


                        if (matchedNumbers.isEmpty()) {

                            Text(

                                "None",

                                fontSize =
                                    9.sp,

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
                                    Arrangement.spacedBy(4.dp)
                            ) {

                                matchedNumbers.forEach {
                                        number ->

                                    MiniLotoPreviousNumberBall(
                                        number
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


// ============================================================
// HISTORY NUMBER
// ============================================================

@Composable
private fun MiniLotoHistoryNumber(

    number: Int,

    latest: Boolean,

    modifier: Modifier =
        Modifier
) {

    Box(

        modifier =
            modifier
                .size(22.dp)
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
                        RoundedCornerShape(50)
                ),

        contentAlignment =
            Alignment.Center
    ) {

        Text(

            text =
                number
                    .toString()
                    .padStart(2, '0'),

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

            fontSize =
                7.sp,

            fontWeight =
                FontWeight.Bold,

            maxLines =
                1
        )
    }
}


// ============================================================
// PREVIOUS NUMBER BALL
// ============================================================

@Composable
private fun MiniLotoPreviousNumberBall(
    number: Int
) {

    Box(

        modifier =
            Modifier
                .size(22.dp)
                .background(

                    color =
                        MaterialTheme
                            .colorScheme
                            .surfaceVariant,

                    shape =
                        RoundedCornerShape(50)
                ),

        contentAlignment =
            Alignment.Center
    ) {

        Text(

            text =
                number
                    .toString()
                    .padStart(2, '0'),

            fontSize =
                7.sp,

            fontWeight =
                FontWeight.Bold,

            color =
                MaterialTheme
                    .colorScheme
                    .onSurfaceVariant
        )
    }
}


// ============================================================
// PRIZE ROW
// ============================================================

@Composable
fun MiniLotoPrizeRow(
    prize: MiniLotoPrize
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

            text =
                "Division ${prize.division}",

            modifier =
                Modifier.weight(1.4f),

            fontSize =
                10.sp,

            fontWeight =
                FontWeight.Medium,

            maxLines =
                1
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

            fontSize =
                10.sp,

            textAlign =
                TextAlign.Center,

            color =
                MaterialTheme
                    .colorScheme
                    .onSurfaceVariant,

            maxLines =
                1
        )


        Text(

            text =
                formatMiniLotoYen(
                    prize.prizeAmount
                ),

            modifier =
                Modifier.weight(1.4f),

            fontSize =
                10.sp,

            fontWeight =
                FontWeight.Bold,

            textAlign =
                TextAlign.End,

            maxLines =
                1
        )
    }
}


// ============================================================
// YEN FORMAT
// ============================================================

fun formatMiniLotoYen(
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


// ============================================================
// FREQUENCY SCREEN
// ============================================================

@Composable
fun MiniLotoFrequencyScreen(
    history: List<MiniLotoDraw>
) {

    val analysisDraws =
        history
            .drop(1)
            .take(10)


    val frequency =
        (1..31).associateWith {
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
            PaddingValues(16.dp),

        verticalArrangement =
            Arrangement.spacedBy(8.dp)
    ) {

        item {

            MiniLotoSectionHeader(

                "Frequency analysis",

                "Previous 10 draws • latest result excluded"
            )
        }


        items(
            sortedFrequency
        ) { entry ->

            MiniLotoFrequencyRow(

                number =
                    entry.key,

                frequency =
                    entry.value,

                maxFrequency =
                    maxFrequency
            )
        }
    }
}


// ============================================================
// FREQUENCY ROW
// ============================================================

@Composable
fun MiniLotoFrequencyRow(

    number: Int,

    frequency: Int,

    maxFrequency: Int
) {

    val percentage =

        if (maxFrequency == 0) {

            0f

        } else {

            frequency.toFloat() /
                    maxFrequency.toFloat()
        }


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

                    MiniLotoNumberBubble(
                        number
                    )

                    Spacer(
                        Modifier.size(10.dp)
                    )

                    Text(

                        "Number ${
                            number
                                .toString()
                                .padStart(2, '0')
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
                        percentage.coerceIn(
                            0f,
                            1f
                        )
                    },

                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(7.dp)
                            .clip(
                                RoundedCornerShape(50)
                            )
                )
        }
    }
}