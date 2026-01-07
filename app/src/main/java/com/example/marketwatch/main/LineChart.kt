package com.example.marketwatch.main

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun LineChart(data: List<Double>) {
    val color = MaterialTheme.colorScheme.primary

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
    ) { 
        if (data.size > 1) {
            val max = data.maxOrNull() ?: 0.0
            val min = data.minOrNull() ?: 0.0
            val range = if (max - min == 0.0) 1.0 else max - min

            val path = Path()
            data.forEachIndexed { index, value ->
                val x = size.width * index / (data.size - 1)
                val y = size.height * (1 - ((value - min) / range).toFloat())

                if (index == 0) {
                    path.moveTo(x, y.toFloat())
                } else {
                    path.lineTo(x, y.toFloat())
                }
            }
            drawPath(path, color, style = Stroke(width = 4f))
        }
    }
}
