package com.example.moodrive.ui.control

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import com.example.moodrive.R
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import androidx.annotation.DrawableRes
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.moodrive.domain.model.DriveMode
import com.example.moodrive.domain.model.FanDirection
import com.example.moodrive.domain.model.Playlist
import com.example.moodrive.ui.common.DetailIcon
import com.example.moodrive.ui.common.DriveStatusPill
import com.example.moodrive.ui.common.FavoriteToggle
import com.example.moodrive.ui.theme.accentLight
import com.example.moodrive.ui.common.clickableNoRipple
import com.example.moodrive.ui.common.fig
import com.example.moodrive.ui.common.paletteBrush
import com.example.moodrive.ui.common.rememberLastNonNull
import com.example.moodrive.ui.theme.text
import com.example.moodrive.ui.common.DetailKind
import com.example.moodrive.ui.mood.HEAT_RED
import com.example.moodrive.ui.mood.MoodDraft
import com.example.moodrive.ui.mood.VENT_BLUE
import com.example.moodrive.ui.theme.MoodriveColors
import com.example.moodrive.ui.theme.MoodriveTheme
import com.example.moodrive.ui.theme.accentOf

data class SceneControlActions(
    val onBack: () -> Unit = {},
    val onToggleFavorite: () -> Unit = {},
    val onEditMood: () -> Unit = {},
    val onDraftChange: (MoodDraft) -> Unit = {},
    val onCloseEditor: () -> Unit = {},
    val onSaveMood: () -> Unit = {},
    val onEyedropper: () -> Unit = {},
    val onEnd: () -> Unit = {},
    val onOpenUrl: (String) -> Unit = {},
    /** 탐색 상세 화면(미리보기)의 "내 Mood에 저장" */
    val onSaveToLibrary: () -> Unit = {},
    /** 탐색 상세 화면(미리보기)의 "Mood 적용" */
    val onApplyPreview: () -> Unit = {},
)

/**
 * Scene 실행 화면 (시안 2026-11 "적용하기 이후").
 * 하단 카드는 보여주기만 하고, 값은 "무드 수정"에서만 바꾼다.
 * 탐색에서 연 무드는 같은 화면을 미리보기로 보여준다 ([SceneControlUiState.isPreview]): 차량에 적용하기 전이라
 * 무드 수정 대신 하단에 "내 Mood에 저장" / "Mood 적용" 버튼을 둔다.
 */
@Composable
fun SceneControlScreen(state: SceneControlUiState, actions: SceneControlActions, modifier: Modifier = Modifier) {
    // "더 보러가기" → 분할 화면 (왼쪽 세로형 실행 화면 + 오른쪽 플레이리스트 목록)
    var split by rememberSaveable { mutableStateOf(false) }
    BackHandler {
        when {
            split -> split = false
            state.moodDraft != null -> actions.onCloseEditor()
            else -> actions.onBack()
        }
    }
    val accent = remember(state.paletteArgb) { accentOf(state.paletteArgb) }

    BoxWithConstraints(
        modifier
            .fillMaxSize()
            .background(MoodriveColors.Background),
    ) {
        val fullWidth = maxWidth
        // 시안 780:47459: 앱 840 + 구분선 20 + 목록 840 (= 캔버스 1700)
        val appWidth by animateDpAsState(if (split) fig(SPLIT_APP) else fullWidth, tween(SPLIT_MS, easing = FastOutSlowInEasing), label = "splitWidth")
        val appRadius by animateDpAsState(if (split) fig(32) else 0.dp, tween(SPLIT_MS), label = "splitRadius")

        Box(
            Modifier
                .width(appWidth)
                .fillMaxHeight()
                .clip(RoundedCornerShape(appRadius)),
        ) {
            Backdrop(state)
            // 세로형에선 카드가 사진 밝은 쪽에 놓이므로 배경을 한 단계 더 어둡게 한다 (시안 Overlay)
            val splitDim by animateFloatAsState(if (split) 0.45f else 0f, tween(SPLIT_MS), label = "splitDim")
            Box(Modifier.fillMaxSize().background(OVERLAY.copy(alpha = splitDim)))
            // 레이아웃이 줄어드는 동안 내용은 원래 폭을 유지한 채 잘리고, 두 레이아웃은 교차 페이드된다
            Crossfade(split, animationSpec = tween(SPLIT_MS), label = "splitContent") { compact ->
                if (compact) {
                    CompactContent(
                        state, actions, accent,
                        // 무드 수정은 전체 화면 패널이라 분할을 먼저 닫는다
                        onEditMood = { split = false; actions.onEditMood() },
                        modifier = Modifier.requiredWidth(fig(SPLIT_APP)).fillMaxHeight(),
                    )
                } else {
                    WideContent(
                        state, actions, accent,
                        onMore = { split = true },
                        modifier = Modifier.requiredWidth(fullWidth).fillMaxHeight(),
                    )
                }
            }
        }

        AnimatedVisibility(
            split,
            modifier = Modifier.align(Alignment.CenterEnd),
            enter = slideInHorizontally(tween(SPLIT_MS, easing = FastOutSlowInEasing)) { it } + fadeIn(tween(SPLIT_MS)),
            exit = slideOutHorizontally(tween(SPLIT_MS, easing = FastOutSlowInEasing)) { it } + fadeOut(tween(SPLIT_MS)),
        ) {
            Row(Modifier.fillMaxHeight()) {
                WindowDivider()
                PlaylistMorePanel(
                    query = state.moreQuery,
                    playlists = state.playlists,
                    palette = state.paletteArgb,
                    onSearch = { state.moreUrl?.let(actions.onOpenUrl) },
                    onOpen = { actions.onOpenUrl(it.url) },
                    onClose = { split = false },
                )
            }
        }

        MoodEditLayer(state, actions)
    }
}

