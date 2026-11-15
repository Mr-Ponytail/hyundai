package com.example.moodrive.vehicle.pleos

import ai.pleos.playground.vehicle.api.model.capability.AccessType
import com.example.moodrive.domain.model.Access
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Pleos SDK의 (성공 콜백, 실패 콜백) 형태를 suspend 함수로 바꾼다. 콜백이 오기 전엔 반환하지 않는다. */
internal suspend fun <T> sdkCall(
    name: String,
    block: (onSuccess: (T?) -> Unit, onError: (Exception) -> Unit) -> Unit,
): T = suspendCancellableCoroutine { cont ->
    block(
        { value ->
            when {
                !cont.isActive -> Unit
                value == null -> cont.resumeWithException(IllegalStateException("$name: null 응답"))
                else -> cont.resume(value)
            }
        },
        { error -> if (cont.isActive) cont.resumeWithException(error) },
    )
}

/** WRITE 단독은 현재 상태를 읽을 수 없어 복원이 불가능하므로 NONE으로 취급한다. */
internal fun AccessType?.toAccess(): Access = when (this) {
    AccessType.READ_WRITE -> Access.READ_WRITE
    AccessType.READ -> Access.READ
    else -> Access.NONE
}
