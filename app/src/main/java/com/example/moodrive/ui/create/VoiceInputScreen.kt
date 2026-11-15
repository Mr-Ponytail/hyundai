package com.example.moodrive.ui.create

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import com.example.moodrive.R
import com.example.moodrive.domain.model.DriveMode
import com.example.moodrive.ui.common.FlowButton
import com.example.moodrive.ui.common.White
import com.example.moodrive.ui.common.White64
import com.example.moodrive.ui.common.px
import com.example.moodrive.ui.common.style
import com.example.moodrive.ui.theme.MoodriveColors
import com.example.moodrive.ui.theme.MoodriveTheme
import kotlinx.coroutines.delay
import kotlin.math.sin

/*
 * 새 무드 만들기 - 1단계 "말로 설명하기" 듣는 중 화면 (Figma 435:46170 "2 · 활기찬 드라이브").
 * 시안에는 정지 화면만 있고, 마이크 링·웨이브폼 애니메이션은 시안 값(크기·투명도)을 기준으로 덧붙였다.
 */

data class VoiceInputActions(
    val onBack: () -> Unit = {},
    /** 완료 버튼 또는 말이 멈춘 뒤 자동으로 넘어갈 때 */
    val onDone: (String) -> Unit = {},
)

/**
 * 음성 인식 결과. [confirmed]는 확정된 앞부분(흰색), [pending]은 아직 인식 중인 뒷부분(흐리게).
 * [speaking]이 true면 마이크 링과 웨이브가 크게 움직인다.
 */
data class Transcript(val confirmed: String = "", val pending: String = "", val speaking: Boolean = false) {
    val text get() = listOf(confirmed, pending).filter { it.isNotBlank() }.joinToString(" ")
}

/** 음성 인식(STT)을 붙이기 전까지 시안 문장을 단어 단위로 받아 적는 데모 */
private const val DEMO_UTTERANCE = "활기찬 드라이브를 하고 싶어"
private const val WORD_INTERVAL_MS = 650L
/** 기획: 말을 멈추면 자동으로 다음으로 넘어감 */
private const val AUTO_ADVANCE_MS = 2500L

@Composable
fun VoiceInputScreen(driveMode: DriveMode, actions: VoiceInputActions) {
    BackHandler(onBack = actions.onBack)
    // 다시 말하기를 누를 때마다 바뀌어 받아쓰기를 처음부터 다시 시작한다
    var session by rememberSaveable { mutableIntStateOf(0) }
    var transcript by remember { mutableStateOf(Transcript()) }

    LaunchedEffect(session) {
        transcript = Transcript(speaking = true)
        val words = DEMO_UTTERANCE.split(" ")
        delay(900)
        for (i in 1..words.size) {
            // 마지막 두 단어는 인식 중(흐림)으로, 그 앞은 확정으로 보여준다
            val pendingFrom = (i - 2).coerceAtLeast(0)
            transcript = Transcript(
                confirmed = words.take(pendingFrom).joinToString(" "),
                pending = words.subList(pendingFrom, i).joinToString(" "),
                speaking = true,
            )
            delay(WORD_INTERVAL_MS)
        }
        transcript = Transcript(confirmed = DEMO_UTTERANCE, speaking = false)
        delay(AUTO_ADVANCE_MS)
        actions.onDone(transcript.text)
    }

    VoiceInputContent(
        driveMode = driveMode,
        transcript = transcript,
        onBack = actions.onBack,
        onDone = { transcript.text.takeIf { it.isNotBlank() }?.let(actions.onDone) },
        onRetry = { session++ },
    )
}

