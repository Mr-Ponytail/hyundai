package com.example.moodrive

import com.example.moodrive.color.WeightedColor
import com.example.moodrive.domain.model.AmbientLightState
import com.example.moodrive.domain.model.ClimateState
import com.example.moodrive.domain.model.Comfort
import com.example.moodrive.domain.model.FanDirection
import com.example.moodrive.domain.model.Scene
import com.example.moodrive.domain.model.Visual
import com.example.moodrive.ui.common.DetailKind
import com.example.moodrive.ui.home.DetailItemUi
import com.example.moodrive.ui.home.SceneDetailFormatter
import org.junit.Assert.assertEquals
import org.junit.Test

class SceneDetailFormatterTest {

    private fun scene(comfort: Comfort?) = Scene(
        id = "s", name = "Cozy Fireplace", authorId = "u",
        visual = Visual(palette = listOf(WeightedColor(0xFF8E2A22.toInt(), 1f)), brightnessPercent = 50),
        comfort = comfort,
    )

    /** 시안과 같은 값 */
    @Test fun `시안 예시`() {
        val items = SceneDetailFormatter.items(
            scene(Comfort(fanSpeed = 1, fanDirection = FanDirection.FLOOR, seatHeatLevel = 2, steeringWheelHeatLevel = 1)),
            light = AmbientLightState(on = true, colorArgb = 0, brightness = 30),
            brightnessRange = 0..100,
            climate = ClimateState(fanSpeed = 2, fanDirection = FanDirection.FACE, seatHeatLevel = 0, steeringWheelHeatLevel = 0),
        )
        assertEquals(
            listOf(
                DetailItemUi(DetailKind.AMBIENT, "현재 30%", "50", "%"),
                DetailItemUi(DetailKind.SEAT, "현재 OFF", "2", "단 · 열선"),
                DetailItemUi(DetailKind.WIND, "현재 2단 · 얼굴", "1", "단 · 발 방향"),
                DetailItemUi(DetailKind.STEERING, "현재 OFF", "ON", null),
            ),
            items,
        )
        assertEquals(listOf("앰비언트", "시트", "바람", "핸들 열선"), items.map { it.kind.label })
    }

    @Test fun `공조 값이 없으면 유지, 차량 값을 모르면 현재 -`() {
        val items = SceneDetailFormatter.items(scene(null), light = null, brightnessRange = null, climate = null)
        assertEquals(listOf("현재 -", "현재 -", "현재 -", "현재 -"), items.map { it.current })
        assertEquals(listOf("50", "유지", "유지", "OFF"), items.map { it.value })
    }

    @Test fun `밝기는 차량 범위를 퍼센트로 환산`() {
        val items = SceneDetailFormatter.items(scene(null), AmbientLightState(true, 0, 5), 0..10, null)
        assertEquals("현재 50%", items.first().current)
    }

    @Test fun `조명이 꺼져 있으면 OFF`() {
        val items = SceneDetailFormatter.items(scene(null), AmbientLightState(false, 0, 5), 0..10, null)
        assertEquals("현재 OFF", items.first().current)
    }

    @Test fun `풍량 0은 OFF`() {
        val wind = SceneDetailFormatter.items(scene(Comfort(fanSpeed = 0)), null, null, ClimateState(fanSpeed = 0))[2]
        assertEquals("현재 OFF", wind.current)
        assertEquals("OFF", wind.value)
    }
}
