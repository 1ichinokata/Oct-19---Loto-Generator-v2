package com.example.lotogenerator

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.random.Random

enum class PhilippineLottoGame(
    val key: String,
    val title: String,
    val pcsoName: String,
    val maxNumber: Int,
    val drawDays: Set<DayOfWeek>
) {
    LOTTO_642("phil_lotto_642", "Lotto 6/42", "Lotto 6/42", 42, setOf(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY, DayOfWeek.SATURDAY)),
    MEGA_645("phil_mega_645", "Mega Lotto 6/45", "Megalotto 6/45", 45, setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)),
    SUPER_649("phil_super_649", "Super Lotto 6/49", "Superlotto 6/49", 49, setOf(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY, DayOfWeek.SUNDAY)),
    GRAND_655("phil_grand_655", "Grand Lotto 6/55", "Grand Lotto 6/55", 55, setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.SATURDAY)),
    ULTRA_658("phil_ultra_658", "Ultra Lotto 6/58", "Ultra Lotto 6/58", 58, setOf(DayOfWeek.TUESDAY, DayOfWeek.FRIDAY, DayOfWeek.SUNDAY))
}

enum class PhilippineGeneratorMethod(val label: String) {
    RANDOM("Random"),
    BALANCED("Balanced"),
    FREQUENCY("Frequency"),
    HOT("Hot"),
    COLD("Cold"),
    BALANCED_FREQUENCY("Balanced + Frequency"),
    BALANCED_HOT("Balanced + Hot"),
    BALANCED_COLD("Balanced + Cold")
}

enum class PhilippineOddEvenFilter(val label: String, val odd: Int?, val even: Int?) {
    ANY("Any", null, null), THREE_THREE("3-3", 3, 3), FOUR_TWO("4-2", 4, 2),
    TWO_FOUR("2-4", 2, 4), FIVE_ONE("5-1", 5, 1), ONE_FIVE("1-5", 1, 5),
    SIX_ZERO("6-0", 6, 0), ZERO_SIX("0-6", 0, 6)
}

enum class PhilippineLowHighFilter(val label: String, val low: Int?, val high: Int?) {
    ANY("Any", null, null), THREE_THREE("3-3", 3, 3), FOUR_TWO("4-2", 4, 2),
    TWO_FOUR("2-4", 2, 4), FIVE_ONE("5-1", 5, 1), ONE_FIVE("1-5", 1, 5),
    SIX_ZERO("6-0", 6, 0), ZERO_SIX("0-6", 0, 6)
}

data class PhilippineCombination(val numbers: List<Int>, val score: Int, val method: PhilippineGeneratorMethod)

class PhilippineLottoViewModel(application: Application) : AndroidViewModel(application) {
    private val store = FavoriteStore(application)
    private val historyCache = PhilippineLottoHistoryCache(application)
    private val _game = MutableStateFlow(PhilippineLottoGame.LOTTO_642)
    val game: StateFlow<PhilippineLottoGame> = _game
    private val _history = MutableStateFlow<List<PhilippineLottoDraw>>(emptyList())
    val history: StateFlow<List<PhilippineLottoDraw>> = _history
    private val _latest = MutableStateFlow<PhilippineLottoDraw?>(null)
    val latest: StateFlow<PhilippineLottoDraw?> = _latest
    private val _combinations = MutableStateFlow<List<PhilippineCombination>>(emptyList())
    val combinations: StateFlow<List<PhilippineCombination>> = _combinations
    private val _favorites = MutableStateFlow<List<FavoriteItem>>(emptyList())
    val favorites: StateFlow<List<FavoriteItem>> = _favorites
    private val _method = MutableStateFlow(PhilippineGeneratorMethod.BALANCED)
    val method: StateFlow<PhilippineGeneratorMethod> = _method
    private val _oddEven = MutableStateFlow(PhilippineOddEvenFilter.ANY)
    val oddEven: StateFlow<PhilippineOddEvenFilter> = _oddEven
    private val _lowHigh = MutableStateFlow(PhilippineLowHighFilter.ANY)
    val lowHigh: StateFlow<PhilippineLowHighFilter> = _lowHigh
    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error
    private val _updateAvailable = MutableStateFlow(false)
    val updateAvailable: StateFlow<Boolean> = _updateAvailable

