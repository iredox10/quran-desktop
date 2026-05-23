import React, { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { Play, Pause, Settings, X, Timer } from 'lucide-react';
import { useAppStore } from '../store/useAppStore';
import PomodoroConfigModal from './PomodoroConfigModal';

function formatTime(totalSeconds) {
    const mins = String(Math.floor(totalSeconds / 60)).padStart(2, '0');
    const secs = String(totalSeconds % 60).padStart(2, '0');
    return `${mins}:${secs}`;
}

export default function FloatingPomodoro() {
    const {
        pomodoroIsRunning,
        pomodoroSecondsLeft,
        pomodoroMode,
        showGlobalPomodoro,
        togglePomodoroRunning,
    } = useAppStore();

    const [isConfigModalOpen, setIsConfigModalOpen] = useState(false);
    const [isMinimized, setIsMinimized] = useState(false);

    useEffect(() => {
        if (!showGlobalPomodoro) {
            setIsMinimized(false);
        }
    }, [showGlobalPomodoro]);

    if (!showGlobalPomodoro) {
        return null;
    }

    return (
        <>
            <AnimatePresence>
                {showGlobalPomodoro && !isMinimized && (
                    <motion.div
                        initial={{ opacity: 0, y: 20, scale: 0.95 }}
                        animate={{ opacity: 1, y: 0, scale: 1 }}
                        exit={{ opacity: 0, y: 20, scale: 0.95 }}
                        transition={{ type: 'spring', stiffness: 400, damping: 25 }}
                        className="fixed bottom-20 right-5 z-[1000] flex items-center gap-2 rounded-[999px] border border-[var(--border-color)] bg-[var(--bg-surface)] px-4 py-[0.4rem] shadow-[var(--shadow-lg)]"
                    >
                        <div className="flex cursor-pointer items-center gap-2" onClick={() => setIsConfigModalOpen(true)}>
                            <Timer size={16} color={pomodoroMode === 'focus' ? 'var(--accent-primary)' : '#4ade80'} />
                            <span className="w-12 text-center font-mono text-[1.2rem] font-extrabold text-[var(--text-primary)]">
                                {formatTime(pomodoroSecondsLeft)}
                            </span>
                        </div>

                        <div className="mx-1 h-6 w-px bg-[var(--border-color)]" />

                        <button
                            onClick={togglePomodoroRunning}
                            className={`flex h-9 w-9 items-center justify-center rounded-full border-none transition-all duration-200 ${
                                pomodoroIsRunning
                                    ? pomodoroMode === 'focus' ? 'bg-accent text-white' : 'bg-green-400 text-white'
                                    : 'bg-[var(--bg-secondary)] text-[var(--text-primary)]'
                            }`}
                        >
                            {pomodoroIsRunning ? <Pause size={16} fill="currentColor" /> : <Play size={16} fill="currentColor" className="ml-[2px]" />}
                        </button>

                        <button
                            onClick={() => setIsConfigModalOpen(true)}
                            className="flex h-8 w-8 cursor-pointer items-center justify-center rounded-full border-none bg-transparent text-[var(--text-muted)] transition-all duration-200 hover:bg-[var(--bg-secondary)] hover:text-accent hover:shadow-[var(--shadow-sm)]"
                        >
                            <Settings size={16} />
                        </button>

                        <button
                            onClick={() => setIsMinimized(true)}
                            className="flex h-8 w-8 cursor-pointer items-center justify-center rounded-full border-none bg-transparent text-[var(--text-muted)] transition-all duration-200 hover:bg-[var(--bg-secondary)] hover:text-accent hover:shadow-[var(--shadow-sm)]"
                        >
                            <X size={16} />
                        </button>
                    </motion.div>
                )}

                {showGlobalPomodoro && isMinimized && (
                    <motion.div
                        drag
                        dragMomentum={false}
                        initial={{ opacity: 0, scale: 0.5 }}
                        animate={{ opacity: 1, scale: 1 }}
                        exit={{ opacity: 0, scale: 0.5 }}
                        transition={{ type: 'spring', stiffness: 400, damping: 25 }}
                        onTap={() => setIsMinimized(false)}
                        whileHover={{ scale: 1.1 }}
                        whileTap={{ scale: 0.9 }}
                        className={`fixed bottom-[100px] right-[25px] z-[1000] flex h-9 w-9 cursor-grab items-center justify-center rounded-full ${
                            pomodoroIsRunning
                                ? pomodoroMode === 'focus' ? 'bg-accent' : 'bg-green-400'
                                : 'bg-[var(--bg-surface)]'
                        }`}
                        style={{
                            border: pomodoroIsRunning
                                ? '2px solid transparent'
                                : `2px solid ${pomodoroMode === 'focus' ? 'var(--accent-primary)' : '#4ade80'}`,
                            boxShadow: '0 4px 12px rgba(0,0,0,0.15)',
                        }}
                    >
                        <div
                            className="h-3 w-3 rounded-full transition-all duration-300"
                            style={{
                                background: pomodoroIsRunning ? '#fff' : (pomodoroMode === 'focus' ? 'var(--accent-primary)' : '#4ade80'),
                                opacity: pomodoroIsRunning ? 1 : 0.8,
                                boxShadow: pomodoroIsRunning ? '0 0 8px rgba(255,255,255,0.8)' : 'none',
                            }}
                        />
                    </motion.div>
                )}
            </AnimatePresence>

            <PomodoroConfigModal isOpen={isConfigModalOpen} onClose={() => setIsConfigModalOpen(false)} />
        </>
    );
}
