package com.nur.quran.desktop.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import com.nur.quran.desktop.data.AudioEngine
import com.nur.quran.desktop.data.QuranStore
import com.nur.quran.desktop.data.TranslationStore
import com.nur.quran.desktop.ui.components.DesktopFonts
import com.nur.quran.desktop.ui.theme.NurPalette
import com.nur.quran.desktop.ui.theme.rememberBodyFontFamily
import com.nur.quran.desktop.ui.theme.rememberUiFontFamily
import com.nur.quran.shared.Reciters
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class BundledPack(
    val title: String,
    val detail: String,
    val resourcePaths: List<String>,
    val icon: ImageVector,
)

private data class AudioReciter(val name: String, val style: String)

/**
 * Desktop Downloads screen mirroring Android `DownloadsScreen`:
 * header + storage summary + one flat row per pack/reciter.
 *
 * Desktop reality: everything ships bundled offline — there is no download
 * backend, so bundled packs show a green "Bundled • X.X MB" chip (sizes read
 * live from classpath resources) and audio reciters show a muted
 * "Streaming not supported yet" chip with no action buttons.
 */
@Composable
fun DownloadsScreenDesktop(pal: NurPalette, onBack: () -> Unit = {}) {
    val fontUi = rememberUiFontFamily()
    val fontBody = rememberBodyFontFamily()

    val arabicFontNames = remember {
        DesktopFonts.names.filter { DesktopFonts.fileFor(it) != null }
    }
    val arabicFontPaths = remember(arabicFontNames) {
        arabicFontNames.mapNotNull { DesktopFonts.fileFor(it) }
    }
    val uiFontPaths = remember {
        listOf(
            "fonts/cormorant_garamond_regular.ttf",
            "fonts/cormorant_garamond_bold.ttf",
            "fonts/piazzolla_regular.ttf",
            "fonts/piazzolla_bold.ttf",
        )
    }
    val fontDisplayNames = remember(arabicFontNames) {
        arabicFontNames + listOf("Cormorant Garamond", "Piazzolla")
    }

    val bundledPacks = remember(arabicFontPaths) {
        listOf(
            BundledPack(
                title = "Quran text (Uthmani + translation)",
                detail = "Full mushaf text bundled offline",
                resourcePaths = listOf("data/quran_full.json"),
                icon = Icons.Filled.MenuBook,
            ),
            BundledPack(
                title = "Surah metadata",
                detail = "114 surah names, order and revelation info",
                resourcePaths = listOf("data/chapters.json"),
                icon = Icons.Filled.LibraryBooks,
            ),
            BundledPack(
                title = "Tafsir — Ibn Kathir",
                detail = "Verse commentary bundled offline",
                resourcePaths = listOf("data/tafsir_ibn_kathir.json"),
                icon = Icons.Filled.MenuBook,
            ),
            BundledPack(
                title = "Arabic fonts (${fontDisplayNames.size} families)",
                detail = fontDisplayNames.joinToString(", "),
                resourcePaths = arabicFontPaths + uiFontPaths,
                icon = Icons.Filled.TextFields,
            ),
        )
    }

    val packSizes: List<Long> = bundledPacks.map { pack ->
        remember(pack.title) { pack.resourcePaths.sumOf { resourceBytes("/$it") } }
    }
    val totalBytes = remember(packSizes) { packSizes.sum() }

    val reciters = remember { desktopAudioReciters }

    // ── Bundled-pack integrity verification (read-only; never downloads) ──
    val verifyScope = rememberCoroutineScope()
    var verifying by remember { mutableStateOf(false) }
    var packVerdicts by remember { mutableStateOf<Map<Int, Boolean>>(emptyMap()) }

    fun verifyPacks() {
        if (verifying) return
        verifying = true
        verifyScope.launch(Dispatchers.IO) {
            val results = bundledPacks.indices.associateWith { index ->
                verifyBundledPack(bundledPacks[index])
            }
            packVerdicts = results
            verifying = false
        }
    }

    // ── Available translation packs (append-only section; bundled UI above untouched)
    val packScope = rememberCoroutineScope()
    var packTick by remember { mutableStateOf(0) }
    var busyPackId by remember { mutableStateOf<Int?>(null) }
    var packStatus by remember { mutableStateOf<Map<Int, String>>(emptyMap()) }

    fun downloadFatiha(resId: Int) {
        if (busyPackId != null || resId == TranslationStore.BUNDLED_ID) return
        busyPackId = resId
        packStatus = packStatus + (resId to "Downloading Al-Fatiha…")
        packScope.launch {
            val result = withContext(Dispatchers.IO) { TranslationStore.getChapter(1, resId) }
            packStatus = packStatus + (
                resId to if (result != null) "Cached • ${result.size} verses offline"
                else "Failed • check connection and retry"
            )
            if (result != null) packTick++
            busyPackId = null
        }
    }

    fun deleteTranslationPack(resId: Int) {
        if (busyPackId != null) return
        packScope.launch {
            val ok = withContext(Dispatchers.IO) { TranslationStore.deletePack(resId) }
            packStatus = packStatus + (resId to if (ok) "Deleted • cache cleared" else "Delete failed • retry")
            packTick++
        }
    }

    // ── Offline recitation (audio cache manager; append-only section) ──
    val audioScope = rememberCoroutineScope()
    var audioTick by remember { mutableStateOf(0) }
    var audioStatus by remember { mutableStateOf("") }
    var downloadJob by remember { mutableStateOf<Job?>(null) }
    var cacheBytes by remember(audioTick, AudioEngine.reciterId) { mutableStateOf(0L) }
    var cacheFiles by remember(audioTick, AudioEngine.reciterId) { mutableStateOf(0) }

    LaunchedEffect(audioTick, AudioEngine.reciterId) {
        val stats = withContext(Dispatchers.IO) {
            val dir = File(System.getProperty("user.home"), ".quran-nur/audio/${AudioEngine.reciterId}")
            val files = dir.listFiles { f -> f.isFile && f.name.endsWith(".mp3") }
                ?.filter { it.length() > 0 }
                .orEmpty()
            files.size to files.sumOf { it.length() }
        }
        cacheFiles = stats.first
        cacheBytes = stats.second
    }

    fun downloadJuzAmma() {
        if (downloadJob?.isActive == true) return
        audioStatus = "Starting…"
        downloadJob = audioScope.launch(Dispatchers.IO) {
            val reciter = AudioEngine.reciterId
            val dir = File(System.getProperty("user.home"), ".quran-nur/audio/$reciter")
            val verses = (78..114).flatMap { ch -> QuranStore.versesOfChapter(ch) }
            var done = 0
            var failed = 0
            try {
                for (verse in verses) {
                    currentCoroutineContext().ensureActive()
                    val file = File(dir, "${verse.chapterId}_${verse.verseNumber}.mp3")
                    if (file.isFile && file.length() > 0) {
                        done++
                        audioStatus = "$done / ${verses.size} (cached)"
                        continue
                    }
                    val url = Reciters.buildAudioUrl(reciter, verse.verseKey)
                    if (url == null) {
                        failed++
                        continue
                    }
                    try {
                        val conn = URL(url).openConnection() as HttpURLConnection
                        conn.connectTimeout = 15_000
                        conn.readTimeout = 15_000
                        conn.instanceFollowRedirects = true
                        try {
                            conn.connect()
                            val bytes = conn.inputStream.use { it.readBytes() }
                            dir.mkdirs()
                            file.writeBytes(bytes)
                            done++
                        } finally {
                            conn.disconnect()
                        }
                    } catch (_: Exception) {
                        failed++
                    }
                    audioStatus = "$done / ${verses.size}…"
                }
                audioStatus = if (failed == 0) "Complete • $done verses offline"
                else "Done with $failed failed • $done cached"
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) {
                    audioStatus = "Cancelled • $done / ${verses.size} downloaded"
                } else {
                    audioStatus = "Failed • ${e.message ?: "check connection"}"
                }
            }
            audioTick++
        }
    }

    fun clearAudioCache() {
        if (downloadJob?.isActive == true) return
        audioScope.launch(Dispatchers.IO) {
            val dir = File(System.getProperty("user.home"), ".quran-nur/audio/${AudioEngine.reciterId}")
            runCatching { dir.deleteRecursively() }
            audioStatus = "Cache cleared"
            audioTick++
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(pal.white)) {
        DownloadsTopBar(pal = pal, fontUi = fontUi, onBack = onBack)
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                modifier = Modifier.widthIn(max = 640.dp).fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    Column {
                        Text(
                            text = "Downloads",
                            fontFamily = fontUi,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = pal.ink,
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Offline library • everything ships with the app",
                            fontFamily = fontBody,
                            fontSize = 12.sp,
                            color = pal.inkMuted,
                        )
                    }
                }

                item {
                    StorageSummaryCard(
                        pal = pal,
                        fontUi = fontUi,
                        fontBody = fontBody,
                        totalBytes = totalBytes,
                        packCount = bundledPacks.size,
                    )
                }

                item {
                    SectionHeader(pal = pal, title = "BUNDLED PACKS • ${bundledPacks.size}")
                }

                item {
                    VerifyPacksRow(
                        pal = pal,
                        fontUi = fontUi,
                        fontBody = fontBody,
                        verifying = verifying,
                        healthy = packVerdicts.values.count { it },
                        total = bundledPacks.size,
                        checked = packVerdicts.isNotEmpty(),
                        onVerify = { verifyPacks() },
                    )
                }

                items(bundledPacks.indices.toList()) { index ->
                    val pack = bundledPacks[index]
                    BundledPackRow(
                        pal = pal,
                        fontUi = fontUi,
                        fontBody = fontBody,
                        title = pack.title,
                        detail = pack.detail,
                        sizeBytes = packSizes[index],
                        icon = pack.icon,
                        verdict = packVerdicts[index],
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    SectionHeader(pal = pal, title = "AUDIO PACKS • ${reciters.size} RECITERS")
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Offline audio arrives with a future update.",
                        fontFamily = fontBody,
                        fontSize = 12.sp,
                        color = pal.inkMuted,
                    )
                }

                items(reciters) { reciter ->
                    AudioReciterRow(pal = pal, fontUi = fontUi, fontBody = fontBody, reciter = reciter)
                }

                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    SectionHeader(
                        pal = pal,
                        title = "AVAILABLE TRANSLATION PACKS • ${TranslationStore.KNOWN_EDITIONS.size}"
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Download Al-Fatiha to test an edition. Chapters load on demand and stay cached offline.",
                        fontFamily = fontBody,
                        fontSize = 12.sp,
                        color = pal.inkMuted,
                    )
                }

                items(TranslationStore.KNOWN_EDITIONS) { edition ->
                    val (resId, name) = edition
                    val cached = remember(packTick, resId) { TranslationStore.hasAnyCache(resId) }
                    val status = packStatus[resId] ?: when {
                        resId == TranslationStore.BUNDLED_ID -> "Bundled • built-in translation"
                        cached -> "Cached • Al-Fatiha offline"
                        else -> "Not downloaded"
                    }
                    TranslationPackRow(
                        pal = pal,
                        fontUi = fontUi,
                        fontBody = fontBody,
                        name = name,
                        resId = resId,
                        status = status,
                        cached = cached,
                        busy = busyPackId == resId,
                        actionsEnabled = busyPackId == null,
                        onDownload = { downloadFatiha(resId) },
                        onDelete = { deleteTranslationPack(resId) },
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    SectionHeader(
                        pal = pal,
                        title = "OFFLINE RECITATION • RECITER ${AudioEngine.reciterId}"
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "$cacheFiles verses • ${formatBytes(cacheBytes)} cached for the current reciter",
                        fontFamily = fontBody,
                        fontSize = 12.sp,
                        color = pal.inkMuted,
                    )
                }
                item {
                    val downloading = downloadJob?.isActive == true
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Surface(
                            onClick = { downloadJuzAmma() },
                            shape = RoundedCornerShape(12.dp),
                            color = pal.teal,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(
                                text = if (downloading) "Downloading…" else "Download Juz Amma (78–114)",
                                fontFamily = fontUi,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = androidx.compose.ui.graphics.Color.White,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            )
                        }
                        Surface(
                            onClick = {
                                if (downloading) downloadJob?.cancel() else clearAudioCache()
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = androidx.compose.ui.graphics.Color.Transparent,
                            border = BorderStroke(1.5.dp, pal.boneDark),
                        ) {
                            Text(
                                text = if (downloading) "Cancel" else "Clear cache",
                                fontFamily = fontUi,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = pal.inkMid,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            )
                        }
                    }
                    if (audioStatus.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = audioStatus,
                            fontFamily = fontBody,
                            fontSize = 12.sp,
                            color = if (audioStatus.startsWith("Failed")) pal.gold else pal.inkMuted,
                        )
                    }
                }

                item {
                    StorageFooterCard(
                        pal = pal,
                        fontUi = fontUi,
                        fontBody = fontBody,
                        totalBytes = totalBytes,
                    )
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}

