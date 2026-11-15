package com.example.moodrive.ui.explore

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.moodrive.R
import com.example.moodrive.domain.model.DriveMode
import com.example.moodrive.ui.common.ColorDot
import com.example.moodrive.ui.common.DriveStatusPill
import com.example.moodrive.ui.common.NavDestination
import com.example.moodrive.ui.common.SettingChip
import com.example.moodrive.ui.common.SettingChipIcon
import com.example.moodrive.ui.common.SideBar
import com.example.moodrive.ui.common.SideBarPadding
import com.example.moodrive.ui.common.White
import com.example.moodrive.ui.common.White64
import com.example.moodrive.ui.common.White84
import com.example.moodrive.ui.common.px
import com.example.moodrive.ui.common.style
import com.example.moodrive.ui.theme.MoodriveColors
import com.example.moodrive.ui.theme.MoodriveTheme

/*
 * 탐색 (커뮤니티 · 추천 탭). Figma 469:47038 "커뮤니티 · ① 추천 탭 · 정리본".
 * 화면 전체가 위아래로 스크롤되고, 컬러 테마·큐레이션·컬렉션 목록은 좌우로 스크롤된다.
 * 커뮤니티 API가 붙기 전까지 시안 내용을 샘플 데이터로 보여준다.
 */

data class ExploreActions(
    val onNavigate: (NavDestination) -> Unit = {},
    val onCreate: () -> Unit = {},
    val onSearch: () -> Unit = {},
    val onVoiceSearch: () -> Unit = {},
    val onShare: () -> Unit = {},
    val onOpenMood: (ExploreMoodUi) -> Unit = {},
    val onSeeAllCuration: () -> Unit = {},
    val onSeeAllCollections: () -> Unit = {},
    val onOpenCollection: (ExploreCollectionUi) -> Unit = {},
)

/** 사이드바에 필요한 프로필·상태 */
data class ExploreProfile(
    val handle: String,
    val avatarUrl: String?,
    val avatarFallback: String,
    val driveMode: DriveMode,
    val canCreate: Boolean,
)

@Composable
fun ExploreScreen(profile: ExploreProfile, actions: ExploreActions, feed: ExploreFeed = SAMPLE_EXPLORE_FEED) {
    // 뒤로가기는 내 Mood(홈)로
    BackHandler { actions.onNavigate(NavDestination.MY_MOOD) }
    var themeIndex by rememberSaveable { mutableIntStateOf(0) }

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(0f to MoodriveColors.BackgroundTop, 0.88f to Color(0xFF1A1B20))),
    ) {
        Row(Modifier.fillMaxSize().padding(SideBarPadding)) {
            SideBar(
                handle = profile.handle,
                avatarUrl = profile.avatarUrl,
                avatarFallback = profile.avatarFallback,
                selected = NavDestination.EXPLORE,
                canCreate = profile.canCreate,
                onNavigate = actions.onNavigate,
                onCreate = actions.onCreate,
            )
            // 사이드바 오른쪽 영역만 스크롤된다 (가로 목록도 이 영역 안에서 잘린다)
            Box(Modifier.fillMaxSize().clipToBounds()) {
                ExploreFeedContent(profile.driveMode, feed, themeIndex, onTheme = { themeIndex = it }, actions)
            }
        }
    }
}

/** 시안 기준 콘텐츠 시작 x(238px)에서 사이드바 오른쪽 끝(25dp + 82dp)을 뺀 값 */
private val ContentStart = px(238) - 107.dp
private val ContentEnd = px(60)

@Composable
private fun ExploreFeedContent(driveMode: DriveMode, feed: ExploreFeed, themeIndex: Int, onTheme: (Int) -> Unit, actions: ExploreActions) {
    val side = Modifier.padding(start = ContentStart, end = ContentEnd)
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            // 사이드바와 위쪽 여백(22dp)을 빼고 시안 y=60px에서 시작
            .padding(top = px(60) - 22.dp, bottom = px(143)),
    ) {
        Header(driveMode, side)
        Spacer(Modifier.height(px(32)))
        SearchRow(actions, side)
        Spacer(Modifier.height(px(60)))
        Recommended(feed, actions, side)

        Spacer(Modifier.height(px(60)))
        SectionHeader("컬러리스트 큐레이션", "빛과 색으로 완성하는 이번 주의 무드", actions.onSeeAllCuration, side)
        Spacer(Modifier.height(px(32)))
        ThemeRow(feed.themes, themeIndex, onTheme)
        Spacer(Modifier.height(px(32)))
        // 고른 테마에 따라 큐레이터와 무드 목록이 바뀐다
        AnimatedContent(
            targetState = feed.themes[themeIndex.coerceIn(feed.themes.indices)],
            transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(200)) },
            label = "curation",
        ) { theme -> CurationRow(theme, actions) }

        Spacer(Modifier.height(px(60)))
        SectionHeader("컬러로 이어진 사진들", null, actions.onSeeAllCollections, side)
        Spacer(Modifier.height(px(32)))
        LazyRow(
            contentPadding = PaddingValues(start = ContentStart, end = ContentEnd),
            horizontalArrangement = Arrangement.spacedBy(px(26)),
        ) {
            items(feed.collections, key = { it.title }) { CollectionCard(it) { actions.onOpenCollection(it) } }
        }
    }
}

