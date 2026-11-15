package com.example.moodrive.ui.control

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.moodrive.R
import com.example.moodrive.ui.common.fig

/** 카드·패널 오른쪽에 붙는 ‹ 탭 (플레이리스트 접기, 무드 수정 닫기) */
@Composable
internal fun SideTab(contentDescription: String, bordered: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(topEnd = fig(16), bottomEnd = fig(16))
    Box(
        modifier
            .size(fig(74), fig(80))
            .clip(shape)
            .background(PLAYLIST_BG)
            .then(if (bordered) Modifier.border(1.dp, Color.White.copy(alpha = 0.10f), shape) else Modifier)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(R.drawable.ic_chevron_left), contentDescription = contentDescription, tint = Color.White, modifier = Modifier.size(fig(30)))
    }
}
