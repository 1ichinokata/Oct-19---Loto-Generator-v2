package com.example.lotogenerator

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

/**
 * Horizontal navigation for the five main tabs.
 * Horizontal drags are recognized after horizontal touch slop, leaving
 * vertical scrolling in LazyColumn and verticalScroll available.
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
                    2 -> 3 // History -> Frequency
                    3 -> 4 // Frequency -> Favorites
                    4 -> 2 // Favorites -> History
                    else -> selectedTab
                }
                if (next != selectedTab) onTabSelected(next)
            } else if (dragTotal >= threshold) {
                val next = when (selectedTab) {
                    1 -> 0 // Generator -> Home
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
