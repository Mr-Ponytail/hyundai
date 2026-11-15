package com.example.moodrive.ui.control

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moodrive.domain.model.AmbientLightState
import com.example.moodrive.domain.model.ClimateState
import com.example.moodrive.domain.model.DriveMode
import com.example.moodrive.domain.model.Playlist
import com.example.moodrive.domain.model.Scene
import com.example.moodrive.domain.port.PlaylistRecommender
import com.example.moodrive.domain.port.SceneRepository
import com.example.moodrive.domain.session.ItemResult
import com.example.moodrive.domain.session.ScenePlan
import com.example.moodrive.domain.session.ScenePlanner
import com.example.moodrive.domain.session.SceneSession
import com.example.moodrive.domain.session.SessionState
import com.example.moodrive.ui.common.LOG_TAG
import com.example.moodrive.ui.common.driveModeOrUnknown
import com.example.moodrive.ui.common.toggleFavorite
import com.example.moodrive.ui.mood.MoodDraft
import com.example.moodrive.vehicle.VehicleModule
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SceneControlUiState(
    val title: String = "",
    val tags: String = "",
    val imageUrl: String? = null,
    val paletteArgb: List<Int> = emptyList(),
    val isFavorite: Boolean = false,
    val driveMode: DriveMode = DriveMode.UNKNOWN,
    /** 추천 플레이리스트. 카드는 앞의 2개, "더 보러가기" 목록은 전부 보여준다 */
    val playlists: List<Playlist> = emptyList(),
    /** "더 보러가기" 목록 위 검색어 */
    val moreQuery: String = "",
    val moreUrl: String? = null,
    val cards: List<ControlCardUi> = emptyList(),
    /** 카드에 보여주는 목표값 (Scene 값을 이 차량에 맞춘 것) */
    val targets: ControlTargets = ControlTargets(),
    val guide: AmbientLightState? = null,
    val message: String? = null,
    /** 무드 수정 화면이 열려 있으면 고치는 중인 값 */
    val moodDraft: MoodDraft? = null,
    val isSaving: Boolean = false,
    /** 탐색에서 연 상세 화면: 아직 차량에 적용하지 않았다 (하단 버튼이 저장·적용으로 바뀐다) */
    val isPreview: Boolean = false,
    /** 이 Scene이 내 Mood에 있는지 */
    val isSaved: Boolean = false,
) {
    /** 기획서 4-4: 편집은 정차 중에만 */
    val canEdit get() = driveMode != DriveMode.DRIVING
}

/**
 * Scene 실행 화면. 홈에서 "적용하기"를 누르면 [open]으로 Scene을 적용한다.
 *
 * - 하단 카드는 보여주기만 한다. 값을 바꾸는 것은 "무드 수정"에서만 하고, 저장하면 다시 적용한다.
 * - 조명과 공조를 각각 따로 적용한다. 조명은 안내 모드에서 최대 60초 기다릴 수 있어서,
 *   한 번에 적용하면 그동안 공조 적용이 막힌다.
 */
