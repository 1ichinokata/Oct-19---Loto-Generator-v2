package com.example.lotogenerator

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.SharedPreferences
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class FavoriteItem(
    val numbers: List<Int>,
    val score: Int,
    val method: String,
    val savedAt: Long = System.currentTimeMillis(),
    val targetDrawNumber: Int? = null
)

class FavoriteStore(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(
            "lottery_favorites",
            Context.MODE_PRIVATE
        )

    fun load(lotteryKey: String): List<FavoriteItem> {

        val raw = prefs.getString(lotteryKey, "") ?: return emptyList()

        if (raw.isBlank()) {
            return emptyList()
        }

        return raw.lineSequence()
            .mapNotNull { line ->

                val parts = line.split("|")

                if (parts.size < 3) {
                    return@mapNotNull null
                }

                val numbers =
                    parts[0]
                        .split(",")
                        .mapNotNull { it.toIntOrNull() }

                val score =
                    parts[1].toIntOrNull()
                        ?: return@mapNotNull null

                val method = parts[2]

                val savedAt =
                    if (parts.size >= 4) {
                        parts[3].toLongOrNull()
                            ?: System.currentTimeMillis()
                    } else {
                        0L
                    }

                val targetDrawNumber =
                    if (parts.size >= 5) {
                        parts[4].toIntOrNull()
                    } else {
                        null
                    }

                FavoriteItem(
                    numbers = numbers,
                    score = score,
                    method = method,
                    savedAt = savedAt,
                    targetDrawNumber = targetDrawNumber
                )
            }
            .toList()
    }

    fun save(
        lotteryKey: String,
        item: FavoriteItem
    ) {

        val current = load(lotteryKey).toMutableList()

        if (
            current.any {
                it.numbers == item.numbers
            }
        ) {
            return
        }

        current.add(item)

        persist(
            lotteryKey,
            current
        )
    }

    fun remove(
        lotteryKey: String,
        numbers: List<Int>
    ) {

        persist(
            lotteryKey,
            load(lotteryKey).filterNot {
                it.numbers == numbers
            }
        )
    }

    private fun persist(
        lotteryKey: String,
        items: List<FavoriteItem>
    ) {

        val raw =
            items.joinToString("\n") {

                "${it.numbers.joinToString(",")}" +
                        "|${it.score}" +
                        "|${it.method.replace("|", "")}" +
                        "|${it.savedAt}" +
                        "|${it.targetDrawNumber ?: ""}"
            }

        prefs.edit()
            .putString(
                lotteryKey,
                raw
            )
            .apply()
    }
}

private fun formatFavoriteDate(
    timestamp: Long
): String {

    if (timestamp <= 0L) {
        return "Previously saved"
    }

    return SimpleDateFormat(
        "EEE, MMM dd, yyyy • h:mm a",
        Locale.getDefault()
    ).format(
        Date(timestamp)
    )
}

/*
 * Returns the correct lottery rules.
 *
 * Mini Loto = 5 numbers, 1–31
 * Loto 6    = 6 numbers, 1–43
 * Loto 7    = 7 numbers, 1–37
 */
private fun getLotteryRules(
    lotteryName: String
): Pair<Int, Int> {

    return when (lotteryName.lowercase()) {

        "mini loto" ->
            31 to 5

        "loto 6" ->
            43 to 6

        "loto 7" ->
            37 to 7

        else ->
            43 to 6
    }
}

