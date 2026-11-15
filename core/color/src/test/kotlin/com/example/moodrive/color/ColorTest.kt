package com.example.moodrive.color

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LabTest {
    @Test fun white() = assertLab(Lab(100.0, 0.0, 0.0), Lab.fromArgb(0xFFFFFFFF.toInt()))
    @Test fun black() = assertLab(Lab(0.0, 0.0, 0.0), Lab.fromArgb(0xFF000000.toInt()))
    @Test fun red() = assertLab(Lab(53.24, 80.09, 67.20), Lab.fromArgb(0xFFFF0000.toInt()))
    @Test fun blue() = assertLab(Lab(32.30, 79.19, -107.86), Lab.fromArgb(0xFF0000FF.toInt()))

    private fun assertLab(expected: Lab, actual: Lab) {
        assertEquals(expected.l, actual.l, 0.05)
        assertEquals(expected.a, actual.a, 0.05)
        assertEquals(expected.b, actual.b, 0.05)
    }
}

/** Sharma et al. (2005) CIEDE2000 검증 데이터 일부 */
class DeltaETest {
    private fun check(l1: Double, a1: Double, b1: Double, l2: Double, a2: Double, b2: Double, expected: Double) =
        assertEquals(expected, DeltaE.ciede2000(Lab(l1, a1, b1), Lab(l2, a2, b2)), 1e-4)

    @Test fun pair1() = check(50.0, 2.6772, -79.7751, 50.0, 0.0, -82.7485, 2.0425)
    @Test fun pair2() = check(50.0, 3.1571, -77.2803, 50.0, 0.0, -82.7485, 2.8615)
    @Test fun pair3() = check(50.0, 2.8361, -74.0200, 50.0, 0.0, -82.7485, 3.4412)
    @Test fun pair4() = check(50.0, -1.3802, -84.2814, 50.0, 0.0, -82.7485, 1.0000)
    @Test fun pair7() = check(50.0, 0.0, 0.0, 50.0, -1.0, 2.0, 2.3669)
    @Test fun pair17() = check(50.0, 2.5, 0.0, 73.0, 25.0, -18.0, 27.1492)
    @Test fun pair18() = check(50.0, 2.5, 0.0, 61.0, -5.0, 29.0, 22.8977)
    @Test fun pair25() = check(60.2574, -34.0099, 36.2677, 60.4626, -34.1751, 39.4387, 1.2644)

    @Test fun symmetric() {
        val a = Lab(40.0, 20.0, -30.0)
        val b = Lab(70.0, -10.0, 15.0)
        assertEquals(DeltaE.ciede2000(a, b), DeltaE.ciede2000(b, a), 1e-9)
    }
}

class PaletteExtractorTest {
    private val red = 0xFFFF0000.toInt()
    private val blue = 0xFF0000FF.toInt()

    @Test fun `두 색으로 반씩 나뉜 이미지는 두 색이 각각 50%`() {
        val pixels = IntArray(100) { if (it < 50) red else blue }
        val palette = PaletteExtractor().extract(pixels, k = 5)

        assertEquals(2, palette.size)
        assertEquals(setOf(red, blue), palette.map { it.argb }.toSet())
        palette.forEach { assertEquals(0.5f, it.weight, 1e-6f) }
    }

    @Test fun `비중 내림차순 정렬`() {
        val pixels = IntArray(100) { if (it < 70) blue else red }
        val palette = PaletteExtractor().extract(pixels, k = 2)
        assertEquals(blue, palette.first().argb)
        assertEquals(0.7f, palette.first().weight, 1e-6f)
    }

    @Test fun `투명 픽셀은 무시`() {
        val pixels = IntArray(10) { if (it < 9) 0x00000000 else red }
        assertEquals(listOf(WeightedColor(red, 1f)), PaletteExtractor().extract(pixels))
    }

    @Test fun `같은 입력이면 같은 결과`() {
        val pixels = IntArray(400) { (0xFF000000.toInt()) or (it * 997 and 0xFFFFFF) }
        assertEquals(PaletteExtractor().extract(pixels), PaletteExtractor().extract(pixels))
    }
}

class ColorMatcherTest {
    // 에뮬레이터 지원색 (값은 VehicleColors 참고)
    private val cyan = 0xFF00FFFF.toInt()
    private val blue = 0xFF0000FF.toInt()
    private val purple = 0xFF8000FF.toInt()
    private val white = 0xFFFFFFFF.toInt()
    private val supported = listOf(cyan, blue, purple, white)

    @Test fun `하늘색은 Cyan`() = assertEquals(cyan, ColorMatcher.nearest(0xFF40E0E0.toInt(), supported).matched)
    @Test fun `남색은 Blue`() = assertEquals(blue, ColorMatcher.nearest(0xFF1020B0.toInt(), supported).matched)
    @Test fun `보라는 Purple`() = assertEquals(purple, ColorMatcher.nearest(0xFF9030E0.toInt(), supported).matched)
    @Test fun `아이보리는 White`() = assertEquals(white, ColorMatcher.nearest(0xFFFFF8E7.toInt(), supported).matched)

    @Test fun `정확히 같은 색이면 deltaE 0`() = assertEquals(0.0, ColorMatcher.nearest(blue, supported).deltaE, 1e-9)

    @Test fun `팔레트는 차량이 잘 재현하는 색을 고른다`() {
        val palette = listOf(
            WeightedColor(0xFF202020.toInt(), 0.6f),  // 어두운 배경
            WeightedColor(0xFF2040F0.toInt(), 0.3f),  // 파란 포인트
        )
        val match = ColorMatcher.matchPalette(palette, supported)!!
        assertEquals(blue, match.matched)
        assertTrue(match.deltaE < 10)
    }
}
