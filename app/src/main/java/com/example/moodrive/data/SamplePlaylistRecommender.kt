package com.example.moodrive.data

import com.example.moodrive.domain.model.Playlist
import com.example.moodrive.domain.model.Scene
import com.example.moodrive.domain.port.PlaylistRecommender
import java.net.URLEncoder

/**
 * Spotify 연동 전 임시 추천. Scene별로 정해 둔 검색어·제목을 돌려주고, 링크는 Spotify 웹 검색으로 연결한다.
 * 실행 화면 카드는 앞의 2개, "더 보러가기" 목록은 최대 9개를 보여준다.
 * 썸네일은 assets/playlists/<sceneId>_<n>.jpg (없으면 화면에서 팔레트로 대체)
 */
class SamplePlaylistRecommender : PlaylistRecommender {

    override suspend fun recommend(scene: Scene, limit: Int): List<Playlist> {
        val titles = SAMPLES[scene.id]?.titles ?: listOf("${scene.name} 무드 플레이리스트", "${scene.mood.keywords.joinToString(" ")} 드라이브")
        return titles.take(limit).mapIndexed { i, title ->
            Playlist(
                title = title,
                subtitle = "플레이리스트",
                imageUrl = "file:///android_asset/playlists/${scene.id}_${i + 1}.jpg",
                url = search(title),
            )
        }
    }

    override fun searchQuery(scene: Scene): String =
        SAMPLES[scene.id]?.query ?: scene.mood.keywords.joinToString(" ").ifBlank { scene.name }

    override fun moreUrl(scene: Scene): String = search(searchQuery(scene))

    private fun search(query: String) = "https://open.spotify.com/search/" + URLEncoder.encode(query, "UTF-8").replace("+", "%20")

    private class Sample(val query: String, val titles: List<String>)

