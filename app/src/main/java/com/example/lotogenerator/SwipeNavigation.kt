package com.example.lotogenerator

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

/**
 * Horizontal navigation for the five main tabs.
 * Swipe left advances; swipe right goes back.
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
                // Swipe left: Home -> Generator -> History -> Frequency -> Favorites
                val next = when (selectedTab) {
                    0 -> 1 // Home -> Generator
                    1 -> 2 // Generator -> History
                    2 -> 3 // History -> Frequency
                    3 -> 4 // Frequency -> Favorites
                    else -> selectedTab // Favorites -> No action
                }
                if (next != selectedTab) onTabSelected(next)
            } else if (dragTotal >= threshold) {
                // Swipe right: Favorites -> Frequency -> History -> Generator -> Home
                val next = when (selectedTab) {
                    1 -> 0 // Generator -> Home
                    2 -> 1 // History -> Generator
                    3 -> 2 // Frequency -> History
                    4 -> 3 // Favorites -> Frequency
                    else -> selectedTab // Home -> No action
                }
                if (next != selectedTab) onTabSelected(next)
            }
            dragTotal = 0f
        },
        onDragCancel = { dragTotal = 0f }
    )
}

/** Settings: swipe right returns to the screen that opened Settings. */
fun Modifier.lotoSettingsSwipe(onBack: () -> Unit): Modifier =
    pointerInput(Unit) {
        var dragTotal = 0f
        val threshold = 72.dp.toPx()
        detectHorizontalDragGestures(
            onHorizontalDrag = { _, dragAmount -> dragTotal += dragAmount },
            onDragEnd = {
                if (dragTotal >= threshold) onBack()
                dragTotal = 0f
            },
            onDragCancel = { dragTotal = 0f }
        )
    }
