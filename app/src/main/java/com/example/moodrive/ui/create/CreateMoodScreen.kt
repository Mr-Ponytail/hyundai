package com.example.moodrive.ui.create

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.moodrive.R
import com.example.moodrive.domain.model.DriveMode
import com.example.moodrive.ui.common.DriveStatusPill
import com.example.moodrive.ui.common.White
import com.example.moodrive.ui.common.White64
import com.example.moodrive.ui.common.White84
import com.example.moodrive.ui.common.px
import com.example.moodrive.ui.common.style
import com.example.moodrive.ui.theme.MoodriveColors
import com.example.moodrive.ui.theme.MoodriveTheme

/*
 * 새 무드 만들기 - 1단계 "설명하기" (Figma 435:46077 "1 · 활기찬 드라이브").
 * 시안 프레임 1700×1184px을 DesignScale 캔버스(1000×696dp)에 맞추려고 모든 수치를 px / 1.7 로 옮긴다.
 */

data class RecentRequestUi(
    val prompt: String,
    /** "Lavender Dusk · 어제" */
    val caption: String,
    @DrawableRes val thumbRes: Int,
)

data class CreateMoodActions(
    val onBack: () -> Unit = {},
    val onVoice: () -> Unit = {},
    /** 텍스트 입력·예시 칩·최근 요청으로 설명을 보냈을 때 */
    val onSubmit: (String) -> Unit = {},
)

/** 시안 예시. 새로고침 버튼을 누르면 다음 묶음을 보여준다 */
private val EXAMPLE_PROMPT_SETS = listOf(
    listOf("활기찬 드라이브", "새벽 해안도로 드라이브", "캠핑장 모닥불 옆", "퇴근길, 차분하게 쉬고 싶어"),
    listOf("비 오는 밤 재즈바처럼", "벚꽃길 봄 소풍", "한여름 바닷가 오후", "눈 내리는 산장"),
)

/** 최근 요청 기록 저장소가 생기기 전까지 쓰는 시안 데이터 */
private val SAMPLE_RECENT_REQUESTS = listOf(
    RecentRequestUi("퇴근길에 조용히 쉬고 싶어", "Lavender Dusk · 어제", R.drawable.thumb_recent_lavender_dusk),
    RecentRequestUi("주말 아침 해안도로, 상쾌하게", "Frost Morning · 3일 전", R.drawable.thumb_recent_frost_morning),
    RecentRequestUi("비 오는 날 창가 자리처럼", "Rainy Window · 1주 전", R.drawable.thumb_recent_rainy_window),
)

@Composable
fun CreateMoodScreen(
    driveMode: DriveMode,
    actions: CreateMoodActions,
    recent: List<RecentRequestUi> = SAMPLE_RECENT_REQUESTS,
) {
    BackHandler(onBack = actions.onBack)
    var input by rememberSaveable { mutableStateOf("") }
    var exampleSet by rememberSaveable { mutableIntStateOf(0) }
    // 기획서 4-4: 주행 중에는 음성만 허용 (배지 "주행 중에도 가능")
    val canType = driveMode != DriveMode.DRIVING

    CreateFlowScaffold(driveMode = driveMode, step = 1, onBack = actions.onBack) {
        Column(Modifier.padding(start = px(60), end = px(60), top = px(188))) {
            Text("나의 새 무드 만들기", style = style(48, FontWeight.Bold, White, lineHeight = 64))
            Spacer(Modifier.height(px(42)))
            Text(
                "원하는 차 안 분위기를 말해 주세요. 무드 이미지와 차량 설정으로 만들어 드려요",
                style = style(26, FontWeight.Normal, White64, lineHeight = 34),
            )
            Spacer(Modifier.height(px(46)))
            Row(Modifier.height(px(624))) {
                VoiceCard(onClick = actions.onVoice, modifier = Modifier.width(px(900)).fillMaxHeight())
                Spacer(Modifier.width(px(20)))
                Column(Modifier.width(px(660))) {
                    TextInputCard(
                        value = input,
                        onValueChange = { input = it },
                        enabled = canType,
                        onSend = {
                            val text = input.trim()
                            if (text.isNotEmpty()) actions.onSubmit(text)
                        },
                    )
                    Spacer(Modifier.height(px(20)))
                    RecentCard(recent, onSelect = { actions.onSubmit(it.prompt) })
                }
            }
            Spacer(Modifier.height(px(66)))
            ExampleChips(
                prompts = EXAMPLE_PROMPT_SETS[exampleSet % EXAMPLE_PROMPT_SETS.size],
                onSelect = { input = it },
                onRefresh = { exampleSet = (exampleSet + 1) % EXAMPLE_PROMPT_SETS.size },
            )
        }
    }
}

