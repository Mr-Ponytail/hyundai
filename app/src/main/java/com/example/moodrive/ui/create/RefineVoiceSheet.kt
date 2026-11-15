package com.example.moodrive.ui.create

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.moodrive.ui.common.White
import com.example.moodrive.ui.common.clickableNoRipple
import com.example.moodrive.ui.common.consumeTaps
import com.example.moodrive.ui.common.px
import com.example.moodrive.ui.common.style
import kotlinx.coroutines.delay
import kotlin.math.sin

/*
 * 무드 고르기 - "말로 다듬기" 듣는 중 시트 (Figma 435:46729 "4-1 · 말로 다듬기 (듣는 중)").
 * 지금 요청은 그대로 두고 조건만 한 번 더 말한다. 시트 밖을 누르거나 뒤로 가면 닫힌다.
 */

/** 음성 인식(STT)을 붙이기 전까지 시안 문장을 단어 단위로 받아 적는 데모 */
private const val REFINE_DEMO_UTTERANCE = "조금 더 밝은 낮에 달리고 싶어"
private const val WORD_INTERVAL_MS = 650L
/** 말을 멈춘 뒤 다시 찾기 전까지 결과를 보여주는 시간 */
private const val AUTO_ADVANCE_MS = 1500L

private val SheetColor = Color(0xFF1D1E21)

/**
 * 화면 아래에서 올라오는 시트. [BoxScope] 안에서 화면 전체를 덮도록 쓴다.
 * 말이 끝나면 [onDone]으로 덧붙일 조건 문장을 넘긴다.
 */
@Composable
internal fun BoxScope.RefineVoiceSheet(onDismiss: () -> Unit, onDone: (String) -> Unit) {
    BackHandler(onBack = onDismiss)
    val done by rememberUpdatedState(onDone)
    var transcript by remember { mutableStateOf(Transcript(speaking = true)) }

    LaunchedEffect(Unit) {
        val words = REFINE_DEMO_UTTERANCE.split(" ")
        delay(700)
        for (i in 1..words.size) {
            // 마지막 두 단어는 인식 중(흐림), 그 앞은 확정
            val pendingFrom = (i - 2).coerceAtLeast(0)
            transcript = Transcript(
                confirmed = words.take(pendingFrom).joinToString(" "),
                pending = words.subList(pendingFrom, i).joinToString(" "),
                speaking = true,
            )
            delay(WORD_INTERVAL_MS)
        }
        transcript = Transcript(confirmed = REFINE_DEMO_UTTERANCE, speaking = false)
        delay(AUTO_ADVANCE_MS)
        done(transcript.text)
    }

    val level by animateFloatAsState(if (transcript.speaking) 1f else 0.18f, tween(500), label = "refineLevel")
    val time = rememberTimeSeconds()

    // 뒤 화면을 어둡게 (시트 밖을 누르면 닫기)
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)).clickableNoRipple(onDismiss))

    val shape = RoundedCornerShape(topStart = px(40), topEnd = px(40))
    Box(
        Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .height(px(400))
            .clip(shape)
            .background(SheetColor.copy(alpha = 0.96f))
            .border(1.dp, White.copy(alpha = 0.1f), shape)
            .consumeTaps(),
    ) {
        // 마이크 뒤 푸른 글로우 (시트 밖으로 넘치는 부분은 잘린다)
        RadialGlow(Color(0xFFBDD4FF), 0.3f, Modifier.offset(px(-363), px(-154)).size(px(1108), px(826)))
        // 268px 링, 중심 (201, 202)
        MicPulse(time, level, diameter = 268f, modifier = Modifier.offset(px(67), px(68)))

        Column(Modifier.offset(px(423), px(108))) {
            RefineListening(transcript.speaking)
            Spacer(Modifier.height(px(14)))
            RefineTranscript(transcript)
        }
        RefineWaveform(time, level, Modifier.offset(px(427), px(253)))
    }
}

@Composable
private fun RefineListening(speaking: Boolean) {
    val blink by rememberInfiniteTransition(label = "refineRec").animateFloat(
        initialValue = 1f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "refineDot",
    )
    Row(Modifier.height(px(28)), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(px(12)).alpha(if (speaking) blink else 1f).clip(CircleShape).background(White))
        Spacer(Modifier.width(px(10)))
        Text(
            if (speaking) "듣고 있어요 · 지금 무드는 그대로 두고 조건만 더해요" else "잘 들었어요 · 조건을 더해 다시 찾아볼게요",
            style = style(24, FontWeight.Normal, White.copy(alpha = 0.64f), lineHeight = 28),
            maxLines = 1,
        )
    }
}

@Composable
private fun RefineTranscript(transcript: Transcript) {
    val dim = SpanStyle(color = White.copy(alpha = 0.4f))
    val text = buildAnnotatedString {
        append("“")
        append(transcript.confirmed)
        if (transcript.pending.isNotEmpty()) {
            if (transcript.confirmed.isNotEmpty()) append(" ")
            withStyle(dim) { append(transcript.pending) }
            withStyle(dim) { append("”") }
        } else {
            append("”")
        }
    }
    Box(Modifier.height(px(52))) {
        if (transcript.text.isNotEmpty()) {
            Text(text, style = style(40, FontWeight.Bold, White, lineHeight = 52), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** 시안 막대 17개 높이(px). 높은 막대는 진하게, 낮은 막대는 반투명 */
private val REFINE_BARS = intArrayOf(10, 18, 30, 44, 26, 52, 36, 20, 40, 58, 30, 16, 34, 48, 24, 12, 8)
private const val BAR_W = 6f
private const val BAR_STEP = 12f
private const val BAR_MIN = 8f
private const val BAR_MAX = 58f

@Composable
private fun RefineWaveform(time: State<Float>, level: Float, modifier: Modifier) {
    Canvas(modifier.size(px(198), px(BAR_MAX))) {
        val t = time.value
        val unit = size.height / BAR_MAX
        val cy = size.height / 2
        REFINE_BARS.forEachIndexed { i, base ->
            val wobble = 0.6f + 0.4f * sin(t * (5f + (i % 5) * 1.3f) + i * 0.9f)
            val target = BAR_MIN + (base - BAR_MIN) * wobble * 1.25f
            val h = (BAR_MIN + (target - BAR_MIN) * level).coerceIn(BAR_MIN, BAR_MAX)
            drawRoundRect(
                color = White.copy(alpha = if (h >= 36f) 0.95f else 0.5f),
                topLeft = Offset(i * BAR_STEP * unit, cy - h * unit / 2),
                size = Size(BAR_W * unit, h * unit),
                cornerRadius = CornerRadius(3f * unit),
            )
        }
    }
}
