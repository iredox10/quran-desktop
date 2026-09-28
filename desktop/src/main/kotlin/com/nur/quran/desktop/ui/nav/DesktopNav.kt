package com.nur.quran.desktop.ui.nav

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nur.quran.desktop.ui.theme.NurPalette
import com.nur.quran.desktop.ui.theme.rememberUiFontFamily

object DesktopRoutes {
    const val HOME = "home"
    const val MEMORIZE = "memorize"
    const val PLANNER = "planner"
    const val ANALYTICS = "analytics"
    const val LIBRARY = "library"
    const val DOWNLOADS = "downloads"
    const val HISTORY = "history"
    const val PROFILE = "profile"

    fun isTopLevel(route: String): Boolean =
        route == HOME ||
            route == MEMORIZE ||
            route == PLANNER ||
            route == ANALYTICS ||
            route == LIBRARY ||
            route == DOWNLOADS ||
            route == HISTORY ||
            route == PROFILE

    fun topLevelOf(route: String): String =
        when {
            route == HOME || route.startsWith("surah/") || route.startsWith("page/") -> HOME
            route == MEMORIZE || route.startsWith("memorize/") -> MEMORIZE
            route == PLANNER || route == "planner_reader" || route.startsWith("planner_reader/") -> PLANNER
            isTopLevel(route) -> route
            else -> HOME
        }
}

private data class NavEntry(val route: String, val label: String, val icon: ImageVector)

private val navEntries: List<NavEntry> = listOf(
    NavEntry(DesktopRoutes.HOME, "Home", Icons.Filled.Home),
    NavEntry(DesktopRoutes.MEMORIZE, "Memorize", Icons.Filled.School),
    NavEntry(DesktopRoutes.PLANNER, "Planner", Icons.Filled.CalendarMonth),
    NavEntry(DesktopRoutes.ANALYTICS, "Analytics", Icons.Filled.BarChart),
    NavEntry(DesktopRoutes.LIBRARY, "Library", Icons.Filled.Bookmark),
    NavEntry(DesktopRoutes.HISTORY, "History", Icons.Filled.History),
    NavEntry(DesktopRoutes.DOWNLOADS, "Downloads", Icons.Filled.Download),
    NavEntry(DesktopRoutes.PROFILE, "Profile", Icons.Filled.Person),
)

@Composable
fun AppSidebar(pal: NurPalette, current: String, onNavigate: (String) -> Unit, modifier: Modifier = Modifier) {
    val uiFont = rememberUiFontFamily()
    val active = DesktopRoutes.topLevelOf(current)
    Row(
        modifier = modifier
            .width(208.dp)
            .background(pal.cream)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(horizontal = 12.dp, vertical = 16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 4.dp)
            ) {
                Image(
                    painter = painterResource("drawable/ic_logo.png"),
                    contentDescription = "Quran Nur logo",
                    modifier = Modifier.size(28.dp).clip(RoundedCornerShape(8.dp))
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Quran Nur",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = uiFont,
                    color = pal.ink
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            navEntries.forEach { entry ->
                val selected = active == entry.route
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (selected) pal.tealSoft else androidx.compose.ui.graphics.Color.Transparent)
                        .clickable { onNavigate(entry.route) }
                        .padding(horizontal = 10.dp, vertical = 10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(20.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(if (selected) pal.teal else androidx.compose.ui.graphics.Color.Transparent)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Icon(
                            imageVector = entry.icon,
                            contentDescription = entry.label,
                            tint = if (selected) pal.teal else pal.inkMid,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = entry.label,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = uiFont,
                            color = if (selected) pal.teal else pal.inkMid
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "Desktop edition",
                fontSize = 10.sp,
                color = pal.inkMuted,
                fontFamily = uiFont,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
        Box(
            modifier = Modifier
                .width(1.dp)
                .fillMaxHeight()
                .background(pal.boneDark)
        )
    }
}
