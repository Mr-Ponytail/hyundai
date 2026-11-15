package com.example.moodrive.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.moodrive.R
import com.example.moodrive.domain.model.DriveMode
import com.example.moodrive.ui.common.DetailIcon
import com.example.moodrive.ui.common.DetailKind
import com.example.moodrive.ui.common.DriveStatusPill
import com.example.moodrive.ui.common.FavoriteToggle
import com.example.moodrive.ui.common.NavDestination
import com.example.moodrive.ui.common.SideBar
import com.example.moodrive.ui.common.SideBarPadding
import com.example.moodrive.ui.theme.accentBorderBrush
import com.example.moodrive.ui.theme.accentLight
import com.example.moodrive.ui.theme.accentPale
import com.example.moodrive.ui.common.clickableNoRipple
import com.example.moodrive.ui.common.paletteBrush
import com.example.moodrive.ui.common.rememberLastNonNull
import com.example.moodrive.ui.theme.text
import com.example.moodrive.ui.theme.MoodriveColors
import com.example.moodrive.ui.theme.MoodriveTheme
import com.example.moodrive.ui.theme.accentOf
import kotlinx.coroutines.launch

data class HomeActions(
    val onFilterSelected: (HomeFilter) -> Unit = {},
    val onSceneSelected: (String) -> Unit = {},
    val onDismissSelection: () -> Unit = {},
    val onApply: () -> Unit = {},
    val onEnd: () -> Unit = {},
    val onFineTune: () -> Unit = {},
    val onToggleFavorite: (String) -> Unit = {},
    val onNavigate: (NavDestination) -> Unit = {},
    val onCreate: () -> Unit = {},
)

@Composable
fun HomeScreen(state: HomeUiState, actions: HomeActions, modifier: Modifier = Modifier) {
    val selected = state.selected
    BackHandler(enabled = selected != null, onBack = actions.onDismissSelection)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(0f to MoodriveColors.BackgroundTop, 0.88f to MoodriveColors.BackgroundBottom))
            // 카드 밖을 누르면 선택 해제
            .clickableNoRipple(actions.onDismissSelection),
    ) {
        AmbientCanvas(selected?.card)

        Row(Modifier.fillMaxSize().padding(SideBarPadding)) {
            SideBar(
                handle = state.handle,
                avatarUrl = state.avatarUrl,
                avatarFallback = state.greetingName.take(1),
                // 홈은 항상 '내 Mood' 탭이다. 탭 전환은 MainActivity가 한다
                selected = NavDestination.MY_MOOD,
                canCreate = state.canCreate,
                onNavigate = actions.onNavigate,
                onCreate = actions.onCreate,
            )
            Box(Modifier.fillMaxSize()) {
                Column(Modifier.fillMaxSize().padding(start = 30.dp, top = 11.dp)) {
                    // 워드마크 원본 153×34 비율, 글자 높이가 기존 15sp 텍스트와 비슷하도록 높이 20dp
                    Image(painterResource(R.drawable.img_logo_moodrive), contentDescription = "Moodrive", modifier = Modifier.size(90.dp, 20.dp))
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "${state.greetingName}님, 무슨 바이브로 가볼까요?",
                        style = text(28.sp, FontWeight.Bold, MoodriveColors.TextPrimary, lineHeight = 38.sp),
                    )
                    Spacer(Modifier.height(15.dp))
                    FilterChips(state, actions.onFilterSelected, Modifier.padding(start = 6.dp))

                    // 선택 시 안내 문구는 접히고 카드 영역이 위로 올라온다
                    AnimatedVisibility(selected == null, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                        Column {
                            Spacer(Modifier.height(25.dp))
                            Text(
                                "원하는 차량 무드를 선택해주세요",
                                style = text(19.sp, FontWeight.Normal, MoodriveColors.TextPrimary, lineHeight = 24.sp),
                                modifier = Modifier.padding(start = 6.dp),
                            )
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                    SceneCarousel(state, actions)
                    DetailPanel(selected, Modifier.padding(start = 6.dp, end = 34.dp, top = 15.dp))
                }
                DriveStatusPill(state.driveMode, Modifier.align(Alignment.TopEnd).padding(top = 11.dp, end = 35.dp))
                // 하단 패널과 겹치지 않도록 주행 상태 표시 아래에 둔다
            }
        }
    }
}

