# Quran Nur — Desktop (Kotlin)

Compose Multiplatform desktop reader — Kotlin only, no Android plugin.
Offline-first: verses, translations and Arabic fonts are bundled.

## Run it

```bash
# from this folder (uses the system Gradle; JDK 17 via JAVA_HOME)
gradle :desktop:run
```

Or launch the prebuilt bundle directly (no Gradle needed):

```bash
./desktop/build/compose/binaries/main/app/desktop
```

Rebuild the bundle after changes:

```bash
gradle :desktop:createDistributable
```

## What works (reader-first milestone)

- Homepage (mirrors the Android `HomeScreen` + web `Home.jsx`):
  top navbar, greeting hero, Continue-reading card, Verse of the Day,
  reading-stats strip, full-text Quran search, Browse the Quran with
  Surah / Page / Juz / Hizb modes and a responsive 1/2/3-column grid
- Surah reader (mirrors Android `SurahScreen`): top app bar with
  back / title + Surah-Ayah-Page navigator / reading-mode toggle /
  theme toggle / settings; surah header with chapter audio play pill;
  Bismillah; flat verse list with gold verse dividers, Page pills,
  Juz/Hizb pills, per-verse actions (persisted bookmark, share dialog
  with PNG export, expandable Ibn Kathir tafsir, per-verse audio play),
  word-level tajweed coloring (shape-safe), sajdah badges,
  scroll-to-verse with gold highlight, reading mode (continuous
  justified pages), prev/next surah buttons, mini player with
  auto-advance, and timed reading-session logging.
  Settings add Arabic/translation size, line spacing, translation toggle,
  18-edition translation picker, reciter picker
- Page reader: mushaf pages 1–604 with previous/next navigation,
  Verses / page-accurate 15-line Mushaf view (word packs cached offline)
- Recently-read strip + FSRS due-for-review nudge on the homepage,
  full reading-history page (day-grouped, clear-all) in the sidebar
- Audio setup sheet: reciter picker, repeat off/ayah/chapter,
  sleep timer (15/30/60) — all persisted; mini-player gear opens it
- Offline recitation manager in Downloads: one-tap Juz Amma (78–114)
  bulk download into the audio cache, live progress, cancel, cache size + clear
- Prayer slots in the Planner (Aladhan timings, city/country persisted),
  My-plans management (activate/delete) and per-plan analytics
- Keyboard navigation in the surah reader: ←/→ switch surah,
  Home/End jump, Esc back
- Memorize: goal card, memorized/due/strong breakdown, per-surah
  progress, FSRS-powered hifdh reader (blur + reveal + Again/Hard/Good/Easy)
- Planner: Today/Progress/Journal tabs, plan templates + custom plans,
  pace ring, journal, day reader with timer and completion tracking
- Analytics: streak, today/total cards, weekly goal ring, 7-day heatmap,
  activity mix, achievements
- Profile: reading totals, weekly goal stepper, quick links, preferences,
  local backup export/import, danger zone
- Library: persisted bookmarks + named verse collections (CRUD),
  add-to-collection dialog on every verse row
- Memorize: chapter search, goal/breakdown/test modals wired
  into the hub and the hifdh reader
- Analytics: streak, today/total cards, weekly goal ring, 7-day heatmap,
  cumulative weekly flow chart, activity mix, achievements
- Downloads: honest offline-pack inventory (text, metadata, tafsir,
  fonts) + reciter list (audio streaming: future update)
- Welcome first-run page, light/dark themes, left sidebar navigation
- Rendering is shape-safe: Arabic uses a single text style per verse
  (the desktop equivalent of Android's `TextView + ForegroundColorSpan`
  guarantee — no span-split join breakage)

## Verify without a display

```bash
gradle :desktop:jvmTest
# screenshots land in desktop/build/screenshots/ (home.png, surah1.png)
```

## Modules

- `shared/` — pure-JVM Kotlin logic (TajweedProcessor, Mushaf, verse-text
  helpers, translations, HTML stripper). Sources stay under
  `src/commonMain/kotlin` so the module can move back to
  kotlin-multiplatform later.
- `desktop/` — Compose for Desktop app (`mainClass=com.nur.quran.desktop.MainKt`),
  resources under `src/main/resources` (fonts + `data/*.json`).

## Next (not yet ported)

Cloud sync, Android Auto, onboarding tours. Everything else in the Android
app now has a desktop counterpart — see `VERIFY_PAGES.md` for the full
parity table.
