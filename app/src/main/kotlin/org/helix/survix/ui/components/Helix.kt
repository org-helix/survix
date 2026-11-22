package org.helix.survix.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import org.helix.survix.R
import org.helix.survix.core.PixelSunset
import org.helix.survix.ui.theme.Helix
import org.helix.survix.ui.theme.Radii
import kotlin.math.ceil

/** Small label above a title or a group: sentence case, parts joined by a middle dot. */
@Composable
fun Eyebrow(vararg parts: String, modifier: Modifier = Modifier, color: Color = Helix.colors.muted) {
    Text(
        text = parts.joinToString(" · "),
        modifier = modifier,
        style = MaterialTheme.typography.labelMedium,
        color = color,
    )
}

/** The Helix mark: two pixel-aligned strands crossing (28 × 20 grid, as on the website). */
@Composable
fun HelixMark(modifier: Modifier = Modifier, height: Dp = 16.dp) {
    val c = Helix.colors
    val blush = Color(org.helix.survix.core.HelixPalette.BLUSH)
    Canvas(modifier.size(width = height * 1.4f, height = height)) {
        val u = size.height / 20f
        fun r(x: Int, y: Int, w: Int, h: Int, color: Color) =
            drawRect(color, topLeft = Offset(x * u, y * u), size = Size(w * u, h * u))
        // strand B (behind)
        r(0, 14, 8, 4, blush); r(8, 10, 4, 4, blush); r(16, 6, 4, 4, blush); r(20, 2, 8, 4, blush)
        // strand A
        r(0, 2, 8, 4, c.accent); r(8, 6, 4, 4, c.accent); r(12, 8, 4, 4, c.accent)
        r(16, 10, 4, 4, c.accent); r(20, 14, 8, 4, c.accent)
    }
}

/** A card: Paper (or the dark plate) with a hairline outline, never a shadow. */
@Composable
fun HelixCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    selected: Boolean = false,
    contentPadding: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = Helix.colors
    val border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) c.ring else c.hairline)
    val inner: @Composable () -> Unit = { Column(Modifier.padding(contentPadding), content = content) }
    if (onClick != null) {
        Surface(onClick = onClick, modifier = modifier, shape = Radii.xl, color = c.surface, contentColor = c.text, border = border, content = inner)
    } else {
        Surface(modifier = modifier, shape = Radii.xl, color = c.surface, contentColor = c.text, border = border, content = inner)
    }
}

/**
 * A text field in the Helix style: a label above, a filled field with no underline,
 * and a 2 dp ring when focused.
 */
@Composable
fun HelixField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String?,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    singleLine: Boolean = true,
    minLines: Int = 1,
    textStyle: TextStyle = MaterialTheme.typography.bodyLarge,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    error: String? = null,
    suffix: String? = null,
    onSurface: Boolean = true,
) {
    val c = Helix.colors
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    Column(modifier) {
        if (label != null) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = c.muted)
            Spacer(Modifier.height(6.dp))
        }
        val container = if (onSurface) c.field else c.surface
        TextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = if (focused) 2.dp else 1.dp,
                    color = when {
                        error != null -> c.error
                        focused -> c.ring
                        else -> c.hairline
                    },
                    shape = Radii.md,
                )
                .semantics { if (label != null) contentDescription = label },
            textStyle = textStyle,
            placeholder = placeholder?.let { { Text(it, style = textStyle, color = c.muted) } },
            suffix = suffix?.let { { Text(it, color = c.muted) } },
            singleLine = singleLine,
            minLines = minLines,
            isError = error != null,
            keyboardOptions = keyboardOptions,
            interactionSource = interaction,
            shape = Radii.md,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = container,
                unfocusedContainerColor = container,
                errorContainerColor = container,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                errorIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                focusedTextColor = c.text,
                unfocusedTextColor = c.text,
                errorTextColor = c.text,
                cursorColor = c.ring,
                errorCursorColor = c.error,
            ),
        )
        if (error != null) {
            Spacer(Modifier.height(4.dp))
            Text(error, style = MaterialTheme.typography.bodySmall, color = c.error)
        }
    }
}

/** A row with a label and a switch; the whole row toggles. */
@Composable
fun SwitchRow(text: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val c = Helix.colors
    Row(
        modifier
            .fillMaxWidth()
            .clip(Radii.md)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = c.text)
        Switch(
            checked = checked,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(
                checkedTrackColor = c.accent,
                checkedThumbColor = c.onAccent,
                uncheckedTrackColor = c.fill,
                uncheckedThumbColor = c.muted,
                uncheckedBorderColor = c.muted,
            ),
        )
    }
}

/** One option among a few, as a tile (welcome screen and settings). */
@Composable
fun ChoiceTile(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    val c = Helix.colors
    Surface(
        modifier = modifier
            .clip(Radii.lg)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
        shape = Radii.lg,
        color = if (selected) c.selected else c.fill,
        contentColor = c.text,
        border = if (selected) BorderStroke(2.dp, c.ring) else null,
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (icon != null) Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Text(
                text,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium),
            )
        }
    }
}

/** A square badge holding an icon: Pale Orange with Rust in light, rust step with Blush in dark. */
@Composable
fun IconBadge(icon: ImageVector, modifier: Modifier = Modifier, size: Dp = 36.dp) {
    val c = Helix.colors
    Box(
        modifier
            .size(size)
            .clip(Radii.md)
            .background(c.accentSoft),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = c.onAccentSoft, modifier = Modifier.size(size * 0.55f))
    }
}

/**
 * The Helix pixel-art sunset, painted at one logical pixel per [pixel] dp and scaled
 * up without smoothing. Decorative: described once for screen readers.
 */
@Composable
fun PixelSunsetImage(modifier: Modifier = Modifier, pixel: Dp = 4.dp) {
    val description = androidx.compose.ui.res.stringResource(R.string.illustration_description)
    Spacer(
        modifier
            .clip(Radii.xl)
            .semantics { contentDescription = description }
            .drawWithCache {
                val p = pixel.toPx().coerceAtLeast(1f)
                val w = ceil(size.width / p).toInt().coerceAtLeast(1)
                val h = ceil(size.height / p).toInt().coerceAtLeast(1)
                val pixels = PixelSunset.paint(w, h)
                val bitmap = Bitmap.createBitmap(pixels, w, h, Bitmap.Config.ARGB_8888).asImageBitmap()
                onDrawBehind {
                    drawImage(
                        image = bitmap,
                        srcOffset = IntOffset.Zero,
                        srcSize = IntSize(w, h),
                        dstOffset = IntOffset.Zero,
                        dstSize = IntSize(ceil(w * p).toInt(), ceil(h * p).toInt()),
                        filterQuality = FilterQuality.None,
                    )
                }
            },
    )
}