// ───────────────────────── 배경 (Ambient Canvas) ─────────────────────────

/** 선택한 Scene의 이미지/색을 흐리게 깔아 화면 전체 분위기를 바꾼다 (기획서 3-3 Ambient Canvas) */
@Composable
private fun AmbientCanvas(card: SceneCardUi?) {
    Crossfade(targetState = card, animationSpec = tween(400), label = "ambient") { c ->
        if (c == null) return@Crossfade
        Box(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize().background(paletteBrush(c.paletteArgb)).blur(60.dp))
            if (c.imageUrl != null) {
                AsyncImage(c.imageUrl, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().blur(60.dp))
            }
            // 시안 'BG blur' 오버레이: 왼쪽 아래는 거의 검게, 오른쪽 위로 갈수록 이미지가 비친다
            Box(
                Modifier.fillMaxSize().drawBehind {
                    drawRect(
                        Brush.linearGradient(
                            listOf(Color(0xF003050A), Color(0x4D131417)),
                            start = Offset(size.width * 0.55f, size.height * 0.8f),
                            end = Offset(size.width * 0.8f, 0f),
                        ),
                    )
                },
            )
        }
    }
}

// ───────────────────────── 상단 ─────────────────────────

@Composable
private fun FilterChips(state: HomeUiState, onSelected: (HomeFilter) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Chip("전체 ${state.totalCount}", state.filter == HomeFilter.ALL) { onSelected(HomeFilter.ALL) }
        Chip("최근 실행", state.filter == HomeFilter.RECENT) { onSelected(HomeFilter.RECENT) }
        Chip("즐겨찾기 ${state.favoriteCount}", state.filter == HomeFilter.FAVORITE) { onSelected(HomeFilter.FAVORITE) }
    }
}

@Composable
private fun Chip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .height(38.dp)
            .clip(CircleShape)
            .background(if (selected) Color.White.copy(alpha = 0.16f) else Color.Transparent)
            .then(if (selected) Modifier else Modifier.border(1.dp, Color.White.copy(alpha = 0.14f), CircleShape))
            .clickable(onClick = onClick)
            .padding(horizontal = 15.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = text(14.sp, if (selected) FontWeight.SemiBold else FontWeight.Normal, if (selected) Color.White else Color.White.copy(alpha = 0.72f)),
        )
    }
}

// ───────────────────────── 카드 목록 ─────────────────────────

