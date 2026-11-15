package com.example.moodrive.color

/** 매핑 결과. [deltaE]가 클수록 원본과 차이가 크다 (대략 10 이상이면 "다른 색"으로 보인다). */
data class ColorMatch(val source: Int, val matched: Int, val deltaE: Double)

/** 원하는 색을 차량이 지원하는 색 중 ΔE2000이 가장 작은 색으로 바꾼다. */
object ColorMatcher {

    fun nearest(target: Int, supported: List<Int>): ColorMatch {
        require(supported.isNotEmpty()) { "supported colors must not be empty" }
        val targetLab = Lab.fromArgb(target)
        return supported
            .map { ColorMatch(target, it, DeltaE.ciede2000(targetLab, Lab.fromArgb(it))) }
            .minBy { it.deltaE }
    }

    /**
     * 팔레트를 차량 조명 한 색으로 바꾼다.
     * 현재 정책: 비중 상위 [topN]개 색 각각을 가장 가까운 지원 색에 대응시키고, 그중 ΔE가 가장 작은 조합을 고른다.
     * (차량 색이 4개뿐이라, 1순위가 어두운 배경색이어도 차량으로 잘 재현되는 포인트 색을 택하기 위함. 시연 후 튜닝 대상)
     */
    fun matchPalette(palette: List<WeightedColor>, supported: List<Int>, topN: Int = 3): ColorMatch? =
        palette.take(topN).map { nearest(it.argb, supported) }.minByOrNull { it.deltaE }
}
