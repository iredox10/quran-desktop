# Bundled Fonts

Source: `quran-kotlin/app/src/main/assets/fonts/*.ttf` (copied verbatim).

Display names match the Android settings values resolved by
`getArabicFontFamily` in `SurahScreen.kt`.

| Display name      | Regular file              | Bold file                  |
|-------------------|---------------------------|----------------------------|
| KFGQPC Hafs       | `kfgqpc_hafs.ttf`         | — (single weight)          |
| Amiri Quran       | `amiri_regular.ttf`       | `amiri_bold.ttf`           |
| Noto Naskh Arabic | `noto_regular.ttf`        | `noto_bold.ttf`            |
| Scheherazade New  | `scheherazade_regular.ttf`| `scheherazade_bold.ttf`    |
| System Default    | — (no file, uses `FontFamily.Default`) | — |

> Historical note: `uthman_taha_naskh.ttf` (and the old `noto_*.ttf`) shipped
> as corrupt HTML 404 pages — same bytes as the Android repo's copies — and
> the only downloadable Uthman Taha (Ver10 woff2) lacks Uthmani mark coverage
> entirely. "Uthman Taha Naskh" was therefore dropped from
> `DesktopFonts.names`; saved prefs for it resolve to Scheherazade.
> `noto_*.ttf` were replaced with the real Noto Naskh Arabic variable font
> (OFL, google/fonts).

## Extra files (not in the Android font map)

These were copied for future desktop use but have no display-name mapping yet:

| File                              | Notes                        |
|-----------------------------------|------------------------------|
| `cormorant_garamond_regular.ttf`  | Latin UI font, regular       |
| `cormorant_garamond_bold.ttf`     | Latin UI font, bold          |
| `piazzolla_regular.ttf`           | Latin UI font, regular       |
| `piazzolla_bold.ttf`              | Latin UI font, bold          |

## Notes

- `DesktopFonts.fileFor(name)` resolves a display name to the regular-weight
  resource path; bold companions are listed here for when bold Compose
  `FontFamily` wiring is added (see TODO in `DesktopFonts.kt`).
- KFGQPC Hafs has an embedded end-of-ayah marker — see
  `DesktopFonts.isEmbeddedEndMarker()` (mirrors Android's
  `usesEmbeddedEndMarker`): do not prepend U+06DD for that font.
