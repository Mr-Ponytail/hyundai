package com.example.moodrive.ui.mood

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import com.example.moodrive.ui.theme.MoodriveColors
import com.example.moodrive.R
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import androidx.annotation.DrawableRes
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.moodrive.domain.model.FanDirection
import com.example.moodrive.ui.common.fig
import com.example.moodrive.ui.common.hexRgb
import com.example.moodrive.ui.theme.text
import kotlin.math.roundToInt

// ───────────────────────── 섹션 묶음 ─────────────────────────

/**
 * 앰비언트·시트·바람·핸들 열선 섹션. 실행 중 무드 수정 패널과 새 무드 만들기 완성 화면이 함께 쓴다.
 * 섹션 너비는 부모가 정한다 (시안 693px).
 */
@Composable
internal fun MoodEditSections(draft: MoodDraft, change: (MoodDraft) -> Unit, onEyedropper: () -> Unit) {
    Column {
        Section("앰비언트", draft.lightOn, { change(draft.copy(lightOn = it)) }, contentGap = fig(24)) {
            Label("컬러")
            Spacer(Modifier.height(fig(20)))
            Row {
                LightPreview(draft.mainColor, draft.subColor, draft.lightOn)
                Spacer(Modifier.width(fig(20)))
                Column(verticalArrangement = Arrangement.spacedBy(fig(12))) {
                    ColorSlotCard("Main · 상단", draft.mainColor, draft.editingSlot == ColorSlot.MAIN) { change(draft.copy(editingSlot = ColorSlot.MAIN)) }
                    ColorSlotCard("Sub · 하단", draft.subColor, draft.editingSlot == ColorSlot.SUB) { change(draft.copy(editingSlot = ColorSlot.SUB)) }
                }
            }
            Spacer(Modifier.height(fig(24) + fig(4)))
            val current = if (draft.editingSlot == ColorSlot.MAIN) draft.mainColor else draft.subColor
            Row(Modifier.fillMaxWidth().padding(horizontal = fig(4)), horizontalArrangement = Arrangement.SpaceBetween) {
                MoodDraft.PRESET_COLORS.forEach { c -> PaletteDot(c, selected = c == current) { change(draft.pickColor(c)) } }
                EyedropperDot(onEyedropper)
            }
            Spacer(Modifier.height(fig(30) + fig(4)))
            Label("전체 밝기")
            Spacer(Modifier.height(fig(20)))
            BrightnessBar(draft.brightnessPercent) { change(draft.copy(brightnessPercent = it)) }
        }
        Spacer(Modifier.height(SECTION_GAP))

        Section("시트", draft.seatOn, { change(draft.copy(seatOn = it)) }) {
            OptionRow("기능") {
                // Figma 722:23819: 선택하면 열선은 빨강, 통풍은 파랑 아이콘
                OptionSegmented(
                    listOf(
                        SeatMode.HEAT to Opt("열선", OptIcon.State(R.drawable.ic_opt_seat_heat, HEAT_RED)),
                        SeatMode.VENT to Opt("통풍", OptIcon.State(R.drawable.ic_opt_seat_vent, VENT_BLUE)),
                    ),
                    draft.seatMode,
                ) { change(draft.copy(seatMode = it)) }
            }
            Spacer(Modifier.height(fig(16)))
            OptionRow("단계") { LevelSegmented(draft.seatLevel) { change(draft.copy(seatLevel = it)) } }
        }
        Spacer(Modifier.height(SECTION_GAP))

        Section("바람", draft.windOn, { change(draft.copy(windOn = it)) }) {
            OptionRow("방향") {
                OptionSegmented(MoodDraft.WIND_DIRECTIONS.map { it to it.option() }, draft.windDirection) { change(draft.copy(windDirection = it)) }
            }
            Spacer(Modifier.height(fig(16)))
            OptionRow("풍량") { FanStepper(draft.fanLevel) { change(draft.copy(fanLevel = it)) } }
        }
        Spacer(Modifier.height(SECTION_GAP))

        Section("핸들 열선", draft.steeringOn, { change(draft.copy(steeringOn = it)) }, content = null)
    }
}

// ───────────────────────── 제목 ─────────────────────────

/**
 * 이름 + 수정 버튼. 버튼을 누르면 그 자리에서 고친다 (최대 30자).
 * 실행 중 무드 수정 패널과 새 무드 만들기 완성 화면이 함께 쓴다.
 * 수정 중 여부는 화면마다 보존 방식(remember / rememberSaveable)이 달라 호출부가 들고 있는다.
 */
