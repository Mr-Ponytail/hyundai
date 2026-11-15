package com.example.moodrive.domain

import com.example.moodrive.domain.model.Access
import com.example.moodrive.domain.model.AmbientLightState
import com.example.moodrive.domain.port.GuidedLightController
import kotlinx.coroutines.async
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class GuidedLightControllerTest {
    private val target = AmbientLightState(on = true, colorArgb = BLUE, brightness = 7)

    @Test fun `쓰기 권한이 있으면 바로 적용`() = runTest {
        val light = FakeLight(access = Access.READ_WRITE)
        val guided = GuidedLightController(light)
        guided.apply(target)
        assertEquals(listOf(target), light.applied)
        assertNull(guided.guide.value)
    }

    @Test fun `조회만 가능하면 안내를 띄우고 콘솔 변경을 감지하면 완료`() = runTest {
        val light = FakeLight(access = Access.READ)
        val guided = GuidedLightController(light)

        val job = async { guided.apply(target) }
        runCurrent()
        assertEquals(target, guided.guide.value)
        assertTrue(light.applied.isEmpty())

        // 사용자가 가상 콘솔에서 값을 바꿈
        light.state.value = target.copy(autoColorByDriveMode = true)
        job.await()
        assertNull(guided.guide.value)
    }

    @Test fun `성공 응답이어도 실제로 반영되지 않으면 안내 모드로 전환`() = runTest {
        val light = FakeLight(access = Access.READ_WRITE).apply { ignoreWrites = true }
        val guided = GuidedLightController(light)

        val job = async { guided.apply(target) }
        advanceTimeBy(2.seconds)
        assertEquals(listOf(target), light.applied)
        assertEquals(target, guided.guide.value)

        light.state.value = target
        job.await()
        assertNull(guided.guide.value)
    }

    @Test fun `시간 안에 맞추지 않으면 실패`() = runTest {
        val light = FakeLight(access = Access.READ)
        val guided = GuidedLightController(light, timeout = 5.seconds)

        val job = async { runCatching { guided.apply(target) } }
        advanceTimeBy(6.seconds)
        assertTrue(job.await().exceptionOrNull() is TimeoutCancellationException)
        assertNull(guided.guide.value)
    }
}
