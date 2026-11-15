package com.example.moodrive.domain

import com.example.moodrive.domain.model.Access
import com.example.moodrive.domain.model.AmbientLightState
import com.example.moodrive.domain.model.ClimateCapabilities
import com.example.moodrive.domain.model.ClimateState
import com.example.moodrive.domain.model.LightCapabilities
import com.example.moodrive.domain.model.VehicleSnapshot
import com.example.moodrive.domain.port.ClimateController
import com.example.moodrive.domain.port.LightController
import com.example.moodrive.domain.port.PartialApplyException
import com.example.moodrive.domain.port.SnapshotStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

val CYAN = 0xFF00FFFF.toInt()
val BLUE = 0xFF0000FF.toInt()
val PURPLE = 0xFF8000FF.toInt()
val WHITE = 0xFFFFFFFF.toInt()

class FakeLight(
    initial: AmbientLightState = AmbientLightState(on = false, colorArgb = WHITE, brightness = 3, autoColorByDriveMode = true),
    var access: Access = Access.READ_WRITE,
) : LightController {
    val state = MutableStateFlow(initial)
    var failRead = false
    var failApply = false
    /** 성공을 돌려주지만 값은 바꾸지 않음 (Playground 에뮬레이터 재현) */
    var ignoreWrites = false
    val applied = mutableListOf<AmbientLightState>()

    override suspend fun capabilities() = LightCapabilities(access, 0..10, listOf(CYAN, BLUE, PURPLE, WHITE))
    override suspend fun read(): AmbientLightState {
        if (failRead) error("read failed")
        return state.value
    }
    /** 적용이 끝나지 않음 (안내 모드 대기 재현) */
    var hang = false

    override suspend fun apply(target: AmbientLightState) {
        if (hang) kotlinx.coroutines.awaitCancellation()
        if (failApply) error("apply failed")
        applied += target
        if (!ignoreWrites) state.value = target
    }
    override fun observe(): Flow<AmbientLightState> = state
}

class FakeClimate(
    initial: ClimateState = ClimateState(temperatureC = 22f, fanSpeed = 2, seatHeatLevel = 0),
) : ClimateController {
    val state = MutableStateFlow(initial)
    var failApply = false
    var partialFail: String? = null

    override suspend fun capabilities() = ClimateCapabilities(Access.READ_WRITE, 16f..30f, 0..8)
    override suspend fun read() = state.value
    override suspend fun apply(target: ClimateState) {
        if (failApply) error("apply failed")
        partialFail?.let { throw PartialApplyException(it) }
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

class MemorySnapshotStore : SnapshotStore {
    var value: VehicleSnapshot? = null
    override suspend fun load() = value
    override suspend fun save(snapshot: VehicleSnapshot) { value = snapshot }
    override suspend fun clear() { value = null }
}
