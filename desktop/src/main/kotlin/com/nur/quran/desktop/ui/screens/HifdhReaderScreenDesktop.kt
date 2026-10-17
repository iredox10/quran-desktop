package com.nur.quran.desktop.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nur.quran.desktop.PrefsCache
import com.nur.quran.desktop.data.AudioEngine
import com.nur.quran.desktop.data.BookmarkStore
import com.nur.quran.desktop.data.QuranStore
import com.nur.quran.desktop.data.SessionStore
import com.nur.quran.desktop.ui.components.verseDisplayArabic
import com.nur.quran.desktop.ui.input.AppShortcuts
import com.nur.quran.desktop.ui.input.swipeToNavigate
import com.nur.quran.desktop.ui.theme.NurPalette
import com.nur.quran.desktop.ui.theme.rememberArabicFontFamily
import com.nur.quran.desktop.ui.theme.rememberBodyFontFamily
import com.nur.quran.desktop.ui.theme.rememberUiFontFamily
import com.nur.quran.desktop.ui.hifdh.HifdhBreakdownDialog
import com.nur.quran.desktop.ui.library.AddToCollectionDialog
import com.nur.quran.shared.FsrsRating
import com.nur.quran.shared.FsrsScheduler
import com.nur.quran.shared.HifdhHistoryEntry
import com.nur.quran.shared.HifdhStore
import com.nur.quran.shared.HtmlStripper