// ───────────────────────── 만들기 흐름 공통 틀 ─────────────────────────

/** 상단 글로우 (시안 px). 무드 고르기 완성 화면은 분홍 글로우를 쓴다 */
internal data class TopGlow(val color: Color, val alpha: Float, val x: Int, val width: Int) {
    companion object {
        val Default = TopGlow(Color(0xFFB4CDFF), 0.2f, x = 150, width = 1400)
    }
}

/** 배경 그라데이션·상단 글로우·뒤로·단계 표시·주행 상태. 만들기 단계 화면들이 함께 쓴다 */
@Composable
internal fun CreateFlowScaffold(
    driveMode: DriveMode,
    step: Int,
    onBack: () -> Unit,
    /** null 이면 상단 글로우를 그리지 않는다 */
    glow: TopGlow? = TopGlow.Default,
    /** 그라데이션 위, 뒤로·단계·상태 아래에 깔 레이어 (무드 이미지, 완성 화면의 편집 패널 등) */
    background: @Composable BoxScope.() -> Unit = {},
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(0.03f to MoodriveColors.BackgroundTop, 0.88f to MoodriveColors.BackgroundBottom)),
    ) {
        background()
        // 상단 글로우 (프레임 위로 480px 넘어가는 타원)
        if (glow != null) {
            RadialGlow(glow.color, glow.alpha, Modifier.offset(px(glow.x), px(-480)).size(px(glow.width), px(900)))
        }

        Image(
            painterResource(R.drawable.ic_back),
            contentDescription = "뒤로",
            modifier = Modifier
                .offset(px(40), px(50))
                .size(px(64))
                .clip(CircleShape)
                .clickable(onClick = onBack),
        )
        StepIndicator(current = step, modifier = Modifier.align(Alignment.TopCenter).padding(top = px(60)))
        DriveStatusPill(driveMode, Modifier.align(Alignment.TopEnd).padding(top = px(60), end = px(60)))

        content()
    }
}

// ───────────────────────── 단계 표시 ─────────────────────────

private val STEPS = listOf("설명하기", "무드 고르기", "설정 만들기", "완성")

@Composable
private fun StepIndicator(current: Int, modifier: Modifier = Modifier) {
    Row(modifier.height(px(64)), verticalAlignment = Alignment.CenterVertically) {
        STEPS.forEachIndexed { i, label ->
            val step = i + 1
            if (i > 0) {
                Spacer(Modifier.width(px(12)))
                Box(Modifier.size(px(28), px(2)).background(White.copy(alpha = 0.2f)))
                Spacer(Modifier.width(px(12)))
            }
            val active = step == current
            val done = step < current
            // 현재 단계는 182px 고정 폭 알약 안에 가운데 정렬, 나머지는 좌우 15~16px 여백
            val pill = if (active) {
                Modifier.size(px(182), px(64)).clip(CircleShape).background(White.copy(alpha = 0.2f))
            } else {
                Modifier.height(px(52)).padding(horizontal = if (done) px(15) else px(16))
            }
            Row(pill, horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(px(32)).clip(CircleShape).background(if (active || done) White else White.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center,
                ) {
                    if (done) {
                        Image(painterResource(R.drawable.ic_create_step_done), contentDescription = "완료", modifier = Modifier.size(px(20)))
                    } else {
                        Text(
                            "$step",
                            style = style(20, FontWeight.Normal, if (active) MoodriveColors.Scrim else White64, lineHeight = 24, align = TextAlign.Center),
                        )
                    }
                }
                Spacer(Modifier.width(px(10)))
                Text(label, style = style(20, FontWeight.Normal, if (active || done) White else White64, lineHeight = 24))
            }
        }
    }
}

