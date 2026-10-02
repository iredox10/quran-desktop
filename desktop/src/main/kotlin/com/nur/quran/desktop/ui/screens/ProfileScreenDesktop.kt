package com.nur.quran.desktop.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nur.quran.desktop.PrefsCache
import com.nur.quran.desktop.data.BackupStore
import com.nur.quran.desktop.data.BookmarkStore
import com.nur.quran.desktop.data.QuranStore
import com.nur.quran.desktop.data.SessionStore
import com.nur.quran.desktop.ui.components.DesktopFonts
import com.nur.quran.desktop.ui.theme.NurPalette
import com.nur.quran.desktop.ui.theme.rememberBodyFontFamily
import com.nur.quran.desktop.ui.theme.rememberUiFontFamily
import com.nur.quran.shared.AnalyticsStats
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.prefs.Preferences
import javax.swing.JFileChooser

private val RECITER_OPTIONS = listOf("Mishary Alafasy", "Abdul Basit", "Saad Al-Ghamdi")
private val TRANSLATION_OPTIONS = listOf("Saheeh International", "Haleem", "Hausa Gumi")
private val MUSHAF_OPTIONS = listOf("Uthmani", "Indopak")

private const val PROFILE_PREFS_NODE = "profile_prefs"
private const val KEY_WEEKLY_GOAL_MIN = "weekly_goal_min"
private const val DEFAULT_WEEKLY_GOAL_MIN = 180

private fun nextOption(current: String, options: List<String>): String {
    val idx = options.indexOf(current)
    return options[(idx + 1) % options.size]
}

/**
 * Desktop Profile screen mirroring Android `ProfileScreen` + `ui/components/profile/`:
 * hero, weekly goal card, quick links, settings groups, local backup card,
 * danger zone and footer — all inlined in this file.
 */
@Composable
fun ProfileScreenDesktop(
    pal: NurPalette,
    dark: Boolean,
    onToggleTheme: () -> Unit,
    onBack: () -> Unit = {},
    onOpenLibrary: () -> Unit = {},
    onOpenDownloads: () -> Unit = {},
    onOpenPlanner: () -> Unit = {}
) {
    val fontUi = rememberUiFontFamily()
    val fontBody = rememberBodyFontFamily()
    var refreshTick by remember { mutableStateOf(0) }

    val sessions: List<AnalyticsStats.Session> = remember(refreshTick) {
        runCatching {
            SessionStore.all().map { s ->
                AnalyticsStats.Session(
                    date = s.date,
                    durationSec = s.durationSec,
                    type = s.type,
                    chapterId = s.chapterId
                )
            }
        }.getOrDefault(emptyList())
    }
    val totalMinutes = remember(sessions) { AnalyticsStats.allTimeMinutes(sessions) }
    val weeklyMinutes = remember(sessions) { last7DaysMinutes(sessions) }
    val bookmarkCount = remember(refreshTick) {
        runCatching { BookmarkStore.all().size }.getOrDefault(0)
    }

    Column(modifier = Modifier.fillMaxSize().background(pal.white)) {
        ProfileTopBar(pal = pal, fontUi = fontUi, onBack = onBack)
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                modifier = Modifier.widthIn(max = 640.dp).fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = 16.dp,
                    vertical = 8.dp
                ),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                item {
                    ProfileHeroDesktop(
                        pal = pal,
                        fontUi = fontUi,
                        fontBody = fontBody,
                        totalMinutes = totalMinutes,
                        sessionCount = sessions.size,
                        bookmarkCount = bookmarkCount
                    )
                }
                item {
                    ProfileGoalCardDesktop(
                        pal = pal,
                        fontUi = fontUi,
                        fontBody = fontBody,
                        weeklyMinutes = weeklyMinutes,
                        refreshTick = refreshTick
                    )
                }
                item {
                    ProfileQuickLinksDesktop(
                        pal = pal,
                        fontUi = fontUi,
                        onOpenLibrary = onOpenLibrary,
                        onOpenDownloads = onOpenDownloads,
                        onOpenPlanner = onOpenPlanner
                    )
                }
                item {
                    ProfileRecentSessionsDesktop(
                        pal = pal,
                        fontUi = fontUi,
                        fontBody = fontBody,
                        sessions = sessions
                    )
                }
                item {
                    ProfileSettingsGroupsDesktop(
                        pal = pal,
                        fontUi = fontUi,
                        fontBody = fontBody,
                        dark = dark,
                        onToggleTheme = onToggleTheme,
                        refreshTick = refreshTick,
                        onPrefsChanged = { refreshTick++ }
                    )
                }
                item {
                    ProfileBackupCardDesktop(
                        pal = pal,
                        fontUi = fontUi,
                        fontBody = fontBody
                    )
                }
                item {
                    ProfileDangerZoneDesktop(
                        pal = pal,
                        fontUi = fontUi,
                        fontBody = fontBody,
                        onCleared = { refreshTick++ }
                    )
                }
                item {
                    ProfileFooterDesktop(pal = pal, fontUi = fontUi)
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}

private fun last7DaysMinutes(sessions: List<AnalyticsStats.Session>): Int {
    val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val last7 = mutableSetOf<String>()
    val cal = Calendar.getInstance()
    repeat(7) {
        last7.add(fmt.format(cal.time))
        cal.add(Calendar.DATE, -1)
    }
    return Math.round(
        sessions.filter { it.date in last7 }.sumOf { it.durationSec } / 60f
    ).toInt()
}

// ── Top bar ────────────────────────────────────────────────────────────────
@Composable
private fun ProfileTopBar(pal: NurPalette, fontUi: FontFamily, onBack: () -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth().height(56.dp), color = pal.cream) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = pal.ink,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = "Profile",
                    fontFamily = fontUi,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = pal.ink,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.size(40.dp))
            }
            HorizontalDivider(color = pal.boneDark.copy(alpha = 0.5f), thickness = 1.dp)
        }
    }
}

