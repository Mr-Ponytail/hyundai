package com.example.moodrive.ui.create

import com.example.moodrive.color.WeightedColor
import com.example.moodrive.domain.model.AmbientColors
import com.example.moodrive.domain.model.Mood
import com.example.moodrive.domain.model.Scene
import com.example.moodrive.domain.model.Visual
import com.example.moodrive.ui.mood.MoodDraft

/**
 * 만들기 완성 화면에서 저장한 무드 → 내 Mood에 넣을 Scene. (서버 연동 전 샘플 후보 기반)
 * 후보의 이미지·키워드·색을 바탕으로 만들고, 완성 화면에서 고친 값([draft])을 덮어쓴다.
 * 저장할 때마다 새 id라 같은 후보를 여러 번 저장하면 각각 추가된다.
 * @param imageUri 그림 리소스 id → 이미지 주소 ("android.resource://…")
 */
fun MoodCandidateUi.toCreatedScene(draft: MoodDraft, imageUri: (Int) -> String, now: Long = System.currentTimeMillis()): Scene {
    val base = Scene(
        id = "created_$now",
        name = name,
        authorId = "me",
        // applyTo가 version을 1 올리므로 0에서 시작해 저장 결과가 1이 되게 한다
        version = 0,
        visual = Visual(
            imageUrl = imageUri(imageRes),
            palette = listOf(WeightedColor(settings.lightArgb, 0.6f), WeightedColor(settings.subArgb, 0.4f)),
            brightnessPercent = settings.brightness,
            ambient = AmbientColors(settings.lightArgb, settings.subArgb),
        ),
        mood = Mood(keywords = keywords.split("·").map { it.trim() }.filter { it.isNotEmpty() }),
    )
    return draft.applyTo(base)
}
