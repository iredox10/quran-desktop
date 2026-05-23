import React, { useRef, useEffect, useState } from 'react';
import { useAppStore } from '../store/useAppStore';
import { getLocalAudioUrl } from '../utils/localAudio';
import { Play, Pause, X, Music, SkipBack, SkipForward, Square, Settings2 } from 'lucide-react';
import { motion, AnimatePresence } from 'framer-motion';

const DELAY_OPTIONS = [0, 1, 2, 3, 5, 10];
const REPEAT_OPTIONS = [1, 2, 3, 5, 10, -1];
const SPEED_OPTIONS = [0.5, 0.75, 1.0, 1.25, 1.5, 2.0];

export default function GlobalAudioPlayer() {
    const {
        currentAudioUrl, audioPlaylist, audioTrackIndex, isPlaying, audioSettings,
        setAudioTrackIndex, updateAudioSettings, setIsPlaying, stopAudio,
        isPlayerVisible, setIsPlayerVisible,
        localAudioDirHandle
    } = useAppStore();

    const audioRef = useRef(null);
    const delayTimeoutRef = useRef(null);
    const prevIsPlayingRef = useRef(false);

    const [isSettingsOpen, setIsSettingsOpen] = useState(false);
    const [currentAyaLoopCount, setCurrentAyaLoopCount] = useState(0);
    const [currentSelectionLoopCount, setCurrentSelectionLoopCount] = useState(0);

    const hasAudio = !!(currentAudioUrl || audioPlaylist.length > 0);
    const activeUrl = audioPlaylist.length > 0 ? audioPlaylist[audioTrackIndex]?.url : currentAudioUrl;
    const currentTitle = audioPlaylist.length > 0
        ? `Ayah ${audioPlaylist[audioTrackIndex]?.verseNumber || '...'}`
        : 'Recitation';

    // Scroll to & highlight current verse while playing
    useEffect(() => {
        if (isPlaying && audioSettings.scrollWhilePlaying && audioPlaylist.length > 0) {
            const currentVerse = audioPlaylist[audioTrackIndex];
            if (currentVerse?.verseKey) {
                const el = document.getElementById(`verse-${currentVerse.verseKey}`);
                if (el) {
                    el.scrollIntoView({ behavior: 'smooth', block: 'center' });
                }
            }
        }
    }, [audioTrackIndex, isPlaying, audioPlaylist, audioSettings.scrollWhilePlaying]);

    // Auto-show player only on false→true transition
    useEffect(() => {
        const wasPlaying = prevIsPlayingRef.current;
        prevIsPlayingRef.current = isPlaying;
        if (!wasPlaying && isPlaying && hasAudio) {
            setIsPlayerVisible(true);
        }
    }, [isPlaying, hasAudio, setIsPlayerVisible]);

    const [resolvedAudioUrl, setResolvedAudioUrl] = useState(null);

    // Resolve local-audio:// to object URL if needed
    useEffect(() => {
        if (!activeUrl) {
            setResolvedAudioUrl(null);
            return;
        }

        if (activeUrl.startsWith('local-audio://') && localAudioDirHandle) {
            const fileName = activeUrl.replace('local-audio://', '');
            getLocalAudioUrl(localAudioDirHandle, fileName).then(url => {
                setResolvedAudioUrl(url || activeUrl);
            });
        } else {
            setResolvedAudioUrl(activeUrl);
        }

        return () => {};
    }, [activeUrl, localAudioDirHandle]);

    // Sync with audio element
    useEffect(() => {
        if (!audioRef.current) return;
        audioRef.current.playbackRate = audioSettings.playbackSpeed;
        if (isPlaying && resolvedAudioUrl) {
            if (delayTimeoutRef.current) clearTimeout(delayTimeoutRef.current);
            audioRef.current.play().catch(e => { console.error('Audio failed', e); setIsPlaying(false); });
        } else {
            audioRef.current.pause();
        }
    }, [isPlaying, resolvedAudioUrl, audioSettings.playbackSpeed]);

    const handleStop = () => { stopAudio(); setIsPlayerVisible(false); setIsSettingsOpen(false); };

    const handleEnded = () => {
        if (currentAudioUrl) { setIsPlaying(false); return; }
        if (!audioPlaylist.length) return;

        const playNext = (idx) => {
            if (audioSettings.delayBetweenAyas > 0) {
                audioRef.current?.pause();
                delayTimeoutRef.current = setTimeout(() => setAudioTrackIndex(idx), audioSettings.delayBetweenAyas * 1000);
            } else { setAudioTrackIndex(idx); }
        };

        if (audioSettings.repeatAya === -1 || currentAyaLoopCount + 1 < audioSettings.repeatAya) {
            setCurrentAyaLoopCount(p => p + 1);
            if (audioRef.current) { audioRef.current.currentTime = 0; audioRef.current.play(); }
            return;
        }

        setCurrentAyaLoopCount(0);
        const endRange = audioSettings.endRange ?? audioPlaylist.length - 1;
        const startRange = audioSettings.startRange ?? 0;

        if (audioTrackIndex >= endRange) {
            if (audioSettings.repeatSelection === -1 || currentSelectionLoopCount + 1 < audioSettings.repeatSelection) {
                setCurrentSelectionLoopCount(p => p + 1);
                playNext(startRange);
            } else {
                setCurrentSelectionLoopCount(0);
                setIsPlaying(false);
                setAudioTrackIndex(startRange);
            }
        } else {
            playNext(audioTrackIndex + 1);
        }
    };

    const handleNext = () => {
        const end = audioSettings.endRange ?? audioPlaylist.length - 1;
        if (audioTrackIndex < end) { setAudioTrackIndex(audioTrackIndex + 1); setCurrentAyaLoopCount(0); }
    };

    const handlePrev = () => {
        const start = audioSettings.startRange ?? 0;
        if (audioTrackIndex > start) { setAudioTrackIndex(audioTrackIndex - 1); setCurrentAyaLoopCount(0); }
        else if (audioRef.current) audioRef.current.currentTime = 0;
    };

    return (
        <>
            {/* ── Floating Player Pill ── */}
            <AnimatePresence>
                {isPlayerVisible && (
                    <motion.div
                        key="player-pill"
                        initial={{ y: 100, opacity: 0, x: '-50%' }}
                        animate={{ y: 0, opacity: 1, x: '-50%' }}
                        exit={{ y: 100, opacity: 0, x: '-50%' }}
                        transition={{ type: 'spring', stiffness: 300, damping: 30 }}
                        className="fixed bottom-8 left-1/2 -translate-x-1/2 w-[calc(100vw-1rem)] max-w-[480px] px-3 py-2 bg-[var(--glass-bg)] backdrop-blur-xl border-[var(--glass-border)] z-[900] flex items-center justify-between gap-2 rounded-full shadow-[var(--shadow-xl)]"
                    >
                        {hasAudio && <audio ref={audioRef} src={activeUrl || ''} onEnded={handleEnded} />}

                        {!hasAudio ? (
                            <>
                                <div className="flex items-center gap-2 flex-1 min-w-0">
                                    <div className="w-8 h-8 rounded-full bg-[var(--bg-secondary)] shrink-0 flex items-center justify-center text-[var(--text-muted)]">
                                        <Music size={16} />
                                    </div>
                                    <span className="text-[0.85rem] text-[var(--text-muted)] font-medium whitespace-nowrap overflow-hidden text-ellipsis">
                                        Open a Surah & press Play
                                    </span>
                                </div>
                                <button className="btn-icon w-7 h-7 text-[var(--text-muted)] shrink-0" onClick={() => setIsPlayerVisible(false)}>
                                    <X size={16} />
                                </button>
                            </>
                        ) : (
                            <>
                                {/* Track info (truncates if too long) */}
                                <div className="flex items-center gap-2 flex-1 min-w-0">
                                    <div className="w-8 h-8 rounded-full bg-[var(--accent-light)] shrink-0 flex items-center justify-center text-[var(--accent-primary)]">
                                        <Music size={16} />
                                    </div>
                                    <div className="flex flex-col min-w-0">
                                        <span className="text-[0.85rem] font-semibold text-[var(--text-primary)] leading-[1.2] whitespace-nowrap overflow-hidden text-ellipsis">
                                            {currentTitle}
                                        </span>
                                        <span className="text-[0.7rem] text-[var(--accent-primary)] whitespace-nowrap" style={{ opacity: isPlaying ? 1 : 0.7 }}>
                                            {isPlaying ? 'Playing' : 'Paused'}
                                        </span>
                                    </div>
                                </div>

                                {/* Controls */}
                                <div className="flex items-center gap-[0.15rem] shrink-0">
                                    {audioPlaylist.length > 0 && <button className="btn-icon w-7 h-7" onClick={handlePrev}><SkipBack size={16} /></button>}
                                    <button className="btn-primary w-9 h-9 p-0 rounded-full flex items-center justify-center mx-[0.2rem]" onClick={() => setIsPlaying(!isPlaying)}>
                                        {isPlaying ? <Pause size={18} /> : <Play size={18} className="ml-[2px]" />}
                                    </button>
                                    <button className="btn-icon w-7 h-7 text-[var(--accent-primary)]" onClick={handleStop} title="Stop"><Square size={14} fill="currentColor" /></button>
                                    {audioPlaylist.length > 0 && <button className="btn-icon w-7 h-7" onClick={handleNext}><SkipForward size={16} /></button>}
                                </div>

                                <div className="w-px h-5 bg-[var(--border-color)] mx-[0.15rem] shrink-0" />

                                {/* Settings & Close */}
                                <div className="flex items-center gap-[0.15rem] shrink-0">
                                    {audioPlaylist.length > 0 && (
                                        <button className="btn-icon w-7 h-7" onClick={() => setIsSettingsOpen(true)} style={{ color: isSettingsOpen ? 'var(--accent-primary)' : 'var(--text-muted)' }} title="Audio Settings">
                                            <Settings2 size={16} />
                                        </button>
                                    )}
                                    <button className="btn-icon w-7 h-7 text-[var(--text-muted)]" onClick={handleStop} aria-label="Close Player">
                                        <X size={16} />
                                    </button>
                                </div>
                            </>
                        )}
                    </motion.div>
                )}
            </AnimatePresence>

            {/* ── Audio Settings Bottom Drawer (shown during playback) ── */}
            <AnimatePresence>
                {isSettingsOpen && (
                    <>
                        {/* Backdrop */}
                        <motion.div
                            initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}
                            onClick={() => setIsSettingsOpen(false)}
                            className="fixed inset-0 z-[998] bg-black/50 backdrop-blur-sm"
                        />
                        {/* Bottom Drawer — outer row handles centering via flex */}
                        <motion.div
                            initial={{ y: '100%' }}
                            animate={{ y: 0 }}
                            exit={{ y: '100%' }}
                            transition={{ type: 'spring', damping: 28, stiffness: 280 }}
                            className="fixed bottom-0 inset-x-0 flex justify-center z-[999]"
                        >
                            {/* Inner card */}
                            <div className="w-full max-w-[520px] max-h-[85vh] flex flex-col bg-[var(--bg-surface)] rounded-t-[24px] shadow-[0_-8px_40px_rgba(0,0,0,0.25)] border border-[var(--border-color)] border-b-0 overflow-hidden">
                                {/* Drag handle */}
                                <div className="flex justify-center pt-3 pb-1 shrink-0">
                                    <div className="w-10 h-[5px] rounded-full bg-[var(--border-color)]" />
                                </div>

                                {/* Header */}
                                <div className="px-6 pb-4 shrink-0 flex justify-between items-center">
                                    <h3 className="m-0 text-[var(--text-primary)] text-[1.1rem] font-bold">Audio Settings</h3>
                                    <button className="btn-icon bg-[var(--bg-secondary)] w-8 h-8" onClick={() => setIsSettingsOpen(false)}>
                                        <X size={16} />
                                    </button>
                                </div>

                                {/* Scrollable body */}
                                <div className="flex-1 overflow-y-auto px-6 pb-6 grid gap-4">
                                    {/* Range */}
                                    <div className="flex gap-4">
                                        <div className="flex-1">
                                            <label className="block text-[0.85rem] text-[var(--text-muted)] mb-1">Start Ayah</label>
                                            <select className="form-input" value={audioSettings.startRange ?? 0} onChange={(e) => updateAudioSettings({ startRange: Number(e.target.value) })}>
                                                {audioPlaylist.map((v, i) => <option key={v.verseKey || i} value={i}>{v.verseKey}</option>)}
                                            </select>
                                        </div>
                                        <div className="flex-1">
                                            <label className="block text-[0.85rem] text-[var(--text-muted)] mb-1">End Ayah</label>
                                            <select className="form-input" value={audioSettings.endRange ?? audioPlaylist.length - 1} onChange={(e) => updateAudioSettings({ endRange: Number(e.target.value) })}>
                                                {audioPlaylist.map((v, i) => <option key={v.verseKey || i} value={i}>{v.verseKey}</option>)}
                                            </select>
                                        </div>
                                    </div>

                                    {/* Repeats */}
                                    <div className="flex gap-4">
                                        <div className="flex-1">
                                            <label className="block text-[0.85rem] text-[var(--text-muted)] mb-1">Repeat Ayah</label>
                                            <select className="form-input" value={audioSettings.repeatAya} onChange={(e) => updateAudioSettings({ repeatAya: Number(e.target.value) })}>
                                                {REPEAT_OPTIONS.map(opt => <option key={opt} value={opt}>{opt === -1 ? '∞ Infinite' : `${opt}×`}</option>)}
                                            </select>
                                        </div>
                                        <div className="flex-1">
                                            <label className="block text-[0.85rem] text-[var(--text-muted)] mb-1">Repeat Selection</label>
                                            <select className="form-input" value={audioSettings.repeatSelection} onChange={(e) => updateAudioSettings({ repeatSelection: Number(e.target.value) })}>
                                                {REPEAT_OPTIONS.map(opt => <option key={opt} value={opt}>{opt === -1 ? '∞ Infinite' : `${opt}×`}</option>)}
                                            </select>
                                        </div>
                                    </div>

                                    {/* Advanced */}
                                    <div className="flex gap-4">
                                        <div className="flex-1">
                                            <label className="block text-[0.85rem] text-[var(--text-muted)] mb-1">Delay (sec)</label>
                                            <select className="form-input" value={audioSettings.delayBetweenAyas} onChange={(e) => updateAudioSettings({ delayBetweenAyas: Number(e.target.value) })}>
                                                {DELAY_OPTIONS.map(opt => <option key={opt} value={opt}>{opt === 0 ? 'None' : `${opt}s`}</option>)}
                                            </select>
                                        </div>
                                        <div className="flex-1">
                                            <label className="block text-[0.85rem] text-[var(--text-muted)] mb-1">Speed</label>
                                            <select className="form-input" value={audioSettings.playbackSpeed} onChange={(e) => updateAudioSettings({ playbackSpeed: Number(e.target.value) })}>
                                                {SPEED_OPTIONS.map(opt => <option key={opt} value={opt}>{opt}×</option>)}
                                            </select>
                                        </div>
                                    </div>

                                    {/* Auto-scroll toggle */}
                                    <label className="flex items-center gap-3 cursor-pointer p-3 rounded-xl bg-[var(--bg-secondary)]">
                                        <input type="checkbox" checked={audioSettings.scrollWhilePlaying} onChange={(e) => updateAudioSettings({ scrollWhilePlaying: e.target.checked })} className="w-[18px] h-[18px] accent-[var(--accent-primary)] shrink-0" />
                                        <div>
                                            <div className="text-[0.9rem] font-semibold text-[var(--text-primary)]">Auto-scroll while playing</div>
                                            <div className="text-xs text-[var(--text-muted)]">Highlights and scrolls to each Ayah</div>
                                        </div>
                                    </label>
                                </div>
                            </div>
                        </motion.div>
                    </>
                )}
            </AnimatePresence>
            {resolvedAudioUrl && (
                <audio
                    ref={audioRef}
                    src={resolvedAudioUrl}
                    onEnded={handleEnded}
                    onError={(e) => {
                        console.error("Audio playback error", e);
                        setIsPlaying(false);
                        handleEnded();
                    }}
                />
            )}
        </>
    );
}
