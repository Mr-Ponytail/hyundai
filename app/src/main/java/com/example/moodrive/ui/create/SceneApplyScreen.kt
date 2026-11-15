package com.example.moodrive.ui.create

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.example.moodrive.R
import com.example.moodrive.domain.model.DriveMode
import com.example.moodrive.ui.common.White
import com.example.moodrive.ui.common.White64
import com.example.moodrive.ui.common.px
import com.example.moodrive.ui.common.rememberSpinAngle
import com.example.moodrive.ui.common.style
import com.example.moodrive.ui.theme.MoodriveColors
import com.example.moodrive.ui.theme.MoodriveTheme
import kotlinx.coroutines.delay

/*
 * 새 무드 만들기 - 3단계 "설정 만들기". Figma 435:46340 "5 · 활기찬 드라이브".
 * 고른 무드를 차량에 항목별로 적용하며 진행률을 보여준다.
 * 실제 차량 적용이 붙기 전까지는 시간에 따라 진행되는 흉내를 낸다.
 */

data class SceneApplyActions(
    val onCancel: () -> Unit = {},
    val onComplete: (MoodCandidateUi) -> Unit = {},
)

private enum class ApplyStatus { WAITING, RUNNING, DONE }

private class ApplyStep(val title: String, val detail: (ApplyStatus) -> String)

private fun applyStepsOf(s: MoodSettingsUi) = listOf(
    ApplyStep("앰비언트 라이트") { "${s.lightName} · ${s.brightness}%" },
    ApplyStep("바람") { s.wind },
    ApplyStep("시트") { s.seat.removePrefix("시트 ") + (s.wheel?.let { " · $it" } ?: "") },
    ApplyStep("사운드·음악") { s.playlist },
    ApplyStep("최종 확인") { if (it == ApplyStatus.RUNNING) "확인 중" else "준비 완료" },
)

/** 아직 대기 중인 항목은 상세 설정 대신 "—"를 보여준다 */
private fun ApplyStep.detailFor(status: ApplyStatus) = if (status == ApplyStatus.WAITING) "—" else detail(status)

/** 전체 적용 시간과, 100% 뒤 다음 단계로 넘어가기 전 잠깐 보여주는 시간 */
private const val APPLY_TOTAL_MS = 7000f
private const val COMPLETE_HOLD_MS = 800L

@Composable
fun SceneApplyScreen(
    driveMode: DriveMode,
    mood: MoodCandidateUi,
    actions: SceneApplyActions,
) {
    BackHandler(onBack = actions.onCancel)
    var progress by rememberSaveable { mutableFloatStateOf(0f) }
    val onComplete by rememberUpdatedState(actions.onComplete)
    LaunchedEffect(Unit) {
        var last = withFrameMillis { it }
        while (progress < 1f) {
            val now = withFrameMillis { it }
            progress = (progress + (now - last) / APPLY_TOTAL_MS).coerceAtMost(1f)
            last = now
        }
        delay(COMPLETE_HOLD_MS)
        onComplete(mood)
    }
    SceneApplyContent(driveMode, mood, progress, actions)
}

@Composable
private fun SceneApplyContent(
    driveMode: DriveMode,
    mood: MoodCandidateUi,
    progress: Float,
    actions: SceneApplyActions,
) {
    val light = Color(mood.settings.lightArgb)
    val steps = applyStepsOf(mood.settings)
    // 항목은 같은 시간씩 차례로 적용된다
    val running = (progress * steps.size).toInt().coerceAtMost(steps.size - 1)
    fun statusOf(i: Int) = when {
        progress >= 1f || i < running -> ApplyStatus.DONE
        i == running -> ApplyStatus.RUNNING
        else -> ApplyStatus.WAITING
    }

    CreateFlowScaffold(
        driveMode = driveMode,
        step = 3,
        onBack = actions.onCancel,
        glow = TopGlow(light, 0.22f, x = 600, width = 1400),
        background = { MoodBackdrop(mood.imageRes, light) },
    ) {
        Column(
            Modifier.fillMaxWidth().padding(top = px(169)),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                mood.name,
                style = style(48, FontWeight.Bold, White, 64, TextAlign.Center),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(px(7.5)))
            Text(
                if (progress >= 1f) "무드 생성 완료" else "무드 생성 중",
                style = style(26, FontWeight.Normal, White64, 34, TextAlign.Center),
            )
            Spacer(Modifier.height(px(68.5)))
            ProgressRing(progress)
        }

        Row(
            Modifier
                .fillMaxWidth()
                .offset(y = px(780))
                .padding(horizontal = px(60)),
            horizontalArrangement = Arrangement.spacedBy(px(21)),
        ) {
            steps.forEachIndexed { i, step ->
                val status = statusOf(i)
                ApplyStepCard(step.title, step.detailFor(status), status, Modifier.weight(1f))
            }
        }

        Text(
            "적용 취소",
            style = style(26, FontWeight.Bold, White64, 34, TextAlign.Center),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = px(1040))
                .clip(RoundedCornerShape(px(16)))
                .clickable(onClick = actions.onCancel)
                .padding(horizontal = px(24), vertical = px(18)),
        )
    }
}

