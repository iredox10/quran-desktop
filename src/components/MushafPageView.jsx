import { useMemo } from 'react';
import { getWordArabicText } from '../utils/quranText';

export default function MushafPageView({ verses, mushaf, arabicFont, fontSize, activeAudioVerseKey }) {
  const lines = useMemo(() => {
    const lineMap = new Map();

    verses.forEach((verse) => {
      verse.words?.forEach((word) => {
        const lineNumber = Number(word.line_number || 0);
        if (!lineNumber) {
          return;
        }

        if (!lineMap.has(lineNumber)) {
          lineMap.set(lineNumber, []);
        }

        lineMap.get(lineNumber).push({
          key: `${verse.verse_key}-${word.position || lineMap.get(lineNumber).length}`,
          text: getWordArabicText(word, mushaf),
          verseKey: verse.verse_key,
          charType: word.char_type_name,
        });
      });
    });

    return Array.from(lineMap.entries())
      .sort((a, b) => a[0] - b[0])
      .map(([lineNumber, words]) => ({ lineNumber, words }));
  }, [mushaf, verses]);

  if (!lines.length) {
    return null;
  }

  return (
    <div className="mx-auto max-w-[840px] rounded-[24px] border border-[var(--border-color)] bg-[var(--bg-surface)] p-6 shadow-[var(--shadow-md)]">
      <div className="mb-4 text-[0.8rem] uppercase tracking-[0.08em] text-[var(--text-muted)]">
        {mushaf.name} · line-grouped page scaffolding
      </div>

      <div className="flex flex-col gap-[0.3rem]">
        {lines.map((line) => (
          <div
            key={line.lineNumber}
            data-line-number={line.lineNumber}
            className="flex min-h-[2.8rem] items-center justify-between gap-[0.4rem] text-justify"
            style={{ direction: 'rtl' }}
          >
            {line.words.map((word) => (
              <span
                key={word.key}
                style={{
                  fontFamily: arabicFont,
                  fontSize: `${fontSize * 0.4 + 1.35}rem`,
                  lineHeight: 1.95,
                  whiteSpace: 'nowrap',
                  transition: 'color 0.3s ease'
                }}
                className={
                  word.verseKey === activeAudioVerseKey || word.charType === 'end'
                    ? 'text-accent'
                    : 'text-[var(--text-primary)]'
                }
              >
                {word.text}
              </span>
            ))}
          </div>
        ))}
      </div>
    </div>
  );
}
