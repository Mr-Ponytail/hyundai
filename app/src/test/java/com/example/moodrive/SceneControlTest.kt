package com.example.moodrive

import com.example.moodrive.domain.model.FanDirection
import com.example.moodrive.ui.common.DetailKind
import com.example.moodrive.ui.control.ControlTargets
import com.example.moodrive.ui.control.SceneControlFormatter
import org.junit.Assert.assertEquals
import org.junit.Test

class SceneControlTest {

    private val base = ControlTargets(
        brightnessPercent = 50, colorArgb = 0xFF8000FF.toInt(),
        seatHeat = 2, fanSpeed = 1, fanDirection = FanDirection.FLOOR, steeringHeat = 1,
    )

    /** 시안: 앰비언트 50% · 시트 2단 · 바람 1단 · 핸들 열선 ON */
    @Test fun `시안 예시`() {
        val cards = SceneControlFormatter.cards(base)
        assertEquals(listOf("50", "2", "1", "ON"), cards.map { it.value })
        assertEquals(listOf("%", "단", "단", null), cards.map { it.unit })
    }

    @Test fun `조명 미지원이면 미지원 표시`() {
        assertEquals("미지원", SceneControlFormatter.valueOf(DetailKind.AMBIENT, ControlTargets()).first)
    }

    @Test fun `값이 없는 공조 항목은 유지, 0은 OFF`() {
        val t = ControlTargets(seatHeat = 0)
        assertEquals("OFF", SceneControlFormatter.valueOf(DetailKind.SEAT, t).first)
        assertEquals("유지", SceneControlFormatter.valueOf(DetailKind.WIND, t).first)
        // 핸들 열선은 유지 없이 ON/OFF만
        assertEquals("OFF", SceneControlFormatter.valueOf(DetailKind.STEERING, t).first)
    }
}