    private companion object {
        val SAMPLES = mapOf(
            "lazy_sunday" to Sample(
                "나른한 일요일",
                listOf(
                    "늦잠 자고 일어난 일요일 오후 ☕", "햇살 드는 창가에서 듣는 어쿠스틱", "주말 아침 침대에서 듣는 로파이",
                    "느긋한 브런치 카페 재즈", "아무것도 안 하고 싶은 날의 플리", "일요일 오후 낮잠 BGM",
                    "포근한 담요 같은 인디 팝", "커피 한 잔과 어쿠스틱 기타", "천천히 흘러가는 주말 오후",
                ),
            ),
            "cozy_fireplace" to Sample(
                "가을 분위기",
                listOf(
                    "분위기 깡패 가을 플레이리스트🍂", "🍂 가을 냄새가 물씬 풍기는 가을", "가을 재즈 카페 2026 🍂☕ 업무·공부할 때 듣는 재즈",
                    "가을의 분위기 | R&B 소울 팝송 모음", "가을이 시작되는 순간에 듣는 재즈", "상큼한 봄 여름 일요일 아침 햇살 식물 카페",
                    "가을 분위기 플레이리스트", "가을 타는 사람들의 플리", "가을 분위기 노래",
                ),
            ),
            "blue_hour" to Sample(
                "해 질 녘 드라이브",
                listOf(
                    "해 질 녘 파란 시간의 드림팝", "퇴근길 차분해지는 로파이", "노을이 지고 난 뒤의 시티팝",
                    "블루 아워에 듣는 앰비언트", "하루를 정리하는 저녁 인디", "창밖이 파랗게 물들 때",
                    "잔잔한 저녁 드라이브 R&B", "어스름한 하늘 아래 피아노", "해 지는 강변 드라이브",
                ),
            ),
            "neon_night" to Sample(
                "시티팝 드라이브",
                listOf(
                    "네온 아래 시티팝 드라이브 🌃", "심야 고속도로 신스웨이브", "도시의 밤을 달리는 일렉트로닉",
                    "80년대 감성 시티팝 모음", "불 꺼지지 않는 도시의 R&B", "한밤중 드라이브 레트로 팝",
                    "네온사인 같은 신스팝", "새벽 도심 드라이브 하우스", "밤공기 가르는 퓨처 펑크",
                ),
            ),
            "forest_walk" to Sample(
                "숲속 산책",
                listOf(
                    "숲속 아침 공기 같은 포크", "창문 열고 듣는 상쾌한 인디", "새소리와 어울리는 어쿠스틱",
                    "초록빛 산책길 플레이리스트", "피톤치드 가득한 힐링 음악", "맑은 날 숲길 드라이브",
                    "자연 속에서 듣는 잔잔한 피아노", "아침 햇살 포크 팝", "마음이 맑아지는 인디 포크",
                ),
            ),
            "autumn_sunset" to Sample(
                "노을 드라이브",
                listOf(
                    "노을 지는 해안도로 R&B 🌅", "가을 저녁 감성 발라드", "붉은 하늘 아래 소울",
                    "해 질 녘 드라이브 팝", "노을 보며 듣는 재즈 보컬", "가을 저녁 어쿠스틱",
                    "황금빛 오후의 인디 팝", "석양과 어울리는 시티팝", "하루 끝 노을 플레이리스트",
                ),
            ),
            "ocean_drive" to Sample(
                "바다 드라이브",
                listOf(
                    "바닷바람 맞으며 듣는 서머 팝 🌊", "해안도로 드라이브 필수곡", "파도 소리와 어울리는 트로피컬 하우스",
                    "여름 바다 신나는 팝", "창문 열고 달리는 해변 드라이브", "청량한 여름 인디 록",
                    "바다가 보이는 카페 보사노바", "여름휴가 로드트립 플리", "푸른 바다 칠 아웃",
                ),
            ),
            "rainy_cafe" to Sample(
                "비 오는 날 카페",
                listOf(
                    "비 오는 날 카페 재즈 ☔", "빗소리와 어울리는 피아노", "창가에 앉아 듣는 로파이",
                    "비 내리는 오후의 어쿠스틱", "우산 쓰고 걷는 날의 인디", "촉촉한 감성 R&B",
                    "빗길 드라이브 잔잔한 팝", "비 오는 밤 재즈 보컬", "흐린 날 듣기 좋은 발라드",
                ),
            ),
            "cherry_blossom" to Sample(
                "벚꽃 드라이브",
                listOf(
                    "벚꽃길 드라이브 봄 노래 🌸", "설레는 봄날의 어쿠스틱", "봄바람 살랑이는 인디 팝",
                    "꽃구경 갈 때 듣는 플리", "봄 햇살 같은 사랑 노래", "벚꽃 엔딩 감성 모음",
                    "산뜻한 봄 드라이브 팝", "봄날 피크닉 어쿠스틱", "분홍빛 봄밤 R&B",
                ),
            ),
            "midnight_jazz" to Sample(
                "자정 재즈",
                listOf(
                    "자정의 스무스 재즈 🎷", "도시의 밤과 어울리는 소울", "늦은 밤 위스키 바 재즈",
                    "새벽 감성 재즈 피아노", "한밤의 색소폰 발라드", "조용한 밤 드라이브 재즈",
                    "재즈 클럽 라이브 모음", "잠 못 드는 밤의 R&B", "어두운 밤 로파이 재즈",
                ),
            ),
            "desert_dune" to Sample(
                "로드트립 록",
                listOf(
                    "사막 로드트립 록 🏜️", "끝없는 도로 위 인디 록", "뜨거운 태양 아래 블루스 록",
                    "장거리 운전 필수 록 모음", "모래바람 속 사이키델릭", "자유로운 여행 컨트리 록",
                    "고속도로 위 클래식 록", "황야를 달리는 기타 리프", "해 질 녘 사막 포크 록",
                ),
            ),
            "aurora" to Sample(
                "몽환적인 밤",
                listOf(
                    "오로라 아래 앰비언트 🌌", "몽환적인 밤하늘 일렉트로닉", "별이 쏟아지는 밤의 드림팝",
                    "우주를 떠다니는 듯한 앰비언트", "고요한 겨울밤 피아노", "북극의 밤 칠 아웃",
                    "신비로운 밤 일렉트로닉 모음", "별 보러 가는 길 인디", "깊은 밤 명상 음악",
                ),
            ),
        )
    }
}
