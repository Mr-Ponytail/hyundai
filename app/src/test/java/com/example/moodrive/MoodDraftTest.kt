package com.example.moodrive

import com.example.moodrive.color.WeightedColor
import com.example.moodrive.domain.model.AmbientColors
import com.example.moodrive.domain.model.Comfort
import com.example.moodrive.domain.model.FanDirection
import com.example.moodrive.domain.model.Scene
import com.example.moodrive.domain.model.Visual
import com.example.moodrive.ui.control.ControlTargets
import com.example.moodrive.ui.control.from
import com.example.moodrive.ui.mood.ColorSlot
import com.example.moodrive.ui.mood.MoodDraft
import com.example.moodrive.ui.mood.MoodDraftSaver
import com.example.moodrive.ui.mood.SeatMode
import androidx.compose.runtime.saveable.SaverScope
import org.junit.Assert.assertEquals
import org.junit.Test

class MoodDraftTest {
    private val red = 0xFF8E2A22.toInt()
    private val gold = 0xFFD9A441.toInt()
    private val scene = Scene(
        id = "cozy", name = "Cozy Fireplace", authorId = "u",
        visual = Visual(palette = listOf(WeightedColor(red, 0.6f), WeightedColor(gold, 0.4f)), brightnessPercent = 50),
        comfort = Comfort(fanSpeed = 1, fanDirection = FanDirection.FLOOR, seatHeatLevel = 2, steeringWheelHeatLevel = 1),
    )

    @Test fun `실행 중 조절 값에서 시작`() {
        val d = MoodDraft.from(scene, ControlTargets(brightnessPercent = 70, fanSpeed = 3), paletteColor = gold)
        assertEquals(gold, d.mainColor)
        assertEquals(red, d.subColor)
        assertEquals(70, d.brightnessPercent)
        assertEquals(3, d.fanLevel)
        assertEquals(true, d.seatOn)
        assertEquals(SeatMode.HEAT, d.seatMode)
        assertEquals(2, d.seatLevel)
        assertEquals(true, d.steeringOn)
    }

    @Test fun `색은 지금 고른 칸에만 들어간다`() {
        val d = MoodDraft.from(scene, ControlTargets(), null)
        val sub = d.copy(editingSlot = ColorSlot.SUB).pickColor(MoodDraft.PRESET_COLORS[4])
        assertEquals(d.mainColor, sub.mainColor)
        assertEquals(MoodDraft.PRESET_COLORS[4], sub.subColor)
    }

    @Test fun `저장하면 Scene에 반영, 끈 기능은 0`() {
        val d = MoodDraft.from(scene, ControlTargets(), null).copy(
            name = "  난로 앞  ", lightOn = false, mainColor = 1, subColor = 2,
            seatMode = SeatMode.VENT, seatLevel = 3, windOn = false, steeringOn = false,
        )
        val saved = d.applyTo(scene)
        assertEquals("난로 앞", saved.name)
        assertEquals(2, saved.version)
        assertEquals(AmbientColors(1, 2), saved.visual.ambient)
        assertEquals(false, saved.visual.lightOn)
        assertEquals(0, saved.comfort?.seatHeatLevel)
        assertEquals(3, saved.comfort?.seatVentLevel)
        assertEquals(0, saved.comfort?.fanSpeed)
        assertEquals(0, saved.comfort?.steeringWheelHeatLevel)
    }

    @Test fun `빈 이름은 원래 이름 유지`() {
        assertEquals("Cozy Fireplace", MoodDraft.from(scene, ControlTargets(), null).copy(name = " ").applyTo(scene).name)
    }

    @Test fun `시안에 없는 바람 방향은 발로`() {
        val s = scene.copy(comfort = Comfort(fanDirection = FanDirection.DEFROST))
        assertEquals(FanDirection.FLOOR, MoodDraft.from(s, ControlTargets(), null).windDirection)
    }

    @Test fun `화면이 다시 만들어져도 고치던 값이 그대로 복원된다`() {
        val edited = MoodDraft(
            name = "내가 고친 무드", lightOn = false, mainColor = red, subColor = gold, editingSlot = ColorSlot.SUB,
            brightnessPercent = 35, seatOn = true, seatMode = SeatMode.VENT, seatLevel = 2,
            windOn = false, windDirection = FanDirection.FACE_AND_FLOOR, fanLevel = 3, steeringOn = true,
        )
        val saved = with(MoodDraftSaver) { SaverScope { true }.save(edited) }!!
        assertEquals(edited, MoodDraftSaver.restore(saved))
    }
}
