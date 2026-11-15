import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.moodrive"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.moodrive"
        minSdk = 28
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"

        // 차량 연동 구현: "pleos"(에뮬레이터/실차) | "mock"(UI 개발용). ./gradlew installDebug -Pmoodrive.vehicle=mock
        val vehicleImpl = (project.findProperty("moodrive.vehicle") as String?) ?: "pleos"
        buildConfigField("String", "VEHICLE_IMPL", "\"$vehicleImpl\"")
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    // release 서명: 루트의 keystore.properties(git 제외)가 있으면 그 키로, 없으면 debug 키로 서명한다
    val keystoreProps = rootProject.file("keystore.properties").takeIf { it.exists() }?.let { file ->
        Properties().apply { file.inputStream().use { load(it) } }
    }
    signingConfigs {
        if (keystoreProps != null) {
            create("release") {
                storeFile = file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
        }
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
    implementation(project(":core:domain"))
    implementation(project(":vehicle"))
    implementation(libs.kotlinx.coroutines.android)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.coil.compose)

    testImplementation(libs.junit)
}
