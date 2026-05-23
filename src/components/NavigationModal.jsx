import React, { useState, useEffect, useMemo } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { useQuery } from '@tanstack/react-query';
import { useNavigate, useLocation } from 'react-router-dom';
import { X, Search, BookOpen, Layers, Hash } from 'lucide-react';
import { getChapters } from '../services/api/quranApi';

export default function NavigationModal({ isOpen, onClose }) {
    const navigate = useNavigate();
    const location = useLocation();

    const match = location.pathname.match(/\/(surah|memorize|page)\/(\d+)/);
    const contextType = match && match[1] !== 'page' ? match[1] : 'surah';
    const currentSurahId = match && match[1] !== 'page' ? parseInt(match[2], 10) : 1;
    const currentPageId = match && match[1] === 'page' ? parseInt(match[2], 10) : '';

    const [activeTab, setActiveTab] = useState('surah');
    const [surahSearch, setSurahSearch] = useState('');
    const [selectedSurahForAyah, setSelectedSurahForAyah] = useState(currentSurahId);
    const [ayahNumber, setAyahNumber] = useState('');
    const [pageNumber, setPageNumber] = useState(currentPageId);

    useEffect(() => {
        if (isOpen) {
            setSelectedSurahForAyah(currentSurahId);
            setAyahNumber('');
            setPageNumber(currentPageId);
            setSurahSearch('');
            setActiveTab(match && match[1] === 'page' ? 'page' : 'surah');
        }
    }, [isOpen, currentSurahId]);

    const { data: chapters = [] } = useQuery({
        queryKey: ['chapters'],
        queryFn: getChapters,
        staleTime: Infinity,
        enabled: isOpen
    });

    const filteredSurahs = useMemo(() => {
        return chapters.filter(c =>
            c.name_simple.toLowerCase().includes(surahSearch.toLowerCase()) ||
            c.name_arabic.includes(surahSearch) ||
            c.id.toString() === surahSearch
        );
    }, [chapters, surahSearch]);

    const handleSurahClick = (id) => {
        navigate(`/${contextType}/${id}`);
        onClose();
    };

    const handleAyahSubmit = (e) => {
        e.preventDefault();
        if (!selectedSurahForAyah || !ayahNumber) return;
        const chap = chapters.find(c => c.id === parseInt(selectedSurahForAyah));
        const maxAyahs = chap?.verses_count || 286;
        const validAyah = Math.min(Math.max(1, parseInt(ayahNumber) || 1), maxAyahs);
        navigate(`/${contextType}/${selectedSurahForAyah}?verse=${selectedSurahForAyah}:${validAyah}`);
        onClose();
    };

    const handlePageSubmit = (e) => {
        e.preventDefault();
        const validPage = Math.min(Math.max(1, parseInt(pageNumber) || 1), 604);
        navigate(`/page/${validPage}`);
        onClose();
    };

    return (
        <AnimatePresence>
            {isOpen && (
                <>
                    <motion.div
                        initial={{ opacity: 0 }}
                        animate={{ opacity: 1 }}
                        exit={{ opacity: 0 }}
                        onClick={onClose}
                        className="fixed inset-0 z-[2000] bg-black/50 backdrop-blur-sm"
                    />
                    <motion.div
                        initial={{ opacity: 0, scale: 0.95, y: -20, x: '-50%' }}
                        animate={{ opacity: 1, scale: 1, y: '-50%', x: '-50%' }}
                        exit={{ opacity: 0, scale: 0.95, y: -20, x: '-50%' }}
                        transition={{ type: 'spring', damping: 25, stiffness: 300 }}
                        className="fixed top-1/2 left-1/2 z-[2001] flex max-h-[85vh] w-[90%] max-w-[400px] flex-col rounded-[24px] bg-[var(--bg-surface)] shadow-[0_20px_40px_rgba(0,0,0,0.2),0_0_0_1px_var(--border-color)]"
                    >
                        <div className="flex items-center justify-between border-b border-[var(--border-color)] px-4 py-4">
                            <h3 className="m-0 text-[1.2rem] font-semibold text-[var(--text-primary)]">Navigation</h3>
                            <button onClick={onClose} className="flex h-10 w-10 items-center justify-center rounded-full text-[var(--text-secondary)] transition-all duration-200 hover:bg-[var(--bg-secondary)] hover:text-accent hover:shadow-[var(--shadow-sm)]">
                                <X size={20} />
                            </button>
                        </div>

                        <div className="relative flex border-b border-[var(--border-color)]">
                            {['surah', 'ayah', 'page'].map((tab) => (
                                <button
                                    key={tab}
                                    onClick={() => setActiveTab(tab)}
                                    className={`flex flex-1 cursor-pointer items-center justify-center gap-[6px] border-none bg-transparent px-3 py-3 text-[0.95rem] ${
                                        activeTab === tab
                                            ? 'font-semibold text-accent'
                                            : 'font-medium text-[var(--text-muted)]'
                                    }`}
                                >
                                    {tab === 'surah' && <BookOpen size={16} />}
                                    {tab === 'ayah' && <Hash size={16} />}
                                    {tab === 'page' && <Layers size={16} />}
                                    {tab.charAt(0).toUpperCase() + tab.slice(1)}
                                </button>
                            ))}
                            <motion.div
                                animate={{
                                    left: activeTab === 'surah' ? '0%' : activeTab === 'ayah' ? '33.33%' : '66.66%'
                                }}
                                transition={{ type: 'spring', stiffness: 300, damping: 30 }}
                                className="absolute bottom-0 h-[2px] w-1/3 rounded-[2px_2px_0_0] bg-accent"
                            />
                        </div>

                        <div className="min-h-[300px] flex-1 overflow-y-auto p-5">
                            {activeTab === 'surah' && (
                                <motion.div initial={{ opacity: 0, x: -10 }} animate={{ opacity: 1, x: 0 }}>
                                    <div className="relative mb-4">
                                        <Search size={18} className="absolute left-4 top-1/2 -translate-y-1/2 text-[var(--text-muted)]" />
                                        <input
                                            type="text"
                                            placeholder="Search Surah by name or number..."
                                            value={surahSearch}
                                            onChange={(e) => setSurahSearch(e.target.value)}
                                            autoFocus
                                            className="w-full rounded-xl border border-[var(--border-color)] bg-[var(--bg-secondary)] px-4 py-3 pl-[2.8rem] text-[0.95rem] text-[var(--text-primary)] outline-none"
                                        />
                                    </div>
                                    <div className="flex flex-col gap-1">
                                        {filteredSurahs.map(surah => (
                                            <button
                                                key={surah.id}
                                                onClick={() => handleSurahClick(surah.id)}
                                                className={`flex cursor-pointer items-center justify-between rounded-xl border-none px-4 py-3 text-left transition-all duration-200 ${
                                                    surah.id === currentSurahId
                                                        ? 'bg-[var(--accent-light)] text-accent'
                                                        : 'bg-transparent text-[var(--text-primary)] hover:bg-[var(--bg-secondary)]'
                                                }`}
                                            >
                                                <div className="flex items-center gap-3">
                                                    <div className={`flex h-7 w-7 items-center justify-center rounded-full text-[0.8rem] font-semibold ${
                                                        surah.id === currentSurahId
                                                            ? 'bg-accent text-white'
                                                            : 'bg-[var(--bg-tertiary)]'
                                                    }`}>
                                                        {surah.id}
                                                    </div>
                                                    <span className="font-medium">{surah.name_simple}</span>
                                                </div>
                                                <span style={{ fontFamily: 'var(--font-arabic)', fontSize: '1.2rem' }}>{surah.name_arabic}</span>
                                            </button>
                                        ))}
                                        {filteredSurahs.length === 0 && (
                                            <div className="py-8 text-center text-[var(--text-muted)]">No Surah found.</div>
                                        )}
                                    </div>
                                </motion.div>
                            )}

                            {activeTab === 'ayah' && (
                                <motion.form initial={{ opacity: 0, x: -10 }} animate={{ opacity: 1, x: 0 }} onSubmit={handleAyahSubmit} className="flex flex-col gap-4">
                                    <div>
                                        <label className="mb-2 block text-[0.9rem] text-[var(--text-muted)]">Select Surah</label>
                                        <select
                                            value={selectedSurahForAyah}
                                            onChange={(e) => setSelectedSurahForAyah(e.target.value)}
                                            className="w-full rounded-xl border border-[var(--border-color)] bg-[var(--bg-secondary)] px-4 py-3 text-[0.95rem] text-[var(--text-primary)] outline-none"
                                        >
                                            {chapters.map(surah => (
                                                <option key={surah.id} value={surah.id}>
                                                    {surah.id}. {surah.name_simple}
                                                </option>
                                            ))}
                                        </select>
                                    </div>
                                    <div>
                                        <label className="mb-2 block text-[0.9rem] text-[var(--text-muted)]">Ayah Number</label>
                                        <input
                                            type="number"
                                            value={ayahNumber}
                                            onChange={(e) => setAyahNumber(e.target.value)}
                                            placeholder="e.g. 255"
                                            min="1"
                                            max={chapters.find(c => c.id === parseInt(selectedSurahForAyah))?.verses_count || 286}
                                            required
                                            autoFocus
                                            className="w-full rounded-xl border border-[var(--border-color)] bg-[var(--bg-secondary)] px-4 py-3 text-[0.95rem] text-[var(--text-primary)] outline-none"
                                        />
                                        <div className="mt-2 text-[0.8rem] text-[var(--text-muted)]">
                                            Verses 1 - {chapters.find(c => c.id === parseInt(selectedSurahForAyah))?.verses_count || '?'}
                                        </div>
                                    </div>
                                    <button
                                        type="submit"
                                        className="mt-2 cursor-pointer rounded-xl border-none bg-accent px-4 py-3 text-base font-semibold text-white transition-all duration-200 hover:bg-[var(--accent-hover)]"
                                    >
                                        Go to Ayah
                                    </button>
                                </motion.form>
                            )}

                            {activeTab === 'page' && (
                                <motion.form initial={{ opacity: 0, x: -10 }} animate={{ opacity: 1, x: 0 }} onSubmit={handlePageSubmit} className="flex flex-col gap-4">
                                    <div>
                                        <label className="mb-2 block text-[0.9rem] text-[var(--text-muted)]">Mushaf Page</label>
                                        <input
                                            type="number"
                                            value={pageNumber}
                                            onChange={(e) => setPageNumber(e.target.value)}
                                            placeholder="e.g. 1 (1 to 604)"
                                            min="1"
                                            max="604"
                                            required
                                            autoFocus
                                            className="w-full rounded-xl border border-[var(--border-color)] bg-[var(--bg-secondary)] px-4 py-3 text-[0.95rem] text-[var(--text-primary)] outline-none"
                                        />
                                    </div>
                                    <button
                                        type="submit"
                                        className="mt-2 cursor-pointer rounded-xl border-none bg-accent px-4 py-3 text-base font-semibold text-white transition-all duration-200 hover:bg-[var(--accent-hover)]"
                                    >
                                        Go to Page
                                    </button>
                                </motion.form>
                            )}
                        </div>
                    </motion.div>
                </>
            )}
        </AnimatePresence>
    );
}