class SceneControlViewModel(
    private val scenes: SceneRepository,
    private val playlists: PlaylistRecommender,
    private val vehicle: VehicleModule,
    private val session: SceneSession,
    private val clock: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    private data class Loaded(
        val scene: Scene,
        val plan: ScenePlan,
        val base: ControlTargets,
        val playlists: List<Playlist>,
        val moreQuery: String,
        val moreUrl: String,
    )

    private val loaded = MutableStateFlow<Loaded?>(null)
    private val message = MutableStateFlow<String?>(null)
    private val draft = MutableStateFlow<MoodDraft?>(null)
    private val saving = MutableStateFlow(false)
    private val previewing = MutableStateFlow(false)

    /** 지금 화면에 연(또는 여는 중인) Scene. 같은 무드로 다시 들어올 때 적용 중인 작업을 끊지 않게 한다 */
    private var currentId: String? = null
    private var openJob: Job? = null
    private var lightJob: Job? = null
    private var climateJob: Job? = null

    private val driveMode = vehicle.driveModeOrUnknown()

    val uiState: StateFlow<SceneControlUiState> =
        combine(loaded, scenes.entries) { l, entries ->
            if (l == null) return@combine SceneControlUiState()
            val entry = entries.firstOrNull { it.scene.id == l.scene.id }
            SceneControlUiState(
                title = l.scene.name,
                tags = l.scene.mood.keywords.joinToString(" · "),
                imageUrl = l.scene.visual.backdropUrl ?: l.scene.visual.imageUrl,
                paletteArgb = l.scene.visual.palette.map { it.argb },
                isFavorite = entry?.isFavorite == true,
                isSaved = entry != null,
                playlists = l.playlists,
                moreQuery = l.moreQuery,
                moreUrl = l.moreUrl,
                cards = SceneControlFormatter.cards(l.base),
                targets = l.base,
            )
        }
            .combine(combine(message, vehicle.light.guide, driveMode, ::Triple)) { s, (msg, guide, drive) ->
                s.copy(message = msg, guide = guide, driveMode = drive)
            }
            .combine(combine(draft, saving, previewing, ::Triple)) { s, (d, busy, preview) ->
                s.copy(moodDraft = d, isSaving = busy, isPreview = preview)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SceneControlUiState())

    /**
     * 화면 진입. 이미 이 Scene이 실행 중이면 다시 보여주기만 한다.
     * @param reapply 실행 중이어도 다시 적용 (무드 저장 후)
     */
    fun open(sceneId: String, reapply: Boolean = false) {
        val active = activeSceneId()
        if (!reapply && loaded.value?.scene?.id == sceneId && active == sceneId) return

        reset()
        previewing.value = false
        currentId = sceneId
        openJob = viewModelScope.launch {
            val entry = scenes.get(sceneId) ?: return@launch
            val plan = load(entry.scene).plan
            Log.i(LOG_TAG, "open ${entry.scene.id}: $plan")
            if (!reapply && active == sceneId) return@launch

            // 공조를 먼저 보내야 조명 안내 모드 대기 중에도 공조가 적용된다
            plan.climate?.let(::applyClimate)
            plan.light?.let(::applyLight)
            if (plan.light == null && plan.climate == null) message.value = "이 차량에서 적용할 수 있는 항목이 없어요"
        }
    }

    /**
     * 탐색 무드 상세 화면 진입. 차량에는 아직 적용하지 않는다.
     * 이미 연 무드면(뒤로 갔다 다시 열기, 적용 중) 그대로 두고, 실행 중인 무드면 실행 화면으로 연다.
     */
    fun preview(scene: Scene) {
        if (currentId == scene.id) return
        if (activeSceneId() == scene.id) {
            open(scene.id)
            return
        }
        reset()
        previewing.value = true
        currentId = scene.id
        openJob = viewModelScope.launch {
            val l = load(scene)
            Log.i(LOG_TAG, "preview ${scene.id}: ${l.plan}")
        }
    }

    /** 상세 화면의 "내 Mood에 저장" */
    fun onSavePreview() {
        loaded.value?.scene?.let(::saveToLibrary)
    }

    /** 탐색 무드를 내 Mood에 넣는다. 이미 있으면 내 Mood에서 고친 값을 덮어쓰지 않는다 */
    private fun saveToLibrary(scene: Scene) {
        viewModelScope.launch {
            if (scenes.get(scene.id) != null) {
                message.value = "이미 내 Mood에 있는 무드예요"
                return@launch
            }
            scenes.save(scene)
            message.value = "'${scene.name}'을 내 Mood에 저장했어요"
        }
    }

    /** 상세 화면의 "Mood 적용" */
    fun onApplyPreview() {
        if (!previewing.value) return
        loaded.value?.scene?.let(::applyExplore)
    }

    /**
     * 탐색 무드를 적용한다. 실행 중인 무드를 홈에서 다시 찾고 종료할 수 있도록 내 Mood에 넣은 뒤 적용한다.
     * 이미 저장한 무드는 내 Mood의 값으로 적용한다.
     */
    private fun applyExplore(scene: Scene) {
        previewing.value = false
        currentId = scene.id
        viewModelScope.launch {
            if (scenes.get(scene.id) == null) scenes.save(scene)
            open(scene.id, reapply = true)
        }
    }

    fun onToggleFavorite() {
        val id = loaded.value?.scene?.id ?: return
        viewModelScope.launch { scenes.toggleFavorite(id) }
    }

    /** Scene 종료 → 적용 전 상태로 복원. 화면은 바로 닫고 복원은 뒤에서 계속한다 */
    fun onEnd() {
        cancelJobs()
        currentId = null
        viewModelScope.launch {
            val report = session.end()
            Log.i(LOG_TAG, "restore: $report")
            val problems = listOf(report.light, report.climate).mapNotNull {
                when (it) { is ItemResult.Failed -> it.reason; is ItemResult.Partial -> it.reason; else -> null }
            }
            message.value = if (problems.isEmpty()) "원래 설정으로 되돌렸어요" else "일부 복원 실패: ${problems.first()}"
            loaded.value = null
        }
    }

    // ───────── 무드 수정 ─────────

    /** 지금 실행 중인 값에서 시작한다 */
    fun onEditMood() {
        if (previewing.value) return
        val l = loaded.value ?: return
        draft.value = MoodDraft.from(l.scene, l.base, l.plan.colorMatch?.source)
    }

    fun onDraftChange(value: MoodDraft) {
        if (draft.value != null) draft.value = value
    }

    fun onCloseEditor() {
        draft.value = null
    }

    /** Scene에 저장하고, 저장한 값으로 다시 적용한다 */
    fun onSaveMood() {
        val d = draft.value ?: return
        val l = loaded.value ?: return
        if (saving.value) return
        viewModelScope.launch {
            saving.value = true
            try {
                val updated = d.applyTo(l.scene)
                scenes.save(updated)
                Log.i(LOG_TAG, "mood saved: $updated")
                draft.value = null
                message.value = "'${updated.name}' 무드를 저장했어요"
                open(updated.id, reapply = true)
            } finally {
                saving.value = false
            }
        }
    }

    fun onMessageShown() {
        message.value = null
    }

    // ───────── 적용 ─────────

    private fun activeSceneId() = (session.state.value as? SessionState.Active)?.sceneId

    private fun reset() {
        cancelJobs()
        draft.value = null
    }

    /** 차량 능력에 맞춰 적용값을 계산해 화면에 올린다 (적용은 하지 않음) */
    private suspend fun load(scene: Scene): Loaded {
        val lightCaps = runCatching { vehicle.light.capabilities() }.getOrNull()
        val climateCaps = runCatching { vehicle.climate.capabilities() }.getOrNull()
        val plan = ScenePlanner.plan(scene, lightCaps, climateCaps, applyComfort = true)
        return Loaded(
            scene = scene,
            plan = plan,
            base = ControlTargets.base(scene, plan),
            playlists = runCatching { playlists.recommend(scene, limit = MORE_PLAYLISTS) }.getOrDefault(emptyList()),
            moreQuery = playlists.searchQuery(scene),
            moreUrl = playlists.moreUrl(scene),
        ).also { loaded.value = it }
    }

    private fun applyLight(target: AmbientLightState) {
        val sceneId = loaded.value?.scene?.id ?: return
        lightJob?.cancel()
        lightJob = viewModelScope.launch {
            val result = session.apply(ScenePlan(sceneId, target, null, null)).light
            Log.i(LOG_TAG, "light $target → $result")
            onApplied(sceneId, "조명", result)
        }
    }

    private fun applyClimate(target: ClimateState) {
        val sceneId = loaded.value?.scene?.id ?: return
        climateJob?.cancel()
        climateJob = viewModelScope.launch {
            val result = session.apply(ScenePlan(sceneId, null, target, null)).climate
            Log.i(LOG_TAG, "climate $target → $result")
            onApplied(sceneId, "공조", result)
        }
    }

    private suspend fun onApplied(sceneId: String, label: String, result: ItemResult) {
        when (result) {
            ItemResult.Applied, is ItemResult.Partial -> scenes.markApplied(sceneId, clock())
            else -> Unit
        }
        when (result) {
            is ItemResult.Failed -> message.value = "$label 적용 실패: ${result.reason}"
            is ItemResult.Partial -> message.value = "$label 일부 적용: ${result.reason}"
            else -> Unit
        }
    }

    private fun cancelJobs() {
        openJob?.cancel()
        lightJob?.cancel()
        climateJob?.cancel()
    }
}

/** "더 보러가기" 목록에 보여줄 추천 플레이리스트 수 (시안 9개) */
private const val MORE_PLAYLISTS = 9