// ───────────────────────── 상단 ─────────────────────────

@Composable
private fun Header(driveMode: DriveMode, modifier: Modifier) {
    // 주행 상태 표시도 제목과 함께 스크롤된다 (시안 그대로)
    Row(modifier.fillMaxWidth()) {
        Column(Modifier.weight(1f)) {
            Image(painterResource(R.drawable.img_logo_moodrive), contentDescription = "Moodrive", modifier = Modifier.size(px(153), px(34)))
            Spacer(Modifier.height(px(4)))
            Text("비 오는 저녁, 이런 무드 어때요?", style = style(48, FontWeight.Bold, White, 64))
        }
        DriveStatusPill(driveMode)
    }
}

@Composable
private fun SearchRow(actions: ExploreActions, modifier: Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Row(
            Modifier
                .weight(1f)
                .height(px(80))
                .clip(RoundedCornerShape(px(24)))
                .background(White.copy(alpha = 0.1f))
                .clickable(onClick = actions.onSearch)
                .padding(start = px(30), end = px(12)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(painterResource(R.drawable.ic_explore_search), null, Modifier.size(px(35)))
            Spacer(Modifier.width(px(18)))
            Text("무드, 장소, 제작자로 찾기", style = style(24, FontWeight.Normal, White64, 28), modifier = Modifier.weight(1f))
            Image(
                painterResource(R.drawable.ic_explore_mic),
                contentDescription = "음성으로 찾기",
                modifier = Modifier.size(px(56)).clip(CircleShape).clickable(onClick = actions.onVoiceSearch),
            )
        }
        Spacer(Modifier.width(px(146)))
        val shape = RoundedCornerShape(px(24))
        Row(
            Modifier
                .size(px(278), px(80))
                .clip(shape)
                .background(White.copy(alpha = 0.04f))
                .border(px(2), White.copy(alpha = 0.4f), shape)
                .clickable(onClick = actions.onShare),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(painterResource(R.drawable.ic_explore_share), null, Modifier.size(px(36)))
            Spacer(Modifier.width(px(10)))
            Text("내 무드 공유하기", style = style(24, FontWeight.Normal, White, 28))
        }
    }
}

@Composable
private fun SectionHeader(title: String, subtitle: String?, onSeeAll: () -> Unit, modifier: Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = style(32, FontWeight.Normal, White, 40))
            if (subtitle != null) {
                Spacer(Modifier.height(px(14)))
                Text(subtitle, style = style(24, FontWeight.Normal, White.copy(alpha = 0.6f), 28))
            }
        }
        Row(
            Modifier.clip(RoundedCornerShape(px(16))).clickable(onClick = onSeeAll).padding(start = px(12)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("전체 보기", style = style(24, FontWeight.Normal, White64, 28))
            Image(painterResource(R.drawable.ic_explore_chevron), null, Modifier.size(px(64)))
        }
    }
}

// ───────────────────────── 추천 (히어로 + 작은 카드 2개) ─────────────────────────

@Composable
private fun Recommended(feed: ExploreFeed, actions: ExploreActions, modifier: Modifier) {
    Row(modifier.fillMaxWidth().height(px(560))) {
        HeroCard(feed.hero, actions, Modifier.weight(1f).fillMaxHeight())
        Spacer(Modifier.width(px(24)))
        Column(Modifier.width(px(400)).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(px(26))) {
            feed.picks.take(2).forEach { mood ->
                SmallCard(mood, Modifier.weight(1f).fillMaxWidth()) { actions.onOpenMood(mood) }
            }
        }
    }
}

