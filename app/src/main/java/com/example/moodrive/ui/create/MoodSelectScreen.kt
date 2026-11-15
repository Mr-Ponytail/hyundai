package com.example.moodrive.ui.create

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.moodrive.R
import com.example.moodrive.domain.model.DriveMode
import com.example.moodrive.ui.common.ColorDot
import com.example.moodrive.ui.common.FlowButton
import com.example.moodrive.ui.common.SettingChip
import com.example.moodrive.ui.common.SettingChipIcon
import com.example.moodrive.ui.common.White
import com.example.moodrive.ui.common.White64
import com.example.moodrive.ui.common.White84
import com.example.moodrive.ui.common.px
import com.example.moodrive.ui.common.rememberSpinAngle
import com.example.moodrive.ui.common.style
import com.example.moodrive.ui.theme.MoodriveColors
import com.example.moodrive.ui.theme.MoodriveTheme
import kotlinx.coroutines.delay

/*
 * 새 무드 만들기 - 2단계 "무드 고르기".
 *  - 만드는 중: Figma 435:46250 "3 · 활기찬 드라이브" (1순위 완성, 나머지 만드는 중/대기 중)
 *  - 완성: Figma 435:46592 "4 · 활기찬 드라이브" (1순위 크게, 2~5순위 카드, 하단 버튼)
 */

/** 시트·공조·핸들 설정 요약 (히어로 카드 하단 칩) */
data class MoodSettingsUi(
    val lightArgb: Int,
    val brightness: Int,
    val seat: String,
    val wind: String,
    val wheel: String?,
    /** 앰비언트 라이트 색 이름 ("네온 핑크") */
    val lightName: String,
    /** 함께 틀 플레이리스트 */
    val playlist: String,
    /** 하단 앰비언트 색 (상단은 [lightArgb]) */
    val subArgb: Int,
)

data class MoodCandidateUi(
    val rank: Int,
    val name: String,
    /** "Vivid · Energetic · Sunset City" */
    val keywords: String,
    val matchPercent: Int,
    @DrawableRes val imageRes: Int,
    val settings: MoodSettingsUi,
)

data class MoodSelectActions(
    val onBack: () -> Unit = {},
    val onRetryVoice: () -> Unit = {},
    /** 말로 다듬기: 지금 요청에 덧붙일 조건 문장 */
    val onRefine: (String) -> Unit = {},
    val onMore: () -> Unit = {},
    val onConfirm: (MoodCandidateUi) -> Unit = {},
)

/** 무드 생성 API가 붙기 전까지 쓰는 시안 결과 ("활기찬 드라이브를 하고 싶어") */
internal val SAMPLE_CANDIDATES = listOf(
    MoodCandidateUi(1, "City Pulse Drive", "Vivid · Energetic · Sunset City", 96, R.drawable.mood_midnight_rain_cafe,
        MoodSettingsUi(0xFFFF3DA5.toInt(), 60, "시트 열선 1단", "2단 · 얼굴", "핸들 열선", "네온 핑크", "Upbeat Drive", 0xFF2CC9F0.toInt())),
    MoodCandidateUi(2, "Night City Rush", "Bold · Urban · Night Drive", 91, R.drawable.mood_night_city_rush,
        MoodSettingsUi(0xFF8B6CFF.toInt(), 70, "시트 통풍 1단", "3단 · 얼굴", null, "일렉트릭 퍼플", "Night Run", 0xFFFF6FB5.toInt())),
    MoodCandidateUi(3, "Golden Breeze", "Warm · Free · Autumn Road", 87, R.drawable.mood_golden_breeze,
        MoodSettingsUi(0xFFFFA53D.toInt(), 55, "시트 열선 1단", "1단 · 발", "핸들 열선", "선셋 오렌지", "Golden Hour", 0xFFFFE2B8.toInt())),
    MoodCandidateUi(4, "Lakeside Weekend", "Fresh · Relaxed · Lakeside", 78, R.drawable.mood_lakeside_weekend,
        MoodSettingsUi(0xFF4FC3F7.toInt(), 45, "시트 통풍 1단", "2단 · 얼굴·발", null, "레이크 블루", "Weekend Chill", 0xFF3D8BFF.toInt())),
    MoodCandidateUi(5, "Green Road Trip", "Bright · Open · Road Trip", 71, R.drawable.mood_green_road_trip,
        MoodSettingsUi(0xFF6FDC6F.toInt(), 50, "시트 통풍 2단", "2단 · 얼굴", null, "포레스트 그린", "Open Road", 0xFFFFE2B8.toInt())),
)

