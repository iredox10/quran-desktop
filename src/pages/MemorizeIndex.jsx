import React, { useState, useEffect, useMemo } from 'react';
import { useQuery } from '@tanstack/react-query';
import { getChapters } from '../services/api/quranApi';
import { Link } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';
import { useAppStore } from '../store/useAppStore';
import { Search, Award, X, ArrowRight, CheckCircle, Folder, BookOpen } from 'lucide-react';
import { Helmet } from 'react-helmet-async';

export default function MemorizeIndex() {
    const { setNavHeaderTitle, bookmarks, collections, memorizedSurahs, memorizedAyahs, readingSessions } = useAppStore();
    const [searchQuery, setSearchQuery] = useState('');
    const [showOnlyMemorized, setShowOnlyMemorized] = useState(false);
    const [showSurahsModal, setShowSurahsModal] = useState(false);
    const [showAyahsModal, setShowAyahsModal] = useState(false);

    useEffect(() => {
        setNavHeaderTitle('Hifdh');
        return () => setNavHeaderTitle(null);
    }, [setNavHeaderTitle]);

    const { data: chapters, isLoading, error } = useQuery({
        queryKey: ['chapters'],
        queryFn: getChapters,
    });

    const lastSession = useMemo(() => {
        if (!readingSessions?.length) return null;
        const memSessions = readingSessions.filter(s => s.type === 'memorizing' && s.chapterId);
        if (!memSessions.length) return null;
        return memSessions[memSessions.length - 1];
    }, [readingSessions]);

    const lastSessionChapter = useMemo(() => {
        if (!lastSession || !chapters) return null;
        return chapters.find(c => c.id === lastSession.chapterId);
    }, [lastSession, chapters]);

    const surahMemCounts = useMemo(() => {
        const counts = {};
        (memorizedAyahs || []).forEach(key => {
            const surahId = key.split(':')[0];
            counts[surahId] = (counts[surahId] || 0) + 1;
        });
        return counts;
    }, [memorizedAyahs]);

    const memorizedAyahsGrouped = useMemo(() => {
        const acc = {};
        (memorizedAyahs || []).forEach(key => {
            const [surahId, ayahNum] = key.split(':');
            if (!acc[surahId]) acc[surahId] = [];
            acc[surahId].push(Number(ayahNum));
        });
        Object.keys(acc).forEach(id => acc[id].sort((a, b) => a - b));
        return acc;
    }, [memorizedAyahs]);

    const timeSince = useMemo(() => {
        if (!lastSession) return '';
        const diff = Date.now() - lastSession.timestamp;
        const mins = Math.floor(diff / 60000);
        if (mins < 60) return `${mins}m ago`;
        const hrs = Math.floor(mins / 60);
        if (hrs < 24) return `${hrs}h ago`;
        const days = Math.floor(hrs / 24);
        return `${days}d ago`;
    }, [lastSession]);

    if (isLoading) return (
        <div className="mx-auto max-w-[1200px] px-4 pb-20">
            <div className="py-[10vh] text-center text-[var(--mem-ink-muted)]">
                <motion.div animate={{ rotate: 360 }} transition={{ repeat: Infinity, duration: 1, ease: 'linear' }}
                    className="mb-4 inline-block h-9 w-9 rounded-full"
                    style={{ border: '3px solid var(--mem-bone-dark)', borderTopColor: 'var(--mem-teal)' }} />
                <p>Loading Surahs...</p>
            </div>
        </div>
    );

    if (error) return (
        <div className="mx-auto max-w-[1200px] px-4 pb-20">
            <div className="py-12 text-center text-[0.9rem] italic text-[var(--mem-ink-muted)]">Error fetching data. Please try again later.</div>
        </div>
    );

    let filteredChapters = chapters?.filter(c =>
        c.name_simple.toLowerCase().includes(searchQuery.toLowerCase()) ||
        c.translated_name.name.toLowerCase().includes(searchQuery.toLowerCase())
    );

    if (showOnlyMemorized) {
        filteredChapters = filteredChapters?.filter(c => (memorizedSurahs || []).includes(c.id));
    }

    const totalSurahs = (memorizedSurahs || []).length;
    const totalAyahs = (memorizedAyahs || []).length;

    return (
        <div className="mx-auto max-w-[1200px] px-4 pb-20">
            <Helmet>
                <title>Hifdh — Memorization Tracker</title>
                <meta name="description" content="Track your Quran memorization journey. Select a Surah for Hifdh." />
            </Helmet>

            <motion.div initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.4 }}>
                <p className="mb-1 mt-4 text-center font-mono text-[0.62rem] uppercase tracking-[0.14em] text-[var(--mem-ink-muted)]">Memorization</p>
                <h1 className="mb-6 text-center font-ui text-[1.6rem] font-bold text-[var(--mem-ink)]">Hifdh Tracker</h1>

                {lastSessionChapter && (
                    <Link to={`/memorize/${lastSessionChapter.id}`} className="mb-6 flex items-center gap-4 rounded-2xl bg-gradient-to-br from-[var(--mem-teal)] to-[var(--mem-teal-mid)] px-5 py-4 text-white no-underline transition-all duration-150 hover:-translate-y-px hover:shadow-[0_8px_24px_rgba(46,79,74,0.25)] md:p-5">
                        <div className="flex h-11 w-11 shrink-0 items-center justify-center rounded-xl bg-white/15"><BookOpen size={22} /></div>
                        <div className="min-w-0 flex-1">
                            <div className="mb-[0.15rem] font-ui text-base font-semibold">Continue: {lastSessionChapter.name_simple}</div>
                            <div className="font-mono text-[0.72rem] tracking-[0.02em] opacity-75">Last practiced {timeSince}</div>
                        </div>
                        <ArrowRight size={18} className="shrink-0 opacity-60" />
                    </Link>
                )}

                <div className="mb-7 flex gap-2">
                    <div className="flex-1 cursor-pointer rounded-[14px] border-[1.5px] border-[var(--mem-bone-dark)] bg-[var(--mem-cream)] px-3 py-[0.85rem] text-center transition-all duration-200 hover:border-[var(--mem-gold)] hover:shadow-[0_2px_12px_rgba(184,146,74,0.12)]" onClick={() => setShowSurahsModal(true)}>
                        <div className="font-ui text-2xl font-bold leading-[1.2] text-[var(--mem-ink)]">{totalSurahs}<small className="text-[0.7rem] font-normal text-[var(--mem-ink-muted)]">/114</small></div>
                        <div className="mt-[0.2rem] font-mono text-[0.58rem] uppercase tracking-[0.1em] text-[var(--mem-ink-muted)]">Surahs</div>
                    </div>
                    <div className="flex-1 cursor-pointer rounded-[14px] border-[1.5px] border-[var(--mem-bone-dark)] bg-[var(--mem-cream)] px-3 py-[0.85rem] text-center transition-all duration-200 hover:border-[var(--mem-gold)] hover:shadow-[0_2px_12px_rgba(184,146,74,0.12)]" onClick={() => setShowAyahsModal(true)}>
                        <div className="font-ui text-2xl font-bold leading-[1.2] text-[var(--mem-ink)]">{totalAyahs}<small className="text-[0.7rem] font-normal text-[var(--mem-ink-muted)]">/6236</small></div>
                        <div className="mt-[0.2rem] font-mono text-[0.58rem] uppercase tracking-[0.1em] text-[var(--mem-ink-muted)]">Ayahs</div>
                    </div>
                    <div className="flex-1 rounded-[14px] border-[1.5px] border-[var(--mem-bone-dark)] bg-[var(--mem-cream)] px-3 py-[0.85rem] text-center">
                        <div className="font-ui text-2xl font-bold leading-[1.2] text-[var(--mem-ink)]">{totalSurahs > 0 ? Math.round((totalAyahs / 6236) * 100) : 0}<small className="text-[0.7rem] font-normal text-[var(--mem-ink-muted)]">%</small></div>
                        <div className="mt-[0.2rem] font-mono text-[0.58rem] uppercase tracking-[0.1em] text-[var(--mem-ink-muted)]">Progress</div>
                    </div>
                </div>

                {(bookmarks?.length > 0 || collections?.length > 0) && (
                    <>
                        <div className="mb-4 mt-8 flex items-center justify-between">
                            <h2 className="font-ui text-[1.15rem] font-semibold text-[var(--mem-ink)]">Quick Resume</h2>
                            <Link to="/library" className="text-[0.78rem] font-semibold text-[var(--mem-teal)] no-underline">View Library</Link>
                        </div>
                        <div className="mb-6 grid gap-2.5" style={{ gridTemplateColumns: 'repeat(auto-fill, minmax(200px, 1fr))' }}>
                            {bookmarks?.slice(0, 4).map((b, i) => (
                                <Link key={`b-${i}`} to={`/memorize/${b.chapterId}?verse=${b.verseKey}`} className="flex items-center justify-between rounded-xl border-[1.5px] border-[var(--mem-bone-dark)] bg-[var(--mem-cream)] px-4 py-3.5 no-underline text-inherit transition-all duration-200 hover:border-[var(--mem-teal)] hover:shadow-[0_2px_10px_rgba(46,79,74,0.08)]">
                                    <div>
                                        <div className="text-[0.9rem] font-semibold text-[var(--mem-teal)]">{b.surahName}</div>
                                        <div className="mt-0.5 text-[0.72rem] text-[var(--mem-ink-muted)]">Ayah {b.verseKey.split(':')[1]}</div>
                                    </div>
                                    <ArrowRight size={14} color="var(--mem-teal)" />
                                </Link>
                            ))}
                            {collections?.slice(0, 2).map(c => (
                                <Link key={c.id} to={`/memorize/${c.items[0]?.chapterId}`} className="flex items-center gap-3 rounded-xl border-[1.5px] border-[var(--mem-gold)] bg-[var(--mem-gold-soft)] px-4 py-3.5 no-underline text-inherit transition-all duration-200 hover:shadow-[0_2px_10px_rgba(184,146,74,0.15)]">
                                    <Folder size={18} color="var(--mem-gold)" />
                                    <div>
                                        <div className="text-[0.9rem] font-semibold text-[var(--mem-gold)]">{c.name}</div>
                                        <div className="mt-0.5 text-[0.72rem] text-[var(--mem-ink-muted)]">{c.items.length} verses</div>
                                    </div>
                                </Link>
                            ))}
                        </div>
                    </>
                )}

                <div className="mb-4 flex items-center gap-3 rounded-[14px] border-[1.5px] border-[var(--mem-bone-dark)] bg-[var(--mem-cream)] px-[1.15rem] py-[0.85rem] transition-colors duration-200 focus-within:border-[var(--mem-teal)] md:px-5">
                    <Search size={18} className="shrink-0 text-[var(--mem-ink-muted)]" />
                    <input type="text" placeholder="Search Surahs..." value={searchQuery}
                        onChange={e => setSearchQuery(e.target.value)}
                        className="w-full border-none bg-transparent font-[inherit] text-[0.95rem] text-[var(--mem-ink)] outline-none placeholder:text-[var(--mem-ink-muted)]" />
                </div>

                <div className="mb-5 flex justify-end gap-2">
                    <button
                        className={`flex cursor-pointer items-center gap-1.5 rounded-[20px] border-[1.5px] px-3.5 py-[7px] text-[0.78rem] font-semibold font-[inherit] transition-all duration-200 ${
                            showOnlyMemorized
                                ? 'border-[var(--mem-green)] bg-[var(--mem-green-soft)] text-[var(--mem-green)]'
                                : 'border-[var(--mem-bone-dark)] bg-transparent text-[var(--mem-ink-muted)]'
                        }`}
                        onClick={() => setShowOnlyMemorized(!showOnlyMemorized)}
                    >
                        <Award size={14} />
                        {showOnlyMemorized ? 'Memorized Only' : 'Filter Memorized'}
                    </button>
                </div>

                <div className="grid grid-cols-1 gap-2.5 sm:grid-cols-2 md:grid-cols-3">
                    {filteredChapters?.map(chapter => {
                        const isMemorized = (memorizedSurahs || []).includes(chapter.id);
                        const memCount = surahMemCounts[String(chapter.id)] || 0;
                        const memPct = Math.round((memCount / chapter.verses_count) * 100);
                        const hasPartial = !isMemorized && memCount > 0;

                        return (
                            <motion.div key={chapter.id} whileHover={{ scale: 1.01 }} whileTap={{ scale: 0.98 }}>
                                <Link to={`/memorize/${chapter.id}`} className={`flex items-center gap-3 overflow-hidden rounded-[14px] border-[1.5px] bg-[var(--mem-cream)] p-4 no-underline text-inherit transition-all duration-150 hover:-translate-y-px hover:border-[var(--mem-teal)] hover:shadow-[0_4px_14px_rgba(46,79,74,0.08)] ${
                                    isMemorized ? 'border-[var(--mem-green)]' : 'border-[var(--mem-bone-dark)]'
                                }`}>
                                    <div className={`flex h-9 w-9 shrink-0 items-center justify-center rounded-lg font-mono text-[0.8rem] font-bold ${
                                        isMemorized ? 'bg-[var(--mem-green-soft)] text-[var(--mem-green)]' : 'bg-[var(--mem-teal-soft)] text-[var(--mem-teal)]'
                                    }`}>{chapter.id}</div>
                                    <div className="min-w-0 flex-1">
                                        <div className="flex items-center gap-[0.35rem] font-ui text-[0.95rem] font-semibold text-[var(--mem-ink)]">
                                            {chapter.name_simple}
                                            {isMemorized && <CheckCircle size={14} className="shrink-0 text-[var(--mem-green)]" />}
                                        </div>
                                        <div className="mt-[0.15rem] flex items-center gap-2 text-[0.72rem] text-[var(--mem-ink-muted)]">
                                            <span>{chapter.verses_count} Ayahs</span>
                                            {hasPartial && (
                                                <span className="rounded-lg bg-[var(--mem-gold-soft)] px-1.5 py-0.5 text-[0.62rem] font-semibold text-[var(--mem-gold)]">{memCount} memorized</span>
                                            )}
                                        </div>
                                        {(isMemorized || hasPartial) && (
                                            <div className="mt-1.5 h-[3px] overflow-hidden rounded-sm bg-[var(--mem-bone-dark)]">
                                                <div className="h-full rounded-sm transition-all duration-[0.4s] ease-in-out"
                                                    style={{ width: `${isMemorized ? 100 : memPct}%`, background: isMemorized ? 'var(--mem-green)' : 'var(--mem-teal)' }} />
                                            </div>
                                        )}
                                    </div>
                                </Link>
                            </motion.div>
                        );
                    })}
                </div>

                {filteredChapters?.length === 0 && (
                    <div className="py-12 text-center text-[0.9rem] italic text-[var(--mem-ink-muted)]">No surahs found matching your search.</div>
                )}
            </motion.div>

            <AnimatePresence>
                {showSurahsModal && (
                    <motion.div className="fixed inset-0 z-[1000] flex items-center justify-center bg-[rgba(30,35,32,0.45)] p-4 backdrop-blur-sm"
                        initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}
                        onClick={e => { if (e.target === e.currentTarget) setShowSurahsModal(false); }}
                    >
                        <motion.div className="flex max-h-[80vh] w-full max-w-[500px] flex-col overflow-hidden rounded-[20px] bg-[var(--mem-white)] shadow-[0_24px_80px_rgba(0,0,0,0.18),0_0_0_1px_rgba(0,0,0,0.05)]"
                            initial={{ opacity: 0, y: 30, scale: 0.96 }}
                            animate={{ opacity: 1, y: 0, scale: 1 }}
                            exit={{ opacity: 0, y: 20, scale: 0.96 }}
                        >
                            <div className="flex items-center justify-between border-b border-[var(--mem-bone-dark)] px-6 py-5">
                                <h3 className="flex items-center gap-2 font-ui text-[1.15rem] font-semibold text-[var(--mem-ink)]">
                                    <Award size={20} color="var(--mem-green)" /> Memorized Surahs
                                </h3>
                                <button className="flex h-[34px] w-[34px] cursor-pointer items-center justify-center rounded-full border-none bg-[var(--mem-bone)] text-[var(--mem-ink-mid)] transition-all duration-150 hover:bg-[var(--mem-bone-dark)]" onClick={() => setShowSurahsModal(false)}>
                                    <X size={18} />
                                </button>
                            </div>
                            <div className="flex-1 overflow-y-auto px-5 py-4">
                                {!(memorizedSurahs?.length > 0) ? (
                                    <div className="py-12 text-center text-[0.9rem] italic text-[var(--mem-ink-muted)]">No surahs memorized yet. Keep going!</div>
                                ) : (
                                    memorizedSurahs.map(id => {
                                        const ch = chapters?.find(c => c.id === id);
                                        return ch ? (
                                            <Link to={`/memorize/${ch.id}`} key={`surah-${id}`}
                                                className="mb-[0.3rem] flex items-center gap-3 rounded-[10px] px-3 py-3 no-underline text-inherit transition-all duration-150 hover:bg-[var(--mem-bone)]"
                                                onClick={() => setShowSurahsModal(false)}>
                                                <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-[var(--mem-green-soft)] font-bold text-[0.8rem] text-[var(--mem-green)]">{ch.id}</div>
                                                <div>
                                                    <div className="text-[0.95rem] font-semibold text-[var(--mem-ink)]">{ch.name_simple}</div>
                                                    <div className="text-[0.72rem] text-[var(--mem-ink-muted)]">{ch.verses_count} Ayahs</div>
                                                </div>
                                                <ArrowRight size={14} color="var(--mem-teal)" className="ml-auto" />
                                            </Link>
                                        ) : null;
                                    })
                                )}
                            </div>
                        </motion.div>
                    </motion.div>
                )}

                {showAyahsModal && (
                    <motion.div className="fixed inset-0 z-[1000] flex items-center justify-center bg-[rgba(30,35,32,0.45)] p-4 backdrop-blur-sm"
                        initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}
                        onClick={e => { if (e.target === e.currentTarget) setShowAyahsModal(false); }}
                    >
                        <motion.div className="flex max-h-[80vh] w-full max-w-[500px] flex-col overflow-hidden rounded-[20px] bg-[var(--mem-white)] shadow-[0_24px_80px_rgba(0,0,0,0.18),0_0_0_1px_rgba(0,0,0,0.05)]"
                            initial={{ opacity: 0, y: 30, scale: 0.96 }}
                            animate={{ opacity: 1, y: 0, scale: 1 }}
                            exit={{ opacity: 0, y: 20, scale: 0.96 }}
                        >
                            <div className="flex items-center justify-between border-b border-[var(--mem-bone-dark)] px-6 py-5">
                                <h3 className="flex items-center gap-2 font-ui text-[1.15rem] font-semibold text-[var(--mem-ink)]">
                                    <CheckCircle size={20} color="var(--mem-green)" /> Memorized Ayahs
                                </h3>
                                <button className="flex h-[34px] w-[34px] cursor-pointer items-center justify-center rounded-full border-none bg-[var(--mem-bone)] text-[var(--mem-ink-mid)] transition-all duration-150 hover:bg-[var(--mem-bone-dark)]" onClick={() => setShowAyahsModal(false)}>
                                    <X size={18} />
                                </button>
                            </div>
                            <div className="flex-1 overflow-y-auto px-5 py-4">
                                {!(memorizedAyahs?.length > 0) ? (
                                    <div className="py-12 text-center text-[0.9rem] italic text-[var(--mem-ink-muted)]">No ayahs memorized yet. Keep going!</div>
                                ) : (
                                    Object.keys(memorizedAyahsGrouped)
                                        .sort((a, b) => Number(a) - Number(b))
                                        .map(surahId => {
                                            const ch = chapters?.find(c => String(c.id) === surahId);
                                            const ayahs = memorizedAyahsGrouped[surahId];
                                            return (
                                                <div key={`ayah-group-${surahId}`} className="mb-5">
                                                    <div className="mb-2 flex items-center gap-1.5 text-[0.95rem] font-semibold text-[var(--mem-ink)]">
                                                        <Folder size={14} color="var(--mem-teal)" />
                                                        Surah {ch?.name_simple || surahId}
                                                    </div>
                                                    <div className="flex flex-wrap gap-[0.35rem]">
                                                        {ayahs.map(ayahNum => (
                                                            <Link
                                                                to={`/memorize/${surahId}?verse=${surahId}:${ayahNum}`}
                                                                onClick={() => setShowAyahsModal(false)}
                                                                key={`${surahId}:${ayahNum}`}
                                                                className="cursor-pointer rounded-md border-[1.5px] border-[var(--mem-bone-dark)] bg-[var(--mem-bone)] px-2.5 py-1 text-[0.78rem] font-semibold text-[var(--mem-ink)] no-underline transition-all duration-150 hover:border-[var(--mem-teal)] hover:bg-[var(--mem-teal-soft)] hover:text-[var(--mem-teal)]"
                                                            >
                                                                {ayahNum}
                                                            </Link>
                                                        ))}
                                                    </div>
                                                </div>
                                            );
                                        })
                                )}
                            </div>
                        </motion.div>
                    </motion.div>
                )}
            </AnimatePresence>
        </div>
    );
}
