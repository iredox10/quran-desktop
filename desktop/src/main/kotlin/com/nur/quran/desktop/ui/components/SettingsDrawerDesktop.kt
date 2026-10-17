package com.nur.quran.desktop.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.nur.quran.desktop.PrefsCache
import com.nur.quran.desktop.data.AudioEngine
import com.nur.quran.desktop.data.BackupStore
import com.nur.quran.desktop.data.TranslationStore
import com.nur.quran.shared.Mushaf
import com.nur.quran.shared.Reciters
import com.nur.quran.shared.fontNameToId
import com.nur.quran.desktop.ui.theme.NurPalette
import com.nur.quran.desktop.ui.theme.rememberBodyFontFamily
import com.nur.quran.desktop.ui.theme.rememberUiFontFamily
import java.io.File
import java.net.NetworkInterface
import javax.swing.JFileChooser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ── Views (mirrors web `VIEWS` map in SettingsDrawer.jsx) ──────────────────────
private const val VIEW_ROOT = "root"
private const val VIEW_MUSHAF = "mushaf"
private const val VIEW_TRANSLATION = "translation"
private const val VIEW_RECITER = "reciter"
private const val VIEW_FONT = "arabicFont"
private const val VIEW_TAFSIR = "tafsir"
private const val VIEW_SYNC = "sync"

private val TABS = listOf("General", "Reading", "Data")
private val WORD_HOVER_OPTIONS = listOf("None", "Translation", "Tajweed")

/** The desktop edition bundles exactly one tafsir (Ibn Kathir, English). */
private const val TAFSIR_NAME = "Ibn Kathir (Abridged)"

/** Picker/section titles — web header shows these with a back button. */
private fun viewTitle(view: String): String = when (view) {
    VIEW_MUSHAF -> "Choose Mushaf"
    VIEW_TRANSLATION -> "Choose Translation"
    VIEW_RECITER -> "Choose Reciter"
    VIEW_FONT -> "Choose Arabic Font"
    VIEW_TAFSIR -> "Choose Tafsir"
    VIEW_SYNC -> "Cloud Sync"
    else -> "Settings"
}

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1_048_576L -> "%.1f MB".format(bytes / 1_048_576.0)
    bytes >= 1024L -> "%.0f KB".format(bytes / 1024.0)
    else -> "$bytes B"
}

/** Best-effort link check: any non-loopback interface that is up and has addresses. */
private fun isOnline(): Boolean = runCatching {
    NetworkInterface.getNetworkInterfaces()?.toList()?.any { ni ->
        ni.isUp && !ni.isLoopback && ni.interfaceAddresses.isNotEmpty()
    } == true
}.getOrDefault(false)

/**
 * Desktop Settings dialog — structure mirrors the WEB `SettingsDrawer.jsx`
 * exactly:
 *
 * Header: [back when in a sub-view] title (+ root subtitle) ... close.
 * Root: segmented General | Reading | Data tabs.
 *   General  → Appearance (Light/Dark segmented) + Essentials card of
 *              SelectionRows (Mushaf / Translation / Reciter / Cloud Sync)
 *              that navigate into sub-view pickers.
 *   Reading  → Text Preferences card (Arabic Font / Tafsir rows, 3D Mushaf
 *              Mode, Tajweed, Show translation, Word Hover Action) + Sizes
 *              card (Arabic Size / Translation Size sliders).
 *   Data     → Local Audio card + Offline Library card (online badge, stat
 *              boxes, Open Downloads) + footer credit.
 * Sub-views: Choose Mushaf / Choose Translation / Choose Reciter (grouped
 *            by style) / Choose Arabic Font / Choose Tafsir / Cloud Sync
 *            (desktop: local backup export+import, since there is no backend).
 *
 * Everything persists to [PrefsCache] immediately; [tick] re-reads prefs after
 * import/reset. After dismiss the coordinator re-reads prefs via its own
 * settingsTick — no extra callback needed.
 */