// ── Hero ───────────────────────────────────────────────────────────────────
@Composable
private fun ProfileHeroDesktop(
    pal: NurPalette,
    fontUi: FontFamily,
    fontBody: FontFamily,
    totalMinutes: Int,
    sessionCount: Int,
    bookmarkCount: Int
) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(190.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(pal.gold.copy(alpha = 0.18f), Color.Transparent)
                    ),
                    shape = CircleShape
                )
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(brush = Brush.linearGradient(listOf(pal.surface, pal.cream)))
                    .border(2.dp, pal.gold.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Q",
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = fontUi,
                    color = pal.gold
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "YOUR PROFILE",
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.4.sp,
                color = pal.inkMuted
            )
            Text(
                text = "Quran Nur Desktop",
                fontFamily = fontUi,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = pal.ink,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Your personal Quran companion",
                fontFamily = fontBody,
                fontSize = 13.sp,
                color = pal.inkMuted,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                HeroStat(pal, fontUi, fontBody, formatMinutes(totalMinutes), "READING")
                HeroStatDivider(pal)
                HeroStat(pal, fontUi, fontBody, sessionCount.toString(), "SESSIONS")
                HeroStatDivider(pal)
                HeroStat(pal, fontUi, fontBody, bookmarkCount.toString(), "BOOKMARKS")
            }
        }
    }
}

@Composable
private fun HeroStat(
    pal: NurPalette,
    fontUi: FontFamily,
    fontBody: FontFamily,
    value: String,
    label: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        Text(
            text = value,
            fontFamily = fontUi,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = pal.ink
        )
        Text(
            text = label,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            letterSpacing = 1.2.sp,
            color = pal.inkMuted
        )
    }
}

@Composable
private fun HeroStatDivider(pal: NurPalette) {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(36.dp)
            .background(pal.boneDark.copy(alpha = 0.6f))
            .padding(vertical = 4.dp)
    )
}

