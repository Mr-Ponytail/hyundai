package com.example.moodrive.color

import kotlin.math.cbrt
import kotlin.math.pow

/** CIELAB (D65) 색. 사람 눈의 색 차이에 가깝게 거리를 잴 수 있어 팔레트 추출·매핑의 기준 공간으로 쓴다. */
data class Lab(val l: Double, val a: Double, val b: Double) {

    companion object {
        // D65 기준 백색점
        private const val XN = 0.95047
        private const val YN = 1.00000
        private const val ZN = 1.08883

        /** sRGB 0xAARRGGBB → Lab. 알파는 무시한다. */
        fun fromArgb(argb: Int): Lab {
            val r = linearize((argb shr 16) and 0xFF)
            val g = linearize((argb shr 8) and 0xFF)
            val b = linearize(argb and 0xFF)

            val x = (0.4124564 * r + 0.3575761 * g + 0.1804375 * b) / XN
            val y = (0.2126729 * r + 0.7151522 * g + 0.0721750 * b) / YN
            val z = (0.0193339 * r + 0.1191920 * g + 0.9503041 * b) / ZN

            val fx = f(x)
            val fy = f(y)
            val fz = f(z)
            return Lab(116 * fy - 16, 500 * (fx - fy), 200 * (fy - fz))
        }

        private fun linearize(channel: Int): Double {
            val c = channel / 255.0
            return if (c <= 0.04045) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
        }

        private fun f(t: Double): Double =
            if (t > 216.0 / 24389.0) cbrt(t) else (24389.0 / 27.0 * t + 16) / 116
    }
}
