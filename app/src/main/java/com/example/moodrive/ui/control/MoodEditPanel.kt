package com.example.moodrive.ui.control

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import com.example.moodrive.ui.theme.MoodriveColors
import com.example.moodrive.R
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.moodrive.domain.model.FanDirection
import com.example.moodrive.ui.common.consumeTaps
import com.example.moodrive.ui.common.fig
import com.example.moodrive.ui.theme.text
import com.example.moodrive.ui.mood.EditableName
import com.example.moodrive.ui.mood.MoodDraft
import com.example.moodrive.ui.mood.MoodEditSections
import com.example.moodrive.ui.mood.SeatMode
import com.example.moodrive.ui.theme.MoodriveTheme

data class MoodEditActions(
    val onChange: (MoodDraft) -> Unit = {},
    val onClose: () -> Unit = {},
    val onSave: () -> Unit = {},
    val onEyedropper: () -> Unit = {},
)

/**
 * 무드 수정 (시안 2026-11). 왼쪽에서 나오는 패널.
 * 섹션은 스크롤되고 "무드에 저장" 버튼은 하단에 고정된다.
 */
@Composable
fun MoodEditPanel(draft: MoodDraft, saving: Boolean, actions: MoodEditActions, modifier: Modifier = Modifier) {
    val change = actions.onChange
    Row(modifier.fillMaxHeight(), verticalAlignment = Alignment.Top) {
        Box(
            Modifier
                .width(PANEL_WIDTH)
                .fillMaxHeight()
                .clip(RoundedCornerShape(topEnd = fig(40)))
                .background(PANEL_BG)
                // 패널 안 빈 곳을 눌러도 뒤로 전달되지 않게
                .consumeTaps(),
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(start = fig(60), end = fig(58), top = fig(60), bottom = SAVE_AREA),
            ) {
                TitleRow(draft.name) { change(draft.copy(name = it)) }
                Spacer(Modifier.height(fig(34)))

                MoodEditSections(draft, change, actions.onEyedropper)
            }

            // 하단 고정 저장 버튼 (위로 스크롤되는 내용이 자연스럽게 가려지도록 그라데이션)
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(SAVE_AREA)
                    .background(Brush.verticalGradient(0f to PANEL_BG.copy(alpha = 0f), 0.35f to PANEL_BG)),
            ) {
                Box(
                    Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = fig(48), bottom = fig(61))
                        .width(fig(693))
                        .height(fig(100))
                        .alpha(if (saving) 0.6f else 1f)
                        .clip(RoundedCornerShape(fig(32)))
                        .background(Color.White)
                        .clickable(enabled = !saving, onClick = actions.onSave),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(if (saving) "저장 중" else "무드에 저장", style = text(18.5.sp, FontWeight.Bold, MoodriveColors.Scrim))
                }
            }
        }
        // 패널 오른쪽에 붙은 닫기 탭 (플레이리스트 접기 탭과 같은 모양)
        SideTab("무드 수정 닫기", bordered = true, onClick = actions.onClose, modifier = Modifier.padding(top = fig(66)))
    }
}

// ───────────────────────── 제목 ─────────────────────────

@Composable
private fun TitleRow(name: String, onRename: (String) -> Unit) {
    var editing by remember { mutableStateOf(false) }
    EditableName(
        name = name,
        onRename = onRename,
        editing = editing,
        onEditingChange = { editing = it },
        textStyle = text(23.5.sp, FontWeight.Bold, Color.White, lineHeight = 30.sp),
        height = fig(56),
        gap = fig(16),
        buttonSize = fig(56),
        buttonCorner = fig(12),
        buttonRestAlpha = 0.1f,
    ) { description ->
        Icon(painterResource(R.drawable.ic_pencil), contentDescription = description, tint = Color.White, modifier = Modifier.size(fig(56)))
    }
}

private val PANEL_WIDTH = 476.dp
/** 시안은 96%지만 뒤 실행 화면 글자가 비쳐 보여 불투명으로 둔다 */
private val PANEL_BG = MoodriveColors.Panel
private val SAVE_AREA = fig(224)

@Preview(widthDp = 1000, heightDp = 696)
@Composable
private fun MoodEditPanelPreview() {
    MoodriveTheme {
        MoodEditPanel(
            MoodDraft(
                name = "Cozy Fireplace", lightOn = true, mainColor = 0xFFFF9F43.toInt(), subColor = 0xFFFFE2B8.toInt(),
                brightnessPercent = 50, seatOn = true, seatMode = SeatMode.HEAT, seatLevel = 1,
                windOn = true, windDirection = FanDirection.FLOOR, fanLevel = 1, steeringOn = true,
            ),
            saving = false,
            actions = MoodEditActions(),
        )
    }
}