// ── Weekly goal card ───────────────────────────────────────────────────────
@Composable
private fun ProfileGoalCardDesktop(
    pal: NurPalette,
    fontUi: FontFamily,
    fontBody: FontFamily,
    weeklyMinutes: Int,
    refreshTick: Int
) {
    val goalPrefs = remember { Preferences.userRoot().node(PROFILE_PREFS_NODE) }
    var weeklyGoal by remember(refreshTick) {
        mutableStateOf(goalPrefs.getInt(KEY_WEEKLY_GOAL_MIN, DEFAULT_WEEKLY_GOAL_MIN))
    }
    fun persistGoal(next: Int) {
        val clamped = next.coerceIn(30, 1440)
        runCatching { goalPrefs.putInt(KEY_WEEKLY_GOAL_MIN, clamped) }
        weeklyGoal = clamped
    }
    val pct = if (weeklyGoal > 0) {
        (weeklyMinutes.toFloat() / weeklyGoal.toFloat()).coerceIn(0f, 1f)
    } else 0f

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = pal.cream),
        border = BorderStroke(1.5.dp, pal.boneDark)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(pal.goldLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Timer,
                            contentDescription = null,
                            tint = pal.gold,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Weekly Goal",
                            fontFamily = fontUi,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = pal.ink
                        )
                        Text(
                            text = "${formatMinutes(weeklyMinutes)} / ${formatMinutes(weeklyGoal)} this week",
                            fontFamily = fontBody,
                            fontSize = 12.sp,
                            color = pal.inkMuted
                        )
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    GoalStepperButton(pal = pal, image = Icons.Filled.Remove, desc = "Decrease goal") {
                        persistGoal(weeklyGoal - 15)
                    }
                    Text(
                        text = formatMinutes(weeklyGoal),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = pal.ink,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                    GoalStepperButton(pal = pal, image = Icons.Filled.Add, desc = "Increase goal") {
                        persistGoal(weeklyGoal + 15)
                    }
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape)
                    .background(pal.surface)
            ) {
                if (pct > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction = pct)
                            .height(8.dp)
                            .clip(CircleShape)
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(pal.gold, pal.green)
                                )
                            )
                    )
                }
            }
            Text(
                text = "${(pct * 100).toInt()}% OF WEEKLY GOAL · TAP + / − TO ADJUST (STEPS OF 15M)",
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = pal.inkMuted
            )
        }
    }
}

@Composable
private fun GoalStepperButton(
    pal: NurPalette,
    image: ImageVector,
    desc: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(pal.surface)
            .border(1.dp, pal.boneDark, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(imageVector = image, contentDescription = desc, tint = pal.inkMid, modifier = Modifier.size(14.dp))
    }
}

// ── Quick links ────────────────────────────────────────────────────────────
@Composable
private fun ProfileQuickLinksDesktop(
    pal: NurPalette,
    fontUi: FontFamily,
    onOpenLibrary: () -> Unit,
    onOpenDownloads: () -> Unit,
    onOpenPlanner: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        ProfileSectionHeaderDesktop(pal = pal, title = "QUICK LINKS")
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = pal.cream),
            border = BorderStroke(1.5.dp, pal.boneDark)
        ) {
            Column {
                QuickLinkRow(pal, fontUi, Icons.Filled.LibraryBooks, "Library", onOpenLibrary)
                HorizontalDivider(color = pal.boneDark.copy(alpha = 0.6f), thickness = 1.dp)
                QuickLinkRow(pal, fontUi, Icons.Filled.Download, "Downloads", onOpenDownloads)
                HorizontalDivider(color = pal.boneDark.copy(alpha = 0.6f), thickness = 1.dp)
                QuickLinkRow(pal, fontUi, Icons.Filled.CalendarMonth, "Planner", onOpenPlanner)
            }
        }
    }
}

@Composable
private fun QuickLinkRow(
    pal: NurPalette,
    fontUi: FontFamily,
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(pal.goldLight),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = pal.gold, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = label,
            fontFamily = fontUi,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = pal.ink,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = pal.inkMuted.copy(alpha = 0.5f),
            modifier = Modifier.size(16.dp)
        )
    }
}