@Composable
private fun HeroCard(mood: ExploreMoodUi, actions: ExploreActions, modifier: Modifier) {
    val shape = RoundedCornerShape(px(40))
    // Figma 724:32058 "Hero · 날씨 추천". 카드를 누르면 상세 화면 (적용·저장은 상세 화면에서)
    Box(modifier.clip(shape).border(px(2), White.copy(alpha = 0.12f), shape).clickable { actions.onOpenMood(mood) }) {
        Image(painterResource(mood.imageRes), mood.name, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Color(0xEB131417), Color(0x1A131417)))))
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(px(260))
                .background(Brush.verticalGradient(listOf(Color(0x00131417), Color(0xBF131417)))),
        )

        AuthorRow(mood.author, avatar = px(42), gap = px(8), textSize = 24, lineHeight = 28, Modifier.padding(start = px(38), top = px(34)))
        SaveCount(mood.saves, Modifier.align(Alignment.TopEnd).padding(top = px(46), end = px(38)))

        // 오늘의 추천 · 제목 · 키워드 · 설정 칩을 카드 아래쪽에 모은다 (시안 y=257부터)
        Column(Modifier.padding(start = px(38), top = px(257))) {
            TodayPickTag(Modifier.offset(x = px(-2)))
            Spacer(Modifier.height(px(16)))
            Text(mood.name, style = style(60, FontWeight.Bold, White, 80), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(px(2)))
            Text(mood.keywords, style = style(28, FontWeight.Normal, White.copy(alpha = 0.8f), 36))
            Spacer(Modifier.height(px(20)))
            mood.settings?.let { SettingChips(it) }
        }
    }
}

/** "✦ 오늘의 추천" 배지 (181×52, 흰색 14%) */
@Composable
private fun TodayPickTag(modifier: Modifier = Modifier) {
    Row(
        modifier
            .height(px(52))
            .clip(RoundedCornerShape(px(26)))
            .background(White.copy(alpha = 0.14f))
            .padding(horizontal = px(24)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(painterResource(R.drawable.ic_create_spark), contentDescription = null, modifier = Modifier.size(px(24)))
        Spacer(Modifier.width(px(8)))
        Text("오늘의 추천", style = style(22, FontWeight.SemiBold, White, 26), maxLines = 1)
    }
}

@Composable
private fun SettingChips(s: ExploreSettingsUi) {
    // 만들기 화면과 달리 줄 수 제한 없음 (시안 그대로), 핸들 칩은 항상 ON/OFF 로 보인다
    Row(horizontalArrangement = Arrangement.spacedBy(px(10))) {
        SettingChip("${s.brightness}%") { ColorDot(Color(s.lightArgb)) }
        SettingChip(s.seat) { SettingChipIcon(R.drawable.ic_create_seat) }
        SettingChip(s.wind) { SettingChipIcon(R.drawable.ic_create_wind) }
        SettingChip(if (s.wheelOn) "ON" else "OFF") { SettingChipIcon(R.drawable.ic_create_wheel) }
    }
}

@Composable
private fun SmallCard(mood: ExploreMoodUi, modifier: Modifier, onClick: () -> Unit) {
    Box(modifier.clip(RoundedCornerShape(px(28))).clickable(onClick = onClick)) {
        Image(painterResource(mood.imageRes), mood.name, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.35f to MoodriveColors.CardScrim.copy(alpha = 0f), 1f to MoodriveColors.CardScrim.copy(alpha = 0.92f))))
        AuthorRow(mood.author, avatar = px(36), gap = px(8), textSize = 20, lineHeight = 24, Modifier.padding(start = px(24), top = px(24)))
        SaveCount(mood.saves, Modifier.align(Alignment.TopEnd).padding(top = px(30), end = px(28)))
        MoodTitle(mood, Modifier.align(Alignment.BottomStart).padding(start = px(28), bottom = px(28)))
    }
}