// ── Top bar ────────────────────────────────────────────────────────────────
@Composable
private fun DownloadsTopBar(pal: NurPalette, fontUi: FontFamily, onBack: () -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth().height(56.dp), color = pal.cream) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = pal.ink,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Text(
                    text = "Offline library",
                    fontFamily = fontUi,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = pal.ink,
                    modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.size(40.dp))
            }
            HorizontalDivider(color = pal.boneDark.copy(alpha = 0.5f), thickness = 1.dp)
        }
    }
}

// ── Storage summary (mirrors Android STORAGE card) ─────────────────────────
@Composable
private fun StorageSummaryCard(
    pal: NurPalette,
    fontUi: FontFamily,
    fontBody: FontFamily,
    totalBytes: Long,
    packCount: Int,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = pal.cream),
        border = BorderStroke(1.dp, pal.boneDark),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "STORAGE",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = pal.inkMuted,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.5.sp,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = formatBytes(totalBytes),
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = pal.ink,
                fontFamily = fontUi,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Bundled • $packCount packs offline • no downloads needed",
                fontSize = 12.sp,
                color = pal.inkMid,
                fontFamily = fontBody,
            )
        }
    }
}

// ── Pack rows (mirror Android flat rows: icon box, name, detail, chip) ─────
@Composable
private fun BundledPackRow(
    pal: NurPalette,
    fontUi: FontFamily,
    fontBody: FontFamily,
    title: String,
    detail: String,
    sizeBytes: Long,
    icon: ImageVector,
    verdict: Boolean? = null,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = pal.cream),
        border = BorderStroke(1.dp, pal.boneDark),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(pal.goldLight),
                contentAlignment = Alignment.Center,
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = pal.gold, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontFamily = fontUi,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = pal.ink,
                )
                Text(
                    text = detail,
                    fontFamily = fontBody,
                    fontSize = 12.sp,
                    color = pal.inkMuted,
                    maxLines = 2,
                )
                if (verdict != null) {
                    Text(
                        text = if (verdict) "✓ Verified" else "✗ Corrupt — reinstall",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (verdict) pal.green else VerifyRed,
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            StatusChip(
                text = "Bundled • ${formatBytes(sizeBytes)}",
                container = pal.green.copy(alpha = 0.14f),
                content = pal.green,
                icon = Icons.Filled.CheckCircle,
            )
        }
    }
}

