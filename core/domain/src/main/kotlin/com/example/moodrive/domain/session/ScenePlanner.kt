package com.example.moodrive.domain.session

import com.example.moodrive.color.ColorMatch
import com.example.moodrive.color.ColorMatcher
import com.example.moodrive.domain.model.AmbientLightState
import com.example.moodrive.domain.model.ClimateCapabilities
import com.example.moodrive.domain.model.ClimateState
import com.example.moodrive.domain.model.LightCapabilities
import com.example.moodrive.domain.model.Scene
import kotlin.math.roundToInt

/** ScenePlan 미리보기 화면 기능용 예약 타입 ([ScenePlan.excluded] 키). 아직 앱에서 읽지 않는다 */
enum class Feature { LIGHT, CLIMATE }

/**
 * 이 차량에 실제로 적용할 값. 미리보기 화면은 이 값을 그대로 보여준다 ("실제 적용 색상 사전 표시").
 * [light]/[climate]가 null이면 적용하지 않는다. 지원하지 않아 빠진 항목은 [excluded]에 이유와 함께 담는다.
 */
data class ScenePlan(
    val sceneId: String,
    val light: AmbientLightState?,
    val climate: ClimateState?,
    val colorMatch: ColorMatch?,
    /** 미리보기 화면 기능용 예약 필드. 아직 앱에서 읽지 않는다 */
    val excluded: Map<Feature, String> = emptyMap(),
)

/** Scene(원본) + 차량 Capability → ScenePlan(이 차에 적용할 값) */
object ScenePlanner {

    fun plan(
        scene: Scene,
        light: LightCapabilities?,
        climate: ClimateCapabilities?,
        /** 공조는 사용자가 승인했을 때만 적용 */
        applyComfort: Boolean,
    ): ScenePlan {
        val excluded = mutableMapOf<Feature, String>()

        // 사용자가 고른 색이 있으면 그 색, 없으면 팔레트에서 가장 가깝게 낼 수 있는 색
        val chosen = scene.visual.ambient?.main
        val match = light?.takeIf { it.access.canRead && it.supportedColors.isNotEmpty() }?.let {
            if (chosen != null) ColorMatcher.nearest(chosen, it.supportedColors)
            else ColorMatcher.matchPalette(scene.visual.palette, it.supportedColors)
        }
        val lightTarget = when {
            light == null || !light.access.canRead -> null.also { excluded[Feature.LIGHT] = "앰비언트 조명 미지원" }
            match == null -> null.also { excluded[Feature.LIGHT] = "매핑할 색상 없음" }
            else -> AmbientLightState(
                on = scene.visual.lightOn,
                colorArgb = match.matched,
                brightness = scale(scene.visual.brightnessPercent, light.brightnessRange),
                autoColorByDriveMode = false,
            )
        }

        val comfort = scene.comfort
        val climateTarget = when {
            comfort == null || !applyComfort -> null
            climate == null || !climate.access.canRead -> null.also { excluded[Feature.CLIMATE] = "공조 미지원" }
            else -> ClimateState(
                temperatureC = comfort.temperatureC?.coerceIn(climate.temperatureRange),
                fanSpeed = comfort.fanSpeed?.coerceIn(climate.fanSpeedRange),
                fanDirection = comfort.fanDirection,
                seatHeatLevel = comfort.seatHeatLevel?.coerceIn(climate.seatHeatRange),
                seatVentLevel = climate.seatVentRange?.let { r -> comfort.seatVentLevel?.coerceIn(r) },
                steeringWheelHeatLevel = comfort.steeringWheelHeatLevel?.coerceIn(climate.steeringWheelHeatRange),
            ).takeUnless { it.isEmpty }
        }

        return ScenePlan(scene.id, lightTarget, climateTarget, match, excluded)
    }

    /** 밝기 % → 차량 밝기 값 */
    fun scale(percent: Int, range: IntRange): Int =
        range.first + ((range.last - range.first) * percent.coerceIn(0, 100) / 100.0).roundToInt()

    /** 차량 밝기 값 → % */
    fun percentOf(value: Int, range: IntRange): Int {
        if (range.last <= range.first) return value.coerceIn(0, 100)
        return ((value - range.first) * 100.0 / (range.last - range.first)).roundToInt().coerceIn(0, 100)
    }
}
