package com.example.moodrive.ui.common

import android.util.Log
import com.example.moodrive.domain.model.AmbientLightState
import com.example.moodrive.domain.model.ClimateState
import com.example.moodrive.domain.model.DriveMode
import com.example.moodrive.domain.port.SceneRepository
import com.example.moodrive.vehicle.VehicleModule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

/* 홈·실행 화면 ViewModel 공통 */

internal const val LOG_TAG = "MOODRIVE"

/** 주행 상태. 첫 값이 오기 전에는 UNKNOWN */
internal fun VehicleModule.driveModeOrUnknown(): Flow<DriveMode> =
    driveState.driveMode.onStart { emit(DriveMode.UNKNOWN) }

/** 차량의 지금 조명. 첫 값 전·조회 실패 시 null → "현재 -" */
internal fun VehicleModule.currentLight(): Flow<AmbientLightState?> = light.observe()
    .map<AmbientLightState, AmbientLightState?> { it }
    .onStart { emit(null) }
    .catch { Log.w(LOG_TAG, "조명 관찰 실패: ${it.message}"); emit(null) }

/** 차량의 지금 공조. 첫 값 전·조회 실패 시 null */
internal fun VehicleModule.currentClimate(): Flow<ClimateState?> = climate.observe()
    .map<ClimateState, ClimateState?> { it }
    .onStart { emit(null) }
    .catch { Log.w(LOG_TAG, "공조 관찰 실패: ${it.message}"); emit(null) }

internal suspend fun SceneRepository.toggleFavorite(sceneId: String) {
    val entry = get(sceneId) ?: return
    setFavorite(sceneId, !entry.isFavorite)
}
