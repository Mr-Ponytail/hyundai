package com.example.moodrive.ui.explore

import com.example.moodrive.color.WeightedColor
import com.example.moodrive.domain.model.AmbientColors
import com.example.moodrive.domain.model.Mood
import com.example.moodrive.domain.model.Scene
import com.example.moodrive.domain.model.Visual

/** 상세 화면을 열 수 있는 탐색 상단 무드(히어로 + 추천). 이름으로 찾는다 (Route에 저장) */
fun exploreMoodOf(name: String, feed: ExploreFeed = SAMPLE_EXPLORE_FEED): ExploreMoodUi? =
    (listOf(feed.hero) + feed.picks).firstOrNull { it.name == name && it.preset != null }

/**
 * 탐색 무드 → Scene. 내 Mood에 저장·적용할 때 그대로 쓴다.
 * @param imageUri 그림 리소스 id → 이미지 주소 ("android.resource://…")
 */
fun ExploreMoodUi.toScene(imageUri: (Int) -> String): Scene? {
    val p = preset ?: return null
    return Scene(
        id = "explore_" + name.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_'),
        name = name,
        authorId = author.handle,
        visual = Visual(
            imageUrl = imageUri(imageRes),
            palette = p.palette.mapIndexed { i, c -> WeightedColor(c, PALETTE_WEIGHTS.getOrElse(i) { PALETTE_WEIGHTS.last() }) },
            brightnessPercent = p.brightness,
            ambient = AmbientColors(p.ambientArgb),
        ),
        comfort = p.comfort,
        mood = Mood(keywords = keywords.split("·").map { it.trim() }.filter { it.isNotEmpty() }),
    )
}

private val PALETTE_WEIGHTS = listOf(0.5f, 0.3f, 0.2f)