    init {
        _favorites.value = store.load(_game.value.key)
        loadSavedHistory(_game.value)
        if (_history.value.isEmpty()) load() else checkForNewResults()
    }

    fun selectGame(value: PhilippineLottoGame) {
        _game.value = value
        _favorites.value = store.load(value.key)
        loadSavedHistory(value)
        if (_history.value.isEmpty()) load() else checkForNewResults()
    }

    private fun loadSavedHistory(game: PhilippineLottoGame) {
        val saved = historyCache.load(game)
        if (saved.isEmpty()) return

        _history.value = saved
        _latest.value = saved.firstOrNull()
        _error.value = "Saved results loaded"
    }

    fun setMethod(value: PhilippineGeneratorMethod) {
        _method.value = value
    }

    fun setOddEven(value: PhilippineOddEvenFilter) {
        _oddEven.value = value
    }

    fun setLowHigh(value: PhilippineLowHighFilter) {
        _lowHigh.value = value
    }

    fun clearCombinations() {
        _combinations.value = emptyList()
    }

    fun checkForNewResults() {
        val selectedGame = _game.value
        viewModelScope.launch {
            try {
                val remote = PhilippineLottoApi.fetchRecentDraws(selectedGame, 1)
                val localLatest = _latest.value?.drawNumber
                val remoteLatest = remote.firstOrNull()?.drawNumber
                _updateAvailable.value =
                    localLatest != null && remoteLatest != null && remoteLatest > localLatest
            } catch (_: Exception) {
                _updateAvailable.value = false
            }
        }
    }

    fun dismissUpdatePrompt() {
        _updateAvailable.value = false
    }

    fun load() {
        _updateAvailable.value = false
        val selectedGame = _game.value
        viewModelScope.launch {
            _loading.value = true
            _error.value = "Updating from Server..."

            try {
                val result = PhilippineLottoApi.fetchRecentDraws(selectedGame, 40)

                if (result.isEmpty()) {
                    _error.value = if (_history.value.isNotEmpty()) {
                        "No results received from Server — showing saved results"
                    } else {
                        "No results received from Server"
                    }
                    return@launch
                }

                _history.value = result
                _latest.value = result.firstOrNull()
                historyCache.save(selectedGame, result)
                _error.value = "Updated from Server"
            } catch (e: Exception) {
                _error.value = if (_history.value.isNotEmpty()) {
                    "Server temporarily unavailable — showing saved results"
                } else {
                    "Server update failed"
                }
            } finally {
                _loading.value = false
            }
        }
    }

    fun generate(count: Int = 5) {
        val game = _game.value
        val analysis = _history.value.drop(1).take(10)
        val excluded = _latest.value?.numbers?.toSet().orEmpty()
        val frequency = (1..game.maxNumber).associateWith { n -> analysis.count { n in it.numbers } }
        val pool = (1..game.maxNumber).filterNot { it in excluded }
        val oldSets = analysis.map { it.numbers.toSet() }.toSet()
        val output = mutableListOf<PhilippineCombination>()

        repeat(30000) {
            if (output.size >= count) return@repeat
            val candidate = makeCandidate(pool, frequency, game)
            if (candidate.size != 6) return@repeat
            if (candidate.toSet() in oldSets) return@repeat
            if (!passes(candidate, game)) return@repeat
            if (output.none { it.numbers == candidate }) {
                output += PhilippineCombination(candidate, score(candidate, game, frequency), _method.value)
            }
        }

        _combinations.value = output
    }