@Composable
private fun MoodTitle(mood: ExploreMoodUi, modifier: Modifier) {
    Column(modifier) {
        Text(mood.name, style = style(32, FontWeight.Bold, White, 40), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(px(8)))
        Text(mood.keywords, style = style(24, FontWeight.Normal, White64, 28), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** 저장 수 (북마크 아이콘 + "2.3k") */
@Composable
private fun SaveCount(count: String, modifier: Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Image(painterResource(R.drawable.ic_explore_bookmark), null, Modifier.size(px(20)))
        Spacer(Modifier.width(px(4)))
        Text(count, style = style(20, FontWeight.Normal, White.copy(alpha = 0.7f), 24))
    }
}

/** 작성자 줄 (아바타 + @핸들). 카드마다 시안 크기·간격이 달라 호출부에서 넘긴다 */
@Composable
private fun AuthorRow(author: ExploreAuthorUi, avatar: Dp, gap: Dp, textSize: Int, lineHeight: Int, modifier: Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Avatar(author, avatar)
        Spacer(Modifier.width(gap))
        Text("@${author.handle}", style = style(textSize, FontWeight.Normal, White, lineHeight))
    }
}

/** 프로필 사진이 있으면 사진, 없으면 그라데이션이나 단색 원에 첫 글자 */
@Composable
private fun Avatar(author: ExploreAuthorUi, size: Dp) {
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(author.gradient?.let { Brush.linearGradient(it) } ?: Brush.linearGradient(listOf(AvatarBlue, AvatarBlue))),
        contentAlignment = Alignment.Center,
    ) {
        when {
            author.photoRes != null -> Image(painterResource(author.photoRes), "@${author.handle}", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            author.gradient == null -> Text(
                author.handle.take(1).uppercase(),
                style = style(20, FontWeight.Normal, MoodriveColors.Scrim, 24, TextAlign.Center),
            )
        }
    }
}

// ───────────────────────── 컬러리스트 큐레이션 ─────────────────────────

@Composable
private fun ThemeRow(themes: List<ColorThemeUi>, selected: Int, onSelect: (Int) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(start = ContentStart, end = ContentEnd),
        horizontalArrangement = Arrangement.spacedBy(px(18)),
    ) {
        items(themes.size) { i -> ThemeChip(themes[i], i == selected) { onSelect(i) } }
    }
}

@Composable
private fun ThemeChip(theme: ColorThemeUi, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(px(28))
    Row(
        Modifier
            .size(px(289), px(108))
            .clip(shape)
            .background(White.copy(alpha = if (selected) 0.14f else 0.05f))
            .border(px(2), if (selected) White.copy(alpha = 0.8f) else Color.Transparent, shape)
            .clickable(onClick = onClick)
            .padding(start = px(20)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 위 반은 메인 색, 아래 반은 서브 색
        Column(Modifier.size(px(60)).clip(CircleShape)) {
            Box(Modifier.weight(1f).fillMaxWidth().background(Color(theme.mainArgb)))
            Box(Modifier.weight(1f).fillMaxWidth().background(Color(theme.subArgb)))
        }
        Spacer(Modifier.width(px(16)))
        Column {
            Text(theme.name, style = style(26, FontWeight.Bold, White.copy(alpha = if (selected) 1f else 0.85f), 34), maxLines = 1)
            Spacer(Modifier.height(px(4)))
            Text(theme.mood, style = style(20, FontWeight.Normal, White.copy(alpha = 0.6f), 24), maxLines = 1)
        }
    }
}

@Composable
private fun CurationRow(theme: ColorThemeUi, actions: ExploreActions) {
    // 큐레이터 카드는 고정, 무드 카드만 그 오른쪽 영역 안에서 좌우로 스크롤된다
    Row(Modifier.padding(start = ContentStart)) {
        CuratorCard(theme, actions.onSeeAllCuration)
        Spacer(Modifier.width(px(24)))
        LazyRow(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(end = ContentEnd),
            horizontalArrangement = Arrangement.spacedBy(px(24)),
        ) {
            items(theme.moods, key = { it.name }) { mood -> CommunityCard(mood) { actions.onOpenMood(mood) } }
        }
    }
}

@Composable
private fun CuratorCard(theme: ColorThemeUi, onSeeAll: () -> Unit) {
    val curator = theme.curator
    Column(
        Modifier
            .size(px(500), px(440))
            .clip(RoundedCornerShape(px(32)))
            .background(White.copy(alpha = 0.06f))
            .padding(px(32)),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar(curator.author, px(72))
            Spacer(Modifier.width(px(16)))
            Column {
                Text("@${curator.author.handle}", style = style(28, FontWeight.Bold, White, 36))
                Spacer(Modifier.height(px(4)))
                Text(curator.role, style = style(20, FontWeight.Normal, White.copy(alpha = 0.6f), 24))
            }
        }
        Spacer(Modifier.height(px(31)))
        Text(
            "“${curator.quote}”",
            style = style(30, FontWeight.Normal, White.copy(alpha = 0.95f), 38),
            maxLines = 2,
            modifier = Modifier.height(px(76)),
        )
        Spacer(Modifier.weight(1f))
        Row(Modifier.fillMaxWidth().height(px(56)).clip(RoundedCornerShape(px(16)))) {
            theme.palette.forEach { Box(Modifier.weight(1f).fillMaxHeight().background(Color(it))) }
        }
        Spacer(Modifier.height(px(31)))
        Box(
            Modifier
                .fillMaxWidth()
                .height(px(80))
                .clip(RoundedCornerShape(px(28)))
                .background(White.copy(alpha = 0.12f))
                .clickable(onClick = onSeeAll),
            contentAlignment = Alignment.Center,
        ) {
            Text("큐레이션 전체보기", style = style(26, FontWeight.Bold, White, 34))
        }
    }
}

