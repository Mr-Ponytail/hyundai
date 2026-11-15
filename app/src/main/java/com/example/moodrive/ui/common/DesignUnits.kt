package com.example.moodrive.ui.common

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.moodrive.ui.theme.text

/*
 * 새 무드 만들기·탐색 시안(1700×1184px 프레임) 단위.
 * DesignScale 캔버스(1000×696dp)에 맞추려고 시안 px 값을 1.7 로 나눠 쓴다.
 * 실행 화면(control)은 시안 기준이 달라 fig() 를 따로 쓴다.
 */

internal const val FIGMA_PX_PER_DP = 1.7f

internal fun px(value: Number): Dp = (value.toFloat() / FIGMA_PX_PER_DP).dp
internal fun spOf(value: Number): TextUnit = (value.toFloat() / FIGMA_PX_PER_DP).sp

/**
 * 시안(Figma) 수치 → dp. 시안 프레임 단위와 실행 화면 캔버스(1000dp) 비율.
 * 플레이리스트 카드 폭 793이 시안 이미지에서 잰 467dp와 같도록 맞췄다.
 */
internal fun fig(value: Int): Dp = (value * 467f / 793f).dp

/** 시안 px 기준 글자 스타일. 정렬 기본값은 Start (text() 의 Unspecified 와 다름) */
internal fun style(size: Number, weight: FontWeight, color: Color, lineHeight: Number, align: TextAlign = TextAlign.Start) =
    text(spOf(size), weight, color, lineHeight = spOf(lineHeight), textAlign = align)

internal val White = Color.White
internal val White64 = White.copy(alpha = 0.64f)
internal val White84 = White.copy(alpha = 0.84f)