/** 기본(넓은) 실행 화면: 제목 · 추천 플레이리스트 카드 · 하단 카드 4개 + 버튼 */
@Composable
private fun WideContent(state: SceneControlUiState, actions: SceneControlActions, accent: Color, onMore: () -> Unit, modifier: Modifier) {
    Box(modifier) {
        // 상단: 뒤로 · 즐겨찾기 · 제목
        // 시안 화살표(64px) 위치에 맞추고 터치 영역은 48dp로 둔다
        BackButton(actions.onBack, Modifier.padding(start = fig(40) - 5.dp, top = fig(50) - 5.dp))
        TitleBlock(state, actions, accent, Modifier.padding(start = fig(60), top = fig(184)))
        DriveStatusPill(state.driveMode, Modifier.align(Alignment.TopEnd).padding(top = fig(59), end = SIDE_PADDING))

        PlaylistSection(state, actions.onOpenUrl, onMore = onMore, modifier = Modifier.padding(start = fig(60), top = fig(520)))

        // 하단: 항목 카드 4개 + 무드 수정 / Scene 종료
        Row(
            Modifier.align(Alignment.BottomStart).padding(start = CARD_START, end = SIDE_PADDING, bottom = BOTTOM_PADDING).height(CARD_HEIGHT),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(CARD_GAP)) {
                state.cards.forEach { card ->
                    ControlCard(card, state.targets, accent)
                }
            }
            Spacer(Modifier.weight(1f))
            Column(Modifier.width(fig(483)).fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween) {
                BottomButtons(state, actions, onEditMood = actions.onEditMood)
            }
        }
    }
}

/**
 * 분할 화면의 세로형 실행 화면 (시안 780:47468, 840×1184).
 * 제목 · 카드 2×2 · 무드 수정 / Scene 종료. 추천 플레이리스트 카드는 오른쪽 목록으로 대신한다.
 */
@Composable
private fun CompactContent(state: SceneControlUiState, actions: SceneControlActions, accent: Color, onEditMood: () -> Unit, modifier: Modifier) {
    Box(modifier) {
        BackButton(actions.onBack, Modifier.padding(start = fig(40) - 5.dp, top = fig(50) - 5.dp))
        TitleBlock(state, actions, accent, Modifier.padding(start = fig(60), top = fig(154), end = fig(60)))
        DriveStatusPill(state.driveMode, Modifier.align(Alignment.TopEnd).padding(top = fig(59), end = SIDE_PADDING))

        Column(Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(start = fig(60), end = fig(60), bottom = BOTTOM_PADDING)) {
            state.cards.chunked(2).forEachIndexed { i, row ->
                if (i > 0) Spacer(Modifier.height(CARD_GAP))
                Row(horizontalArrangement = Arrangement.spacedBy(CARD_GAP)) {
                    row.forEach { card -> ControlCard(card, state.targets, accent, Modifier.weight(1f)) }
                }
            }
            Spacer(Modifier.height(fig(56)))
            Column(verticalArrangement = Arrangement.spacedBy(fig(32))) {
                BottomButtons(state, actions, onEditMood = onEditMood)
            }
        }
    }
}

