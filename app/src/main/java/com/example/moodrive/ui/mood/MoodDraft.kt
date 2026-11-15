package com.example.moodrive.ui.mood

import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import com.example.moodrive.domain.model.AmbientColors
import com.example.moodrive.domain.model.Comfort
import com.example.moodrive.domain.model.FanDirection
import com.example.moodrive.domain.model.Scene

/** 화면 회전·주간/야간 전환으로 화면이 다시 만들어져도 고치던 값이 남도록 Bundle에 저장한다 */
internal val MoodDraftSaver: Saver<MoodDraft, Any> = listSaver(
    save = {
        listOf(
            it.name, it.lightOn, it.mainColor, it.subColor, it.editingSlot.name, it.brightnessPercent,
            it.seatOn, it.seatMode.name, it.seatLevel, it.windOn, it.windDirection.name, it.fanLevel, it.steeringOn,
        )
    },
    restore = {
        MoodDraft(
            name = it[0] as String,
            lightOn = it[1] as Boolean,
            mainColor = it[2] as Int,
            subColor = it[3] as Int,
            editingSlot = ColorSlot.valueOf(it[4] as String),
            brightnessPercent = it[5] as Int,
            seatOn = it[6] as Boolean,
            seatMode = SeatMode.valueOf(it[7] as String),
            seatLevel = it[8] as Int,
            windOn = it[9] as Boolean,
            windDirection = FanDirection.valueOf(it[10] as String),
            fanLevel = it[11] as Int,
            steeringOn = it[12] as Boolean,
        )
    },
)

enum class ColorSlot { MAIN, SUB }
enum class SeatMode { HEAT, VENT }

/**
 * 무드 수정 화면에서 고치는 값. "무드에 저장"을 누르면 [applyTo]로 Scene에 반영한다.
 * 각 섹션 토글은 그 기능을 켜고 끄는 값이다 (끄면 0단 / 조명 꺼짐으로 저장).
 */
data class MoodDraft(
    val name: String,
    val lightOn: Boolean,
    val mainColor: Int,
    val subColor: Int,
    val editingSlot: ColorSlot = ColorSlot.MAIN,
    val brightnessPercent: Int,
    val seatOn: Boolean,
    val seatMode: SeatMode,
    val seatLevel: Int,
    val windOn: Boolean,
    val windDirection: FanDirection,
    val fanLevel: Int,
    val steeringOn: Boolean,
) {
    /** 팔레트 원을 누르면 지금 고른 칸(Main/Sub)의 색을 바꾼다 */
    fun pickColor(argb: Int) = when (editingSlot) {
        ColorSlot.MAIN -> copy(mainColor = argb)
        ColorSlot.SUB -> copy(subColor = argb)
    }

    fun applyTo(scene: Scene): Scene {
        val base = scene.comfort ?: Comfort()
        return scene.copy(
            name = name.trim().ifEmpty { scene.name },
            version = scene.version + 1,
            visual = scene.visual.copy(
                brightnessPercent = brightnessPercent.coerceIn(0, 100),
                ambient = AmbientColors(mainColor, subColor),
                lightOn = lightOn,
            ),
            comfort = base.copy(
                seatHeatLevel = if (seatOn && seatMode == SeatMode.HEAT) seatLevel else 0,
                seatVentLevel = if (seatOn && seatMode == SeatMode.VENT) seatLevel else 0,
                fanSpeed = if (windOn) fanLevel else 0,
                fanDirection = windDirection,
                steeringWheelHeatLevel = if (steeringOn) 1 else 0,
            ),
        )
    }

    companion object {
        /** 시안의 색 8개. 스포이드(임의 색)는 추후 */
        val PRESET_COLORS: List<Int> = listOf(
            0xFFFF9F43, 0xFFFFE2B8, 0xFFFF4D4D, 0xFFFF6FB5,
            0xFF8B6CFF, 0xFF3D8BFF, 0xFF2ED3C6, 0xFF4ADE80,
        ).map { it.toInt() }

        /** 시트 단계 */
        val LEVELS = 1..3
        /** 풍량 단계 (스테퍼). 차량 풍량 범위(0..8)에서 끔(0)을 뺀 값 */
        val FAN_LEVELS = 1..8
        /** 무드 수정의 바람 방향 (Figma 722:23832: 발 · 얼굴). 그 밖의 방향은 발로 연다 */
        val WIND_DIRECTIONS = listOf(FanDirection.FLOOR, FanDirection.FACE)
    }
}
