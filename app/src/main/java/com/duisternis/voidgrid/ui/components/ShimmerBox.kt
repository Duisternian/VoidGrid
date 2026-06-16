package com.duisternis.voidgrid.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset


@Composable
fun ShimmerBox(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        0f, 1000f,
        infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Restart)
    )
    val brush = Brush.linearGradient(
        colors = listOf(Color(0xFF191919), Color(0xFF2C2C2C), Color(0xFF191919)),
        start = Offset(translateAnim - 500f, 0f),
        end = Offset(translateAnim, 0f)
    )
    Box(modifier = modifier.background(brush))
}