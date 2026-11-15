package com.example.moodrive.domain.port

import com.example.moodrive.domain.model.AmbientLightState
import com.example.moodrive.domain.model.ClimateCapabilities
import com.example.moodrive.domain.model.ClimateState
import com.example.moodrive.domain.model.DriveMode
import com.example.moodrive.domain.model.LibraryEntry
import com.example.moodrive.domain.model.LightCapabilities
import com.example.moodrive.domain.model.Playlist
import com.example.moodrive.domain.model.Scene
import com.example.moodrive.domain.model.UserProfile
import com.example.moodrive.domain.model.VehicleSnapshot
import kotlinx.coroutines.flow.Flow

/*
 * 도메인이 바깥 세계에 요구하는 인터페이스(포트).
 * 구현은 :vehicle(Pleos SDK, Mock)과 :app(저장소)에 있다. 도메인은 SDK를 모른다.
 *
 * 규칙: 실패는 예외로 던진다. suspend 함수는 SDK 콜백 성공을 받은 뒤에만 반환한다.
 */

interface LightController {
    suspend fun capabilities(): LightCapabilities
    suspend fun read(): AmbientLightState
    suspend fun apply(target: AmbientLightState)
    /** 현재 상태를 먼저 내보내고, 이후 변경될 때마다 내보낸다 (가상 콘솔 조작 포함) */
    fun observe(): Flow<AmbientLightState>
}

/** 여러 항목 중 일부만 적용됐을 때. 적용된 항목은 그대로 두고 실패한 항목만 [message]로 알린다. */
class PartialApplyException(message: String) : Exception(message)

interface ClimateController {
    suspend fun capabilities(): ClimateCapabilities
    suspend fun read(): ClimateState
    /**
     * null 필드는 건드리지 않는다. 항목별로 독립 적용하며,
     * 일부만 실패하면 [PartialApplyException], 전부 실패하면 그 외 예외를 던진다.
     */
    suspend fun apply(target: ClimateState)
    fun observe(): Flow<ClimateState>
}

interface DriveStateSource {
    val driveMode: Flow<DriveMode>
}

/** 내 Scene 라이브러리. 지금은 로컬 샘플, 이후 Firestore 동기화 구현으로 교체 */
interface SceneRepository {
    val entries: Flow<List<LibraryEntry>>
    suspend fun get(sceneId: String): LibraryEntry?
    suspend fun markApplied(sceneId: String, at: Long)
    suspend fun setFavorite(sceneId: String, favorite: Boolean)
    /** 무드 수정 저장. 같은 id의 Scene을 바꾸고, 없으면 라이브러리 맨 앞에 추가한다 (탐색에서 가져온 무드) */
    suspend fun save(scene: Scene)
}

interface ProfileRepository {
    val profile: Flow<UserProfile>
}

/** 적용 전 스냅샷 영속화. 앱이 죽거나 시동이 꺼져도 다음 실행에서 복원할 수 있어야 한다. */
interface SnapshotStore {
    suspend fun load(): VehicleSnapshot?
    suspend fun save(snapshot: VehicleSnapshot)
    suspend fun clear()
}

/** Scene 무드에 맞는 플레이리스트 추천. 지금은 샘플, 이후 Spotify 검색 API 연동으로 교체 */
interface PlaylistRecommender {
    suspend fun recommend(scene: Scene, limit: Int = 2): List<Playlist>
    /** "더 보러가기" 목록 위에 보여주는 검색어 (예: "가을 분위기") */
    fun searchQuery(scene: Scene): String
    /** 검색창을 눌렀을 때 여는 음악 서비스 검색 주소 */
    fun moreUrl(scene: Scene): String
}
