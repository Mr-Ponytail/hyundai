package com.example.moodrive.ui.common

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.moodrive.R
import com.example.moodrive.domain.model.AmbientLightState
import com.example.moodrive.domain.model.DriveMode
import com.example.moodrive.ui.theme.MoodriveColors
import com.example.moodrive.ui.theme.text

/* 여러 화면(홈·실행·만들기·탐색)이 함께 쓰는 요소 */

@Composable
internal fun DriveStatusPill(mode: DriveMode, modifier: Modifier = Modifier) {
    val (label, bg, fg) = when (mode) {
        DriveMode.PARKED -> Triple("P 정차 중", MoodriveColors.ParkedBg, MoodriveColors.ParkedFg)
        DriveMode.DRIVING -> Triple("D 주행 중", MoodriveColors.DrivingBg, MoodriveColors.DrivingFg)
        DriveMode.UNKNOWN -> Triple("상태 확인 중", MoodriveColors.SurfaceRaised, MoodriveColors.TextTertiary)
    }
    Row(
        modifier = modifier.height(38.dp).clip(CircleShape).background(bg).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(7.dp).clip(CircleShape).background(fg))
        Spacer(Modifier.width(6.dp))
        Text(label, style = text(14.sp, FontWeight.Normal, fg))
    }
}

@Composable
internal fun DetailIcon(kind: DetailKind, tint: Color, size: Dp = 20.dp) {
    // 시안(Figma) 아이콘. 단색 PNG라 tint로 색을 입힌다
    val res = when (kind) {
        DetailKind.AMBIENT -> R.drawable.ic_detail_bulb
        DetailKind.SEAT -> R.drawable.ic_detail_seat
        DetailKind.WIND -> R.drawable.ic_detail_wind
        DetailKind.STEERING -> R.drawable.ic_detail_steering
    }
    Icon(painterResource(res), contentDescription = null, tint = tint, modifier = Modifier.size(size))
}


internal fun paletteBrush(argb: List<Int>): Brush = when (argb.size) {
    0 -> Brush.verticalGradient(listOf(MoodriveColors.SurfaceRaised, MoodriveColors.Background))
    1 -> Brush.verticalGradient(listOf(Color(argb[0]), Color(argb[0])))
    else -> Brush.linearGradient(argb.map(::Color))
}

/** 즐겨찾기 토글. 크기·모서리·배경은 화면마다 시안 값이 달라 호출부에서 넘긴다 */
@Composable
internal fun FavoriteToggle(
    favorite: Boolean,
    onClick: () -> Unit,
    size: Dp,
    cornerRadius: Dp,
    background: Color,
    filledStarSize: Dp,
    outlineStarSize: Dp,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(cornerRadius))
            .background(background)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (favorite) {
            // 시안 별 아이콘 (색 포함)
            Image(painterResource(R.drawable.ic_star_filled), contentDescription = "즐겨찾기 해제", modifier = Modifier.size(filledStarSize))
        } else {
            Icon(painterResource(R.drawable.ic_m_star_outline), contentDescription = "즐겨찾기", tint = Color.White, modifier = Modifier.size(outlineStarSize))
        }
    }
}

/**
 * 만들기 흐름·탐색 히어로의 큰 버튼 (시안 높이 100px): [아이콘(기본 32px)] + 라벨을 가운데 정렬.
 * 너비·모서리·색·글자는 화면마다 시안 값이 달라 호출부에서 넘긴다.
 */
@Composable
internal fun FlowButton(
    label: String,
    textStyle: TextStyle,
    background: Color,
    cornerRadius: Dp,
    modifier: Modifier = Modifier,
    @DrawableRes icon: Int? = null,
    border: BorderStroke? = null,
    enabled: Boolean = true,
    // 탐색 히어로 버튼은 아이콘 40px·간격 10px
    iconSize: Dp = px(32),
    iconGap: Dp = px(12),
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(cornerRadius)
    Row(
        modifier
            .height(px(100))
            // 비활성일 때만 흐리게 (활성 버튼에는 레이어를 더하지 않는다)
            .then(if (enabled) Modifier else Modifier.alpha(0.4f))
            .clip(shape)
            .background(background)
            .then(if (border != null) Modifier.border(border, shape) else Modifier)
            .clickable(enabled = enabled, onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Image(painterResource(icon), contentDescription = null, modifier = Modifier.size(iconSize))
            Spacer(Modifier.width(iconGap))
        }
        Text(label, style = textStyle)
    }
}

/**
 * 무드 설정 요약 칩 (만들기 1순위 카드·탐색 히어로 카드).
 * 만들기는 한 줄로 자르고 탐색은 제한이 없어 maxLines 를 호출부에서 정한다.
 */
@Composable
internal fun SettingChip(label: String, maxLines: Int = Int.MAX_VALUE, leading: @Composable () -> Unit) {
    Row(
        Modifier
            .height(px(56))
            .clip(RoundedCornerShape(px(18)))
            .background(MoodriveColors.Scrim.copy(alpha = 0.55f))
            .padding(horizontal = px(14)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading()
        Spacer(Modifier.width(px(10)))
        Text(label, style = style(24, FontWeight.Normal, White, 28), maxLines = maxLines)
    }
}

/** 설정 칩 아이콘 (시트·바람·핸들) */
@Composable
internal fun SettingChipIcon(@DrawableRes icon: Int) {
    Image(painterResource(icon), contentDescription = null, modifier = Modifier.size(px(24)))
}

/** 조명색 점 (설정 칩·탐색 컬렉션 카드) */
@Composable
internal fun ColorDot(color: Color) {
    Box(Modifier.size(px(18)).clip(CircleShape).background(color))
}

/** 1초에 한 바퀴 도는 각도(0~360). State 로 돌려줘 그리기 단계에서만 읽을 수 있게 한다 */
@Composable
internal fun rememberSpinAngle(label: String): State<Float> =
    rememberInfiniteTransition(label = label).animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1000, easing = LinearEasing)),
        label = label,
    )
