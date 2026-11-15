package com.example.moodrive.domain.model

/** 무드에 맞춰 추천하는 음악 플레이리스트 (지금은 Spotify 기준) */
data class Playlist(
    val title: String,
    /** "플레이리스트" 등 보조 설명 */
    val subtitle: String,
    val imageUrl: String? = null,
    /** 외부 앱/웹으로 여는 주소 */
    val url: String,
)
