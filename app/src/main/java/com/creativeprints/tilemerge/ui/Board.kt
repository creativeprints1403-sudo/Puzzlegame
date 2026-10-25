package com.creativeprints.tilemerge.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.creativeprints.tilemerge.game.Direction
import com.creativeprints.tilemerge.game.GameState
import com.creativeprints.tilemerge.game.Tile
import kotlin.math.abs

@Composable
fun Board(state: GameState, onSwipe: (Direction) -> Unit, modifier: Modifier = Modifier) {
    val dark = isSystemInDarkTheme()
    val boardColor = if (dark) Color(0xFF3A3530) else Color(0xFFBBADA0)
    BoxWithConstraints(
        modifier
            .aspectRatio(1f)
            .background(boardColor, RoundedCornerShape(8.dp))
            .pointerInput(Unit) {
                val threshold = 24.dp.toPx()
                var total = Offset.Zero
                detectDragGestures(
                    onDragStart = { total = Offset.Zero },
                    onDragEnd = {
                        val (dx, dy) = total
                        if (maxOf(abs(dx), abs(dy)) >= threshold) {
                            onSwipe(
                                if (abs(dx) > abs(dy)) {
                                    if (dx > 0) Direction.RIGHT else Direction.LEFT
                                } else {
                                    if (dy > 0) Direction.DOWN else Direction.UP
                                }
                            )
                        }
                    },
                    onDrag = { change, drag ->
                        change.consume()
                        total += drag
                    },
                )
            },
    ) {
        val n = state.size
        val gap = if (n <= 4) 10.dp else 8.dp
        val cell = (maxWidth - gap * (n + 1)) / n
        fun pos(i: Int): Dp = gap + (cell + gap) * i

        for (r in 0 until n) for (c in 0 until n) {
            Box(
                Modifier
                    .offset(pos(c), pos(r))
                    .size(cell)
                    .background(emptyCellColor(dark), RoundedCornerShape(6.dp))
            )
        }
        state.cells.forEachIndexed { index, tile ->
            if (tile != null) {
                key(tile.id) {
                    TileView(tile, x = pos(index % n), y = pos(index / n), cell = cell, gridSize = n)
                }
            }
        }
    }
}

@Composable
private fun TileView(tile: Tile, x: Dp, y: Dp, cell: Dp, gridSize: Int) {
    val ax by animateDpAsState(x, tween(100), label = "x")
    val ay by animateDpAsState(y, tween(100), label = "y")
    val scale = remember { Animatable(if (tile.isNew) 0f else 1f) }
    LaunchedEffect(tile.value, tile.merged, tile.isNew) {
        when {
            tile.isNew -> scale.animateTo(1f, tween(140, delayMillis = 80))
            tile.merged -> {
                scale.snapTo(1.18f)
                scale.animateTo(1f, tween(140))
            }
        }
    }
    val digits = tile.value.toString().length
    val base = when (gridSize) { 4 -> 34; 5 -> 28; else -> 22 }
    val fontSize = when {
        digits <= 2 -> base
        digits == 3 -> base * 0.82f
        digits == 4 -> base * 0.66f
        else -> base * 0.52f
    }
    Box(
        Modifier
            .offset(ax, ay)
            .size(cell)
            .scale(scale.value)
            .background(tileColor(tile.value), RoundedCornerShape(6.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = tile.value.toString(),
            color = tileTextColor(tile.value),
            fontSize = fontSize.toFloat().sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
    }
}
