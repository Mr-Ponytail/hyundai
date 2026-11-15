package com.example.moodrive.ui.create

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.example.moodrive.R
import com.example.moodrive.domain.model.DriveMode
import com.example.moodrive.domain.model.FanDirection
import com.example.moodrive.ui.common.FlowButton
import com.example.moodrive.ui.common.White
import com.example.moodrive.ui.common.White64
import com.example.moodrive.ui.common.consumeTaps
import com.example.moodrive.ui.common.px
import com.example.moodrive.ui.common.style
import com.example.moodrive.ui.mood.EditableName
import com.example.moodrive.ui.mood.MoodDraft
import com.example.moodrive.ui.mood.MoodDraftSaver
import com.example.moodrive.ui.mood.MoodEditSections
import com.example.moodrive.ui.mood.SeatMode
import com.example.moodrive.ui.theme.MoodriveColors
import com.example.moodrive.ui.theme.MoodriveTheme

/*
 * 새 무드 만들기 - 4단계 "완성". Figma 435:46416 "6 · 활기찬 드라이브".
 * 왼쪽: 만든 설정을 바로 고치는 Edit 패널 (실행 중 무드 수정과 같은 섹션)
 * 오른쪽: 무드 이미지 위에 이름·키워드, 실행해보기 / 저장
 */

data class MoodCompleteActions(
    val onBack: () -> Unit = {},
    val onTryOut: (MoodDraft) -> Unit = {},
    val onSave: (MoodDraft) -> Unit = {},
    val onEyedropper: () -> Unit = {},
)

/** 무드 후보의 설정을 편집 가능한 값으로 바꾼다 */
internal fun MoodCandidateUi.toDraft(): MoodDraft {
    val s = settings
    fun level(text: String, range: IntRange = MoodDraft.LEVELS) = Regex("(\\d)단").find(text)?.groupValues?.get(1)?.toInt()?.coerceIn(range) ?: 1
    return MoodDraft(
        name = name,
        lightOn = true,
        mainColor = s.lightArgb,
        subColor = s.subArgb,
        brightnessPercent = s.brightness,
        seatOn = true,
        seatMode = if ("통풍" in s.seat) SeatMode.VENT else SeatMode.HEAT,
        seatLevel = level(s.seat),
        windOn = true,
        windDirection = if ("얼굴" in s.wind && "발" !in s.wind) FanDirection.FACE else FanDirection.FLOOR,
        fanLevel = level(s.wind, MoodDraft.FAN_LEVELS),
        steeringOn = s.wheel != null,
    )
}

@Composable
fun MoodCompleteScreen(
    driveMode: DriveMode,
    mood: MoodCandidateUi,
    actions: MoodCompleteActions,
) {
    BackHandler(onBack = actions.onBack)
    var draft by rememberSaveable(mood.rank, stateSaver = MoodDraftSaver) { mutableStateOf(mood.toDraft()) }
    MoodCompleteContent(driveMode, mood, draft, onChange = { draft = it }, actions)
}

@Composable
private fun MoodCompleteContent(
    driveMode: DriveMode,
    mood: MoodCandidateUi,
    draft: MoodDraft,
    onChange: (MoodDraft) -> Unit,
    actions: MoodCompleteActions,
) {
    CreateFlowScaffold(
        driveMode = driveMode,
        step = 4,
        onBack = actions.onBack,
        // 이 화면은 상단 글로우 대신 조명색 블러 두 개를 쓴다
        glow = null,
        background = {
            Backdrop(mood, draft)
            PreviewSide(mood, draft, onChange, actions)
            EditPanel(draft, onChange, actions.onEyedropper)
            // 왼쪽 패널: 위로 스크롤되는 내용을 헤더 아래로 덮는 그라데이션.
            // 시안(288px, 37%까지 불투명)보다 길게 잡아 스크롤된 섹션이 "Edit" 제목과 겹치지 않게 한다
            Box(
                Modifier
                    .width(px(811))
                    .height(px(260))
                    .background(Brush.verticalGradient(0f to MoodriveColors.Panel, 0.8f to MoodriveColors.Panel, 1f to MoodriveColors.Panel.copy(alpha = 0f))),
            )
            // 오른쪽 이미지: 화면 맨 위까지 보이되 위로 갈수록 서서히 흐려진다
            Box(
                Modifier
                    .padding(start = px(811))
                    .fillMaxWidth()
                    .height(px(260))
                    .background(Brush.verticalGradient(0f to MoodriveColors.Panel, 1f to MoodriveColors.Panel.copy(alpha = 0f))),
            )
            Text("Edit", style = style(40, FontWeight.Bold, White, 52), modifier = Modifier.offset(px(60), px(150)))
        },
    ) {}
}