/** 즐겨찾기 · 제목 · 태그 */
@Composable
private fun TitleBlock(state: SceneControlUiState, actions: SceneControlActions, accent: Color, modifier: Modifier) {
    Column(modifier) {
        // 미리보기는 아직 내 Mood에 없는 무드라 즐겨찾기를 숨긴다 (자리는 유지해 제목 위치를 맞춘다)
        Box(Modifier.size(fig(64)).alpha(if (state.isPreview) 0f else 1f)) {
            FavoriteToggle(
                state.isFavorite, { if (!state.isPreview) actions.onToggleFavorite() },
                size = fig(64), cornerRadius = fig(16), background = Color.White.copy(alpha = 0.2f),
                filledStarSize = fig(32), outlineStarSize = fig(34),
            )
        }
        Spacer(Modifier.height(fig(20)))
        Text(
            state.title,
            style = text(35.sp, FontWeight.Bold, Color.White, lineHeight = 47.sp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(fig(24)))
        // 시안 #FFD6AA: 강조색을 밝게 푼 색
        Text(state.tags, style = text(17.5.sp, FontWeight.Normal, accentLight(accent), lineHeight = 22.sp))
    }
}

/** 미리보기면 "내 Mood에 저장" / "Mood 적용", 실행 중이면 "무드 수정" / "Scene 종료" */
@Composable
private fun BottomButtons(state: SceneControlUiState, actions: SceneControlActions, onEditMood: () -> Unit) {
    if (state.isPreview) {
        SaveToLibraryButton(saved = state.isSaved, onClick = actions.onSaveToLibrary)
        ApplyPreviewButton(actions.onApplyPreview)
    } else {
        EditMoodButton(enabled = state.canEdit, onClick = onEditMood)
        EndButton(actions.onEnd)
    }
}

/** 분할 화면 가운데 구분선 (시안 ivi_window_bar: 폭 20, 손잡이 4×160) */
@Composable
private fun WindowDivider() {
    Box(Modifier.width(fig(20)).fillMaxHeight(), contentAlignment = Alignment.Center) {
        Box(Modifier.size(fig(4), fig(160)).clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.2f)))
    }
}

/** 무드 수정: 뒤 화면을 어둡게 하고 왼쪽에서 패널을 밀어 넣는다 (닫히는 동안 마지막 값 유지) */
@Composable
private fun MoodEditLayer(state: SceneControlUiState, actions: SceneControlActions) {
    val last = rememberLastNonNull(state.moodDraft)
    val visible = state.moodDraft != null

    AnimatedVisibility(visible, enter = fadeIn(tween(250)), exit = fadeOut(tween(250))) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f))
                .clickableNoRipple(actions.onCloseEditor),
        )
    }
    AnimatedVisibility(
        visible,
        enter = slideInHorizontally(tween(320, easing = FastOutSlowInEasing)) { -it },
        exit = slideOutHorizontally(tween(260, easing = FastOutSlowInEasing)) { -it },
    ) {
        last?.let { draft ->
            MoodEditPanel(
                draft = draft,
                saving = state.isSaving,
                actions = MoodEditActions(
                    onChange = actions.onDraftChange,
                    onClose = actions.onCloseEditor,
                    onSave = actions.onSaveMood,
                    onEyedropper = actions.onEyedropper,
                ),
            )
        }
    }
}

// ───────────────────────── 배경 ─────────────────────────

@Composable
private fun Backdrop(state: SceneControlUiState) {
    Box(Modifier.fillMaxSize().background(paletteBrush(state.paletteArgb)))
    state.imageUrl?.let { AsyncImage(it, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
    // 시안 Overlay: 왼쪽(텍스트)일수록 어둡게 + 아래(카드·버튼)로 갈수록 어둡게
    Box(
        Modifier.fillMaxSize().background(
            Brush.horizontalGradient(0f to OVERLAY.copy(alpha = 0.88f), 0.55f to OVERLAY.copy(alpha = 0.45f), 1f to OVERLAY.copy(alpha = 0.10f)),
        ),
    )
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.47f to Color.Transparent, 0.78f to OVERLAY.copy(alpha = 0.85f))))
}

@Composable
private fun BackButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier.size(48.dp).clip(CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(R.drawable.ic_back), contentDescription = "뒤로", tint = Color.White, modifier = Modifier.size(fig(64)))
    }
}

