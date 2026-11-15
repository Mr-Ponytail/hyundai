package com.example.moodrive.domain.model

import com.example.moodrive.color.WeightedColor

/**
 * 하나의 분위기를 구현하는 차량 설정 묶음.
 * 차종과 무관한 "원본" 정보만 담는다. 차량별 매핑 결과는 [com.example.moodrive.domain.session.ScenePlan]으로 그때그때 계산한다.
 */
data class Scene(
    val id: String,
    val name: String,
    val authorId: String,
    /** 공유·공개 범위 기능용 예약 필드. 아직 앱에서 읽지 않는다 */
    val visibility: Visibility = Visibility.PRIVATE,
    val version: Int = 1,
    /** Remix로 만든 Scene이면 원작 정보 (원작자 표시 유지). Remix 기능용 예약 필드, 아직 앱에서 읽지 않는다 */
    val origin: RemixOrigin? = null,
    val visual: Visual,
    /** null이면 공조는 기존 설정 유지 (기본값) */
    val comfort: Comfort? = null,
    /** 사운드(플레이리스트) 기능용 예약 필드. 아직 앱에서 읽지 않는다 */
    val sound: Sound? = null,
    val mood: Mood = Mood(),
)

/** 공유·공개 범위 기능용 예약 타입. 아직 앱에서 읽지 않는다 */
enum class Visibility { PRIVATE, PUBLIC }

/** Remix 기능용 예약 타입. 아직 앱에서 읽지 않는다 */
data class RemixOrigin(val sceneId: String, val authorId: String)

data class Visual(
    val imageUrl: String? = null,
    /** CIELAB k-means로 뽑은 대표 색, 비중 내림차순 */
    val palette: List<WeightedColor>,
    /** 0~100 (%). 차량 밝기 범위로 환산해 적용 */
    val brightnessPercent: Int,
    /** 사용자가 직접 고른 앰비언트 색. null이면 팔레트에서 고른다 */
    val ambient: AmbientColors? = null,
    /** false = 이 무드에서는 앰비언트 조명을 끈다 */
    val lightOn: Boolean = true,
    /** 실행 화면 전체 배경용 가로 이미지. 없으면 imageUrl을 쓴다 */
    val backdropUrl: String? = null,
)

/**
 * 무드 수정에서 고른 앰비언트 색 (0xAARRGGBB).
 * 상단/하단을 따로 켤 수 있는 차량을 위해 둘 다 저장한다. 지금 차량(에뮬레이터)은 앰비언트 영역이 하나라 [main]만 적용한다.
 */
data class AmbientColors(val main: Int, val sub: Int? = null)

/** null 필드는 "기존 설정 유지" */
data class Comfort(
    val temperatureC: Float? = null,
    val fanSpeed: Int? = null,
    val fanDirection: FanDirection? = null,
    /** 운전석 열선 단계 (0 = 끔) */
    val seatHeatLevel: Int? = null,
    /** 운전석 통풍 단계 (0 = 끔). 열선과 동시에 켜지 않는다 */
    val seatVentLevel: Int? = null,
    /** 핸들 열선 단계 (0 = 끔) */
    val steeringWheelHeatLevel: Int? = null,
)

enum class FanDirection { FACE, FLOOR, FACE_AND_FLOOR, DEFROST, DEFROST_AND_FLOOR, FACE_AND_DEFROST, ALL }

/** 사운드(플레이리스트) 기능용 예약 타입. 아직 앱에서 읽지 않는다 */
data class Sound(
    val playlistUrl: String,
    val title: String? = null,
)

/** LLM 무드 분석 결과 */
data class Mood(
    val keywords: List<String> = emptyList(),
    /** 미리보기 화면 기능용 예약 필드. 아직 앱에서 읽지 않는다 */
    val brightnessCharacter: String? = null,
)