// ── Recent sessions ────────────────────────────────────────────────────────
@Composable
private fun ProfileRecentSessionsDesktop(
    pal: NurPalette,
    fontUi: FontFamily,
    fontBody: FontFamily,
    sessions: List<AnalyticsStats.Session>
) {
    val recent = remember(sessions) { sessions.reversed().take(8) }
    Column(modifier = Modifier.fillMaxWidth()) {
        ProfileSectionHeaderDesktop(pal = pal, title = "RECENT SESSIONS")
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = pal.cream),
            border = BorderStroke(1.5.dp, pal.boneDark)
        ) {
            if (recent.isEmpty()) {
                Text(
                    text = "No sessions yet",
                    fontFamily = fontBody,
                    fontSize = 13.sp,
                    color = pal.inkMuted,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 18.dp),
                    textAlign = TextAlign.Center
                )
            } else {
                Column {
                    recent.forEachIndexed { index, session ->
                        RecentSessionRow(pal = pal, fontUi = fontUi, fontBody = fontBody, session = session)
                        if (index < recent.lastIndex) {
                            HorizontalDivider(color = pal.boneDark.copy(alpha = 0.6f), thickness = 1.dp)
                        }
                    }
                }
            }
        }
    }
}

private fun sessionIcon(type: String): ImageVector {
    return when (type.lowercase()) {
        "reading" -> Icons.Filled.MenuBook
        "memorizing" -> Icons.Filled.LibraryBooks
        "focus", "pomodoro" -> Icons.Filled.Timer
        "listening" -> Icons.Filled.Mic
        else -> Icons.Filled.MenuBook
    }
}

