package org.helix.survix.core

/**
 * The Helix colour palette (org-helix design), in two variants that share every rule:
 * - light: ink on Pale Amber, Paper cards, Red as the only action colour;
 * - dark: the dark Helix sky used by netlix — deepened ink canvas, Pale Amber text, Blush focus.
 *
 * These are the only colours in the app. The Compose theme reads them, and
 * `HelixPaletteTest` checks that every text/background pair meets WCAG AA.
 * Values are 0xAARRGGBB.
 */
data class HelixPalette(
    val dark: Boolean,
    /** Behind everything. */
    val canvas: Long,
    /** Cards, sheets, dialogs, top bar. */
    val surface: Long,
    /** A card's background under the finger, or a selected item. */
    val selected: Long,
    /** Text fields. */
    val field: Long,
    /** Titles and strong text. */
    val text: Long,
    /** Running text. */
    val body: Long,
    /** Secondary text: meta, hints, eyebrows. */
    val muted: Long,
    /** Translucent fill of neutral buttons and chips. */
    val fill: Long,
    val fillHover: Long,
    /** Separators and card outlines. Decorative only. */
    val hairline: Long,
    /** Focus ring and selection outline. */
    val ring: Long,
    val accent: Long,
    val accentHover: Long,
    val onAccent: Long,
    /** Soft accent background (chips, the icon of a block). */
    val accentSoft: Long,
    /** Text and icons on [accentSoft]. */
    val onAccentSoft: Long,
    val link: Long,
    val error: Long,
    val success: Long,
    /** Snackbars and other floating hints: the opposite of the canvas. */
    val inverse: Long,
    val onInverse: Long,
) {
    companion object {
        // ── Helix brand palette ──────────────────────────────────────────
        const val RED = 0xFFB24E30
        const val RUST = 0xFF863B24
        const val INK = 0xFF322921
        const val PALE_AMBER = 0xFFFAF6ED
        const val PAPER = 0xFFFFFFFF
        const val DUSTY_ORANGE = 0xFF635242
        const val STONE_GRAY = 0xFF918F89
        const val PALE_ORANGE = 0xFFECD3CB
        const val BLUSH = 0xFFDCAFA2

        private fun ink(alpha: Int) = (alpha.toLong() shl 24) or 0x322921L
        private fun amber(alpha: Int) = (alpha.toLong() shl 24) or 0xFAF6EDL

        val LIGHT = HelixPalette(
            dark = false,
            canvas = PALE_AMBER,
            surface = PAPER,
            selected = PALE_ORANGE,
            field = PALE_AMBER,
            text = INK,
            body = ink(217),
            muted = DUSTY_ORANGE,
            fill = ink(15),
            fillHover = ink(26),
            hairline = ink(31),
            ring = INK,
            accent = RED,
            accentHover = RUST,
            onAccent = PAPER,
            accentSoft = PALE_ORANGE,
            onAccentSoft = RUST,
            link = RED,
            error = RUST,
            // Helix: extrapolated — the palette has no success colour (same as netlix).
            success = 0xFF3E6B48,
            inverse = INK,
            onInverse = PALE_AMBER,
        )

        val DARK = HelixPalette(
            dark = true,
            canvas = 0xFF1E1914, // ink, deepened
            surface = 0xFF28201A,
            selected = 0xFF5C3223, // ink/rust step of the Helix sky
            field = 0xFF1E1914,
            text = PALE_AMBER,
            body = amber(204),
            muted = amber(166),
            fill = amber(20),
            fillHover = amber(36),
            hairline = amber(20),
            ring = BLUSH,
            accent = RED,
            accentHover = RUST,
            onAccent = PAPER,
            accentSoft = 0xFF5C3223,
            onAccentSoft = BLUSH,
            link = BLUSH,
            error = 0xFFFF8A8A,
            success = 0xFF4ADE80,
            inverse = PALE_AMBER,
            onInverse = INK,
        )

        /** WCAG 2 relative luminance of an opaque colour (alpha ignored). */
        fun luminance(argb: Long): Double {
            fun ch(shift: Int): Double {
                val v = ((argb shr shift) and 0xFF) / 255.0
                return if (v <= 0.04045) v / 12.92 else Math.pow((v + 0.055) / 1.055, 2.4)
            }
            return 0.2126 * ch(16) + 0.7152 * ch(8) + 0.0722 * ch(0)
        }

        /** Composites a translucent [fg] over an opaque [bg]. */
        fun over(fg: Long, bg: Long): Long {
            val a = ((fg shr 24) and 0xFF) / 255.0
            fun c(shift: Int): Long {
                val f = (fg shr shift) and 0xFF
                val b = (bg shr shift) and 0xFF
                return Math.round(f * a + b * (1 - a)) and 0xFF
            }
            return (0xFFL shl 24) or (c(16) shl 16) or (c(8) shl 8) or c(0)
        }

        /** WCAG contrast ratio of [fg] (composited if translucent) on an opaque [bg]. */
        fun contrast(fg: Long, bg: Long): Double {
            val x = luminance(over(fg, bg))
            val y = luminance(bg)
            return (maxOf(x, y) + 0.05) / (minOf(x, y) + 0.05)
        }
    }
}