@Composable
private fun SceneCarousel(state: HomeUiState, actions: HomeActions) {
    val cards = state.cards
    val selectedId = state.selected?.card?.id
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // 탭을 바꾸거나, 방금 실행한 Scene이 최근 목록 맨 앞으로 올라오면 처음으로 이동
    LaunchedEffect(state.filter, cards.firstOrNull()?.id) { listState.animateScrollToItem(0) }
    // 선택한 카드가 잘리지 않도록, 앞 카드 하나가 보이는 위치로 이동 (시안 배치)
    LaunchedEffect(selectedId) {
        val index = cards.indexOfFirst { it.id == selectedId }
        if (index >= 0) listState.animateScrollToItem((index - 1).coerceAtLeast(0))
    }

    if (cards.isEmpty()) {
        EmptyCards(state.filter)
        return
    }

    val rowHeight by animateDpAsState(if (selectedId != null) SELECTED_HEIGHT else CARD_HEIGHT, label = "rowHeight")
    Box(Modifier.fillMaxWidth().height(rowHeight)) {
        LazyRow(
            state = listState,
            horizontalArrangement = Arrangement.spacedBy(30.dp),
            verticalAlignment = Alignment.CenterVertically,
            // LazyRow는 자기 영역 밖을 잘라 그리므로, 선택 카드 그림자가 잘리지 않게 영역을 바깥으로 넓히고
            // 넓힌 만큼 안쪽 여백을 줘서 카드 위치는 그대로 둔다
            contentPadding = PaddingValues(start = 6.dp + SHADOW_BLEED_START, end = 120.dp, top = SHADOW_BLEED_TOP, bottom = SHADOW_BLEED_BOTTOM),
            modifier = Modifier.fillMaxSize().bleed(SHADOW_BLEED_START, SHADOW_BLEED_TOP, SHADOW_BLEED_BOTTOM),
        ) {
            items(cards, key = { it.id }) { card ->
                val isSelected = card.id == selectedId
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SceneCard(
                        card = card,
                        selected = isSelected,
                        dimmed = selectedId != null && !isSelected,
                        onClick = { actions.onSceneSelected(card.id) },
                        onToggleFavorite = { actions.onToggleFavorite(card.id) },
                    )
                    AnimatedVisibility(
                        visible = isSelected,
                        enter = fadeIn(tween(250, delayMillis = 100)) + expandHorizontally(expandFrom = Alignment.Start),
                        exit = fadeOut(tween(150)) + shrinkHorizontally(shrinkTowards = Alignment.Start),
                    ) {
                        Row {
                            Spacer(Modifier.width(14.dp))
                            ActionButtons(
                                card = card,
                                busy = state.isApplying,
                                canFineTune = state.driveMode != DriveMode.DRIVING,
                                onApply = actions.onApply,
                                onEnd = actions.onEnd,
                                onFineTune = actions.onFineTune,
                            )
                        }
                    }
                }
            }
        }
        // 양쪽에 더 볼 카드가 있을 때만 화살표 표시 (선택 상태에서도 유지)
        AnimatedVisibility(
            visible = listState.canScrollBackward,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.CenterStart),
        ) {
            ArrowButton(forward = false, onClick = { scope.launch { listState.scrollToPreviousPage() } })
        }
        AnimatedVisibility(
            visible = listState.canScrollForward,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 27.dp),
        ) {
            ArrowButton(forward = true, onClick = { scope.launch { listState.scrollToNextPage() } })
        }
    }
}

/** 부모 크기는 그대로 차지하면서, 실제 그리는 영역만 왼쪽·위·아래로 넓힌다 */
private fun Modifier.bleed(start: Dp, top: Dp, bottom: Dp) = layout { measurable, constraints ->
    val s = start.roundToPx()
    val t = top.roundToPx()
    val b = bottom.roundToPx()
    val placeable = measurable.measure(
        constraints.copy(
            minWidth = constraints.minWidth + s,
            maxWidth = constraints.maxWidth + s,
            minHeight = constraints.minHeight + t + b,
            maxHeight = constraints.maxHeight + t + b,
        ),
    )
    layout(constraints.maxWidth, constraints.maxHeight) { placeable.place(-s, -t) }
}

// 그림자(28dp)가 퍼질 여유. 위쪽은 필터 칩 터치를 가리지 않도록 카드 위 간격(24dp)보다 작게 둔다
private val SHADOW_BLEED_START = 30.dp
private val SHADOW_BLEED_TOP = 20.dp
private val SHADOW_BLEED_BOTTOM = 40.dp

/** 화면에 완전히 보이는 카드 수 (최소 1) */
private fun LazyListState.fullyVisibleCount(): Int {
    val info = layoutInfo
    return info.visibleItemsInfo.count { it.offset >= 0 && it.offset + it.size <= info.viewportEndOffset }.coerceAtLeast(1)
}

/** 보이는 카드 수만큼 다음으로 넘긴다 */
private suspend fun LazyListState.scrollToNextPage() {
    val target = (firstVisibleItemIndex + fullyVisibleCount()).coerceAtMost(layoutInfo.totalItemsCount - 1)
    animateScrollToItem(target)
}

/** 보이는 카드 수만큼 이전으로 넘긴다. 첫 카드가 일부만 보이는 상태면 그 카드부터 보이게 한다 */
private suspend fun LazyListState.scrollToPreviousPage() {
    val partiallyHidden = firstVisibleItemScrollOffset > 0
    val step = fullyVisibleCount() - if (partiallyHidden) 1 else 0
    animateScrollToItem((firstVisibleItemIndex - step).coerceAtLeast(0))
}