// ───────────────────────── 말로 설명하기 ─────────────────────────

@Composable
private fun VoiceCard(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(px(32))
    Box(
        modifier
            .clip(shape)
            .background(
                Brush.linearGradient(
                    0f to White.copy(alpha = 0.06f),
                    0.79f to Color(0xFF1C1C1C).copy(alpha = 0.06f),
                    start = Offset(0f, 0f),
                    end = Offset.Infinite,
                ),
            )
            .border(1.dp, White.copy(alpha = 0.2f), shape)
            .clickable(onClick = onClick),
    ) {
        // 카드 상단 절반에 걸친 900×900 글로우 (카드 밖은 잘림)
        RadialGlow(Color(0xFFBDD4FF), 0.3f, Modifier.offset(y = px(-186)).size(px(900)))

        Box(
            Modifier
                .offset(px(28), px(28))
                .height(px(44))
                .clip(RoundedCornerShape(px(22)))
                .background(White.copy(alpha = 0.1f))
                .padding(horizontal = px(17)),
            contentAlignment = Alignment.Center,
        ) {
            Text("주행 중에도 가능", style = style(20, FontWeight.Normal, White84, lineHeight = 24))
        }

        Column(
            Modifier.fillMaxWidth().padding(top = px(72)),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.size(px(300)).clip(CircleShape).background(White.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                Box(Modifier.size(px(240)).clip(CircleShape).background(White.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                    Box(Modifier.size(px(180)).clip(CircleShape).background(White), contentAlignment = Alignment.Center) {
                        Image(painterResource(R.drawable.ic_create_mic), contentDescription = "음성으로 설명하기", modifier = Modifier.size(px(84)))
                    }
                }
            }
            Spacer(Modifier.height(px(34)))
            Text("말로 설명하기", style = style(40, FontWeight.Bold, White, lineHeight = 52))
            Spacer(Modifier.height(px(13.5f)))
            Text("“활기찬 드라이브를 하고 싶어”", style = style(26, FontWeight.Normal, White64, lineHeight = 34))
        }
    }
}

// ───────────────────────── 텍스트로 입력 ─────────────────────────

@Composable
private fun TextInputCard(value: String, onValueChange: (String) -> Unit, enabled: Boolean, onSend: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .height(px(198))
            .clip(RoundedCornerShape(px(32)))
            .background(MoodriveColors.Panel)
            .padding(start = px(36), top = px(24), end = px(36)),
    ) {
        Text("텍스트로 입력", style = style(30, FontWeight.Bold, White, lineHeight = 38))
        Spacer(Modifier.height(px(20)))
        Row(Modifier.alpha(if (enabled) 1f else 0.4f), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(px(480), px(78))
                    .clip(RoundedCornerShape(px(24)))
                    .background(White.copy(alpha = 0.1f))
                    .padding(horizontal = px(28)),
                contentAlignment = Alignment.CenterStart,
            ) {
                val textStyle = style(24, FontWeight.Normal, White, lineHeight = 28)
                if (value.isEmpty()) {
                    Text(if (enabled) "어떤 분위기를 원하세요?" else "주행 중에는 음성으로 말해 주세요", style = textStyle.copy(color = White64))
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    enabled = enabled,
                    singleLine = true,
                    textStyle = textStyle,
                    cursorBrush = SolidColor(White),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { onSend() }),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(Modifier.width(px(20)))
            Box(
                Modifier
                    .size(px(88), px(80))
                    .clip(RoundedCornerShape(px(24)))
                    .background(MoodriveColors.ButtonLight)
                    .clickable(enabled = enabled, onClick = onSend),
                contentAlignment = Alignment.Center,
            ) {
                Image(painterResource(R.drawable.ic_create_send), contentDescription = "보내기", modifier = Modifier.size(px(36)))
            }
        }
    }
}

