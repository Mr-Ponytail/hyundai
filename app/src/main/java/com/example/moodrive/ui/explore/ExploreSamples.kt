package com.example.moodrive.ui.explore

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.example.moodrive.R
import com.example.moodrive.domain.model.Comfort
import com.example.moodrive.domain.model.FanDirection

/** 무드를 올린 사람. 사진이 없으면 그라데이션 원, 그것도 없으면 첫 글자 */
@Immutable
data class ExploreAuthorUi(
    val handle: String,
    @DrawableRes val photoRes: Int? = null,
    val gradient: List<Color>? = null,
)

/** 히어로 카드 칩에 보여줄 설정 요약 */
data class ExploreSettingsUi(
    val lightArgb: Int,
    val brightness: Int,
    val seat: String,
    val wind: String,
    val wheelOn: Boolean,
)

@Immutable
data class ExploreMoodUi(
    val name: String,
    val keywords: String,
    @DrawableRes val imageRes: Int,
    val author: ExploreAuthorUi,
    /** 저장 수 ("2.3k") */
    val saves: String,
    val settings: ExploreSettingsUi? = null,
    /** 상세 화면·적용에 쓰는 차량 설정. 있는 무드만 상세 화면을 연다 */
    val preset: ExplorePreset? = null,
)

/** 탐색 무드를 Scene으로 바꿀 때 쓰는 값 (커뮤니티 API 전까지 샘플) */
@Immutable
data class ExplorePreset(
    /** 대표 색, 비중 내림차순 */
    val palette: List<Int>,
    val brightness: Int,
    /** 앰비언트 색 */
    val ambientArgb: Int,
    val comfort: Comfort,
)

@Immutable
data class CuratorUi(val author: ExploreAuthorUi, val role: String, val quote: String)

@Immutable
data class ColorThemeUi(
    val name: String,
    val mood: String,
    val mainArgb: Int,
    val subArgb: Int,
    val curator: CuratorUi,
    /** 이번 주 팔레트 5색 */
    val palette: List<Int>,
    val moods: List<ExploreMoodUi>,
)

@Immutable
data class ExploreCollectionUi(
    val title: String,
    val colorName: String,
    val count: Int,
    val dotArgb: Int,
    val tintArgb: Int,
    /** 큰 사진 1장 + 작은 사진 2장 */
    val photos: List<Int>,
)

data class ExploreFeed(
    val hero: ExploreMoodUi,
    val picks: List<ExploreMoodUi>,
    val themes: List<ColorThemeUi>,
    val collections: List<ExploreCollectionUi>,
)

private fun argb(v: Long) = v.toInt()

private val LUMI_GRADIENT = listOf(Color(0xFFFF7A3D), Color(0xFFFFE2B8))
private val LUMI = ExploreAuthorUi("lumi", gradient = LUMI_GRADIENT)
private val DUUUSIK = ExploreAuthorUi("duuusik")
private val HYENI = ExploreAuthorUi("hyeni")
private val MORI = ExploreAuthorUi("mori")

// 추천과 테마 목록에 함께 나오는 무드
private val RAINY_BUS_RIDE = ExploreMoodUi("Rainy Bus Ride", "Dreamy · Melancholic", R.drawable.explore_rainy_bus, DUUUSIK, "2.3k")
private val GOLDEN_HOUR = ExploreMoodUi("Golden Hour", "Serene · Peaceful", R.drawable.explore_golden_hour, LUMI, "2.3k")

/**
 * 커뮤니티 API 전까지 쓰는 시안 데이터.
 * 코퍼 & 크림 테마까지는 시안 그대로이고, 나머지 테마의 큐레이터·문구·무드는 화면 확인용으로 만든 값이다.
 */