@Composable
private fun RecentSessionRow(
    pal: NurPalette,
    fontUi: FontFamily,
    fontBody: FontFamily,
    session: AnalyticsStats.Session
) {
    val chapterName = remember(session.chapterId) {
        val id = session.chapterId
        if (id == null) null
        else runCatching { QuranStore.chapter(id)?.nameSimple }.getOrNull()?.takeIf { it.isNotBlank() }
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(pal.goldLight),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = sessionIcon(session.type),
                contentDescription = null,
                tint = pal.gold,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${session.date} · ${AnalyticsStats.formatMinutes(session.durationSec)}",
                fontFamily = fontUi,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = pal.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (chapterName != null) {
                Text(
                    text = chapterName,
                    fontFamily = fontBody,
                    fontSize = 12.sp,
                    color = pal.inkMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// ── Settings groups ────────────────────────────────────────────────────────
@Composable
private fun ProfileSettingsGroupsDesktop(
    pal: NurPalette,
    fontUi: FontFamily,
    fontBody: FontFamily,
    dark: Boolean,
    onToggleTheme: () -> Unit,
    refreshTick: Int,
    onPrefsChanged: () -> Unit
) {
    var reciter by remember(refreshTick) { mutableStateOf(PrefsCache.getReciter()) }
    var translation by remember(refreshTick) { mutableStateOf(PrefsCache.getTranslation()) }
    var mushaf by remember(refreshTick) { mutableStateOf(PrefsCache.getMushaf()) }
    var font by remember(refreshTick) { mutableStateOf(PrefsCache.getFont()) }
    var tajweed by remember(refreshTick) { mutableStateOf(PrefsCache.getTajweedEnabled()) }

    Column(modifier = Modifier.fillMaxWidth()) {
        ProfileSectionHeaderDesktop(pal = pal, title = "READING EXPERIENCE")
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = pal.cream),
            border = BorderStroke(1.5.dp, pal.boneDark)
        ) {
            Column {
                CyclingSettingRow(
                    pal = pal, fontBody = fontBody,
                    icon = Icons.Filled.Mic, label = "Reciter", detail = reciter,
                    onClick = {
                        reciter = nextOption(reciter, RECITER_OPTIONS)
                        PrefsCache.putReciter(reciter)
                        onPrefsChanged()
                    }
                )
                HorizontalDivider(color = pal.boneDark.copy(alpha = 0.6f), thickness = 1.dp)
                CyclingSettingRow(
                    pal = pal, fontBody = fontBody,
                    icon = Icons.Filled.Translate, label = "Translation", detail = translation,
                    onClick = {
                        translation = nextOption(translation, TRANSLATION_OPTIONS)
                        PrefsCache.putTranslation(translation)
                        onPrefsChanged()
                    }
                )
                HorizontalDivider(color = pal.boneDark.copy(alpha = 0.6f), thickness = 1.dp)
                CyclingSettingRow(
                    pal = pal, fontBody = fontBody,
                    icon = Icons.Filled.MenuBook, label = "Mushaf", detail = mushaf,
                    onClick = {
                        mushaf = nextOption(mushaf, MUSHAF_OPTIONS)
                        PrefsCache.putMushaf(mushaf)
                        onPrefsChanged()
                    }
                )
                HorizontalDivider(color = pal.boneDark.copy(alpha = 0.6f), thickness = 1.dp)
                CyclingSettingRow(
                    pal = pal, fontBody = fontBody,
                    icon = Icons.Filled.TextFields, label = "Arabic Font", detail = font,
                    onClick = {
                        font = nextOption(font, DesktopFonts.names)
                        PrefsCache.putFont(font)
                        onPrefsChanged()
                    }
                )
                HorizontalDivider(color = pal.boneDark.copy(alpha = 0.6f), thickness = 1.dp)
                ToggleSettingRow(
                    pal = pal, fontBody = fontBody,
                    icon = Icons.Filled.Contrast, label = "Tajweed Colors",
                    checked = tajweed,
                    onToggle = {
                        tajweed = it
                        PrefsCache.putTajweedEnabled(it)
                        onPrefsChanged()
                    }
                )
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
        ProfileSectionHeaderDesktop(pal = pal, title = "APPEARANCE")
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = pal.cream),
            border = BorderStroke(1.5.dp, pal.boneDark)
        ) {
            CyclingSettingRow(
                pal = pal, fontBody = fontBody,
                icon = if (dark) Icons.Filled.DarkMode else Icons.Filled.LightMode,
                label = "Theme",
                detail = if (dark) "Dark" else "Light",
                onClick = onToggleTheme
            )
        }
    }
}

@Composable
private fun ProfileSectionHeaderDesktop(pal: NurPalette, title: String) {
    Text(
        text = title,
        fontFamily = FontFamily.Monospace,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.4.sp,
        color = pal.inkMuted,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
    )
}

@Composable
private fun CyclingSettingRow(
    pal: NurPalette,
    fontBody: FontFamily,
    icon: ImageVector,
    label: String,
    detail: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f, fill = false),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(pal.goldLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = pal.gold, modifier = Modifier.size(18.dp))
            }
            Text(
                text = label,
                fontFamily = fontBody,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = pal.ink
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = detail,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = pal.inkMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 140.dp)
            )
            Icon(
                imageVector = Icons.Filled.Refresh,
                contentDescription = "Cycle option",
                tint = pal.inkMuted.copy(alpha = 0.5f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun ToggleSettingRow(
    pal: NurPalette,
    fontBody: FontFamily,
    icon: ImageVector,
    label: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle(!checked) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f, fill = false),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(pal.goldLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = pal.gold, modifier = Modifier.size(18.dp))
            }
            Text(
                text = label,
                fontFamily = fontBody,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = pal.ink
            )
        }
        Switch(checked = checked, onCheckedChange = onToggle)
    }
}

