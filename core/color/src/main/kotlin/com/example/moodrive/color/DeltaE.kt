package com.example.moodrive.color

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/** CIEDE2000 색차. (Sharma, Wu, Dalal 2005 구현 기준, kL = kC = kH = 1) */
object DeltaE {

    fun ciede2000(lab1: Lab, lab2: Lab): Double {
        val c1 = sqrt(lab1.a * lab1.a + lab1.b * lab1.b)
        val c2 = sqrt(lab2.a * lab2.a + lab2.b * lab2.b)
        val cBar7 = ((c1 + c2) / 2).pow(7)
        val g = 0.5 * (1 - sqrt(cBar7 / (cBar7 + POW25_7)))

        val a1p = (1 + g) * lab1.a
        val a2p = (1 + g) * lab2.a
        val c1p = sqrt(a1p * a1p + lab1.b * lab1.b)
        val c2p = sqrt(a2p * a2p + lab2.b * lab2.b)
        val h1p = hueDegrees(lab1.b, a1p)
        val h2p = hueDegrees(lab2.b, a2p)

        val dLp = lab2.l - lab1.l
        val dCp = c2p - c1p
        val dhp = when {
            c1p * c2p == 0.0 -> 0.0
            abs(h2p - h1p) <= 180 -> h2p - h1p
            h2p - h1p > 180 -> h2p - h1p - 360
            else -> h2p - h1p + 360
        }
        val dHp = 2 * sqrt(c1p * c2p) * sin(rad(dhp / 2))

        val lBarP = (lab1.l + lab2.l) / 2
        val cBarP = (c1p + c2p) / 2
        val hBarP = when {
            c1p * c2p == 0.0 -> h1p + h2p
            abs(h1p - h2p) <= 180 -> (h1p + h2p) / 2
            h1p + h2p < 360 -> (h1p + h2p + 360) / 2
            else -> (h1p + h2p - 360) / 2
        }

        val t = 1 - 0.17 * cos(rad(hBarP - 30)) + 0.24 * cos(rad(2 * hBarP)) +
            0.32 * cos(rad(3 * hBarP + 6)) - 0.20 * cos(rad(4 * hBarP - 63))
        val dTheta = 30 * exp(-((hBarP - 275) / 25).pow(2))
        val cBarP7 = cBarP.pow(7)
        val rc = 2 * sqrt(cBarP7 / (cBarP7 + POW25_7))
        val lBarP50 = (lBarP - 50).pow(2)
        val sl = 1 + 0.015 * lBarP50 / sqrt(20 + lBarP50)
        val sc = 1 + 0.045 * cBarP
        val sh = 1 + 0.015 * cBarP * t
        val rt = -sin(rad(2 * dTheta)) * rc

        val dl = dLp / sl
        val dc = dCp / sc
        val dh = dHp / sh
        return sqrt(dl * dl + dc * dc + dh * dh + rt * dc * dh)
    }

    fun ciede2000(argb1: Int, argb2: Int): Double = ciede2000(Lab.fromArgb(argb1), Lab.fromArgb(argb2))

    private const val POW25_7 = 6103515625.0 // 25^7

    private fun hueDegrees(b: Double, aPrime: Double): Double {
        if (b == 0.0 && aPrime == 0.0) return 0.0
        val h = Math.toDegrees(atan2(b, aPrime))
        return if (h < 0) h + 360 else h
    }

    private fun rad(deg: Double) = Math.toRadians(deg)
}