// ── Verify-packs integrity row (bundled section only; never downloads) ──────
@Composable
private fun VerifyPacksRow(
    pal: NurPalette,
    fontUi: FontFamily,
    fontBody: FontFamily,
    verifying: Boolean,
    healthy: Int,
    total: Int,
    checked: Boolean,
    onVerify: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Surface(
            onClick = onVerify,
            shape = RoundedCornerShape(12.dp),
            color = if (verifying) pal.bone else pal.teal,
        ) {
            Text(
                text = if (verifying) "Verifying…" else "Verify packs",
                fontFamily = fontUi,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (verifying) pal.inkMuted else androidx.compose.ui.graphics.Color.White,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            )
        }
        if (verifying) {
            Text(
                text = "Checking bundled resources…",
                fontFamily = fontBody,
                fontSize = 12.sp,
                color = pal.inkMuted,
            )
        } else if (checked) {
            Text(
                text = "$healthy/$total packs healthy",
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (healthy == total) pal.green else VerifyRed,
            )
        }
    }
}

@Composable
private fun AudioReciterRow(
    pal: NurPalette,
    fontUi: FontFamily,
    fontBody: FontFamily,
    reciter: AudioReciter,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = pal.cream),
        border = BorderStroke(1.dp, pal.boneDark),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(pal.bone),
                contentAlignment = Alignment.Center,
            ) {
                Icon(imageVector = Icons.Filled.Mic, contentDescription = null, tint = pal.inkMuted, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = reciter.name,
                    fontFamily = fontUi,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = pal.ink,
                )
                Text(
                    text = reciter.style,
                    fontFamily = fontBody,
                    fontSize = 12.sp,
                    color = pal.inkMuted,
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            StatusChip(
                text = "Streaming not supported yet",
                container = pal.bone,
                content = pal.inkMuted,
                icon = Icons.Filled.CloudOff,
            )
        }
    }
}

