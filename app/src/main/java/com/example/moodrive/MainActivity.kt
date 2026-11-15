package com.example.moodrive

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.moodrive.ui.common.NavDestination
import com.example.moodrive.ui.control.SceneControlActions
import com.example.moodrive.ui.create.CreateMoodActions
import com.example.moodrive.ui.create.CreateMoodScreen
import com.example.moodrive.ui.create.MoodCompleteActions
import com.example.moodrive.ui.create.MoodCompleteScreen
import com.example.moodrive.ui.create.MoodSelectActions
import com.example.moodrive.ui.create.MoodSelectScreen
import com.example.moodrive.ui.create.SAMPLE_CANDIDATES
import com.example.moodrive.ui.create.toCreatedScene
import com.example.moodrive.ui.create.SceneApplyActions
import com.example.moodrive.ui.create.SceneApplyScreen
import com.example.moodrive.ui.create.VoiceInputActions
import com.example.moodrive.ui.create.VoiceInputScreen
import com.example.moodrive.ui.control.SceneControlScreen
import com.example.moodrive.ui.control.SceneControlViewModel
import com.example.moodrive.ui.explore.ExploreActions
import com.example.moodrive.ui.explore.ExploreProfile
import com.example.moodrive.ui.explore.ExploreScreen
import com.example.moodrive.ui.explore.exploreMoodOf
import com.example.moodrive.ui.explore.toScene
import com.example.moodrive.ui.home.HomeActions
import com.example.moodrive.ui.home.HomeScreen
import com.example.moodrive.ui.home.HomeViewModel
import com.example.moodrive.ui.theme.DesignScale
import com.example.moodrive.ui.theme.MoodriveTheme

class MainActivity : ComponentActivity() {