// ── Local backup card ──────────────────────────────────────────────────────
@Composable
private fun ProfileBackupCardDesktop(
    pal: NurPalette,
    fontUi: FontFamily,
    fontBody: FontFamily
) {
    var status by remember {
        mutableStateOf("Desktop edition — data stays on this device")
    }
    var busy by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        ProfileSectionHeaderDesktop(pal = pal, title = "LOCAL BACKUP")
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = pal.cream),
            border = BorderStroke(1.5.dp, pal.boneDark)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(pal.goldLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Save,
                            contentDescription = null,
                            tint = pal.gold,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Back up your data",
                            fontFamily = fontUi,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = pal.ink
                        )
                        Text(
                            text = "Export or restore bookmarks, settings and progress.",
                            fontFamily = fontBody,
                            fontSize = 12.sp,
                            color = pal.inkMuted
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    BackupButton(
                        pal = pal, fontUi = fontUi,
                        icon = Icons.Filled.Save, label = "Export",
                        enabled = !busy,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            busy = true
                            try {
                                val chooser = JFileChooser()
                                chooser.dialogTitle = "Export Quran Nur backup"
                                chooser.selectedFile = File("quran-nur-backup.json")
                                if (chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
                                    val target = chooser.selectedFile
                                    val ok = runCatching { BackupStore.exportToFile(target) }
                                        .getOrDefault(false)
                                    status = if (ok) {
                                        "Backup saved to ${target.absolutePath}"
                                    } else {
                                        "Export failed — please try again"
                                    }
                                }
                            } catch (e: Exception) {
                                status = "Export failed: ${e.message}"
                            } finally {
                                busy = false
                            }
                        }
                    )
                    BackupButton(
                        pal = pal, fontUi = fontUi,
                        icon = Icons.Filled.FolderOpen, label = "Import",
                        enabled = !busy,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            busy = true
                            try {
                                val chooser = JFileChooser()
                                chooser.dialogTitle = "Import Quran Nur backup"
                                if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                                    val source = chooser.selectedFile
                                    val ok = runCatching { BackupStore.importFromFile(source) }
                                        .getOrDefault(false)
                                    status = if (ok) {
                                        "Backup restored from ${source.name}"
                                    } else {
                                        "Import failed — invalid backup file"
                                    }
                                }
                            } catch (e: Exception) {
                                status = "Import failed: ${e.message}"
                            } finally {
                                busy = false
                            }
                        }
                    )
                }
                Text(
                    text = status,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = pal.inkMuted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun BackupButton(
    pal: NurPalette,
    fontUi: FontFamily,
    icon: ImageVector,
    label: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (enabled) pal.teal else pal.boneDark)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = label,
                fontFamily = fontUi,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

// ── Danger zone ────────────────────────────────────────────────────────────
@Composable
private fun ProfileDangerZoneDesktop(
    pal: NurPalette,
    fontUi: FontFamily,
    fontBody: FontFamily,
    onCleared: () -> Unit
) {
    var message by remember { mutableStateOf<String?>(null) }
    val danger = Color(0xFFDC2626)

    Column(modifier = Modifier.fillMaxWidth()) {
        ProfileSectionHeaderDesktop(pal = pal, title = "DANGER ZONE")
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = pal.cream),
            border = BorderStroke(1.5.dp, danger.copy(alpha = 0.4f))
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = null,
                        tint = danger,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "These actions cannot be undone.",
                        fontFamily = fontBody,
                        fontSize = 13.sp,
                        color = pal.inkMuted
                    )
                }
                DangerButton(pal = pal, fontUi = fontUi, danger = danger, label = "Reset preferences") {
                    runCatching { PrefsCache.clear() }
                    message = "Preferences reset to defaults"
                    onCleared()
                }
                DangerButton(pal = pal, fontUi = fontUi, danger = danger, label = "Clear bookmarks") {
                    val count = runCatching {
                        val keys = BookmarkStore.all().toList()
                        keys.forEach { BookmarkStore.toggle(it) }
                        keys.size
                    }.getOrDefault(0)
                    message = "Removed $count bookmark(s)"
                    onCleared()
                }
                if (message != null) {
                    Text(
                        text = message!!,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = pal.inkMuted
                    )
                }
            }
        }
    }
}

@Composable
private fun DangerButton(
    pal: NurPalette,
    fontUi: FontFamily,
    danger: Color,
    label: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.5.dp, danger.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontFamily = fontUi,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = danger
        )
    }
}

// ── Footer ─────────────────────────────────────────────────────────────────
@Composable
private fun ProfileFooterDesktop(pal: NurPalette, fontUi: FontFamily) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "Quran Nur • Desktop edition",
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.4.sp,
            color = pal.inkMuted
        )
        Text(
            text = "Made with care for the Ummah",
            fontFamily = fontUi,
            fontSize = 13.sp,
            color = pal.inkMuted.copy(alpha = 0.8f)
        )
    }
}

private fun formatMinutes(minutes: Int): String {
    if (minutes < 60) return "${minutes}m"
    val h = minutes / 60
    val r = minutes % 60
    return if (r > 0) "${h}h ${r}m" else "${h}h"
}
