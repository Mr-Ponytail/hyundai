package com.example.moodrive.domain.model

/** SDK Capability의 AccessType에 대응. READ면 조회만 가능 (제어 권한 미공개 상태). */
enum class Access {
    NONE, READ, READ_WRITE;

    val canRead get() = this != NONE
    val canWrite get() = this == READ_WRITE
}

data class LightCapabilities(
    val access: Access,
    val brightnessRange: IntRange,
    /** 차량이 낼 수 있는 앰비언트 색 (0xAARRGGBB). SDK가 목록을 주지 않아 설정값으로 관리 */
    val supportedColors: List<Int>,
)

data class ClimateCapabilities(
    val access: Access,
    val temperatureRange: ClosedFloatingPointRange<Float>,
    val fanSpeedRange: IntRange,
    val seatHeatRange: IntRange = 0..3,
    /** null = 통풍 시트 없음 */
    val seatVentRange: IntRange? = null,
    val steeringWheelHeatRange: IntRange = 0..1,
)

data class AmbientLightState(
    val on: Boolean,
    val colorArgb: Int,
    val brightness: Int,
    /** 주행 모드에 따라 색을 자동 변경하는 기능. 켜져 있으면 Scene 색을 덮어쓰므로 적용 시 끄고 복원 시 되돌린다. null = 미지원 */
    val autoColorByDriveMode: Boolean? = null,
) {
    /** 화면에 보이는 결과가 같은지 (안내 모드에서 사용자가 값을 맞췄는지 판정) */
    fun visiblyEquals(other: AmbientLightState) =
        on == other.on && colorArgb == other.colorArgb && brightness == other.brightness
}

/**
 * 공조 계열 상태. null 필드는 "건드리지 않음" (적용 대상일 때) 또는 "읽지 못함/미지원" (조회 결과일 때).
 */
data class ClimateState(
    val temperatureC: Float? = null,
    val fanSpeed: Int? = null,
    val fanDirection: FanDirection? = null,
    val seatHeatLevel: Int? = null,
    val seatVentLevel: Int? = null,
    val steeringWheelHeatLevel: Int? = null,
) {
    /** [known]에서 값을 읽을 수 있었던 항목만 남긴다. 현재 상태를 모르는 항목은 되돌릴 수 없으므로 적용하지 않는다. */
    fun onlyKnownIn(known: ClimateState) = ClimateState(
        temperatureC = temperatureC.takeIf { known.temperatureC != null },
        fanSpeed = fanSpeed.takeIf { known.fanSpeed != null },
        fanDirection = fanDirection.takeIf { known.fanDirection != null },
        seatHeatLevel = seatHeatLevel.takeIf { known.seatHeatLevel != null },
        seatVentLevel = seatVentLevel.takeIf { known.seatVentLevel != null },
        steeringWheelHeatLevel = steeringWheelHeatLevel.takeIf { known.steeringWheelHeatLevel != null },
    )

    /** 비어 있는(null) 항목만 [other]에서 채운다 */
    fun orElse(other: ClimateState) = ClimateState(
        temperatureC = temperatureC ?: other.temperatureC,
        fanSpeed = fanSpeed ?: other.fanSpeed,
        fanDirection = fanDirection ?: other.fanDirection,
        seatHeatLevel = seatHeatLevel ?: other.seatHeatLevel,
        seatVentLevel = seatVentLevel ?: other.seatVentLevel,
        steeringWheelHeatLevel = steeringWheelHeatLevel ?: other.steeringWheelHeatLevel,
    )

    val isEmpty get() = this == ClimateState()
}

/** Scene 적용 직전 상태. 종료 시 이 값으로 되돌린다. null 필드 = 이번 세션에서 건드리지 않은 영역 */
data class VehicleSnapshot(
    val light: AmbientLightState? = null,
    val climate: ClimateState? = null,
) {
    val isEmpty get() = light == null && climate == null
}

enum class DriveMode { PARKED, DRIVING, UNKNOWN }
