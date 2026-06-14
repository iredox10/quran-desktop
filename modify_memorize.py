import re

with open("src/pages/MemorizeIndex.jsx", "r") as f:
    content = f.read()

new_ui = """
        <div className="mx-auto max-w-[1200px] px-4 pb-20">
            <Helmet>
                <title>Hifdh — Memorization Tracker</title>
                <meta name="description" content="Track your Quran memorization journey. Select a Surah for Hifdh." />
            </Helmet>

            <motion.div initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.4 }}>
                <div className="mb-6 text-center pt-6">
                    <p className="mb-1 font-mono text-[0.65rem] uppercase tracking-[0.14em] text-[var(--h-ink-muted)]">Memorization Hub</p>
                    <h1 className="mb-6 font-ui text-[2rem] font-bold text-[var(--h-ink)]">Hifdh Tracker</h1>

                    {lastSessionChapter && (
                        <Link to={`/memorize/${lastSessionChapter.id}`} className="group mx-auto flex max-w-[460px] items-center gap-4 rounded-[20px] bg-gradient-to-br from-[var(--h-teal)] to-[var(--h-teal-mid)] p-4 text-white no-underline transition-all duration-300 hover:-translate-y-1 hover:shadow-[0_12px_32px_rgba(46,79,74,0.25)] md:p-5 relative overflow-hidden">
                            <div className="absolute -right-6 -top-6 h-32 w-32 rounded-full bg-white/5 blur-2xl transition-transform duration-500 group-hover:scale-150" />
                            <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-xl bg-white/15 backdrop-blur-sm"><BookOpen size={24} /></div>
                            <div className="min-w-0 flex-1 text-left relative z-10">
                                <div className="mb-[0.15rem] font-ui text-[1.1rem] font-semibold">Continue: {lastSessionChapter.name_simple}</div>
                                <div className="font-mono text-[0.72rem] tracking-[0.02em] opacity-80">Last practiced {timeSince}</div>
                            </div>
                            <button onClick={(e) => handlePlayAudio(e, lastSessionChapter.id)} className="relative z-10 flex h-10 w-10 items-center justify-center rounded-full bg-white/20 text-white transition-all hover:bg-white/30 backdrop-blur-md hover:scale-110 active:scale-95">
                                <Play size={18} fill="currentColor" style={{ marginLeft: '2px' }} />
                            </button>
                        </Link>
                    )}
                </div>

                <div className="mb-8 flex gap-3">
                    <div className="group flex-1 cursor-pointer rounded-[20px] border-[1.5px] border-[var(--h-bone-dark)] bg-[var(--h-cream)] p-4 text-center transition-all duration-300 hover:-translate-y-1 hover:border-[#b8924a] hover:shadow-[0_12px_32px_rgba(184,146,74,0.12)]" onClick={() => setShowSurahsModal(true)}>
                        <div className="font-ui text-[1.8rem] font-bold leading-[1.1] text-[var(--h-ink)] transition-colors group-hover:text-[var(--h-gold)]">{totalSurahs}<small className="text-[0.8rem] font-normal text-[var(--h-ink-muted)]">/114</small></div>
                        <div className="mt-1 font-mono text-[0.6rem] uppercase tracking-[0.12em] text-[var(--h-ink-muted)]">Surahs</div>
                    </div>
                    <div className="group flex-1 cursor-pointer rounded-[20px] border-[1.5px] border-[var(--h-bone-dark)] bg-[var(--h-cream)] p-4 text-center transition-all duration-300 hover:-translate-y-1 hover:border-[#b8924a] hover:shadow-[0_12px_32px_rgba(184,146,74,0.12)]" onClick={() => setShowAyahsModal(true)}>
                        <div className="font-ui text-[1.8rem] font-bold leading-[1.1] text-[var(--h-ink)] transition-colors group-hover:text-[var(--h-gold)]">{totalAyahs}<small className="text-[0.8rem] font-normal text-[var(--h-ink-muted)]">/6236</small></div>
                        <div className="mt-1 font-mono text-[0.6rem] uppercase tracking-[0.12em] text-[var(--h-ink-muted)]">Ayahs</div>
                    </div>
                    <div className="group flex-1 rounded-[20px] border-[1.5px] border-[var(--h-bone-dark)] bg-[var(--h-cream)] p-4 text-center transition-all duration-300 hover:-translate-y-1 hover:border-[var(--h-teal)] hover:shadow-[0_12px_32px_rgba(46,79,74,0.12)]">
                        <div className="font-ui text-[1.8rem] font-bold leading-[1.1] text-[var(--h-ink)] transition-colors group-hover:text-[var(--h-teal)]">{totalSurahs > 0 ? Math.round((totalAyahs / 6236) * 100) : 0}<small className="text-[0.8rem] font-normal text-[var(--h-ink-muted)]">%</small></div>
                        <div className="mt-1 font-mono text-[0.6rem] uppercase tracking-[0.12em] text-[var(--h-ink-muted)]">Progress</div>
                    </div>
                </div>

                <div className="mb-10 grid gap-4 grid-cols-2">
                    <button onClick={() => setShowTestModal(true)} className="group relative flex flex-col items-center justify-center gap-3 overflow-hidden rounded-[24px] bg-[var(--h-teal)] p-6 text-white shadow-[0_8px_24px_rgba(46,79,74,0.15)] transition-all duration-300 hover:-translate-y-1 hover:shadow-[0_12px_32px_rgba(46,79,74,0.3)]">
                        <div className="absolute inset-0 bg-gradient-to-br from-white/10 to-transparent opacity-0 transition-opacity duration-300 group-hover:opacity-100" />
                        <div className="flex h-14 w-14 items-center justify-center rounded-full bg-white/15 backdrop-blur-md transition-transform duration-500 ease-out group-hover:scale-110 group-hover:rotate-12">
                            <RefreshCw size={26} />
                        </div>
                        <div className="text-center relative z-10">
                            <div className="font-ui text-[1.15rem] font-bold tracking-wide">Test My Hifdh</div>
                            <div className="mt-0.5 font-mono text-[0.65rem] uppercase tracking-[0.15em] opacity-75">Review mistakes</div>
                        </div>
                    </button>
                    
                    <button onClick={() => setShowGoalModal(true)} className="group relative flex flex-col items-center justify-center gap-3 overflow-hidden rounded-[24px] border-[1.5px] border-[var(--h-bone-dark)] bg-[var(--h-white)] p-6 text-[var(--h-ink)] shadow-sm transition-all duration-300 hover:-translate-y-1 hover:border-[#b8924a] hover:bg-[var(--h-gold-soft)] hover:shadow-[0_12px_32px_rgba(184,146,74,0.15)]">
                        <div className="flex h-14 w-14 items-center justify-center rounded-full bg-[var(--h-bone)] text-[var(--h-ink-mid)] transition-all duration-500 ease-out group-hover:scale-110 group-hover:bg-[var(--h-gold)] group-hover:text-white">
                            <Target size={26} />
                        </div>
                        <div className="text-center relative z-10">
                            <div className="font-ui text-[1.15rem] font-bold tracking-wide">Set Goal</div>
                            <div className="mt-0.5 font-mono text-[0.65rem] uppercase tracking-[0.15em] text-[var(--h-ink-muted)] group-hover:text-[#a8823b] transition-colors">Plan memorization</div>
                        </div>
                    </button>
                </div>

                {needsRevision.length > 0 && (
                    <div className="mb-10 rounded-[20px] border-[1.5px] border-[#fca5a5] bg-gradient-to-br from-[#fef2f2] to-[#fee2e2] p-5 shadow-sm dark:border-[#7f1d1d] dark:from-[#450a0a] dark:to-[#220505]">
                        <div className="mb-4 flex items-center gap-2.5 font-ui text-[1.1rem] font-bold text-[#b91c1c] dark:text-[#f87171]">
                            <AlertCircle size={20} /> Needs Revision
                        </div>
                        <div className="flex gap-2.5 overflow-x-auto pb-2 [scrollbar-width:none] [-ms-overflow-style:none] [&::-webkit-scrollbar]:hidden">
                            {needsRevision.map(ayah => (
                                <Link to={`/memorize/${ayah.split(':')[0]}?verse=${ayah}`} key={ayah} className="flex-shrink-0 rounded-xl border border-[#fecaca] bg-white px-4 py-2.5 text-[0.85rem] font-semibold text-[#b91c1c] no-underline shadow-sm transition-all hover:-translate-y-px hover:border-[#f87171] hover:shadow-md dark:border-[#991b1b] dark:bg-[var(--h-cream)] dark:text-[#fca5a5]">
                                    {chapters?.find(c => c.id == ayah.split(':')[0])?.name_simple} {ayah.split(':')[1]}
                                </Link>
                            ))}
                        </div>
                    </div>
                )}

                {(hifdhGoals?.length > 0) && (
                    <div className="mb-10">
                        <div className="mb-4 flex items-center justify-between">
                            <h2 className="flex items-center gap-2 font-ui text-[1.35rem] font-bold text-[var(--h-ink)]"><Target size={20} className="text-[var(--h-gold)]" /> Active Goals</h2>
                        </div>
                        <div className="grid gap-3">
                            {hifdhGoals.map(goal => {
                                const ch = chapters?.find(c => c.id == goal.targetId);
                                const daysLeft = Math.max(0, Math.ceil((new Date(goal.targetDate) - new Date()) / (1000 * 60 * 60 * 24)));
                                const curMem = surahMemCounts[goal.targetId] || 0;
                                const total = ch?.verses_count || 0;
                                const pct = total > 0 ? Math.round((curMem / total) * 100) : 0;
                                
                                return (
                                    <div key={goal.id} className="group relative flex items-center justify-between overflow-hidden rounded-[20px] border-[1.5px] border-[var(--h-bone-dark)] bg-[var(--h-cream)] p-5 transition-all duration-300 hover:border-[#b8924a] hover:shadow-[0_8px_24px_rgba(184,146,74,0.12)]">
                                        <div className="absolute left-0 top-0 bottom-0 w-1.5 bg-[#b8924a]" />
                                        <div className="pl-2">
                                            <div className="flex items-center gap-2.5 font-ui text-[1.1rem] font-bold text-[var(--h-ink)] group-hover:text-[#b8924a] transition-colors">
                                                Memorize {ch?.name_simple || 'Target'}
                                            </div>
                                            <div className="mt-1 flex items-center gap-2 text-[0.78rem] text-[var(--h-ink-muted)]">
                                                <Calendar size={14} /> {daysLeft} days left
                                            </div>
                                        </div>
                                        <div className="flex items-center gap-4">
                                            <div className="text-right hidden sm:block">
                                                <div className="text-[0.95rem] font-bold text-[var(--h-ink)]">{curMem} <span className="text-[0.75rem] font-normal text-[var(--h-ink-muted)]">/ {total}</span></div>
                                                <div className="font-mono text-[0.6rem] uppercase tracking-wider text-[var(--h-ink-muted)]">Ayahs</div>
                                            </div>
                                            <div className="relative flex h-12 w-12 items-center justify-center rounded-full bg-[var(--h-bone)]">
                                                <svg className="absolute inset-0 h-full w-full -rotate-90 transform" viewBox="0 0 36 36">
                                                    <path className="text-[var(--h-bone-dark)]" d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831" fill="none" stroke="currentColor" strokeWidth="3" />
                                                    <path className="text-[#b8924a] transition-all duration-1000 ease-out" strokeDasharray={`${pct}, 100`} d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831" fill="none" stroke="currentColor" strokeWidth="3" />
                                                </svg>
                                                <span className="font-mono text-[0.6rem] font-bold text-[var(--h-ink)]">{pct}%</span>
                                            </div>
                                        </div>
                                    </div>
                                );
                            })}
                        </div>
                    </div>
                )}

                <div className="mb-5 flex items-center justify-between">
                    <div className="flex rounded-xl bg-[var(--h-bone)] p-1">
                        <button 
                            className={`rounded-lg px-5 py-2 text-[0.85rem] font-semibold transition-all ${viewMode === 'surah' ? 'bg-[var(--h-white)] text-[var(--h-ink)] shadow-sm' : 'text-[var(--h-ink-muted)] hover:text-[var(--h-ink)]'}`}
                            onClick={() => setViewMode('surah')}
                        >
                            Surahs
                        </button>
                        <button 
                            className={`rounded-lg px-5 py-2 text-[0.85rem] font-semibold transition-all ${viewMode === 'juz' ? 'bg-[var(--h-white)] text-[var(--h-ink)] shadow-sm' : 'text-[var(--h-ink-muted)] hover:text-[var(--h-ink)]'}`}
                            onClick={() => setViewMode('juz')}
                        >
                            Ajzaa (Juz)
                        </button>
                    </div>
                </div>

                <div className="mb-6 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
                    <div className="flex flex-1 items-center gap-3 rounded-[16px] border-[1.5px] border-[var(--h-bone-dark)] bg-[var(--h-cream)] px-[1.15rem] py-[0.85rem] transition-colors duration-200 focus-within:border-[var(--h-teal)] md:px-5">
                        <Search size={18} className="shrink-0 text-[var(--h-ink-muted)]" />
                        <input type="text" placeholder="Search..." value={searchQuery}
                            onChange={e => setSearchQuery(e.target.value)}
                            className="w-full border-none bg-transparent font-[inherit] text-[0.95rem] text-[var(--h-ink)] outline-none placeholder:text-[var(--h-ink-muted)]" />
                    </div>

                    <button
                        className={`flex h-full shrink-0 cursor-pointer items-center gap-2 rounded-[16px] border-[1.5px] px-5 py-[0.85rem] text-[0.85rem] font-semibold font-[inherit] transition-all duration-200 ${
                            showOnlyMemorized
                                ? 'border-[var(--h-green)] bg-[var(--h-green-soft)] text-[var(--h-green)]'
                                : 'border-[var(--h-bone-dark)] bg-[var(--h-cream)] text-[var(--h-ink-mid)] hover:bg-[var(--h-bone)]'
                        }`}
                        onClick={() => setShowOnlyMemorized(!showOnlyMemorized)}
                    >
                        <Award size={18} />
                        {showOnlyMemorized ? 'Memorized Only' : 'Filter Memorized'}
                    </button>
                </div>

                <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 md:grid-cols-3">
                    {viewMode === 'surah' ? (
                        filteredChapters?.map(chapter => {
                            const isMemorized = (memorizedSurahs || []).includes(chapter.id);
                            const memCount = surahMemCounts[String(chapter.id)] || 0;
                            const memPct = Math.round((memCount / chapter.verses_count) * 100);
                            const hasPartial = !isMemorized && memCount > 0;

                            return (
                                <motion.div key={chapter.id} whileHover={{ scale: 1.02 }} whileTap={{ scale: 0.98 }}>
                                    <Link to={`/memorize/${chapter.id}`} className={`group flex items-center gap-4 overflow-hidden rounded-[20px] border-[1.5px] bg-[var(--h-cream)] shadow-sm p-4 no-underline text-inherit transition-all duration-300 hover:-translate-y-1 hover:shadow-[0_8px_24px_rgba(46,79,74,0.12)] ${
                                        isMemorized ? 'border-[var(--h-green)]' : 'border-[var(--h-bone-dark)] hover:border-[var(--h-teal)]'
                                    }`}>
                                        <div className={`flex h-[42px] w-[42px] shrink-0 items-center justify-center rounded-xl font-mono text-[0.85rem] font-bold transition-colors ${
                                            isMemorized ? 'bg-[var(--h-green-soft)] text-[var(--h-green)]' : 'bg-[var(--h-bone)] text-[var(--h-ink-mid)] group-hover:bg-[var(--h-teal-soft)] group-hover:text-[var(--h-teal)]'
                                        }`}>{chapter.id}</div>
                                        <div className="min-w-0 flex-1">
                                            <div className="flex items-center gap-[0.35rem] font-ui text-[1.05rem] font-semibold text-[var(--h-ink)]">
                                                {chapter.name_simple}
                                                {isMemorized && <CheckCircle size={15} className="shrink-0 text-[var(--h-green)]" />}
                                            </div>
                                            <div className="mt-[0.2rem] flex items-center justify-between text-[0.72rem] text-[var(--h-ink-muted)]">
                                                <span>{chapter.verses_count} Ayahs</span>
                                                {hasPartial && (
                                                    <span className="rounded-[8px] bg-[var(--h-gold-soft)] px-2 py-0.5 text-[0.62rem] font-bold text-[#b8924a]">{memCount} done</span>
                                                )}
                                            </div>
                                            {(isMemorized || hasPartial) && (
                                                <div className="mt-2.5 h-1 overflow-hidden rounded-full bg-[var(--h-bone)]">
                                                    <div className="h-full rounded-full transition-all duration-[0.6s] ease-out"
                                                        style={{ width: `${isMemorized ? 100 : memPct}%`, background: isMemorized ? 'var(--h-green)' : 'var(--h-teal)' }} />
                                                </div>
                                            )}
                                        </div>
                                        <button onClick={(e) => handlePlayAudio(e, chapter.id)} className={`flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-[var(--h-bone)] text-[var(--h-ink-muted)] transition-all duration-300 hover:scale-110 active:scale-95 ${isMemorized ? 'hover:bg-[var(--h-green)] hover:text-white' : 'hover:bg-[var(--h-teal)] hover:text-white'}`}>
                                            <Play size={16} fill="currentColor" style={{ marginLeft: '2px' }} />
                                        </button>
                                    </Link>
                                </motion.div>
                            );
                        })
                    ) : (
                        juzs?.map(juz => {
                            let totalJuzAyahs = 0;
                            let memJuzAyahs = 0;
                            
                            if (juz.verse_mapping) {
                                Object.keys(juz.verse_mapping).forEach(sId => {
                                    const range = juz.verse_mapping[sId].split('-');
                                    const start = Number(range[0]);
                                    const end = Number(range[1]);
                                    const count = end - start + 1;
                                    totalJuzAyahs += count;

                                    if (memorizedAyahsGrouped[sId]) {
                                        const memorizedInRange = memorizedAyahsGrouped[sId].filter(a => a >= start && a <= end).length;
                                        memJuzAyahs += memorizedInRange;
                                    }
                                });
                            }

                            const memPct = totalJuzAyahs > 0 ? Math.round((memJuzAyahs / totalJuzAyahs) * 100) : 0;
                            const isMemorized = memPct === 100;
                            
                            if (showOnlyMemorized && !isMemorized) return null;

                            return (
                                <motion.div key={juz.id} whileHover={{ scale: 1.02 }} whileTap={{ scale: 0.98 }}>
                                    <div className={`group flex items-center gap-4 overflow-hidden rounded-[20px] border-[1.5px] bg-[var(--h-cream)] shadow-sm p-4 text-inherit transition-all duration-300 hover:-translate-y-1 hover:shadow-[0_8px_24px_rgba(46,79,74,0.12)] ${
                                        isMemorized ? 'border-[var(--h-green)]' : 'border-[var(--h-bone-dark)] hover:border-[var(--h-teal)]'
                                    }`}>
                                        <div className={`flex h-[42px] w-[42px] shrink-0 items-center justify-center rounded-xl font-mono text-[0.85rem] font-bold transition-colors ${
                                            isMemorized ? 'bg-[var(--h-green-soft)] text-[var(--h-green)]' : 'bg-[var(--h-bone)] text-[var(--h-ink-mid)] group-hover:bg-[var(--h-teal-soft)] group-hover:text-[var(--h-teal)]'
                                        }`}>{juz.juz_number}</div>
                                        <div className="min-w-0 flex-1">
                                            <div className="flex items-center gap-[0.35rem] font-ui text-[1.05rem] font-semibold text-[var(--h-ink)]">
                                                Juz {juz.juz_number}
                                                {isMemorized && <CheckCircle size={15} className="shrink-0 text-[var(--h-green)]" />}
                                            </div>
                                            <div className="mt-[0.2rem] flex items-center justify-between text-[0.72rem] text-[var(--h-ink-muted)]">
                                                <span>{memJuzAyahs} / {totalJuzAyahs} Ayahs</span>
                                                <span className="font-mono font-bold text-[var(--h-ink-mid)]">{memPct}%</span>
                                            </div>
                                            <div className="mt-2.5 h-1 overflow-hidden rounded-full bg-[var(--h-bone)]">
                                                <div className="h-full rounded-full transition-all duration-[0.6s] ease-out"
                                                    style={{ width: `${memPct}%`, background: isMemorized ? 'var(--h-green)' : 'var(--h-teal)' }} />
                                            </div>
                                        </div>
                                    </div>
                                </motion.div>
                            );
                        })
                    )}
                </div>

            </motion.div>

            <AnimatePresence>
                {showTestModal && <HifdhTestModal onClose={() => setShowTestModal(false)} />}
                
                {showGoalModal && (
                    <motion.div className="fixed inset-0 z-[1000] flex items-center justify-center bg-[var(--h-ink)]/40 p-4 backdrop-blur-sm"
                        initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}
                        onClick={e => { if (e.target === e.currentTarget) setShowGoalModal(false); }}
                    >
                        <motion.div className="flex w-full max-w-[400px] flex-col overflow-hidden rounded-[24px] bg-[var(--h-cream)] p-7 shadow-2xl border-[1px] border-[var(--glass-border)]"
                            initial={{ opacity: 0, y: 30, scale: 0.96 }} animate={{ opacity: 1, y: 0, scale: 1 }} exit={{ opacity: 0, y: 20, scale: 0.96 }}
                        >
                            <h3 className="mb-5 font-ui text-2xl font-bold text-[var(--h-ink)]">Set Hifdh Goal</h3>
                            <div className="mb-5">
                                <label className="mb-2 block text-[0.8rem] uppercase tracking-wider font-mono font-semibold text-[var(--h-ink-muted)]">Target Surah</label>
                                <select 
                                    className="w-full rounded-xl border-[1.5px] border-[var(--h-bone-dark)] bg-[var(--h-white)] px-4 py-3.5 text-[var(--h-ink)] outline-none focus:border-[var(--h-gold)] transition-colors"
                                    value={goalTarget} onChange={e => setGoalTarget(e.target.value)}
                                >
                                    <option value="">Select Surah...</option>
                                    {chapters?.map(c => <option key={c.id} value={c.id}>{c.id}. {c.name_simple}</option>)}
                                </select>
                            </div>
                            <div className="mb-8">
                                <label className="mb-2 block text-[0.8rem] uppercase tracking-wider font-mono font-semibold text-[var(--h-ink-muted)]">Target Date</label>
                                <input 
                                    type="date" 
                                    className="w-full rounded-xl border-[1.5px] border-[var(--h-bone-dark)] bg-[var(--h-white)] px-4 py-3.5 text-[var(--h-ink)] outline-none focus:border-[var(--h-gold)] transition-colors"
                                    value={goalDate} onChange={e => setGoalDate(e.target.value)}
                                    min={new Date().toISOString().split('T')[0]}
                                />
                            </div>
                            <button onClick={handleCreateGoal} className="w-full rounded-xl bg-gradient-to-r from-[var(--h-gold)] to-[#a8823b] py-3.5 font-bold text-white transition-all hover:scale-[1.02] hover:shadow-[0_8px_20px_rgba(184,146,74,0.3)] active:scale-95">
                                Create Goal
                            </button>
                        </motion.div>
                    </motion.div>
                )}
            </AnimatePresence>
        </div>
"""

# Find where the return statement starts
match = re.search(r'return\s*\(\s*<div className="mx-auto max-w-\[1200px\]', content)
if not match:
    print("Could not find return statement")
    exit(1)

start_idx = match.start()
end_idx = content.rfind(');')

new_content = content[:start_idx] + "return (\n" + new_ui + "    );" + content[end_idx+2:]

with open("src/pages/MemorizeIndex.jsx", "w") as f:
    f.write(new_content)

print("Successfully updated MemorizeIndex.jsx")
