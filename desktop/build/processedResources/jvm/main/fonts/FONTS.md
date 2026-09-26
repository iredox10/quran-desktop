# Bundled Fonts

Source: `quran-kotlin/app/src/main/assets/fonts/*.ttf` (copied verbatim).

Display names match the Android settings values resolved by
`getArabicFontFamily` in `SurahScreen.kt`.

| Display name      | Regular file              | Bold file                  |
|-------------------|---------------------------|----------------------------|
| KFGQPC Hafs       | `kfgqpc_hafs.ttf`         | — (single weight)          |
| Uthman Taha Naskh | `uthman_taha_naskh.ttf`   | — (single weight)          |
| Amiri Quran       | `amiri_regular.ttf`       | `amiri_bold.ttf`           |
| Noto Naskh Arabic | `noto_regular.ttf`        | `noto_bold.ttf`            |
| Scheherazade New  | `scheherazade_regular.ttf`| `scheherazade_bold.ttf`    |
| System Default    | — (no file, uses `FontFamily.Default`) | — |

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
