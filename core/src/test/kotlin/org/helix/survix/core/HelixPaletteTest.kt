package org.helix.survix.core

import org.helix.survix.core.HelixPalette.Companion.contrast
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Every text/background pair the app uses meets WCAG AA (4.5:1), in both themes. */
class HelixPaletteTest {

    @Test
    fun `text pairs meet WCAG AA`() {
        for (p in listOf(HelixPalette.LIGHT, HelixPalette.DARK)) {
            val name = if (p.dark) "dark" else "light"
            val pairs = listOf(
                Triple(p.text, p.canvas, "text on canvas"),
                Triple(p.text, p.surface, "text on surface"),
                Triple(p.body, p.canvas, "body on canvas"),
                Triple(p.body, p.surface, "body on surface"),
                Triple(p.muted, p.canvas, "muted on canvas"),
                Triple(p.muted, p.surface, "muted on surface"),
                Triple(p.text, p.field, "text in a field"),
                Triple(p.text, p.selected, "text on selection"),
                Triple(p.text, HelixPalette.over(p.fill, p.canvas), "neutral button"),
                Triple(p.text, HelixPalette.over(p.fillHover, p.surface), "pressed neutral button"),
                Triple(p.onAccent, p.accent, "primary button"),
                Triple(p.onAccent, p.accentHover, "pressed primary button"),
                Triple(p.onAccentSoft, p.accentSoft, "block icon"),
                Triple(p.link, p.surface, "link on surface"),
                Triple(p.link, p.canvas, "link on canvas"),
                Triple(p.error, p.surface, "error text"),
                Triple(p.success, p.surface, "success text"),
                Triple(p.onInverse, p.inverse, "snackbar"),
            )
            for ((fg, bg, what) in pairs) {
                val ratio = contrast(fg, bg)
                assertTrue(ratio >= 4.5, "$name: $what is ${"%.2f".format(ratio)}:1")
            }
            // The focus ring must stand out from what it surrounds (WCAG 1.4.11: 3:1).
            assertTrue(contrast(p.ring, p.surface) >= 3.0, "$name: focus ring")
            assertTrue(contrast(p.accent, p.canvas) >= 3.0 || p.dark, "$name: primary button edge")
        }
    }

    @Test
    fun `brand colours are the Helix ones`() {
        assertEquals(0xFFB24E30, HelixPalette.LIGHT.accent)
        assertEquals(0xFFFAF6ED, HelixPalette.LIGHT.canvas)
        assertEquals(0xFF322921, HelixPalette.LIGHT.text)
    }

    @Test
    fun compositing() {
        assertEquals(0xFFFFFFFF, HelixPalette.over(0x00000000, 0xFFFFFFFF))
        assertEquals(0xFF000000, HelixPalette.over(0xFF000000, 0xFFFFFFFF))
        assertEquals(21.0, contrast(0xFF000000, 0xFFFFFFFF), 0.01)
    }
}
