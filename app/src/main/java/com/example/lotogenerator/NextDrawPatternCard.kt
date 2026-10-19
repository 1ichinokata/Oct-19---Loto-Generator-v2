package com.example.lotogenerator

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun NextDrawPatternCard(prediction: PatternPrediction?) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Text("Next Draw Pattern Estimate", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            if (prediction == null) {
                Text("Not enough history to estimate the next pattern.", style = MaterialTheme.typography.bodySmall)
            } else {
                Text("Odd / Even: ${prediction.odd} / ${prediction.even}  •  Score ${prediction.oddEvenScore}")
                Text("Low / High: ${prediction.low} / ${prediction.high}  •  Score ${prediction.lowHighScore}")
                Spacer(Modifier.height(4.dp))
                Text("Alternative: O/E ${prediction.alternativeOdd}/${prediction.alternativeEven}  •  L/H ${prediction.alternativeLow}/${prediction.alternativeHigh}", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(6.dp))
                Text("Statistical estimate from recent draws; lottery outcomes remain random.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
