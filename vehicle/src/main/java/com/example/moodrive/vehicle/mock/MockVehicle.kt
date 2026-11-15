package com.example.moodrive.vehicle.mock

import com.example.moodrive.domain.model.Access
import com.example.moodrive.domain.model.AmbientLightState
import com.example.moodrive.domain.model.ClimateCapabilities
import com.example.moodrive.domain.model.ClimateState
import com.example.moodrive.domain.model.DriveMode
import com.example.moodrive.domain.model.FanDirection
import com.example.moodrive.domain.model.LightCapabilities
import com.example.moodrive.domain.port.ClimateController
import com.example.moodrive.domain.port.DriveStateSource
import com.example.moodrive.domain.port.LightController
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * 차량·에뮬레이터 없이 UI를 개발할 때 쓰는 메모리 구현체. [lightAccess]를 READ로 두면 안내 모드를 재현할 수 있다.
 * 앱에서는 VehicleModule.mock(lightAccess = Access.READ)로 넘긴다.
 */
class MockLightController(
    private val supportedColors: List<Int>,
    var lightAccess: Access = Access.READ_WRITE,
) : LightController {
    val state = MutableStateFlow(AmbientLightState(on = true, colorArgb = supportedColors.last(), brightness = 5, autoColorByDriveMode = false))

    override suspend fun capabilities() = LightCapabilities(lightAccess, 0..10, supportedColors)
    override suspend fun read() = state.value
    override suspend fun apply(target: AmbientLightState) { state.value = target }
    override fun observe(): Flow<AmbientLightState> = state
}

class MockClimateController : ClimateController {
    val state = MutableStateFlow(
        ClimateState(temperatureC = 22f, fanSpeed = 2, fanDirection = FanDirection.FACE, seatHeatLevel = 0, seatVentLevel = 0, steeringWheelHeatLevel = 0),
    )

    override suspend fun capabilities() = ClimateCapabilities(Access.READ_WRITE, 16f..32f, 0..8, seatVentRange = 0..3)
    override suspend fun read() = state.value
    override suspend fun apply(target: ClimateState) {
        val cur = state.value
        state.value = ClimateState(
            temperatureC = target.temperatureC ?: cur.temperatureC,
            fanSpeed = target.fanSpeed ?: cur.fanSpeed,
            fanDirection = target.fanDirection ?: cur.fanDirection,
            seatHeatLevel = target.seatHeatLevel ?: cur.seatHeatLevel,
            seatVentLevel = target.seatVentLevel ?: cur.seatVentLevel,
            steeringWheelHeatLevel = target.steeringWheelHeatLevel ?: cur.steeringWheelHeatLevel,
        )
    }
    override fun observe(): Flow<ClimateState> = state
}

/** 주행 상태 재현용. 초기값은 VehicleModule.mock(driveMode = ...)로 넘긴다 */
class MockDriveStateSource(initial: DriveMode = DriveMode.PARKED) : DriveStateSource {
    val mode = MutableStateFlow(initial)
    override val driveMode: Flow<DriveMode> = mode
}
