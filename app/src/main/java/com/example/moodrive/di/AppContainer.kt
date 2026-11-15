package com.example.moodrive.di

import android.content.Context
import com.example.moodrive.BuildConfig
import com.example.moodrive.data.PrefsSnapshotStore
import com.example.moodrive.data.SamplePlaylistRecommender
import com.example.moodrive.data.SampleProfileRepository
import com.example.moodrive.data.SampleSceneRepository
import com.example.moodrive.domain.port.PlaylistRecommender
import com.example.moodrive.domain.port.ProfileRepository
import com.example.moodrive.domain.port.SceneRepository
import com.example.moodrive.domain.session.SceneSession
import com.example.moodrive.vehicle.VehicleModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * 수동 DI. 앱 전체에서 하나만 존재하는 객체를 만든다.
 * (일정상 Hilt 대신 수동 조립. 객체 수가 늘어나면 Hilt로 옮긴다)
 */
class AppContainer(context: Context) {

    /** 화면과 무관하게 앱 프로세스 동안 도는 작업용 (시작 시 세션 복구 등) */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val vehicle: VehicleModule =
        if (BuildConfig.VEHICLE_IMPL == "mock") VehicleModule.mock() else VehicleModule.pleos(context)

    val snapshotStore = PrefsSnapshotStore(context)

    val sceneSession = SceneSession(vehicle.light, vehicle.climate, snapshotStore)

    // 서버 연동 전 임시 구현
    val sceneRepository: SceneRepository = SampleSceneRepository()
    val profileRepository: ProfileRepository = SampleProfileRepository()
    val playlistRecommender: PlaylistRecommender = SamplePlaylistRecommender()
}
