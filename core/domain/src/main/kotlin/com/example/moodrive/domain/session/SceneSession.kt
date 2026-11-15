package com.example.moodrive.domain.session

import com.example.moodrive.domain.model.VehicleSnapshot
import com.example.moodrive.domain.port.ClimateController
import com.example.moodrive.domain.port.LightController
import com.example.moodrive.domain.port.PartialApplyException
import com.example.moodrive.domain.port.SnapshotStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.cancellation.CancellationException

sealed interface ItemResult {
    data object Applied : ItemResult
    /** 일부 항목만 적용됨 (예: 공조 전원이 꺼져 바람 설정만 실패) */
    data class Partial(val reason: String) : ItemResult
    data object NotRequested : ItemResult
    data class Failed(val reason: String) : ItemResult
}

data class ApplyReport(val light: ItemResult, val climate: ItemResult)
data class RestoreReport(val light: ItemResult, val climate: ItemResult)

sealed interface SessionState {
    data object Idle : SessionState
    data class Applying(val sceneId: String) : SessionState
    data class Active(val sceneId: String, val report: ApplyReport) : SessionState
    data object Restoring : SessionState
}

/**
 * Scene 적용·복원 상태 머신.
 *
 *   Idle ─apply→ Applying ─→ Active ─end→ Restoring ─→ Idle
 *                               └─apply(다른 Scene)→ Applying (최초 스냅샷 유지)
 *
 * 규칙
 * - 적용 전에 해당 영역의 현재 상태를 스냅샷으로 저장한다. 스냅샷을 못 뜨면 그 영역은 적용하지 않는다 (되돌릴 수 없으므로).
 * - 이미 Scene이 적용된 상태에서 다른 Scene을 적용하면 기존 스냅샷을 유지한다 → 종료 시 "최초" 상태로 돌아간다.
 * - 조명 → 공조 순서로 적용하고, 한 영역이 실패해도 나머지는 계속한다.
 * - 조명만/공조만 담은 계획은 서로 막지 않고 동시에 진행된다 (실행 화면의 항목별 조절).
 * - 복원에 실패한 영역은 스냅샷에 남겨 두고 [recoverIfNeeded]에서 다시 시도한다.
 */