/** 무드 이미지를 크게 흐려 깔고, 무드 조명색 블러와 시안 블러를 얹는다 */
@Composable
private fun MoodBackdrop(@DrawableRes imageRes: Int, light: Color) {
    Image(
        painterResource(imageRes),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize().blur(px(50)).alpha(0.5f),
    )
    // 시안 "Mood blur": 조명색, 위쪽 가운데에서 넓게 퍼짐
    RadialGlow(light, 0.55f, Modifier.offset(px(-888), px(-665)).size(px(3498), px(1426)))
    // 시안 오른쪽 위 시안색 블러
    RadialGlow(Color(0xFF2CC9F0), 0.1f, Modifier.offset(px(1081), px(108)).size(px(720)))
}

@Composable
private fun ProgressRing(progress: Float) {
    Box(Modifier.size(px(340)), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = px(16).toPx()
            val inset = stroke / 2
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawCircle(White.copy(alpha = 0.1f), radius = arcSize.width / 2, style = Stroke(stroke))
            drawArc(
                White,
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(stroke, cap = StrokeCap.Round),
            )
        }
        Text("${(progress * 100).toInt()}%", style = style(60, FontWeight.Bold, White, 80, TextAlign.Center))
    }
}

@Composable
private fun ApplyStepCard(title: String, detail: String, status: ApplyStatus, modifier: Modifier) {
    val active = status == ApplyStatus.RUNNING
    val shape = RoundedCornerShape(px(30))
    val bg by animateFloatAsState(if (active) 0.2f else 0.05f, tween(300), label = "bg")
    val border by animateFloatAsState(if (active) 0.64f else 0f, tween(300), label = "border")
    val height by animateDpAsState(if (active) px(224) else px(218), tween(300), label = "height")
    val titleColor by animateColorAsState(if (status == ApplyStatus.WAITING) White64 else White, label = "title")

    Column(
        modifier
            .height(height)
            .clip(shape)
            .background(White.copy(alpha = bg))
            .border(px(2), White.copy(alpha = border), shape)
            .padding(px(26)),
    ) {
        Row(Modifier.height(px(34)), verticalAlignment = Alignment.CenterVertically) {
            StatusIcon(status)
            Spacer(Modifier.width(px(11)))
            Text(
                when (status) {
                    ApplyStatus.DONE -> "완료"
                    ApplyStatus.RUNNING -> "적용 중"
                    ApplyStatus.WAITING -> "대기"
                },
                style = style(
                    20,
                    FontWeight.Normal,
                    when (status) {
                        ApplyStatus.DONE -> MoodriveColors.Positive
                        ApplyStatus.RUNNING -> White
                        ApplyStatus.WAITING -> White64
                    },
                    24,
                ),
            )
        }
        Spacer(Modifier.height(px(11)))
        Text(title, style = style(28, FontWeight.Bold, titleColor, 36), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(px(10.7)))
        Text(detail, style = style(20, FontWeight.Normal, White64, 24), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun StatusIcon(status: ApplyStatus) {
    val modifier = Modifier.size(px(34))
    when (status) {
        ApplyStatus.DONE -> Image(painterResource(R.drawable.ic_apply_done), null, modifier)
        ApplyStatus.WAITING -> Image(painterResource(R.drawable.ic_apply_waiting), null, modifier)
        ApplyStatus.RUNNING -> {
            val angle by rememberSpinAngle("spin")
            Image(painterResource(R.drawable.ic_apply_running), null, modifier.rotate(angle))
        }
    }
}

@Preview(widthDp = 1000, heightDp = 696)
@Composable
private fun SceneApplyPreview() {
    MoodriveTheme {
        SceneApplyContent(DriveMode.PARKED, SAMPLE_CANDIDATES.first(), progress = 0.64f, actions = SceneApplyActions())
    }
}