// ───────────────────────── 하단 카드 ─────────────────────────

@Composable
/** 보여주기만 하는 카드 (값은 무드 수정에서 바꾼다) */
private fun ControlCard(card: ControlCardUi, targets: ControlTargets, accent: Color, modifier: Modifier = Modifier.width(CARD_WIDTH)) {
    val shape = RoundedCornerShape(fig(32))
    val sub = Color.White.copy(alpha = 0.64f)
    Box(
        modifier
            .height(CARD_HEIGHT)
            .clip(shape)
            .background(Color.White.copy(alpha = 0.10f))
            .border(1.dp, Color.White.copy(alpha = 0.08f), shape)
            .padding(start = fig(24), end = fig(20), top = fig(24), bottom = fig(14)),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            DetailIcon(card.kind, if (card.kind == DetailKind.AMBIENT) accent else Color.White, size = fig(32))
            Spacer(Modifier.width(fig(8)))
            Text(card.kind.label, style = text(14.sp, FontWeight.Normal, sub, lineHeight = 16.5.sp), maxLines = 1, modifier = Modifier.weight(1f))
        }
        // 시트·바람은 시안처럼 왼쪽 아래 아이콘 상자
        cardGlyph(card.kind, targets)?.let { glyph ->
            Box(
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(bottom = fig(26) - fig(14))
                    .size(fig(58))
                    .clip(RoundedCornerShape(fig(12)))
                    .background(Color.White.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                when (glyph) {
                    // 시안 아이콘(색 포함)
                    is CardGlyph.Res -> Image(painterResource(glyph.id), null, modifier = Modifier.size(fig(36)))
                    is CardGlyph.Tinted -> Icon(painterResource(glyph.id), null, tint = glyph.color, modifier = Modifier.size(fig(36)))
                    is CardGlyph.Vector -> Icon(painterResource(glyph.id), null, tint = Color.White, modifier = Modifier.size(fig(32)))
                }
            }
        }
        Row(Modifier.align(Alignment.BottomEnd), verticalAlignment = Alignment.Bottom) {
            Text(
                card.value,
                style = text(if (card.value.length > 3) 24.sp else 35.sp, FontWeight.Bold, Color.White, lineHeight = 47.sp),
            )
            card.unit?.let {
                Spacer(Modifier.width(fig(8)))
                Text(it, style = text(15.sp, FontWeight.Normal, sub, lineHeight = 20.sp), modifier = Modifier.padding(bottom = fig(12)))
            }
        }
    }
}

private sealed interface CardGlyph {
    data class Res(@DrawableRes val id: Int) : CardGlyph
    /** 무드 수정과 같은 단색 시안 아이콘 ([color]로 칠한다) */
    data class Tinted(@DrawableRes val id: Int, val color: Color) : CardGlyph
    /** Material 아이콘 (흰색으로 칠한다) */
    data class Vector(@DrawableRes val id: Int) : CardGlyph
}

/**
 * 시트 열선·통풍, 바람 발·얼굴은 무드 수정과 같은 아이콘(열선 빨강, 통풍 파랑, 바람 흰색)을 쓴다.
 * 무드 수정에 없는 방향(얼굴+발 등)과 꺼짐은 기존 아이콘.
 */
private fun cardGlyph(kind: DetailKind, t: ControlTargets): CardGlyph? = when (kind) {
    DetailKind.SEAT -> when {
        t.seatVenting -> CardGlyph.Tinted(R.drawable.ic_opt_seat_vent, VENT_BLUE)
        (t.seatLevel ?: 0) > 0 -> CardGlyph.Tinted(R.drawable.ic_opt_seat_heat, HEAT_RED)
        else -> CardGlyph.Vector(R.drawable.ic_m_airline_seat_recline_extra)
    }
    DetailKind.WIND -> when (t.fanDirection) {
        FanDirection.FACE_AND_FLOOR -> CardGlyph.Res(R.drawable.ic_airflow_face_feet)
        FanDirection.FACE -> CardGlyph.Tinted(R.drawable.ic_opt_air_face, Color.White)
        FanDirection.FLOOR -> CardGlyph.Tinted(R.drawable.ic_opt_air_feet, Color.White)
        null -> CardGlyph.Vector(R.drawable.ic_m_air)
        else -> CardGlyph.Vector(R.drawable.ic_m_window)
    }
    else -> null
}

