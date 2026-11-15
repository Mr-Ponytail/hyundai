package com.example.moodrive.domain.model

/** 내 라이브러리의 Scene 한 개 + 사용자별 메타데이터 */
data class LibraryEntry(
    val scene: Scene,
    val isFavorite: Boolean = false,
    /** 마지막으로 차량에 적용한 시각 (epoch ms). null = 실행한 적 없음 */
    val lastAppliedAt: Long? = null,
)

/** 홈 화면 표시용 사용자 정보 */
data class UserProfile(
    val displayName: String,
    val handle: String,
    val avatarUrl: String? = null,
)
