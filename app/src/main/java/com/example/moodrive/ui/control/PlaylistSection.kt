package com.example.moodrive.ui.control

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.moodrive.R
import com.example.moodrive.domain.model.Playlist
import com.example.moodrive.ui.common.fig
import com.example.moodrive.ui.common.paletteBrush
import com.example.moodrive.ui.theme.text
import com.example.moodrive.ui.theme.MoodriveColors

// ───────────────────────── 추천 플레이리스트 ─────────────────────────

/**
 * 추천 플레이리스트. 펼친 카드 ↔ 접힌 알약 버튼.
 * 같은 배경 상자가 크기를 바꾸며 접히고(왼쪽 위 기준), 내용은 그 사이에 교차 페이드된다.
 */
@Composable
internal fun PlaylistSection(state: SceneControlUiState, onOpenUrl: (String) -> Unit, onMore: () -> Unit, modifier: Modifier = Modifier) {
    if (state.playlists.isEmpty()) return
    var expanded by rememberSaveable { mutableStateOf(true) }
    // 펼친 카드(32) ↔ 접힌 알약(20) 모서리를 함께 바꾼다
    val radius by animateDpAsState(if (expanded) fig(32) else fig(20), tween(PLAYLIST_MORPH_MS), label = "playlistRadius")
    val shape = RoundedCornerShape(radius)

    Row(modifier, verticalAlignment = Alignment.Top) {
        Box(
            Modifier
                .clip(shape)
                .background(PLAYLIST_BG)
                .border(1.dp, Color.White.copy(alpha = 0.10f), shape),
        ) {
            AnimatedContent(
                targetState = expanded,
                contentAlignment = Alignment.TopStart,
                transitionSpec = {
                    (fadeIn(tween(220, delayMillis = PLAYLIST_MORPH_MS / 2)) togetherWith fadeOut(tween(120))) using
                        SizeTransform(clip = true) { _, _ -> tween(PLAYLIST_MORPH_MS, easing = FastOutSlowInEasing) }
                },
                label = "playlist",
            ) { open ->
                if (open) PlaylistCard(state, onOpenUrl, onMore) else PlaylistPill(onClick = { expanded = true })
            }
        }
        // 카드 오른쪽에 붙은 접기 탭 (접힌 상태에선 알약 자체가 펼치기 버튼)
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(tween(200, delayMillis = PLAYLIST_MORPH_MS)),
            exit = fadeOut(tween(100)),
        ) {
            SideTab("플레이리스트 접기", bordered = false, onClick = { expanded = false }, modifier = Modifier.padding(top = fig(28)))
        }
    }
}

@Composable
private fun PlaylistCard(state: SceneControlUiState, onOpenUrl: (String) -> Unit, onMore: () -> Unit) {
    Box(Modifier.width(fig(793)).aspectRatio(793f / 326f)) {
        // 왼쪽: 제목 + 플레이리스트 2개
        Row(Modifier.padding(start = fig(44), top = fig(34)), verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(R.drawable.ic_music), null, tint = Color.White.copy(alpha = 0.64f), modifier = Modifier.size(fig(32)))
            Spacer(Modifier.width(fig(8)))
            Text("무드에 맞는 추천 플레이리스트", style = text(14.sp, FontWeight.Normal, Color.White.copy(alpha = 0.64f), lineHeight = 16.5.sp))
        }
        state.playlists.take(2).forEachIndexed { i, p ->
            PlaylistRow(
                p, state.paletteArgb,
                onClick = { onOpenUrl(p.url) },
                modifier = Modifier.padding(start = fig(56), top = fig(101 + i * 99)).width(fig(556 - 56 - 16)),
            )
        }
        // 오른쪽: Spotify · 더 보러가기
        Box(
            Modifier
                .align(Alignment.CenterEnd)
                .width(fig(237))
                .fillMaxHeight()
                .background(Color.White.copy(alpha = 0.12f)),
        ) {
            SpotifyLogo(Modifier.align(Alignment.TopCenter).padding(top = fig(91)))
            Box(
                Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = fig(183))
                    .size(fig(171), fig(80))
                    .clip(RoundedCornerShape(fig(24)))
                    .background(Color.White.copy(alpha = 0.2f))
                    .clickable(onClick = onMore),
                contentAlignment = Alignment.Center,
            ) {
                Text("더 보러가기", style = text(15.sp, FontWeight.Normal, Color.White))
            }
        }
    }
}

/** 접힌 상태: [Spotify  추천 플레이 리스트 보기  ›] */
@Composable
private fun PlaylistPill(onClick: () -> Unit) {
    Row(
        Modifier
            .height(fig(74))
            .clickable(onClick = onClick)
            .padding(start = fig(22), end = fig(22)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SpotifyLogo()
        Spacer(Modifier.width(fig(12)))
        Text("추천 플레이 리스트 보기", style = text(12.sp, FontWeight.Normal, Color.White), maxLines = 1)
        Spacer(Modifier.width(fig(12)))
        Icon(
            painterResource(R.drawable.ic_chevron_left),
            contentDescription = "플레이리스트 펼치기",
            tint = Color.White,
            modifier = Modifier.size(fig(30)).scale(scaleX = -1f, scaleY = 1f),
        )
    }
}

@Composable
private fun PlaylistRow(playlist: Playlist, palette: List<Int>, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.clip(RoundedCornerShape(fig(8))).clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(fig(75)).clip(RoundedCornerShape(fig(8))).background(paletteBrush(palette)), contentAlignment = Alignment.Center) {
            Icon(painterResource(R.drawable.ic_m_music_note), null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(fig(34)))
            playlist.imageUrl?.let { AsyncImage(it, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
        }
        Spacer(Modifier.width(fig(18)))
        Column(Modifier.weight(1f)) {
            Text(playlist.title, style = text(14.sp, FontWeight.Normal, Color.White), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(playlist.subtitle, style = text(11.5.sp, FontWeight.Normal, Color.White.copy(alpha = 0.5f)))
        }
    }
}

/** Spotify 로고 (시안 이미지 140×38) */
@Composable
private fun SpotifyLogo(modifier: Modifier = Modifier) {
    Image(painterResource(R.drawable.img_spotify_logo), contentDescription = "Spotify", modifier = modifier.size(fig(140), fig(38)))
}

/** 플레이리스트 카드·접기 탭 배경 (무드 수정 닫기 탭도 같은 색) */
internal val PLAYLIST_BG = MoodriveColors.Scrim.copy(alpha = 0.64f)
private const val PLAYLIST_MORPH_MS = 380
