package com.example.moodrive.data

import android.content.Context
import com.example.moodrive.domain.model.AmbientLightState
import com.example.moodrive.domain.model.ClimateState
import com.example.moodrive.domain.model.FanDirection
import com.example.moodrive.domain.model.VehicleSnapshot
import com.example.moodrive.domain.port.SnapshotStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 적용 전 스냅샷을 SharedPreferences에 동기 저장(commit)한다.
 * 앱이 죽어도 다음 실행 때 SceneSession.recoverIfNeeded()로 복원할 수 있게 하는 것이 목적이라 apply()가 아닌 commit()을 쓴다.
 */
class PrefsSnapshotStore(context: Context) : SnapshotStore {

    private val prefs = context.applicationContext.getSharedPreferences("scene_snapshot", Context.MODE_PRIVATE)

    override suspend fun load(): VehicleSnapshot? = withContext(Dispatchers.IO) {
        val light = if (prefs.getBoolean(HAS_LIGHT, false)) {
            AmbientLightState(
                on = prefs.getBoolean(LIGHT_ON, false),
                colorArgb = prefs.getInt(LIGHT_COLOR, 0),
                brightness = prefs.getInt(LIGHT_BRIGHTNESS, 0),
                autoColorByDriveMode = when (prefs.getInt(LIGHT_AUTO, -1)) { 0 -> false; 1 -> true; else -> null },
            )
        } else null

        val climate = if (prefs.getBoolean(HAS_CLIMATE, false)) {
            ClimateState(
                temperatureC = prefs.getFloat(CLIMATE_TEMP, Float.NaN).takeUnless { it.isNaN() },
                fanSpeed = prefs.getInt(CLIMATE_FAN, -1).takeIf { it >= 0 },
                fanDirection = prefs.getString(CLIMATE_FAN_DIR, null)?.let { runCatching { FanDirection.valueOf(it) }.getOrNull() },
                seatHeatLevel = prefs.getInt(CLIMATE_SEAT, -1).takeIf { it >= 0 },
                seatVentLevel = prefs.getInt(CLIMATE_SEAT_VENT, -1).takeIf { it >= 0 },
                steeringWheelHeatLevel = prefs.getInt(CLIMATE_STEERING, -1).takeIf { it >= 0 },
            )
        } else null

        VehicleSnapshot(light, climate).takeUnless { it.isEmpty }
    }

    override suspend fun save(snapshot: VehicleSnapshot) = withContext(Dispatchers.IO) {
        val editor = prefs.edit().clear()
        snapshot.light?.let {
            editor.putBoolean(HAS_LIGHT, true)
                .putBoolean(LIGHT_ON, it.on)
                .putInt(LIGHT_COLOR, it.colorArgb)
                .putInt(LIGHT_BRIGHTNESS, it.brightness)
                .putInt(LIGHT_AUTO, when (it.autoColorByDriveMode) { false -> 0; true -> 1; null -> -1 })
        }
        snapshot.climate?.let {
            editor.putBoolean(HAS_CLIMATE, true)
                .putFloat(CLIMATE_TEMP, it.temperatureC ?: Float.NaN)
                .putInt(CLIMATE_FAN, it.fanSpeed ?: -1)
                .putString(CLIMATE_FAN_DIR, it.fanDirection?.name)
                .putInt(CLIMATE_SEAT, it.seatHeatLevel ?: -1)
                .putInt(CLIMATE_SEAT_VENT, it.seatVentLevel ?: -1)
                .putInt(CLIMATE_STEERING, it.steeringWheelHeatLevel ?: -1)
        }
        check(editor.commit()) { "스냅샷 저장 실패" }
    }

    override suspend fun clear() = withContext(Dispatchers.IO) {
        check(prefs.edit().clear().commit()) { "스냅샷 삭제 실패" }
    }

    private companion object {
        const val HAS_LIGHT = "has_light"
        const val LIGHT_ON = "light_on"
        const val LIGHT_COLOR = "light_color"
        const val LIGHT_BRIGHTNESS = "light_brightness"
        const val LIGHT_AUTO = "light_auto"
        const val HAS_CLIMATE = "has_climate"
        const val CLIMATE_TEMP = "climate_temp"
        const val CLIMATE_FAN = "climate_fan"
        const val CLIMATE_FAN_DIR = "climate_fan_dir"
        const val CLIMATE_SEAT = "climate_seat"
        const val CLIMATE_SEAT_VENT = "climate_seat_vent"
        const val CLIMATE_STEERING = "climate_steering"
    }
}