    private fun makeCandidate(
        pool: List<Int>,
        frequency: Map<Int, Int>,
        game: PhilippineLottoGame
    ): List<Int> {
        return when (_method.value) {
            PhilippineGeneratorMethod.RANDOM -> pool.shuffled().take(6).sorted()
            PhilippineGeneratorMethod.BALANCED -> {
                repeat(100) {
                    val n = pool.shuffled().take(6).sorted()
                    if (passes(n, game) && n.last() - n.first() >= game.maxNumber / 2) return n
                }
                pool.shuffled().take(6).sorted()
            }
            PhilippineGeneratorMethod.FREQUENCY -> weighted(pool, frequency, 1.0)
            PhilippineGeneratorMethod.HOT -> weighted(pool, frequency, 2.0)
            PhilippineGeneratorMethod.COLD -> weighted(pool, frequency, -1.0)
            PhilippineGeneratorMethod.BALANCED_FREQUENCY -> balancedWeighted(pool, frequency, game, 1.0)
            PhilippineGeneratorMethod.BALANCED_HOT -> balancedWeighted(pool, frequency, game, 2.0)
            PhilippineGeneratorMethod.BALANCED_COLD -> balancedWeighted(pool, frequency, game, -1.0)
        }
    }

    private fun balancedWeighted(
        pool: List<Int>,
        frequency: Map<Int, Int>,
        game: PhilippineLottoGame,
        multiplier: Double
    ): List<Int> {
        repeat(150) {
            val candidate = weighted(pool, frequency, multiplier)
            if (
                candidate.size == 6 &&
                passes(candidate, game) &&
                candidate.last() - candidate.first() >= game.maxNumber / 2
            ) {
                return candidate
            }
        }
        return weighted(pool, frequency, multiplier)
    }

    private fun weighted(pool: List<Int>, frequency: Map<Int, Int>, multiplier: Double): List<Int> {
        val selected = mutableListOf<Int>()
        repeat(6) {
            val available = pool.filterNot { it in selected }
            if (available.isEmpty()) return@repeat
            val weights = available.map { n ->
                val f = frequency[n] ?: 0
                if (multiplier < 0) 1.0 + (10 - f).coerceAtLeast(0) * 0.5 else 1.0 + f * multiplier
            }
            val total = weights.sum()
            var point = Random.nextDouble() * total
            var pick = available.last()
            for (i in available.indices) {
                point -= weights[i]
                if (point <= 0.0) {
                    pick = available[i]
                    break
                }
            }
            selected += pick
        }
        return selected.sorted()
    }

    private fun passes(n: List<Int>, game: PhilippineLottoGame): Boolean {
        val odd = n.count { it % 2 != 0 }
        val even = 6 - odd
        if (_oddEven.value.odd != null && odd != _oddEven.value.odd) return false
        if (_oddEven.value.even != null && even != _oddEven.value.even) return false

        val lowBoundary = game.maxNumber / 2
        val low = n.count { it <= lowBoundary }
        val high = 6 - low
        if (_lowHigh.value.low != null && low != _lowHigh.value.low) return false
        if (_lowHigh.value.high != null && high != _lowHigh.value.high) return false
        return true
    }

    private fun score(n: List<Int>, game: PhilippineLottoGame, frequency: Map<Int, Int>): Int {
        val odd = n.count { it % 2 != 0 }
        val spreadPenalty = if (n.last() - n.first() < game.maxNumber / 2) 15 else 0
        return (100 - abs(odd - 3) * 10 - spreadPenalty + n.sumOf { frequency[it] ?: 0 } * 2).coerceIn(0, 100)
    }

    fun toggleFavorite(c: PhilippineCombination) {
        val game = _game.value
        if (_favorites.value.any { it.numbers == c.numbers }) {
            store.remove(game.key, c.numbers)
        } else {
            val target = _latest.value?.let { dateKey(nextDraw(it.date, game)) }
            store.save(game.key, FavoriteItem(c.numbers, c.score, c.method.label, System.currentTimeMillis(), target))
        }
        _favorites.value = store.load(game.key)
    }