// ───────────────────────── 최근 요청 ─────────────────────────

@Composable
private fun RecentCard(items: List<RecentRequestUi>, onSelect: (RecentRequestUi) -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .height(px(406))
            .clip(RoundedCornerShape(px(32)))
            .background(MoodriveColors.Panel)
            .padding(start = px(24), top = px(24), end = px(36)),
    ) {
        Text("최근 요청", style = style(30, FontWeight.Bold, White, lineHeight = 38), modifier = Modifier.padding(start = px(12)))
        Spacer(Modifier.height(px(20)))
        Column(verticalArrangement = Arrangement.spacedBy(px(20))) {
            items.take(3).forEach { item ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(px(84))
                        .clip(RoundedCornerShape(px(20)))
                        .clickable { onSelect(item) }
                        .padding(start = px(12)),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Image(
                        painterResource(item.thumbRes),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(px(60)).clip(RoundedCornerShape(px(12))),
                    )
                    Spacer(Modifier.width(px(16)))
                    Column(Modifier.weight(1f)) {
                        Text(item.prompt, style = style(24, FontWeight.Normal, White84, lineHeight = 28), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.height(px(6)))
                        Text(item.caption, style = style(20, FontWeight.Normal, White64, lineHeight = 24), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Image(painterResource(R.drawable.ic_create_chevron), contentDescription = null, modifier = Modifier.size(px(24)))
                }
            }
        }
    }
}

// ───────────────────────── 예시 칩 ─────────────────────────

@Composable
private fun ExampleChips(prompts: List<String>, onSelect: (String) -> Unit, onRefresh: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(px(16)), verticalAlignment = Alignment.CenterVertically) {
        prompts.forEach { prompt ->
            val shape = RoundedCornerShape(px(40))
            Row(
                Modifier
                    .height(px(80))
                    .clip(shape)
                    .background(White.copy(alpha = 0.05f))
                    .border(1.dp, White.copy(alpha = 0.1f), shape)
                    .clickable { onSelect(prompt) }
                    .padding(horizontal = px(28)),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(painterResource(R.drawable.ic_create_sparkle), contentDescription = null, modifier = Modifier.size(px(32)))
                Spacer(Modifier.width(px(12)))
                Text(prompt, style = style(28, FontWeight.Normal, White, lineHeight = 36), maxLines = 1)
            }
        }
        val shape = RoundedCornerShape(px(40))
        Box(
            Modifier
                .size(px(91), px(80))
                .clip(shape)
                .background(MoodriveColors.Scrim.copy(alpha = 0.2f))
                .border(1.dp, White.copy(alpha = 0.1f), shape)
                .clickable(onClick = onRefresh),
            contentAlignment = Alignment.Center,
        ) {
            Image(painterResource(R.drawable.ic_create_refresh), contentDescription = "다른 예시 보기", modifier = Modifier.size(px(32)))
        }
    }
}

// ───────────────────────── 공통 ─────────────────────────

@Composable
internal fun RadialGlow(color: Color, alpha: Float, modifier: Modifier) {
    Canvas(modifier) {
        // 시안의 타원형 radial gradient: 원으로 그린 뒤 가로·세로 비율로 늘린다
        val r = size.minDimension / 2
        scale(size.width / (2 * r), size.height / (2 * r)) {
            drawCircle(
                Brush.radialGradient(listOf(color.copy(alpha = alpha), Color(0x00191A1F)), center = center, radius = r),
                radius = r,
                center = center,
            )
        }
    }
}

@Preview(widthDp = 1000, heightDp = 696)
@Composable
private fun CreateMoodScreenPreview() {
    MoodriveTheme { CreateMoodScreen(DriveMode.PARKED, CreateMoodActions()) }
}
