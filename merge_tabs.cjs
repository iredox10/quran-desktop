const fs = require('fs');
const filePath = '/home/iredox/Desktop/personal-apps/quran-app/src/pages/Planner.jsx';
let content = fs.readFileSync(filePath, 'utf8');

const startIdx = content.indexOf('function ActiveView({ planner,');
if (startIdx === -1) throw new Error('Could not find ActiveView start');

const endIdx = content.indexOf('function ActiveViewWrapper', startIdx);
if (endIdx === -1) throw new Error('Could not find ActiveView end');

const before = content.slice(0, startIdx);
const after = content.slice(endIdx);

const newActiveView = `function ActiveView({ planner, planners, activePlannerId, onSwitchPlan, onDelete, setPlannerAssignmentProgress, togglePlannerDayComplete, chapters }) {
    const { prayerTimes, setPrayerTimes, location, setLocation, shiftPlannerSchedule, setPlanner, prayerSettings, plannerReflections, plannerBookmarks } = useAppStore();
    const overview = useMemo(() => getPlannerOverview(planner), [planner]);
    const metrics = useMemo(() => getPlannerSuccessMetrics(planner), [planner]);
    const difficulty = useMemo(() => getDifficultyIndicators(planner?.assignments), [planner]) || {};
    const analytics = useMemo(() => getPlannerAnalytics(planner), [planner]);
    const weeklySummary = useMemo(() => getWeeklySummary(planner), [planner]);
    const today = formatPlannerDate(new Date());

    const [showAdjustPace, setShowAdjustPace] = useState(false);
    const [newDuration, setNewDuration] = useState(planner.durationDays);
    const [showSettings, setShowSettings] = useState(false);
    const [activeTab, setActiveTab] = useState('today');

    useEffect(() => {
        if (location && (!prayerTimes || prayerTimes.date !== today)) {
            fetch(\`https://api.aladhan.com/v1/timings?latitude=\${location.lat}&longitude=\${location.lng}&method=2\`)
                .then(r => r.json())
                .then(data => {
                    if (data.code === 200) {
                        setPrayerTimes({ date: today, timings: data.data.timings });
                    }
                })
                .catch(e => console.error("Prayer API error", e));
        }
    }, [location, prayerTimes, today, setPrayerTimes]);

    const requestLocation = () => {
        if (navigator.geolocation) {
            navigator.geolocation.getCurrentPosition(
                (pos) => setLocation({ lat: pos.coords.latitude, lng: pos.coords.longitude }),
                (err) => alert("Could not access location for prayer times.")
            );
        }
    };

    const handleExportCalendar = () => {
        if (!planner) return;
        let icsContent = "BEGIN:VCALENDAR\\nVERSION:2.0\\nPRODID:-//QuranApp//Planner//EN\\n";
        planner.assignments.forEach(a => {
            const dateStr = a.date.replace(/-/g, '');
            icsContent += \`BEGIN:VEVENT\\nDTSTART;VALUE=DATE:\${dateStr}\\nDTEND;VALUE=DATE:\${dateStr}\\nSUMMARY:Quran Plan - Day \${a.dayNumber}\\nDESCRIPTION:Read \${a.items.length} \${PLANNER_UNITS[planner.unitType]?.plural}\\nEND:VEVENT\\n\`;
        });
        icsContent += "END:VCALENDAR";
        const blob = new Blob([icsContent], { type: 'text/calendar;charset=utf-8' });
        const url = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = \`quran_plan.ics\`;
        link.click();
    };

    const todayAssignment = useMemo(() => {
        if (!planner) return null;
        return planner.assignments.find(a => a.date === today) || planner.assignments[overview?.currentDayNumber - 1] || null;
    }, [planner, today, overview]);

    const prayerSlots = useMemo(() => buildPrayerSlots(planner, todayAssignment, prayerTimes, prayerSettings), [planner, todayAssignment, prayerTimes, prayerSettings]);

    const unitsLabel = planner ? PLANNER_UNITS[planner.unitType]?.plural : 'Pages';
    const completionDate = planner ? new Date(\`\${addDays(planner.startDate, planner.durationDays - 1)}T00:00:00\`)
        .toLocaleDateString('en-US', { month: 'short', day: 'numeric' }) : '—';
    const streakDays = metrics?.consistencyStreak ?? 0;
    const currentDay = overview?.currentDayNumber ?? 1;

    const todayProgress = useMemo(() => todayAssignment ? getAssignmentProgress(planner, todayAssignment) : null, [planner, todayAssignment]);
    const todayComplete = todayProgress?.isComplete ?? false;

    const nextAssignment = useMemo(() => {
        if (!planner || !todayComplete) return null;
        return planner.assignments.find(a => {
            if (todayAssignment && a.dayNumber <= todayAssignment.dayNumber) return false;
            return !getAssignmentProgress(planner, a).isComplete;
        }) || null;
    }, [planner, todayComplete, todayAssignment]);

    const nextAssignmentProgress = nextAssignment ? getAssignmentProgress(planner, nextAssignment) : null;
    const nextReadRoute = todayComplete
        ? (nextAssignment ? \`/planner/read/\${nextAssignment.dayNumber}\` : null)
        : (todayAssignment ? \`/planner/read/\${todayAssignment.dayNumber}\` : null);

    const todayDone = todayProgress?.completedCount ?? 0;
    const todayTotal = todayProgress?.totalCount ?? 1;
    const todayPct = Math.round((todayDone / todayTotal) * 100);

    const resumeRoute = nextReadRoute;
    const hasStartedReading = todayDone > 0 || !!planner?.lastReadPage;

    const overallPct = overview ? Math.round(overview.completionRatio * 100) : 0;
    const completedDays = overview?.completedCount ?? 0;

    const handleMarkPrayer = (slot) => {
        if (!todayAssignment) return;
        const newCount = slot.completedUpTo;
        setPlannerAssignmentProgress(todayAssignment.dayNumber, newCount);
    };

    const handleUndoPrayer = (slot) => {
        if (!todayAssignment || slot.status !== 'completed') return;
        setPlannerAssignmentProgress(todayAssignment.dayNumber, slot.slotStartCount);
    };

    const handleShareProgress = async () => {
        if (!navigator.share) {
            alert('Sharing is not supported on this device/browser.');
            return;
        }
        try {
            await navigator.share({
                title: 'My Quran Reading Plan',
                text: \`I've completed \${completedDays} days of my \${planner.durationDays}-day Quran reading plan on the Quran App! I'm currently on Day \${currentDay}.\`,
                url: window.location.href,
            });
        } catch (e) {
            console.error('Share failed', e);
        }
    };

    const nextPrayer = prayerSlots.find(s => s.status === 'current' || s.status === 'upcoming');
    const daySubtitle = (() => {
        if (overview?.isFinishedWindow && !nextAssignment) return 'Plan complete! 🎉';
        if (!todayAssignment) return 'Starting soon…';
        if (todayComplete && nextAssignment) return \`Day \${currentDay} complete! 🎉 · Day \${nextAssignment.dayNumber} ready\`;
        if (todayComplete) return \`Day \${currentDay} complete! 🎉\`;
        if (todayDone > 0 && todayDone < todayTotal && nextPrayer) return \`\${todayDone} of \${todayTotal} \${unitsLabel} read · \${nextPrayer.name} next\`;
        if (nextPrayer) return \`\${todayTotal} \${unitsLabel} today · start with \${nextPrayer.name}\`;
        return 'All done for today! ✓';
    })();

    const ringLabel = \`\${todayDone} OF \${todayTotal} \${PLANNER_UNITS[planner.unitType]?.label.toUpperCase()}S TODAY\`;

    const planDone = overview?.isFinishedWindow && !nextAssignment;
    const ctaRoute = todayComplete ? (nextReadRoute || resumeRoute) : (resumeRoute || nextReadRoute);
    const ctaLabel = planDone
        ? 'Plan Complete ✓'
        : todayComplete && nextAssignment
            ? \`Continue to Day \${nextAssignment.dayNumber}\`
            : todayComplete ? 'All Caught Up'
                : hasStartedReading ? 'Resume Reading' : 'Open Al-Quran';

    return (
        <div className="relative min-h-dvh overflow-hidden font-body text-[var(--text-primary)]">
            <div className="pointer-events-none absolute inset-0 z-0" style={{
                background: \`
                    radial-gradient(ellipse 80% 55% at 50% -10%, var(--accent-light) 0%, transparent 70%),
                    radial-gradient(ellipse 90% 60% at 80% 20%, rgba(198,168,124,0.1) 0%, transparent 60%)
                \`
            }} aria-hidden="true" />
            <motion.div className="relative z-10 mx-auto flex w-full max-w-[960px] flex-col items-center px-5 pt-10 pb-24 md:px-8 md:pb-16" initial={{ opacity: 0 }} animate={{ opacity: 1 }} transition={{ duration: 0.6 }}>
                
                {/* --- GLOBAL HEADER (Always Visible) --- */}
                <div className="w-full max-w-[480px] md:max-w-[800px] mb-8">
                    <div className="mb-6 text-center md:text-left flex flex-col md:flex-row md:justify-between md:items-end">
                        <div>
                            {planners && planners.length > 1 ? (
                                <div className="mb-2 relative inline-block">
                                    <select 
                                        value={activePlannerId || ''} 
                                        onChange={(e) => onSwitchPlan(e.target.value)}
                                        className="appearance-none bg-transparent border-none font-ui text-[clamp(1.6rem,5vw,2rem)] font-semibold tracking-tight text-[var(--text-primary)] pr-8 cursor-pointer outline-none md:text-left"
                                    >
                                        {planners.map(p => (
                                            <option key={p.id} value={p.id}>{p.title || \`\${p.durationDays} Day Plan\`}</option>
                                        ))}
                                    </select>
                                    <div className="pointer-events-none absolute inset-y-0 right-0 flex items-center px-2 text-[var(--text-primary)]">
                                        <svg className="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M19 9l-7 7-7-7"></path></svg>
                                    </div>
                                </div>
                            ) : (
                                <h1 className="mb-2 font-ui text-[clamp(1.7rem,5vw,2.2rem)] font-semibold tracking-tight text-[var(--text-primary)]">{planner.title || 'Quran Plan'}</h1>
                            )}
                            <div className="font-ui text-[1.1rem] font-medium text-[var(--accent-primary)] mb-1">Day {currentDay} of {planner.durationDays}</div>
                            <p className="font-body text-[0.9rem] text-[var(--text-secondary)]">{daySubtitle}</p>
                        </div>
                        
                        <div className="mt-4 md:mt-0 md:min-w-[200px]">
                            {planDone ? (
                                <button 
                                    className="w-full cursor-pointer rounded-full border-none bg-[var(--accent-primary)] p-[0.9rem] font-ui text-[1rem] font-semibold tracking-[0.02em] text-white shadow-md transition-all duration-200 hover:bg-[var(--accent-hover)]"
                                    onClick={() => {
                                        const rev = buildRevisionPlanner(planner, chapters); 
                                        if(rev) {
                                            const s = useAppStore.getState();
                                            s.setPlanner(rev);
                                        }
                                    }}
                                >
                                    Start Revision Plan
                                </button>
                            ) : !ctaRoute ? (
                                <button className="w-full cursor-pointer rounded-full border-none bg-[var(--bg-surface)] p-[0.9rem] font-ui text-[1rem] font-semibold tracking-[0.02em] text-[var(--text-muted)] shadow-sm disabled:opacity-70" disabled>{ctaLabel}</button>
                            ) : (
                                <Link to={ctaRoute} className="inline-flex w-full items-center justify-center rounded-full bg-[var(--accent-primary)] p-[0.9rem] font-ui text-[1rem] font-semibold tracking-[0.02em] text-white no-underline shadow-md transition-all duration-200 hover:bg-[var(--accent-hover)]">{ctaLabel}</Link>
                            )}
                        </div>
                    </div>

                    {/* Stats Header Cards */}
                    <div className="flex w-full gap-[0.7rem] md:gap-4">
                        {[
                            { label: 'Overall', val: \`\${overallPct}%\` },
                            { label: 'Finish by', val: completionDate },
                            { label: 'Streak', val: \`\${streakDays} Day\${streakDays !== 1 ? 's' : ''}\` },
                        ].map(s => (
                            <div key={s.label} className="flex flex-1 flex-col gap-[0.2rem] rounded-xl bg-[var(--glass-bg)] p-3 md:p-4 border border-[var(--glass-border)] shadow-[var(--shadow-glass)] backdrop-blur-md">
                                <span className="font-body text-[0.75rem] md:text-[0.8rem] text-[var(--text-muted)]">{s.label}</span>
                                <span className="font-ui text-[1.2rem] md:text-[1.4rem] font-semibold text-[var(--text-primary)]">{s.val}</span>
                            </div>
                        ))}
                    </div>
                </div>

                {/* --- TAB NAVIGATION --- */}
                <div className="flex w-full max-w-[480px] md:max-w-[800px] gap-1 md:gap-2 mb-8 p-1.5 bg-[var(--glass-bg)] rounded-[14px] border border-[var(--glass-border)] shadow-sm backdrop-blur-sm">
                    {[ {id: 'today', label: 'Dashboard'}, {id: 'journal', label: 'Journal'} ].map(tab => (
                        <button key={tab.id} onClick={() => setActiveTab(tab.id)}
                            className={\`flex-1 py-2 md:py-2.5 px-2 md:px-4 rounded-lg font-ui text-[1rem] md:text-[1.05rem] font-semibold transition-all \${
                                activeTab === tab.id 
                                    ? 'bg-[var(--bg-primary)] shadow-sm text-[var(--accent-primary)] border border-[var(--border-color)]' 
                                    : 'text-[var(--text-muted)] hover:text-[var(--text-primary)]'
                            }\`}>
                            {tab.label}
                        </button>
                    ))}
                </div>

                {/* --- TAB CONTENTS --- */}
                <div className="w-full max-w-[480px] md:max-w-[800px]">
                    {/* --- DASHBOARD TAB (Merged Today + Journey) --- */}
                    {activeTab === 'today' && (
                        <div className="flex flex-col gap-10">
                            {/* Top Section: Daily Focus */}
                            <div className="flex flex-col md:flex-row gap-8">
                                <div className="flex-1 flex justify-center md:justify-start items-center">
                                    <RingProgress percent={todayPct} size={220} stroke={10}>
                                        <span className="font-ui text-[3rem] font-semibold leading-none text-[var(--accent-primary)]">{todayPct}%</span>
                                        <span className="font-mono text-[0.6rem] font-normal tracking-[0.1em] text-[var(--text-secondary)] mt-2">{ringLabel}</span>
                                    </RingProgress>
                                </div>

                                <div className="flex-1 w-full">
                                    <div className="mb-4 flex items-center justify-between">
                                        <h2 className="font-ui text-[1.35rem] font-semibold tracking-[0.01em] text-[var(--text-primary)]">Daily Ritual</h2>
                                        <div className="flex gap-2">
                                            {navigator.share && (
                                                <button onClick={handleShareProgress} className="flex h-8 w-8 cursor-pointer items-center justify-center rounded-full border border-[var(--glass-border)] bg-[var(--bg-surface)] text-[var(--text-secondary)] hover:bg-[var(--bg-secondary)] hover:text-[var(--text-primary)] transition-colors" title="Share Progress">
                                                    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                                                        <circle cx="18" cy="5" r="3"></circle><circle cx="6" cy="12" r="3"></circle><circle cx="18" cy="19" r="3"></circle><line x1="8.59" y1="13.51" x2="15.42" y2="17.49"></line><line x1="15.41" y1="6.51" x2="8.59" y2="10.49"></line>
                                                    </svg>
                                                </button>
                                            )}
                                            <button onClick={() => setShowSettings(true)} className="flex h-8 w-8 cursor-pointer items-center justify-center rounded-full border border-[var(--glass-border)] bg-[var(--bg-surface)] text-[var(--text-secondary)] hover:bg-[var(--bg-secondary)] hover:text-[var(--text-primary)] transition-colors" title="Settings">
                                                <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><circle cx="12" cy="12" r="3"></circle><path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z"></path></svg>
                                            </button>
                                        </div>
                                    </div>
                                    <div className="flex flex-col gap-[0.7rem]">
                                        {prayerSlots.map((slot, i) => (
                                            <motion.div key={slot.name}
                                                className={\`flex items-center gap-[0.85rem] rounded-xl px-4 py-4 shadow-sm transition-shadow \${
                                                    slot.status === 'current'
                                                        ? 'bg-[var(--bg-surface)] border border-[var(--accent-primary)] shadow-[0_4px_18px_rgba(198,168,124,0.15)]'
                                                        : 'bg-[var(--glass-bg)] border border-[var(--glass-border)]'
                                                }\`}
                                                initial={{ opacity: 0, x: -10 }} animate={{ opacity: 1, x: 0 }}
                                                transition={{ delay: 0.05 * i, duration: 0.35 }}
                                            >
                                                <div className={\`flex h-[38px] w-[38px] shrink-0 items-center justify-center rounded-full transition-colors duration-200 text-white \${
                                                    slot.status === 'completed' ? 'bg-[#10b981]' : slot.status === 'current' ? 'bg-[var(--accent-primary)]' : 'bg-[var(--bg-secondary)] text-[var(--text-muted)]'
                                                }\`}>
                                                    {slot.status === 'completed' && <CheckIcon size={18} />}
                                                    {slot.status === 'current' && <BookIcon />}
                                                    {slot.status === 'upcoming' && <ClockIcon />}
                                                </div>
                                                <div className="flex min-w-0 flex-1 flex-col gap-[0.18rem]">
                                                    <span className={\`font-ui text-base font-semibold tracking-[0.01em] text-[var(--text-primary)] flex items-center gap-2 \${slot.status === 'completed' ? 'line-through opacity-55' : ''}\`}>
                                                        {slot.name}
                                                        {slot.time && <span className="text-[0.7rem] font-mono text-[var(--text-secondary)] bg-[var(--bg-secondary)] px-1.5 py-0.5 rounded-sm no-underline opacity-80 inline-block">{slot.time}</span>}
                                                    </span>
                                                    <span className={\`font-body text-[0.78rem] leading-[1.3] \${slot.status === 'current' ? 'text-[var(--accent-primary)]' : 'text-[var(--text-muted)]'}\`}>
                                                        {slot.status === 'completed' && \`\${slot.doneInSlot}/\${slot.count} \${PLANNER_UNITS[planner.unitType]?.plural} ✓\`}
                                                        {slot.status === 'current' && \`\${slot.doneInSlot} of \${slot.count} \${PLANNER_UNITS[planner.unitType]?.plural} read\`}
                                                        {slot.status === 'upcoming' && \`\${slot.count} \${PLANNER_UNITS[planner.unitType]?.plural} · not started\`}
                                                    </span>
                                                </div>
                                                <div className="shrink-0">
                                                    {slot.status === 'completed' && (
                                                        <button className="flex h-[26px] w-[26px] cursor-pointer items-center justify-center rounded-full border-none bg-[rgba(16,185,129,0.12)] text-[#10b981] transition-all duration-[0.18s] hover:scale-110 hover:bg-[rgba(220,38,38,0.1)] hover:text-[#dc2626]"
                                                            onClick={() => handleUndoPrayer(slot)} title="Undo this prayer">
                                                            <CheckIcon size={14} />
                                                        </button>
                                                    )}
                                                    {slot.status === 'current' && slot.slotRoute && (
                                                        <Link to={hasStartedReading ? (resumeRoute || slot.slotRoute) : slot.slotRoute}
                                                            className="inline-flex cursor-pointer items-center rounded-full border-none bg-[var(--accent-primary)] px-[18px] py-[7px] font-body text-[0.82rem] italic text-white no-underline transition-all duration-200 hover:bg-[var(--accent-hover)]">
                                                            {hasStartedReading ? 'Resume' : 'Start'}
                                                        </Link>
                                                    )}
                                                    {slot.status === 'upcoming' && (
                                                        <button className="flex cursor-pointer items-center border-none bg-transparent p-0.5" onClick={() => handleMarkPrayer(slot)} title="Mark done">
                                                            <div className="h-5 w-5 rounded-full border-[1.5px] border-[var(--text-muted)]" />
                                                        </button>
                                                    )}
                                                </div>
                                            </motion.div>
                                        ))}
                                    </div>
                                </div>
                            </div>

                            {/* Bottom Section: Journey & Analytics */}
                            <div className="flex flex-col gap-8 border-t border-[var(--glass-border)] pt-8">
                                <div className="flex flex-col rounded-2xl bg-[var(--glass-bg)] border border-[var(--glass-border)] p-5 shadow-[var(--shadow-glass)]">
                                    <h3 className="mb-4 font-ui text-[1.1rem] font-semibold text-[var(--text-primary)]">Performance Insights</h3>
                                    <div className="grid gap-4 md:grid-cols-3 mb-2">
                                        <div className="flex flex-col gap-1 bg-[var(--bg-primary)] p-4 rounded-[14px] shadow-sm">
                                            <span className="font-mono text-[0.65rem] uppercase tracking-[0.08em] text-[var(--text-muted)]">On-Time Completion</span>
                                            <span className="font-ui text-[1.6rem] font-bold text-[#10b981]">{analytics.onTimeRate}%</span>
                                        </div>
                                        <div className="flex flex-col gap-1 bg-[var(--bg-primary)] p-4 rounded-[14px] shadow-sm">
                                            <span className="font-mono text-[0.65rem] uppercase tracking-[0.08em] text-[var(--text-muted)]">Catch-ups Used</span>
                                            <span className="font-ui text-[1.6rem] font-bold text-[var(--accent-primary)]">{analytics.catchUpDaysCount}</span>
                                        </div>
                                        <div className="flex flex-col gap-1 bg-[var(--bg-primary)] p-4 rounded-[14px] shadow-sm">
                                            <span className="font-mono text-[0.65rem] uppercase tracking-[0.08em] text-[var(--text-muted)]">Current Pace</span>
                                            <span className="font-ui text-[1.6rem] font-bold text-[var(--text-primary)]">{Math.round(analytics.avgUnitsPerDay)} <span className="text-[1rem] text-[var(--text-muted)] font-medium">u/day</span></span>
                                        </div>
                                    </div>
                                </div>

                                <div className="flex flex-col">
                                    <div className="mb-3 flex items-baseline justify-between px-1">
                                        <h2 className="font-ui text-[1.1rem] font-semibold text-[var(--text-primary)]">Timeline</h2>
                                        <div className="flex gap-2">
                                            <button onClick={() => setShowAdjustPace(true)} className="cursor-pointer border-none bg-transparent text-[0.75rem] font-medium tracking-[0.05em] text-[var(--text-secondary)] hover:text-[var(--text-primary)] hover:underline">Adjust Pace</button>
                                            <span className="font-mono text-[0.65rem] tracking-[0.05em] text-[var(--text-muted)] hidden md:inline-block">· {completedDays} of {planner.durationDays} done</span>
                                        </div>
                                    </div>
                                    <div className="flex flex-wrap gap-[6px] rounded-[18px] border border-[var(--glass-border)] bg-[var(--glass-bg)] p-4 md:gap-[8px]">
                                        {planner.assignments.map(a => {
                                            const status = getAssignmentStatus(planner, a, today);
                                            const progress = getAssignmentProgress(planner, a);
                                            const isToday = a.date === today;
                                            const pct = progress.totalCount ? Math.round((progress.completedCount / progress.totalCount) * 100) : 0;
                                            const statusStyles = {
                                                completed: 'bg-[var(--accent-primary)] text-white',
                                                today: 'shadow-[0_0_0_2px_var(--accent-primary)] bg-[var(--accent-light)] text-[var(--accent-primary)]',
                                                overdue: 'shadow-[0_0_0_1.5px_rgba(220,38,38,0.3)] bg-[rgba(220,38,38,0.1)] text-[#dc2626]',
                                                upcoming: 'bg-[var(--bg-surface)] text-[var(--text-muted)]',
                                            };
                                            return (
                                                <motion.div key={a.dayNumber}
                                                    className={\`relative flex h-[34px] w-[34px] cursor-default flex-col items-center justify-center rounded-full transition-all duration-200 md:h-[38px] md:w-[38px] \${statusStyles[status] || statusStyles.upcoming}\`}
                                                    title={\`Day \${a.dayNumber}: \${a.title} — \${status === 'completed' ? '✓ Done' : status === 'today' ? \`\${pct}%\` : status}\`}
                                                    whileHover={{ scale: 1.15 }} whileTap={{ scale: 0.9 }}>
                                                    <span className="relative z-10 font-mono text-[0.55rem] font-medium leading-none">{a.dayNumber}</span>
                                                    {difficulty[a.dayNumber] && difficulty[a.dayNumber].level !== 'moderate' && (
                                                        <span className={\`absolute top-0 right-0 h-2 w-2 rounded-full border border-[var(--bg-primary)] \${
                                                            difficulty[a.dayNumber].level === 'heavy' ? 'bg-[#dc2626]' : 'bg-[#10b981]'
                                                        }\`} title={difficulty[a.dayNumber].level === 'heavy' ? 'Heavier reading day' : 'Lighter reading day'} />
                                                    )}
                                                    {status === 'completed' && (
                                                        <svg className="absolute bottom-px right-px opacity-85" width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round">
                                                            <polyline points="20 6 9 17 4 12"/>
                                                        </svg>
                                                    )}
                                                    {isToday && status !== 'completed' && pct > 0 && (
                                                        <div className="pointer-events-none absolute inset-0 rounded-full opacity-25" style={{ background: \`conic-gradient(var(--accent-primary) \${pct}%, transparent \${pct}%)\` }} />
                                                    )}
                                                </motion.div>
                                            );
                                        })}
                                    </div>
                                    <div className="mt-3 flex flex-wrap justify-center gap-4">
                                        {[
                                            { label: 'Done', color: 'var(--accent-primary)' },
                                            { label: 'Today', color: 'var(--accent-hover)' },
                                            { label: 'Missed', color: '#dc2626' },
                                            { label: 'Upcoming', color: 'var(--text-muted)' },
                                        ].map(item => (
                                            <span key={item.label} className="flex items-center gap-1 font-mono text-[0.6rem] tracking-[0.04em] text-[var(--text-muted)]">
                                                <span className="inline-block h-2.5 w-2.5 shrink-0 rounded-full" style={{ background: item.color }} />
                                                {item.label}
                                            </span>
                                        ))}
                                    </div>
                                </div>

                                {weeklySummary && weeklySummary.length > 0 && (
                                    <div className="flex flex-col rounded-2xl bg-[var(--glass-bg)] border border-[var(--glass-border)] p-5 shadow-[var(--shadow-glass)]">
                                        <h4 className="mb-4 font-ui text-[1.1rem] font-semibold text-[var(--text-primary)]">Weekly Progress</h4>
                                        <div className="flex flex-col gap-3">
                                            {weeklySummary.slice(-4).map((week, i) => (
                                                <div key={i} className="flex items-center justify-between bg-[var(--bg-primary)] px-4 py-3.5 rounded-[12px] shadow-sm">
                                                    <span className="font-body text-[0.85rem] font-medium text-[var(--text-primary)]">{week.label}</span>
                                                    <div className="flex items-center gap-4">
                                                        <span className="font-mono text-[0.75rem] text-[var(--text-secondary)]">{week.completedUnits} / {week.totalUnits}</span>
                                                        <div className="w-[100px] h-2.5 bg-[var(--bg-surface)] rounded-full overflow-hidden">
                                                            <div className="h-full bg-[var(--accent-primary)] rounded-full transition-all duration-500" style={{ width: \`\${Math.min(100, Math.round((week.completedUnits / week.totalUnits) * 100))}%\` }} />
                                                        </div>
                                                    </div>
                                                </div>
                                            ))}
                                        </div>
                                    </div>
                                )}

                                {overview?.overdueDays > 0 && (
                                    <div className="mt-2 flex flex-col gap-3 rounded-2xl bg-[rgba(220,38,38,0.05)] border border-[rgba(220,38,38,0.1)] p-5">
                                        <h4 className="font-ui text-[1.05rem] font-semibold text-[#dc2626]">Need to catch up?</h4>
                                        <button onClick={() => shiftPlannerSchedule(planner.id, overview.overdueDays)} className="w-full cursor-pointer rounded-full border border-[#dc2626] bg-transparent p-[0.95rem] font-ui text-[0.95rem] font-bold text-[#dc2626] shadow-sm transition-all duration-200 hover:bg-[rgba(220,38,38,0.1)]">
                                            Shift Plan ({overview.overdueDays} Days)
                                        </button>
                                        <button onClick={() => {
                                            const updated = redistributeMissedAssignments(planner);
                                            if (updated) setPlanner(updated);
                                        }} className="w-full cursor-pointer rounded-full border-none bg-[rgba(220,38,38,0.12)] p-[0.95rem] font-ui text-[0.95rem] font-bold text-[#dc2626] shadow-none transition-all duration-200 hover:bg-[rgba(220,38,38,0.18)]" title="Keep the same end date, distribute missed reading across remaining days">
                                            Redistribute Missed Pages
                                        </button>
                                    </div>
                                )}
                            </div>
                        </div>
                    )}

                    {/* --- JOURNAL TAB --- */}
                    {activeTab === 'journal' && (
                        <div className="flex flex-col gap-6">
                            {/* Reflections Section */}
                            <div className="flex flex-col rounded-2xl bg-[var(--glass-bg)] border border-[var(--glass-border)] p-5 shadow-[var(--shadow-glass)]">
                                <h3 className="mb-4 font-ui text-[1.1rem] font-semibold text-[var(--text-primary)] flex items-center gap-2">
                                    <BookIcon /> Daily Reflections
                                </h3>
                                <div className="flex flex-col gap-4 max-h-[400px] overflow-y-auto pr-2 custom-scrollbar">
                                    {plannerReflections && plannerReflections[planner.id] && Object.keys(plannerReflections[planner.id]).length > 0 ? (
                                        Object.entries(plannerReflections[planner.id])
                                            .sort((a, b) => Number(b[0]) - Number(a[0]))
                                            .map(([dayId, ref]) => (
                                                <div key={dayId} className="rounded-xl bg-[var(--bg-primary)] p-4 shadow-sm border border-[var(--glass-border)]">
                                                    <div className="font-mono text-[0.65rem] text-[var(--text-muted)] mb-2">DAY {dayId}</div>
                                                    <p className="font-body text-[0.9rem] text-[var(--text-primary)] m-0 leading-relaxed italic">"{ref.text}"</p>
                                                </div>
                                            ))
                                    ) : (
                                        <div className="text-center py-10 text-[var(--text-muted)] font-body text-[0.9rem]">
                                            No reflections yet. Complete a day to write one!
                                        </div>
                                    )}
                                </div>
                            </div>

                            {/* Bookmarks / Highlights Section */}
                            <div className="flex flex-col rounded-2xl bg-[var(--glass-bg)] border border-[var(--glass-border)] p-5 shadow-[var(--shadow-glass)]">
                                <h3 className="mb-4 font-ui text-[1.1rem] font-semibold text-[var(--text-primary)] flex items-center gap-2">
                                    <GemIcon /> Plan Highlights
                                </h3>
                                <div className="flex flex-col gap-3 max-h-[400px] overflow-y-auto pr-2 custom-scrollbar">
                                    {plannerBookmarks && plannerBookmarks[planner.id] && plannerBookmarks[planner.id].length > 0 ? (
                                        plannerBookmarks[planner.id]
                                            .sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt))
                                            .map(b => (
                                                <Link key={b.verseKey} to={\`/surah/\${b.verseKey.split(':')[0]}?ayah=\${b.verseKey.split(':')[1]}\`} className="rounded-xl bg-[var(--bg-primary)] p-4 shadow-sm border border-[var(--glass-border)] no-underline transition-colors hover:border-[var(--accent-primary)]">
                                                    <div className="flex justify-between items-center">
                                                        <span className="font-ui text-[0.95rem] font-bold text-[var(--text-primary)]">{b.surahName}</span>
                                                        <span className="font-mono text-[0.7rem] bg-[var(--accent-light)] text-[var(--accent-primary)] px-2 py-0.5 rounded-md">{b.verseKey}</span>
                                                    </div>
                                                </Link>
                                            ))
                                    ) : (
                                        <div className="text-center py-10 text-[var(--text-muted)] font-body text-[0.9rem]">
                                            No highlighted verses. Use the bookmark icon while reading!
                                        </div>
                                    )}
                                </div>
                            </div>
                        </div>
                    )}
                </div>

                {/* Footer Controls */}
                <div className="mt-12 mb-4 w-full flex justify-center">
                    <button className="inline-flex cursor-pointer items-center gap-1.5 rounded-md border-none bg-transparent px-4 py-2 font-body text-[0.85rem] text-[var(--text-muted)] opacity-60 transition-all duration-200 hover:opacity-100 hover:text-[#dc2626]" onClick={onDelete} title="Delete plan">
                        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round">
                            <polyline points="3 6 5 6 21 6"/><path d="M19 6l-1 14H6L5 6"/>
                            <path d="M10 11v6"/><path d="M14 11v6"/><path d="M9 6V4h6v2"/>
                        </svg>
                        Delete plan
                    </button>
                </div>

            </motion.div>

            {/* MODALS */}
            <AnimatePresence>
                {showAdjustPace && (
                    <motion.div className="fixed inset-0 z-[1000] flex items-center justify-center bg-[rgba(0,0,0,0.5)] p-4 backdrop-blur-sm"
                        initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}
                        onClick={e => { if (e.target === e.currentTarget) setShowAdjustPace(false); }}>
                        <motion.div className="w-full max-w-[400px] rounded-2xl bg-[var(--bg-primary)] border border-[var(--glass-border)] p-6 shadow-2xl"
                            initial={{ opacity: 0, y: 20, scale: 0.98 }} animate={{ opacity: 1, y: 0, scale: 1 }} exit={{ opacity: 0, y: 20, scale: 0.98 }}>
                            <h2 className="mb-4 font-ui text-xl font-semibold text-[var(--text-primary)]">Adjust Pace</h2>
                            <p className="mb-5 font-body text-[0.85rem] leading-relaxed text-[var(--text-secondary)]">
                                Change how many days you want to complete your remaining plan in. This will re-calculate your daily assignments.
                            </p>
                            <div className="mb-6 flex flex-col gap-2">
                                <label className="font-mono text-[0.68rem] uppercase tracking-[0.1em] text-[var(--text-muted)]">New Total Days</label>
                                <input type="number" min="1" max="1000" className="w-full rounded-[10px] border-[1.5px] border-[var(--glass-border)] bg-[var(--bg-surface)] px-4 py-3 font-body text-[1.1rem] text-[var(--text-primary)] outline-none focus:border-[var(--accent-primary)]"
                                    value={newDuration} onChange={e => setNewDuration(parseInt(e.target.value) || 1)} />
                            </div>
                            <div className="flex gap-3">
                                <button className="flex-1 cursor-pointer rounded-xl border border-[var(--glass-border)] bg-transparent p-3 font-body font-medium text-[var(--text-secondary)] transition-colors hover:bg-[var(--bg-surface)]" onClick={() => setShowAdjustPace(false)}>Cancel</button>
                                <button className="flex-1 cursor-pointer rounded-xl border-none bg-[var(--accent-primary)] p-3 font-body font-medium text-white transition-colors hover:bg-[var(--accent-hover)]" onClick={() => {
                                    const updated = adjustPlannerPace(planner, newDuration);
                                    setPlanner(updated);
                                    setShowAdjustPace(false);
                                }}>Apply Changes</button>
                            </div>
                        </motion.div>
                    </motion.div>
                )}
            </AnimatePresence>

            <AnimatePresence>
                {showSettings && (
                    <motion.div className="fixed inset-0 z-[1000] flex items-center justify-center bg-[rgba(0,0,0,0.5)] p-4 backdrop-blur-sm"
                        initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}
                        onClick={e => { if (e.target === e.currentTarget) setShowSettings(false); }}>
                        <motion.div className="w-full max-w-[400px] rounded-2xl bg-[var(--bg-primary)] border border-[var(--glass-border)] p-6 shadow-2xl"
                            initial={{ opacity: 0, y: 20, scale: 0.98 }} animate={{ opacity: 1, y: 0, scale: 1 }} exit={{ opacity: 0, y: 20, scale: 0.98 }}>
                            <h2 className="mb-4 font-ui text-xl font-semibold text-[var(--text-primary)]">Planner Settings</h2>
                            
                            <div className="flex flex-col gap-5 mb-6">
                                <div className="flex flex-col gap-2">
                                    <h3 className="font-mono text-[0.7rem] uppercase tracking-[0.1em] text-[var(--text-muted)]">Daily Prayers</h3>
                                    <p className="font-body text-[0.8rem] text-[var(--text-secondary)] mb-1">Select which prayers you want to distribute reading across.</p>
                                    <div className="flex flex-wrap gap-2">
                                        {['Fajr', 'Dhuhr', 'Asr', 'Maghrib', 'Isha'].map(p => {
                                            const isActive = useAppStore.getState().prayerSettings?.activePrayers?.includes(p) ?? true;
                                            return (
                                                <button key={p} className={\`cursor-pointer rounded-full border-[1.5px] px-3 py-1 font-body text-[0.8rem] transition-colors \${
                                                    isActive ? 'border-[var(--accent-primary)] bg-[var(--accent-primary)] text-white' : 'border-[var(--glass-border)] text-[var(--text-muted)]'
                                                }\`} onClick={() => {
                                                    const s = useAppStore.getState();
                                                    const current = s.prayerSettings?.activePrayers || ['Fajr', 'Dhuhr', 'Asr', 'Maghrib', 'Isha'];
                                                    const next = current.includes(p) ? current.filter(x => x !== p) : [...current, p];
                                                    if(next.length === 0) return; // Must have at least 1
                                                    s.updatePrayerSettings({ activePrayers: next });
                                                }}>
                                                    {p}
                                                </button>
                                            );
                                        })}
                                    </div>
                                </div>
                                <div className="flex flex-col gap-2">
                                    <h3 className="font-mono text-[0.7rem] uppercase tracking-[0.1em] text-[var(--text-muted)]">Reading Preference</h3>
                                    <select className="w-full rounded-[10px] border-[1.5px] border-[var(--glass-border)] bg-[var(--bg-surface)] px-3 py-2 font-body text-[0.9rem] text-[var(--text-primary)] outline-none"
                                        value={useAppStore.getState().prayerSettings?.readPreference || 'after'}
                                        onChange={e => useAppStore.getState().updatePrayerSettings({ readPreference: e.target.value })}>
                                        <option value="after">Read After Prayer</option>
                                        <option value="before">Read Before Prayer</option>
                                        <option value="split">Split Before & After</option>
                                    </select>
                                </div>
                                <div className="flex items-center justify-between border-t border-[var(--glass-border)] pt-4">
                                    <div className="flex flex-col">
                                        <h3 className="font-mono text-[0.7rem] uppercase tracking-[0.1em] text-[var(--text-muted)]">Intention Prompts</h3>
                                        <p className="font-body text-[0.8rem] text-[var(--text-secondary)]">Show a mindfulness prompt before reading.</p>
                                    </div>
                                    <button className={\`relative h-6 w-11 cursor-pointer rounded-full border-none transition-colors \${
                                        useAppStore.getState().intentionPromptEnabled ? 'bg-[var(--accent-primary)]' : 'bg-[var(--bg-surface)] border border-[var(--glass-border)]'
                                    }\`} onClick={() => useAppStore.getState().toggleIntentionPrompt()}>
                                        <div className={\`absolute left-1 top-1 h-4 w-4 rounded-full bg-white transition-transform \${
                                            useAppStore.getState().intentionPromptEnabled ? 'translate-x-5' : 'translate-x-0'
                                        }\`} />
                                    </button>
                                </div>
                            </div>

                            <button className="w-full cursor-pointer rounded-xl border border-[var(--glass-border)] bg-[var(--bg-surface)] p-3 font-body font-medium text-[var(--text-primary)] transition-colors hover:bg-[var(--bg-secondary)]" onClick={() => setShowSettings(false)}>Done</button>
                        </motion.div>
                    </motion.div>
                )}
            </AnimatePresence>
        </div>
    );
}
`;

fs.writeFileSync(filePath, before + newActiveView + after);
console.log('Successfully replaced ActiveView!');