@Composable
private fun VoiceInputContent(
    driveMode: DriveMode,
    transcript: Transcript,
    onBack: () -> Unit,
    onDone: () -> Unit,
    onRetry: () -> Unit,
) {
    // 말하는 중이면 1, 멈추면 작게 숨쉬는 정도로 줄인다
    val level by animateFloatAsState(if (transcript.speaking) 1f else 0.18f, tween(500), label = "level")
    val time = rememberTimeSeconds()

    CreateFlowScaffold(driveMode = driveMode, step = 1, onBack = onBack) {
        // 마이크 뒤 926×926 글로우
        RadialGlow(Color(0xFFBDD4FF), 0.3f, Modifier.align(Alignment.TopCenter).offset(y = px(19)).size(px(926)))

        Column(
            Modifier.fillMaxWidth().offset(y = px(205)),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ListeningLabel(transcript.speaking)
            Spacer(Modifier.height(px(33)))
            MicPulse(time, level)
            Spacer(Modifier.height(px(47)))
            Waveform(time, level)
            Spacer(Modifier.height(px(63)))
            TranscriptText(transcript)
            Spacer(Modifier.height(px(148)))
            Row(horizontalArrangement = Arrangement.spacedBy(px(24))) {
                FlowButton(
                    label = "완료",
                    textStyle = style(30, FontWeight.Bold, MoodriveColors.Scrim, lineHeight = 38),
                    background = MoodriveColors.ButtonLight,
                    cornerRadius = px(32),
                    modifier = Modifier.size(px(360), px(100)),
                    icon = R.drawable.ic_create_check,
                    enabled = transcript.text.isNotBlank(),
                    onClick = onDone,
                )
                FlowButton(
                    label = "다시 말하기",
                    textStyle = style(28, FontWeight.Bold, White, lineHeight = 36),
                    background = White.copy(alpha = 0.1f),
                    cornerRadius = px(32),
                    modifier = Modifier.size(px(360), px(100)),
                    icon = R.drawable.ic_create_refresh,
                    onClick = onRetry,
                )
            }
            Spacer(Modifier.height(px(28)))
            Text(
                "말을 멈추면 자동으로 다음으로 넘어가요",
                style = style(20, FontWeight.Normal, White64, lineHeight = 24, align = TextAlign.Center),
            )
        }
    }
}

// ───────────────────────── 듣고 있어요 ─────────────────────────

@Composable
private fun ListeningLabel(speaking: Boolean) {
    val blink by rememberInfiniteTransition(label = "rec").animateFloat(
        initialValue = 1f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "recDot",
    )
    Row(Modifier.height(px(52)), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(px(14))
                .alpha(if (speaking) blink else 1f)
                .clip(CircleShape)
                .background(MoodriveColors.Danger),
        )
        Spacer(Modifier.width(px(16)))
        Text(
            if (speaking) "듣고 있어요..." else "잘 들었어요",
            style = style(40, FontWeight.Normal, White.copy(alpha = 0.32f), lineHeight = 52),
        )
    }
}

// ───────────────────────── 마이크 ─────────────────────────

/**
 * 시안의 300/240/180 동심원. 바깥 두 링은 서로 다른 박자로 숨쉬고,
 * 흰 원 바깥으로 물결이 퍼져 나간다. 크기는 [level]에 비례.
 * [diameter]는 바깥 링 지름(시안 px). 말로 다듬기 시트는 같은 비율로 줄여 쓴다.
 */
@Composable
internal fun MicPulse(time: State<Float>, level: Float, diameter: Float = 300f, modifier: Modifier = Modifier) {
    val k = diameter / 300f
    Box(modifier.size(px(diameter)), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(px(diameter))) {
            val t = time.value
            val unit = size.minDimension / 300f // 시안 px → 실제 px
            // 퍼져 나가는 물결 2개 (1.8초 주기, 반 박자 차이)
            repeat(2) { i ->
                val p = ((t / 1.8f) + i * 0.5f) % 1f
                val r = (90f + 90f * p) * unit
                drawCircle(White.copy(alpha = 0.22f * (1f - p) * level), radius = r, style = Stroke(width = 2f * unit))
            }
            val outer = 150f + 10f * level * sin(t * 2.6f)
            val middle = 120f + 8f * level * sin(t * 3.4f + 1.2f)
            drawCircle(White.copy(alpha = 0.1f), radius = outer * unit)
            drawCircle(White.copy(alpha = 0.2f), radius = middle * unit)
        }
        Box(
            Modifier
                .size(px(180 * k))
                .graphicsLayer {
                    val s = 1f + 0.035f * level * sin(time.value * 5f)
                    scaleX = s
                    scaleY = s
                }
                .clip(CircleShape)
                .background(White),
            contentAlignment = Alignment.Center,
        ) {
            Image(painterResource(R.drawable.ic_create_mic), contentDescription = "음성 입력 중", modifier = Modifier.size(px(84 * k)))
        }
    }
}

