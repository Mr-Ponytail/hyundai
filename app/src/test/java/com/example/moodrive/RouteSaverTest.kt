package com.example.moodrive

import androidx.compose.runtime.saveable.SaverScope
import org.junit.Assert.assertEquals
import org.junit.Test

class RouteSaverTest {

    private val scope = SaverScope { true }

    private fun roundTrip(route: Route): Route? {
        val saved = with(RouteSaver) { scope.save(route) }!!
        return RouteSaver.restore(saved)
    }

    @Test
    fun `모든 화면이 저장 후 그대로 복원된다`() {
        val routes = listOf(
            Route.Home,
            Route.Explore,
            Route.Control("scene-1"),
            Route.Describe(Route.Home),
            Route.Voice(Route.Explore),
            Route.SelectMood(Route.Home, "활기찬 드라이브"),
            Route.SelectMood(Route.Explore, "비 오는 날", chosenRank = 3),
            Route.Apply(Route.Home, "활기찬 드라이브", rank = 2),
            Route.Done(Route.Explore, "비 오는 날", rank = 1),
        )
        routes.forEach { assertEquals(it, roundTrip(it)) }
    }
}
