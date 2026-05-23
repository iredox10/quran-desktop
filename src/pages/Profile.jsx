import React, { useEffect, useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { Link } from 'react-router-dom';
import { useAppStore } from '../store/useAppStore';
import { User, Settings, Bookmark, Folder, Download, Moon, Sun, ChevronRight, HardDrive, LogOut, CloudUpload, CloudDownload, Mail, Lock, Loader2 } from 'lucide-react';
import { authService, syncService } from '../services/appwrite';

export default function Profile() {
    const store = useAppStore();
    const {
        setNavHeaderTitle,
        setIsSettingsOpen,
        bookmarks,
        collections,
        downloadedSurahs,
        theme,
        toggleTheme
    } = store;

    const [user, setUser] = useState(null);
    const [authMode, setAuthMode] = useState('login');
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [name, setName] = useState('');
    const [isLoading, setIsLoading] = useState(true);
    const [syncStatus, setSyncStatus] = useState(null);
    const [authError, setAuthError] = useState('');

    useEffect(() => {
        setNavHeaderTitle('Profile');
        checkUser();
        return () => setNavHeaderTitle(null);
    }, [setNavHeaderTitle]);

    const checkUser = async () => {
        setIsLoading(true);
        const currentUser = await authService.getCurrentUser();
        setUser(currentUser);
        setIsLoading(false);
    };

    const handleAuth = async (e) => {
        e.preventDefault();
        setIsLoading(true);
        setAuthError('');
        try {
            if (authMode === 'register') {
                await authService.register(email, password, name);
                await authService.login(email, password);
            } else {
                await authService.login(email, password);
            }
            await checkUser();
            setEmail('');
            setPassword('');
            setName('');
        } catch (error) {
            setAuthError(error.message || 'Authentication failed');
        } finally {
            setIsLoading(false);
        }
    };

    const handleLogout = async () => {
        setIsLoading(true);
        try {
            await authService.logout();
            setUser(null);
        } catch (error) {
            console.error('Logout error', error);
        } finally {
            setIsLoading(false);
        }
    };

    const handlePushSync = async () => {
        if (!user) return;
        setSyncStatus('pushing');
        try {
            const state = useAppStore.getState();
            const dataToSync = {
                theme: state.theme,
                translationId: state.translationId,
                reciterId: state.reciterId,
                fontSize: state.fontSize,
                translationFontSize: state.translationFontSize,
                readingMode: state.readingMode,
                mushafId: state.mushafId,
                arabicFontId: state.arabicFontId,
                tajweedEnabled: state.tajweedEnabled,
                tafsirId: state.tafsirId,
                bookmark: state.bookmark,
                bookmarks: state.bookmarks,
                memorizedAyahs: state.memorizedAyahs,
                memorizedSurahs: state.memorizedSurahs,
                collections: state.collections,
                recentlyRead: state.recentlyRead,
                readingSessions: state.readingSessions,
                pomodoroProfiles: state.pomodoroProfiles,
                activePomodoroProfileId: state.activePomodoroProfileId,
                pomodoroHistory: state.pomodoroHistory,
                pomodoroCompletedFocusCount: state.pomodoroCompletedFocusCount,
                planners: state.planners,
                activePlannerId: state.activePlannerId,
                downloadedSurahs: state.downloadedSurahs,
            };

            await syncService.pushState(user.$id, dataToSync);
            setSyncStatus('success');
            setTimeout(() => setSyncStatus(null), 3000);
        } catch (error) {
            console.error(error);
            setSyncStatus('error');
            setTimeout(() => setSyncStatus(null), 3000);
        }
    };

    const handlePullSync = async () => {
        if (!user) return;
        setSyncStatus('pulling');
        try {
            const remoteState = await syncService.pullState(user.$id);
            if (remoteState) {
                useAppStore.setState(remoteState);
            }
            setSyncStatus('success');
            setTimeout(() => setSyncStatus(null), 3000);
        } catch (error) {
            console.error(error);
            setSyncStatus('error');
            setTimeout(() => setSyncStatus(null), 3000);
        }
    };

    if (isLoading && !user && !authError && email === '') {
        return (
            <div className="container flex justify-center pt-16">
                <Loader2 size={32} className="animate-spin text-accent" />
            </div>
        );
    }

    return (
        <div className="mx-auto mb-24 max-w-[1000px] px-6">
            <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.5 }}>

                {!user ? (
                    <div className="relative mb-8 overflow-hidden rounded-[28px] border border-[var(--border-color)] bg-gradient-to-br from-[var(--bg-surface)] to-[var(--bg-secondary)] p-10 text-center shadow-[var(--shadow-md)]">
                        <div className="absolute inset-0 z-0 opacity-[0.08]" style={{ backgroundImage: 'radial-gradient(var(--accent-primary) 1.5px, transparent 1.5px)', backgroundSize: '32px 32px' }} />
                        <div className="relative z-[2] mb-8">
                            <div className="mx-auto mb-4 flex h-20 w-20 items-center justify-center rounded-full bg-[var(--accent-light)] text-accent">
                                <User size={40} />
                            </div>
                            <h1 className="text-[2rem] font-extrabold text-[var(--text-primary)]">Cloud Sync</h1>
                            <p className="mx-auto mt-2 max-w-[400px] text-[1.05rem] text-[var(--text-secondary)]">
                                Sign in to save your bookmarks, reading progress, and settings to the cloud. Access them from any device.
                            </p>
                        </div>

                        <form onSubmit={handleAuth} className="relative z-[2] mx-auto mb-8 max-w-[440px] rounded-[24px] border border-black/5 bg-[var(--bg-primary)] p-8 shadow-[var(--shadow-lg)]">
                            {authError && (
                                <div className="mb-5 rounded-xl bg-red-500/10 px-4 py-3 text-[0.9rem] font-semibold text-red-500">
                                    {authError}
                                </div>
                            )}

                            {authMode === 'register' && (
                                <input
                                    type="text"
                                    placeholder="Your Name"
                                    value={name}
                                    onChange={(e) => setName(e.target.value)}
                                    className="mb-4 w-full rounded-2xl border border-black/8 bg-[var(--bg-surface)] px-5 py-4 text-base font-medium text-[var(--text-primary)] outline-none transition-all duration-200 focus:border-accent focus:shadow-[0_0_0_4px_var(--accent-light)]"
                                    required
                                />
                            )}
                            <input
                                type="email"
                                placeholder="Email Address"
                                value={email}
                                onChange={(e) => setEmail(e.target.value)}
                                className="mb-4 w-full rounded-2xl border border-black/8 bg-[var(--bg-surface)] px-5 py-4 text-base font-medium text-[var(--text-primary)] outline-none transition-all duration-200 focus:border-accent focus:shadow-[0_0_0_4px_var(--accent-light)]"
                                required
                            />
                            <input
                                type="password"
                                placeholder="Password (min 8 chars)"
                                value={password}
                                onChange={(e) => setPassword(e.target.value)}
                                className="mb-4 w-full rounded-2xl border border-black/8 bg-[var(--bg-surface)] px-5 py-4 text-base font-medium text-[var(--text-primary)] outline-none transition-all duration-200 focus:border-accent focus:shadow-[0_0_0_4px_var(--accent-light)]"
                                minLength={8}
                                required
                            />

                            <button type="submit" disabled={isLoading} className="flex w-full cursor-pointer items-center justify-center gap-2 rounded-2xl border-none bg-accent px-4 py-4 text-[1.05rem] font-bold text-white transition-all duration-200 hover:bg-[var(--accent-hover)] hover:shadow-[0_4px_15px_var(--accent-light)] hover:-translate-y-0.5 disabled:pointer-events-none disabled:opacity-60">
                                {isLoading ? <Loader2 size={20} className="animate-spin" /> : (authMode === 'login' ? 'Sign In' : 'Create Account')}
                            </button>

                            <button
                                type="button"
                                onClick={() => { setAuthMode(authMode === 'login' ? 'register' : 'login'); setAuthError(''); }}
                                className="mt-4 w-full cursor-pointer border-none bg-transparent text-[0.9rem] font-semibold text-[var(--text-secondary)] transition-colors duration-200 hover:text-accent"
                            >
                                {authMode === 'login' ? "Don't have an account? Sign Up" : "Already have an account? Sign In"}
                            </button>
                        </form>
                    </div>
                ) : (
                    <>
                        <div className="mb-8 flex flex-wrap items-center justify-between gap-6 rounded-[28px] border border-[var(--border-color)] bg-gradient-to-br from-[var(--bg-surface)] to-[var(--bg-secondary)] p-10 shadow-[var(--shadow-md)]">
                            <div className="flex items-center gap-6">
                                <div className="flex h-20 w-20 shrink-0 items-center justify-center rounded-full text-white shadow-[0_8px_24px_rgba(198,168,124,0.3)]"
                                    style={{ background: 'linear-gradient(135deg, var(--accent-primary), var(--accent-hover))' }}
                                >
                                    <User size={40} />
                                </div>
                                <div>
                                    <h1 className="mb-1 text-[1.8rem] font-extrabold tracking-tight text-[var(--text-primary)]">
                                        {user.name || 'Quran Student'}
                                    </h1>
                                    <p className="text-[1.05rem] font-medium text-[var(--text-muted)]">{user.email}</p>
                                </div>
                            </div>

                            <button onClick={handleLogout} disabled={isLoading} className="inline-flex shrink-0 cursor-pointer items-center gap-2 rounded-[14px] border-2 px-5 py-[0.85rem] font-bold transition-all duration-200 hover:bg-[var(--accent-light)] disabled:pointer-events-none disabled:opacity-50"
                                style={{ borderColor: 'rgba(239, 68, 68, 0.4)', color: 'rgb(239, 68, 68)' }}
                            >
                                <LogOut size={18} /> Logout
                            </button>
                        </div>

                        <div className="mb-4 flex items-center justify-between gap-6 rounded-[20px] border border-black/4 bg-[var(--bg-primary)] p-6 shadow-[0_4px_15px_rgba(0,0,0,0.03)]">
                            <div className="flex-1">
                                <h3 className="mb-1 text-[1.2rem] font-extrabold text-[var(--text-primary)]">Cloud Synchronization</h3>
                                <p className="text-[0.95rem] leading-[1.5] text-[var(--text-secondary)]">
                                    Keep your bookmarks, planners, and app settings synced across all your devices seamlessly.
                                </p>

                                <AnimatePresence mode="wait">
                                    {syncStatus === 'pushing' && <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} className="mt-2 flex items-center gap-1 text-[0.9rem] font-bold text-accent"><Loader2 size={16} className="animate-spin" /> Saving data securely to cloud...</motion.div>}
                                    {syncStatus === 'pulling' && <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} className="mt-2 flex items-center gap-1 text-[0.9rem] font-bold text-accent"><Loader2 size={16} className="animate-spin" /> Fetching data from cloud...</motion.div>}
                                    {syncStatus === 'success' && <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} className="mt-2 text-[0.9rem] font-bold text-green-500">Sync completed successfully!</motion.div>}
                                    {syncStatus === 'error' && <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} className="mt-2 text-[0.9rem] font-bold text-red-500">Failed to sync. Please try again later.</motion.div>}
                                </AnimatePresence>
                            </div>
                            <div className="flex gap-3 max-sm:w-full max-sm:*:flex-1 max-sm:*:justify-center">
                                <button onClick={handlePullSync} disabled={!!syncStatus} className="inline-flex cursor-pointer items-center gap-2 rounded-[14px] border-2 border-accent bg-transparent px-5 py-[0.85rem] font-bold text-accent transition-all duration-200 hover:bg-[var(--accent-light)] disabled:pointer-events-none disabled:opacity-50">
                                    <CloudDownload size={20} /> Restore
                                </button>
                                <button onClick={handlePushSync} disabled={!!syncStatus} className="inline-flex cursor-pointer items-center gap-2 rounded-[14px] border-none bg-accent px-5 py-[0.85rem] font-bold text-white transition-all duration-200 hover:bg-[var(--accent-hover)] disabled:pointer-events-none disabled:opacity-50">
                                    <CloudUpload size={20} /> Backup
                                </button>
                            </div>
                        </div>
                    </>
                )}

                <div className="mb-10 grid grid-cols-1 gap-6 sm:grid-cols-3">
                    {[
                        { icon: Bookmark, label: 'Bookmarks', count: bookmarks.length },
                        { icon: Folder, label: 'Collections', count: collections.length },
                        { icon: Download, label: 'Downloads', count: downloadedSurahs.length },
                    ].map((item, i) => (
                        <div key={i} className="flex flex-col items-center justify-center gap-2 rounded-[24px] border border-[var(--border-color)] bg-[var(--bg-secondary)] p-6 shadow-[var(--shadow-sm)]">
                            <item.icon size={26} className="mb-1 text-accent" />
                            <span className="text-[1.6rem] font-extrabold text-[var(--text-primary)]">{item.count}</span>
                            <span className="text-[0.8rem] font-bold uppercase tracking-[0.5px] text-[var(--text-muted)]">{item.label}</span>
                        </div>
                    ))}
                </div>

                <h2 className="mb-5 pl-2 text-[1.15rem] font-extrabold uppercase tracking-[0.05em] text-[var(--text-secondary)]">
                    Preferences
                </h2>

                <div className="overflow-hidden rounded-[24px] border border-[var(--border-color)] bg-[var(--bg-secondary)] shadow-[var(--shadow-sm)]">
                    <button onClick={toggleTheme} className="flex w-full cursor-pointer items-center justify-between border-b border-[var(--border-color)] bg-transparent px-7 py-5 text-left text-[var(--text-primary)] no-underline transition-colors duration-200 hover:bg-[var(--bg-primary)] last:border-b-0">
                        <div className="flex items-center gap-4">
                            <div className="flex h-12 w-12 items-center justify-center rounded-[14px] bg-[var(--accent-light)] text-accent">
                                {theme === 'light' ? <Moon size={24} /> : <Sun size={24} />}
                            </div>
                            <span className="text-[1.1rem] font-bold">Appearance</span>
                        </div>
                        <div className="flex items-center gap-2 font-semibold text-[var(--text-muted)]">
                            {theme === 'light' ? 'Light' : 'Dark'}
                            <ChevronRight size={20} />
                        </div>
                    </button>

                    <button onClick={() => setIsSettingsOpen(true)} className="flex w-full cursor-pointer items-center justify-between border-b border-[var(--border-color)] bg-transparent px-7 py-5 text-left text-[var(--text-primary)] no-underline transition-colors duration-200 hover:bg-[var(--bg-primary)] last:border-b-0">
                        <div className="flex items-center gap-4">
                            <div className="flex h-12 w-12 items-center justify-center rounded-[14px] bg-[var(--accent-light)] text-accent">
                                <Settings size={24} />
                            </div>
                            <span className="text-[1.1rem] font-bold">Reading Settings</span>
                        </div>
                        <ChevronRight size={20} className="text-[var(--text-muted)]" />
                    </button>

                    <Link to="/offline-library" className="flex w-full cursor-pointer items-center justify-between border-b border-[var(--border-color)] bg-transparent px-7 py-5 text-left text-[var(--text-primary)] no-underline transition-colors duration-200 hover:bg-[var(--bg-primary)] last:border-b-0">
                        <div className="flex items-center gap-4">
                            <div className="flex h-12 w-12 items-center justify-center rounded-[14px] bg-[var(--accent-light)] text-accent">
                                <HardDrive size={24} />
                            </div>
                            <span className="text-[1.1rem] font-bold">Offline Library</span>
                        </div>
                        <ChevronRight size={20} className="text-[var(--text-muted)]" />
                    </Link>
                </div>

            </motion.div>
        </div>
    );
}
