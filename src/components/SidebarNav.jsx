import React from 'react';
import { Link, useLocation } from 'react-router-dom';
import { BookOpen, Brain, CalendarDays, TrendingUp, User } from 'lucide-react';

export default function SidebarNav() {
    const location = useLocation();

    const isSurahPage = /^\/surah\/\d+/.test(location.pathname);
    const isMemorizePage = /^\/memorize\/\d+/.test(location.pathname);
    const isPagePage = /^\/page\/\d+/.test(location.pathname);
    const isPlannerReader = /^\/planner\/read\/\d+/.test(location.pathname);

    const isActive = (path) => {
        if (path === '/' && location.pathname !== '/') return false;
        return location.pathname.startsWith(path);
    };

    if (isSurahPage || isMemorizePage || isPagePage || isPlannerReader) return null;

    const tabs = [
        { path: '/', icon: BookOpen, label: 'Quran' },
        { path: '/memorize', icon: Brain, label: 'Memorize' },
        { path: '/planner', icon: CalendarDays, label: 'Planner' },
        { path: '/progress', icon: TrendingUp, label: 'Analytics' },
        { path: '/profile', icon: User, label: 'Profile' }
    ];

    return (
        <div className="hidden md:flex fixed left-0 top-[38px] bottom-0 w-[240px] z-[900] flex-col border-r border-[var(--glass-border)] bg-[var(--color-paper)] p-4 pt-16">
            <div className="flex flex-col gap-2">
                {tabs.map((tab) => {
                    const active = isActive(tab.path);
                    const Icon = tab.icon;

                    if (active) {
                        return (
                            <div
                                key={tab.path}
                                className="flex cursor-pointer items-center gap-3 rounded-xl bg-[var(--accent-light)] px-4 py-3 text-[var(--accent)] transition-colors"
                                onClick={() => window.scrollTo({ top: 0, behavior: 'smooth' })}
                            >
                                <Icon size={20} color="currentColor" />
                                <span className="font-ui text-sm font-semibold">{tab.label}</span>
                            </div>
                        );
                    }

                    return (
                        <Link
                            key={tab.path}
                            to={tab.path}
                            className="flex items-center gap-3 rounded-xl px-4 py-3 text-[var(--text-muted)] no-underline transition-colors duration-200 hover:bg-[var(--bone)] hover:text-[var(--accent)]"
                        >
                            <Icon size={20} />
                            <span className="font-ui text-sm font-medium">{tab.label}</span>
                        </Link>
                    );
                })}
            </div>
        </div>
    );
}
