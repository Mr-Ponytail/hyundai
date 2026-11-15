// Pleos Vehicle SDK 어댑터. SDK 타입은 이 모듈 밖으로 노출하지 않는다.
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.example.moodrive.vehicle"
    compileSdk = 35

    defaultConfig {
        minSdk = 28
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    api(project(":core:domain"))
    implementation(libs.pleos.vehicle)
    implementation(libs.kotlinx.coroutines.core)
}
