package com.example.moodrive.ui.common

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.moodrive.R
import com.example.moodrive.ui.theme.MoodriveColors
import com.example.moodrive.ui.theme.text

enum class NavDestination { MY_MOOD, EXPLORE, SETTINGS }

/** 왼쪽 메뉴. 내 Mood(홈)와 탐색 화면이 함께 쓴다 */
/** 탭 화면(내 Mood·탐색)에서 사이드바 바깥 여백. 두 화면이 같은 값을 써야 탭을 바꿔도 사이드바가 움직이지 않는다 */
internal val SideBarPadding = PaddingValues(start = 28.dp, top = 24.dp, bottom = 24.dp)

@Composable
internal fun SideBar(
    handle: String,
    avatarUrl: String?,
    avatarFallback: String,
    selected: NavDestination,
    canCreate: Boolean,
    onNavigate: (NavDestination) -> Unit,
    onCreate: () -> Unit,
) {
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier = Modifier
            .width(82.dp)
            .fillMaxHeight()
            .clip(shape)
            .background(Color.White.copy(alpha = 0.04f))
            .border(1.dp, Color.White.copy(alpha = 0.08f), shape)
            .padding(top = 26.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Avatar(avatarUrl, avatarFallback)
        Spacer(Modifier.height(7.dp))
        Text(
            "@$handle",
            style = text(12.sp, FontWeight.Normal, MoodriveColors.TextPrimary),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 4.dp),
        )

        // 시안 비율: 프로필→메뉴 188px, 메뉴→+버튼 244px
        Spacer(Modifier.weight(188f))
        NavItem(R.drawable.ic_nav_grid, "내 Mood", selected == NavDestination.MY_MOOD) { onNavigate(NavDestination.MY_MOOD) }
        Spacer(Modifier.height(18.dp))
        NavItem(R.drawable.ic_nav_globe, "탐색", selected == NavDestination.EXPLORE) { onNavigate(NavDestination.EXPLORE) }
        Spacer(Modifier.height(18.dp))
        NavItem(R.drawable.ic_nav_gear, "설정", selected == NavDestination.SETTINGS) { onNavigate(NavDestination.SETTINGS) }
        Spacer(Modifier.weight(244f))

        Box(
            modifier = Modifier
                .size(38.dp)
                .alpha(if (canCreate) 1f else 0.35f)
                .clip(CircleShape)
                .background(Color.White)
                .clickable(enabled = canCreate, onClick = onCreate),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(R.drawable.ic_plus), contentDescription = "새 무드 만들기", tint = Color.Black, modifier = Modifier.size(19.dp))
        }
    }
}

@Composable
private fun Avatar(url: String?, fallback: String) {
    Box(
        modifier = Modifier.size(47.dp).clip(CircleShape).background(MoodriveColors.Selected),
        contentAlignment = Alignment.Center,
    ) {
        Text(fallback, style = text(18.sp, FontWeight.Bold, MoodriveColors.TextPrimary))
        if (url != null) {
            AsyncImage(model = url, contentDescription = "프로필", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun NavItem(@DrawableRes icon: Int, label: String, selected: Boolean, onClick: () -> Unit) {
    val color = if (selected) MoodriveColors.TextPrimary else MoodriveColors.TextMuted
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickableNoRipple(onClick),
    ) {
        Box(
            modifier = Modifier
                .size(47.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(if (selected) Color.White.copy(alpha = 0.12f) else Color.Transparent),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(icon), contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.height(7.dp))
        Text(label, style = text(12.sp, FontWeight.Normal, color))
    }
}