private enum class CardStatus { WAITING, MAKING, DONE }

/** 생성 단계: 0 = 2·4순위 만드는 중, 1 = 3·5순위 만드는 중, 2 = 모두 완성 */
private const val PHASE_READY = 2
private const val PHASE_MS = 1800L

private fun statusOf(rank: Int, phase: Int): CardStatus = when {
    rank == 1 || phase >= PHASE_READY -> CardStatus.DONE
    // 시안: 왼쪽 열(2·4순위)이 먼저 만들어지고 오른쪽 열(3·5순위)은 대기
    rank % 2 == 0 -> if (phase == 0) CardStatus.MAKING else CardStatus.DONE
    else -> if (phase == 0) CardStatus.WAITING else CardStatus.MAKING
}

private val PinkGlow = Color(0xFFFF7EB6)
private val SelectedBorder = Brush.linearGradient(
    0f to Color(0xFFFF7EB6),
    0.5f to Color(0xFFA98BFF),
    1f to Color(0xFF5DC8F0),
)

@Composable
fun MoodSelectScreen(
    driveMode: DriveMode,
    request: String,
    actions: MoodSelectActions,
    candidates: List<MoodCandidateUi> = SAMPLE_CANDIDATES,
    /** 3단계에서 돌아올 때: 이미 만든 후보를 다시 만들지 않고 고르던 무드를 유지한다 */
    startReady: Boolean = false,
    initialRank: Int = 1,
) {
    BackHandler(onBack = actions.onBack)
    var phase by rememberSaveable { mutableIntStateOf(if (startReady) PHASE_READY else 0) }
    var selectedRank by rememberSaveable { mutableIntStateOf(initialRank) }
    var refining by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        while (phase < PHASE_READY) {
            delay(PHASE_MS)
            phase++
        }
    }
    MoodSelectContent(
        driveMode = driveMode,
        request = request,
        candidates = candidates,
        phase = phase,
        selectedRank = selectedRank,
        onSelect = { selectedRank = it },
        onRefineClick = { refining = true },
        refining = refining,
        onRefineDismiss = { refining = false },
        actions = actions,
    )
}

