package com.example.moodrive.domain

import com.example.moodrive.domain.model.AmbientLightState
import com.example.moodrive.domain.model.ClimateState
import com.example.moodrive.domain.session.ItemResult
import com.example.moodrive.domain.session.ScenePlan
import com.example.moodrive.domain.session.SceneSession
import com.example.moodrive.domain.session.SessionState
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SceneSessionTest {
    private val light = FakeLight()
    private val climate = FakeClimate()
    private val store = MemorySnapshotStore()
    private val session = SceneSession(light, climate, store)

    private val original = light.state.value
    private val blueLight = AmbientLightState(on = true, colorArgb = BLUE, brightness = 7, autoColorByDriveMode = false)
    private val purpleLight = blueLight.copy(colorArgb = PURPLE)

    private fun plan(id: String, light: AmbientLightState?, climate: ClimateState? = null) =
        ScenePlan(id, light, climate, colorMatch = null)

    @Test fun `적용 후 종료하면 원래 상태로 복원`() = runTest {
        val report = session.apply(plan("a", blueLight, ClimateState(temperatureC = 20f)))
        assertEquals(ItemResult.Applied, report.light)
        assertEquals(ItemResult.Applied, report.climate)
        assertEquals(blueLight, light.state.value)
        assertEquals(20f, climate.state.value.temperatureC)
        assertTrue(session.state.value is SessionState.Active)

        session.end()
        assertEquals(original, light.state.value)
        assertEquals(ClimateState(22f, 2, seatHeatLevel = 0), climate.state.value)
        assertNull(store.value)
        assertEquals(SessionState.Idle, session.state.value)
    }

    @Test fun `스냅샷은 적용 전에 저장된다`() = runTest {
        light.failApply = true
        session.apply(plan("a", blueLight))
        assertEquals(original, store.value?.light)
    }

    @Test fun `공조는 요청하지 않으면 건드리지 않는다`() = runTest {
        val report = session.apply(plan("a", blueLight, climate = null))
        assertEquals(ItemResult.NotRequested, report.climate)
        assertNull(store.value?.climate)
    }

    @Test fun `공조가 실패해도 조명은 적용된다`() = runTest {
        climate.failApply = true
        val report = session.apply(plan("a", blueLight, ClimateState(temperatureC = 20f)))
        assertEquals(ItemResult.Applied, report.light)
        assertTrue(report.climate is ItemResult.Failed)
        assertEquals(blueLight, light.state.value)
    }

    @Test fun `상태 조회에 실패하면 되돌릴 수 없으므로 적용하지 않는다`() = runTest {
        light.failRead = true
        val report = session.apply(plan("a", blueLight))
        assertTrue(report.light is ItemResult.Failed)
        assertTrue(light.applied.isEmpty())
        assertEquals(SessionState.Idle, session.state.value)
    }

    @Test fun `다른 Scene으로 바꿔도 종료 시 최초 상태로 복원`() = runTest {
        session.apply(plan("a", blueLight))
        session.apply(plan("b", purpleLight))
        assertEquals(purpleLight, light.state.value)

        session.end()
        assertEquals(original, light.state.value)
    }

    @Test fun `복원 실패한 영역은 남겨 두고 다음 실행에서 복구`() = runTest {
        session.apply(plan("a", blueLight, ClimateState(temperatureC = 20f)))
        light.failApply = true

        val report = session.end()
        assertTrue(report.light is ItemResult.Failed)
        assertEquals(ItemResult.Applied, report.climate)
        assertEquals(original, store.value?.light)
        assertNull(store.value?.climate)

        // 앱 재시작
        light.failApply = false
        val restarted = SceneSession(light, climate, store)
        val recovered = restarted.recoverIfNeeded()
        assertEquals(ItemResult.Applied, recovered?.light)
        assertEquals(original, light.state.value)
        assertNull(store.value)
    }

    @Test fun `현재 값을 읽을 수 없는 공조 항목은 적용하지 않는다`() = runTest {
        // FakeClimate는 핸들 열선을 읽지 못함(null) → 미지원 취급
        val report = session.apply(plan("a", null, ClimateState(seatHeatLevel = 2, steeringWheelHeatLevel = 1)))
        assertEquals(ItemResult.Applied, report.climate)
        assertEquals(2, climate.state.value.seatHeatLevel)
        assertNull(climate.state.value.steeringWheelHeatLevel)

        session.end()
        assertEquals(0, climate.state.value.seatHeatLevel)
    }

    @Test fun `적용할 수 있는 공조 항목이 하나도 없으면 실패로 알린다`() = runTest {
        val report = session.apply(plan("a", null, ClimateState(steeringWheelHeatLevel = 1)))
        assertTrue(report.climate is ItemResult.Failed)
    }

    @Test fun `공조 일부만 적용되면 Partial로 알린다`() = runTest {
        climate.partialFail = "바람 설정 실패"
        val report = session.apply(plan("a", blueLight, ClimateState(seatHeatLevel = 2)))
        assertEquals(ItemResult.Partial("바람 설정 실패"), report.climate)
        assertTrue(session.state.value is SessionState.Active)
    }

    @Test fun `남은 스냅샷이 없으면 복구하지 않는다`() = runTest {
        assertNull(session.recoverIfNeeded())
    }

    @Test fun `적용 중 취소되면 Applying에 머물지 않고 스냅샷은 유지`() = runTest {
        light.hang = true
        val job = launch { session.apply(plan("a", blueLight)) }
        runCurrent()
        assertTrue(session.state.value is SessionState.Applying)
        job.cancel()
        runCurrent()
        assertEquals("a", (session.state.value as SessionState.Active).sceneId)
        assertEquals(original, store.load()?.light)
    }

    @Test fun `같은 Scene 조절 중 취소되면 이전 결과를 유지`() = runTest {
        session.apply(plan("a", blueLight))
        val before = session.state.value
        light.hang = true
        val job = launch { session.apply(plan("a", purpleLight)) }
        runCurrent()
        job.cancel()
        runCurrent()
        assertEquals(before, session.state.value)
    }

    @Test fun `조명이 안내 모드로 대기 중이어도 공조 조절은 진행된다`() = runTest {
        light.hang = true
        val lightJob = launch { session.apply(plan("a", blueLight)) }
        runCurrent()
        val report = session.apply(plan("a", null, ClimateState(seatHeatLevel = 2)))
        assertEquals(ItemResult.Applied, report.climate)
        assertEquals(2, climate.state.value.seatHeatLevel)
        lightJob.cancel()
    }

    @Test fun `스냅샷에 없던 공조 항목은 원래 값을 채우고 기존 값은 유지`() = runTest {
        // 이전 버전이 저장한 스냅샷: 통풍 항목 없음
        store.save(com.example.moodrive.domain.model.VehicleSnapshot(climate = ClimateState(temperatureC = 18f, seatHeatLevel = 1)))
        climate.state.value = climate.state.value.copy(seatVentLevel = 0)
        val report = session.apply(plan("a", null, ClimateState(seatVentLevel = 2)))
        assertEquals(ItemResult.Applied, report.climate)
        assertEquals(2, climate.state.value.seatVentLevel)
        val saved = store.load()!!.climate!!
        assertEquals(18f, saved.temperatureC)
        assertEquals(1, saved.seatHeatLevel)
        assertEquals(0, saved.seatVentLevel)
    }
}