/**
 * Desktop hifdh (memorization) review flow mirroring Android `HifdhReaderScreen`:
 * one due verse at a time, hidden until tapped, rated Again/Hard/Good/Easy
 * through [FsrsScheduler], with history persisted after each rating.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HifdhReaderScreenDesktop(
    chapterId: Int,
    pal: NurPalette,
    onBack: () -> Unit = {},
    onOpenSurah: (Int, String?) -> Unit = { _, _ -> }
) {
    val chapter = remember(chapterId) { QuranStore.chapter(chapterId) }
    val verses = remember(chapterId) { QuranStore.versesOfChapter(chapterId) }
    val chapterName = chapter?.nameSimple ?: "Surah $chapterId"

    val fontUi = rememberUiFontFamily()
    val fontBody = rememberBodyFontFamily()
    val fontArabic = rememberArabicFontFamily(PrefsCache.getFont())
    val arabicScale = remember { PrefsCache.getArabicScale() }

    val scheduler = remember { FsrsScheduler() }
    var history by remember(chapterId) { mutableStateOf(HifdhStore.loadHifdhHistory()) }
    var links by remember(chapterId) { mutableStateOf(HifdhStore.loadTransitionLinks()) }

    val now = remember(chapterId) { System.currentTimeMillis() }
    // Queue = verses due for review (no history or card due), else the whole surah.
    val queue = remember(chapterId, history) {
        val hist = history
        val due = verses.filter { v ->
            val entry = hist[v.verseKey]
            val card = entry?.card
            card == null || card.isDue(now)
        }
        if (due.isNotEmpty()) due else verses
    }

    var index by remember(chapterId) { mutableStateOf(0) }
    var revealed by remember(chapterId) { mutableStateOf(false) }
    var testMode by remember(chapterId) { mutableStateOf(false) }
    var totalTestRevealed by remember(chapterId) { mutableStateOf(0) }
    var showBreakdown by remember(chapterId) { mutableStateOf(false) }
    var audioEnabled by remember(chapterId) { mutableStateOf(false) }
    val ratingCounts = remember(chapterId) { mutableStateMapOf<Int, Int>() }

    // Per-verse action icons (bookmark / mark-memorized): a tick forces a recompute
    // of the persisted bookmark set so icons refresh right after a toggle.
    var bookmarkTick by remember(chapterId) { mutableStateOf(0) }
    val bookmarkedKeys = remember(bookmarkTick) { BookmarkStore.all() }

    // ── Bottom control bar state (web Memorization parity) ──
    // Batch step size: 1,3,5,10 verses, or -1 meaning "Page" (all verses that
    // share the current verse's pageNumber).
    var batch by remember(chapterId) { mutableStateOf(1) }
    var surahMemTick by remember(chapterId) { mutableStateOf(0) }
    var showCollections by remember { mutableStateOf(false) }
    var showAudioSettings by remember { mutableStateOf(false) }

    // Count consecutive verses from `index` that share the current page (min 1).
    fun pageStep(): Int {
        val page = queue.getOrNull(index)?.pageNumber ?: return 1
        var n = 1
        while (index + n < queue.size && queue[index + n].pageNumber == page) n++
        return n
    }
    fun step(): Int = if (batch == -1) pageStep() else batch

    // ── Session timing: log "memorizing" minutes exactly once ──────────────
    val sessionStart = remember(chapterId) { System.currentTimeMillis() }
    var sessionLogged by remember(chapterId) { mutableStateOf(false) }
    fun logSessionOnce() {
        if (sessionLogged) return
        sessionLogged = true
        val elapsedMinutes = ((System.currentTimeMillis() - sessionStart) / 60000L).toInt()
        SessionStore.log("memorizing", chapterId, elapsedMinutes)
    }
    DisposableEffect(chapterId) {
        onDispose { logSessionOnce() }
    }
    // ── Audio-assisted review: play current verse while toggle is on ──────
    // No auto-advance on track end; user still rates manually.
    LaunchedEffect(index, audioEnabled) {
        if (!audioEnabled) return@LaunchedEffect
        val currentVerse = queue.getOrNull(index) ?: run {
            AudioEngine.stop()
            return@LaunchedEffect
        }
        AudioEngine.playVerse(currentVerse.verseKey)
    }
    DisposableEffect(chapterId) {
        onDispose { AudioEngine.stop() }
    }
    fun goBack() {
        logSessionOnce()
        onBack()
    }

    fun rate(rating: Int, testRevealed: Int = 0) {
        val verse = queue.getOrNull(index) ?: return
        if (testMode) totalTestRevealed += testRevealed
        val nowMs = System.currentTimeMillis()
        val prevCard = history[verse.verseKey]?.card
        val card = if (prevCard != null) {
            scheduler.next(prevCard, nowMs, rating)
        } else {
            scheduler.next(scheduler.createEmptyCard(nowMs), nowMs, rating)
        }
        // Matches Android SurahViewModel.logHifdhReview strength mapping.
        val strength = when (rating) {
            FsrsRating.AGAIN -> "weak"
            FsrsRating.EASY -> "strong"
            else -> "medium"
        }
        val updated = history.toMutableMap()
        updated[verse.verseKey] = HifdhHistoryEntry(card = card, lastReviewed = nowMs, strength = strength)
        history = updated
        HifdhStore.saveHifdhHistory(updated)
        // Android: Again flags the transition link; Good/Easy clears it (Hard leaves it).
        val updatedLinks = links.toMutableSet()
        when (rating) {
            FsrsRating.AGAIN -> updatedLinks.add(verse.verseKey)
            FsrsRating.GOOD, FsrsRating.EASY -> updatedLinks.remove(verse.verseKey)
        }
        links = updatedLinks
        HifdhStore.saveTransitionLinks(updatedLinks)

        ratingCounts[rating] = (ratingCounts[rating] ?: 0) + 1
        revealed = false
        index++
        if (index >= queue.size) {
            AudioEngine.stop()
            logSessionOnce()
        }
    }

    // ── Keyboard navigation (web Memorization parity) ──
    // Focus the root so arrow keys drive prev/next. Disabled while any
    // dialog/popover is open (they take over focus).
    val focusRequester = remember(chapterId) { FocusRequester() }
    LaunchedEffect(chapterId) { focusRequester.requestFocus() }

    if (showBreakdown) {
        val memCount = history.values.count { it.card != null }
        HifdhBreakdownDialog(
            pal = pal,
            chapterId = chapterId,
            memorized = memCount,
            total = verses.size,
            due = queue.size,
            onDismiss = { showBreakdown = false },
            onReview = { showBreakdown = false }
        )
    }

    if (showCollections) {
        val cur = queue.getOrNull(index)
        if (cur != null) AddToCollectionDialog(
            pal = pal,
            verseKey = cur.verseKey,
            onDismiss = { showCollections = false }
        )
    }

    fun hifdhNext(): Boolean {
        val s = step()
        if (index + s < queue.size) index += s
        return true
    }
    fun hifdhPrev(): Boolean {
        val s = step()
        if (index > 0) index = (index - s).coerceAtLeast(0)
        return true
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(pal.cream)
            .focusRequester(focusRequester)
            .focusable()
            .swipeToNavigate(onNext = ::hifdhNext, onPrevious = ::hifdhPrev)
            .onKeyEvent { ev ->
                if (ev.type != KeyEventType.KeyDown) return@onKeyEvent false
                if (showBreakdown || showCollections || showAudioSettings) return@onKeyEvent false
                // RTL layout: ArrowLeft advances (next), ArrowRight goes back.
                AppShortcuts.pageTurn(ev, onNext = ::hifdhNext, onPrevious = ::hifdhPrev) ||
                    when (ev.key) {
                        Key.Escape -> {
                            goBack()
                            true
                        }
                        else -> false
                    }
            }
    ) {
        // ── Top bar ──
        Surface(modifier = Modifier.fillMaxWidth(), color = pal.white, shadowElevation = 1.dp) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = ::goBack, modifier = Modifier.size(48.dp)) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = pal.ink
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Hifdh Mode",
                        fontFamily = fontUi,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = pal.ink
                    )
                    Text(
                        text = chapterName,
                        fontFamily = fontBody,
                        fontSize = 12.sp,
                        color = pal.inkMuted
                    )
                }
                IconButton(onClick = { showBreakdown = true }, modifier = Modifier.size(48.dp)) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = "Surah breakdown",
                        tint = pal.inkMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(
                    onClick = {
                        if (audioEnabled) AudioEngine.stop()
                        audioEnabled = !audioEnabled
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (audioEnabled) Icons.Filled.Stop else Icons.Filled.VolumeUp,
                        contentDescription = if (audioEnabled) "Stop verse audio" else "Play verse audio",
                        tint = if (audioEnabled) pal.gold else pal.inkMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
                if (queue.isNotEmpty() && index < queue.size) {
                    Surface(
                        shape = RoundedCornerShape(100),
                        color = pal.goldSoft,
                        modifier = Modifier.clickable { testMode = !testMode }
                    ) {
                        Text(
                            text = if (testMode) "Test: On" else "Test: Off",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = pal.gold,
                            fontFamily = fontUi,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${index + 1} / ${queue.size}",
                        fontFamily = fontUi,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = pal.teal
                    )
                }
            }
        }
        // ── Thin progress bar ──
        val fraction = if (queue.isNotEmpty()) (index.coerceAtMost(queue.size).toFloat() / queue.size) else 0f
        Box(modifier = Modifier.fillMaxWidth().height(3.dp).background(pal.boneDark)) {
            Box(modifier = Modifier.fillMaxWidth(fraction).height(3.dp).background(pal.teal))
        }

        if (verses.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No verses found for this surah.", fontFamily = fontBody, fontSize = 14.sp, color = pal.inkMuted)
            }
            return@Column
        }

        val verse = queue.getOrNull(index)
        if (verse == null) {
            // ── Completion card ──
            Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Surface(
                    modifier = Modifier.fillMaxWidth(0.7f),
                    shape = RoundedCornerShape(16.dp),
                    color = pal.white,
                    shadowElevation = 2.dp
                ) {
                    Column(
                        modifier = Modifier.padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Session Complete",
                            fontFamily = fontUi,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = pal.ink,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "You reviewed ${queue.size} verse${if (queue.size == 1) "" else "s"} from $chapterName.",
                            fontFamily = fontBody,
                            fontSize = 14.sp,
                            color = pal.inkMuted,
                            textAlign = TextAlign.Center
                        )
                        if (testMode || totalTestRevealed > 0) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Test words revealed: $totalTestRevealed",
                                fontFamily = fontBody,
                                fontSize = 13.sp,
                                color = pal.inkMuted,
                                textAlign = TextAlign.Center
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            RatingStat(pal, fontUi, "Again", ratingCounts[FsrsRating.AGAIN] ?: 0, Color(0xFFEF4444))
                            RatingStat(pal, fontUi, "Hard", ratingCounts[FsrsRating.HARD] ?: 0, Color(0xFFF59E0B))
                            RatingStat(pal, fontUi, "Good", ratingCounts[FsrsRating.GOOD] ?: 0, pal.teal)
                            RatingStat(pal, fontUi, "Easy", ratingCounts[FsrsRating.EASY] ?: 0, pal.gold)
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedButton(onClick = ::goBack) { Text("Back") }
                            Button(
                                onClick = { onOpenSurah(chapterId, null) },
                                colors = ButtonDefaults.buttonColors(containerColor = pal.teal)
                            ) { Text("Read in Surah") }
                        }
                    }
                }
            }
            return@Column
        }

        // ── Review card ──
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(0.5f))
            Surface(shape = RoundedCornerShape(100), color = pal.goldSoft) {
                Text(
                    text = verse.verseKey,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = pal.gold,
                    fontFamily = fontUi,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            // ── Per-verse actions: bookmark + mark-memorized (mirrors web reader) ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val isBookmarked = verse.verseKey in bookmarkedKeys
                val isMemorized = history.containsKey(verse.verseKey)
                IconButton(onClick = { BookmarkStore.toggle(verse.verseKey); bookmarkTick++ }) {
                    Icon(
                        imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                        contentDescription = "Bookmark",
                        tint = if (isBookmarked) pal.teal else pal.inkMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(onClick = {
                    val nowMs = System.currentTimeMillis()
                    val updated = history.toMutableMap()
                    if (isMemorized) updated.remove(verse.verseKey)
                    else updated[verse.verseKey] = HifdhHistoryEntry(
                        card = scheduler.next(scheduler.createEmptyCard(nowMs), nowMs, FsrsRating.GOOD),
                        lastReviewed = nowMs, strength = "strong")
                    history = updated
                    HifdhStore.saveHifdhHistory(updated)
                    bookmarkTick++
                }) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = "Mark memorized",
                        tint = if (isMemorized) pal.green else pal.inkMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            // ── Test mode state (resets per verse index) ──
            // Display text routes through the shared mushaf pipeline (floating
            // marks stripped, single end marker) — same as the reader.
            val displayArabic = remember(verse.verseKey, fontArabic) {
                verseDisplayArabic(verse, PrefsCache.getFont())
            }
            val testWords = remember(verse.verseKey, displayArabic) { displayArabic.split(" ") }
            val testMasked: Set<Int> = remember(verse.verseKey, verse.arabic) {
                testWords.indices.filter { i ->
                    !isTestEndMarkerWord(testWords[i]) && isTestMasked(verse.verseKey, i)
                }.toSet()
            }
            var testRevealedWords by remember(index, verse.verseKey) { mutableStateOf(mutableSetOf<Int>()) }
            val testRevealedCount = testRevealedWords.count { it in testMasked }
            val testGuessed = (testMasked.size - testRevealedCount).coerceAtLeast(0)
            if (testMode) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(pal.white)
                        .padding(28.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                testWords.forEachIndexed { wi, w ->
                                    if (wi in testMasked && wi !in testRevealedWords) {
                                        Text(
                                            text = "█".repeat(w.length.coerceAtLeast(1)),
                                            fontFamily = fontArabic,
                                            fontSize = (28 * arabicScale).sp,
                                            lineHeight = (48 * arabicScale).sp,
                                            color = pal.ink,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier
                                                .clickable {
                                                    testRevealedWords =
                                                        (testRevealedWords + wi).toMutableSet()
                                                }
                                                .padding(horizontal = 4.dp)
                                        )
                                    } else {
                                        Text(
                                            text = w,
                                            fontFamily = fontArabic,
                                            fontSize = (28 * arabicScale).sp,
                                            lineHeight = (48 * arabicScale).sp,
                                            color = pal.ink,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(horizontal = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                        if (testRevealedCount < testMasked.size) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Reveal all",
                                fontFamily = fontUi,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = pal.teal,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .clickable { testRevealedWords = testMasked.toMutableSet() }
                                    .padding(8.dp)
                            )
                        }
                        if (verse.translation.isNotBlank()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = verse.translation,
                                fontFamily = fontBody,
                                fontSize = 14.sp,
                                color = pal.inkMuted,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(pal.white)
                    .clickable { revealed = !revealed }
                    .padding(28.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = displayArabic,
                        fontFamily = fontArabic,
                        fontSize = (28 * arabicScale).sp,
                        lineHeight = (48 * arabicScale).sp,
                        color = pal.ink,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().alpha(if (revealed) 1f else 0.05f)
                    )
                    if (verse.translation.isNotBlank()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = verse.translation,
                            fontFamily = fontBody,
                            fontSize = 14.sp,
                            color = pal.inkMuted,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().alpha(if (revealed) 1f else 0.05f)
                        )
                    }
                    if (!revealed) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Tap to reveal",
                            fontFamily = fontUi,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = pal.teal,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            }
            // ── Translation card (bordered cream) — shown when reader translation pref on ──
            if (PrefsCache.getReaderTranslationEnabled() && verse.translation.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = pal.cream),
                    border = BorderStroke(1.dp, pal.boneDark),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = HtmlStripper.strip(verse.translation),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        fontSize = (13 * PrefsCache.getTranslationScale()).sp,
                        lineHeight = (20 * PrefsCache.getTranslationScale()).sp,
                        color = pal.inkMid,
                        fontFamily = fontBody
                    )
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            if (testMode) {
                Text(
                    text = "Guessed $testGuessed/${testMasked.size} — tap words you couldn't recall",
                    fontFamily = fontBody,
                    fontSize = 13.sp,
                    color = pal.inkMuted,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
            }
            Text(
                text = "How well did you recall it?",
                fontFamily = fontBody,
                fontSize = 13.sp,
                color = pal.inkMuted
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RatingButton(
                    label = "Again",
                    color = Color(0xFFEF4444),
                    modifier = Modifier.weight(1f),
                    fontUi = fontUi,
                    onClick = { rate(FsrsRating.AGAIN, if (testMode) testRevealedCount else 0) }
                )
                RatingButton(
                    label = "Hard",
                    color = Color(0xFFF59E0B),
                    modifier = Modifier.weight(1f),
                    fontUi = fontUi,
                    onClick = { rate(FsrsRating.HARD, if (testMode) testRevealedCount else 0) }
                )
                RatingButton(
                    label = "Good",
                    color = pal.teal,
                    modifier = Modifier.weight(1f),
                    fontUi = fontUi,
                    onClick = { rate(FsrsRating.GOOD, if (testMode) testRevealedCount else 0) }
                )
                RatingButton(
                    label = "Easy",
                    color = pal.gold,
                    modifier = Modifier.weight(1f),
                    fontUi = fontUi,
                    onClick = { rate(FsrsRating.EASY, if (testMode) testRevealedCount else 0) }
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            // ── Bottom control bar (web Memorization parity) ──
            val curVerse = queue.getOrNull(index)
            if (curVerse != null) {
                // Info row: Surah name + mark-surah-memorized award + page number.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Surah ${chapter?.nameSimple ?: "Surah $chapterId"}",
                        fontFamily = fontUi,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = pal.teal
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    val surahOn = remember(surahMemTick) { HifdhStore.loadMemorizedSurahs().contains(chapterId) }
                    IconButton(
                        onClick = { HifdhStore.toggleMemorizedSurah(chapterId); surahMemTick++ },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MilitaryTech,
                            contentDescription = "Mark surah memorized",
                            tint = if (surahOn) pal.green else pal.inkMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Page ${curVerse.pageNumber}",
                        fontFamily = fontBody,
                        fontSize = 11.sp,
                        color = pal.inkMuted
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                // Controls row: play/pause, batch cycle, add-to-collection,
                // audio-settings popover, prev/next.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val isPlayingThis = AudioEngine.current?.verseKey == curVerse.verseKey && AudioEngine.playing
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(if (isPlayingThis) pal.gold else pal.teal)
                            .clickable {
                                if (isPlayingThis) AudioEngine.stop()
                                else AudioEngine.playVerse(curVerse.verseKey)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlayingThis) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (isPlayingThis) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    // Batch size cycle: 1 → 3 → 5 → 10 → Page → 1.
                    val batchLabels = listOf("1", "3", "5", "10", "Page")
                    val batchLabel = if (batch == -1) "Page"
                    else batchLabels[batchLabels.indexOfFirst { it == batch.toString() }.takeIf { it >= 0 } ?: 0]
                    Button(
                        onClick = { batch = when (batch) { 1 -> 3; 3 -> 5; 5 -> 10; 10 -> -1; else -> 1 } },
                        colors = ButtonDefaults.buttonColors(containerColor = pal.cream, contentColor = pal.inkMid),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(42.dp)
                    ) {
                        Icon(Icons.Filled.Layers, null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(batchLabel, fontFamily = fontUi, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = { showCollections = true }, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Filled.PlaylistAdd, "Add to collection", tint = pal.inkMid, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    // Audio settings popover: repeat mode + sleep timer.
                    Box {
                        IconButton(
                            onClick = { showAudioSettings = !showAudioSettings },
                            modifier = Modifier.size(42.dp)
                        ) {
                            Icon(Icons.Filled.Tune, "Audio settings", tint = pal.inkMid, modifier = Modifier.size(18.dp))
                        }
                        if (showAudioSettings) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = pal.white,
                                border = BorderStroke(1.dp, pal.boneDark),
                                shadowElevation = 8.dp,
                                modifier = Modifier.padding(top = 44.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text("Repeat", fontFamily = fontUi, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = pal.ink)
                                    val repLabels = mapOf("off" to "Off", "ayah" to "Ayah", "chapter" to "Chapter")
                                    Button(
                                        onClick = {
                                            AudioEngine.repeatMode = when (AudioEngine.repeatMode) {
                                                "off" -> "ayah"; "ayah" -> "chapter"; else -> "off"
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = pal.cream, contentColor = pal.ink),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(repLabels[AudioEngine.repeatMode] ?: "Off", fontFamily = fontUi, fontSize = 12.sp)
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Sleep timer", fontFamily = fontUi, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = pal.ink)
                                    val sleepOpts = listOf(0, 15, 30, 60)
                                    Button(
                                        onClick = {
                                            val i = sleepOpts.indexOf(AudioEngine.sleepMinutes)
                                            AudioEngine.sleepMinutes = sleepOpts[(i + 1) % sleepOpts.size]
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = pal.cream, contentColor = pal.ink),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(if (AudioEngine.sleepMinutes == 0) "Off" else "${AudioEngine.sleepMinutes}m", fontFamily = fontUi, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(onClick = { if (index > 0) index-- }, enabled = index > 0, modifier = Modifier.size(42.dp)) {
                        Icon(Icons.Filled.ChevronLeft, "Previous", tint = pal.inkMid, modifier = Modifier.size(20.dp))
                    }
                    IconButton(
                        onClick = { val s = step(); if (index + s < queue.size) index += s },
                        enabled = index < queue.size - 1,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Icon(Icons.Filled.ChevronRight, "Next", tint = pal.inkMid, modifier = Modifier.size(20.dp))
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

/**
 * Deterministic ~35% word mask for Test mode: stable per verseKey + word index.
 */