@Composable
private fun MoodSelectContent(
    driveMode: DriveMode,
    request: String,
    candidates: List<MoodCandidateUi>,
    phase: Int,
    selectedRank: Int,
    onSelect: (Int) -> Unit,
    actions: MoodSelectActions,
    onRefineClick: () -> Unit = {},
    refining: Boolean = false,
    onRefineDismiss: () -> Unit = {},
) {
    val ready = phase >= PHASE_READY
    val hero = candidates.firstOrNull { it.rank == selectedRank } ?: candidates.first()
    val others = candidates.filter { it.rank != hero.rank }.sortedBy { it.rank }

    CreateFlowScaffold(
        driveMode = driveMode,
        step = 2,
        onBack = actions.onBack,
        glow = if (ready) TopGlow(PinkGlow, 0.1f, x = 327, width = 1047) else TopGlow.Default,
    ) {
        Column(Modifier.padding(start = px(60), end = px(60), top = px(150))) {
            Text("이런 무드는 어떠세요?", style = style(48, FontWeight.Bold, White, lineHeight = 64))
            Spacer(Modifier.height(px(22)))
            RequestBar(request, onRefine = onRefineClick)
            Spacer(Modifier.height(px(24)))
            Row(Modifier.height(px(624))) {
                HeroCard(hero, ready, Modifier.width(px(900)).fillMaxHeight())
                Spacer(Modifier.width(px(46)))
                Column(verticalArrangement = Arrangement.spacedBy(px(24))) {
                    others.chunked(2).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(px(34))) {
                            row.forEach { c ->
                                MoodCard(c, statusOf(c.rank, phase), enabled = ready, onClick = { onSelect(c.rank) })
                            }
                        }
                    }
                }
            }
        }

        AnimatedContent(
            targetState = ready,
            transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) },
            modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(start = px(60), end = px(60), bottom = px(52)),
            label = "bottomBar",
        ) { done ->
            Box(Modifier.fillMaxWidth().height(px(100))) {
                if (done) {
                    RetryVoiceButton(actions.onRetryVoice, Modifier.align(Alignment.BottomStart))
                    Row(Modifier.align(Alignment.BottomEnd), horizontalArrangement = Arrangement.spacedBy(px(30))) {
                        FlowButton(
                            "더 많은 무드 찾아보기", style(28, FontWeight.Bold, White84, lineHeight = 36), White.copy(alpha = 0.05f), px(24),
                            Modifier.width(px(380)), icon = R.drawable.ic_create_refresh, onClick = actions.onMore,
                        )
                        FlowButton(
                            "이 무드로 Scene 만들기", style(30, FontWeight.Bold, MoodriveColors.Scrim, lineHeight = 38), MoodriveColors.ButtonLight, px(24),
                            Modifier.width(px(480)),
                        ) { actions.onConfirm(hero) }
                    }
                } else {
                    FlowButton(
                        "취소", style(28, FontWeight.Bold, White84, lineHeight = 36), White.copy(alpha = 0.05f), px(24),
                        Modifier.align(Alignment.BottomEnd).width(px(300)), icon = R.drawable.ic_create_close, onClick = actions.onBack,
                    )
                }
            }
        }

        // 말로 다듬기: 아래에서 올라오는 듣는 중 시트
        AnimatedVisibility(
            visible = refining,
            enter = fadeIn(tween(200)) + slideInVertically(tween(320)) { it / 3 },
            exit = fadeOut(tween(180)) + slideOutVertically(tween(260)) { it / 3 },
        ) {
            Box(Modifier.fillMaxSize()) {
                RefineVoiceSheet(
                    onDismiss = onRefineDismiss,
                    onDone = {
                        onRefineDismiss()
                        actions.onRefine(it)
                    },
                )
            }
        }
    }
}

// ───────────────────────── 요청 바 ─────────────────────────

