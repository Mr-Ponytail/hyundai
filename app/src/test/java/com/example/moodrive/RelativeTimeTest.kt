package com.example.moodrive

import com.example.moodrive.ui.common.relativeTimeKo
import org.junit.Assert.assertEquals
import org.junit.Test

class RelativeTimeTest {
    private val now = 1_000_000_000_000L
    private val min = 60_000L
    private val hour = 60 * min
    private val day = 24 * hour

    @Test fun justNow() = assertEquals("방금 전", relativeTimeKo(now - 30_000, now))
    @Test fun minutes() = assertEquals("5분 전", relativeTimeKo(now - 5 * min, now))
    @Test fun hours() = assertEquals("1시간 전", relativeTimeKo(now - hour, now))
    @Test fun days() = assertEquals("4일 전", relativeTimeKo(now - 4 * day, now))
    @Test fun weeks() = assertEquals("2주 전", relativeTimeKo(now - 15 * day, now))
    @Test fun future() = assertEquals("방금 전", relativeTimeKo(now + hour, now))
}
