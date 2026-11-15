package com.example.moodrive.vehicle.pleos

import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.api.model.area.SeatArea
import ai.pleos.playground.vehicle.api.model.capability.AreaBounds
import ai.pleos.playground.vehicle.api.model.capability.CapabilityConfig
import ai.pleos.playground.vehicle.constant.control.LightState
import ai.pleos.playground.vehicle.constant.control.LightSwitch
import ai.pleos.playground.vehicle.listener.AmbientLightBrightnessColorListener
import ai.pleos.playground.vehicle.listener.AmbientLightBrightnessListener
import ai.pleos.playground.vehicle.listener.LightStateListener
import android.util.Log
import com.example.moodrive.domain.model.Access
import com.example.moodrive.domain.model.AmbientLightState
import com.example.moodrive.domain.model.LightCapabilities
import com.example.moodrive.domain.port.LightController
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.coroutines.cancellation.CancellationException

/**
 * @param preferredArea 우선 사용할 영역. 차량이 이 영역을 지원하지 않으면 Capability가 알려준 첫 영역을 쓴다.
 *   (에뮬레이터는 앰비언트 조명을 운전석이 아닌 별도 영역으로 노출한다)
 */
class PleosLightController(
    private val vehicle: Vehicle,
    private val preferredArea: SeatArea,
    private val supportedColors: List<Int>,
) : LightController {

    private val light get() = vehicle.getLight()

    @Volatile private var resolvedArea: SeatArea? = null

    /** 실제로 제어할 영역. 처음 호출 시 Capability로 결정한다. */
    private suspend fun area(): SeatArea = resolvedArea ?: run {
        capabilities()
        resolvedArea ?: preferredArea
    }

    override suspend fun capabilities(): LightCapabilities {
        val access = runCatchingSdk {
            sdkCall<Access>("color capability") { ok, err ->
                light.checkAmbientLightBrightnessColorCapabilities(
                    { config: CapabilityConfig, areas: List<SeatArea> ->
                        val chosen = areas.firstOrNull { it == preferredArea } ?: areas.firstOrNull() ?: preferredArea
                        resolvedArea = chosen
                        Log.i(TAG, "color capability: access=${config.accessType}, areas=${areas.map { it.values.toList() }}, chosen=${chosen.values.toList()}")
                        ok(config.accessType.toAccess())
                    },
                    err,
                )
            }
        } ?: Access.NONE

        val bounds = runCatchingSdk {
            sdkCall<AreaBounds<Int>>("brightness capability") { ok, err ->
                light.checkAmbientLightBrightnessCapabilities(
                    { _: CapabilityConfig, map: Map<SeatArea, AreaBounds<Int>> ->
                        Log.i(TAG, "brightness capability: ${map.mapValues { "${it.value.minValue}..${it.value.maxValue}" }}")
                        ok(map[resolvedArea ?: preferredArea] ?: map.values.firstOrNull())
                    },
                    err,
                )
            }
        }
        val range = bounds?.let { it.minValue..it.maxValue } ?: 0..10

        return LightCapabilities(access, range, supportedColors)
    }

    override suspend fun read(): AmbientLightState {
        val area = area()
        val state = sdkCall<LightState>("ambient state") { ok, err -> light.getAmbientLightState(area, ok, err) }
        val color = sdkCall<Int>("ambient color") { ok, err -> light.getAmbientLightBrightnessColor(area, ok, err) }
        val brightness = sdkCall<Int>("ambient brightness") { ok, err -> light.getAmbientLightBrightness(area, ok, err) }
        val auto = runCatchingSdk {
            sdkCall<Boolean>("auto color") { ok, err -> light.getAmbientColorAutoByDriveModeEnabled(ok, err) }
        }
        return AmbientLightState(state == LightState.ON, color, brightness, auto)
    }

    /**
     * 순서: (자동 색 끄기) → 켜기 → 색 → 밝기 → (끄기) → (자동 색 켜기)
     * 끈 상태로 복원할 때도 색·밝기를 먼저 맞춰 두어야 다음에 켰을 때 원래 색이 나온다.
     */
    override suspend fun apply(target: AmbientLightState) {
        val area = area()
        if (target.autoColorByDriveMode == false) setAutoColor(false)
        if (target.on) setSwitch(area, LightSwitch.ON)
        sdkCall<Int>("set color") { ok, err ->
            light.setAmbientLightBrightnessColor(area, target.colorArgb, { _, v -> ok(v) }, err)
        }
        sdkCall<Int>("set brightness") { ok, err ->
            light.setAmbientLightBrightness(area, target.brightness, { _, v -> ok(v) }, err)
        }
        if (!target.on) setSwitch(area, LightSwitch.OFF)
        if (target.autoColorByDriveMode == true) setAutoColor(true)
    }

    override fun observe(): Flow<AmbientLightState> = callbackFlow {
        val area = area()
        val lock = Any()
        var current = read()
        trySend(current)

        fun update(change: (AmbientLightState) -> AmbientLightState) {
            val next = synchronized(lock) { change(current).also { current = it } }
            trySend(next)
        }

        val colorListener = object : AmbientLightBrightnessColorListener {
            override fun onBrightnessColorUpdated(value: Int?) { value?.let { v -> update { it.copy(colorArgb = v) } } }
            override fun onFailed(e: Exception) { close(e) }
        }
        val brightnessListener = object : AmbientLightBrightnessListener {
            override fun onBrightnessUpdated(value: Int?) { value?.let { v -> update { it.copy(brightness = v) } } }
            override fun onFailed(e: Exception) { close(e) }
        }
        val stateListener = object : LightStateListener {
            override fun onLightStateUpdated(value: LightState?) { value?.let { v -> update { it.copy(on = v == LightState.ON) } } }
            override fun onFailed(e: Exception) { close(e) }
        }
        light.registerAmbientLightBrightnessColorListener(area, colorListener)
        light.registerAmbientLightBrightnessListener(area, brightnessListener)
        light.registerAmbientLightStateListener(area, stateListener)

        awaitClose {
            light.unregisterAmbientLightBrightnessColorListener(area, colorListener)
            light.unregisterAmbientLightBrightnessListener(area, brightnessListener)
            light.unregisterAmbientLightStateListener(area, stateListener)
        }
    }.distinctUntilChanged()

    private suspend fun setSwitch(area: SeatArea, value: LightSwitch) {
        sdkCall<LightSwitch>("set switch") { ok, err -> light.setAmbientLightSwitch(area, value, { _, v -> ok(v) }, err) }
    }

    /** 차량에 따라 미지원(에뮬레이터 포함)이므로 실패해도 Scene 적용은 계속한다 */
    private suspend fun setAutoColor(enabled: Boolean) {
        runCatchingSdk {
            sdkCall<Boolean>("set auto color") { ok, err -> light.setAmbientColorAutoByDriveModeEnabled(enabled, ok, err) }
        }
    }
}

/** 선택적 정보 조회용: 실패하면 로그를 남기고 null (취소는 그대로 전파) */
internal suspend fun <T> runCatchingSdk(block: suspend () -> T): T? =
    try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.w(TAG, "SDK 조회 실패: ${e.javaClass.simpleName}: ${e.message}")
        null
    }

internal const val TAG = "MOODRIVE"