val SAMPLE_EXPLORE_FEED = ExploreFeed(
    hero = ExploreMoodUi(
        name = "Midnight Rain Cafe",
        keywords = "Cozy · Nostalgic",
        imageRes = R.drawable.explore_hero_rain_cafe,
        author = ExploreAuthorUi("lumi", photoRes = R.drawable.avatar_lumi),
        saves = "2.3k",
        settings = ExploreSettingsUi(argb(0xFF0096C8), 60, "1단 · 열선", "2단 · 얼굴", wheelOn = true),
        preset = ExplorePreset(
            palette = listOf(0xFF1F2A33, 0xFF6B4A33, 0xFF0096C8).map(::argb), brightness = 60, ambientArgb = argb(0xFF0096C8),
            comfort = Comfort(fanSpeed = 2, fanDirection = FanDirection.FACE, seatHeatLevel = 1, seatVentLevel = 0, steeringWheelHeatLevel = 1),
        ),
    ),
    picks = listOf(
        ExploreMoodUi(
            "Puddle Jump", "Playful · Refreshing", R.drawable.explore_puddle_jump, HYENI, "2.3k",
            preset = ExplorePreset(
                palette = listOf(0xFF3E5A6E, 0xFFF2C14E, 0xFF9FC3D6).map(::argb), brightness = 70, ambientArgb = argb(0xFFF2C14E),
                comfort = Comfort(fanSpeed = 2, fanDirection = FanDirection.FACE, seatHeatLevel = 0, seatVentLevel = 1, steeringWheelHeatLevel = 0),
            ),
        ),
        RAINY_BUS_RIDE.copy(
            preset = ExplorePreset(
                palette = listOf(0xFF2A3440, 0xFF5E7A94, 0xFFB8C4CF).map(::argb), brightness = 40, ambientArgb = argb(0xFF5E7A94),
                comfort = Comfort(fanSpeed = 1, fanDirection = FanDirection.FLOOR, seatHeatLevel = 1, seatVentLevel = 0, steeringWheelHeatLevel = 0),
            ),
        ),
    ),
    themes = listOf(
        ColorThemeUi(
            name = "코퍼 & 크림", mood = "따뜻한 · 아늑한",
            mainArgb = argb(0xFFB5652E), subArgb = argb(0xFFF4D3A6),
            curator = CuratorUi(LUMI, "조명 컬러리스트 · 팔로워 4.2k", "겨울 저녁엔 앰버보다\n한 톤 낮은 코퍼가 덜 피곤해요"),
            palette = listOf(0xFFB5652E, 0xFFE39A5B, 0xFFF4D3A6, 0xFF5B4636, 0xFF2E2A33).map(::argb),
            moods = listOf(
                ExploreMoodUi("Soft Ivory", "Romantic · Cozy", R.drawable.explore_soft_ivory, LUMI, "2.3k"),
                GOLDEN_HOUR,
                ExploreMoodUi("Copper Night", "Mystical · Solitary", R.drawable.explore_copper_night, LUMI, "2.3k"),
            ),
        ),
        ColorThemeUi(
            name = "딥 블루 & 실버", mood = "고요한 · 깊은",
            mainArgb = argb(0xFF3D5BFF), subArgb = argb(0xFFC9D6EA),
            curator = CuratorUi(DUUUSIK, "야간 드라이브 큐레이터 · 팔로워 3.1k", "비 오는 밤엔 채도를 낮춘\n블루가 눈이 편해요"),
            palette = listOf(0xFF1B2A4A, 0xFF3D5BFF, 0xFF7FA3F0, 0xFFC9D6EA, 0xFF2A2E3A).map(::argb),
            moods = listOf(
                RAINY_BUS_RIDE,
                ExploreMoodUi("Night Sail", "Calm · Infinite", R.drawable.explore_col_blue_1, DUUUSIK, "1.8k"),
                ExploreMoodUi("Midnight Rain Cafe", "Cozy · Nostalgic", R.drawable.explore_hero_rain_cafe, LUMI, "2.3k"),
            ),
        ),
        ColorThemeUi(
            name = "네온 퍼플 & 핑크", mood = "짜릿한 · 도시의",
            mainArgb = argb(0xFFB23CFF), subArgb = argb(0xFFFF4FA3),
            curator = CuratorUi(HYENI, "시티 라이트 컬러리스트 · 팔로워 5.6k", "퍼플은 상단, 핑크는 하단에\n두면 실내가 넓어 보여요"),
            palette = listOf(0xFF3A0F2A, 0xFFB23CFF, 0xFFFF4FA3, 0xFFFF9ACB, 0xFF1C1430).map(::argb),
            moods = listOf(
                ExploreMoodUi("Night City Rush", "Bold · Urban", R.drawable.mood_night_city_rush, HYENI, "3.4k"),
                ExploreMoodUi("Pink Skyline", "Vivid · Energetic", R.drawable.mood_midnight_rain_cafe, HYENI, "2.9k"),
            ),
        ),
        ColorThemeUi(
            name = "포레스트 & 라임", mood = "상쾌한 · 맑은",
            mainArgb = argb(0xFF2F7D4F), subArgb = argb(0xFFC8F07A),
            curator = CuratorUi(MORI, "자연광 컬러리스트 · 팔로워 2.4k", "아침 출근길엔 라임을 살짝\n섞으면 기분이 맑아져요"),
            palette = listOf(0xFF2F4A2A, 0xFF2F7D4F, 0xFF8FB07A, 0xFFC8F07A, 0xFFE6EED8).map(::argb),
            moods = listOf(
                ExploreMoodUi("Forest Pause", "Fresh · Natural", R.drawable.explore_col_green_1, MORI, "1.2k"),
                ExploreMoodUi("Green Road Trip", "Bright · Open", R.drawable.mood_green_road_trip, MORI, "980"),
                ExploreMoodUi("Lakeside Weekend", "Fresh · Relaxed", R.drawable.mood_lakeside_weekend, MORI, "1.5k"),
            ),
        ),
        ColorThemeUi(
            name = "오렌지 & 로즈", mood = "설레는 · 낭만적인",
            mainArgb = argb(0xFFFF7A3D), subArgb = argb(0xFFFF6F91),
            curator = CuratorUi(LUMI, "조명 컬러리스트 · 팔로워 4.2k", "노을 드라이브엔 로즈를\n하단에만 은은하게 깔아요"),
            palette = listOf(0xFFFF7A3D, 0xFFFFA36B, 0xFFFF6F91, 0xFFFFD1B8, 0xFF3A2230).map(::argb),
            moods = listOf(
                ExploreMoodUi("Golden Breeze", "Warm · Free", R.drawable.mood_golden_breeze, LUMI, "2.1k"),
                GOLDEN_HOUR,
            ),
        ),
    ),
    collections = listOf(
        ExploreCollectionUi(
            "퇴근 후, 온기를 모으다", "Warm Amber", 9, argb(0xFFF2A65A), argb(0xFF8A5220),
            listOf(R.drawable.explore_col_amber_1, R.drawable.explore_col_amber_2, R.drawable.explore_col_amber_3),
        ),
        ExploreCollectionUi(
            "고요한 밤의 기록", "Midnight Blue", 7, argb(0xFF7FA3F0), argb(0xFF213B80),
            listOf(R.drawable.explore_col_blue_1, R.drawable.explore_col_blue_2, R.drawable.explore_col_blue_3),
        ),
        ExploreCollectionUi(
            "자연을 가까이 두는 방법", "Forest Green", 6, argb(0xFF9CCB8F), argb(0xFF2B5536),
            listOf(R.drawable.explore_col_green_1, R.drawable.explore_col_green_2, R.drawable.explore_col_green_3),
        ),
        // 여기부터는 시안에 없는 컬렉션 (가로 스크롤 확인용, 프로젝트에 있는 이미지 재사용)
        ExploreCollectionUi(
            "네온이 번지는 도시의 밤", "Neon Pink", 8, argb(0xFFFF6FB5), argb(0xFF6A1F4F),
            listOf(R.drawable.mood_midnight_rain_cafe, R.drawable.mood_night_city_rush, R.drawable.explore_rainy_bus),
        ),
        ExploreCollectionUi(
            "노을을 따라 달리는 길", "Sunset Orange", 5, argb(0xFFFFA36B), argb(0xFF8A3B1E),
            listOf(R.drawable.explore_golden_hour, R.drawable.mood_golden_breeze, R.drawable.explore_copper_night),
        ),
        ExploreCollectionUi(
            "비 오는 날의 창가", "Rain Gray", 6, argb(0xFFA9B4C2), argb(0xFF3A4350),
            listOf(R.drawable.explore_hero_rain_cafe, R.drawable.explore_puddle_jump, R.drawable.thumb_recent_rainy_window),
        ),
        ExploreCollectionUi(
            "호숫가에서 보내는 주말", "Lake Blue", 4, argb(0xFF4FC3F7), argb(0xFF1E5A73),
            listOf(R.drawable.mood_lakeside_weekend, R.drawable.thumb_recent_frost_morning, R.drawable.mood_green_road_trip),
        ),
    ),
)
