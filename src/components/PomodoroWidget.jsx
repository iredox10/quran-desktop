import { useEffect, useMemo, useState } from 'react';
import { Clock3, Play, PlusCircle, RotateCcw, TimerReset, Trash2 } from 'lucide-react';
import { useAppStore } from '../store/useAppStore';
import PomodoroConfigModal from './PomodoroConfigModal';

function formatTime(totalSeconds) {
    const mins = String(Math.floor(totalSeconds / 60)).padStart(2, '0');
    const secs = String(totalSeconds % 60).padStart(2, '0');
    return `${mins}:${secs}`;
}

export default function PomodoroWidget({ compact = false, showConfigurator = true }) {
    const {
        pomodoroProfiles,
        activePomodoroProfileId,
        addPomodoroProfile,
        updatePomodoroProfile,
        deletePomodoroProfile,
        setActivePomodoroProfile,
        pomodoroMode,
        pomodoroIsRunning,
        pomodoroSecondsLeft,
        pomodoroCompletedFocusCount,
        pomodoroDailyGoal,
        togglePomodoroRunning,
        resetPomodoroSession,
        switchPomodoroMode,
    } = useAppStore();

    const activeProfile = useMemo(
        () => (pomodoroProfiles || []).find((profile) => profile.id === activePomodoroProfileId) || pomodoroProfiles?.[0] || null,
        [activePomodoroProfileId, pomodoroProfiles]
    );

    const [draftName, setDraftName] = useState('');
    const [draftFocusMinutes, setDraftFocusMinutes] = useState(25);
    const [draftBreakMinutes, setDraftBreakMinutes] = useState(5);
    const [isConfigModalOpen, setIsConfigModalOpen] = useState(false);

    useEffect(() => {
        if (!activeProfile) {
            return;
        }

        setDraftName(activeProfile.name);
        setDraftFocusMinutes(activeProfile.focusMinutes);
        setDraftBreakMinutes(activeProfile.breakMinutes);
    }, [activeProfile?.id]);

    const progressRatio = useMemo(() => {
        const totalSeconds = activeProfile ? (pomodoroMode === 'focus' ? activeProfile.focusMinutes : activeProfile.breakMinutes) * 60 : 0;
        return totalSeconds ? ((totalSeconds - pomodoroSecondsLeft) / totalSeconds) * 100 : 0;
    }, [activeProfile, pomodoroMode, pomodoroSecondsLeft]);

    const handleCreateProfile = () => {
        addPomodoroProfile({
            name: `Pomodoro ${(pomodoroProfiles?.length || 0) + 1}`,
            focusMinutes: 25,
            breakMinutes: 5,
        });
    };

    const handleSaveProfile = () => {
        if (!activeProfile) {
            return;
        }

        updatePomodoroProfile(activeProfile.id, {
            name: draftName,
            focusMinutes: draftFocusMinutes,
            breakMinutes: draftBreakMinutes,
        });
    };

    if (!activeProfile) {
        return null;
    }

    return (
        <>
            <button
                type="button"
                onClick={() => setIsConfigModalOpen(true)}
                className={`flex items-center gap-3 rounded-[999px] border shadow-[var(--shadow-sm)] transition-all duration-200 ${
                    compact
                        ? 'w-auto justify-center px-[0.9rem] py-[0.6rem]'
                        : 'w-full justify-between px-5 py-[0.85rem]'
                } ${
                    pomodoroIsRunning
                        ? 'border-transparent bg-accent text-white'
                        : 'border-[var(--border-color)] bg-[var(--bg-surface)] text-[var(--text-primary)]'
                }`}
            >
                <div className={`flex items-center gap-2 font-bold ${compact ? 'text-[0.9rem]' : 'text-base'}`}>
                    {pomodoroIsRunning ? <Play size={compact ? 16 : 18} fill="currentColor" /> : <Clock3 size={compact ? 16 : 18} />}
                    {compact ? (
                        <span>{formatTime(pomodoroSecondsLeft)}</span>
                    ) : (
                        <div className="flex flex-col items-start">
                            <span>{activeProfile.name} Timer</span>
                            <span className={`text-[0.75rem] font-semibold ${pomodoroIsRunning ? 'text-white/80' : 'text-[var(--text-muted)]'}`}>
                                {pomodoroCompletedFocusCount}/{pomodoroDailyGoal || 4} Sessions Today
                            </span>
                        </div>
                    )}
                </div>

                {!compact && (
                    <div className="flex items-center gap-2">
                        <span className="font-mono text-[1.1rem] font-extrabold">
                            {formatTime(pomodoroSecondsLeft)}
                        </span>
                        <div className={`h-2 w-2 rounded-full ${pomodoroIsRunning ? 'bg-green-400' : 'bg-[var(--text-muted)]'}`} />
                    </div>
                )}
            </button>

            <PomodoroConfigModal isOpen={isConfigModalOpen} onClose={() => setIsConfigModalOpen(false)} />
        </>
    );
}