    fun addManualFavorite(numbers: List<Int>) {
        val game = _game.value
        val sorted = numbers.distinct().sorted()
        if (sorted.size != 6 || sorted.any { it !in 1..game.maxNumber }) return
        val target = _latest.value?.let { dateKey(nextDraw(it.date, game)) }
        store.save(game.key, FavoriteItem(sorted, 0, "Manual", System.currentTimeMillis(), target))
        _favorites.value = store.load(game.key)
    }

    fun removeFavorite(item: FavoriteItem) {
        store.remove(_game.value.key, item.numbers)
        _favorites.value = store.load(_game.value.key)
    }
}

private fun dateKey(date: LocalDate): Int =
    date.year * 10000 + date.monthValue * 100 + date.dayOfMonth

private fun nextDraw(from: LocalDate, game: PhilippineLottoGame): LocalDate {
    var date = from.plusDays(1)
    repeat(14) {
        if (date.dayOfWeek in game.drawDays) return date
        date = date.plusDays(1)
    }
    return from.plusDays(1)
}

@Composable
fun PhilippineLottoApp(
    selectedGame: PhilippineLottoGame = PhilippineLottoGame.LOTTO_642,
    onHome: () -> Unit = {},
    vm: PhilippineLottoViewModel = viewModel()
) {
    val game by vm.game.collectAsState()
    val history by vm.history.collectAsState()
    val latest by vm.latest.collectAsState()
    val combinations by vm.combinations.collectAsState()
    val favorites by vm.favorites.collectAsState()
    val method by vm.method.collectAsState()
    val oddEven by vm.oddEven.collectAsState()
    val lowHigh by vm.lowHigh.collectAsState()
    val loading by vm.loading.collectAsState()
    val error by vm.error.collectAsState()
    val updateAvailable by vm.updateAvailable.collectAsState()

    if (updateAvailable) {
        AlertDialog(
            onDismissRequest = vm::dismissUpdatePrompt,
            title = { Text("New Results Available") },
            text = { Text("The saved results are behind the latest Server results.") },
            confirmButton = {
                TextButton(onClick = {
                    vm.dismissUpdatePrompt()
                    vm.load()
                }) { Text("Update Results") }
            },
            dismissButton = {
                TextButton(onClick = vm::dismissUpdatePrompt) { Text("Later") }
            }
        )
    }
    val tabPrefs = LocalContext.current.getSharedPreferences("navigation_state", Context.MODE_PRIVATE)
    var tab by androidx.compose.runtime.saveable.rememberSaveable {
        mutableIntStateOf(tabPrefs.getInt("philippine_tab", 1))
    }
    LaunchedEffect(tab) {
        tabPrefs.edit().putInt("philippine_tab", tab).apply()
    }
    val tabs = listOf("Home", "Generator", "History", "Frequency", "Favorites")
    val icons = listOf(
        Icons.Default.Home,
        Icons.Default.Casino,
        Icons.Default.History,
        Icons.Default.BarChart,
        Icons.Default.Favorite
    )

    LaunchedEffect(selectedGame) {
        if (game != selectedGame) {
            vm.selectGame(selectedGame)
        }
    }

    Scaffold(
        bottomBar = {
            Column {
                AccessExpiryWarning()
                NavigationBar {
                tabs.forEachIndexed { index, title ->
                    NavigationBarItem(
                        selected = tab == index,
                        onClick = { if (index == 0) onHome() else tab = index },
                        icon = { Icon(icons[index], contentDescription = title) },
                        label = { Text(title) }
                    )
                }
            }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                    .lotoTabSwipe(tab) { nextTab ->
                        if (nextTab == 0) onHome() else tab = nextTab
                    }
        ) {
            Spacer(Modifier.height(64.dp))
            PhilippineLatestResultCard(
                game = game,
                draw = latest,
                isUpdating = loading,
                onUpdate = vm::load
            )

            when (tab) {
                1 -> PhilippineGeneratorScreen(
                    game = game,
                    history = history,
                    combinations = combinations,
                    favorites = favorites,
                    method = method,
                    oddEven = oddEven,
                    lowHigh = lowHigh,
                    loading = loading,
                    error = error,
                    onMethod = vm::setMethod,
                    onOddEven = vm::setOddEven,
                    onLowHigh = vm::setLowHigh,
                    onRefresh = vm::load,
                    onGenerate = vm::generate,
                    onClear = vm::clearCombinations,
                    onFavorite = vm::toggleFavorite
                )
                2 -> PhilippineHistoryScreen(
                    game = game,
                    history = history,
                    loading = loading,
                    error = error,
                    onRefresh = vm::load
                )
                3 -> PhilippineFrequencyScreen(game = game, history = history)
                4 -> FavoriteListScreen(
                    game.title,
                    favorites,
                    latest?.drawNumber,
                    latest?.numbers?.toSet().orEmpty(),
                    vm::removeFavorite,
                    vm::addManualFavorite,
                    historyDrawNumbers = history.associate { it.drawNumber to it.numbers.toSet() },
                    historyDrawBonuses = emptyMap()
                )
            }
        }
    }
}

@Composable
private fun PhilippineLatestResultCard(
    game: PhilippineLottoGame,
    draw: PhilippineLottoDraw?,
    isUpdating: Boolean,
    onUpdate: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.EmojiEvents, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.size(7.dp))
                        Text("LATEST RESULT", fontWeight = FontWeight.Bold)
                    }
                    Text(
                        "Server • ${draw?.let { game.title + " Draw " + it.drawNumber } ?: "Updating…"}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = .75f)
                    )
                }
                if (isUpdating) {
                    CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                } else {
                    IconButton(onClick = onUpdate) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            if (draw == null) {
                Text("Loading ${game.title} results…")
            } else {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    draw.numbers.forEach { NumberBall(it, latestResult = true) }
                }
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        draw.date.format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US)),
                        style = MaterialTheme.typography.labelMedium
                    )
                    draw.jackpot?.let {
                        Spacer(Modifier.weight(1f))
                        Text(
                            "Jackpot ₱" + String.format(Locale.US, "%,.0f", it),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PhilippineFrequencyScreen(
    game: PhilippineLottoGame,
    history: List<PhilippineLottoDraw>
) {
    val analysis = history.drop(1).take(10)
    val sortedFrequency = (1..game.maxNumber).associateWith { n ->
        analysis.count { draw -> n in draw.numbers }
    }.entries.sortedWith(compareByDescending<Map.Entry<Int, Int>> { it.value }.thenBy { it.key })
    val maxFrequency = sortedFrequency.firstOrNull()?.value?.coerceAtLeast(1) ?: 1

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            PhilippineSectionHeader("Frequency analysis", "Previous 10 draws • latest result excluded")
        }
        items(sortedFrequency.toList()) { entry ->
            val percentage = entry.value.toFloat() / maxFrequency.toFloat()
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            NumberBall(entry.key)
                            Spacer(Modifier.size(10.dp))
                            Text("Number ${entry.key.toString().padStart(2, '0')}", fontWeight = FontWeight.SemiBold)
                        }
                        Text("${entry.value} / 10", fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { percentage.coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(50))
                    )
                }
            }
        }
    }
}

