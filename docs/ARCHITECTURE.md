# Moodrive 아키텍처

사진·문장으로 표현한 취향을 차량 조명·공조·음악 설정(Scene)으로 바꾸고, 적용 → 복원 → 공유하는 Pleos Connect 차량 앱.
현재 범위는 **차량 앱**이다. 모바일 앱은 후속 개발.

## 1. 시스템 구성

```
┌──────────────────────────┐       ┌──────────────────────┐
│ Firebase (예정)           │◀─────▶│ 차량 앱 (AAOS)         │
│ Auth · Firestore ·        │       │ 호환성·미리보기·적용·복원 │
│ Storage · Functions       │       │ 주행 상태별 UI         │
│   └ AI 파이프라인          │       └──────────┬───────────┘
│     (LLM 키는 서버에만)     │                  │
└──────────────────────────┘        Pleos Vehicle SDK 2.3.2
                                    (AAOS Car API: 예정)
```

- LLM 호출은 Cloud Functions에서만 한다. API 키를 앱에 넣지 않는다.
- 색 추출·ΔE2000 매핑은 순수 Kotlin(`:core:color`)으로 구현해, 이후 모바일 앱과 공유한다.

## 2. 모듈

```
moodrive/
├── app/            차량 앱: 화면, 의존성 조립(AppContainer), 스냅샷 영속화
├── core/
│   ├── color/      [순수 Kotlin] CIELAB 변환, CIEDE2000, k-means 팔레트 추출, 지원색 매칭
│   └── domain/     [순수 Kotlin] Scene 모델, 차량 포트(인터페이스), ScenePlanner, SceneSession
└── vehicle/        [Android lib] Pleos SDK 어댑터, Mock 구현, VehicleModule(진입점)
```

의존 방향: `app → vehicle → core:domain → core:color`

| 규칙 | 이유 |
|---|---|
| Pleos SDK 타입은 `:vehicle` 밖으로 나가지 않는다 | SDK 교체·Mock 전환을 한 곳에서 처리 |
| `core:*`는 Android 의존 없음 | JVM 단위 테스트로 빠르게 검증 (`./gradlew test`) |
| 모바일 앱은 추가 시 `:app-mobile`로 만들고 `core:*`를 공유 | 같은 색 매핑 결과 보장 |

## 3. 핵심 흐름

### 3-1. Scene → 적용값 계산 (`ScenePlanner`)

```
Scene(원본 팔레트·밝기%·공조 희망값)
  + LightCapabilities(권한, 밝기 범위, 지원색)
  + ClimateCapabilities
  + 공조 승인 여부
  → ScenePlan(실제 적용할 조명/공조 값, 매핑 ΔE, 제외 항목과 이유)
```

- 미리보기 화면(예정)은 `ScenePlan`을 그대로 보여준다 (실제 적용될 색 사전 표시). 제외 항목(`excluded`, `Feature`)은 이 화면용이라 지금 앱은 읽지 않는다.
- 공조는 사용자가 승인했을 때만 계획에 포함된다.
- 원본 팔레트를 Scene에 보관하므로 다른 차종에서도 다시 매핑할 수 있다 (Remix).
- `Scene.visibility`(공유·공개 범위)·`origin`(Remix)·`sound`(사운드), `Mood.brightnessCharacter`는 예정 기능용 예약 필드다. 지금 앱은 읽지 않는다.

### 3-2. 적용·복원 (`SceneSession`)

```
Idle ─apply→ Applying ─→ Active ─end→ Restoring ─→ Idle
                            └─apply(다른 Scene)→ Applying (최초 스냅샷 유지)
```

1. 이번에 처음 건드리는 영역의 현재 상태를 읽어 **스냅샷을 디스크에 먼저 저장**한다.
2. 스냅샷을 못 뜬 영역은 적용하지 않는다 (되돌릴 수 없으므로).
3. 조명 → 공조 순서로 적용. 한 영역이 실패해도 나머지는 진행하고 `ApplyReport`로 항목별 결과를 돌려준다.
4. 종료 시 스냅샷으로 복원. 복원 실패한 영역은 저장소에 남겨 두고 앱 시작 시(`MoodriveApp.onCreate`에서 `connect()` 직후) `recoverIfNeeded()`로 재시도한다. SDK가 연결 전 호출을 대기열에 쌓았다가 처리하므로 바인딩 완료를 기다리지 않는다.
5. 모든 차량 호출은 SDK 성공 콜백을 받은 뒤에만 완료로 처리한다.
6. 잠금은 스냅샷 / 조명 / 공조로 나뉜다. 조명은 안내 모드에서 최대 60초 기다리므로, 조명만·공조만 담은 계획은 서로 막지 않는다. 종료·복구는 모든 잠금을 잡는다.
7. 적용 중 취소되면(실행 화면에서 값을 연달아 바꿀 때) 상태를 `Applying`에 두지 않고 `Active`(스냅샷이 있으면) 또는 `Idle`로 정리한다.