@Composable
fun SettingsDrawerDesktop(
    pal: NurPalette,
    dark: Boolean,
    onToggleTheme: () -> Unit,
    onDismiss: () -> Unit,
    onOpenDownloads: () -> Unit = {}
) {
    val fontUi = rememberUiFontFamily()
    val fontBody = rememberBodyFontFamily()
    var activeView by remember { mutableStateOf(VIEW_ROOT) }
    var tab by remember { mutableStateOf(TABS.first()) }
    var tick by remember { mutableStateOf(0) }

    var translation by remember(tick) { mutableStateOf(PrefsCache.getTranslation()) }
    var mushaf by remember(tick) { mutableStateOf(PrefsCache.getMushaf()) }
    var font by remember(tick) { mutableStateOf(PrefsCache.getFont()) }
    var arabicScale by remember(tick) { mutableStateOf(PrefsCache.getArabicScale()) }
    var translationScale by remember(tick) { mutableStateOf(PrefsCache.getTranslationScale()) }
    var showTranslation by remember(tick) { mutableStateOf(PrefsCache.getReaderTranslationEnabled()) }
    var tajweed by remember(tick) { mutableStateOf(PrefsCache.getTajweedEnabled()) }
    var mushafMode by remember(tick) { mutableStateOf(PrefsCache.getMushafMode()) }
    var backupStatus by remember { mutableStateOf("") }

    val mushafObj = remember(mushaf) { Mushaf.fromId(mushaf) }
    val supportsTajweed = mushafObj.supportsTajweedToggle
    val tajweedEffective = Mushaf.isTajweedEffective(mushafObj, tajweed)
    val selectedTranslationName = remember(translation) {
        TranslationStore.nameOf(translation.toIntOrNull() ?: TranslationStore.BUNDLED_ID)
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = pal.surface,
            border = BorderStroke(1.5.dp, pal.boneDark)
        ) {
            Column(
                modifier = Modifier.padding(20.dp).widthIn(min = 360.dp, max = 480.dp)
            ) {
                // ── Header (web parity): back + title/subtitle + close ──────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (activeView != VIEW_ROOT) {
                        IconButton(
                            onClick = { activeView = VIEW_ROOT },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = pal.inkMid
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = viewTitle(activeView),
                            fontFamily = fontUi,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = pal.ink
                        )
                        if (activeView == VIEW_ROOT) {
                            Text(
                                text = "Customize your reading experience",
                                fontFamily = fontBody,
                                fontSize = 11.sp,
                                color = pal.inkMuted
                            )
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close settings",
                            tint = pal.inkMuted
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                // ── Body (scrolls; sub-view replaces root) ───────────────────
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 620.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    if (activeView == VIEW_ROOT) {
                        // Segmented tabs: General | Reading | Data
                        SegmentedControl(
                            pal = pal,
                            fontUi = fontUi,
                            items = TABS,
                            selected = TABS.indexOf(tab),
                            onSelect = { tab = TABS[it] }
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        when (tab) {
                            "General" -> {
                                // ── Appearance ────────────────────────────
                                SectionHeader(pal, fontUi, "Appearance")
                                SegmentedControl(
                                    pal = pal,
                                    fontUi = fontUi,
                                    items = listOf("Light", "Dark"),
                                    icons = listOf(Icons.Filled.LightMode, Icons.Filled.DarkMode),
                                    selected = if (dark) 1 else 0,
                                    onSelect = { i -> if ((i == 1) != dark) onToggleTheme() }
                                )
                                Spacer(modifier = Modifier.height(16.dp))

                                // ── Essentials ────────────────────────────
                                SectionHeader(pal, fontUi, "Essentials")
                                Card(pal) {
                                    SelectionRow(
                                        pal = pal, fontUi = fontUi, fontBody = fontBody,
                                        label = "Mushaf",
                                        value = mushafObj.name
                                    ) { activeView = VIEW_MUSHAF }
                                    SelectionRow(
                                        pal = pal, fontUi = fontUi, fontBody = fontBody,
                                        label = "Translation",
                                        value = selectedTranslationName
                                    ) { activeView = VIEW_TRANSLATION }
                                    SelectionRow(
                                        pal = pal, fontUi = fontUi, fontBody = fontBody,
                                        label = "Reciter",
                                        value = Reciters.nameOf(AudioEngine.reciterId)
                                    ) { activeView = VIEW_RECITER }
                                    SelectionRow(
                                        pal = pal, fontUi = fontUi, fontBody = fontBody,
                                        label = "Cloud Sync",
                                        hint = "Backup your progress"
                                    ) { activeView = VIEW_SYNC }
                                }
                            }

                            "Reading" -> {
                                // ── Text Preferences ─────────────────────
                                SectionHeader(pal, fontUi, "Text Preferences")
                                Card(pal) {
                                    SelectionRow(
                                        pal = pal, fontUi = fontUi, fontBody = fontBody,
                                        label = "Arabic Font",
                                        value = font
                                    ) { activeView = VIEW_FONT }
                                    SelectionRow(
                                        pal = pal, fontUi = fontUi, fontBody = fontBody,
                                        label = "Tafsir",
                                        value = TAFSIR_NAME
                                    ) { activeView = VIEW_TAFSIR }
                                    ToggleRow(
                                        pal = pal, fontUi = fontUi, fontBody = fontBody,
                                        label = "3D Mushaf Mode",
                                        hint = "Read page-by-page like a physical Mushaf",
                                        checked = mushafMode
                                    ) {
                                        mushafMode = it
                                        PrefsCache.putMushafMode(it)
                                    }
                                    ToggleRow(
                                        pal = pal, fontUi = fontUi, fontBody = fontBody,
                                        label = "Tajweed",
                                        hint = if (supportsTajweed) {
                                            "Color cues for pronunciation"
                                        } else {
                                            "Not available for this Mushaf"
                                        },
                                        checked = tajweedEffective,
                                        enabled = supportsTajweed
                                    ) {
                                        tajweed = it
                                        PrefsCache.putTajweedEnabled(it)
                                    }
                                    // Desktop addition: web keeps the translation always on;
                                    // the desktop reader has always had this switch, so it
                                    // stays reachable here (same row style as the web).
                                    ToggleRow(
                                        pal = pal, fontUi = fontUi, fontBody = fontBody,
                                        label = "Show translation",
                                        hint = "Show the translation under each verse",
                                        checked = showTranslation
                                    ) {
                                        showTranslation = it
                                        PrefsCache.putReaderTranslationEnabled(it)
                                    }
                                    // Structure parity: the row exists but the desktop has no
                                    // word-level tooltips yet, so the control is shown disabled.
                                    WordHoverActionRow(pal, fontUi, fontBody)
                                }
                                Spacer(modifier = Modifier.height(16.dp))

                                // ── Sizes ─────────────────────────────────
                                SectionHeader(pal, fontUi, "Sizes")
                                Card(pal) {
                                    SliderRow(
                                        pal = pal, fontUi = fontUi, fontBody = fontBody,
                                        label = "Arabic Size",
                                        value = arabicScale,
                                        range = 0.5f..2.0f,
                                        onChange = {
                                            arabicScale = it
                                            PrefsCache.putArabicScale(it)
                                        }
                                    )
                                    HorizontalDivider(color = pal.boneDark)
                                    SliderRow(
                                        pal = pal, fontUi = fontUi, fontBody = fontBody,
                                        label = "Translation Size",
                                        value = translationScale,
                                        range = 0.5f..2.0f,
                                        onChange = {
                                            translationScale = it
                                            PrefsCache.putTranslationScale(it)
                                        }
                                    )
                                }
                            }

                            "Data" -> {
                                val scope = rememberCoroutineScope()
                                var online by remember { mutableStateOf(false) }
                                var cacheFiles by remember { mutableStateOf(0) }
                                var cacheBytes by remember { mutableStateOf(0L) }
                                var audioStatus by remember { mutableStateOf("") }
                                LaunchedEffect(tick) {
                                    val stats = withContext(Dispatchers.IO) {
                                        val dir = File(
                                            System.getProperty("user.home"),
                                            ".quran-nur/audio/${AudioEngine.reciterId}"
                                        )
                                        val files = dir.listFiles { f ->
                                            f.isFile && f.name.endsWith(".mp3")
                                        }?.filter { it.length() > 0 }.orEmpty()
                                        Triple(files.size, files.sumOf { it.length() }, isOnline())
                                    }
                                    cacheFiles = stats.first
                                    cacheBytes = stats.second
                                    online = stats.third
                                }

                                // ── Local Audio (web: choose folder → cache mgmt) ──
                                SectionHeader(pal, fontUi, "Local Audio")
                                Card(pal) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth().padding(16.dp)
                                    ) {
                                        Text(
                                            text = "Local Offline Audio",
                                            fontFamily = fontUi,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = pal.ink
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Recitations stream from the reciter servers " +
                                                "and cache to disk — played verses replay offline.",
                                            fontFamily = fontBody,
                                            fontSize = 11.sp,
                                            color = pal.inkMuted
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text(
                                            text = "$cacheFiles verses • ${formatBytes(cacheBytes)} " +
                                                "cached for ${Reciters.nameOf(AudioEngine.reciterId)}",
                                            fontFamily = fontBody,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = pal.inkMid
                                        )
                                        if (audioStatus.isNotBlank()) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = audioStatus,
                                                fontFamily = fontBody,
                                                fontSize = 11.sp,
                                                color = pal.inkMuted
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(10.dp))
                                        OutlineButton(
                                            pal = pal, fontUi = fontUi,
                                            label = "Clear cached recitations",
                                            icon = Icons.Filled.Delete
                                        ) {
                                            audioStatus = ""
                                            scope.launch(Dispatchers.IO) {
                                                val dir = File(
                                                    System.getProperty("user.home"),
                                                    ".quran-nur/audio/${AudioEngine.reciterId}"
                                                )
                                                runCatching { dir.deleteRecursively() }
                                                audioStatus = "Cache cleared"
                                            }
                                            tick++
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))

                                // ── Offline Library (web parity) ──────────────────
                                SectionHeader(pal, fontUi, "Offline Library")
                                Card(pal) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth().padding(16.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "Offline Library",
                                                    fontFamily = fontUi,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = pal.ink
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = "Manage Quran packs bundled with this install.",
                                                    fontFamily = fontBody,
                                                    fontSize = 11.sp,
                                                    color = pal.inkMuted
                                                )
                                            }
                                            OnlineBadge(pal = pal, fontUi = fontUi, online = online)
                                        }
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            StatBox(
                                                pal = pal, fontBody = fontBody,
                                                label = "Quran text",
                                                value = "Bundled • 7.8 MB",
                                                modifier = Modifier.weight(1f)
                                            )
                                            StatBox(
                                                pal = pal, fontBody = fontBody,
                                                label = "Tafsir + Tajweed",
                                                value = "Bundled • offline",
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(12.dp))
                                        PrimaryButton(pal, fontUi, "Open Downloads") {
                                            onOpenDownloads()
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "built by iredox.tech",
                                    fontFamily = fontBody,
                                    fontSize = 11.sp,
                                    color = pal.inkMuted,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                                )
                            }
                        }
                    } else {
                        // ── Sub-views ─────────────────────────────────────────
                        when (activeView) {
                            VIEW_MUSHAF -> {
                                PickerList {
                                    Mushaf.ALL.forEach { m ->
                                        PickerOption(
                                            pal = pal, fontUi = fontUi, fontBody = fontBody,
                                            title = m.name,
                                            subtitle = m.description,
                                            active = m.id == mushafObj.id
                                        ) {
                                            mushaf = m.id
                                            PrefsCache.putMushaf(m.id)
                                            activeView = VIEW_ROOT
                                        }
                                    }
                                }
                            }

                            VIEW_TRANSLATION -> {
                                PickerList {
                                    TranslationStore.KNOWN_EDITIONS.forEach { (id, name) ->
                                        val subtitle = when {
                                            id == TranslationStore.BUNDLED_ID -> "Bundled offline"
                                            runCatching {
                                                TranslationStore.isCached(1, id)
                                            }.getOrDefault(false) -> "Downloaded for offline use"
                                            else -> "Downloads on first use"
                                        }
                                        PickerOption(
                                            pal = pal, fontUi = fontUi, fontBody = fontBody,
                                            title = name,
                                            subtitle = subtitle,
                                            active = id == (translation.toIntOrNull()
                                                ?: TranslationStore.BUNDLED_ID)
                                        ) {
                                            translation = id.toString()
                                            PrefsCache.putTranslation(id.toString())
                                            activeView = VIEW_ROOT
                                        }
                                    }
                                }
                            }

                            VIEW_RECITER -> {
                                // Web parity: grouped by style with uppercase headers.
                                Column(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Reciters.groupedByStyle().forEach { (style, group) ->
                                        Column {
                                            SectionHeader(pal, fontUi, style)
                                            group.forEach { reciter ->
                                                PickerOption(
                                                    pal = pal, fontUi = fontUi, fontBody = fontBody,
                                                    title = reciter.name,
                                                    active = reciter.id == AudioEngine.reciterId
                                                ) {
                                                    AudioEngine.reciterId = reciter.id
                                                    activeView = VIEW_ROOT
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            VIEW_FONT -> {
                                PickerList {
                                    DesktopFonts.names.forEach { name ->
                                        val fontId = fontNameToId(name)
                                        val compatible =
                                            Mushaf.compatibleFontId(mushafObj, fontId) == fontId
                                        val sample = if (name == DesktopFonts.SYSTEM_DEFAULT) {
                                            null
                                        } else {
                                            DesktopFonts.rememberFontFamily(name)
                                        }
                                        PickerOption(
                                            pal = pal, fontUi = fontUi, fontBody = fontBody,
                                            title = name,
                                            subtitle = if (compatible) {
                                                "Compatible with ${mushafObj.name}"
                                            } else {
                                                null
                                            },
                                            active = name == font,
                                            sampleFont = sample
                                        ) {
                                            font = name
                                            PrefsCache.putFont(name)
                                            activeView = VIEW_ROOT
                                        }
                                    }
                                }
                            }

                            VIEW_TAFSIR -> {
                                PickerList {
                                    PickerOption(
                                        pal = pal, fontUi = fontUi, fontBody = fontBody,
                                        title = TAFSIR_NAME,
                                        subtitle = "English • bundled offline",
                                        active = true
                                    ) { activeView = VIEW_ROOT }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "The desktop edition ships Ibn Kathir; more " +
                                            "editions arrive with the translations service.",
                                        fontFamily = fontBody,
                                        fontSize = 11.sp,
                                        color = pal.inkMuted,
                                        modifier = Modifier.padding(horizontal = 14.dp)
                                    )
                                }
                            }

                            VIEW_SYNC -> {
                                // Web parity: Cloud Sync view slot. The desktop has no
                                // backend, so this view carries the local backup card.
                                Column(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Card(pal) {
                                        Column(
                                            modifier = Modifier.fillMaxWidth().padding(16.dp)
                                        ) {
                                            Text(
                                                text = "Cloud Sync",
                                                fontFamily = fontUi,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = pal.ink
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "Cloud backup isn't available in the desktop " +
                                                    "edition — bookmarks, plans and progress are stored " +
                                                    "on this device.",
                                                fontFamily = fontBody,
                                                fontSize = 11.sp,
                                                color = pal.inkMuted
                                            )
                                        }
                                    }
                                    SectionHeader(pal, fontUi, "Backup & Restore")
                                    Card(pal) {
                                        Column(
                                            modifier = Modifier.fillMaxWidth().padding(16.dp)
                                        ) {
                                            Text(
                                                text = "Local Backup",
                                                fontFamily = fontUi,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = pal.ink
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "Export everything to a JSON file and restore " +
                                                    "it on any device.",
                                                fontFamily = fontBody,
                                                fontSize = 11.sp,
                                                color = pal.inkMuted
                                            )
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                ActionButton(
                                                    pal = pal, fontUi = fontUi, label = "Export",
                                                    modifier = Modifier.weight(1f),
                                                    onClick = {
                                                        try {
                                                            val chooser = JFileChooser()
                                                            chooser.dialogTitle = "Export Quran Nur backup"
                                                            chooser.selectedFile = File("quran-nur-backup.json")
                                                            if (chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
                                                                val target = chooser.selectedFile
                                                                val ok = runCatching {
                                                                    BackupStore.exportToFile(target)
                                                                }.getOrDefault(false)
                                                                backupStatus = if (ok) {
                                                                    "Backup saved to ${target.absolutePath}"
                                                                } else {
                                                                    "Export failed — please try again"
                                                                }
                                                                if (ok) tick++
                                                            }
                                                        } catch (e: Exception) {
                                                            backupStatus = "Export failed: ${e.message}"
                                                        }
                                                    }
                                                )
                                                ActionButton(
                                                    pal = pal, fontUi = fontUi, label = "Import",
                                                    modifier = Modifier.weight(1f),
                                                    onClick = {
                                                        try {
                                                            val chooser = JFileChooser()
                                                            chooser.dialogTitle = "Import Quran Nur backup"
                                                            if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                                                                val source = chooser.selectedFile
                                                                val ok = runCatching {
                                                                    BackupStore.importFromFile(source)
                                                                }.getOrDefault(false)
                                                                backupStatus = if (ok) {
                                                                    "Backup restored from ${source.name}"
                                                                } else {
                                                                    "Import failed — invalid backup file"
                                                                }
                                                                if (ok) tick++
                                                            }
                                                        } catch (e: Exception) {
                                                            backupStatus = "Import failed: ${e.message}"
                                                        }
                                                    }
                                                )
                                            }
                                            if (backupStatus.isNotBlank()) {
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text(
                                                    text = backupStatus,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontSize = 11.sp,
                                                    color = pal.inkMuted
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Primitives (mirrors web SelectionRow / ToggleRow / PickerOption /
//    SegmentedOption / section labels + bordered cards) ───────────────────────

/** Uppercase section label: `text-[0.72rem] font-bold uppercase tracking-[0.08em]`. */
@Composable
private fun SectionHeader(pal: NurPalette, fontUi: FontFamily, text: String) {
    Text(
        text = text.uppercase(),
        fontFamily = fontUi,
        fontSize = 10.sp,
        letterSpacing = 0.8.sp,
        fontWeight = FontWeight.Bold,
        color = pal.inkMuted,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
    )
}

/** Bordered rounded card grouping rows: `rounded-[12px] border bg-primary`. */
@Composable
private fun Card(pal: NurPalette, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(pal.cream)
            .border(1.dp, pal.boneDark, RoundedCornerShape(12.dp)),
        content = content
    )
}

/**
 * Flush segmented control (web `SegmentedOption` row): bordered rounded box,
 * equal-weight segments, active = accent fill + white label.
 */
@Composable
private fun SegmentedControl(
    pal: NurPalette,
    fontUi: FontFamily,
    items: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    icons: List<ImageVector?> = emptyList()
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, pal.boneDark, RoundedCornerShape(12.dp))
            .background(pal.cream)
    ) {
        items.forEachIndexed { i, label ->
            val active = i == selected
            Row(
                modifier = Modifier
                    .weight(1f)
                    .background(if (active) pal.gold else Color.Transparent)
                    .clickable { onSelect(i) }
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                icons.getOrNull(i)?.let { icon ->
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (active) pal.white else pal.inkMid,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text(
                    text = label,
                    fontFamily = fontUi,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (active) pal.white else pal.inkMid
                )
            }
        }
    }
}

/** Web `SelectionRow`: label (+ value/hint line) with a trailing chevron. */
@Composable
private fun SelectionRow(
    pal: NurPalette,
    fontUi: FontFamily,
    fontBody: FontFamily,
    label: String,
    value: String? = null,
    hint: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontFamily = fontUi,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = pal.ink
            )
            if (value != null) {
                Text(
                    text = value,
                    fontFamily = fontBody,
                    fontSize = 11.sp,
                    color = pal.inkMuted
                )
            } else if (hint != null) {
                Text(
                    text = hint,
                    fontFamily = fontBody,
                    fontSize = 11.sp,
                    color = pal.inkMuted
                )
            }
        }
        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = pal.inkMuted,
            modifier = Modifier.size(16.dp)
        )
    }
}

/** Web `ToggleRow`: label + hint left, switch right; disabled = 50% opacity. */
@Composable
private fun ToggleRow(
    pal: NurPalette,
    fontUi: FontFamily,
    fontBody: FontFamily,
    label: String,
    hint: String,
    checked: Boolean,
    enabled: Boolean = true,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.5f)
            .clickable(enabled = enabled) { onToggle(!checked) }
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontFamily = fontUi,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = pal.ink
            )
            Text(
                text = hint,
                fontFamily = fontBody,
                fontSize = 11.sp,
                color = pal.inkMuted
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(checked = checked, onCheckedChange = onToggle, enabled = enabled)
    }
}

/** Picker sub-view list: `flex flex-col gap-1 px-3 py-3`. */
@Composable
private fun PickerList(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        content = content
    )
}

/** Web `PickerOption`: rounded option with optional subtitle/check/sample font. */
@Composable
private fun PickerOption(
    pal: NurPalette,
    fontUi: FontFamily,
    fontBody: FontFamily,
    title: String,
    subtitle: String? = null,
    active: Boolean,
    sampleFont: FontFamily? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (active) pal.goldSoft else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontFamily = sampleFont ?: fontUi,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = pal.ink
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    fontFamily = fontBody,
                    fontSize = 11.sp,
                    color = pal.inkMuted
                )
            }
        }
        if (active) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = pal.gold,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

/**
 * Web `Word Hover Action` segmented row. The desktop has no word-level
 * tooltips yet, so the whole block renders disabled (structure parity without
 * a dead control).
 */
@Composable
private fun WordHoverActionRow(pal: NurPalette, fontUi: FontFamily, fontBody: FontFamily) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(0.5f)
            .padding(horizontal = 14.dp, vertical = 11.dp)
    ) {
        Text(
            text = "Word Hover Action",
            fontFamily = fontUi,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = pal.ink
        )
        Text(
            text = "Word-level taps arrive in a later desktop update.",
            fontFamily = fontBody,
            fontSize = 11.sp,
            color = pal.inkMuted
        )
        Spacer(modifier = Modifier.height(8.dp))
        SegmentedControl(
            pal = pal,
            fontUi = fontUi,
            items = WORD_HOVER_OPTIONS,
            selected = 0,
            onSelect = {}
        )
    }
}

/** Web Sizes card row: label + value on one line, range slider below. */
@Composable
private fun SliderRow(
    pal: NurPalette,
    fontUi: FontFamily,
    fontBody: FontFamily,
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontFamily = fontUi,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = pal.ink
            )
            Text(
                text = "${(value * 100).toInt()}%",
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = pal.inkMuted
            )
        }
        Slider(value = value, onValueChange = onChange, valueRange = range)
    }
}