@Composable
private fun SceneCard(
    card: SceneCardUi,
    selected: Boolean,
    dimmed: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    val accent = remember(card.paletteArgb) { accentOf(card.paletteArgb) }
    val width by animateDpAsState(if (selected) SELECTED_WIDTH else CARD_WIDTH, label = "cardWidth")
    val height by animateDpAsState(if (selected) SELECTED_HEIGHT else CARD_HEIGHT, label = "cardHeight")
    val dim by animateFloatAsState(if (dimmed) 0.6f else 0f, label = "dim")
    val shape = RoundedCornerShape(14.dp)

    Box(
        modifier = Modifier
            .width(width)
            .height(height)
            .then(if (selected) Modifier.shadow(28.dp, shape, ambientColor = accent, spotColor = accent) else Modifier)
            .clip(shape)
            .background(paletteBrush(card.paletteArgb))
            .border(
                when {
                    // 시안: 밝은 살구색 → 강조색으로 흐르는 그라데이션 테두리
                    selected -> BorderStroke(
                        1.5.dp,
                        Brush.linearGradient(0f to accentPale(accent), 0.375f to accentLight(accent), 0.94f to accent),
                    )
                    card.isActive -> BorderStroke(2.dp, Color.White)
                    else -> BorderStroke(1.dp, Color.White.copy(alpha = 0.10f))
                },
                shape,
            )
            .clickable(onClick = onClick),
    ) {
        if (card.imageUrl != null) {
            AsyncImage(card.imageUrl, card.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        }
        if (selected) {
            // 선택 카드: 가장자리를 살짝 어둡게 (시안 비네트)
            Box(
                Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.03f)).drawBehind {
                    drawRect(
                        Brush.radialGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.5f)),
                            center = Offset(size.width * 0.5f, size.height * 0.455f),
                            radius = size.height * 0.545f,
                        ),
                    )
                },
            )
        }
        // 하단 텍스트 가독성용 스크림
        Box(
            Modifier.fillMaxSize().background(
                if (selected) {
                    Brush.verticalGradient(0.36f to MoodriveColors.Scrim.copy(alpha = 0f), 0.95f to MoodriveColors.Scrim.copy(alpha = 0.97f))
                } else {
                    Brush.verticalGradient(0.43f to MoodriveColors.Scrim.copy(alpha = 0f), 1f to MoodriveColors.Scrim.copy(alpha = 0.95f))
                },
            ),
        )

        card.badge?.let {
            Text(
                it,
                style = text(12.sp, FontWeight.Normal, Color.White.copy(alpha = 0.84f), lineHeight = 14.sp),
                modifier = Modifier
                    .padding(if (selected) 14.dp else 9.dp)
                    .clip(CircleShape)
                    .background(MoodriveColors.Scrim.copy(alpha = 0.66f))
                    .padding(horizontal = 9.dp, vertical = 6.dp),
            )
        }

        if (selected) {
            FavoriteToggle(
                card.isFavorite, onToggleFavorite,
                size = 38.dp, cornerRadius = 9.dp, background = MoodriveColors.Scrim.copy(alpha = 0.64f),
                filledStarSize = 19.dp, outlineStarSize = 21.dp,
                modifier = Modifier.align(Alignment.TopEnd).padding(14.dp),
            )
        }

        Column(Modifier.align(Alignment.BottomStart).padding(start = if (selected) 14.dp else 16.dp, end = 16.dp, bottom = if (selected) 31.dp else 18.dp)) {
            Text(
                card.title,
                style = text(if (selected) 23.5.sp else 18.sp, FontWeight.Bold, MoodriveColors.TextPrimary, lineHeight = if (selected) 30.sp else 22.sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(if (selected) 6.dp else 5.dp))
            Text(
                card.tags,
                style = text(14.sp, FontWeight.Normal, Color.White.copy(alpha = 0.64f), lineHeight = 16.sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        // 선택되지 않은 카드는 어둡게
        if (dim > 0f) Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = dim)))
    }
}

