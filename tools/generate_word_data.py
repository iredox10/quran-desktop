#!/usr/bin/env python3
"""Build the desktop word-by-word dataset from the Android app's assets.

The Android project (`../quran-kotlin`) ships one JSON file per chapter with
Uthmani text, translation, transliteration and tajweed markup for every word.
The desktop reader needs the same data for its word tooltips, so this script
copies it into `desktop/src/main/resources/data/words/{chapter}.json` using a
compact schema:

    [{"k": "1:1", "w": [{"a": <uthmani>, "t": <translation>,
                          "y": <transliteration>, "j": <tajweed html>,
                          "e": 0|1 (end-of-ayah marker)}]}]

Usage:
    python3 tools/generate_word_data.py [--source PATH] [--out PATH]
"""

import argparse
import glob
import json
import os

DEFAULT_SOURCE = "../quran-kotlin/app/src/main/assets/data/words"
DEFAULT_OUT = "desktop/src/main/resources/data/words"


def convert_chapter(path):
    with open(path, encoding="utf-8") as fh:
        data = json.load(fh)
    verses = []
    for verse in data.get("verses", []):
        words = []
        for word in verse.get("words", []):
            words.append(
                {
                    "a": word.get("text_uthmani") or "",
                    "t": word.get("translation") or "",
                    "y": word.get("transliteration") or "",
                    "j": word.get("text_uthmani_tajweed") or "",
                    "e": 1 if word.get("char_type_name") == "end" else 0,
                }
            )
        verses.append({"k": verse.get("verse_key") or "", "w": words})
    return verses


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--source", default=DEFAULT_SOURCE)
    parser.add_argument("--out", default=DEFAULT_OUT)
    args = parser.parse_args()

    paths = sorted(glob.glob(os.path.join(args.source, "*.json")), key=lambda p: int(os.path.basename(p)[:-5]))
    if not paths:
        raise SystemExit(f"No chapter word files found under {args.source}")

    os.makedirs(args.out, exist_ok=True)
    total_words = 0
    total_bytes = 0
    for path in paths:
        chapter = os.path.basename(path)[:-5]
        verses = convert_chapter(path)
        total_words += sum(len(v["w"]) for v in verses)
        out_path = os.path.join(args.out, f"{chapter}.json")
        with open(out_path, "w", encoding="utf-8") as fh:
            json.dump(verses, fh, ensure_ascii=False, separators=(",", ":"))
        total_bytes += os.path.getsize(out_path)

    print(f"Wrote {len(paths)} chapters, {total_words} words ({total_bytes / 1e6:.2f} MB) to {args.out}")


if __name__ == "__main__":
    main()