    private val container get() = (application as MoodriveApp).container

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    private fun openUrl(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: ActivityNotFoundException) {
            toast("이 기기에서 링크를 열 수 있는 앱이 없어요")
        }
    }

    /** 그림 리소스 id → Scene에 저장하는 이미지 주소 */
    private fun resourceUri(resId: Int) = "android.resource://$packageName/$resId"

    /** 탐색 상단 무드 → Scene (상세 화면을 열 수 없는 무드면 null) */
    private fun exploreScene(name: String) = exploreMoodOf(name)?.toScene(::resourceUri)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val factory = viewModelFactory {
            initializer { HomeViewModel(container.sceneRepository, container.profileRepository, container.vehicle, container.sceneSession) }
            initializer { SceneControlViewModel(container.sceneRepository, container.playlistRecommender, container.vehicle, container.sceneSession) }
        }

        setContent {
            MoodriveTheme {
                val home: HomeViewModel = viewModel(factory = factory)
                val control: SceneControlViewModel = viewModel(factory = factory)
                val homeState by home.uiState.collectAsStateWithLifecycle()
                val controlState by control.uiState.collectAsStateWithLifecycle()
                var route by rememberSaveable(stateSaver = RouteSaver) { mutableStateOf<Route>(Route.Home) }

                val navigate: (NavDestination) -> Unit = { dest ->
                    when (dest) {
                        NavDestination.MY_MOOD -> route = Route.Home
                        NavDestination.EXPLORE -> route = Route.Explore
                        // 설정 화면이 생기기 전까지 디버그 빌드에서는 디버그 화면으로 연결.
                        // DebugActivity는 src/debug에만 있어 클래스 이름으로 연다
                        NavDestination.SETTINGS ->
                            if (BuildConfig.DEBUG) {
                                startActivity(Intent().setClassName(this@MainActivity, "com.example.moodrive.DebugActivity"))
                            } else {
                                toast("설정은 준비 중이에요")
                            }
                    }
                }
                // 만들기는 홈·탐색 탭에서만 시작한다. 항상 1단계부터 (이전 흐름 상태는 Route에 남지 않는다)
                val startCreate: () -> Unit = { (route as? Route.Tab)?.let { route = Route.Describe(it) } }

                // 화면이 다시 만들어져도(회전·프로세스 복원) 실행 화면 상태를 맞춘다
                val controlSceneId = (route as? Route.Control)?.sceneId
                LaunchedEffect(controlSceneId) { controlSceneId?.let(control::open) }
                val exploreMoodName = (route as? Route.ExploreMood)?.name
                LaunchedEffect(exploreMoodName) { exploreMoodName?.let(::exploreScene)?.let(control::preview) }

                LaunchedEffect(controlState.message) {
                    controlState.message?.let {
                        toast(it)
                        control.onMessageShown()
                    }
                }

                DesignScale {
                    AnimatedContent(
                        targetState = route,
                        transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) },
                        label = "screen",
                    ) { target ->
                        when (target) {
                            is Route.Describe -> CreateMoodScreen(
                                driveMode = homeState.driveMode,
                                actions = CreateMoodActions(
                                    onBack = { route = target.origin },
                                    onVoice = { route = Route.Voice(target.origin) },
                                    onSubmit = { route = Route.SelectMood(target.origin, it) },
                                ),
                            )
                            is Route.Voice -> VoiceInputScreen(
                                driveMode = homeState.driveMode,
                                actions = VoiceInputActions(
                                    onBack = { route = Route.Describe(target.origin) },
                                    onDone = { route = Route.SelectMood(target.origin, it) },
                                ),
                            )
                            is Route.SelectMood -> MoodSelectScreen(
                                driveMode = homeState.driveMode,
                                request = target.request,
                                startReady = target.chosenRank != null,
                                initialRank = target.chosenRank ?: 1,
                                actions = MoodSelectActions(
                                    onBack = { route = Route.Describe(target.origin) },
                                    onRetryVoice = { route = Route.Voice(target.origin) },
                                    // 지금 요청은 그대로 두고 말한 조건을 덧붙여 후보를 다시 만든다
                                    onRefine = { route = Route.SelectMood(target.origin, "${target.request}, $it") },
                                    onMore = { toast("더 많은 무드 찾아보기는 준비 중이에요") },
                                    onConfirm = { route = Route.Apply(target.origin, target.request, it.rank) },
                                ),
                            )
                            is Route.Apply -> SceneApplyScreen(
                                driveMode = homeState.driveMode,
                                mood = candidateOf(target.rank),
                                actions = SceneApplyActions(
                                    onCancel = { route = Route.SelectMood(target.origin, target.request, target.rank) },
                                    onComplete = { route = Route.Done(target.origin, target.request, target.rank) },
                                ),
                            )
                            is Route.Done -> MoodCompleteScreen(
                                driveMode = homeState.driveMode,
                                mood = candidateOf(target.rank),
                                actions = MoodCompleteActions(
                                    // 다시 적용 단계로 가지 않고 무드 고르기로 돌아간다
                                    onBack = { route = Route.SelectMood(target.origin, target.request, target.rank) },
                                    onTryOut = { toast("‘${it.name}’ 실행해보기는 준비 중이에요") },
                                    // 내 Mood '전체' 맨 앞에 추가하고 홈으로 돌아간다
                                    onSave = { draft ->
                                        home.onMoodCreated(candidateOf(target.rank).toCreatedScene(draft, ::resourceUri))
                                        route = Route.Home
                                        toast("‘${draft.name.trim().ifEmpty { candidateOf(target.rank).name }}’ 무드를 저장했어요")
                                    },
                                    onEyedropper = { toast("스포이드는 준비 중이에요") },
                                ),
                            )
                            Route.Home -> HomeScreen(
                                state = homeState,
                                actions = HomeActions(
                                    onFilterSelected = home::onFilterSelected,
                                    onSceneSelected = home::onSceneSelected,
                                    onDismissSelection = home::onDismissSelection,
                                    onApply = { homeState.selected?.card?.id?.let { route = Route.Control(it) } },
                                    onEnd = control::onEnd,
                                    onFineTune = { toast("세부 조절 화면은 다음 단계에서 구현합니다") },
                                    onToggleFavorite = home::onToggleFavorite,
                                    onNavigate = navigate,
                                    onCreate = startCreate,
                                ),
                            )
                            Route.Explore -> ExploreScreen(
                                profile = ExploreProfile(
                                    handle = homeState.handle,
                                    avatarUrl = homeState.avatarUrl,
                                    avatarFallback = homeState.greetingName.take(1),
                                    driveMode = homeState.driveMode,
                                    canCreate = homeState.canCreate,
                                ),
                                actions = ExploreActions(
                                    onNavigate = navigate,
                                    onCreate = startCreate,
                                    onSearch = { toast("검색은 준비 중이에요") },
                                    onVoiceSearch = { toast("음성 검색은 준비 중이에요") },
                                    onShare = { toast("내 무드 공유하기는 준비 중이에요") },
                                    onOpenMood = { mood ->
                                        if (exploreScene(mood.name) != null) route = Route.ExploreMood(mood.name)
                                        else toast("‘${mood.name}’ 상세 화면은 준비 중이에요")
                                    },
                                    onSeeAllCuration = { toast("큐레이션 전체보기는 준비 중이에요") },
                                    onSeeAllCollections = { toast("컬렉션 전체보기는 준비 중이에요") },
                                    onOpenCollection = { toast("‘${it.title}’ 컬렉션은 준비 중이에요") },
                                ),
                            )
                            // 실행 화면은 홈에서, 탐색 무드 상세 화면은 탐색에서 열리므로 나가면 그 탭으로 돌아간다
                            is Route.Control -> SceneControlScreen(state = controlState, actions = controlActions(control, back = Route.Home) { route = it })
                            is Route.ExploreMood -> SceneControlScreen(state = controlState, actions = controlActions(control, back = Route.Explore) { route = it })
                        }
                    }
                }
            }
        }
    }

    /** 실행 화면(상세 화면) 동작. 나가면 [back] 탭으로 돌아간다 */
    private fun controlActions(control: SceneControlViewModel, back: Route.Tab, navigate: (Route) -> Unit) = SceneControlActions(
        onBack = { navigate(back) },
        onToggleFavorite = control::onToggleFavorite,
        onEditMood = control::onEditMood,
        onDraftChange = control::onDraftChange,
        onCloseEditor = control::onCloseEditor,
        onSaveMood = control::onSaveMood,
        onEyedropper = { toast("스포이드는 준비 중이에요") },
        onEnd = {
            control.onEnd()
            navigate(back)
        },
        onOpenUrl = ::openUrl,
        onSaveToLibrary = control::onSavePreview,
        onApplyPreview = control::onApplyPreview,
    )
}

/** 2단계에서 고른 순위의 무드 후보. (서버 연동 전 샘플 후보) */
private fun candidateOf(rank: Int) = SAMPLE_CANDIDATES.firstOrNull { it.rank == rank } ?: SAMPLE_CANDIDATES.first()
