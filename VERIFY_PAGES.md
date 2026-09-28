# Page Parity Checklist — Android → Desktop

Source of truth for per-page port status. Desktop target: Compose Multiplatform (`desktop/`, `shared/`).

| Page | Android source | Desktop file | Status | Gaps |
|---|---|---|---|---|
| Home | `ui/screens/HomeScreen.kt` | `ui/screens/App.kt` + `ui/home/BrowseItems.kt` | Done | Verse-of-Day rotation, search polish |
| Surah | `ui/screens/SurahScreen.kt` | `ui/screens/SurahScreenDesktop.kt` | Done | Word-level tajweed (majority-rule, shape-safe), audio mini-player, share-as-image, reading timer |
| Page | `ui/screens/PageScreen.kt` + `ui/components/MushafPageView.kt` | `ui/screens/PageScreenDesktop.kt` + `ui/components/MushafPageViewDesktop.kt` | Done | Verses list + page-accurate 15-line Mushaf view (cached word packs) |
| Memorize | `ui/screens/MemorizeScreen.kt` + `data/hifdh/{HifdhStore,FsrsScheduler}.kt` | `ui/screens/MemorizeScreenDesktop.kt` + `shared/{HifdhStore,FsrsScheduler}.kt` | Done | FSRS intervals ported; review UI, test modals (`HifdhTestModal`, `HifdhGoalModal`, `HifdhBreakdownModal`) |
| HifdhReader | `ui/screens/HifdhReaderScreen.kt` | `ui/screens/HifdhReaderScreenDesktop.kt` | Done | Blank-out mode, audio-linked rows (`LinkedAudioRow`) |
| Planner | `ui/screens/PlannerScreen.kt` + `data/planner/PlannerEngine.kt` + `ui/components/planner/*` (19 files) | `ui/screens/PlannerScreenDesktop.kt` + `ui/planner/PlannerComponents.kt` + `shared/PlannerEngine.kt` + `data/PlannerStore.kt` | Done | PlanToday/Progress/Journal tabs, `CustomPlanForm`, `PaceRing`, templates |
| PlannerReader | `ui/screens/PlannerReaderScreen.kt` | `ui/screens/PlannerReaderScreenDesktop.kt` | Done | Assignment check-off sync with PlannerStore |
| Analytics | `ui/screens/AnalyticsScreen.kt` + `analytics/AnalyticsStats.kt` + `ui/components/analytics/*` (6 files) | `ui/screens/AnalyticsScreenDesktop.kt` + `ui/analytics/AnalyticsComponents.kt` + `shared/AnalyticsStats.kt` | Done | Heatmap, flow chart, achievements parity |
| Profile | `ui/screens/ProfileScreen.kt` + `ui/components/profile/*` (8 files) | `ui/screens/ProfileScreenDesktop.kt` | Done | Goal card, quick links, danger zone, local prefs instead of cloud |
| Library | `ui/screens/LibraryScreen.kt` + `ui/components/library/*` | (new `ui/screens/LibraryScreenDesktop.kt`) | Done | Bookmarks/collections sections; backed by `data/BookmarkStore.kt` |
| Downloads | `ui/screens/DownloadsScreen.kt` + `ui/components/audio/{PackDownloadRow,DownloadRow,AudioDurationEstimator}.kt` | (new `ui/screens/DownloadsScreenDesktop.kt`) | Done | Bundled packs + downloadable translation packs + audio cache status; no WorkManager queue |
| Welcome | onboarding flow | (new `ui/screens/WelcomeScreenDesktop.kt`) | Done | First-run prefs, font preload notice |
| SettingsDrawer | `ui/components/SettingsDrawer.kt` | (new `ui/components/SettingsDrawerDesktop.kt`) | Done | Arabic/translation size, line spacing, translation toggle |
| Sidebar | app nav drawer | `ui/screens/App.kt` (nav rail) | Done | Nav destinations for new screens |
| Reciters | `ui/components/audio/{ReciterLibraryPanel,ChosenReciterCard,TranslationPickerRow}.kt` | `shared/Reciters.kt` + `ui/audio/AudioCards.kt` | Done | Wired to JLayer engine via settings picker |
| Backup | `ProfileCloudSyncCard` + `SyncStatusCard` | (new `data/BackupStore.kt`) | Done | Export/import JSON to local file, no cloud |
| Audio playback | `ui/components/audio/*` (MiniPlayer, AudioSetupSheet, RepetitionCard, PlaybackRangeCard) + Media3 service | `data/AudioEngine.kt` (JLayer) + `ui/audio/MiniPlayerDesktop.kt` | Done | Per-ayah streaming + disk cache; no repeat/range cards, no background service |
| Appwrite sync | `ProfileCloudSyncCard`, `SyncStatusCard` | — | Dropped (no desktop backend) | No Appwrite JVM auth in scope; local JSON backup instead |
| Android Auto | automotive module | — | Dropped (no desktop backend) | No car head-unit target on desktop |
| Tours | `PageTourModal`, `Coachmark` | — | Dropped (no desktop backend) | No onboarding-tour framework; Welcome screen covers first run |
| Splash | `ui/components/SplashScreen.kt` | — | Dropped (no desktop backend) | Desktop launches straight into `Main.kt`; no splash API |

## Rendering safety

- Arabic verses render with a **single text style per verse** (`ui/components/PlainVerseText.kt`, `VerseRow.kt`) — the desktop equivalent of Android's `TextView + ForegroundColorSpan` guarantee, so no span-split join breakage.
- Tajweed word coloring is done WITHOUT a browser (no WebView dep resolvable; system WebKit is 4.1/6.0 only): `shared/TajweedProcessor.kt` emits segments → `ui/components/TajweedWordText.kt` colors each whole word by majority rule in a single SpanStyle, which cannot break Arabic joins. Known limitation vs Android TextView: intra-word multi-color granularity.

## How to verify

```bash
gradle :desktop:jvmTest
# screenshots land in desktop/build/screenshots/ (home.png, surah1.png)
```

- `desktop/src/jvmTest/.../ScreenshotTest.kt` renders Home + Surah 1 headless.
- Compare screenshots against Android for Surah header, Bismillah, verse dividers, Page/Juz pills.
