package com.example.moodrive.ui.control

import com.example.moodrive.domain.model.FanDirection
import com.example.moodrive.domain.model.Scene
import com.example.moodrive.domain.session.ScenePlan
import com.example.moodrive.ui.common.isSeatVentOnly
import com.example.moodrive.ui.common.levelValue
import com.example.moodrive.ui.common.onOffValue
import com.example.moodrive.ui.mood.MoodDraft
import com.example.moodrive.ui.mood.SeatMode
import com.example.moodrive.ui.common.DetailKind

/**
 * 실행 화면 카드에 보여주는 목표값. null = 이 Scene에 값이 없음(기존 설정 유지) 또는 미지원.
 * 밝기는 화면 표시와 같은 %다.
 */
data class ControlTargets(
    val brightnessPercent: Int? = null,
    val colorArgb: Int? = null,
    val seatHeat: Int? = null,
    val seatVent: Int? = null,
    val fanSpeed: Int? = null,
    val fanDirection: FanDirection? = null,
    val steeringHeat: Int? = null,
) {
    /** 통풍만 켜져 있으면 통풍 모드 */
    val seatVenting get() = isSeatVentOnly(seatHeat, seatVent)

    /** 지금 모드(열선/통풍)의 단계 */
    val seatLevel get() = if (seatVenting) seatVent else seatHeat

    companion object {
        /** Scene과 이 차량용 계획(plan)에서 화면의 기준값을 만든다 */
        fun base(scene: Scene, plan: ScenePlan): ControlTargets {
            val comfort = scene.comfort
            val climate = plan.climate
            return ControlTargets(
                brightnessPercent = plan.light?.let { scene.visual.brightnessPercent },
                colorArgb = plan.light?.colorArgb,
                seatHeat = climate?.seatHeatLevel ?: comfort?.seatHeatLevel,
                seatVent = climate?.seatVentLevel ?: comfort?.seatVentLevel,
                fanSpeed = climate?.fanSpeed ?: comfort?.fanSpeed,
                fanDirection = climate?.fanDirection ?: comfort?.fanDirection,
                steeringHeat = climate?.steeringWheelHeatLevel ?: comfort?.steeringWheelHeatLevel,
            )
        }
    }
}

/** 하단 카드 한 칸 */
data class ControlCardUi(
    val kind: DetailKind,
    val value: String,
    val unit: String? = null,
)

object SceneControlFormatter {

    fun cards(targets: ControlTargets): List<ControlCardUi> =
        DetailKind.entries.map { kind ->
            val (value, unit) = valueOf(kind, targets)
            ControlCardUi(kind, value, unit)
        }

    fun valueOf(kind: DetailKind, t: ControlTargets): Pair<String, String?> = when (kind) {
        DetailKind.AMBIENT -> t.brightnessPercent?.let { "$it" to "%" } ?: ("미지원" to null)
        DetailKind.SEAT -> levelValue(t.seatLevel)
        DetailKind.WIND -> levelValue(t.fanSpeed)
        DetailKind.STEERING -> onOffValue(t.steeringHeat) to null
    }
}

/**
 * 지금 실행 중인 값에서 시작한다.
 * @param effective 실행 화면의 목표값 ([ControlTargets.base])
 * @param paletteColor 팔레트에서 고른 대표색 (사용자가 고른 색이 없을 때)
 */
internal fun MoodDraft.Companion.from(scene: Scene, effective: ControlTargets, paletteColor: Int?): MoodDraft {
    val ambient = scene.visual.ambient
    val palette = scene.visual.palette.map { it.argb }
    val main = ambient?.main ?: paletteColor ?: palette.firstOrNull() ?: PRESET_COLORS.first()
    val sub = ambient?.sub ?: palette.firstOrNull { it != main } ?: main

    val heat = effective.seatHeat ?: scene.comfort?.seatHeatLevel ?: 0
    val vent = scene.comfort?.seatVentLevel ?: 0
    val ventOnly = isSeatVentOnly(heat, vent)
    val fan = effective.fanSpeed ?: scene.comfort?.fanSpeed ?: 0
    val direction = (effective.fanDirection ?: scene.comfort?.fanDirection)
        ?.takeIf { it in WIND_DIRECTIONS } ?: FanDirection.FLOOR

    return MoodDraft(
        name = scene.name,
        lightOn = scene.visual.lightOn,
        mainColor = main,
        subColor = sub,
        brightnessPercent = effective.brightnessPercent ?: scene.visual.brightnessPercent,
        seatOn = heat > 0 || vent > 0,
        seatMode = if (ventOnly) SeatMode.VENT else SeatMode.HEAT,
        seatLevel = (if (ventOnly) vent else heat).coerceIn(LEVELS),
        windOn = fan > 0,
        windDirection = direction,
        fanLevel = fan.coerceIn(MoodDraft.FAN_LEVELS),
        steeringOn = (effective.steeringHeat ?: scene.comfort?.steeringWheelHeatLevel ?: 0) > 0,
    )
}
