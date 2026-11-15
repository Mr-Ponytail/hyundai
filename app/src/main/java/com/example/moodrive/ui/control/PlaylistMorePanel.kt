package com.example.moodrive.ui.control

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.moodrive.R
import com.example.moodrive.domain.model.Playlist
import com.example.moodrive.ui.common.clickableNoRipple
import com.example.moodrive.ui.common.paletteBrush
import com.example.moodrive.ui.theme.text

// ───────────────────────── 추천 플레이리스트 더 보기 ─────────────────────────

/**
 * "더 보러가기"로 여는 오른쪽 패널 (시안 780:47541 "가을 분위기 · 검색 UI", 840×1184).
 * 검색창은 입력을 받지 않는다(주행 중 텍스트 입력 금지). 누르면 음악 서비스 검색을 연다.
 * ⊕(라이브러리에 추가)는 아직 서비스 연동 전이라 화면에서만 체크 표시로 바뀐다.
 */
@Composable
internal fun PlaylistMorePanel(
    query: String,
    playlists: List<Playlist>,
    palette: List<Int>,
    onSearch: () -> Unit,
    onOpen: (Playlist) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var cleared by rememberSaveable { mutableStateOf(false) }
    var added by rememberSaveable { mutableStateOf(emptyList<Int>()) }
    val shape = RoundedCornerShape(f(28.47f))

    Column(
        modifier
            .width(f(840f))
            .fillMaxHeight()
            .clip(shape)
            .background(PANEL_BG)
            // 패널 안 빈 곳을 눌러도 뒤의 닫기 영역으로 넘어가지 않게 한다
            .clickableNoRipple {},
    ) {
        // 검색창 · 취소
        Row(Modifier.padding(start = f(30f), top = f(34f)), verticalAlignment = Alignment.CenterVertically) {
            Row(
                Modifier
                    .size(f(673f), f(59f))
                    .clip(PILL)
                    .background(FIELD_BG)
                    .border(1.dp, FIELD_STROKE, PILL)
                    .clickable(onClick = onSearch)
                    .padding(start = f(23f), end = f(11f)),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    if (cleared) "검색" else query,
                    style = text(s(28.47f), FontWeight.Normal, Color.White.copy(alpha = if (cleared) 0.4f else 0.84f)),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (!cleared) {
                    Box(
                        Modifier.size(f(32f)).clip(CircleShape).clickable { cleared = true },
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(Modifier.size(f(28f)).clip(CircleShape).background(CLEAR_BG), contentAlignment = Alignment.Center) {
                            Icon(painterResource(R.drawable.ic_close), contentDescription = "검색어 지우기", tint = FIELD_BG, modifier = Modifier.size(f(16f)))
                        }
                    }
                }
            }
            Spacer(Modifier.width(f(12f)))
            Box(
                Modifier
                    .size(f(86f), f(59f))
                    .clip(PILL)
                    .background(FIELD_BG)
                    .border(1.dp, FIELD_STROKE, PILL)
                    .clickable(onClick = onClose),
                contentAlignment = Alignment.Center,
            ) {
                Text("취소", style = text(s(23.13f), FontWeight.Normal, Color.White.copy(alpha = 0.84f)))
            }
        }

        // 목록: 첫 행 top 123, 행 간격 119 (아트워크 89 + 여백 30)
        Column(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(top = f(30f), bottom = f(19f)),
            verticalArrangement = Arrangement.spacedBy(f(30f)),
        ) {
            playlists.forEachIndexed { i, p ->
                MoreRow(
                    playlist = p,
                    palette = palette,
                    added = i in added,
                    onClick = { onOpen(p) },
                    onToggleAdd = { added = if (i in added) added - i else added + i },
                )
            }
        }
    }
}

@Composable
private fun MoreRow(playlist: Playlist, palette: List<Int>, added: Boolean, onClick: () -> Unit, onToggleAdd: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(f(89f))
            .clickable(onClick = onClick)
            .padding(start = f(30f), end = f(39f)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(f(89f)).clip(RoundedCornerShape(f(8f))).background(paletteBrush(palette)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(R.drawable.ic_m_music_note), null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(f(40f)))
            playlist.imageUrl?.let { AsyncImage(it, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
        }
        Spacer(Modifier.width(f(22f)))
        Column(Modifier.weight(1f)) {
            Text(
                playlist.title,
                style = text(s(28.47f), FontWeight.Normal, Color.White.copy(alpha = 0.84f), lineHeight = s(35.58f)),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(f(5f)))
            Text(playlist.subtitle, style = text(s(23.13f), FontWeight.Normal, Color.White.copy(alpha = 0.64f), lineHeight = s(28.91f)))
        }
        Spacer(Modifier.width(f(40f)))
        // 라이브러리에 추가 (36×36, 원 테두리 28)
        Box(
            Modifier.size(f(36f)).clip(CircleShape).clickable(onClick = onToggleAdd),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier
                    .size(f(28f))
                    .clip(CircleShape)
                    .then(if (added) Modifier.background(Color.White) else Modifier.border(1.5.dp, Color.White.copy(alpha = 0.84f), CircleShape)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painterResource(if (added) R.drawable.ic_check else R.drawable.ic_plus),
                    contentDescription = if (added) "라이브러리에서 빼기" else "라이브러리에 추가",
                    tint = if (added) PANEL_BG else Color.White.copy(alpha = 0.84f),
                    modifier = Modifier.size(f(18f)),
                )
            }
        }
    }
}

/** 시안 px(소수 포함) → dp. 실행 화면 [com.example.moodrive.ui.common.fig]와 같은 비율 */
private fun f(value: Float): Dp = (value * 467f / 793f).dp
private fun s(value: Float): TextUnit = (value * 467f / 793f).sp

private val PANEL_BG = Color(0xFF111111)
private val FIELD_BG = Color(0xFF212121)
private val FIELD_STROKE = Color(0xFF363636)
private val CLEAR_BG = Color(0xFFB4B4B4)
private val PILL = RoundedCornerShape(50)
