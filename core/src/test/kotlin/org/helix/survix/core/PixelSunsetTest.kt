package org.helix.survix.core

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PixelSunsetTest {
    @Test
    fun `paints every pixel opaque and is deterministic`() {
        val a = PixelSunset.paint(90, 56)
        assertEquals(90 * 56, a.size)
        assertTrue(a.all { (it ushr 24) == 0xFF })
        assertContentEquals(a, PixelSunset.paint(90, 56))
    }

    @Test
    fun `top is ink sky, the sun is Pale Amber, the bottom is a hill`() {
        val w = 120
        val h = 80
        val p = PixelSunset.paint(w, h)
        fun rgb(x: Int, y: Int) = p[y * w + x] and 0xFFFFFF
        assertEquals(0x322921, rgb(1, 0))
        assertEquals(0x322921, rgb(w / 2, h - 1))
        assertTrue(p.any { (it and 0xFFFFFF) == 0xFAF6ED }, "sun or warm star")
    }

    @Test
    fun `tiny sizes do not crash`() {
        for (w in 1..12) for (h in 1..12) PixelSunset.paint(w, h)
    }
}
