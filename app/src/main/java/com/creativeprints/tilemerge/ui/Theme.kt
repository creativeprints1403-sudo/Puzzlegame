package com.creativeprints.tilemerge.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Accent = Color(0xFF8F7A66)

private val Light = lightColorScheme(
    primary = Accent,
    onPrimary = Color.White,
    background = Color(0xFFFAF8EF),
    onBackground = Color(0xFF776E65),
    surface = Color(0xFFFAF8EF),
    onSurface = Color(0xFF776E65),
    surfaceVariant = Color(0xFFBBADA0),
    onSurfaceVariant = Color(0xFFEEE4DA),
)

private val Dark = darkColorScheme(
    primary = Color(0xFFD7B98E),
    onPrimary = Color(0xFF2B2117),
    background = Color(0xFF1C1B1F),
    onBackground = Color(0xFFE8E1D9),
    surface = Color(0xFF262429),
    onSurface = Color(0xFFE8E1D9),
    surfaceVariant = Color(0xFF3A3530),
    onSurfaceVariant = Color(0xFFD8CFC4),
)

@Composable
fun TileMergeTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) Dark else Light, content = content)
}

/** Classic tile palette; values past 2048 share one dark colour. */
fun tileColor(value: Int): Color = when (value) {
    2 -> Color(0xFFEEE4DA)
    4 -> Color(0xFFEDE0C8)
    8 -> Color(0xFFF2B179)
    16 -> Color(0xFFF59563)
    32 -> Color(0xFFF67C5F)
    64 -> Color(0xFFF65E3B)
    128 -> Color(0xFFEDCF72)
    256 -> Color(0xFFEDCC61)
    512 -> Color(0xFFEDC850)
    1024 -> Color(0xFFEDC53F)
    2048 -> Color(0xFFEDC22E)
    else -> Color(0xFF3C3A32)
}

fun tileTextColor(value: Int): Color = if (value <= 4) Color(0xFF776E65) else Color(0xFFF9F6F2)

fun emptyCellColor(dark: Boolean): Color = if (dark) Color(0xFF4A443D) else Color(0xFFCDC1B4)