/** Web's full-width bordered button (`Choose Audio Folder` style). */
@Composable
private fun OutlineButton(
    pal: NurPalette,
    fontUi: FontFamily,
    label: String,
    icon: ImageVector? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .border(2.dp, pal.boneDark, RoundedCornerShape(10.dp))
            .background(Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = pal.ink,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(
            text = label,
            fontFamily = fontUi,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = pal.ink
        )
    }
}

/** Web's accent-filled full-width button (`Open Offline Library` style). */
@Composable
private fun PrimaryButton(
    pal: NurPalette,
    fontUi: FontFamily,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(pal.gold)
            .clickable(onClick = onClick)
            .padding(vertical = 11.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontFamily = fontUi,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@Composable
private fun ActionButton(
    pal: NurPalette,
    fontUi: FontFamily,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(pal.teal)
            .clickable(onClick = onClick)
            .padding(vertical = 11.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontFamily = fontUi,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

/** Web's Online/Offline chip: green when connected, red when offline. */
@Composable
private fun OnlineBadge(pal: NurPalette, fontUi: FontFamily, online: Boolean) {
    val color = if (online) Color(0xFF16A34A) else Color(0xFFDC2626)
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (online) "Online" else "Offline",
            fontFamily = fontUi,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

/** Web's stat box inside the Offline Library card. */
@Composable
private fun StatBox(
    pal: NurPalette,
    fontBody: FontFamily,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(pal.white)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            fontFamily = fontBody,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = pal.inkMuted
        )
        Text(
            text = value,
            fontFamily = fontBody,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = pal.ink
        )
    }
}
