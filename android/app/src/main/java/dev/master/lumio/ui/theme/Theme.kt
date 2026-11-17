@file:OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)

package dev.master.lumio.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.*
import androidx.compose.ui.unit.sp
import dev.master.lumio.R

val Coral = Color(0xFFF47B65)
val Ink = Color(0xFF242320)
val Paper = Color(0xFFF7F2E8)
private val Light = lightColorScheme(
    primary = Coral, onPrimary = Ink, primaryContainer = Coral, onPrimaryContainer = Ink,
    secondary = Color(0xFFA83F30), onSecondary = Paper,
    background = Paper, onBackground = Ink,
    surface = Paper, onSurface = Ink, surfaceContainer = Color(0xFFFFFCF7),
    surfaceVariant = Color(0xFFEEE7DC), onSurfaceVariant = Color(0xFF69635C),
    outline = Color(0xFF8C8378), outlineVariant = Color(0xFFD9CFC0),
    error = Color(0xFFA73838), onError = Paper,
)
private val Dark = darkColorScheme(
    primary = Coral, onPrimary = Ink, primaryContainer = Coral, onPrimaryContainer = Ink,
    secondary = Coral, onSecondary = Ink,
    background = Color(0xFF1D1C1A), onBackground = Paper,
    surface = Color(0xFF1D1C1A), onSurface = Paper, surfaceContainer = Color(0xFF292723),
    surfaceVariant = Color(0xFF34312C), onSurfaceVariant = Color(0xFFBDB4A8),
    outline = Color(0xFF8C8378), outlineVariant = Color(0xFF49443D),
    error = Color(0xFFF3A09B), onError = Ink,
)
private val Fraunces = FontFamily(Font(R.font.fraunces_variable, weight = FontWeight.SemiBold,
    variationSettings = FontVariation.Settings(FontVariation.weight(600), FontVariation.opticalSizing(30.sp),
        FontVariation.Setting("SOFT", 0f), FontVariation.Setting("WONK", 0f))))
private val DmSans = FontFamily(
    Font(R.font.dm_sans_variable, weight = FontWeight.Normal,
        variationSettings = FontVariation.Settings(FontVariation.weight(400))),
    Font(R.font.dm_sans_variable, weight = FontWeight.Medium,
        variationSettings = FontVariation.Settings(FontVariation.weight(500))),
    Font(R.font.dm_sans_variable, weight = FontWeight.SemiBold,
        variationSettings = FontVariation.Settings(FontVariation.weight(600))),
)
private fun heading(size: Int, line: Int) = TextStyle(fontFamily = Fraunces, fontWeight = FontWeight.SemiBold, fontSize = size.sp, lineHeight = line.sp)
private fun body(size: Int, line: Int, weight: FontWeight = FontWeight.Normal) = TextStyle(fontFamily = DmSans, fontWeight = weight, fontSize = size.sp, lineHeight = line.sp)
val LumioTypography = Typography(
    displayLarge = heading(36,42), displayMedium = heading(36,42), displaySmall = heading(30,36),
    headlineLarge = heading(30,36), headlineMedium = heading(24,30), headlineSmall = heading(20,26),
    titleLarge = heading(20,26), titleMedium = body(16,22,FontWeight.SemiBold), titleSmall = body(14,20,FontWeight.Medium),
    bodyLarge = body(17,26), bodyMedium = body(14,20), bodySmall = body(13,18),
    labelLarge = body(16,22,FontWeight.SemiBold), labelMedium = body(13,18,FontWeight.Medium), labelSmall = body(13,18,FontWeight.Medium),
)
val LocalLumioDark = staticCompositionLocalOf { false }
@Composable fun LumioTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalLumioDark provides dark) {
        MaterialTheme(colorScheme = if(dark) Dark else Light, typography = LumioTypography, content = content)
    }
}
