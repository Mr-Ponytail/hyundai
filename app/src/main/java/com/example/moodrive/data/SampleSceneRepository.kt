package com.example.moodrive.data

import com.example.moodrive.color.WeightedColor
import com.example.moodrive.domain.model.AmbientColors
import com.example.moodrive.domain.model.Comfort
import com.example.moodrive.domain.model.FanDirection
import com.example.moodrive.domain.model.LibraryEntry
import com.example.moodrive.domain.model.Mood
import com.example.moodrive.domain.model.Scene
import com.example.moodrive.domain.model.UserProfile
import com.example.moodrive.domain.model.Visual
import com.example.moodrive.domain.port.ProfileRepository
import com.example.moodrive.domain.port.SceneRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import java.util.concurrent.TimeUnit

/**
 * 서버 연동 전까지 쓰는 메모리 라이브러리 (앱 재시작 시 초기화).
 *
 * 이미지는 assets/scenes/<slug>.jpg 에서 읽는다. 파일이 없으면 화면에서 팔레트 그라데이션으로 대체한다.
 */
class SampleSceneRepository(now: Long = System.currentTimeMillis()) : SceneRepository {

    private val state = MutableStateFlow(samples(now))
    override val entries: Flow<List<LibraryEntry>> = state.asStateFlow()

    override suspend fun get(sceneId: String) = state.value.firstOrNull { it.scene.id == sceneId }

    override suspend fun markApplied(sceneId: String, at: Long) =
        state.update { list -> list.map { if (it.scene.id == sceneId) it.copy(lastAppliedAt = at) else it } }

    override suspend fun save(scene: Scene) = state.update { list ->
        if (list.any { it.scene.id == scene.id }) list.map { if (it.scene.id == scene.id) it.copy(scene = scene) else it }
        else listOf(LibraryEntry(scene)) + list
    }

    override suspend fun setFavorite(sceneId: String, favorite: Boolean) =
        state.update { list -> list.map { if (it.scene.id == sceneId) it.copy(isFavorite = favorite) else it } }

    private companion object {
        val DAY = TimeUnit.DAYS.toMillis(1)

        fun samples(now: Long) = listOf(
            entry("cozy_fireplace", "Cozy Fireplace", listOf("Vintage", "Warm"), 50, 0xFF8E2A22, 0xFFD9A441, 0xFF2B1A14, appliedAt = now - DAY, comfort = Comfort(fanSpeed = 1, fanDirection = FanDirection.FLOOR, seatHeatLevel = 2, steeringWheelHeatLevel = 1), wideBackdrop = true, ambient = AmbientColors(0xFFFF9F43.toInt(), 0xFFFFE2B8.toInt())),
            entry("neon_night", "Neon Night", listOf("Vivid", "Urban"), 80, 0xFF3A0F2A, 0xFFE0408F, 0xFF7A1F5C, appliedAt = now - 4 * DAY, comfort = Comfort(fanSpeed = 3, fanDirection = FanDirection.FACE, seatHeatLevel = 0, steeringWheelHeatLevel = 0)),
            entry("forest_walk", "Forest Walk", listOf("Fresh", "Airy"), 65, 0xFF2F4A2A, 0xFF8FB07A, 0xFFD8E3C8, appliedAt = now - 7 * DAY, comfort = Comfort(fanSpeed = 2, fanDirection = FanDirection.FACE_AND_FLOOR, seatHeatLevel = 0)),
            entry("autumn_sunset", "Autumn Sunset", listOf("Warm", "Romantic"), 60, 0xFFE07A3A, 0xFF8E3B4A, 0xFF2A1A2E, comfort = Comfort(fanSpeed = 1, fanDirection = FanDirection.FLOOR, seatHeatLevel = 1)),
            entry("ocean_drive", "Ocean Drive", listOf("Cool", "Breezy"), 75, 0xFF1E6F9F, 0xFF7FD1E0, 0xFFF0EDE4, favorite = true, comfort = Comfort(fanSpeed = 4, fanDirection = FanDirection.FACE, seatHeatLevel = 0, steeringWheelHeatLevel = 0)),
            entry("rainy_cafe", "Rainy Cafe", listOf("Moody", "Calm"), 35, 0xFF3A3F46, 0xFF8A6A4E, 0xFFC9B9A0, comfort = Comfort(fanSpeed = 1, fanDirection = FanDirection.FLOOR, seatHeatLevel = 1, steeringWheelHeatLevel = 1)),
            entry("cherry_blossom", "Cherry Blossom", listOf("Soft", "Lovely"), 70, 0xFFF2C4D0, 0xFFE58FA8, 0xFF6E8A5A, comfort = Comfort(fanSpeed = 2, fanDirection = FanDirection.FACE_AND_FLOOR)),
            entry("midnight_jazz", "Midnight Jazz", listOf("Deep", "Smooth"), 30, 0xFF141A3A, 0xFF4B2E83, 0xFFC08A3E, comfort = Comfort(fanSpeed = 1, fanDirection = FanDirection.FLOOR, seatHeatLevel = 2)),
            entry("desert_dune", "Desert Dune", listOf("Warm", "Earthy"), 55, 0xFFD9A066, 0xFF9A5B34, 0xFF4A6A8E, comfort = Comfort(fanSpeed = 3, fanDirection = FanDirection.FACE, seatHeatLevel = 0, steeringWheelHeatLevel = 0)),
            entry("aurora", "Aurora", listOf("Mystic", "Dreamy"), 50, 0xFF0F2A3A, 0xFF35D6A0, 0xFF7A4FD0, comfort = Comfort(fanSpeed = 1, fanDirection = FanDirection.FLOOR, seatHeatLevel = 2, steeringWheelHeatLevel = 1)),
        )

        fun entry(
            slug: String,
            name: String,
            keywords: List<String>,
            brightness: Int,
            vararg colors: Long,
            favorite: Boolean = false,
            appliedAt: Long? = null,
            comfort: Comfort? = null,
            /** assets/scenes/<slug>_wide.jpg 가 있는 Scene만 true (실행 화면 배경) */
            wideBackdrop: Boolean = false,
            /** 사용자가 고른 앰비언트 색 (없으면 팔레트에서 고른다) */
            ambient: AmbientColors? = null,
        ) = LibraryEntry(
            scene = Scene(
                id = slug,
                name = name,
                authorId = "nightowl",
                visual = Visual(
                    imageUrl = "file:///android_asset/scenes/$slug.jpg",
                    palette = colors.mapIndexed { i, c -> WeightedColor(c.toInt(), listOf(0.5f, 0.3f, 0.2f)[i]) },
                    brightnessPercent = brightness,
                    ambient = ambient,
                    backdropUrl = if (wideBackdrop) "file:///android_asset/scenes/${slug}_wide.jpg" else null,
                ),
                mood = Mood(keywords = keywords),
                comfort = comfort,
            ),
            isFavorite = favorite,
            lastAppliedAt = appliedAt,
        )
    }
}

class SampleProfileRepository : ProfileRepository {
    override val profile: Flow<UserProfile> = flowOf(
        UserProfile(displayName = "연우", handle = "nightowl", avatarUrl = "file:///android_asset/profile/avatar.jpg"),
    )
}