/** 오른쪽 무드 이미지 + 어둡게 + 고른 앰비언트 색 블러 (색을 바꾸면 바로 따라간다) */
@Composable
private fun Backdrop(mood: MoodCandidateUi, draft: MoodDraft) {
    Image(
        painterResource(mood.imageRes),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier.offset(x = px(281)).size(px(1698), px(1183)),
    )
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(MoodriveColors.CardScrim.copy(alpha = 0.55f), MoodriveColors.CardScrim.copy(alpha = 0.92f)))))
    val main by animateColorAsState(Color(draft.mainColor), tween(400), label = "main")
    val sub by animateColorAsState(Color(draft.subColor), tween(400), label = "sub")
    val glowAlpha = if (draft.lightOn) 0.3f else 0f
    RadialGlow(main, glowAlpha, Modifier.offset(px(620), px(-159)).size(px(847)))
    RadialGlow(sub, glowAlpha, Modifier.offset(px(1223), px(67)).size(px(847)))
}

@Composable
private fun PreviewSide(mood: MoodCandidateUi, draft: MoodDraft, onChange: (MoodDraft) -> Unit, actions: MoodCompleteActions) {
    Box(Modifier.fillMaxSize().padding(start = px(869), end = px(60))) {
        Column(Modifier.padding(top = px(248))) {
            NameRow(draft.name) { onChange(draft.copy(name = it)) }
            Spacer(Modifier.height(px(18)))
            Text(mood.keywords, style = style(26, FontWeight.Normal, White64, 34))
        }
        Column(Modifier.align(Alignment.BottomStart).padding(bottom = px(40))) {
            CompleteButton("▶  실행해보기", primary = false) { actions.onTryOut(draft) }
            Spacer(Modifier.height(px(20)))
            CompleteButton("저장", primary = true) { actions.onSave(draft) }
        }
    }
}

/** 무드 이름 + 이름 수정 버튼. 누르면 그 자리에서 고친다 */
@Composable
private fun NameRow(name: String, onRename: (String) -> Unit) {
    var editing by rememberSaveable { mutableStateOf(false) }
    EditableName(
        name = name,
        onRename = onRename,
        editing = editing,
        onEditingChange = { editing = it },
        textStyle = style(60, FontWeight.Bold, White, 80),
        height = px(80),
        gap = px(14),
        buttonSize = px(56),
        buttonCorner = px(12),
        buttonRestAlpha = 0f,
    ) { description ->
        Image(painterResource(R.drawable.ic_create_rename), contentDescription = description, modifier = Modifier.size(px(56)))
    }
}

@Composable
private fun CompleteButton(label: String, primary: Boolean, onClick: () -> Unit) {
    FlowButton(
        label = label,
        textStyle = style(28, FontWeight.Bold, if (primary) MoodriveColors.Scrim else White, 36, TextAlign.Center),
        background = if (primary) MoodriveColors.ButtonLight else White.copy(alpha = 0.1f),
        cornerRadius = px(32),
        modifier = Modifier.fillMaxWidth(),
        border = if (primary) null else BorderStroke(px(2), White.copy(alpha = 0.2f)),
        onClick = onClick,
    )
}

/** 왼쪽 Edit 패널. 섹션은 스크롤되고 아래쪽은 그라데이션으로 흐려진다 */
@Composable
private fun EditPanel(draft: MoodDraft, onChange: (MoodDraft) -> Unit, onEyedropper: () -> Unit) {
    Box(
        Modifier
            .width(px(811))
            .fillMaxHeight()
            .background(MoodriveColors.Panel)
            // 패널 빈 곳을 눌러도 뒤로 전달되지 않게
            .consumeTaps(),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = px(60), end = px(58), top = px(250), bottom = px(120)),
        ) {
            MoodEditSections(draft, onChange, onEyedropper)
        }
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(px(200))
                .background(Brush.verticalGradient(listOf(MoodriveColors.Panel.copy(alpha = 0f), MoodriveColors.Panel))),
        )
    }
}

@Preview(widthDp = 1000, heightDp = 696)
@Composable
private fun MoodCompletePreview() {
    MoodriveTheme {
        val mood = SAMPLE_CANDIDATES.first()
        MoodCompleteContent(DriveMode.PARKED, mood, mood.toDraft(), onChange = {}, actions = MoodCompleteActions())
    }
}
