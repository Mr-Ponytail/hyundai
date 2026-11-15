package com.example.moodrive

import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver

/**
 * 지금 보여줄 최상위 화면. 화면에 필요한 값(Scene id, 요청 문장, 고른 무드 순위)을 함께 들고 있어서
 * 유효하지 않은 조합이 생기지 않는다.
 */
internal sealed interface Route {

    /** 왼쪽 메뉴 탭. 만들기를 나가면 시작했던 탭으로 돌아간다 */
    sealed interface Tab : Route
    data object Home : Tab
    data object Explore : Tab

    data class Control(val sceneId: String) : Route

    /** 탐색 상단 무드의 상세 화면 (적용 전 미리보기 → 적용하면 같은 화면이 실행 화면이 된다). [name] = 무드 이름 */
    data class ExploreMood(val name: String) : Route

    /** 새 무드 만들기 단계. [origin] = 만들기를 시작한 탭 */
    sealed interface Create : Route { val origin: Tab }

    /** 1단계: 설명하기 */
    data class Describe(override val origin: Tab) : Create

    /** 1단계에서 '말로 설명하기'를 눌러 듣는 중 */
    data class Voice(override val origin: Tab) : Create

    /**
     * 2단계: 무드 고르기.
     * [chosenRank]가 있으면 3·4단계에서 돌아온 것 → 후보를 다시 만들지 않고 그 무드를 골라 둔다.
     */
    data class SelectMood(override val origin: Tab, val request: String, val chosenRank: Int? = null) : Create

    /** 3단계: 설정 만들기 */
    data class Apply(override val origin: Tab, val request: String, val rank: Int) : Create

    /** 4단계: 완성 */
    data class Done(override val origin: Tab, val request: String, val rank: Int) : Create
}

/** 화면 회전·프로세스 복원 때 [Route]를 Bundle에 저장할 수 있는 값으로 바꾼다 */
internal val RouteSaver: Saver<Route, Any> = listSaver<Route, Any?>(
    save = { route ->
        when (route) {
            Route.Home -> listOf("home")
            Route.Explore -> listOf("explore")
            is Route.Control -> listOf("control", route.sceneId)
            is Route.ExploreMood -> listOf("explore_mood", route.name)
            is Route.Describe -> listOf("describe", route.origin.key)
            is Route.Voice -> listOf("voice", route.origin.key)
            is Route.SelectMood -> listOf("select", route.origin.key, route.request, route.chosenRank)
            is Route.Apply -> listOf("apply", route.origin.key, route.request, route.rank)
            is Route.Done -> listOf("done", route.origin.key, route.request, route.rank)
        }
    },
    restore = { v ->
        fun origin() = tabOf(v[1] as String)
        when (v[0]) {
            "home" -> Route.Home
            "explore" -> Route.Explore
            "control" -> Route.Control(v[1] as String)
            "explore_mood" -> Route.ExploreMood(v[1] as String)
            "describe" -> Route.Describe(origin())
            "voice" -> Route.Voice(origin())
            "select" -> Route.SelectMood(origin(), v[2] as String, v[3] as Int?)
            "apply" -> Route.Apply(origin(), v[2] as String, v[3] as Int)
            "done" -> Route.Done(origin(), v[2] as String, v[3] as Int)
            else -> Route.Home
        }
    },
)

private val Route.Tab.key get() = if (this == Route.Explore) "explore" else "home"

private fun tabOf(key: String): Route.Tab = if (key == "explore") Route.Explore else Route.Home