@Composable
fun FavoriteListScreen(
    lotteryName: String,
    favorites: List<FavoriteItem>,
    latestDrawNumber: Int?,
    latestDrawNumbers: Set<Int>,
    onRemove: (FavoriteItem) -> Unit,
    onAddManual: (List<Int>) -> Unit
) {

    var showAddDialog by remember {
        mutableStateOf(false)
    }

    if (showAddDialog) {

        ManualFavoriteDialog(
            lotteryName = lotteryName,

            onDismiss = {
                showAddDialog = false
            },

            onSave = { numbers ->

                showAddDialog = false

                onAddManual(numbers)
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            horizontal = 16.dp,
            vertical = 12.dp
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {

        item {

            Column {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {

                        Text(
                            "$lotteryName Favorites",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            "Your saved combinations stay on this device.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = {
                            showAddDialog = true
                        },
                        shape = RoundedCornerShape(14.dp)
                    ) {

                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )

                        Spacer(
                            Modifier.size(5.dp)
                        )

                        Text("Add")
                    }
                }
            }
        }

        if (favorites.isEmpty()) {

            item {

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp)
                ) {

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {

                        Text(
                            "No favorites yet",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(
                            Modifier.height(6.dp)
                        )

                        Text(
                            "Tap Add to enter numbers manually, or tap the ♥ button on a generated combination.",
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

        } else {

            items(
                items = favorites,
                key = {
                    it.numbers.joinToString("-")
                }
            ) { item ->

                FavoriteItemCard(
                    item = item,
                    lotteryName = lotteryName,
                    latestDrawNumber = latestDrawNumber,
                    latestDrawNumbers = latestDrawNumbers,
                    onRemove = {
                        onRemove(item)
                    }
                )
            }
        }
    }
}

@Composable
private fun FavoriteItemCard(
    item: FavoriteItem,
    lotteryName: String,
    latestDrawNumber: Int?,
    latestDrawNumbers: Set<Int>,
    onRemove: () -> Unit
) {

    var expanded by remember {
        mutableStateOf(false)
    }

    val (_, requiredNumbers) =
        getLotteryRules(lotteryName)

    val isTargetDraw =
        item.targetDrawNumber != null &&
                latestDrawNumber == item.targetDrawNumber

    val matchedNumbers =
        if (isTargetDraw) {
            item.numbers.filter {
                it in latestDrawNumbers
            }
        } else {
            emptyList()
        }

    val matchedCount =
        matchedNumbers.size

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor =
                if (isTargetDraw && matchedCount > 0) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surface
                }
        ),
        onClick = {
            expanded = !expanded
        }
    ) {

        Column(
            modifier = Modifier.padding(14.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {

                    Text(
                        item.method.uppercase(),
                        modifier = Modifier.padding(
                            horizontal = 8.dp,
                            vertical = 4.dp
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(
                    Modifier.weight(1f)
                )

                Text(
                    "${item.score}/100",
                    fontWeight = FontWeight.Bold
                )

                IconButton(
                    onClick = onRemove
                ) {

                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Remove favorite"
                    )
                }

                IconButton(
                    onClick = {
                        expanded = !expanded
                    }
                ) {

                    Icon(
                        if (expanded) {
                            Icons.Default.ExpandLess
                        } else {
                            Icons.Default.ExpandMore
                        },
                        contentDescription =
                            if (expanded) {
                                "Collapse favorite"
                            } else {
                                "Show favorite details"
                            }
                    )
                }
            }

            Spacer(
                Modifier.height(10.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

                item.numbers.forEach { number ->

                    val isMatch =
                        number in matchedNumbers

                    FavoriteNumberBubble(
                        number = number,
                        isMatch = isMatch
                    )
                }

                val context =
                    LocalContext.current

                IconButton(
                    onClick = {

                        val clipboard =
                            context.getSystemService(
                                Context.CLIPBOARD_SERVICE
                            ) as ClipboardManager

                        clipboard.setPrimaryClip(
                            ClipData.newPlainText(
                                lotteryName,
                                item.numbers.joinToString("-")
                            )
                        )
                    }
                ) {

                    Icon(
                        Icons.Default.ContentCopy,
                        contentDescription = "Copy favorite"
                    )
                }
            }

            if (isTargetDraw) {

                Spacer(
                    Modifier.height(8.dp)
                )

                if (matchedCount > 0) {

                    Text(
                        "$matchedCount number${if (matchedCount == 1) "" else "s"} matched the target draw",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelMedium
                    )

                } else {

                    Text(
                        "0 / $requiredNumbers matched the target draw",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (expanded) {

                Spacer(
                    Modifier.height(10.dp)
                )

                androidx.compose.material3.HorizontalDivider()

                Spacer(
                    Modifier.height(10.dp)
                )

                Text(
                    "Favorite Details",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(
                    Modifier.height(8.dp)
                )

                Text(
                    "Saved",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    formatFavoriteDate(item.savedAt),
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(
                    Modifier.height(8.dp)
                )

                Text(
                    "Target Draw",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (item.targetDrawNumber != null) {

                    Text(
                        "#${item.targetDrawNumber}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )

                } else {

                    Text(
                        "Target draw not assigned",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(
                    Modifier.height(10.dp)
                )

                Text(
                    "Status",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                when {

                    item.targetDrawNumber == null -> {

                        Text(
                            "Saved favorite",
                            fontWeight = FontWeight.Medium
                        )
                    }

                    latestDrawNumber == null -> {

                        Text(
                            "Waiting for results..."
                        )
                    }

                    latestDrawNumber < item.targetDrawNumber -> {

                        Text(
                            "Waiting for draw #${item.targetDrawNumber}",
                            fontWeight = FontWeight.Medium
                        )
                    }

                    latestDrawNumber == item.targetDrawNumber -> {

                        Text(
                            if (matchedCount > 0) {
                                "$matchedCount / $requiredNumbers MATCHED"
                            } else {
                                "0 / $requiredNumbers MATCHED"
                            },
                            fontWeight = FontWeight.Bold,
                            color =
                                if (matchedCount > 0) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                        )

                        if (matchedCount > 0) {

                            Spacer(
                                Modifier.height(5.dp)
                            )

                            Text(
                                "Matched: ${
                                    matchedNumbers.joinToString(" • ") {
                                        it.toString().padStart(2, '0')
                                    }
                                }",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    else -> {

                        Text(
                            "Target draw #${item.targetDrawNumber} has already passed.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FavoriteNumberBubble(
    number: Int,
    isMatch: Boolean
) {

    Surface(
        modifier = Modifier.size(40.dp),
        shape = RoundedCornerShape(50),
        color =
            if (isMatch) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
        contentColor =
            if (isMatch) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        tonalElevation = 2.dp
    ) {

        Box(
            contentAlignment = Alignment.Center
        ) {

            Text(
                number
                    .toString()
                    .padStart(2, '0'),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun ManualFavoriteDialog(
    lotteryName: String,
    onDismiss: () -> Unit,
    onSave: (List<Int>) -> Unit
) {

    val (maxNumber, requiredNumbers) =
        getLotteryRules(lotteryName)

    var numberText by remember {
        mutableStateOf("")
    }

    val numbers =
        numberText
            .split(
                Regex("[,\\s-]+")
            )
            .mapNotNull {
                it.toIntOrNull()
            }
            .distinct()
            .sorted()

    val valid =
        numbers.size == requiredNumbers &&
                numbers.all {
                    it in 1..maxNumber
                }

    AlertDialog(
        onDismissRequest = onDismiss,

        title = {
            Text(
                "Add $lotteryName Favorite"
            )
        },

        text = {

            Column {

                Text(
                    "Enter $requiredNumbers different numbers from 1 to $maxNumber."
                )

                Spacer(
                    Modifier.height(12.dp)
                )

                OutlinedTextField(
                    value = numberText,
                    onValueChange = {
                        numberText = it
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text("Numbers")
                    },
                    placeholder = {

                        Text(
                            when (lotteryName.lowercase()) {

                                "mini loto" ->
                                    "05 12 18 23 31"

                                "loto 6" ->
                                    "05 12 18 23 31 42"

                                "loto 7" ->
                                    "05 12 18 23 31 34 37"

                                else ->
                                    "05 12 18 23 31 42"
                            }
                        )
                    },
                    singleLine = true
                )

                Spacer(
                    Modifier.height(8.dp)
                )

                if (numbers.isNotEmpty()) {

                    Text(
                        "Selected: ${
                            numbers.joinToString(" • ") {
                                it.toString().padStart(2, '0')
                            }
                        }",
                        fontWeight = FontWeight.Bold
                    )
                }

                if (numberText.isNotBlank() && !valid) {

                    Spacer(
                        Modifier.height(6.dp)
                    )

                    Text(
                        "Please enter exactly $requiredNumbers unique numbers from 1–$maxNumber.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        },

        confirmButton = {

            Button(
                enabled = valid,
                onClick = {
                    onSave(numbers)
                }
            ) {
                Text("Save")
            }
        },

        dismissButton = {

            TextButton(
                onClick = onDismiss
            ) {
                Text("Cancel")
            }
        }
    )
}