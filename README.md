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
  top navbar with logo / theme toggle / Arabic-font settings,
  time-of-day greeting hero, Continue-reading card (persisted last read),
  Verse of the Day with copy button, and Browse the Quran with
  Surah / Page / Juz / Hizb mode pills, search, and a responsive
  1 / 2 / 3-column grid on narrow / medium / wide windows
- Surah reader (mirrors Android `SurahScreen`): top app bar with
  back / title + Surah-Ayah-Page navigator / reading-mode toggle /
  theme toggle / settings; surah header (35sp name + 40sp gold Arabic
  name, uppercase meta row, baseline rule); Bismillah; flat verse list
  with gold verse dividers, Page pills, Juz/Hizb pills, per-verse
  actions (persisted bookmark, copy-share, expandable Ibn Kathir
  tafsir), sajdah badges, scroll-to-verse with gold highlight, reading
  mode (continuous justified pages), and prev/next surah buttons.
  Settings add Arabic/translation size, line spacing, translation toggle
- Page reader: mushaf pages 1–604 with previous/next navigation
- Memorize: goal card, memorized/due/strong breakdown, per-surah
  progress, FSRS-powered hifdh reader (blur + reveal + Again/Hard/Good/Easy)
- Planner: Today/Progress/Journal tabs, plan templates + custom plans,
  pace ring, journal, day reader with timer and completion tracking
- Analytics: streak, today/total cards, weekly goal ring, 7-day heatmap,
  activity mix, achievements
- Profile: reading totals, weekly goal stepper, quick links, preferences,
  local backup export/import, danger zone
- Library: persisted bookmarks + named verse collections (CRUD)
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

Tajweed word coloring (needs a WebView/JCEF renderer — see
`ui/components/VerseWebView.kt`), audio playback, planner, memorize, sync.