class SceneSession(
    private val light: LightController,
    private val climate: ClimateController,
    private val store: SnapshotStore,
) {
    /** 스냅샷 읽기-수정-저장 구간 */
    private val snapshotLock = Mutex()
    /**
     * 영역별 적용 잠금. 조명은 안내 모드에서 최대 60초 기다릴 수 있으므로,
     * 그동안에도 공조 조절은 진행되도록 영역마다 따로 잠근다.
     */
    private val lightLock = Mutex()
    private val climateLock = Mutex()
    private val _state = MutableStateFlow<SessionState>(SessionState.Idle)
    val state: StateFlow<SessionState> = _state.asStateFlow()

    suspend fun apply(plan: ScenePlan): ApplyReport {
        val previous = _state.value

        // 1) 이번에 처음 건드리는 영역만 스냅샷
        var lightSnapshotError: String? = null
        var climateSnapshotError: String? = null
        val snapshot = snapshotLock.withLock {
            var snapshot = store.load() ?: VehicleSnapshot()
            if (plan.light != null && snapshot.light == null) {
                catching { light.read() }
                    .onSuccess { snapshot = snapshot.copy(light = it) }
                    .onFailure { lightSnapshotError = "적용 전 상태 조회 실패: ${it.message}" }
            }
            val climateSnap = snapshot.climate
            if (plan.climate != null && climateSnap == null) {
                catching { climate.read() }
                    .onSuccess { snapshot = snapshot.copy(climate = it) }
                    .onFailure { climateSnapshotError = "적용 전 상태 조회 실패: ${it.message}" }
            } else if (plan.climate != null && climateSnap != null && plan.climate.onlyKnownIn(climateSnap) != plan.climate) {
                // 이전 스냅샷에 없던 항목(그때 못 읽었거나 새로 생긴 항목)은 아직 건드린 적이 없으므로 지금 값이 원래 값이다.
                // 기존 값은 그대로 두고 빈 항목만 채운다. 실패해도 그 항목만 빠질 뿐이다.
                catching { climate.read() }.onSuccess { snapshot = snapshot.copy(climate = climateSnap.orElse(it)) }
            }
            if (!snapshot.isEmpty) store.save(snapshot)
            snapshot
        }
        _state.value = SessionState.Applying(plan.sceneId)

        try {
            // 2) 조명 → 공조 (한 영역이 실패해도 나머지는 계속)
            val lightResult = when {
                plan.light == null -> ItemResult.NotRequested
                lightSnapshotError != null -> ItemResult.Failed(lightSnapshotError!!)
                else -> lightLock.withLock { catching { light.apply(plan.light) }.toItemResult() }
            }
            // 현재 값을 모르는 항목(미지원 등)은 되돌릴 수 없으므로 뺀다
            val climateTarget = plan.climate?.let { target -> snapshot.climate?.let { target.onlyKnownIn(it) } ?: target }
            val climateResult = when {
                climateTarget == null -> ItemResult.NotRequested
                climateSnapshotError != null -> ItemResult.Failed(climateSnapshotError!!)
                climateTarget.isEmpty -> ItemResult.Failed("이 차량에서 조회할 수 없는 공조 항목")
                else -> climateLock.withLock { catching { climate.apply(climateTarget) }.toItemResult() }
            }

            val report = ApplyReport(lightResult, climateResult)
            _state.value = if (snapshot.isEmpty) SessionState.Idle else SessionState.Active(plan.sceneId, report)
            return report
        } catch (e: CancellationException) {
            // 조절 화면에서 값을 연달아 바꾸면 이전 적용을 취소한다. 상태가 Applying에 머물지 않게 정리
            _state.value = when {
                snapshot.isEmpty -> SessionState.Idle
                previous is SessionState.Active && previous.sceneId == plan.sceneId -> previous
                else -> SessionState.Active(plan.sceneId, ApplyReport(ItemResult.NotRequested, ItemResult.NotRequested))
            }
            throw e
        }
    }

    private suspend fun <T> allLocks(block: suspend () -> T): T =
        lightLock.withLock { climateLock.withLock { snapshotLock.withLock { block() } } }

    /** Scene 종료: 스냅샷 기준으로 복원 */
    suspend fun end(): RestoreReport = allLocks { restoreLocked() }

    /** 앱 시작 시 호출. 이전 실행에서 복원하지 못한 스냅샷이 남아 있으면 복원한다. */
    suspend fun recoverIfNeeded(): RestoreReport? = allLocks {
        if (store.load()?.isEmpty == false) restoreLocked() else null
    }

    private suspend fun restoreLocked(): RestoreReport {
        val snapshot = store.load() ?: VehicleSnapshot()
        _state.value = SessionState.Restoring

        val lightResult = snapshot.light?.let { s -> catching { light.apply(s) }.toItemResult() } ?: ItemResult.NotRequested
        val climateResult = snapshot.climate?.let { s -> catching { climate.apply(s) }.toItemResult() } ?: ItemResult.NotRequested

        val remaining = VehicleSnapshot(
            light = snapshot.light.takeIf { lightResult is ItemResult.Failed },
            climate = snapshot.climate.takeIf { climateResult is ItemResult.Failed || climateResult is ItemResult.Partial },
        )
        if (remaining.isEmpty) store.clear() else store.save(remaining)

        _state.value = SessionState.Idle
        return RestoreReport(lightResult, climateResult)
    }

    private inline fun <T> catching(block: () -> T): Result<T> =
        try {
            Result.success(block())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }

    private fun Result<Unit>.toItemResult(): ItemResult = fold(
        { ItemResult.Applied },
        {
            val reason = it.message ?: it.javaClass.simpleName
            if (it is PartialApplyException) ItemResult.Partial(reason) else ItemResult.Failed(reason)
        },
    )
}
