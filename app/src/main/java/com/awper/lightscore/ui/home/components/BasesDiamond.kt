package com.awper.lightscore.ui.home.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun BasesDiamond(
    runnerFirst: Boolean,
    runnerSecond: Boolean,
    runnerThird: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val pad = minOf(w, h) * 0.1f
        val bs = minOf(w, h) * 0.18f
        val topY = h * 0.22f
        val bottomY = h * 0.72f
        val sideY = h * 0.50f
        val homeX = w / 2
        val homeY = bottomY
        val firstX = w - pad
        val firstY = sideY
        val secondX = w / 2
        val secondY = topY
        val thirdX = pad
        val thirdY = sideY
        drawBase(firstX, firstY, bs, runnerFirst)
        drawBase(secondX, secondY, bs, runnerSecond)
        drawBase(thirdX, thirdY, bs, runnerThird)
        val hw = bs * 0.5f; val hh = bs * 0.35f
        drawPath(Path().apply {
            moveTo(homeX - hw, homeY - hh)
            lineTo(homeX + hw, homeY - hh)
            lineTo(homeX + hw * 0.65f, homeY)
            lineTo(homeX, homeY + hh * 0.8f)
            lineTo(homeX - hw * 0.65f, homeY)
            close()
        }, Color.White)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawBase(cx: Float, cy: Float, size: Float, filled: Boolean) {
    val half = size / 2
    if (filled) drawRect(Color.White, Offset(cx - half, cy - half), Size(size, size))
    else drawRect(Color.White, Offset(cx - half, cy - half), Size(size, size), style = Stroke(width = 1.5f))
}
