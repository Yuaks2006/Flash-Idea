package com.flashidea.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val LightColorScheme = lightColorScheme(
    primary          = PrimaryColor,
    onPrimary        = Color.White,
    secondary        = SecondaryColor,
    onSecondary      = Color.White,
    tertiary         = AccentColor,
    background       = BackgroundColor,
    surface          = SurfaceColor,
    surfaceVariant   = SurfaceVariant,
    onBackground     = OnSurface,
    onSurface        = OnSurface,
    onSurfaceVariant = OnSurfaceVariant,
    outline = Color(0xFFB5C1CB),
    outlineVariant = Color(0xFFD4DEE7),
    primaryContainer = Color(0xFFD8EEF5),
    onPrimaryContainer = Color(0xFF123445),
    secondaryContainer = Color(0xFFE2E9EF),
    onSecondaryContainer = Color(0xFF243746),
    tertiaryContainer = Color(0xFFE8F8FB),
    onTertiaryContainer = Color(0xFF123E48),
    surfaceContainerLowest = Color(0xFFEAF0F5),
    surfaceContainerLow = Color(0xFFF1F5F8),
    surfaceContainer = Color(0xFFE8EEF3),
    surfaceContainerHigh = Color(0xFFDDE6ED),
)

private val DarkColorScheme = darkColorScheme(
    primary          = DarkPrimaryColor,
    onPrimary        = Color(0xFF062332),
    secondary        = DarkSecondaryColor,
    onSecondary      = Color(0xFF13202A),
    tertiary         = Color(0xFF7DF9FF),
    background       = DarkBackgroundColor,
    surface          = DarkSurfaceColor,
    surfaceVariant   = DarkSurfaceVariant,
    onBackground     = DarkOnSurface,
    onSurface        = DarkOnSurface,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = Color(0xFF485765),
    outlineVariant = Color(0xFF232F39),
    primaryContainer = Color(0xFF143545),
    onPrimaryContainer = Color(0xFFD9F6FF),
    secondaryContainer = Color(0xFF17222C),
    onSecondaryContainer = Color(0xFFD9E5EF),
    tertiaryContainer = Color(0xFF143940),
    onTertiaryContainer = Color(0xFFE5FCFF),
    surfaceContainerLowest = Color(0xFF080B10),
    surfaceContainerLow = Color(0xFF0E1319),
    surfaceContainer = Color(0xFF151D25),
    surfaceContainerHigh = Color(0xFF202B35),
)

private val FlashTypography = Typography(
    headlineMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 30.sp
    ),
    headlineSmall = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 19.sp,
        lineHeight = 25.sp
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 21.sp
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 25.sp
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 21.sp
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp
    ),
    labelMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    labelSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp
    )
)

private val FlashShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun FlashIdeaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography  = FlashTypography,
        shapes = FlashShapes,
        content     = content
    )
}