@Composable
internal fun EditableName(
    name: String,
    onRename: (String) -> Unit,
    editing: Boolean,
    onEditingChange: (Boolean) -> Unit,
    textStyle: TextStyle,
    height: Dp,
    gap: Dp,
    buttonSize: Dp,
    buttonCorner: Dp,
    /** 수정 중이 아닐 때 버튼 바탕 투명도 (수정 중에는 0.3) */
    buttonRestAlpha: Float,
    buttonIcon: @Composable (contentDescription: String) -> Unit,
) {
    val focus = remember { FocusRequester() }
    Row(Modifier.height(height), verticalAlignment = Alignment.CenterVertically) {
        if (editing) {
            BasicTextField(
                value = name,
                onValueChange = { onRename(it.take(30)) },
                singleLine = true,
                textStyle = textStyle,
                cursorBrush = SolidColor(Color.White),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onEditingChange(false) }),
                modifier = Modifier.weight(1f, fill = false).focusRequester(focus),
            )
            LaunchedEffect(Unit) { focus.requestFocus() }
        } else {
            Text(name, style = textStyle, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
        }
        Spacer(Modifier.width(gap))
        Box(
            Modifier
                .size(buttonSize)
                .clip(RoundedCornerShape(buttonCorner))
                .background(Color.White.copy(alpha = if (editing) 0.3f else buttonRestAlpha))
                .clickable { onEditingChange(!editing) },
            contentAlignment = Alignment.Center,
        ) {
            buttonIcon(if (editing) "이름 수정 완료" else "이름 수정")
        }
    }
}

// ───────────────────────── 섹션 ─────────────────────────

@Composable
private fun Section(
    title: String,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    contentGap: Dp = fig(32),
    content: (@Composable ColumnScope.() -> Unit)?,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(fig(32)))
            .background(SECTION_BG)
            .padding(fig(32)),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = text(18.sp, FontWeight.Bold, Color.White, lineHeight = 23.sp), modifier = Modifier.weight(1f))
            Toggle(enabled, onToggle)
        }
        // 끄면 옵션은 접히고 항목 이름만 남는다 (값은 유지 → 다시 켜면 그대로)
        if (content != null) {
            AnimatedVisibility(
                visible = enabled,
                enter = expandVertically(tween(260, easing = FastOutSlowInEasing), expandFrom = Alignment.Top) + fadeIn(tween(200, delayMillis = 80)),
                exit = shrinkVertically(tween(240, easing = FastOutSlowInEasing), shrinkTowards = Alignment.Top) + fadeOut(tween(120)),
            ) {
                Column {
                    Spacer(Modifier.height(contentGap))
                    content()
                }
            }
        }
    }
}

/** 시안 스위치: 높이 48(시안 단위), 켜짐 #00E074 */
@Composable
private fun Toggle(checked: Boolean, onChange: (Boolean) -> Unit) {
    val height = fig(48)
    val width = fig(80)
    val pad = fig(6)
    val thumb = height - pad * 2
    val x by animateDpAsState(if (checked) width - thumb - pad else pad, tween(200, easing = FastOutSlowInEasing), label = "toggleThumb")
    val track by animateColorAsState(if (checked) TOGGLE_ON else TOGGLE_OFF, tween(200), label = "toggleTrack")
    Box(
        Modifier
            .size(width, height)
            .clip(CircleShape)
            .background(track)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onChange),
    ) {
        Box(
            Modifier
                .offset(x = x)
                .align(Alignment.CenterStart)
                .size(thumb)
                .clip(CircleShape)
                .background(Color.White),
        )
    }
}

@Composable
private fun Label(value: String) {
    Text(value, style = text(13.sp, FontWeight.Normal, LABEL, lineHeight = 15.5.sp))
}

@Composable
private fun OptionRow(label: String, control: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = text(14.sp, FontWeight.Normal, LABEL), modifier = Modifier.weight(1f))
        Box(Modifier.width(fig(420))) { control() }
    }
}

// ───────────────────────── 앰비언트 ─────────────────────────

