package com.example.lotogenerator

import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
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
    RANDOM("Random"), BALANCED("Balanced"), FREQUENCY("Frequency"), HOT("Hot"), COLD("Cold")
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

    init {
        _favorites.value = store.load(_game.value.key)
        load()
    }

    fun selectGame(value: PhilippineLottoGame) {
        _game.value = value
        _favorites.value = store.load(value.key)
        load()
    }

    fun setMethod(value: PhilippineGeneratorMethod) {
        _method.value = value
        generate()
    }

    fun setOddEven(value: PhilippineOddEvenFilter) {
        _oddEven.value = value
        generate()
    }

    fun setLowHigh(value: PhilippineLowHighFilter) {
        _lowHigh.value = value
        generate()
    }

    fun load() {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                val result = PhilippineLottoApi.fetchRecentDraws(_game.value, 40)
                _history.value = result
                _latest.value = result.firstOrNull()
            } catch (e: Exception) {
                _error.value = e.message ?: "Unable to load PCSO results."
            }
            generate()
            _loading.value = false
        }
    }

    fun generate() {
        val game = _game.value
        val analysis = _history.value.drop(1).take(10)
        val excluded = _latest.value?.numbers?.toSet().orEmpty()
        val frequency = (1..game.maxNumber).associateWith { n -> analysis.count { n in it.numbers } }
        val pool = (1..game.maxNumber).filterNot { it in excluded }
        val oldSets = analysis.map { it.numbers.toSet() }.toSet()
        val output = mutableListOf<PhilippineCombination>()

        repeat(30000) {
            if (output.size >= 10) return@repeat
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
        }
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
fun PhilippineLottoApp(vm: PhilippineLottoViewModel = viewModel()) {
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
    var tab by remember { mutableIntStateOf(0) }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("🇵🇭 Philippine Lotto", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            PhilippineGameMenu(game) { vm.selectGame(it); tab = 0 }
        }

        TabRow(selectedTabIndex = tab) {
            listOf("Generate", "History", "Favorites").forEachIndexed { index, title ->
                Tab(selected = tab == index, onClick = { tab = index }, text = { Text(title) })
            }
        }

        when (tab) {
            0 -> PhilippineGeneratorScreen(game, latest, combinations, favorites, method, oddEven, lowHigh, loading, error, vm::setMethod, vm::setOddEven, vm::setLowHigh, vm::load, vm::toggleFavorite)
            1 -> PhilippineHistoryScreen(game, history, loading, error, vm::load)
            2 -> FavoriteListScreen(game.title, favorites, latest?.drawNumber, latest?.numbers?.toSet().orEmpty(), vm::removeFavorite, vm::addManualFavorite)
        }
    }
}

@Composable
private fun PhilippineGameMenu(selected: PhilippineLottoGame, onSelected: (PhilippineLottoGame) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Button(onClick = { expanded = true }) { Text(selected.title) }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            PhilippineLottoGame.entries.forEach { game ->
                DropdownMenuItem(text = { Text(game.title) }, onClick = { expanded = false; onSelected(game) })
            }
        }
    }
}

@Composable
private fun PhilippineGeneratorScreen(
    game: PhilippineLottoGame,
    latest: PhilippineLottoDraw?,
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
    onFavorite: (PhilippineCombination) -> Unit
) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(game.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("6 numbers • 1–" + game.maxNumber)
                }
                IconButton(onClick = onRefresh) { Icon(Icons.Default.Refresh, "Refresh") }
            }
        }

        item {
            if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (error != null) Text(error, color = MaterialTheme.colorScheme.error)
        }

        if (latest != null) item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp)) {
                    Text("Latest Result", fontWeight = FontWeight.Bold)
                    Text(latest.date.format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US)))
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        latest.numbers.forEach { NumberBall(it) }
                    }
                    latest.jackpot?.let { Text("Jackpot: ₱" + String.format(Locale.US, "%,.2f", it)) }
                    latest.winners?.let { Text("Winners: " + it) }
                }
            }
        }

        item {
            Text("Method", fontWeight = FontWeight.Bold)
            FilterRow(PhilippineGeneratorMethod.entries, method, { it.label }, onMethod)
        }

        item {
            Text("Odd / Even", fontWeight = FontWeight.Bold)
            FilterRow(PhilippineOddEvenFilter.entries, oddEven, { it.label }, onOddEven)
        }

        item {
            Text("Low / High", fontWeight = FontWeight.Bold)
            FilterRow(PhilippineLowHighFilter.entries, lowHigh, { it.label }, onLowHigh)
        }

        item {
            Text("Generated Combinations", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        items(combinations) { combination ->
            val saved = favorites.any { it.numbers == combination.numbers }
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Row(Modifier.weight(1f), horizontalArrangement = Arrangement.SpaceEvenly) {
                        combination.numbers.forEach { NumberBall(it) }
                    }
                    IconButton(onClick = { onFavorite(combination) }) {
                        Icon(
                            Icons.Default.Star,
                            "Favorite",
                            tint = if (saved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
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
private fun NumberBall(number: Int) {
    Surface(
        modifier = Modifier.size(38.dp),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(number.toString().padStart(2, '0'), fontWeight = FontWeight.Bold)
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
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(game.title + " History", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                IconButton(onClick = onRefresh) { Icon(Icons.Default.Refresh, "Refresh") }
            }
        }
        item {
            Text(
                "Previous 10 draws are used for generator analysis; latest draw is excluded.",
                style = MaterialTheme.typography.bodySmall
            )
        }
        if (loading) item { CircularProgressIndicator() }
        if (error != null) item { Text(error, color = MaterialTheme.colorScheme.error) }

        items(history) { draw ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text(draw.date.format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US)), fontWeight = FontWeight.Bold)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        draw.numbers.forEach { NumberBall(it) }
                    }
                    draw.jackpot?.let { Text("Jackpot: ₱" + String.format(Locale.US, "%,.2f", it)) }
                    draw.winners?.let { Text("Winners: " + it) }
                }
            }
        }
    }
}