조명 적용 순서(`PleosLightController`): 주행 모드 자동 색 끄기 → 켜기 → 색 → 밝기 → (끄기) → (자동 색 복원).
자동 색이 켜져 있으면 Scene 색을 덮어쓰기 때문에, 이 값도 스냅샷에 포함한다.

### 3-3. 제어 권한이 없을 때: 안내 모드 (`GuidedLightController`)

Capability의 `AccessType`이 `READ`면(제어 API 미공개) 자동으로 안내 모드가 된다.
`READ_WRITE`여도 먼저 실제로 적용한 뒤 다시 읽어 반영됐는지 확인하고, 반영되지 않았으면 안내 모드로 넘어간다 (3-5 쓰기 반영 참고).

1. `guide`에 목표 값을 내보낸다 → UI가 "가상 콘솔에서 이 값으로 바꿔 주세요"를 표시
2. 리스너로 같은 값이 감지되면 적용 성공 (시간 초과 시 실패)
3. 권한이 열리면(`READ_WRITE`) 코드 변경 없이 실제 제어로 바뀐다

UI는 안내 모드임을 명확히 표시해야 한다. 실제 제어로 오인하게 하지 않는다.

### 3-4. 주행 상태 (`DriveStateSource`)

현재는 기어 기준(P = 정차, 그 외 = 주행). 기획서 기준인 `CarUxRestrictionsManager` 연동은 에뮬레이터에서 확인한 뒤 교체한다.

### 3-5. 에뮬레이터(Pleos Connect 2.1.0)에서 확인한 사실

| 항목 | 결과 |
|---|---|
| 앰비언트 색 권한 | `READ_WRITE` (실제 제어 가능) |
| 앰비언트 영역 | 운전석 단독이 아니라 `[ROW_1_LEFT, ROW_1_RIGHT, ROW_2_LEFT, ROW_2_CENTER, ROW_2_RIGHT]` 묶음 하나 → 영역은 Capability 응답에서 결정 |
| 밝기 범위 | 0..100 |
| 주행 모드 자동 색 | 미지원 (property `0x21200787`) → 실패해도 무시 |
| **쓰기 반영** | 권한 `READ_WRITE` + 성공 콜백을 주지만 **실제 값은 바뀌지 않음** (조명·시트 열선·핸들 열선 확인). 단 **시트 통풍은 실제로 반영됨** (2026-11-15 확인). 적용 후 다시 읽어 검증하고, 조명은 안내 모드로 전환 |
| 공조 영역 | 온도 `[ROW_1_LEFT]`·`[ROW_1_RIGHT]` 개별, 풍량·바람 방향은 1열 묶음 → 항목별로 Capability에서 영역 결정 |
| 공조 전원 꺼짐 | 풍량·바람 방향 설정이 `NOT_AVAILABLE`로 거부, 온도 조회 불가 → 항목별 독립 적용 후 일부 실패는 `Partial`로 보고 |

## 4. 화면

- 시안 캔버스는 1000×696dp(시안 이미지 2000×1392px의 절반). `DesignScale`이 실제 앱 영역에 맞춰 비율대로 확대한다.
- 이미지: `assets/scenes/<sceneId>.jpg`, 실행 화면 가로 배경 `assets/scenes/<sceneId>_wide.jpg`(있을 때만), 프로필 `assets/profile/avatar.jpg`. 없으면 팔레트 그라데이션/이니셜로 대체.
- 폰트: Pretendard (`res/font`, `ui/theme`의 `MoodriveFont`). 글자 스타일은 `ui/theme`의 `text()`, 시안 px 기준은 `ui/common`의 `style()`.
- 화면 전환: `MainActivity`가 `Route`(sealed interface, `Route.kt`) 하나로 홈·탐색 탭 / 실행 화면 / 만들기 4단계(설명하기·음성 → 무드 고르기 → 설정 만들기 → 완성)를 바꾼다. 회전·프로세스 복원은 `RouteSaver`로 저장한다. 내비게이션 라이브러리는 쓰지 않는다.
- 설정 메뉴: 디버그 빌드는 `DebugActivity`(`app/src/debug`, 디버그 빌드에만 포함)를 열고, 배포 빌드는 "준비 중" 토스트를 띄운다.

UI 패키지 의존 방향: 기능 화면(`ui/home`·`ui/control`·`ui/create`·`ui/explore`) → `ui/mood`(무드 수정 공통 편집기, 실행 화면·만들기 완성 화면이 함께 씀) → `ui/common`(공통 컴포넌트·시안 단위) → `ui/theme`(색·폰트·글자 스타일). `ui/theme`은 다른 UI 패키지를 참조하지 않는다.