private fun isTestMasked(verseKey: String, wordIndex: Int): Boolean {
    val h = ("$verseKey#$wordIndex").hashCode() and 0x7fffffff
    return h % 100 < 35
}

/**
 * End-marker words (ornate parenthesis U+06DD or bare verse-number digits)
 * are never masked.
 */
private fun isTestEndMarkerWord(word: String): Boolean {
    if (word.isEmpty()) return true
    if (word.contains('\u06DD')) return true
    var hasDigit = false
    for (c in word) {
        when {
            c.isDigit() || c in '٠'..'٩' || c in '۰'..'۹' -> hasDigit = true
            c == '﴾' || c == '﴿' || c == '(' || c == ')' -> {}
            else -> return false
        }
    }
    return hasDigit
}

@Composable
private fun RatingButton(
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    fontUi: androidx.compose.ui.text.font.FontFamily,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = Color.White),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(text = label, fontFamily = fontUi, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun RatingStat(
    pal: NurPalette,
    fontUi: androidx.compose.ui.text.font.FontFamily,
    label: String,
    count: Int,
    color: Color
) {
    Surface(shape = RoundedCornerShape(12.dp), color = pal.cream) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = count.toString(),
                fontFamily = fontUi,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(text = label, fontFamily = fontUi, fontSize = 12.sp, color = pal.inkMuted)
        }
    }
}
