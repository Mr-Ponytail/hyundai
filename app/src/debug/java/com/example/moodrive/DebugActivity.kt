package com.example.moodrive

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.example.moodrive.color.WeightedColor
import com.example.moodrive.domain.model.Comfort
import com.example.moodrive.domain.model.Scene
import com.example.moodrive.domain.model.Visual
import com.example.moodrive.domain.session.ScenePlanner
import com.example.moodrive.ui.common.LOG_TAG
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * 디버그 화면: 차량 연동 + 적용·복원 흐름을 에뮬레이터에서 확인하는 용도.
 * 홈의 "설정" 버튼으로 진입 (설정 화면이 생기면 그 안으로 옮긴다). 디버그 빌드에만 포함된다.
 */
class DebugActivity : Activity() {

    private val scope = MainScope()
    private val container get() = (application as MoodriveApp).container
    private lateinit var status: TextView

    /** 시연용 고정 Scene: 파란 계열 사진에서 뽑았다고 가정한 팔레트 */
    private val demoScene = Scene(
        id = "demo-night-drive",
        name = "Night Drive",
        authorId = "local",
        visual = Visual(
            palette = listOf(WeightedColor(0xFF101828.toInt(), 0.55f), WeightedColor(0xFF2F4BE0.toInt(), 0.30f)),
            brightnessPercent = 60,
        ),
        comfort = Comfort(temperatureC = 21f),
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        status = TextView(this).apply { textSize = 20f }
        setContentView(
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(48, 48, 48, 48)
                addView(button("Scene 적용 (조명만)") { applyDemo(applyComfort = false) })
                addView(button("Scene 적용 (조명 + 공조 승인)") { applyDemo(applyComfort = true) })
                addView(button("Scene 종료 · 복원") { endScene() })
                addView(ScrollView(context).apply { addView(status) })
            },
        )

        val vehicle = container.vehicle
        scope.launch {
            // 연결 전 호출은 SDK가 대기열에 쌓아 두므로 기다릴 필요 없다. 이전 세션 복구는 MoodriveApp이 시작 시 한다
            log("구현체: ${BuildConfig.VEHICLE_IMPL}")
            log("조명 capability: ${runCatching { vehicle.light.capabilities() }.getOrElse { "실패 ${it.message}" }}")
            log("공조 capability: ${runCatching { vehicle.climate.capabilities() }.getOrElse { "실패 ${it.message}" }}")

            vehicle.light.observe().onEach { log("조명 변경: $it") }.launchIn(scope)
            vehicle.climate.observe().onEach { log("공조 변경: $it") }.launchIn(scope)
            vehicle.driveState.driveMode.onEach { log("주행 상태: $it") }.launchIn(scope)
            vehicle.light.guide.onEach { if (it != null) log("[안내 모드] 가상 콘솔에서 조명을 이 값으로 바꿔 주세요: $it") }.launchIn(scope)
            container.sceneSession.state.onEach { log("세션: $it") }.launchIn(scope)
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun applyDemo(applyComfort: Boolean) = scope.launch {
        val vehicle = container.vehicle
        val plan = ScenePlanner.plan(
            demoScene,
            runCatching { vehicle.light.capabilities() }.getOrNull(),
            runCatching { vehicle.climate.capabilities() }.getOrNull(),
            applyComfort,
        )
        log("계획: $plan")
        log("적용 결과: ${container.sceneSession.apply(plan)}")
    }

    private fun endScene() = scope.launch {
        log("복원 결과: ${container.sceneSession.end()}")
    }

    private fun button(label: String, onClick: () -> Unit) =
        Button(this).apply { text = label; setOnClickListener { onClick() } }

    private fun log(msg: String) {
        Log.i(LOG_TAG, msg)
        status.append(msg + "\n")
    }
}
