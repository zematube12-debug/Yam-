package com.yampicture.rental.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.yampicture.rental.logic.RentalStatus
import com.yampicture.rental.util.Fmt
import com.yampicture.rental.util.Settings

val LocalFmt = staticCompositionLocalOf { Fmt("en", false) }
val LocalSettings = staticCompositionLocalOf { Settings() }

val Navy = Color(0xFF1B2A41)
val Gold = Color(0xFFC99A2E)
val GoodGreen = Color(0xFF1E7B4F)
val GoodGreenDark = Color(0xFF6FD3A2)
val BadRed = Color(0xFFB3261E)
val BadRedDark = Color(0xFFFFB4AB)

private val LightScheme = lightColorScheme(
    primary = Navy,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCE4F0),
    onPrimaryContainer = Navy,
    secondary = Gold,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF6E7BF),
    onSecondaryContainer = Color(0xFF3B2A00),
    tertiary = Color(0xFF2F6F8F),
    background = Color(0xFFF6F6F3),
    onBackground = Color(0xFF16181D),
    surface = Color.White,
    onSurface = Color(0xFF16181D),
    surfaceVariant = Color(0xFFEDEEF0),
    onSurfaceVariant = Color(0xFF5A6070),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFFAFAF8),
    surfaceContainer = Color(0xFFF1F1EE),
    surfaceContainerHigh = Color(0xFFEBEBE8),
    surfaceContainerHighest = Color(0xFFE5E5E2),
    outline = Color(0xFF8A8F9C),
    outlineVariant = Color(0xFFD5D8DE),
    error = BadRed,
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFFE7BE4A),
    onPrimary = Color(0xFF1B1400),
    primaryContainer = Color(0xFF2A3A55),
    onPrimaryContainer = Color(0xFFDCE4F0),
    secondary = Color(0xFFE7BE4A),
    onSecondary = Color(0xFF1B1400),
    secondaryContainer = Color(0xFF4A3A10),
    onSecondaryContainer = Color(0xFFF6E7BF),
    tertiary = Color(0xFF8CC4E0),
    background = Color(0xFF0F1115),
    onBackground = Color(0xFFE8E9EC),
    surface = Color(0xFF181B22),
    onSurface = Color(0xFFE8E9EC),
    surfaceVariant = Color(0xFF232733),
    onSurfaceVariant = Color(0xFFA4A9B6),
    surfaceContainerLowest = Color(0xFF0B0D11),
    surfaceContainerLow = Color(0xFF14171D),
    surfaceContainer = Color(0xFF1B1E26),
    surfaceContainerHigh = Color(0xFF222630),
    surfaceContainerHighest = Color(0xFF2A2F3B),
    outline = Color(0xFF6B7080),
    outlineVariant = Color(0xFF343949),
    error = BadRedDark,
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun YamTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkScheme else LightScheme,
        shapes = AppShapes,
        content = content,
    )
}

/** Container and content colour for a rental status badge. */
@Composable
fun statusPalette(status: RentalStatus): Pair<Color, Color> {
    val dark = isSystemInDarkTheme()
    return when (status) {
        RentalStatus.OVERDUE ->
            if (dark) Color(0xFF5C1A17) to Color(0xFFFFB4AB) else Color(0xFFFDE0DD) to Color(0xFF9C1C14)
        RentalStatus.ACTIVE ->
            if (dark) Color(0xFF123B4A) to Color(0xFF9FD8EE) else Color(0xFFDDF1F8) to Color(0xFF0B5870)
        RentalStatus.RESERVED ->
            if (dark) Color(0xFF4A3A10) to Color(0xFFF6D77A) else Color(0xFFFBEFC9) to Color(0xFF7A5600)
        RentalStatus.RETURNED ->
            if (dark) Color(0xFF16402B) to Color(0xFF9BE3BC) else Color(0xFFDDF3E6) to Color(0xFF14653F)
    }
}

@Composable
fun goodColor(): Color = if (isSystemInDarkTheme()) GoodGreenDark else GoodGreen

@Composable
fun badColor(): Color = if (isSystemInDarkTheme()) BadRedDark else BadRed
