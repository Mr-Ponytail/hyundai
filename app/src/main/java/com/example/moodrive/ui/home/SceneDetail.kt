package com.example.moodrive.ui.home

import com.example.moodrive.domain.model.AmbientLightState
import com.example.moodrive.domain.model.ClimateState
import com.example.moodrive.domain.model.FanDirection
import com.example.moodrive.domain.model.Scene
import com.example.moodrive.ui.common.DetailKind
import com.example.moodrive.ui.common.KEEP
import com.example.moodrive.ui.common.currentValueText
import com.example.moodrive.ui.common.isSeatVentOnly
import com.example.moodrive.ui.common.label
import com.example.moodrive.ui.common.levelValue
import com.example.moodrive.ui.common.onOffValue

/** 하단 정보 패널의 한 칸: "앰비언트 [현재 30%]  50 %" */
data class DetailItemUi(
    val kind: DetailKind,
    /** 우측 상단 pill. 차량의 지금 값 */
    val current: String,
    /** 크게 표시하는 Scene 값 */
    val value: String,
    val unit: String? = null,
)

/** Scene 값 + 차량 현재 상태 → 패널 표시 문자열. (Android 의존 없음 → 단위 테스트) */
object SceneDetailFormatter {

    fun items(scene: Scene, light: AmbientLightState?, brightnessRange: IntRange?, climate: ClimateState?): List<DetailItemUi> {
        val comfort = scene.comfort
        return listOf(
            DetailItemUi(
                DetailKind.AMBIENT,
                current = currentValueText(DetailKind.AMBIENT, light, brightnessRange, climate),
                value = "${scene.visual.brightnessPercent}", unit = "%",
            ),
            DetailItemUi(
                DetailKind.SEAT,
                current = currentValueText(DetailKind.SEAT, light, brightnessRange, climate),
                value = seatValue(comfort?.seatHeatLevel, comfort?.seatVentLevel).first,
                unit = seatValue(comfort?.seatHeatLevel, comfort?.seatVentLevel).second,
            ),
            windItem(comfort?.fanSpeed, comfort?.fanDirection, climate),
            DetailItemUi(
                DetailKind.STEERING,
                current = currentValueText(DetailKind.STEERING, light, brightnessRange, climate),
                value = onOffValue(comfort?.steeringWheelHeatLevel),
            ),
        )
    }

    private fun windItem(speed: Int?, direction: FanDirection?, climate: ClimateState?): DetailItemUi {
        val current = currentValueText(DetailKind.WIND, null, null, climate)
        val (value, unit) = when {
            speed == null && direction == null -> KEEP to null
            speed == 0 -> "OFF" to null
            speed == null -> direction!!.label() to "방향"
            else -> "$speed" to ("단" + (direction?.let { " · ${it.label()} 방향" } ?: ""))
        }
        return DetailItemUi(DetailKind.WIND, current, value, unit)
    }

    private fun seatValue(heat: Int?, vent: Int?): Pair<String, String?> =
        when {
            isSeatVentOnly(heat, vent) -> "$vent" to "단 · 통풍"
            (heat ?: 0) > 0 -> "$heat" to "단 · 열선"
            else -> levelValue(heat)
        }
}
