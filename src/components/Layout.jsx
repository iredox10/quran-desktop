import { useCallback, useEffect, useState } from 'react';
import { Outlet, Link, useLocation, useNavigate } from 'react-router-dom';
import { useAppStore } from '../store/useAppStore';
import { getLocalAudioDirHandle } from '../utils/localAudio';
import { Moon, Sun, Settings, TrendingUp, LayoutDashboard, Bookmark, ArrowLeft, BookOpen, ChevronsDown, Volume2, ChevronDown } from 'lucide-react';
import { motion, AnimatePresence } from 'framer-motion';
import GlobalAudioPlayer from './GlobalAudioPlayer';
import SettingsDrawer from './SettingsDrawer';
import NavigationModal from './NavigationModal';

export default function Layout() {
    const {
        theme, toggleTheme, navHeaderTitle, readingMode, setReadingMode,
        autoScroll, setAutoScroll,
        setLocalAudioDirHandle,
        isPlayerVisible, setIsPlayerVisible,
        audioPlaylist, currentAudioUrl, isPlaying,
        incrementPlayTrigger,
        isSettingsOpen, setIsSettingsOpen,
        pomodoroIsRunning, tickPomodoro,
    } = useAppStore();
    const [showHeader, setShowHeader] = useState(true);
    const [lastScrollY, setLastScrollY] = useState(0);
    const [isNavModalOpen, setIsNavModalOpen] = useState(false);

    const location = useLocation();
    const navigate = useNavigate();

    const isSurahPage = /^\/surah\/\d+/.test(location.pathname);
    const isMemorizePage = /^\/memorize\/\d+/.test(location.pathname);
    const isPagePage = /^\/page\/\d+/.test(location.pathname);
    const isImmersivePage = isSurahPage || isMemorizePage || isPagePage;
    const hasAudio = audioPlaylist.length > 0 || !!currentAudioUrl;
    const shouldReturnToPlanner = Boolean(location.state?.backToPlanner);
    const shouldForceHomeBack = isSurahPage || isMemorizePage;

    const navigateHomeAtTop = useCallback((replace = false) => {
        navigate('/', {
            replace,
            state: {
                scrollToTop: Date.now(),
            },
        });
    }, [navigate]);

    const handleImmersiveBack = () => {
        if (shouldForceHomeBack) {
            navigateHomeAtTop(true);
            return;
        }

        if (shouldReturnToPlanner) {
            navigate('/planner');
            return;
        }

        navigate('/');
    };

    useEffect(() => {
        document.documentElement.setAttribute('data-theme', theme);
    }, [theme]);

    useEffect(() => {
        getLocalAudioDirHandle().then(handle => {
            if (handle) setLocalAudioDirHandle(handle);
        }).catch(err => console.warn('Could not load local directory handle', err));
    }, [setLocalAudioDirHandle]);

    useEffect(() => {
        if (!pomodoroIsRunning) {
            return undefined;
        }

        const intervalId = window.setInterval(() => {
            tickPomodoro();
        }, 1000);

        return () => window.clearInterval(intervalId);
    }, [pomodoroIsRunning, tickPomodoro]);

    useEffect(() => {
        if (!shouldForceHomeBack) {
            return undefined;
        }

        const state = window.history.state || {};
        if (!state.quranBackTrap || state.quranBackTrapPath !== location.pathname) {
            window.history.pushState(
                {
                    ...state,
                    quranBackTrap: true,
                    quranBackTrapPath: location.pathname,
                },
                ''
            );
        }

        const handlePopState = () => {
            navigateHomeAtTop(true);
        };

        window.addEventListener('popstate', handlePopState);
        return () => window.removeEventListener('popstate', handlePopState);
    }, [location.pathname, navigateHomeAtTop, shouldForceHomeBack]);

    useEffect(() => {
        let hideTimer;

        const handleActivity = () => {
            setShowHeader(true);
            if (isMemorizePage) {
                if (hideTimer) clearTimeout(hideTimer);
                hideTimer = setTimeout(() => {
                    setShowHeader(false);
                }, 3000);
            }
        };

        const handleScroll = () => {
            const currentScrollY = window.scrollY;

            if (!isMemorizePage) {
                if (currentScrollY > lastScrollY && currentScrollY > 100) {
                    setShowHeader(false);
                } else {
                    setShowHeader(true);
                }
            } else {
                handleActivity();
            }
            setLastScrollY(currentScrollY);
        };

        window.addEventListener('scroll', handleScroll, { passive: true });

        if (isMemorizePage) {
            window.addEventListener('mousemove', handleActivity);
            window.addEventListener('touchstart', handleActivity);
            window.addEventListener('click', handleActivity);
            handleActivity();
        }

        return () => {
            window.removeEventListener('scroll', handleScroll);
            if (isMemorizePage) {
                window.removeEventListener('mousemove', handleActivity);
                window.removeEventListener('touchstart', handleActivity);
                window.removeEventListener('click', handleActivity);
                if (hideTimer) clearTimeout(hideTimer);
            }
        };
    }, [lastScrollY, isMemorizePage]);

    useEffect(() => {
        setShowHeader(true);
    }, [location.pathname]);

    useEffect(() => {
        window.scrollTo(0, 0);
    }, [location.pathname]);

    return (
        <div className="flex min-h-screen flex-col">
            <header
                className={`fixed inset-x-0 top-0 z-[1000] border-b-[var(--glass-border)] bg-[var(--bg-surface)] backdrop-blur-xl transition-all duration-[0.4s] ${
                    showHeader
                        ? 'translate-y-0 shadow-[0_4px_20px_rgba(0,0,0,0.06)] pointer-events-auto'
                        : '-translate-y-full shadow-none pointer-events-none'
                }`}
                style={{ transitionTimingFunction: 'cubic-bezier(0.16, 1, 0.3, 1)' }}
            >
                <div className="container flex h-14 items-center justify-between">
                    <div className="flex min-w-0 items-center gap-[10px]">
                        {isImmersivePage ? (
                            <>
                                <button className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full text-[var(--text-secondary)] transition-all duration-200 hover:bg-[var(--bg-secondary)] hover:text-accent hover:shadow-[var(--shadow-sm)]" onClick={handleImmersiveBack} aria-label="Go back">
                                    <ArrowLeft size={20} />
                                </button>
                                <button
                                    onClick={() => setIsNavModalOpen(true)}
                                    className="flex cursor-pointer items-center gap-[6px] rounded-lg border-none bg-transparent px-2 py-1 transition-all duration-200 hover:bg-[var(--bg-secondary)]"
                                >
                                    <span className="max-w-[200px] overflow-hidden text-ellipsis whitespace-nowrap text-[1.1rem] font-semibold text-[var(--text-primary)] md:max-w-[300px]">
                                        {navHeaderTitle || 'Page'}
                                    </span>
                                    <ChevronDown size={16} className="text-[var(--text-muted)]" />
                                </button>
                            </>
                        ) : (
                            <>
                                <Link to="/" className="no-underline">
                                    <span className="text-[1.5rem] font-bold tracking-tight text-accent">
                                        Qur'an
                                    </span>
                                </Link>
                                {navHeaderTitle && (
                                    <div className="flex items-center gap-2">
                                        <span className="text-[var(--text-muted)]">/</span>
                                        <span className="font-semibold text-[var(--text-primary)]">{navHeaderTitle}</span>
                                    </div>
                                )}
                            </>
                        )}
                    </div>

                    <div className="flex items-center gap-[6px]">
                        {(isSurahPage || isPagePage) && (
                            <>
                                <button
                                    onClick={() => setAutoScroll(!autoScroll)}
                                    title={autoScroll ? 'Stop Auto-scroll' : 'Auto-scroll'}
                                    className={`flex h-10 w-10 items-center justify-center rounded-full transition-all duration-200 ${
                                        autoScroll
                                            ? 'bg-[var(--accent-light)] text-accent'
                                            : 'text-[var(--text-muted)] hover:bg-[var(--bg-secondary)] hover:text-accent hover:shadow-[var(--shadow-sm)]'
                                    }`}
                                >
                                    <ChevronsDown size={20} />
                                </button>
                                <button
                                    onClick={() => setReadingMode(!readingMode)}
                                    title={readingMode ? 'Translation Mode' : 'Reading Mode'}
                                    className={`flex h-10 w-10 items-center justify-center rounded-full transition-all duration-200 ${
                                        readingMode
                                            ? 'bg-[var(--accent-light)] text-accent'
                                            : 'text-[var(--text-muted)] hover:bg-[var(--bg-secondary)] hover:text-accent hover:shadow-[var(--shadow-sm)]'
                                    }`}
                                >
                                    <BookOpen size={20} />
                                </button>
                            </>
                        )}

                        {!location.pathname.startsWith('/memorize') &&
                            !['/', '/progress', '/profile'].includes(location.pathname) && (
                                <button
                                    id="audio-player-toggle"
                                    onClick={() => {
                                        if (isSurahPage || isPagePage) {
                                            incrementPlayTrigger();
                                        } else {
                                            setIsPlayerVisible(!isPlayerVisible);
                                        }
                                    }}
                                    title={(isSurahPage || isPagePage) ? 'Play / Pause' : isPlayerVisible ? 'Hide Player' : 'Show Player'}
                                    className={`relative flex h-10 w-10 items-center justify-center rounded-full transition-all duration-200 ${
                                        (isPlayerVisible || isPlaying) ? 'bg-[var(--accent-light)]' : 'hover:bg-[var(--bg-secondary)] hover:shadow-[var(--shadow-sm)]'
                                    } ${
                                        hasAudio ? 'text-accent' : 'text-[var(--text-muted)] hover:text-accent'
                                    }`}
                                >
                                    <Volume2 size={20} />
                                    {isPlaying && (
                                        <span className="absolute right-1 top-1 h-[6px] w-[6px] animate-[pulse_2s_infinite] rounded-full bg-accent" />
                                    )}
                                </button>
                            )}

                        <button onClick={toggleTheme} className="flex h-10 w-10 items-center justify-center rounded-full text-[var(--text-secondary)] transition-all duration-200 hover:bg-[var(--bg-secondary)] hover:text-accent hover:shadow-[var(--shadow-sm)]" aria-label="Toggle Theme">
                            <AnimatePresence mode="wait">
                                <motion.div
                                    key={theme}
                                    initial={{ rotate: -90, opacity: 0 }}
                                    animate={{ rotate: 0, opacity: 1 }}
                                    exit={{ rotate: 90, opacity: 0 }}
                                    transition={{ duration: 0.2 }}
                                >
                                    {theme === 'light' ? <Moon size={20} /> : <Sun size={20} />}
                                </motion.div>
                            </AnimatePresence>
                        </button>
                        <button onClick={() => setIsSettingsOpen(true)} className="flex h-10 w-10 items-center justify-center rounded-full text-[var(--text-secondary)] transition-all duration-200 hover:bg-[var(--bg-secondary)] hover:text-accent hover:shadow-[var(--shadow-sm)]">
                            <Settings size={20} />
                        </button>
                    </div>
                </div>
            </header>

            <main className="flex-1 px-6 pb-[90px] pt-[calc(56px+2.5rem)]">
                <Outlet />
            </main>

            <GlobalAudioPlayer />
            <SettingsDrawer isOpen={isSettingsOpen} onClose={() => setIsSettingsOpen(false)} />
            <NavigationModal isOpen={isNavModalOpen} onClose={() => setIsNavModalOpen(false)} />
        </div>
    );
}
