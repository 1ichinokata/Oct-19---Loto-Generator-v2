package com.example.lotogenerator

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.runtime.Composable
import androidx.compose.ui.composed
import androidx.compose.ui.platform.LocalDensity

/**
 * Horizontal navigation for the five main tabs.
 * Horizontal drags are recognized only after horizontal touch slop, so
 * vertical scrolling in LazyColumn/verticalScroll remains available.
 */
fun Modifier.lotoTabSwipe(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
): Modifier = pointerInput(selectedTab) {
    var dragTotal = 0f
    val threshold = 72.dp.toPx()

    detectHorizontalDragGestures(
        onHorizontalDrag = { _, dragAmount ->
            dragTotal += dragAmount
        },
        onDragEnd = {
            if (dragTotal <= -threshold) {
                val next = when (selectedTab) {
                    1 -> 0 // Generator -> Home
                    2 -> 3 // History -> Frequency
                    3 -> 4 // Frequency -> Favorites
                    4 -> 2 // Favorites -> History
                    else -> selectedTab
                }
                if (next != selectedTab) onTabSelected(next)
            } else if (dragTotal >= threshold) {
                val next = when (selectedTab) {
                    0 -> 1 // Home -> Generator
                    1 -> 2 // Generator -> History
                    2 -> 4 // History -> Favorites
                    3 -> 2 // Frequency -> History
                    else -> selectedTab
                }
                if (next != selectedTab) onTabSelected(next)
            }
            dragTotal = 0f
        },
        onDragCancel = { dragTotal = 0f }
    )
}

/** Settings: swipe left returns to the screen that opened Settings. */
fun Modifier.lotoSettingsSwipe(onBack: () -> Unit): Modifier =
    pointerInput(Unit) {
        var dragTotal = 0f
        val threshold = 72.dp.toPx()
        detectHorizontalDragGestures(
            onHorizontalDrag = { _, dragAmount -> dragTotal += dragAmount },
            onDragEnd = {
                if (dragTotal <= -threshold) onBack()
                dragTotal = 0f
            },
            onDragCancel = { dragTotal = 0f }
        )
    }
