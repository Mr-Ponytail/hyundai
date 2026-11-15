// 순수 Kotlin: 색 공간 변환, ΔE2000, 팔레트 추출. Android 의존 없음 → JVM 단위 테스트
plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    testImplementation(libs.junit)
}
