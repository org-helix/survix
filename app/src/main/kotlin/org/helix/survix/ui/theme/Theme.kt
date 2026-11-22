package org.helix.survix.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.unit.dp
import org.helix.survix.core.HelixPalette

/**
 * Helix colours as Compose colours. UI code reads them through [Helix.colors]
 * and never writes a colour of its own (the palette lives in :core, where its
 * contrast is tested).
 */
@Immutable
class HelixColors(p: HelixPalette) {
    val dark = p.dark
    val canvas = Color(p.canvas)
    val surface = Color(p.surface)
    val selected = Color(p.selected)
    val field = Color(p.field)
    val text = Color(p.text)
    val body = Color(p.body)
    val muted = Color(p.muted)
    val fill = Color(p.fill)
    val fillHover = Color(p.fillHover)
    val hairline = Color(p.hairline)
    val ring = Color(p.ring)
    val accent = Color(p.accent)
    val accentHover = Color(p.accentHover)
    val onAccent = Color(p.onAccent)
    val accentSoft = Color(p.accentSoft)
    val onAccentSoft = Color(p.onAccentSoft)
    val link = Color(p.link)
    val error = Color(p.error)
    val success = Color(p.success)
    val inverse = Color(p.inverse)
    val onInverse = Color(p.onInverse)
}

private val LightColors = HelixColors(HelixPalette.LIGHT)
private val DarkColors = HelixColors(HelixPalette.DARK)

val LocalHelixColors = staticCompositionLocalOf { LightColors }

object Helix {
    val colors: HelixColors
        @Composable get() = LocalHelixColors.current
}

/** Dusk radii: 6 / 8 / 12 / 16 / 28, and 10 for call-to-action buttons. */
object Radii {
    val sm = RoundedCornerShape(6.dp)
    val md = RoundedCornerShape(8.dp)
    val cta = RoundedCornerShape(10.dp)
    val lg = RoundedCornerShape(12.dp)
    val xl = RoundedCornerShape(16.dp)
}

private fun scheme(c: HelixColors) = (if (c.dark) darkColorScheme() else lightColorScheme()).copy(
    primary = c.accent,
    onPrimary = c.onAccent,
    primaryContainer = c.accentSoft,
    onPrimaryContainer = c.onAccentSoft,
    inversePrimary = Color(HelixPalette.BLUSH),
    secondary = c.muted,
    onSecondary = c.surface,
    secondaryContainer = c.selected,
    onSecondaryContainer = c.text,
    tertiary = c.accentHover,
    onTertiary = c.onAccent,
    tertiaryContainer = c.selected,
    onTertiaryContainer = c.text,
    background = c.canvas,
    onBackground = c.text,
    surface = c.canvas,
    onSurface = c.text,
    surfaceVariant = c.fill.compositeOver(c.surface),
    onSurfaceVariant = c.muted,
    // No tonal tint: depth comes from translucent fills, as in Dusk.
    surfaceTint = Color.Transparent,
    surfaceBright = c.surface,
    surfaceDim = c.canvas,
    surfaceContainerLowest = c.surface,
    surfaceContainerLow = c.surface,
    surfaceContainer = c.surface,
    surfaceContainerHigh = c.surface,
    surfaceContainerHighest = c.surface,
    inverseSurface = c.inverse,
    inverseOnSurface = c.onInverse,
    outline = c.muted,
    outlineVariant = c.hairline.compositeOver(c.surface),
    error = c.error,
    onError = c.surface,
    errorContainer = c.selected,
    onErrorContainer = c.text,
)

@Composable
fun SurvixTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colors = if (dark) DarkColors else LightColors
    CompositionLocalProvider(LocalHelixColors provides colors) {
        MaterialTheme(
            colorScheme = scheme(colors),
            typography = SurvixTypography,
            shapes = Shapes(
                extraSmall = Radii.sm,
                small = Radii.md,
                medium = Radii.lg,
                large = Radii.xl,
                extraLarge = RoundedCornerShape(28.dp),
            ),
            content = content,
        )
    }
}
