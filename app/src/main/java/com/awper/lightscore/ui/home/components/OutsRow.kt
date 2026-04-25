package com.awper.lightscore.ui.home.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun OutsRow(outs: Int, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        repeat(3) { i ->
            Box(modifier = Modifier.size(10.dp).padding(1.dp)) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    if (i < outs) drawCircle(Color.White, size.minDimension / 2)
                    else drawCircle(Color.White, size.minDimension / 2, style = Stroke(1f))
                }
            }
            if (i < 2) Spacer(Modifier.width(2.dp))
        }
    }
}

@Composable
fun CountText(balls: Int, strikes: Int, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Text("$balls", style = TextStyle(color = Color.White, fontSize = 14.sp))
        Spacer(Modifier.width(2.dp))
        Text("-", style = TextStyle(color = Color.White, fontSize = 14.sp))
        Spacer(Modifier.width(2.dp))
        Text("$strikes", style = TextStyle(color = Color.White, fontSize = 14.sp))
    }
}

@Composable
fun InningIndicator(inning: Int, isTopInning: Boolean, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(if (isTopInning) "▲" else "▼", style = TextStyle(color = Color.White, fontSize = 14.sp))
        Spacer(Modifier.width(2.dp))
        Text("$inning", style = TextStyle(color = Color.White, fontSize = 14.sp))
    }
}