@Composable
private fun CommunityCard(mood: ExploreMoodUi, onClick: () -> Unit) {
    Box(Modifier.size(px(320), px(440)).clip(RoundedCornerShape(px(28))).clickable(onClick = onClick)) {
        Image(painterResource(mood.imageRes), mood.name, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        TopShade()
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(px(200))
                .background(Brush.verticalGradient(listOf(MoodriveColors.CardScrim.copy(alpha = 0f), MoodriveColors.CardScrim.copy(alpha = 0.92f)))),
        )
        AuthorRow(mood.author, avatar = px(43), gap = px(7), textSize = 20, lineHeight = 24, Modifier.padding(start = px(24), top = px(17)))
        SaveCount(mood.saves, Modifier.align(Alignment.TopEnd).padding(top = px(30), end = px(24)))
        MoodTitle(mood, Modifier.align(Alignment.BottomStart).padding(start = px(24), bottom = px(35)))
    }
}

@Composable
private fun BoxScope.TopShade() {
    Box(
        Modifier
            .align(Alignment.TopCenter)
            .fillMaxWidth()
            .height(px(118))
            .background(Brush.verticalGradient(listOf(MoodriveColors.CardScrim.copy(alpha = 0.92f), MoodriveColors.CardScrim.copy(alpha = 0f)))),
    )
}

// ───────────────────────── 컬러로 이어진 사진들 ─────────────────────────

@Composable
private fun CollectionCard(collection: ExploreCollectionUi, onClick: () -> Unit) {
    Box(
        Modifier
            .size(px(450), px(440))
            .clip(RoundedCornerShape(px(28)))
            .background(Color(collection.tintArgb).copy(alpha = 0.3f))
            .clickable(onClick = onClick),
    ) {
        Image(
            painterResource(collection.photos[0]), null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(px(270), px(296)),
        )
        Image(
            painterResource(collection.photos[1]), null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.offset(x = px(274)).size(px(177), px(146)),
        )
        // 시안 원본에서 위쪽에 빈 여백이 있어 아래 기준으로 자른다
        Image(
            painterResource(collection.photos[2]), null,
            contentScale = ContentScale.Crop,
            alignment = Alignment.BottomCenter,
            modifier = Modifier.offset(x = px(274), y = px(150)).size(px(177), px(146)),
        )
        Box(
            Modifier
                .offset(x = px(363), y = px(24))
                .size(px(64))
                .clip(RoundedCornerShape(px(20)))
                .background(Color(0x80191A1F)),
            contentAlignment = Alignment.Center,
        ) {
            Image(painterResource(R.drawable.ic_explore_chevron), "컬렉션 열기", Modifier.size(px(64)))
        }
        Column(Modifier.align(Alignment.BottomStart).padding(start = px(28), bottom = px(26))) {
            Text(collection.title, style = style(30, FontWeight.Bold, White, 38), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(px(16)))
            Row(verticalAlignment = Alignment.CenterVertically) {
                ColorDot(Color(collection.dotArgb))
                Spacer(Modifier.width(px(10)))
                Text("${collection.colorName} · ${collection.count}개", style = style(20, FontWeight.Normal, White84, 24))
            }
        }
    }
}

private val AvatarBlue = Color(0xFF99B2FF)

@Preview(widthDp = 1000, heightDp = 696)
@Composable
private fun ExplorePreview() {
    MoodriveTheme {
        ExploreScreen(
            ExploreProfile("nightowl", null, "연", DriveMode.PARKED, canCreate = true),
            ExploreActions(),
        )
    }
}
