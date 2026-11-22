package org.helix.survix.core

import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToLong
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * The Helix pixel-art sunset (port of Dusk DS `pixel-sunset.js`, as on the Helix website):
 * a dithered sky, a few stars, a half sun cut by horizontal bands, cloud strips and three
 * hills. [paint] returns one opaque ARGB pixel per logical pixel; the app scales it up
 * without smoothing.
 */
object PixelSunset {

    // Colours of the illustration, from the Helix theme (`--color-sunset-*`).
    private val SKY = longArrayOf(0x322921, 0x5C3223, 0x863B24, 0x9C452A, 0xB24E30, 0xDCAFA2)
    private val SUN = longArrayOf(0xFAF6ED, 0xFAF6ED, 0xECD3CB, 0xDCAFA2)
    private val HILLS = longArrayOf(0x583122, 0x432D22, 0x322921)
    private val STARS = longArrayOf(0x918F89, 0xFAF6ED)

    private val BAYER = arrayOf(
        intArrayOf(0, 8, 2, 10),
        intArrayOf(12, 4, 14, 6),
        intArrayOf(3, 11, 1, 9),
        intArrayOf(15, 7, 13, 5),
    )

    /** Centre x, y and width of each cloud strip, as ratios of the picture. */
    private val CLOUDS = arrayOf(
        doubleArrayOf(0.18, 0.7, 0.16),
        doubleArrayOf(0.32, 0.74, 0.12),
        doubleArrayOf(0.8, 0.72, 0.2),
        doubleArrayOf(0.5, 0.77, 0.08),
    )

    private fun mix(a: Long, b: Long, t: Double): Long {
        fun ch(shift: Int): Long {
            val x = (a shr shift) and 0xFF
            val y = (b shr shift) and 0xFF
            return (x + (y - x) * t).roundToLong() and 0xFF
        }
        return (ch(16) shl 16) or (ch(8) shl 8) or ch(0)
    }

    private fun at(list: LongArray, i: Int) = list[min(list.size - 1, max(0, i))]

    private fun noise1(x: Double, seed: Int): Double {
        val i = floor(x)
        val f = x - i
        fun hash(n: Double): Double {
            val v = sin((n + seed) * 127.1) * 43758.5453
            return v - floor(v)
        }
        val u = f * f * (3 - 2 * f)
        return hash(i) * (1 - u) + hash(i + 1) * u
    }

    fun paint(
        w: Int,
        h: Int,
        horizonRatio: Double = 0.8,
        sunXRatio: Double = 0.63,
        sunRatio: Double = 0.13,
        starDensity: Double = 0.0006,
        seed: Long = 3,
    ): IntArray {
        require(w > 0 && h > 0)
        val data = IntArray(w * h)
        fun put(x: Int, y: Int, rgb: Long) {
            if (x < 0 || y < 0 || x >= w || y >= h) return
            data[y * w + x] = (0xFF000000L or rgb).toInt()
        }

        val horizon = floor(h * horizonRatio).toInt()
        val sunX = floor(w * sunXRatio).toInt()
        val sunR = max(10, floor(min(w, h) * sunRatio).toInt())

        // 1. Sky — continuous gradient, 4 quantised steps per band, Bayer-dithered.
        for (y in 0 until h) {
            for (x in 0 until w) {
                val t = min(1.0, max(0.0, (y.toDouble() / horizon - 0.32) / 0.68)) * (SKY.size - 1)
                val i = min(SKY.size - 2, floor(t).toInt())
                val f = t - i
                val dither = (BAYER[y % 4][x % 4] + 0.5) / 16
                val q = min(4, max(0, floor(f * 4 + dither).toInt())) / 4.0
                put(x, y, mix(at(SKY, i), at(SKY, i + 1), q))
            }
        }

        // 2. Stars — sparse, mostly dim, a few warm.
        var s = seed
        fun random(): Double {
            s = (s * 48271) % 2147483647
            return s / 2147483647.0
        }
        val starCount = w * h * starDensity
        var k = 0
        while (k < starCount) {
            val x = floor(random() * w).toInt()
            val y = floor(random() * horizon * 0.6).toInt()
            put(x, y, at(STARS, if (random() > 0.9) 1 else 0))
            k++
        }

        // 3. Sun — half disc, horizontal cuts thickening toward the bottom.
        for (y in (horizon - sunR) until (horizon + floor(sunR * 0.35).toInt())) {
            val dy = (y - horizon).toDouble()
            val rr = sqrt(max(0.0, sunR.toDouble() * sunR - if (dy < 0) dy * dy else (dy * 0.6) * (dy * 0.6)))
            val rel = (y - (horizon - sunR)) / (sunR * 1.35)
            if (rel > 0.55 && y % max(3L, Math.round(6 - rel * 4)) == 0L) continue
            val color = at(SUN, floor(rel * 4).toInt())
            var x = floor(sunX - rr).toInt()
            while (x <= sunX + rr) {
                put(x, y, color)
                x++
            }
        }

        // 4. Cloud strips.
        for ((cx, cy, cw) in CLOUDS.map { Triple(it[0], it[1], it[2]) }) {
            val y0 = floor(h * cy).toInt()
            val x0 = floor(w * (cx - cw / 2)).toInt()
            val x1 = floor(w * (cx + cw / 2)).toInt()
            for (x in x0 until x1) {
                put(x, y0, mix(at(SKY, 3), at(SUN, 2), 0.55))
                if (x > x0 + 4 && x < x1 - 6) put(x, y0 - 1, mix(at(SKY, 2), at(SKY, 3), 0.6))
            }
        }

        // 5. Hills — three stepped-noise silhouettes, small trees on the nearest layer.
        HILLS.forEachIndexed { layer, color ->
            val base = horizon + floor(h * (0.02 + layer * 0.07)).toInt()
            val amplitude = h * (0.06 + layer * 0.03)
            for (x in 0 until w) {
                val step = (x / (3 + layer)) * (3 + layer)
                val top = floor(base - noise1(step.toDouble() / (14 + layer * 6), layer * 11) * amplitude).toInt()
                for (y in max(0, top) until h) put(x, y, color)
                if (layer == 2 && noise1(x / 3.0, 99) > 0.92) {
                    for (y in top - 6 until top) put(x, y, color)
                    put(x - 1, top - 4, color)
                    put(x + 1, top - 4, color)
                }
            }
        }
        return data
    }
}