// ───────────────────────── 웨이브폼 ─────────────────────────

/** 시안 막대 30개의 높이(px). 가운데가 높은 모양을 유지하면서 출렁이게 한다 */
private val BAR_HEIGHTS = intArrayOf(
    10, 16, 28, 44, 60, 36, 24, 52, 72, 60, 32, 20, 40, 64, 80,
    56, 28, 18, 44, 68, 52, 30, 16, 26, 48, 56, 36, 20, 12, 10,
)
private const val BAR_WIDTH = 8f
private const val BAR_GAP = 10f
private const val BAR_MIN = 10f

@Composable
private fun Waveform(time: State<Float>, level: Float) {
    Canvas(Modifier.size(px(530), px(80))) {
        val t = time.value
        val unit = size.height / 80f
        val cy = size.height / 2
        BAR_HEIGHTS.forEachIndexed { i, base ->
            // 막대마다 속도·위상을 달리해 실제 목소리처럼 불규칙하게
            val wobble = 0.6f + 0.4f * sin(t * (5f + (i % 5) * 1.3f) + i * 0.9f)
            val swell = 0.88f + 0.12f * sin(t * 1.7f - i * 0.35f)
            val target = BAR_MIN + (base - BAR_MIN) * wobble * swell * 1.3f
            val h = (BAR_MIN + (target - BAR_MIN) * level).coerceIn(BAR_MIN, 80f)
            // 시안처럼 높은 막대(≥52px)는 흰색, 낮은 막대는 64%
            val a = 0.64f + 0.36f * ((h - 40f) / 16f).coerceIn(0f, 1f)
            drawRoundRect(
                color = White.copy(alpha = a),
                topLeft = Offset(i * (BAR_WIDTH + BAR_GAP) * unit, cy - h * unit / 2),
                size = Size(BAR_WIDTH * unit, h * unit),
                cornerRadius = CornerRadius(4f * unit),
            )
        }
    }
}

// ───────────────────────── 받아쓰기 ─────────────────────────

@Composable
private fun TranscriptText(transcript: Transcript) {
    val base = style(48, FontWeight.Bold, White, lineHeight = 64, align = TextAlign.Center)
    val dim = SpanStyle(color = White.copy(alpha = 0.32f))
    val text = buildAnnotatedString {
        if (transcript.text.isEmpty()) return@buildAnnotatedString
        append("“")
        append(transcript.confirmed)
        if (transcript.pending.isNotEmpty()) {
            if (transcript.confirmed.isNotEmpty()) append(" ")
            withStyle(dim) { append(transcript.pending) }
        }
        // 닫는 따옴표는 마지막 글자와 같은 색
        if (transcript.pending.isNotEmpty()) withStyle(dim) { append("”") } else append("”")
    }
    Box(Modifier.height(px(64)), contentAlignment = Alignment.Center) {
        Text(text, style = base, maxLines = 1)
    }
}

// ───────────────────────── 공통 ─────────────────────────

/** 프레임마다 갱신되는 경과 시간(초). Canvas/graphicsLayer 안에서만 읽어 재구성 없이 다시 그린다 */
@Composable
internal fun rememberTimeSeconds(): State<Float> = produceState(0f) {
    val start = withFrameMillis { it }
    while (true) {
        withFrameMillis { value = (it - start) / 1000f }
    }
}

@Preview(widthDp = 1000, heightDp = 696)
@Composable
private fun VoiceInputPreview() {
    MoodriveTheme {
        VoiceInputContent(
            driveMode = DriveMode.PARKED,
            transcript = Transcript("활기찬 드라이브를", "하고 싶어", speaking = true),
            onBack = {}, onDone = {}, onRetry = {},
        )
    }
}