/** 상단·하단 조명 미리보기 (빛 번짐 표현) */
@Composable
private fun LightPreview(main: Int, sub: Int, on: Boolean) {
    Box(
        Modifier
            .size(fig(300), fig(200))
            .clip(RoundedCornerShape(fig(24)))
            .background(Color(0xFF0F0F12)),
    ) {
        val labelStyle = text(9.5.sp, FontWeight.SemiBold, Color.White.copy(alpha = 0.6f), lineHeight = 11.sp)
        Text("상단", style = labelStyle, modifier = Modifier.padding(start = fig(40), top = fig(36)))
        Text("하단", style = labelStyle, modifier = Modifier.padding(start = fig(40), top = fig(112)))
        Canvas(Modifier.fillMaxSize()) {
            // 시안: 흐리게 번진 18px 막대(60%) 위에 6px 선
            val glow = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                strokeCap = android.graphics.Paint.Cap.ROUND
                strokeWidth = fig(18).toPx()
                maskFilter = android.graphics.BlurMaskFilter(fig(10).toPx(), android.graphics.BlurMaskFilter.Blur.NORMAL)
            }
            fun glowLine(color: Color, y: Float) {
                val start = Offset(fig(40).toPx(), y)
                val end = Offset(fig(260).toPx(), y)
                val c = if (on) color else Color(0xFF3A3B40)
                if (on) {
                    glow.color = c.copy(alpha = 0.6f).toArgb()
                    drawIntoCanvas { it.nativeCanvas.drawLine(start.x, start.y, end.x, end.y, glow) }
                }
                drawLine(c, start, end, fig(6).toPx(), StrokeCap.Round)
            }
            glowLine(Color(main), fig(67).toPx())
            glowLine(Color(sub), fig(143).toPx())
        }
    }
}

@Composable
private fun ColorSlotCard(label: String, argb: Int, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(fig(24))
    Row(
        Modifier
            .size(fig(309), fig(94))
            .clip(shape)
            .background(Color.White.copy(alpha = if (selected) 0.14f else 0.05f))
            .border(1.dp, if (selected) Color.White.copy(alpha = 0.7f) else Color.Transparent, shape)
            .clickable(onClick = onClick)
            .padding(start = fig(22)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(fig(44)).clip(CircleShape).background(Color(argb)))
        Spacer(Modifier.width(fig(16)))
        Column {
            Text(label, style = text(10.5.sp, FontWeight.Normal, Color.White.copy(alpha = 0.6f), lineHeight = 12.5.sp))
            Spacer(Modifier.height(fig(2)))
            Text(hexRgb(argb), style = text(14.sp, FontWeight.Bold, Color.White, lineHeight = 17.sp))
        }
    }
}

@Composable
private fun PaletteDot(argb: Int, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(DOT)
            .clip(CircleShape)
            .background(Color(argb))
            .border(fig(4), if (selected) Color.White else Color.Transparent, CircleShape)
            .clickable(onClick = onClick),
    )
}

/** 스포이드: 시안에만 있고 기능은 추후 */
@Composable
private fun EyedropperDot(onClick: () -> Unit) {
    Box(
        Modifier.size(DOT).clip(CircleShape).background(Color.White.copy(alpha = 0.1f)).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(R.drawable.ic_color_picker), contentDescription = "스포이드 (준비 중)", tint = Color.White, modifier = Modifier.size(fig(28)))
    }
}

/** 밝기: 막대 전체가 슬라이더. 탭·드래그로 조절 */
@Composable
private fun BrightnessBar(percent: Int, onChange: (Int) -> Unit) {
    val latest by rememberUpdatedState(onChange)
    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .height(fig(80))
            .clip(RoundedCornerShape(fig(24)))
            .background(Color.White.copy(alpha = 0.1f)),
    ) {
        val widthPx = constraints.maxWidth.toFloat()
        fun valueAt(x: Float) = (x / widthPx * 100).roundToInt().coerceIn(0, 100)
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(percent / 100f)
                .clip(RoundedCornerShape(fig(24)))
                .background(Color.White.copy(alpha = 0.92f)),
        )
        Icon(
            painterResource(R.drawable.ic_sun), contentDescription = null,
            tint = if (percent > 8) Color(0xFF1A1A1C) else Color.White,
            modifier = Modifier.align(Alignment.CenterStart).padding(start = fig(24)).size(fig(40)),
        )
        Text(
            "$percent%",
            style = text(15.sp, FontWeight.Bold, if (percent > 88) Color(0xFF1A1A1C) else Color.White.copy(alpha = 0.84f)),
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = fig(16)),
        )
        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(widthPx) { detectTapGestures { latest(valueAt(it.x)) } }
                .pointerInput(widthPx) { detectHorizontalDragGestures { change, _ -> latest(valueAt(change.position.x)) } },
        )
    }
}

// ───────────────────────── 선택 버튼 ─────────────────────────

private sealed interface OptIcon {
    /** 색이 들어간 시안 아이콘 (시트 열선) */
    data class Image(@DrawableRes val id: Int) : OptIcon
    /** 단색 시안 아이콘: 선택 여부에 따라 색을 입힌다 */
    data class Tinted(@DrawableRes val id: Int) : OptIcon
    /** Material 아이콘 */
    data class Vector(@DrawableRes val id: Int) : OptIcon
    /** 단색 시안 아이콘 (Figma 722:23819·23832): 선택하면 [selectedColor], 아니면 흰색 40% */
    data class State(@DrawableRes val id: Int, val selectedColor: Color) : OptIcon
}

