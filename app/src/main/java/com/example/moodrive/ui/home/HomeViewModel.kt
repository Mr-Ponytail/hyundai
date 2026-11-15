package com.example.moodrive.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moodrive.domain.model.AmbientLightState
import com.example.moodrive.domain.model.ClimateState
import com.example.moodrive.domain.model.DriveMode
import com.example.moodrive.domain.model.LibraryEntry
import com.example.moodrive.domain.model.Scene
import com.example.moodrive.domain.model.UserProfile
import com.example.moodrive.domain.port.ProfileRepository
import com.example.moodrive.domain.port.SceneRepository
import com.example.moodrive.domain.session.SceneSession
import com.example.moodrive.domain.session.SessionState
import com.example.moodrive.ui.common.currentClimate
import com.example.moodrive.ui.common.currentLight
import com.example.moodrive.ui.common.driveModeOrUnknown
import com.example.moodrive.ui.common.relativeTimeKo
import com.example.moodrive.ui.common.toggleFavorite
import com.example.moodrive.vehicle.VehicleModule
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class HomeFilter { ALL, RECENT, FAVORITE }

data class SceneCardUi(
    val id: String,
    val title: String,
    /** "Soft · Cozy" */
    val tags: String,
    val imageUrl: String?,
    /** 이미지가 없을 때 그라데이션 대체용 */
    val paletteArgb: List<Int>,
    /** "1시간 전" (실행한 적 없으면 null) */
    val badge: String?,
    val isActive: Boolean,
    val isFavorite: Boolean = false,
)

/** 카드를 눌러 펼친 상태 */
data class SelectedSceneUi(
    val card: SceneCardUi,
    val details: List<DetailItemUi>,
)

data class HomeUiState(
    val greetingName: String = "",
    val handle: String = "",
    val avatarUrl: String? = null,
    val driveMode: DriveMode = DriveMode.UNKNOWN,
    val filter: HomeFilter = HomeFilter.RECENT,
    val totalCount: Int = 0,
    val favoriteCount: Int = 0,
    val cards: List<SceneCardUi> = emptyList(),
    val selected: SelectedSceneUi? = null,
    /** 세션이 적용/복원 중. 적용·복원은 실행 화면(SceneControlViewModel)이 하고 홈은 상태만 보여준다 */
    val isApplying: Boolean = false,
    /** 제어 권한이 없어 가상 콘솔 조작을 기다리는 중인 목표 조명 값 (안내 모드) */
    val guide: AmbientLightState? = null,
) {
    /** 기획서 4-4: 만들기·편집은 정차 중에만 */
    val canCreate get() = driveMode == DriveMode.PARKED
}

class HomeViewModel(
    private val scenes: SceneRepository,
    profiles: ProfileRepository,
    private val vehicle: VehicleModule,
    private val session: SceneSession,
    private val clock: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    private val filter = MutableStateFlow(HomeFilter.RECENT)
    private val selectedId = MutableStateFlow<String?>(null)

    /** "N시간 전" 표시를 갱신하기 위한 1분 주기 틱 */
    private val minuteTick = flow {
        while (true) {
            emit(clock())
            delay(60_000)
        }
    }

    private val driveMode = vehicle.driveModeOrUnknown()

    // 하단 패널의 "현재" 값. 조회 실패 시 null → "현재 -"
    private val currentLight = vehicle.currentLight()
    private val currentClimate = vehicle.currentClimate()
    private val brightnessRange = flow { emit(runCatching { vehicle.light.capabilities().brightnessRange }.getOrNull()) }

    private val vehicleNow = combine(currentLight, currentClimate, brightnessRange) { l, c, r -> VehicleNow(l, c, r) }

    val uiState: StateFlow<HomeUiState> = combine(scenes.entries, profiles.profile, filter, driveMode, session.state) { entries, profile, filter, drive, sessionState ->
        Input(entries, profile, filter, drive, sessionState)
    }
        .combine(combine(selectedId, vehicleNow, minuteTick, ::Triple)) { input, (selected, now, time) -> input.toUiState(selected, now, time) }
        .combine(vehicle.light.guide) { state, guide ->
            state.copy(guide = guide)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun onFilterSelected(value: HomeFilter) {
        filter.value = value
        selectedId.value = null
    }

    /** 만들기에서 저장한 무드를 내 Mood 맨 앞에 넣고, 바로 보이도록 '전체' 탭으로 바꾼다 */
    fun onMoodCreated(scene: Scene) {
        viewModelScope.launch { scenes.save(scene) }
        onFilterSelected(HomeFilter.ALL)
    }

    /** 카드 탭: 펼치기 / 같은 카드를 다시 누르면 접기 */
    fun onSceneSelected(sceneId: String) {
        selectedId.value = if (selectedId.value == sceneId) null else sceneId
    }

    fun onDismissSelection() {
        selectedId.value = null
    }

    fun onToggleFavorite(sceneId: String) {
        viewModelScope.launch { scenes.toggleFavorite(sceneId) }
    }

    private data class VehicleNow(val light: AmbientLightState?, val climate: ClimateState?, val brightnessRange: IntRange?)

    private data class Input(
        val entries: List<LibraryEntry>,
        val profile: UserProfile,
        val filter: HomeFilter,
        val driveMode: DriveMode,
        val session: SessionState,
    ) {
        fun toUiState(selectedId: String?, vehicle: VehicleNow, now: Long): HomeUiState {
            val activeId = (session as? SessionState.Active)?.sceneId
            val visible = when (filter) {
                HomeFilter.ALL -> entries
                HomeFilter.RECENT -> entries.filter { it.lastAppliedAt != null }.sortedByDescending { it.lastAppliedAt }
                HomeFilter.FAVORITE -> entries.filter { it.isFavorite }
            }
            val cards = visible.map { e ->
                SceneCardUi(
                    id = e.scene.id,
                    title = e.scene.name,
                    tags = e.scene.mood.keywords.joinToString(" · "),
                    imageUrl = e.scene.visual.imageUrl,
                    paletteArgb = e.scene.visual.palette.map { it.argb },
                    badge = e.lastAppliedAt?.let { relativeTimeKo(it, now) },
                    isActive = e.scene.id == activeId,
                    isFavorite = e.isFavorite,
                )
            }
            // 즐겨찾기 해제 등으로 목록에서 빠지면 선택도 해제
            val selected = cards.firstOrNull { it.id == selectedId }?.let { card ->
                val scene = visible.first { it.scene.id == card.id }.scene
                SelectedSceneUi(card, SceneDetailFormatter.items(scene, vehicle.light, vehicle.brightnessRange, vehicle.climate))
            }
            return HomeUiState(
                greetingName = profile.displayName,
                handle = profile.handle,
                avatarUrl = profile.avatarUrl,
                driveMode = driveMode,
                filter = filter,
                totalCount = entries.size,
                favoriteCount = entries.count { it.isFavorite },
                cards = cards,
                selected = selected,
                isApplying = session is SessionState.Applying || session is SessionState.Restoring,
            )
        }
    }
}
