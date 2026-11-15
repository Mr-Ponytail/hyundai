package com.example.moodrive.domain.port

import com.example.moodrive.domain.model.AmbientLightState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * 실제 제어가 안 될 때의 시연용 대체 경로 ("안내 모드").
 *
 * 1. 쓰기 권한이 있으면 먼저 실제로 적용하고, 다시 읽어 차량에 반영됐는지 확인한다.
 *    (Pleos Playground 에뮬레이터는 권한이 READ_WRITE이고 성공 콜백도 주지만 실제 값은 바뀌지 않는다)
 * 2. 권한이 없거나 반영되지 않았으면 [guide]로 "이 값으로 바꿔 주세요"를 내보내고,
 *    사용자가 가상 콘솔에서 값을 바꿔 [observe]로 같은 값이 감지되면 성공으로 본다.
 *
 * UI는 [guide]가 null이 아닐 때 안내 모드임을 명확히 표시해야 한다 (실제 제어로 오인하게 하지 않는다).
 */
class GuidedLightController(
    private val delegate: LightController,
    private val timeout: Duration = 60.seconds,
    private val verifyAttempts: Int = 5,
    private val verifyInterval: Duration = 200.milliseconds,
) : LightController by delegate {

    private val _guide = MutableStateFlow<AmbientLightState?>(null)
    val guide: StateFlow<AmbientLightState?> = _guide.asStateFlow()

    override suspend fun apply(target: AmbientLightState) {
        if (delegate.capabilities().access.canWrite) {
            delegate.apply(target)
            if (isReflected(target)) return
        }
        awaitManualChange(target)
    }

    private suspend fun isReflected(target: AmbientLightState): Boolean {
        repeat(verifyAttempts) {
            if (runCatching { delegate.read() }.getOrNull()?.visiblyEquals(target) == true) return true
            delay(verifyInterval)
        }
        return false
    }

    private suspend fun awaitManualChange(target: AmbientLightState) {
        _guide.value = target
        try {
            withTimeout(timeout) { delegate.observe().first { it.visiblyEquals(target) } }
        } finally {
            _guide.value = null
        }
    }
}
