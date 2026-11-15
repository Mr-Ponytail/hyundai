package com.example.moodrive.vehicle.pleos

import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.api.model.area.SeatArea
import ai.pleos.playground.vehicle.api.model.capability.AreaBounds
import ai.pleos.playground.vehicle.api.model.capability.CapabilityConfig
import ai.pleos.playground.vehicle.constant.control.FanDirectionType
import ai.pleos.playground.vehicle.constant.unit.TemperatureUnit
import ai.pleos.playground.vehicle.listener.HvacFanSpeedListener
import ai.pleos.playground.vehicle.listener.HvacSeatTemperatureListener
import ai.pleos.playground.vehicle.listener.HvacSeatVentilationListener
import ai.pleos.playground.vehicle.listener.HvacSteeringWheelHeatStateListener
import ai.pleos.playground.vehicle.listener.HvacTemperatureListener
import android.util.Log
import com.example.moodrive.domain.model.Access
import com.example.moodrive.domain.model.ClimateCapabilities
import com.example.moodrive.domain.model.ClimateState
import com.example.moodrive.domain.model.FanDirection
import com.example.moodrive.domain.port.ClimateController
import com.example.moodrive.domain.port.PartialApplyException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * 공조 계열(온도·풍량·바람 방향·시트 열선·핸들 열선).
 * 각 항목은 차량마다 지원 여부가 달라 조회 실패 시 null로 둔다 → SceneSession이 해당 항목을 적용 대상에서 뺀다.
 */