@Composable
private fun ActionButtons(
    card: SceneCardUi,
    busy: Boolean,
    canFineTune: Boolean,
    onApply: () -> Unit,
    onEnd: () -> Unit,
    onFineTune: () -> Unit,
) {
    val accent = remember(card.paletteArgb) { accentOf(card.paletteArgb) }
    val shape = RoundedCornerShape(14.dp)
    val labelStyle = text(16.5.sp, FontWeight.Medium, Color.White, lineHeight = 21.sp)
    Column(Modifier.width(110.dp).height(SELECTED_HEIGHT)) {
        // 적용하기 / (실행 중이면) 종료하기
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(shape)
                .background(accent.copy(alpha = 0.2f))
                // 시안: 왼쪽 위는 밝고 오른쪽 아래로 갈수록 회색인 유리 테두리
                .border(1.dp, Brush.linearGradient(0.19f to Color.White.copy(alpha = 0.5f), 0.44f to Color(0x26CCCCCC), 1f to Color(0x80999999)), shape)
                .clickable(enabled = !busy, onClick = if (card.isActive) onEnd else onApply),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.alpha(if (busy) 0.5f else 1f)) {
                if (card.isActive) {
                    Icon(painterResource(R.drawable.ic_m_power_settings_new), contentDescription = null, tint = Color.White, modifier = Modifier.size(23.5.dp))
                } else {
                    Icon(painterResource(R.drawable.ic_check), contentDescription = null, tint = Color.White, modifier = Modifier.size(23.5.dp))
                }
                Spacer(Modifier.height(5.dp))
                Text(
                    when {
                        busy -> "처리 중"
                        card.isActive -> "종료하기"
                        else -> "적용하기"
                    },
                    style = labelStyle,
                )
            }
        }
        Spacer(Modifier.height(11.dp))
        // 세부 조절
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(95.dp)
                .alpha(if (canFineTune) 1f else 0.4f)
                .clip(shape)
                .background(Color.White.copy(alpha = 0.2f))
                .border(1.dp, Color(0x4DE7E7E7), shape)
                .clickable(enabled = canFineTune, onClick = onFineTune),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(painterResource(R.drawable.ic_slider), contentDescription = null, tint = Color.White, modifier = Modifier.size(23.5.dp))
                Spacer(Modifier.height(5.dp))
                Text("세부 조절", style = labelStyle)
            }
        }
    }
}