private data class Opt(val label: String, val icon: OptIcon? = null)

@Composable
private fun <T> OptionSegmented(options: List<Pair<T, Opt>>, selected: T, onSelect: (T) -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(fig(80)).clip(RoundedCornerShape(fig(24))).background(Color.White.copy(alpha = 0.06f)).padding(fig(8)),
    ) {
        options.forEach { (value, opt) ->
            val on = value == selected
            Row(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(fig(16)))
                    .background(if (on) Color.White else Color.Transparent)
                    .clickable { onSelect(value) },
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val color = if (on) MoodriveColors.Scrim else Color.White.copy(alpha = 0.64f)
                val iconSize = fig(36)
                opt.icon?.let {
                    when (it) {
                        is OptIcon.Image -> Image(painterResource(it.id), null, modifier = Modifier.size(iconSize))
                        is OptIcon.Tinted -> Icon(painterResource(it.id), null, tint = color, modifier = Modifier.size(iconSize))
                        is OptIcon.Vector -> Icon(painterResource(it.id), null, tint = color, modifier = Modifier.size(fig(28)))
                        is OptIcon.State -> Icon(
                            painterResource(it.id), null,
                            tint = if (on) it.selectedColor else Color.White.copy(alpha = 0.4f),
                            modifier = Modifier.size(iconSize),
                        )
                    }
                    Spacer(Modifier.width(fig(10)))
                }
                Text(opt.label, style = text(if (on) 15.sp else 14.sp, FontWeight.Normal, color, lineHeight = 20.sp), maxLines = 1)
            }
        }
    }
}

@Composable
private fun LevelSegmented(level: Int, onSelect: (Int) -> Unit) {
    OptionSegmented(MoodDraft.LEVELS.map { it to Opt("${it}단") }, level, onSelect)
}

/** Figma 722:23832: 발 · 얼굴 두 가지. 선택하면 아이콘이 글자와 같은 진한 색 */
private fun FanDirection.option() = when (this) {
    FanDirection.FACE -> Opt("얼굴", OptIcon.State(R.drawable.ic_opt_air_face, MoodriveColors.Scrim))
    else -> Opt("발", OptIcon.State(R.drawable.ic_opt_air_feet, MoodriveColors.Scrim))
}

/** Figma 722:24606: 풍량 스테퍼 (− 5단 +). 범위 밖으로는 버튼이 흐려지고 눌리지 않는다 */
@Composable
private fun FanStepper(level: Int, onChange: (Int) -> Unit) {
    val range = MoodDraft.FAN_LEVELS
    Row(
        Modifier.fillMaxWidth().height(fig(80)).clip(RoundedCornerShape(fig(24))).background(Color.White.copy(alpha = 0.06f)).padding(fig(8)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StepperButton(plus = false, enabled = level > range.first) { onChange(level - 1) }
        Text(
            "${level}단",
            style = text(15.sp, FontWeight.SemiBold, Color.White, lineHeight = 20.sp),
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )
        StepperButton(plus = true, enabled = level < range.last) { onChange(level + 1) }
    }
}

@Composable
private fun StepperButton(plus: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(fig(64))
            .alpha(if (enabled) 1f else 0.3f)
            .clip(RoundedCornerShape(fig(16)))
            .background(Color.White.copy(alpha = 0.1f))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        // 22×2.5 막대 (더하기는 세로 막대 하나 더)
        Box(Modifier.size(fig(22), (2.5f * FIG).dp).clip(RoundedCornerShape((1.25f * FIG).dp)).background(Color.White))
        if (plus) Box(Modifier.size((2.5f * FIG).dp, fig(22)).clip(RoundedCornerShape((1.25f * FIG).dp)).background(Color.White))
    }
}

private val SECTION_BG = Color.White.copy(alpha = 0.05f)
/** 시안 아이콘 색: 열선(기존 열선 아이콘 색), 통풍(Figma #386BFF). 실행 화면 카드도 같은 색을 쓴다 */
internal val HEAT_RED = Color(0xFFF01832)
internal val VENT_BLUE = Color(0xFF386BFF)
/** fig() 와 같은 비율 (소수 px 용) */
private const val FIG = 467f / 793f
private val LABEL = Color.White.copy(alpha = 0.64f)
private val TOGGLE_ON = MoodriveColors.Positive
private val TOGGLE_OFF = Color(0xFF8E8F94)
private val SECTION_GAP = fig(50)
private val DOT: Dp = fig(56)
