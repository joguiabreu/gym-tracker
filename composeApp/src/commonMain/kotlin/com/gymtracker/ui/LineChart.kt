package com.gymtracker.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.*
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gymtracker.data.ExerciseProgress
import kotlin.math.roundToInt

@OptIn(ExperimentalTextApi::class)
@Composable
fun ProgressionLineChart(
    data: List<ExerciseProgress>,
    modifier: Modifier = Modifier,
    lineColor: Color = MaterialTheme.colorScheme.primary,
    gridColor: Color = MaterialTheme.colorScheme.outlineVariant,
    labelColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    pointRadius: Dp = 5.dp,
    strokeWidth: Dp = 2.5.dp
) {
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = TextStyle(fontSize = 10.sp, color = labelColor)

    Canvas(modifier = modifier) {
        if (data.size < 1) return@Canvas

        val padLeft = 56f
        val padRight = 16f
        val padTop = 16f
        val padBottom = 36f

        val chartW = size.width - padLeft - padRight
        val chartH = size.height - padTop - padBottom

        val maxY = data.maxOf { it.maxWeightKg }
        val minY = (data.minOf { it.maxWeightKg } - 1.0).coerceAtLeast(0.0)
        val rangeY = (maxY - minY).coerceAtLeast(1.0)

        // --- grid lines (4 horizontal) ---
        val gridCount = 4
        repeat(gridCount + 1) { i ->
            val fraction = i.toFloat() / gridCount
            val y = padTop + chartH * (1f - fraction)
            drawLine(gridColor, Offset(padLeft, y), Offset(padLeft + chartW, y), strokeWidth = 1f)

            val value = minY + rangeY * fraction
            val label = "${value.roundToInt()} kg"
            val measured = textMeasurer.measure(AnnotatedString(label), labelStyle)
            drawText(measured, topLeft = Offset(padLeft - measured.size.width - 4f,
                y - measured.size.height / 2f))
        }

        if (data.size == 1) {
            // Single point — just draw it with a label
            val x = padLeft + chartW / 2f
            val y = padTop + chartH / 2f
            drawCircle(lineColor, radius = pointRadius.toPx(), center = Offset(x, y))
            val label = data[0].date.takeLast(5)   // MM-DD
            val measured = textMeasurer.measure(AnnotatedString(label), labelStyle)
            drawText(measured, topLeft = Offset(x - measured.size.width / 2f,
                padTop + chartH + 6f))
            return@Canvas
        }

        // --- compute point positions ---
        val points = data.mapIndexed { i, point ->
            val x = padLeft + (i.toFloat() / (data.size - 1)) * chartW
            val y = padTop + chartH * (1f - ((point.maxWeightKg - minY) / rangeY).toFloat())
            Offset(x, y)
        }

        // --- filled area under the line ---
        val fillPath = Path().apply {
            moveTo(points.first().x, padTop + chartH)
            points.forEach { lineTo(it.x, it.y) }
            lineTo(points.last().x, padTop + chartH)
            close()
        }
        drawPath(fillPath, lineColor.copy(alpha = 0.12f))

        // --- line ---
        val linePath = Path().apply {
            moveTo(points.first().x, points.first().y)
            points.drop(1).forEach { lineTo(it.x, it.y) }
        }
        drawPath(linePath, lineColor, style = Stroke(width = strokeWidth.toPx()))

        // --- data points + x-axis labels ---
        points.forEachIndexed { i, pt ->
            drawCircle(lineColor, radius = pointRadius.toPx(), center = pt)
            drawCircle(Color.White, radius = (pointRadius - 1.5.dp).toPx(), center = pt)

            // x label: show MM-DD
            val label = data[i].date.takeLast(5)
            val measured = textMeasurer.measure(AnnotatedString(label), labelStyle)
            drawText(measured, topLeft = Offset(pt.x - measured.size.width / 2f,
                padTop + chartH + 6f))
        }
    }
}
