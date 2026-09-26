# VERIFY — desktop entry, navigation, prefs, packaging

Agent 10 scope: app entry + navigation + prefs + packaging notes.
Files owned by this agent: `Main.kt`, `ui/screens/App.kt`, `PrefsCache.kt`,
`ui/screens/SurahScreenDesktop.kt`, `VERIFY.md` (this file).

## Run

```bash
./gradlew :desktop:run
```

Runs `com.nur.quran.desktop.MainKt` — opens `Window(title="Quran Nur")` hosting `App()`.

## Package (jpackage via Compose Multiplatform)

```bash
./gradlew :desktop:packageDistributionForCurrentOS
```

Produces a native installer (`.deb` / `.dmg` / `.msi` depending on host OS) under
`desktop/build/compose/binaries/main/`. Requires a JDK with `jpackage`
(JDK 17+) and platform packaging deps (`fakeroot`/`dpkg` on Linux,
Xcode CLI tools on macOS, WiX on Windows). No Notion / signing config needed
for local verification builds.

## Screenshot parity checklist

Capture each case from `:desktop:run` at 1280×800 and compare against the
Android reference screenshots:

- [ ] Surah Al-Fatiha (1) — `App → Open Surah Al-Fatiha` matches mobile verse text + Bismillah header
- [ ] Ayat al-Kursi (2:255) — navigate `surah/2`, scroll to verse 255; wording + tajweed colours match
- [ ] Surah Al-Kahf (18) — `surah/18`; spot-check first/last verses
- [ ] Mushaf × tajweed matrix (use `PrefsCache` font/mushaf + tajweed toggle):
  - [ ] Uthmani × tajweed ON / OFF
  - [ ] IndoPak × tajweed ON / OFF
  - [ ] Uthman Taha / KFGQPC font × tajweed ON / OFF

Note: `SurahScreenDesktop` currently renders a `PlainVerseText` placeholder for
the first verse; `VerseWebView` rich-text rows are TODO (JCEF / Compose
rich-text fallback reusing shared `TajweedProcessor`). Parity screenshots for
tajweed colours apply once that TODO lands.
