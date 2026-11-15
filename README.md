# Moodrive

> 사진이나 문장으로 표현한 취향을 차량 조명·공조·시트·음악 설정(Mood)으로 바꿔 적용하고, 끝나면 원래 상태로 되돌리는 Pleos Connect 차량 앱

팀 쓰리슬쩍 · 현대자동차 Pleos Connect 공모전 출품작

## APK

| 항목 | 내용 |
|---|---|
| 파일 위치 | [`apk/쓰리슬쩍_Moodrive_release.apk`](apk/쓰리슬쩍_Moodrive_release.apk) |
| 빌드 종류 | release (서명됨, 난독화 없음) |
| 패키지명 | `com.example.moodrive` |

## 실행 방법

### APK로 바로 실행

1. Pleos Connect Emulator 2.1.0을 실행합니다.
2. APK를 설치합니다.
   ```bash
   adb install apk/쓰리슬쩍_Moodrive_release.apk
   ```
3. 에뮬레이터 런처에서 **Moodrive**를 실행합니다. (또는 `adb shell am start -n com.example.moodrive/.MainActivity`)
4. 홈에서 Mood 카드를 고르고 **적용하기**를 누르면 차량 조명·공조·시트·핸들 열선에 적용됩니다. **Scene 종료**를 누르면 적용 전 상태로 돌아갑니다.

### 소스에서 빌드

1. JDK 17을 설치합니다. (Gradle 8.14는 JDK 25에서 실행되지 않습니다. `gradle/gradle-daemon-jvm.properties`가 로컬의 JDK 17을 자동으로 찾아 씁니다.)
2. Android Studio로 이 폴더를 열고 Gradle Sync를 합니다. `local.properties`는 Android Studio가 자동으로 만듭니다.
3. 에뮬레이터를 실행한 뒤 빌드·설치합니다.
   ```bash
   ./gradlew installDebug        # debug
   ./gradlew assembleRelease     # release → app/build/outputs/apk/release/app-release.apk
   ```
   release 빌드는 루트의 `keystore.properties`(git 제외)가 있으면 그 키로 서명하고, 없으면 debug 키로 서명합니다.
4. 로그 확인: `adb logcat -s MOODRIVE`

## 테스트 환경 및 필요한 설정

| 구분 | 내용 |
|---|---|
| 에뮬레이터 | **Pleos Connect Emulator 2.1.0** (AVD `Pleos_Connect_v2.1.0`, Android 14, arm64) |
| 화면 규격 | 디스플레이 **2560×1440px, 160dpi** (가로) / 앱 영역 **1682×1210px** (= 1682×1210dp) |
| 차량 SDK | Pleos Vehicle SDK `ai.pleos.playground:Vehicle:2.3.2` |
| Min / Target / Compile SDK | 28 / 34 / 35 |
| 빌드 도구 | Gradle 8.14, AGP 8.13.1, Kotlin 2.2.10, JDK 17 |

- **별도 설정이나 로그인은 필요 없습니다.** 앱에 샘플 Mood가 들어 있어 설치 후 바로 실행할 수 있습니다.
- 차량 권한(`pleos.car.permission.CAR_*`, `CONTROL_CAR_*`)은 매니페스트에 선언되어 있습니다.
- 에뮬레이터의 **Virtual Console**에서 기어(P/D), 조명, 공조 값을 바꾸면 앱에 실시간으로 반영됩니다.
  - 기어를 P 외의 값으로 바꾸면 주행 중 상태가 되어 Mood 만들기와 세부 조절이 비활성화됩니다.
  - 공조 전원이 꺼져 있으면 풍량·바람 방향 설정이 거부됩니다. 공조를 켠 상태에서 확인해 주세요.
- **에뮬레이터 동작 참고**: 2.1.0에서 조명·시트 열선·핸들 열선은 설정 성공 응답을 받아도 실제 값이 바뀌지 않습니다(시트 통풍은 반영됨). 앱은 적용 후 값을 다시 읽어 확인하고, 실행 화면 카드에는 차량의 현재 값이 아니라 Mood의 **목표 값**을 표시합니다. 조명이 반영되지 않으면 Virtual Console에서 같은 색·밝기로 바꿀 때 적용 완료로 처리합니다.
- 차량 없이 UI만 확인하려면 Mock 구현으로 빌드할 수 있습니다: `./gradlew installDebug -Pmoodrive.vehicle=mock`

## 구조

```
app/            차량 앱: 화면(Jetpack Compose), 의존성 조립, 스냅샷 저장
core/color/     [순수 Kotlin] CIELAB, ΔE2000, 팔레트 추출, 지원색 매칭
core/domain/    [순수 Kotlin] Scene 모델, 차량 포트, ScenePlanner, SceneSession(적용·복원)
vehicle/        Pleos SDK 어댑터 + Mock (SDK 타입은 이 모듈 밖으로 나가지 않음)
apk/            제출용 release APK
```

자세한 설계는 [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)를 참고하세요.

- 단위 테스트: `./gradlew test`

## 협업 규칙

- 브랜치: `main`(배포) ← `develop`(통합) ← `feat/<기능>`, `fix/<내용>`
- 커밋: `feat: ...`, `fix: ...`, `refactor: ...`, `docs: ...`, `chore: ...`
- API 키·서명 키 등 비밀값은 `secrets.properties`, `keystore.properties`(git 제외)에 두고 커밋하지 않습니다.
