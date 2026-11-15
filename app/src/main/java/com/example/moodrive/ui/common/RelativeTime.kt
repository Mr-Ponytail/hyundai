package com.example.moodrive.ui.common

import java.util.concurrent.TimeUnit

/** "방금 전" / "N분 전" / "N시간 전" / "N일 전" / "N주 전" */
fun relativeTimeKo(then: Long, now: Long): String {
    val diff = (now - then).coerceAtLeast(0)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
    val hours = TimeUnit.MILLISECONDS.toHours(diff)
    val days = TimeUnit.MILLISECONDS.toDays(diff)
    return when {
        minutes < 1 -> "방금 전"
        hours < 1 -> "${minutes}분 전"
        days < 1 -> "${hours}시간 전"
        days < 7 -> "${days}일 전"
        else -> "${days / 7}주 전"
    }
}