### 4-1. 실행 화면 (`ui/control`)

- 홈의 "적용하기" → `SceneControlViewModel.open(sceneId)`. 공조를 먼저, 조명을 그다음에 **각각** 적용한다.
- 탐색 상단 무드(히어로·추천)를 누르면 같은 화면을 미리보기로 연다 (`Route.ExploreMood` → `preview(scene)`, 차량에 적용하지 않음). 하단 버튼은 "내 Mood에 저장" / "Mood 적용". 적용하면 홈에서 다시 찾고 종료할 수 있도록 내 Mood에 넣은 뒤(`SceneRepository.save`는 없는 id면 추가) 실행 화면으로 바뀐다. 탐색 무드 → Scene 변환은 `ui/explore/ExploreScenes.kt`.
- 하단 카드(앰비언트·시트·바람·핸들 열선)는 보여주기만 한다. 값은 "무드 수정"에서만 바꾸고, 저장하면 다시 적용한다.
  카드 값은 **목표값**(Scene을 이 차량에 맞춘 값)이다. 차량의 지금 값과는 다를 수 있다 (에뮬레이터는 일부 쓰기가 반영되지 않음).
- "Scene 종료" → 바로 종료하고 적용 전 상태로 복원한다.
- 추천 플레이리스트(`PlaylistSection.kt`)는 `PlaylistRecommender` 포트. 링크는 Intent로 연다. 지금은 `SamplePlaylistRecommender`(고정 제목 + Spotify 웹 검색 링크), 썸네일은 `assets/playlists/<sceneId>_<n>.jpg`.

### 4-2. 무드 수정 (`MoodEditPanel`, `MoodDraft`)

- 실행 화면의 "무드 수정" → 왼쪽에서 패널이 나온다. 지금 실행 중인 값에서 시작한다.
- 앰비언트(Main·상단 / Sub·하단 색, 시안 8색, 전체 밝기), 시트(열선/통풍, 1~3단), 바람(발/얼굴, 풍량 1~8단 스테퍼), 핸들 열선. 섹션 토글을 끄면 그 기능을 끈 값(0단, 조명 꺼짐)으로 저장한다.
- "무드에 저장" → `SceneRepository.save` (version +1) 후 저장한 값으로 다시 적용한다.
- 사용자가 고른 색은 `Visual.ambient`에 저장하고, `ScenePlanner`는 팔레트보다 이 색을 우선해 차량 지원색으로 매핑한다. 지금 차량은 앰비언트 영역이 하나라 Main만 적용한다.
- 시트 통풍: SDK `setHvacSeatVentilation` (에뮬레이터 0..3단 지원 확인). 열선과 동시에 켜지 않으며, 끄는 쪽을 먼저 보낸다.
- 스포이드는 아직 없음 (버튼만 표시).
- 세션 스냅샷에 없던 공조 항목(이전 버전 스냅샷 등)은 적용 전에 현재 값으로 채운다. 기존에 저장된 값은 덮어쓰지 않는다.

## 5. 차량 연동 구현 전환

```bash
./gradlew installDebug                          # Pleos SDK (기본)
./gradlew installDebug -Pmoodrive.vehicle=mock  # 차량 없이 UI 개발
```

Mock에서 안내 모드·주행 상태를 재현하려면 `AppContainer`에서 `VehicleModule.mock(lightAccess = Access.READ, driveMode = DriveMode.DRIVING)`처럼 넘긴다 (기본값은 `READ_WRITE`, `PARKED`).

## 6. 다음 단계

| 순서 | 작업 | 위치 |
|---|---|---|
| 1 | 에뮬레이터에서 지원색 int 값 확인 → `VehicleColors.EMULATOR` 교체 | `:vehicle` |
| 2 | ~~SDK 바인딩 완료 감지~~ 불필요: SDK가 연결 전 호출을 대기열에 쌓았다가 처리한다 (`initialize()`만 먼저 부르면 됨). 시작 시 복구는 `MoodriveApp.onCreate`에서 `connect()` 직후 실행 | — |
| 3 | Compose HMI: 홈 세부 조절 화면, 만들기 흐름 실제 연결(지금은 샘플 후보·저장/실행해보기 준비 중), 탐색 화면 커뮤니티 연동, 스포이드 | `:app` |
| 4 | 이미지 → 팔레트 추출 연결 (Bitmap 축소 → `PaletteExtractor`) | `:app` |
| 5 | Firestore 동기화, `generateScene` Cloud Function | `:core:data`(신규), `functions/`(신규) |
| 6 | CarUxRestrictions 기반 주행 제한 | `:vehicle` |
| 7 | 공조 안내 모드 (조명과 같은 방식) | `:core:domain` |
| 8 | Spotify 검색 API로 플레이리스트 추천 교체 | `:app` |