class PleosClimateController(
    private val vehicle: Vehicle,
    /** 운전석. 항목별로 차량이 다른 영역(예: 1열 묶음)을 쓰면 Capability 응답에서 운전석을 포함한 영역을 고른다 */
    private val area: SeatArea,
) : ClimateController {

    private val hvac get() = vehicle.getHVAC()
    private val seat get() = vehicle.getSeat()
    private val steering get() = vehicle.getSteeringWheel()

    @Volatile private var areas: Areas? = null

    private data class Areas(val temperature: SeatArea, val fanSpeed: SeatArea, val fanDirection: SeatArea)

    private suspend fun areas(): Areas = areas ?: Areas(
        temperature = resolve("temperature") { ok, err ->
            hvac.checkHvacTemperatureCapabilities({ _: CapabilityConfig, m: Map<SeatArea, AreaBounds<Float>> -> ok(m.keys) }, err)
        },
        fanSpeed = resolve("fan speed") { ok, err ->
            hvac.checkHvacFanSpeedCapabilities({ _: CapabilityConfig, m: Map<SeatArea, AreaBounds<Int>> -> ok(m.keys) }, err)
        },
        fanDirection = resolve("fan direction") { ok, err ->
            hvac.checkHvacFanDirectionCapabilities({ _: CapabilityConfig, m: Map<SeatArea, List<FanDirectionType>> -> ok(m.keys) }, err)
        },
    ).also { areas = it }

    private suspend fun resolve(name: String, query: (ok: (Set<SeatArea>?) -> Unit, err: (Exception) -> Unit) -> Unit): SeatArea {
        val keys = runCatchingSdk { sdkCall<Set<SeatArea>>("$name areas") { ok, err -> query(ok, err) } }.orEmpty()
        val driver = area.values.toSet()
        val chosen = keys.firstOrNull { it == area } ?: keys.firstOrNull { it.values.toSet().containsAll(driver) } ?: keys.firstOrNull() ?: area
        Log.i(TAG, "$name area: ${keys.map { it.values.toList() }} → ${chosen.values.toList()}")
        return chosen
    }

    override suspend fun capabilities(): ClimateCapabilities {
        areas()
        val temp = runCatchingSdk {
            sdkCall<Pair<Access, AreaBounds<Float>>>("temperature capability") { ok, err ->
                hvac.checkHvacTemperatureCapabilities(
                    { config: CapabilityConfig, map: Map<SeatArea, AreaBounds<Float>> ->
                        ok((map[areas?.temperature ?: area] ?: map.values.firstOrNull())?.let { config.accessType.toAccess() to it })
                    },
                    err,
                )
            }
        }
        val fan = runCatchingSdk {
            sdkCall<AreaBounds<Int>>("fan capability") { ok, err ->
                hvac.checkHvacFanSpeedCapabilities(
                    { _: CapabilityConfig, map: Map<SeatArea, AreaBounds<Int>> -> ok(map[areas?.fanSpeed ?: area] ?: map.values.firstOrNull()) },
                    err,
                )
            }
        }
        val ventBounds = runCatchingSdk {
            sdkCall<AreaBounds<Int>>("seat vent capability") { ok, err ->
                seat.checkSeatVentilationCapabilities(
                    { _: CapabilityConfig, map: Map<SeatArea, AreaBounds<Int>> ->
                        Log.i(TAG, "seat vent capability: ${map.mapValues { "${it.value.minValue}..${it.value.maxValue}" }}")
                        ok(map[area] ?: map.values.firstOrNull())
                    },
                    err,
                )
            }
        }
        val steeringBounds = runCatchingSdk {
            sdkCall<AreaBounds<Int>>("steering heat capability") { ok, err ->
                steering.checkHvacSteeringWheelHeatCapabilities({ _: CapabilityConfig, b: AreaBounds<Int> -> ok(b) }, err)
            }
        }
        return ClimateCapabilities(
            access = temp?.first ?: Access.NONE,
            temperatureRange = temp?.second?.let { it.minValue..it.maxValue } ?: 16f..32f,
            fanSpeedRange = fan?.let { it.minValue..it.maxValue } ?: 0..8,
            // 시트 열선은 SDK에 범위 조회 API가 없어 일반적인 0~3단으로 둔다
            seatHeatRange = 0..3,
            seatVentRange = ventBounds?.let { it.minValue..it.maxValue },
            steeringWheelHeatRange = steeringBounds?.let { it.minValue..it.maxValue } ?: 0..1,
        )
    }

    override suspend fun read(): ClimateState {
        val a = areas()
        return ClimateState(
        temperatureC = runCatchingSdk { sdkCall("temperature") { ok, err -> hvac.getHvacTemperature(a.temperature, TemperatureUnit.CELSIUS, ok, err) } },
        fanSpeed = runCatchingSdk { sdkCall("fan speed") { ok, err -> hvac.getHvacFanSpeed(a.fanSpeed, ok, err) } },
        fanDirection = runCatchingSdk {
            sdkCall<FanDirectionType>("fan direction") { ok, err -> hvac.getHvacFanDirection(a.fanDirection, ok, err) }
        }?.toDomain(),
        seatHeatLevel = runCatchingSdk { sdkCall("seat heat") { ok, err -> seat.getHvacSeatTemperature(area, ok, err) } },
        seatVentLevel = runCatchingSdk { sdkCall("seat vent") { ok, err -> seat.getHvacSeatVentilationState(area, ok, err) } },
        steeringWheelHeatLevel = runCatchingSdk { sdkCall("steering heat") { ok, err -> steering.getHvacSteeringWheelHeatState(ok, err) } },
        ).also { if (it.isEmpty) error("공조 상태를 하나도 읽지 못함") }
    }

    override suspend fun apply(target: ClimateState) {
        val a = areas()
        val steps = listOfNotNull(
            target.temperatureC?.let { t ->
                "온도" to suspend { sdkCall<Float>("set temperature") { ok, err -> hvac.setHvacTemperature(a.temperature, t, TemperatureUnit.CELSIUS, { _, v -> ok(v) }, err) } }
            },
            target.fanSpeed?.let { f ->
                "풍량" to suspend { sdkCall<Int>("set fan speed") { ok, err -> hvac.setHvacFanSpeed(a.fanSpeed, f, { _, v -> ok(v) }, err) } }
            },
            target.fanDirection?.let { d ->
                "바람 방향" to suspend { sdkCall<FanDirectionType>("set fan direction") { ok, err -> hvac.setHvacFanDirection(a.fanDirection, d.toSdk(), { _, v -> ok(v) }, err) } }
            },
            *seatSteps(target),
            target.steeringWheelHeatLevel?.let { l ->
                "핸들 열선" to suspend { sdkCall<Int>("set steering heat") { ok, err -> steering.setHvacSteeringWheelHeat(l, ok, err) } }
            },
        )
        // 항목별 독립 적용: 하나가 실패해도(예: 공조 전원 꺼짐으로 풍량 거부) 나머지는 적용한다
        // runCatchingSdk는 실패 시 null (성공 시 SDK가 돌려준 값)
        val rejected = steps.filter { (_, step) -> runCatchingSdk { step() } == null }.map { it.first }

        // 성공 응답이어도 실제 반영되지 않는 경우가 있어(Playground 에뮬레이터) 다시 읽어 확인한다
        delay(VERIFY_DELAY_MS)
        val now = runCatchingSdk { read() }
        val notReflected = listOfNotNull(
            "온도".takeIf { target.temperatureC != null && now?.temperatureC != target.temperatureC },
            "풍량".takeIf { target.fanSpeed != null && now?.fanSpeed != target.fanSpeed },
            "바람 방향".takeIf { target.fanDirection != null && now?.fanDirection != target.fanDirection },
            "시트 열선".takeIf { target.seatHeatLevel != null && now?.seatHeatLevel != target.seatHeatLevel },
            "시트 통풍".takeIf { target.seatVentLevel != null && now?.seatVentLevel != target.seatVentLevel },
            "핸들 열선".takeIf { target.steeringWheelHeatLevel != null && now?.steeringWheelHeatLevel != target.steeringWheelHeatLevel },
        ) - rejected.toSet()

        val failedCount = rejected.size + notReflected.size
        if (failedCount == 0) return
        val msg = listOfNotNull(
            rejected.takeIf { it.isNotEmpty() }?.let { "${it.joinToString()} 거부됨(공조 꺼짐 또는 미지원)" },
            notReflected.takeIf { it.isNotEmpty() }?.let { "${it.joinToString()} 차량에 반영 안 됨" },
        ).joinToString(" · ")
        Log.w(TAG, "climate apply: $msg")
        if (failedCount >= steps.size) error(msg) else throw PartialApplyException(msg)
    }

    /** 바람 방향은 SDK에 변경 리스너가 없어 조회 시점 값으로만 갱신된다 */
    override fun observe(): Flow<ClimateState> = callbackFlow {
        val a = areas()
        val lock = Any()
        var current = read()
        trySend(current)

        fun update(change: (ClimateState) -> ClimateState) {
            val next = synchronized(lock) { change(current).also { current = it } }
            trySend(next)
        }

        val tempListener = object : HvacTemperatureListener {
            override fun onHvacTemperatureUpdated(value: Float?) { value?.let { v -> update { it.copy(temperatureC = v) } } }
            override fun onFailed(e: Exception) = Unit
        }
        val fanListener = object : HvacFanSpeedListener {
            override fun onHvacFanSpeedUpdated(value: Int?) { value?.let { v -> update { it.copy(fanSpeed = v) } } }
            override fun onFailed(e: Exception) = Unit
        }
        val seatListener = object : HvacSeatTemperatureListener {
            override fun onHvacSeatTemperatureUpdated(value: Int?) { value?.let { v -> update { it.copy(seatHeatLevel = v) } } }
            override fun onFailed(e: Exception) = Unit
        }
        val ventListener = object : HvacSeatVentilationListener {
            override fun onHvacSeatVentilationUpdated(value: Int?) { value?.let { v -> update { it.copy(seatVentLevel = v) } } }
            override fun onFailed(e: Exception) = Unit
        }
        val steeringListener = object : HvacSteeringWheelHeatStateListener {
            override fun onHvacSteeringWheelHeatStateUpdated(value: Int?) { value?.let { v -> update { it.copy(steeringWheelHeatLevel = v) } } }
            override fun onFailed(e: Exception) = Unit
        }
        // 미지원 항목의 등록 실패가 전체 관찰을 막지 않도록 개별 처리
        runCatching { hvac.registerHvacTemperature(a.temperature, tempListener) }
        runCatching { hvac.registerHvacFanSpeed(a.fanSpeed, fanListener) }
        runCatching { seat.registerHvacSeatTemperature(area, seatListener) }
        runCatching { seat.registerHvacSeatVentilation(area, ventListener) }
        runCatching { steering.registerHvacSteeringWheelHeatState(steeringListener) }

        awaitClose {
            runCatching { hvac.unregisterHvacTemperature(a.temperature, tempListener) }
            runCatching { hvac.unregisterHvacFanSpeed(a.fanSpeed, fanListener) }
            runCatching { seat.unregisterHvacSeatTemperature(area, seatListener) }
            runCatching { seat.unregisterHvacSeatVentilation(area, ventListener) }
            runCatching { steering.unregisterHvacSteeringWheelHeatState(steeringListener) }
        }
    }.distinctUntilChanged()

    /** 열선·통풍은 동시에 켜지 않으므로, 끄는(0) 쪽을 먼저 보낸다 */
    private fun seatSteps(target: ClimateState): Array<Pair<String, suspend () -> Int>> {
        val heat = target.seatHeatLevel?.let { l ->
            "시트 열선" to suspend { sdkCall<Int>("set seat heat") { ok, err -> seat.setHvacSeatTemperature(area, l, { _, v -> ok(v) }, err) } }
        }
        val vent = target.seatVentLevel?.let { l ->
            "시트 통풍" to suspend { sdkCall<Int>("set seat vent") { ok, err -> seat.setHvacSeatVentilation(area, l, { _, v -> ok(v) }, err) } }
        }
        val order = if ((target.seatHeatLevel ?: 0) > 0) listOf(vent, heat) else listOf(heat, vent)
        return order.filterNotNull().toTypedArray()
    }

    private companion object {
        const val VERIFY_DELAY_MS = 300L
    }

    private fun FanDirectionType.toDomain(): FanDirection? = when (this) {
        FanDirectionType.FACE -> FanDirection.FACE
        FanDirectionType.FLOOR -> FanDirection.FLOOR
        FanDirectionType.FACE_AND_FLOOR -> FanDirection.FACE_AND_FLOOR
        FanDirectionType.DEFROST -> FanDirection.DEFROST
        FanDirectionType.FACE_AND_DEFROST -> FanDirection.FACE_AND_DEFROST
        FanDirectionType.DEFROST_AND_FLOOR -> FanDirection.DEFROST_AND_FLOOR
        FanDirectionType.FACE_AND_FLOOR_AND_DEFROST -> FanDirection.ALL
        else -> null
    }

    private fun FanDirection.toSdk(): FanDirectionType = when (this) {
        FanDirection.FACE -> FanDirectionType.FACE
        FanDirection.FLOOR -> FanDirectionType.FLOOR
        FanDirection.FACE_AND_FLOOR -> FanDirectionType.FACE_AND_FLOOR
        FanDirection.DEFROST -> FanDirectionType.DEFROST
        FanDirection.FACE_AND_DEFROST -> FanDirectionType.FACE_AND_DEFROST
        FanDirection.DEFROST_AND_FLOOR -> FanDirectionType.DEFROST_AND_FLOOR
        FanDirection.ALL -> FanDirectionType.FACE_AND_FLOOR_AND_DEFROST
    }
}