@Composable
private fun StatusChip(
    text: String,
    container: androidx.compose.ui.graphics.Color,
    content: androidx.compose.ui.graphics.Color,
    icon: ImageVector,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(container)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = content, modifier = Modifier.size(12.dp))
        Text(
            text = text,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = content,
        )
    }
}

// ── Translation pack rows (append-only; bundled rows above untouched) ─────────
@Composable
private fun TranslationPackRow(
    pal: NurPalette,
    fontUi: FontFamily,
    fontBody: FontFamily,
    name: String,
    resId: Int,
    status: String,
    cached: Boolean,
    busy: Boolean,
    actionsEnabled: Boolean,
    onDownload: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = pal.cream),
        border = BorderStroke(1.dp, pal.boneDark),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(pal.goldLight),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(imageVector = Icons.Filled.MenuBook, contentDescription = null, tint = pal.gold, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = name,
                        fontFamily = fontUi,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = pal.ink,
                    )
                    Text(
                        text = "Resource #$resId",
                        fontFamily = fontBody,
                        fontSize = 12.sp,
                        color = pal.inkMuted,
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                StatusChip(
                    text = if (resId == TranslationStore.BUNDLED_ID) "Bundled" else if (cached) "Cached" else "Online",
                    container = if (resId == TranslationStore.BUNDLED_ID || cached) {
                        pal.green.copy(alpha = 0.14f)
                    } else {
                        pal.bone
                    },
                    content = if (resId == TranslationStore.BUNDLED_ID || cached) pal.green else pal.inkMuted,
                    icon = if (resId == TranslationStore.BUNDLED_ID || cached) {
                        Icons.Filled.CheckCircle
                    } else {
                        Icons.Filled.CloudOff
                    },
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = status,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = pal.inkMid,
                    modifier = Modifier.weight(1f),
                )
                if (resId != TranslationStore.BUNDLED_ID) {
                    TextButton(onClick = onDownload, enabled = actionsEnabled && !busy) {
                        Text(
                            text = if (busy) "Working…" else "Download Fatiha",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                if (cached) {
                    TextButton(onClick = onDelete, enabled = actionsEnabled && !busy) {
                        Text(
                            text = "Delete",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

// ── Section + footer ───────────────────────────────────────────────────────
@Composable
private fun SectionHeader(pal: NurPalette, title: String) {
    Text(
        text = title,
        fontFamily = FontFamily.Monospace,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.4.sp,
        color = pal.inkMuted,
        modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 2.dp),
    )
}

@Composable
private fun StorageFooterCard(
    pal: NurPalette,
    fontUi: FontFamily,
    fontBody: FontFamily,
    totalBytes: Long,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = pal.tealSoft),
        border = BorderStroke(1.dp, pal.boneDark),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.Storage,
                contentDescription = null,
                tint = pal.teal,
                modifier = Modifier.size(22.dp),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Offline data • ${formatBytes(totalBytes)} total",
                    fontFamily = fontUi,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = pal.ink,
                )
                Text(
                    text = "All content works without internet",
                    fontFamily = fontBody,
                    fontSize = 12.sp,
                    color = pal.inkMuted,
                )
            }
        }
    }
}

// ── Helpers ────────────────────────────────────────────────────────────────
private fun resourceBytes(path: String): Long {
    return try {
        DownloadsScreenDesktopAnchor::class.java.getResourceAsStream(path)?.readBytes()?.size?.toLong() ?: 0L
    } catch (_: Exception) {
        0L
    }
}

private object DownloadsScreenDesktopAnchor

// ── Bundled-pack integrity checks (read-only; reuse the size helpers above) ──
private val VerifyRed = Color(0xFFDC2626)

private val verifyGson = Gson()

private data class VerifyVerseKey(
    @SerializedName("verse_key") val verseKey: String = "",
)

private data class VerifyChapter(val id: Int = 0)

private data class VerifyTafsirRow(
    @SerializedName("verse_key") val verseKey: String = "",
    val text: String = "",
)

private fun readResourceText(path: String): String? {
    return try {
        DownloadsScreenDesktopAnchor::class.java.getResourceAsStream(path)
            ?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
    } catch (_: Exception) {
        null
    }
}

private fun verifyBundledPack(pack: BundledPack): Boolean {
    return try {
        when {
            pack.resourcePaths == listOf("data/quran_full.json") -> verifyVerseCount()
            pack.resourcePaths == listOf("data/chapters.json") -> verifyChapterCount()
            pack.resourcePaths == listOf("data/tafsir_ibn_kathir.json") -> verifyTafsirNonEmpty()
            else -> pack.resourcePaths.isNotEmpty() &&
                pack.resourcePaths.all { resourceBytes("/$it") > 0L }
        }
    } catch (_: Exception) {
        false
    }
}

private fun verifyVerseCount(): Boolean {
    val text = readResourceText("/data/quran_full.json") ?: return false
    return try {
        val rows: List<VerifyVerseKey> =
            verifyGson.fromJson(text, object : TypeToken<List<VerifyVerseKey>>() {}.type)
        rows.size == 6236
    } catch (_: Exception) {
        text.split("verse_key").size - 1 == 6236
    }
}

private fun verifyChapterCount(): Boolean {
    val text = readResourceText("/data/chapters.json") ?: return false
    return try {
        val rows: List<VerifyChapter> =
            verifyGson.fromJson(text, object : TypeToken<List<VerifyChapter>>() {}.type)
        rows.size == 114
    } catch (_: Exception) {
        text.split("verses_count").size - 1 == 114
    }
}

private fun verifyTafsirNonEmpty(): Boolean {
    val text = readResourceText("/data/tafsir_ibn_kathir.json") ?: return false
    return try {
        val rows: List<VerifyTafsirRow> =
            verifyGson.fromJson(text, object : TypeToken<List<VerifyTafsirRow>>() {}.type)
        rows.isNotEmpty()
    } catch (_: Exception) {
        text.contains("verse_key")
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0L) return "0 MB"
    val mb = bytes.toDouble() / (1024 * 1024)
    return if (mb < 1) {
        String.format(Locale.US, "%d KB", bytes / 1024)
    } else {
        String.format(Locale.US, "%.1f MB", mb)
    }
}

/** Reciter names mirroring Android `Reciters.ALL` (style kept for the subtitle). */
private val desktopAudioReciters: List<AudioReciter> = listOf(
    AudioReciter("Mishari Rashid al-Afasy", "Murattal"),
    AudioReciter("AbdulBaset AbdulSamad", "Murattal"),
    AudioReciter("Abdur-Rahman as-Sudais", "Murattal"),
    AudioReciter("Abu Bakr al-Shatri", "Murattal"),
    AudioReciter("Hani ar-Rifai", "Murattal"),
    AudioReciter("Mahmoud Khalil Al-Husary", "Murattal"),
    AudioReciter("Mohamed Siddiq Al-Minshawi", "Murattal"),
    AudioReciter("Mishari Rashid al-Afasy (Mujawwad)", "Mujawwad"),
    AudioReciter("Mahmoud Khalil Al-Husary (Muallim)", "Muallim"),
    AudioReciter("Maher Al-Muaiqly (Gapless)", "Murattal"),
    AudioReciter("Idris Abkar", "Murattal"),
    AudioReciter("Mahmoud Khalil Al-Husary (Gapless)", "Murattal"),
    AudioReciter("Maher Al-Muaiqly (KFGQPC)", "Murattal"),
    AudioReciter("Mohamed Siddiq al-Minshawi (Mujawwad)", "Mujawwad"),
    AudioReciter("Saud ash-Shuraym", "Murattal"),
    AudioReciter("Mohamed al-Tablawi", "Murattal"),
    AudioReciter("Maher Al Muaiqly", "Murattal"),
    AudioReciter("Maher Al Muaiqly (Haramain)", "Murattal"),
    AudioReciter("Yasser Ad-Dussary", "Murattal"),
)
