package com.example.moodrive.ui.common

import com.example.moodrive.domain.model.AmbientLightState
import com.example.moodrive.domain.model.ClimateState
import com.example.moodrive.domain.model.FanDirection
import com.example.moodrive.domain.session.ScenePlanner

/** Scene에 값이 없어 차량의 기존 설정을 그대로 두는 항목 */
internal const val KEEP = "유지"

/** 단계 값 → (값, 단위): null 유지 / 0 OFF / N "단" */
internal fun levelValue(level: Int?): Pair<String, String?> = when (level) {
    null -> KEEP to null
    0 -> "OFF" to null
    else -> "$level" to "단"
}

/** 켜짐/꺼짐 값 (핸들 열선): 켜져 있으면 ON, 0이거나 값이 없으면 OFF. "유지"는 쓰지 않는다 */
internal fun onOffValue(level: Int?): String = if ((level ?: 0) > 0) "ON" else "OFF"

/** ARGB → "#RRGGBB" */
internal fun hexRgb(argb: Int): String = "#%06X".format(argb and 0xFFFFFF)

/** 통풍만 켜져 있으면(열선 꺼짐) 통풍 모드 */
internal fun isSeatVentOnly(heat: Int?, vent: Int?): Boolean = (vent ?: 0) > 0 && (heat ?: 0) == 0

internal fun FanDirection.label(): String = when (this) {
    FanDirection.FACE -> "얼굴"
    FanDirection.FLOOR -> "발"
    FanDirection.FACE_AND_FLOOR -> "얼굴·발"
    FanDirection.DEFROST -> "앞유리"
    FanDirection.DEFROST_AND_FLOOR -> "앞유리·발"
    FanDirection.FACE_AND_DEFROST -> "얼굴·앞유리"
    FanDirection.ALL -> "전체"
}

private const val UNKNOWN = "현재 -"

/** 차량의 지금 값: "현재 30%", "현재 OFF", 모르면 "현재 -" */
internal fun currentValueText(kind: DetailKind, light: AmbientLightState?, brightnessRange: IntRange?, climate: ClimateState?): String = when (kind) {
    DetailKind.AMBIENT -> when {
        light == null -> UNKNOWN
        !light.on -> "현재 OFF"
        else -> "현재 ${percent(light.brightness, brightnessRange)}%"
    }
    DetailKind.SEAT -> when {
        isSeatVentOnly(climate?.seatHeatLevel, climate?.seatVentLevel) -> "현재 통풍 ${climate!!.seatVentLevel}단"
        else -> climate?.seatHeatLevel?.let { if (it == 0) "현재 OFF" else "현재 ${it}단" } ?: UNKNOWN
    }
    DetailKind.WIND -> when (val s = climate?.fanSpeed) {
        null -> UNKNOWN
        0 -> "현재 OFF"
        else -> "현재 ${s}단" + (climate.fanDirection?.let { " · ${it.label()}" } ?: "")
    }
    DetailKind.STEERING -> climate?.steeringWheelHeatLevel?.let { if (it == 0) "현재 OFF" else "현재 ON" } ?: UNKNOWN
}

private fun percent(value: Int, range: IntRange?): Int =
    if (range == null || range.last <= range.first) value else ScenePlanner.percentOf(value, range)
