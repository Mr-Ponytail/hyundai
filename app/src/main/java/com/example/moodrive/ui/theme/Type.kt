package com.example.moodrive.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit

/** 앱 공통 글자 스타일 (Pretendard) */
internal fun text(
    size: TextUnit,
    weight: FontWeight,
    color: Color,
    lineHeight: TextUnit = TextUnit.Unspecified,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textAlign: TextAlign = TextAlign.Unspecified,
) = TextStyle(
    fontFamily = MoodriveFont,
    fontSize = size,
    fontWeight = weight,
    color = color,
    lineHeight = lineHeight,
    letterSpacing = letterSpacing,
    textAlign = textAlign,
)
