package com.kitcheninventory.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

// A fixed kitchen-green palette (instead of wallpaper colours) so the app looks the same on every phone.
private val LightColors = lightColorScheme(
    primary = Color(0xFF2E6B3F),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB2F1BF),
    onPrimaryContainer = Color(0xFF00210C),
    secondary = Color(0xFF506352),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD3E8D3),
    onSecondaryContainer = Color(0xFF0E1F12),
    tertiary = Color(0xFF3A6470),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFBEEAF7),
    onTertiaryContainer = Color(0xFF001F26),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFF7FBF3),
    onBackground = Color(0xFF181D18),
    surface = Color(0xFFF7FBF3),
    onSurface = Color(0xFF181D18),
    surfaceVariant = Color(0xFFDDE5DA),
    onSurfaceVariant = Color(0xFF414941),
    outline = Color(0xFF717970),
    outlineVariant = Color(0xFFC1C9BF),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF1F5ED),
    surfaceContainer = Color(0xFFEBEFE7),
    surfaceContainerHigh = Color(0xFFE6E9E2),
    surfaceContainerHighest = Color(0xFFE0E4DC),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF96D5A4),
    onPrimary = Color(0xFF003918),
    primaryContainer = Color(0xFF125229),
    onPrimaryContainer = Color(0xFFB2F1BF),
    secondary = Color(0xFFB7CCB7),
    onSecondary = Color(0xFF233426),
    secondaryContainer = Color(0xFF394B3B),
    onSecondaryContainer = Color(0xFFD3E8D3),
    tertiary = Color(0xFFA2CEDB),
    onTertiary = Color(0xFF033640),
    tertiaryContainer = Color(0xFF214C57),
    onTertiaryContainer = Color(0xFFBEEAF7),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF101510),
    onBackground = Color(0xFFE0E4DC),
    surface = Color(0xFF101510),
    onSurface = Color(0xFFE0E4DC),
    surfaceVariant = Color(0xFF414941),
    onSurfaceVariant = Color(0xFFC1C9BF),
    outline = Color(0xFF8B938A),
    outlineVariant = Color(0xFF414941),
    surfaceContainerLowest = Color(0xFF0B0F0B),
    surfaceContainerLow = Color(0xFF181D18),
    surfaceContainer = Color(0xFF1C211C),
    surfaceContainerHigh = Color(0xFF262B26),
    surfaceContainerHighest = Color(0xFF313631),
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

private val AppTypography = Typography().run {
    copy(
        headlineSmall = headlineSmall.copy(fontWeight = FontWeight.SemiBold),
        titleLarge = titleLarge.copy(fontWeight = FontWeight.SemiBold),
        titleMedium = titleMedium.copy(fontWeight = FontWeight.SemiBold),
        labelLarge = labelLarge.copy(fontWeight = FontWeight.SemiBold),
    )
}

@Composable
fun InventoryTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        shapes = AppShapes,
        typography = AppTypography,
        content = content,
    )
}