@Composable
private fun PhilippineGameMenu(
    selected: PhilippineLottoGame,
    onSelected: (PhilippineLottoGame) -> Unit
) {
    var expanded by remember {
        mutableStateOf(false)
    }

    Box {

        Button(
            onClick = {
                expanded = true
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text(selected.title)
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = {
                expanded = false
            },
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp
        ) {

            PhilippineLottoGame.entries.forEach { game ->

                DropdownMenuItem(
                    text = {
                        Text(
                            text = game.title,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    onClick = {
                        expanded = false
                        onSelected(game)
                    }
                )
            }
        }
    }
}

@Composable
private fun PhilippineGeneratorScreen(
    game: PhilippineLottoGame,
    history: List<PhilippineLottoDraw>,
    combinations: List<PhilippineCombination>,
    favorites: List<FavoriteItem>,
    method: PhilippineGeneratorMethod,
    oddEven: PhilippineOddEvenFilter,
    lowHigh: PhilippineLowHighFilter,
    loading: Boolean,
    error: String?,
    onMethod: (PhilippineGeneratorMethod) -> Unit,
    onOddEven: (PhilippineOddEvenFilter) -> Unit,
    onLowHigh: (PhilippineLowHighFilter) -> Unit,
    onRefresh: () -> Unit,
    onGenerate: (Int) -> Unit,
    onClear: () -> Unit,
    onFavorite: (PhilippineCombination) -> Unit
) {
    var generationCount by androidx.compose.runtime.saveable.rememberSaveable { mutableIntStateOf(5) }

    var showGenerateConfirmation by remember { mutableStateOf(false) }
    var showClearConfirmation by remember { mutableStateOf(false) }

    if (showGenerateConfirmation) {
        AlertDialog(
            onDismissRequest = { showGenerateConfirmation = false },
            title = { Text("Generate combinations?") },
            text = {
                Column {
                    Text("Generate $generationCount new ${game.title} combinations using these settings?")
                    Spacer(Modifier.height(12.dp))
                    Text("Strategy: ${method.label}", fontWeight = FontWeight.Bold)
                    Text("Odd / Even: ${oddEven.label}")
                    Text("Low / High: ${lowHigh.label}")
                    if (combinations.isNotEmpty()) {
                        Spacer(Modifier.height(12.dp))
                        Text("Your current generated combinations will be replaced.")
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showGenerateConfirmation = false; onGenerate(generationCount) }) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null)
                    Spacer(Modifier.size(6.dp))
                    Text("Generate")
                }
            },
            dismissButton = { TextButton(onClick = { showGenerateConfirmation = false }) { Text("Cancel") } }
        )
    }

    if (showClearConfirmation) {
        AlertDialog(
            onDismissRequest = { showClearConfirmation = false },
            title = { Text("Clear combinations?") },
            text = { Text("Are you sure you want to clear all generated ${game.title} combinations?") },
            confirmButton = { Button(onClick = { showClearConfirmation = false; onClear() }) { Text("Clear") } },
            dismissButton = { TextButton(onClick = { showClearConfirmation = false }) { Text("Cancel") } }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            PhilippineSectionHeader("Generate combinations", "${game.title} • 6 numbers • 1–${game.maxNumber}")
        }


        item {
            val prediction = NextDrawPatternPredictor.predict(
                draws = history.map { it.numbers }, numberCount = 6, lowBoundary = game.maxNumber / 2
            )
            NextDrawPatternCard(prediction)
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(Modifier.padding(14.dp)) {
                    Text("Number of combinations", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(1, 3, 5).forEach { count ->
                            FilterChip(
                                selected = generationCount == count,
                                onClick = { generationCount = count },
                                label = { Text("$count set${if (count == 1) "" else "s"}") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        item {
            PhilippineFilterCard(oddEven, lowHigh, method, onOddEven, onLowHigh, onMethod)
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { showGenerateConfirmation = true },
                    modifier = Modifier.weight(1f).height(54.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null)
                    Spacer(Modifier.size(6.dp))
                    Text("Generate $generationCount")
                }
                OutlinedButton(
                    onClick = { showClearConfirmation = true },
                    enabled = combinations.isNotEmpty(),
                    modifier = Modifier.weight(1f).height(54.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(Modifier.size(6.dp))
                    Text("Clear")
                }
            }
        }

        if (combinations.isNotEmpty()) {
            item { PhilippineSectionHeader("Generated combinations", "${combinations.size} combinations") }
            items(combinations) { combination ->
                PhilippineCombinationCard(
                    combination = combination,
                    isFavorite = favorites.any { it.numbers == combination.numbers },
                    onFavorite = { onFavorite(combination) }
                )
            }
        }
    }
}

@Composable
private fun PhilippineSectionHeader(title: String, subtitle: String) {
    Column(Modifier.padding(vertical = 2.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        if (subtitle.isNotBlank()) {
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun PhilippineFilterCard(
    oddEven: PhilippineOddEvenFilter,
    lowHigh: PhilippineLowHighFilter,
    method: PhilippineGeneratorMethod,
    onOddEven: (PhilippineOddEvenFilter) -> Unit,
    onLowHigh: (PhilippineLowHighFilter) -> Unit,
    onMethod: (PhilippineGeneratorMethod) -> Unit
) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.size(8.dp))
                Text("Generation settings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(12.dp))
            PhilippineDropdown("Odd / Even", oddEven.label, PhilippineOddEvenFilter.entries.map { it.label }) { selected ->
                PhilippineOddEvenFilter.entries.firstOrNull { it.label == selected }?.let(onOddEven)
            }
            PhilippineDropdown("Low / High", lowHigh.label, PhilippineLowHighFilter.entries.map { it.label }) { selected ->
                PhilippineLowHighFilter.entries.firstOrNull { it.label == selected }?.let(onLowHigh)
            }
            PhilippineDropdown("Strategy", method.label, PhilippineGeneratorMethod.entries.map { it.label }) { selected ->
                PhilippineGeneratorMethod.entries.firstOrNull { it.label == selected }?.let(onMethod)
            }
        }
    }
}

@Composable
private fun PhilippineDropdown(label: String, selected: String, options: List<String>, onSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        Box(Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Text(selected, modifier = Modifier.weight(1f), textAlign = TextAlign.Start)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { option ->
                    DropdownMenuItem(text = { Text(option) }, onClick = { expanded = false; onSelected(option) })
                }
            }
        }
    }
}

@Composable
private fun PhilippineCombinationCard(
    combination: PhilippineCombination,
    isFavorite: Boolean,
    onFavorite: () -> Unit
) {
    val context = LocalContext.current
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(15.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                    Text(
                        combination.method.label.uppercase(),
                        Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${combination.score}/100", fontWeight = FontWeight.Bold)
                    IconButton(onClick = onFavorite) {
                        Icon(
                            if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = if (isFavorite) "Remove from favorites" else "Save to favorites"
                        )
                    }
                    IconButton(onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Philippine Lotto", combination.numbers.joinToString("-")))
                    }) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy numbers")
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                combination.numbers.forEach { NumberBall(it) }
            }
        }
    }
}

@Composable
private fun <T> FilterRow(
    values: List<T>,
    selected: T,
    label: (T) -> String,
    onSelected: (T) -> Unit
) {
    androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items(values) { value ->
            FilterChip(selected = value == selected, onClick = { onSelected(value) }, label = { Text(label(value)) })
        }
    }
}

@Composable
private fun NumberBall(number: Int, latestResult: Boolean = false) {
    Surface(
        modifier = Modifier.size(38.dp),
        shape = MaterialTheme.shapes.extraLarge,
        color = if (latestResult) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                number.toString().padStart(2, '0'),
                fontWeight = FontWeight.Bold,
                color = if (latestResult) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

@Composable
private fun PhilippineHistoryScreen(
    game: PhilippineLottoGame,
    history: List<PhilippineLottoDraw>,
    loading: Boolean,
    error: String?,
    onRefresh: () -> Unit
) {
    var expandedDrawNumber by remember { mutableStateOf<Int?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(game.title + " History", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Previous draws • Server", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onRefresh) { Icon(Icons.Default.Refresh, "Refresh") }
            }
        }

        if (loading) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        if (error != null) item { Text(error, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(8.dp)) }

        item {
            Surface(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(8.dp)) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 7.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("DRAW", modifier = Modifier.width(70.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    Text("DATE", modifier = Modifier.width(72.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    Text("WINNING NUMBERS", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                }
            }
        }

        items(items = history, key = { it.drawNumber }) { draw ->
            val drawIndex = history.indexOf(draw)
            val isLatest = drawIndex == 0
            val isExpanded = expandedDrawNumber == draw.drawNumber

            Column(Modifier.fillMaxWidth()) {
                Surface(
                    modifier = Modifier.fillMaxWidth().clickable {
                        expandedDrawNumber = if (isExpanded) null else draw.drawNumber
                    },
                    color = if (isLatest) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 7.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.width(70.dp)) {
                            Text("#" + draw.drawNumber, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                            if (isLatest) Text("LATEST", fontSize = 7.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        Text(
                            draw.date.format(DateTimeFormatter.ofPattern("MM/dd/yy", Locale.US)),
                            modifier = Modifier.width(72.dp), fontSize = 8.sp, maxLines = 1,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.weight(1f).padding(horizontal = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            draw.numbers.take(6).forEach { number ->
                                PhilippineHistoryNumber(number, isLatest, Modifier.weight(1f))
                            }
                        }
                    }
                }

                if (isExpanded) {
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, bottom = 4.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp)
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            val oddCount = draw.numbers.count { it % 2 != 0 }
                            val evenCount = 6 - oddCount
                            val lowBoundary = game.maxNumber / 2
                            val lowCount = draw.numbers.count { it <= lowBoundary }
                            val highCount = 6 - lowCount

                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                PhilippineStatCard("ODD / EVEN", "$oddCount / $evenCount", Modifier.weight(1f))
                                PhilippineStatCard("LOW / HIGH", "$lowCount / $highCount", Modifier.weight(1f))
                            }

                            Spacer(Modifier.height(12.dp))
                            Text("Prize Details", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(6.dp))

                            if (draw.jackpot == null && draw.winners == null) {
                                Text(
                                    "Prize information is not available from Server.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                draw.jackpot?.let { Text("Jackpot: ₱" + String.format(Locale.US, "%,.2f", it), fontWeight = FontWeight.Bold) }
                                draw.winners?.let { Text(if (it == 1) "1 winner" else "$it winners") }
                            }

                            Spacer(Modifier.height(16.dp))
                            HorizontalDivider()
                            Spacer(Modifier.height(12.dp))

                                                        val historicalMethodMatches = HistoricalMethodClassifier.classify(
                                numbers = draw.numbers,
                                previousDraws = history.drop(drawIndex + 2).take(10).map { previousDraw -> previousDraw.numbers },
                                maxNumber = game.maxNumber
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

Text("Numbers Appeared in Previous 10 Draws", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

                            // Same behavior as Japan Loto 6: skip the immediately previous draw,
                            // then inspect the ten draws before it.
                            val appearanceDraws = history.drop(drawIndex + 2).take(10)
                            val rangeText = when {
                                appearanceDraws.isEmpty() -> "Not enough earlier draws loaded"
                                appearanceDraws.size == 1 -> appearanceDraws.first().date.format(DateTimeFormatter.ofPattern("M/d/yyyy", Locale.US))
                                else -> appearanceDraws.last().date.format(DateTimeFormatter.ofPattern("M/d/yyyy", Locale.US)) +
                                    " – " + appearanceDraws.first().date.format(DateTimeFormatter.ofPattern("M/d/yyyy", Locale.US))
                            }

                            Text(rangeText, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(10.dp))

                            val previous10Numbers = appearanceDraws.flatMap { it.numbers }.toSet()
                            val matchedNumbers = draw.numbers.filter { it in previous10Numbers }

                            if (matchedNumbers.isEmpty()) {
                                Text("None", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    matchedNumbers.forEach { number ->
                                        Box(
                                            modifier = Modifier.size(32.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                number.toString().padStart(2, '0'),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
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
    }
}

@Composable
private fun PhilippineHistoryNumber(number: Int, latest: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.aspectRatio(1f).background(
            if (latest) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            CircleShape
        ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            number.toString().padStart(2, '0'),
            color = if (latest) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

@Composable
private fun PhilippineStatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Text(label, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(2.dp))
            Text(value, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

