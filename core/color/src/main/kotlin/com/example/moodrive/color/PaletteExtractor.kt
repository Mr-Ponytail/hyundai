package com.example.moodrive.color

import kotlin.random.Random

/** 팔레트의 한 색과 이미지 내 비중(0~1). */
data class WeightedColor(val argb: Int, val weight: Float)

/**
 * 이미지 픽셀에서 대표 색을 뽑는다. CIELAB 공간에서 k-means(k-means++ 초기화)를 돌린다.
 *
 * 입력은 플랫폼 독립적인 ARGB 배열이다. Android에서는 Bitmap을 작게 줄여(예: 128px) getPixels로 넘긴다.
 * 같은 입력이면 같은 결과가 나오도록 seed를 고정한다.
 */
class PaletteExtractor(
    private val maxIterations: Int = 20,
    private val seed: Int = 42,
) {

    fun extract(pixels: IntArray, k: Int = 5): List<WeightedColor> {
        val opaque = pixels.filter { (it ushr 24) >= 0x80 }
        if (opaque.isEmpty()) return emptyList()

        val points = opaque.map(Lab::fromArgb)
        val clusterCount = minOf(k, points.distinct().size)
        var centers = initCenters(points, clusterCount)
        val assignment = IntArray(points.size)

        assignment.fill(-1)
        for (iteration in 0 until maxIterations) {
            var changed = false
            points.forEachIndexed { i, p ->
                val nearest = centers.indices.minBy { distSq(p, centers[it]) }
                if (assignment[i] != nearest) {
                    assignment[i] = nearest
                    changed = true
                }
            }
            if (!changed) break
            centers = centers.indices.map { c -> mean(points.filterIndexed { i, _ -> assignment[i] == c }) ?: centers[c] }
        }

        // 각 군집의 대표색은 Lab 평균이 아니라 군집 안 실제 픽셀 중 평균에 가장 가까운 색을 쓴다 (sRGB 범위 밖 색 방지)
        return centers.indices.mapNotNull { c ->
            val members = opaque.indices.filter { assignment[it] == c }
            if (members.isEmpty()) return@mapNotNull null
            val representative = members.minBy { distSq(points[it], centers[c]) }
            WeightedColor(opaque[representative] or (0xFF shl 24), members.size.toFloat() / opaque.size)
        }.sortedByDescending { it.weight }
    }

    private fun initCenters(points: List<Lab>, k: Int): List<Lab> {
        val random = Random(seed)
        val centers = mutableListOf(points[random.nextInt(points.size)])
        while (centers.size < k) {
            val d = points.map { p -> centers.minOf { distSq(p, it) } }
            val total = d.sum()
            if (total == 0.0) break
            var r = random.nextDouble() * total
            val idx = d.indices.firstOrNull { r -= d[it]; r <= 0 } ?: d.lastIndex
            centers += points[idx]
        }
        return centers
    }

    private fun mean(cluster: List<Lab>): Lab? =
        if (cluster.isEmpty()) null
        else Lab(cluster.sumOf { it.l } / cluster.size, cluster.sumOf { it.a } / cluster.size, cluster.sumOf { it.b } / cluster.size)

    private fun distSq(p: Lab, q: Lab): Double {
        val dl = p.l - q.l
        val da = p.a - q.a
        val db = p.b - q.b
        return dl * dl + da * da + db * db
    }
}
