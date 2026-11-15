package com.example.moodrive.domain

import com.example.moodrive.color.WeightedColor
import com.example.moodrive.domain.model.Access
import com.example.moodrive.domain.model.AmbientColors
import com.example.moodrive.domain.model.ClimateCapabilities
import com.example.moodrive.domain.model.Comfort
import com.example.moodrive.domain.model.LightCapabilities
import com.example.moodrive.domain.model.Scene
import com.example.moodrive.domain.model.Visual
import com.example.moodrive.domain.session.Feature
import com.example.moodrive.domain.session.ScenePlanner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScenePlannerTest {
    private val lightCaps = LightCapabilities(Access.READ_WRITE, 0..10, listOf(CYAN, BLUE, PURPLE, WHITE))
    private val climateCaps = ClimateCapabilities(Access.READ_WRITE, 16f..30f, 0..8)

    private val scene = Scene(
        id = "s1", name = "가을 바다 노을", authorId = "u1",
        visual = Visual(palette = listOf(WeightedColor(0xFF2A3BD8.toInt(), 1f)), brightnessPercent = 70),
        comfort = Comfort(temperatureC = 35f, fanSpeed = 3, seatHeatLevel = 9, steeringWheelHeatLevel = 1),
    )

    @Test fun `팔레트를 지원 색으로 매핑하고 밝기를 차량 범위로 환산`() {
        val plan = ScenePlanner.plan(scene, lightCaps, climateCaps, applyComfort = false)
        assertEquals(BLUE, plan.light?.colorArgb)
        assertEquals(7, plan.light?.brightness)
        assertEquals(true, plan.light?.on)
        assertEquals(false, plan.light?.autoColorByDriveMode)
    }

    @Test fun `공조는 승인하지 않으면 계획에서 빠진다`() {
        assertNull(ScenePlanner.plan(scene, lightCaps, climateCaps, applyComfort = false).climate)
    }

    @Test fun `승인하면 공조 값은 차량 범위로 제한`() {
        val plan = ScenePlanner.plan(scene, lightCaps, climateCaps, applyComfort = true)
        assertEquals(30f, plan.climate?.temperatureC)
        assertEquals(3, plan.climate?.fanSpeed)
        assertEquals(3, plan.climate?.seatHeatLevel)
        assertEquals(1, plan.climate?.steeringWheelHeatLevel)
    }

    @Test fun `조명 미지원 차량은 이유와 함께 제외`() {
        val plan = ScenePlanner.plan(scene, lightCaps.copy(access = Access.NONE), climateCaps, applyComfort = false)
        assertNull(plan.light)
        assertTrue(Feature.LIGHT in plan.excluded)
    }

    @Test fun `조회만 가능해도 계획은 세운다 (안내 모드로 적용)`() {
        val plan = ScenePlanner.plan(scene, lightCaps.copy(access = Access.READ), climateCaps, applyComfort = false)
        assertEquals(BLUE, plan.light?.colorArgb)
    }

    @Test fun `무드 수정에서 고른 색이 팔레트보다 우선`() {
        val edited = scene.copy(visual = scene.visual.copy(ambient = AmbientColors(main = 0xFF8F6BFF.toInt(), sub = 0xFFFFE0B8.toInt())))
        val plan = ScenePlanner.plan(edited, lightCaps, climateCaps, applyComfort = false)
        assertEquals(PURPLE, plan.light?.colorArgb)
        assertEquals(0xFF8F6BFF.toInt(), plan.colorMatch?.source)
    }

    @Test fun `앰비언트를 끈 무드는 조명을 끄도록 계획`() {
        val off = scene.copy(visual = scene.visual.copy(lightOn = false))
        assertEquals(false, ScenePlanner.plan(off, lightCaps, climateCaps, applyComfort = false).light?.on)
    }

    @Test fun `통풍은 지원하는 차량에서만 계획에 넣는다`() {
        val vent = scene.copy(comfort = Comfort(seatHeatLevel = 0, seatVentLevel = 5))
        assertNull(ScenePlanner.plan(vent, lightCaps, climateCaps, applyComfort = true).climate?.seatVentLevel)
        val withVent = climateCaps.copy(seatVentRange = 0..3)
        assertEquals(3, ScenePlanner.plan(vent, lightCaps, withVent, applyComfort = true).climate?.seatVentLevel)
    }
}
