package com.example.moodrive.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

/** 눌림 효과(리플) 없이 탭만 받는다 */
internal fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier =
    clickable(interactionSource = null, indication = null, onClick = onClick)

/** 탭을 소비만 한다. 패널 안 빈 곳을 눌러도 뒤 레이어(밖을 누르면 닫기 등)로 전달되지 않게 */
internal fun Modifier.consumeTaps(): Modifier =
    clickable(interactionSource = null, indication = null) {}

/** 마지막으로 받은 null 아닌 값. 닫히는 애니메이션 동안 내용을 유지할 때 쓴다 */
@Composable
internal fun <T : Any> rememberLastNonNull(value: T?): T? {
    var last by remember { mutableStateOf<T?>(null) }
    if (value != null) last = value
    return last
}