@Composable
private fun EditMoodButton(enabled: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(fig(24))
    Row(
        Modifier
            .fillMaxWidth()
            .height(fig(84))
            .alpha(if (enabled) 1f else 0.4f)
            .clip(shape)
            .background(MoodriveColors.Scrim.copy(alpha = 0.64f))
            .border(1.dp, Color.White.copy(alpha = 0.10f), shape)
            .clickable(enabled = enabled, onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painterResource(R.drawable.ic_edit), null, tint = Color.White, modifier = Modifier.size(fig(40)))
        Spacer(Modifier.width(fig(12)))
        Text("무드 수정", style = text(16.5.sp, FontWeight.Bold, Color.White, lineHeight = 21.sp))
    }
}

@Composable
private fun EndButton(onClick: () -> Unit) {
    val red = MoodriveColors.Danger
    Row(
        Modifier
            .fillMaxWidth()
            .height(fig(84))
            .clip(RoundedCornerShape(fig(24)))
            .background(red.copy(alpha = 0.18f))
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painterResource(R.drawable.ic_stop), null, tint = red, modifier = Modifier.size(fig(40)))
        Spacer(Modifier.width(fig(12)))
        Text("Scene 종료", style = text(16.5.sp, FontWeight.Bold, red, lineHeight = 21.sp))
    }
}

@Composable
private fun SaveToLibraryButton(saved: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(fig(24))
    Row(
        Modifier
            .fillMaxWidth()
            .height(fig(84))
            .alpha(if (saved) 0.5f else 1f)
            .clip(shape)
            .background(MoodriveColors.Scrim.copy(alpha = 0.64f))
            .border(1.dp, Color.White.copy(alpha = 0.10f), shape)
            .clickable(enabled = !saved, onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(painterResource(R.drawable.ic_explore_download), null, modifier = Modifier.size(fig(40)))
        Spacer(Modifier.width(fig(12)))
        Text(if (saved) "내 Mood에 저장됨" else "내 Mood에 저장", style = text(16.5.sp, FontWeight.Bold, Color.White, lineHeight = 21.sp))
    }
}

@Composable
private fun ApplyPreviewButton(onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(fig(84))
            .clip(RoundedCornerShape(fig(24)))
            .background(Color.White)
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(painterResource(R.drawable.ic_explore_play), null, modifier = Modifier.size(fig(40)))
        Spacer(Modifier.width(fig(12)))
        Text("Mood 적용", style = text(16.5.sp, FontWeight.Bold, Color.Black, lineHeight = 21.sp))
    }
}

/** 시안 Overlay 색 */
private val OVERLAY = Color(0xFF0A0B0E)

private val CARD_WIDTH = (235.25f * 467f / 793f).dp
private val CARD_HEIGHT = fig(204)

/** 분할 화면에서 앱(세로형 실행 화면) 폭과 전환 시간 */
private const val SPLIT_APP = 840
private const val SPLIT_MS = 380
private val CARD_GAP = fig(20)
private val CARD_START = fig(60)
private val SIDE_PADDING = fig(60)
private val BOTTOM_PADDING = fig(40)

// ───────────────────────── 프리뷰 ─────────────────────────

private val previewState = run {
    val targets = ControlTargets(brightnessPercent = 50, colorArgb = 0xFF8000FF.toInt(), seatHeat = 2, fanSpeed = 3, fanDirection = FanDirection.FLOOR, steeringHeat = 1)
    SceneControlUiState(
        title = "Cozy Fireplace",
        tags = "Vintage · Warm",
        paletteArgb = listOf(0xFF8E2A22.toInt(), 0xFFD9A441.toInt(), 0xFF2B1A14.toInt()),
        isFavorite = true,
        driveMode = DriveMode.DRIVING,
        playlists = listOf(
            Playlist("분위기 깡패 가을 플레이리스트🍂", "플레이리스트", url = ""),
            Playlist("🍂 가을 냄새가 물씬 풍기는 가을", "플레이리스트", url = ""),
        ),
        moreQuery = "가을 분위기",
        moreUrl = "",
        cards = SceneControlFormatter.cards(targets),
        targets = targets,
    )
}

@Preview(widthDp = 1000, heightDp = 696)
@Composable
private fun SceneControlPreview() {
    MoodriveTheme { SceneControlScreen(previewState, SceneControlActions()) }
}

@Preview(widthDp = 1000, heightDp = 696)
@Composable
private fun SceneControlExplorePreview() {
    MoodriveTheme { SceneControlScreen(previewState.copy(isPreview = true), SceneControlActions()) }
}
