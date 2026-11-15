package com.example.moodrive.ui.theme

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.example.moodrive.R
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import kotlin.math.min

/** 홈 시안(2026-11) 기준 색. 앱 크롬(사이드바·주행 정보)은 무채색으로 유지하고 Scene 색은 콘텐츠 영역에만 쓴다. */
object MoodriveColors {
    val Background = Color(0xFF15161A)
    val Surface = Color(0xFF1B1C21)
    val SurfaceRaised = Color(0xFF1F2025)
    val Selected = Color(0xFF34353B)

    val TextPrimary = Color(0xFFFFFFFF)
    val TextSecondary = Color(0xFFE6E6E8)
    val TextTertiary = Color(0xFFA9AAB0)
    val TextMuted = Color(0xFF8A8B91)
    val Brand = Color(0xFF5B5D64)

    /** 홈 배경 세로 그라데이션 (Pleos 가이드 반영 시안) */
    val BackgroundTop = Color(0xFF16181F)
    val BackgroundBottom = Color(0xFF212329)
    /** 카드 하단 스크림·화살표 버튼 바탕 */
    val Scrim = Color(0xFF131417)
    /** 사진 카드 위아래 그림자. 알파는 카드마다 달라 호출부에서 copy 로 준다 */
    val CardScrim = Color(0xFF0D0D12)
    /** 팝업·편집 패널·만들기 카드 바탕 */
    val Panel = Color(0xFF1D1E21)
    /** 밝은 주 버튼 바탕 (완료·저장 등, 글자는 Scrim) */
    val ButtonLight = Color(0xFFE0E1E6)

    /** 켜짐·완료 초록 (정차 중 표시·스위치·적용 완료) */
    val Positive = Color(0xFF00E074)
    /** 녹음 중 점·종료 버튼 빨강 */
    val Danger = Color(0xFFFF4C28)

    val ParkedBg = Color(0x2400E074)
    val ParkedFg = Positive
    val DrivingBg = Color(0x800082FF)
    val DrivingFg = Color(0xFFFFFFFF)
    /** 안내 모드 등 주의 표시 */
    val Warn = Color(0xFFFBBF24)
}

/**
 * Scene 팔레트에서 강조색을 뽑는다 (선택 카드 테두리·글로우·적용 버튼).
 * 채도가 가장 높은 색을 고르고, 어두운 배경 위에서 보이도록 밝기를 올린다.
 */
fun accentOf(paletteArgb: List<Int>): Color {
    val hsv = FloatArray(3)
    val best = paletteArgb.maxByOrNull { argb ->
        android.graphics.Color.colorToHSV(argb, hsv)
        hsv[1] * hsv[2]
    } ?: return Color(0xFFD9A441)
    android.graphics.Color.colorToHSV(best, hsv)
    hsv[1] = hsv[1].coerceIn(0.35f, 0.7f)
    hsv[2] = hsv[2].coerceAtLeast(0.85f)
    return Color(android.graphics.Color.HSVToColor(hsv))
}

/** 강조색을 밝게 푼 색 (시안 #FFD6AA 계열) */
internal fun accentLight(accent: Color): Color = lerp(accent, Color.White, 0.62f)

/** 강조색을 살구색 쪽으로 푼 색 */
internal fun accentPale(accent: Color): Color = lerp(accent, Color.White, 0.47f)

/** 강조색 → 밝은 살구색 그라데이션 테두리 (홈 하단 정보 바·실행 화면 변경 카드) */
internal fun accentBorderBrush(accent: Color): Brush = Brush.linearGradient(
    0.06f to accent,
    0.625f to accentLight(accent).copy(alpha = 0.5f),
    1f to accentPale(accent),
)

/**
 * 시안 폰트 Pretendard. 코드에서 쓰는 굵기(Normal·Medium·SemiBold·Bold·Black)만 넣었다.
 * 다른 굵기를 쓰면 가까운 굵기로 대체되므로, 새 굵기가 필요하면 res/font 에 파일을 추가할 것.
 */
val MoodriveFont: FontFamily = FontFamily(
    Font(R.font.pretendard_regular, FontWeight.Normal),
    Font(R.font.pretendard_medium, FontWeight.Medium),
    Font(R.font.pretendard_semibold, FontWeight.SemiBold),
    Font(R.font.pretendard_bold, FontWeight.Bold),
    Font(R.font.pretendard_black, FontWeight.Black),
)

/** style 을 따로 주지 않은 Text·Material 컴포넌트도 Pretendard 로 나오게 기본 Typography 전체에 적용. */
private val MoodriveTypography: Typography = Typography().run {
    fun TextStyle.pretendard() = copy(fontFamily = MoodriveFont)
    copy(
        displayLarge = displayLarge.pretendard(),
        displayMedium = displayMedium.pretendard(),
        displaySmall = displaySmall.pretendard(),
        headlineLarge = headlineLarge.pretendard(),
        headlineMedium = headlineMedium.pretendard(),
        headlineSmall = headlineSmall.pretendard(),
        titleLarge = titleLarge.pretendard(),
        titleMedium = titleMedium.pretendard(),
        titleSmall = titleSmall.pretendard(),
        bodyLarge = bodyLarge.pretendard(),
        bodyMedium = bodyMedium.pretendard(),
        bodySmall = bodySmall.pretendard(),
        labelLarge = labelLarge.pretendard(),
        labelMedium = labelMedium.pretendard(),
        labelSmall = labelSmall.pretendard(),
    )
}

/** 시안 캔버스 크기(dp). 시안 이미지 2000×1392px의 절반 기준으로 레이아웃 값을 잡았다. */
val DesignWidth = 1000.dp
val DesignHeight = 696.dp

/**
 * 실제 화면 크기에 맞춰 시안 비율대로 전체를 확대/축소한다.
 * 차량 디스플레이마다 앱 영역 크기가 달라도 시안과 같은 구성으로 보이게 하기 위함.
 */
@Composable
fun DesignScale(content: @Composable () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val scale = min(maxWidth / DesignWidth, maxHeight / DesignHeight)
        val base = LocalDensity.current
        CompositionLocalProvider(LocalDensity provides Density(base.density * scale, base.fontScale)) {
            content()
        }
    }
}

@Composable
fun MoodriveTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            background = MoodriveColors.Background,
            surface = MoodriveColors.Surface,
            onBackground = MoodriveColors.TextPrimary,
            onSurface = MoodriveColors.TextPrimary,
        ),
        typography = MoodriveTypography,
        content = content,
    )
}