@Composable
private fun ArrowButton(forward: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(65.dp)
            .clip(CircleShape)
            .background(MoodriveColors.Scrim.copy(alpha = 0.84f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        // 시안 화살표(오른쪽)를 뒤집어 이전 버튼에도 쓴다
        Icon(
            painterResource(R.drawable.ic_arrow_right),
            contentDescription = if (forward) "다음" else "이전",
            tint = Color.White,
            modifier = Modifier.size(38.dp).then(if (forward) Modifier else Modifier.scale(scaleX = -1f, scaleY = 1f)),
        )
    }
}

@Composable
private fun EmptyCards(filter: HomeFilter) {
    val msg = when (filter) {
        HomeFilter.ALL -> "아직 저장한 무드가 없어요. + 버튼으로 만들어 보세요"
        HomeFilter.RECENT -> "최근 실행한 무드가 없어요"
        HomeFilter.FAVORITE -> "즐겨찾기한 무드가 없어요"
    }
    Box(Modifier.fillMaxWidth().height(CARD_HEIGHT).padding(start = 6.dp), contentAlignment = Alignment.CenterStart) {
        Text(msg, style = text(16.sp, FontWeight.Normal, MoodriveColors.TextTertiary))
    }
}

// ───────────────────────── 하단 정보 패널 ─────────────────────────

@Composable
private fun DetailPanel(selected: SelectedSceneUi?, modifier: Modifier = Modifier) {
    // 닫히는 애니메이션 동안 마지막 내용을 유지
    val shown = rememberLastNonNull(selected) ?: return

    AnimatedVisibility(
        visible = selected != null,
        enter = fadeIn(tween(250, delayMillis = 120)) + slideInVertically { it / 3 },
        exit = fadeOut(tween(150)) + slideOutVertically { it / 3 },
        modifier = modifier,
    ) {
        val accent = remember(shown.card.paletteArgb) { accentOf(shown.card.paletteArgb) }
        val shape = RoundedCornerShape(14.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(92.dp)
                .clip(shape)
                .background(Color.White.copy(alpha = 0.05f))
                // 시안: 강조색 → 밝은 살구색 그라데이션 테두리
                .border(1.dp, accentBorderBrush(accent), shape)
                .padding(top = 13.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            shown.details.forEachIndexed { i, item ->
                if (i > 0) Box(Modifier.width(1.dp).height(56.dp).background(Color.White.copy(alpha = 0.10f)))
                DetailCell(item, accent)
            }
        }
    }
}

@Composable
private fun RowScope.DetailCell(item: DetailItemUi, accent: Color) {
    val sub = Color.White.copy(alpha = 0.64f)
    Column(Modifier.weight(1f).fillMaxHeight().padding(start = 21.dp, end = 18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            DetailIcon(item.kind, if (item.kind == DetailKind.AMBIENT) accent else Color.White, size = 19.dp)
            Spacer(Modifier.width(5.dp))
            Text(item.kind.label, style = text(12.sp, FontWeight.Normal, sub), modifier = Modifier.weight(1f))
            Text(
                item.current,
                style = text(12.sp, FontWeight.Normal, sub, lineHeight = 14.sp),
                maxLines = 1,
                modifier = Modifier
                    .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                    .padding(horizontal = 9.dp, vertical = 3.5.dp),
            )
        }
        Spacer(Modifier.weight(1f))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(item.value, style = text(28.sp, FontWeight.Bold, Color.White, lineHeight = 38.sp))
            item.unit?.let {
                Spacer(Modifier.width(5.dp))
                Text(it, style = text(14.sp, FontWeight.Normal, sub, lineHeight = 16.sp), modifier = Modifier.padding(bottom = 7.dp))
            }
        }
    }
}

// ───────────────────────── 공통 ─────────────────────────

private val CARD_WIDTH = 176.dp
private val CARD_HEIGHT = 341.dp
private val SELECTED_WIDTH = 267.dp
private val SELECTED_HEIGHT = 388.dp

// ───────────────────────── 프리뷰 ─────────────────────────

private val previewCards = listOf(
    SceneCardUi("1", "Lazy Sunday", "Soft · Cozy", null, listOf(0xFFB08A5E.toInt(), 0xFF5A4636.toInt()), "1시간 전", false),
    SceneCardUi("2", "Cozy Fireplace", "Vintage · Warm", null, listOf(0xFF8E2A22.toInt(), 0xFFD9A441.toInt(), 0xFF2B1A14.toInt()), "1일 전", false, true),
    SceneCardUi("3", "Blue Hour", "Dreamy · Calm", null, listOf(0xFF1B2A4A.toInt(), 0xFF3D5A8C.toInt()), "2일 전", false),
    SceneCardUi("4", "Neon Night", "Vivid · Urban", null, listOf(0xFF3A0F2A.toInt(), 0xFFE0408F.toInt()), "4일 전", false),
)

private val previewState = HomeUiState(
    greetingName = "연우", handle = "nightowl", driveMode = DriveMode.PARKED,
    filter = HomeFilter.RECENT, totalCount = 12, favoriteCount = 3, cards = previewCards,
)

@Preview(widthDp = 1000, heightDp = 696)
@Composable
private fun HomeScreenPreview() {
    MoodriveTheme { HomeScreen(previewState, HomeActions()) }
}

@Preview(widthDp = 1000, heightDp = 696)
@Composable
private fun HomeScreenSelectedPreview() {
    MoodriveTheme {
        HomeScreen(
            previewState.copy(
                selected = SelectedSceneUi(
                    previewCards[1],
                    listOf(
                        DetailItemUi(DetailKind.AMBIENT, "현재 30%", "50", "%"),
                        DetailItemUi(DetailKind.SEAT, "현재 OFF", "2", "단 · 열선"),
                        DetailItemUi(DetailKind.WIND, "현재 2단 · 얼굴", "1", "단 · 발 방향"),
                        DetailItemUi(DetailKind.STEERING, "현재 OFF", "ON"),
                    ),
                ),
            ),
            HomeActions(),
        )
    }
}
