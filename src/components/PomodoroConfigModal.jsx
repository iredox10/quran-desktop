import React, { useEffect, useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { X, PlusCircle, Trash2, CheckCircle2, Pause, Play, RotateCcw, TimerReset } from 'lucide-react';
import { useAppStore } from '../store/useAppStore';

function formatTime(totalSeconds) {
    const mins = String(Math.floor(totalSeconds / 60)).padStart(2, '0');
    const secs = String(totalSeconds % 60).padStart(2, '0');
    return `${mins}:${secs}`;
}

export default function PomodoroConfigModal({ isOpen, onClose }) {
    const {
        pomodoroProfiles,
        activePomodoroProfileId,
        setActivePomodoroProfile,
        addPomodoroProfile,
        updatePomodoroProfile,
        deletePomodoroProfile,
        pomodoroMode,
        pomodoroIsRunning,
        pomodoroSecondsLeft,
        togglePomodoroRunning,
        resetPomodoroSession,
        switchPomodoroMode,
        pomodoroSound,
        setPomodoroSound,
        pomodoroAutoStartBreaks,
        setPomodoroAutoStartBreaks,
        pomodoroAutoStartFocus,
        setPomodoroAutoStartFocus,
        pomodoroDailyGoal,
        setPomodoroDailyGoal,
    } = useAppStore();

    const activeProfile = (pomodoroProfiles || []).find((p) => p.id === activePomodoroProfileId) || pomodoroProfiles?.[0];

    const [draftName, setDraftName] = useState('');
    const [draftFocusMinutes, setDraftFocusMinutes] = useState(25);
    const [draftBreakMinutes, setDraftBreakMinutes] = useState(5);

    useEffect(() => {
        if (activeProfile && isOpen) {
            setDraftName(activeProfile.name);
            setDraftFocusMinutes(activeProfile.focusMinutes);
            setDraftBreakMinutes(activeProfile.breakMinutes);
        }
    }, [activeProfile, isOpen]);

    const handleCreateProfile = () => {
        addPomodoroProfile({
            name: `Pomodoro ${(pomodoroProfiles?.length || 0) + 1}`,
            focusMinutes: 25,
            breakMinutes: 5,
        });
    };

    const handleSaveProfile = () => {
        if (!activeProfile) return;
        updatePomodoroProfile(activeProfile.id, {
            name: draftName,
            focusMinutes: Number(draftFocusMinutes) || 25,
            breakMinutes: Number(draftBreakMinutes) || 5,
        });
        onClose();
    };

    if (!isOpen) return null;

    return (
        <AnimatePresence>
            {isOpen && (
                <>
                    <motion.div
                        initial={{ opacity: 0 }}
                        animate={{ opacity: 1 }}
                        exit={{ opacity: 0 }}
                        onClick={onClose}
                        className="fixed inset-0 z-[1100] bg-black/60 backdrop-blur-sm"
                    />

                    <motion.div
                        initial={{ y: '100%' }}
                        animate={{ y: 0 }}
                        exit={{ y: '100%' }}
                        transition={{ type: 'spring', damping: 28, stiffness: 280 }}
                        className="fixed inset-x-0 bottom-0 z-[1101] flex justify-center"
                    >
                        <div className="flex w-full max-w-[520px] flex-col overflow-hidden rounded-t-[24px] border border-[var(--border-color)] border-b-0 bg-[var(--bg-surface)] shadow-[0_-8px_40px_rgba(0,0,0,0.25)]"
                            style={{ maxHeight: '85vh' }}
                        >
                            <div className="flex shrink-0 justify-center pb-1 pt-3">
                                <div className="h-[5px] w-10 rounded-[9999px] bg-[var(--border-color)]" />
                            </div>

                            <div className="shrink-0 border-b border-[var(--border-color)] px-6 pb-4">
                                <div className="flex items-center justify-between">
                                    <div>
                                        <h3 className="m-0 text-[1.2rem] font-bold text-[var(--text-primary)]">Pomodoro Settings</h3>
                                        <p className="m-0 mt-1 text-[0.82rem] text-[var(--text-muted)]">
                                            Customize your reading and break intervals
                                        </p>
                                    </div>
                                    <button onClick={onClose} className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-[var(--bg-secondary)] text-[var(--text-secondary)] transition-all duration-200 hover:text-accent">
                                        <X size={18} />
                                    </button>
                                </div>
                            </div>

                            <div className="flex flex-1 flex-col gap-6 overflow-y-auto p-6">

                                <div className="flex flex-col items-center gap-4 rounded-2xl bg-[var(--bg-secondary)] px-6 py-6">
                                    <div className="font-mono text-[3rem] font-extrabold leading-none text-[var(--text-primary)]">
                                        {formatTime(pomodoroSecondsLeft)}
                                    </div>
                                    <div className="text-[0.9rem] font-semibold uppercase tracking-[0.1em] text-[var(--text-muted)]">
                                        {pomodoroMode === 'focus' ? 'Focus Session' : 'Break Time'}
                                    </div>

                                    <div className="mt-2 flex flex-wrap justify-center gap-3">
                                        <button type="button" onClick={togglePomodoroRunning} className="inline-flex cursor-pointer items-center gap-2 rounded-[999px] border-none bg-accent px-6 py-3 text-base font-bold text-white">
                                            {pomodoroIsRunning ? <Pause size={18} /> : <Play size={18} />}
                                            <span>{pomodoroIsRunning ? 'Pause' : 'Start'}</span>
                                        </button>
                                        <button type="button" onClick={resetPomodoroSession} className="inline-flex cursor-pointer items-center gap-2 rounded-[999px] border border-[var(--border-color)] bg-[var(--bg-primary)] px-5 py-3 text-base font-bold text-[var(--text-primary)]">
                                            <RotateCcw size={16} />
                                            <span>Reset</span>
                                        </button>
                                        <button type="button" onClick={switchPomodoroMode} className="inline-flex cursor-pointer items-center gap-2 rounded-[999px] border border-[var(--border-color)] bg-[var(--bg-primary)] px-5 py-3 text-base font-bold text-[var(--text-primary)]">
                                            <TimerReset size={16} />
                                            <span>{pomodoroMode === 'focus' ? 'Break' : 'Focus'}</span>
                                        </button>
                                    </div>
                                </div>

                                <div>
                                    <div className="mb-3 flex items-center justify-between">
                                        <label className="text-[0.8rem] font-semibold uppercase tracking-[0.05em] text-[var(--text-muted)]">
                                            Profiles
                                        </label>
                                        <button
                                            onClick={handleCreateProfile}
                                            className="flex cursor-pointer items-center gap-1 border-none bg-transparent text-[0.85rem] font-semibold text-accent"
                                        >
                                            <PlusCircle size={16} /> New Profile
                                        </button>
                                    </div>
                                    <div className="flex gap-2 overflow-x-auto pb-2" style={{ scrollbarWidth: 'none' }}>
                                        {(pomodoroProfiles || []).map((profile) => {
                                            const active = profile.id === activePomodoroProfileId;
                                            return (
                                                <button
                                                    key={profile.id}
                                                    type="button"
                                                    onClick={() => setActivePomodoroProfile(profile.id)}
                                                    className={`min-h-10 cursor-pointer whitespace-nowrap rounded-[999px] border-none px-5 py-[0.65rem] font-bold transition-all duration-200 ${
                                                        active
                                                            ? 'bg-accent text-white shadow-[0_4px_12px_rgba(198,168,124,0.3)]'
                                                            : 'bg-[var(--bg-secondary)] text-[var(--text-primary)]'
                                                    }`}
                                                >
                                                    {profile.name}
                                                </button>
                                            );
                                        })}
                                    </div>
                                </div>

                                <div className="flex flex-col gap-4 rounded-2xl bg-[var(--bg-secondary)] px-5 py-5">
                                    <label className="flex flex-col gap-[0.45rem]">
                                        <span className="text-[0.76rem] font-bold uppercase tracking-[0.08em] text-[var(--text-muted)]">Profile Name</span>
                                        <input
                                            type="text"
                                            value={draftName}
                                            onChange={(event) => setDraftName(event.target.value)}
                                            className="min-h-12 rounded-xl border border-[var(--border-color)] bg-[var(--bg-primary)] px-4 py-3 text-base font-medium text-[var(--text-primary)] outline-none"
                                        />
                                    </label>

                                    <div className="flex gap-4">
                                        <label className="flex flex-1 flex-col gap-[0.45rem]">
                                            <span className="text-[0.76rem] font-bold uppercase tracking-[0.08em] text-[var(--text-muted)]">Focus (mins)</span>
                                            <input
                                                type="number"
                                                min="5"
                                                max="120"
                                                value={draftFocusMinutes}
                                                onChange={(event) => setDraftFocusMinutes(event.target.value === '' ? '' : Number(event.target.value))}
                                                className="min-h-12 rounded-xl border border-[var(--border-color)] bg-[var(--bg-primary)] px-4 py-3 text-base font-medium text-[var(--text-primary)] outline-none"
                                            />
                                        </label>

                                        <label className="flex flex-1 flex-col gap-[0.45rem]">
                                            <span className="text-[0.76rem] font-bold uppercase tracking-[0.08em] text-[var(--text-muted)]">Break (mins)</span>
                                            <input
                                                type="number"
                                                min="1"
                                                max="60"
                                                value={draftBreakMinutes}
                                                onChange={(event) => setDraftBreakMinutes(event.target.value === '' ? '' : Number(event.target.value))}
                                                className="min-h-12 rounded-xl border border-[var(--border-color)] bg-[var(--bg-primary)] px-4 py-3 text-base font-medium text-[var(--text-primary)] outline-none"
                                            />
                                        </label>
                                    </div>
                                </div>

                                <div>
                                    <label className="mb-3 block text-[0.8rem] font-semibold uppercase tracking-[0.05em] text-[var(--text-muted)]">
                                        Timer Sound
                                    </label>
                                    <select
                                        value={pomodoroSound || 'allahu-akbar'}
                                        onChange={(e) => {
                                            const val = e.target.value;
                                            setPomodoroSound(val);
                                            if (val !== 'silent') {
                                                try {
                                                    const audioUrl = val === 'allahu-akbar' ? '/allahu-akbar.mp3' :
                                                        val === 'bismillah' ? '/bismillah.mp3' :
                                                            val === 'alhamdulillah' ? '/alhamdulillah.mp3' : '';
                                                    if (audioUrl) {
                                                        const ad = new Audio(audioUrl);
                                                        ad.play().catch(() => { });
                                                    }
                                                } catch {
                                                    // ignore
                                                }
                                            }
                                        }}
                                        className="min-h-12 w-full cursor-pointer appearance-none rounded-xl border border-[var(--border-color)] bg-[var(--bg-primary)] px-4 py-3 text-base font-medium text-[var(--text-primary)] outline-none"
                                    >
                                        <option value="allahu-akbar">Allahu Akbar (Adhan Snippet)</option>
                                        <option value="bismillah">Bismillah (Mishary Alafasy)</option>
                                        <option value="alhamdulillah">Alhamdulillah (Mishary Alafasy)</option>
                                        <option value="silent">Silent / No Sound</option>
                                    </select>
                                </div>

                                <div className="flex flex-col gap-4 rounded-2xl bg-[var(--bg-secondary)] px-5 py-5">
                                    <label className="flex cursor-pointer items-center justify-between">
                                        <span className="text-[0.9rem] font-semibold text-[var(--text-primary)]">Auto-start Breaks</span>
                                        <input
                                            type="checkbox"
                                            checked={pomodoroAutoStartBreaks}
                                            onChange={(e) => setPomodoroAutoStartBreaks(e.target.checked)}
                                            className="h-5 w-5 cursor-pointer"
                                            style={{ accentColor: 'var(--accent-primary)' }}
                                        />
                                    </label>

                                    <div className="h-px w-full bg-[var(--border-color)]" />

                                    <label className="flex cursor-pointer items-center justify-between">
                                        <span className="text-[0.9rem] font-semibold text-[var(--text-primary)]">Auto-start Focus Sessions</span>
                                        <input
                                            type="checkbox"
                                            checked={pomodoroAutoStartFocus}
                                            onChange={(e) => setPomodoroAutoStartFocus(e.target.checked)}
                                            className="h-5 w-5 cursor-pointer"
                                            style={{ accentColor: 'var(--accent-primary)' }}
                                        />
                                    </label>

                                    <div className="h-px w-full bg-[var(--border-color)]" />

                                    <label className="flex items-center justify-between">
                                        <div className="flex flex-col">
                                            <span className="text-[0.9rem] font-semibold text-[var(--text-primary)]">Daily Session Goal</span>
                                            <span className="text-[0.75rem] text-[var(--text-muted)]">How many focus sessions to aim for today</span>
                                        </div>
                                        <input
                                            type="number"
                                            min="1"
                                            max="20"
                                            value={pomodoroDailyGoal}
                                            onChange={(e) => setPomodoroDailyGoal(Number(e.target.value) || 4)}
                                            className="min-h-[38px] w-[70px] rounded-lg border border-[var(--border-color)] bg-[var(--bg-primary)] px-2 py-2 text-center text-base font-semibold text-[var(--text-primary)] outline-none"
                                        />
                                    </label>
                                </div>

                                <div className="mt-2 flex gap-4">
                                    {(pomodoroProfiles || []).length > 1 && (
                                        <button
                                            type="button"
                                            onClick={() => {
                                                if (window.confirm('Are you sure you want to delete this Pomodoro timer?')) {
                                                    deletePomodoroProfile(activeProfile.id);
                                                }
                                            }}
                                            className="inline-flex min-h-12 cursor-pointer items-center justify-center rounded-[14px] border-none bg-red-500/10 px-5 font-bold text-red-500"
                                        >
                                            <Trash2 size={20} />
                                        </button>
                                    )}
                                    <button
                                        type="button"
                                        onClick={handleSaveProfile}
                                        className="flex min-h-12 flex-1 cursor-pointer items-center justify-center gap-2 rounded-[14px] border-none bg-[var(--text-primary)] text-base font-bold text-[var(--bg-primary)]"
                                    >
                                        <CheckCircle2 size={18} /> Save & Apply
                                    </button>
                                </div>
                            </div>

                        </div>
                    </motion.div>
                </>
            )}
        </AnimatePresence>
    );
}