/** Figma 722:23493 "Request bar": 요청 무드 라벨 · 요청 문장 · 오른쪽 "말로 다듬기" 버튼 */
@Composable
private fun RequestBar(request: String, onRefine: () -> Unit) {
    val shape = RoundedCornerShape(px(24))
    Row(
        Modifier
            .fillMaxWidth()
            .height(px(88))
            .clip(shape)
            .background(White.copy(alpha = 0.05f))
            .border(1.dp, White.copy(alpha = 0.08f), shape)
            .padding(start = px(30), end = px(24)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("요청 무드", style = style(26, FontWeight.Normal, White.copy(alpha = 0.32f), lineHeight = 34), maxLines = 1)
        Spacer(Modifier.width(px(27)))
        Text(
            "“$request”",
            style = style(26, FontWeight.Bold, White64, lineHeight = 34),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(px(24)))
        Row(
            Modifier
                .height(px(56))
                .clip(RoundedCornerShape(px(20)))
                .background(White.copy(alpha = 0.08f))
                .clickable(onClick = onRefine)
                .padding(horizontal = px(20)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(painterResource(R.drawable.ic_create_mic_small), contentDescription = null, modifier = Modifier.size(px(28)))
            Spacer(Modifier.width(px(8)))
            Text("말로 다듬기", style = style(22, FontWeight.SemiBold, White, lineHeight = 26), maxLines = 1)
        }
    }
}

// ───────────────────────── 1순위(선택) 카드 ─────────────────────────

@Composable
private fun HeroCard(mood: MoodCandidateUi, ready: Boolean, modifier: Modifier) {
    val shape = RoundedCornerShape(px(32))
    val frame = if (ready) {
        modifier
            .shadow(px(40), shape, ambientColor = PinkGlow, spotColor = PinkGlow)
            .clip(shape)
            .border(2.dp, SelectedBorder, shape)
    } else {
        modifier.clip(shape)
    }
    Box(frame) {
        Crossfade(mood.imageRes, animationSpec = tween(400), label = "heroImage") { res ->
            Image(painterResource(res), contentDescription = mood.name, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        }
        // 왼쪽 647px 영역 아래쪽을 어둡게 (글자 가독성). 시안처럼 경계선이 보이지 않게 오른쪽 끝은 흐려 준다
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                .drawWithContent {
                    drawContent()
                    drawRect(
                        Brush.horizontalGradient(0f to Color.Black, 0.5f to Color.Black, 0.72f to Color.Transparent),
                        blendMode = BlendMode.DstIn,
                    )
                }
                .background(Brush.verticalGradient(listOf(Color(0x001A0718), Color(0xEB291C28)))),
        )

        if (ready) {
            RankMatch(mood.rank, "매칭 ${mood.matchPercent}%", mood.matchPercent, Modifier.offset(px(30), px(28)))
        } else {
            DoneBadge(mood.rank, Modifier.offset(px(30), px(24)))
        }

        Column(Modifier.offset(px(30), px(373))) {
            Text(mood.name, style = style(60, FontWeight.Bold, White, lineHeight = 80), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(px(2)))
            Text(mood.keywords, style = style(26, FontWeight.Normal, White84, lineHeight = 34), maxLines = 1)
            Spacer(Modifier.height(px(20)))
            SettingsChips(mood.settings)
        }
    }
}

@Composable
private fun DoneBadge(rank: Int, modifier: Modifier) {
    Row(
        modifier
            .height(px(44))
            .clip(RoundedCornerShape(px(22)))
            .background(White)
            .padding(horizontal = px(20.5f)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(painterResource(R.drawable.ic_create_badge_check), contentDescription = null, modifier = Modifier.size(px(22)))
        Spacer(Modifier.width(px(8)))
        Text("$rank · 완성", style = style(20, FontWeight.Normal, MoodriveColors.Scrim, lineHeight = 24))
    }
}

@Composable
private fun SettingsChips(s: MoodSettingsUi) {
    Row(horizontalArrangement = Arrangement.spacedBy(px(10))) {
        SettingChip("${s.brightness}%", maxLines = 1) { ColorDot(Color(s.lightArgb)) }
        SettingChip(s.seat, maxLines = 1) { SettingChipIcon(R.drawable.ic_create_seat) }
        SettingChip(s.wind, maxLines = 1) { SettingChipIcon(R.drawable.ic_create_wind) }
        s.wheel?.let { SettingChip(it, maxLines = 1) { SettingChipIcon(R.drawable.ic_create_wheel) } }
    }
}

// ───────────────────────── 2~5순위 카드 ─────────────────────────

@Composable
private fun MoodCard(mood: MoodCandidateUi, status: CardStatus, enabled: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(px(24))
    Crossfade(status == CardStatus.DONE, animationSpec = tween(500), label = "card${mood.rank}") { done ->
        if (done) {
            Box(Modifier.size(px(300)).clip(shape).clickable(enabled = enabled, onClick = onClick)) {
                Image(painterResource(mood.imageRes), contentDescription = mood.name, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                Box(
                    Modifier
                        .offset(y = px(80))
                        .size(px(300), px(220))
                        .background(Brush.verticalGradient(listOf(Color(0x00111115), Color(0xEB111115)))),
                )
                RankMatch(mood.rank, "${mood.matchPercent}%", mood.matchPercent, Modifier.offset(px(16), px(16)))
                Text(
                    mood.name,
                    style = style(26, FontWeight.Bold, White, lineHeight = 34),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.offset(px(20), px(202)).padding(end = px(40)),
                )
                Text(
                    mood.keywords,
                    style = style(20, FontWeight.Normal, White.copy(alpha = 0.72f), lineHeight = 24),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.offset(px(20), px(244)).padding(end = px(40)),
                )
            }
        } else {
            PendingCard(making = status == CardStatus.MAKING, shape = shape)
        }
    }
}

@Composable
private fun PendingCard(making: Boolean, shape: RoundedCornerShape) {
    Box(
        Modifier
            .size(px(300))
            .clip(shape)
            .background(MoodriveColors.Panel)
            .border(1.dp, White.copy(alpha = 0.1f), shape),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(top = px(88)),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spinner(spinning = making)
            Spacer(Modifier.height(px(24.5f)))
            Text(
                if (making) "만드는 중…" else "대기 중",
                style = style(24, FontWeight.Normal, White.copy(alpha = if (making) 0.84f else 0.64f), lineHeight = 28, align = TextAlign.Center),
            )
        }
    }
}

/** 72px 링. 대기 중이면 트랙만, 만드는 중이면 흰 호가 돈다 */
@Composable
private fun Spinner(spinning: Boolean) {
    val angle by rememberSpinAngle("spinner")
    Canvas(Modifier.size(px(72))) {
        val stroke = size.minDimension * 0.1f
        val inset = stroke / 2
        val arcSize = Size(size.width - stroke, size.height - stroke)
        val topLeft = Offset(inset, inset)
        drawArc(White.copy(alpha = 0.1f), 0f, 360f, false, topLeft, arcSize, style = Stroke(stroke))
        if (spinning) {
            drawArc(White, angle - 90f, 150f, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
        }
    }
}

// ───────────────────────── 순위·매칭 배지 ─────────────────────────

@Composable
private fun RankMatch(rank: Int, label: String, match: Int, modifier: Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(px(44)).clip(CircleShape).background(White), contentAlignment = Alignment.Center) {
            Text("$rank", style = style(24, FontWeight.Normal, MoodriveColors.Scrim, lineHeight = 28, align = TextAlign.Center))
        }
        Spacer(Modifier.width(px(8)))
        val shape = RoundedCornerShape(px(22))
        Row(
            Modifier
                .height(px(44))
                .clip(shape)
                .background(MoodriveColors.Scrim.copy(alpha = 0.5f))
                .border(1.dp, White.copy(alpha = 0.18f), shape)
                .padding(horizontal = px(15.5f)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(painterResource(R.drawable.ic_create_spark), contentDescription = null, modifier = Modifier.size(px(18)))
            Spacer(Modifier.width(px(8)))
            Text(label, style = style(20, FontWeight.Normal, White, lineHeight = 24))
            Spacer(Modifier.width(px(8)))
            // 매칭 정도 막대 (36×6)
            Box(Modifier.size(px(36), px(6)).clip(CircleShape).background(White.copy(alpha = 0.22f))) {
                Box(Modifier.size(px(36f * match / 100f), px(6)).clip(CircleShape).background(White))
            }
        }
    }
}

// ───────────────────────── 하단 버튼 ─────────────────────────

@Composable
private fun RetryVoiceButton(onClick: () -> Unit, modifier: Modifier) {
    val shape = RoundedCornerShape(px(16))
    Row(
        modifier
            .height(px(64))
            .clip(shape)
            .background(White.copy(alpha = 0.06f))
            .border(1.dp, White.copy(alpha = 0.12f), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = px(23.5f)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(painterResource(R.drawable.ic_create_mic_outline), contentDescription = null, modifier = Modifier.size(px(26)))
        Spacer(Modifier.width(px(10)))
        Text("다시 말하기", style = style(24, FontWeight.Normal, White, lineHeight = 28))
    }
}

@Preview(widthDp = 1000, heightDp = 696)
@Composable
private fun MoodSelectLoadingPreview() {
    MoodriveTheme {
        MoodSelectContent(DriveMode.PARKED, "활기찬 드라이브를 하고 싶어", SAMPLE_CANDIDATES, phase = 0, selectedRank = 1, onSelect = {}, actions = MoodSelectActions())
    }
}

@Preview(widthDp = 1000, heightDp = 696)
@Composable
private fun MoodSelectReadyPreview() {
    MoodriveTheme {
        MoodSelectContent(DriveMode.PARKED, "활기찬 드라이브를 하고 싶어", SAMPLE_CANDIDATES, phase = PHASE_READY, selectedRank = 1, onSelect = {}, actions = MoodSelectActions())
    }
}
