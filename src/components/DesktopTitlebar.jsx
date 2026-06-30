import { Window } from '@tauri-apps/api/window';
import { Minus, Square, X } from 'lucide-react';
import { useEffect, useState } from 'react';

const appWindow = new Window('main');

export default function DesktopTitlebar() {
  const [isMaximized, setIsMaximized] = useState(false);
  const [isTauri, setIsTauri] = useState(false);

  useEffect(() => {
    // Only run if in Tauri environment
    if (window.__TAURI_INTERNALS__) {
      setIsTauri(true);
      appWindow.isMaximized().then(setIsMaximized);
      
      const unlisten = appWindow.onResized(async () => {
        setIsMaximized(await appWindow.isMaximized());
      });
      
      return () => {
        unlisten.then(f => f());
      };
    }
  }, []);

  if (!isTauri) return null;

  return (
    <div
      data-tauri-drag-region
      className="fixed inset-x-0 top-0 z-[2000] flex h-[38px] select-none items-center justify-between border-b border-[var(--glass-border)] bg-[var(--color-paper)]"
    >
      <div className="pointer-events-none flex h-full items-center pl-4">
        <span className="font-ui text-xs font-semibold text-[var(--h-ink-mid)] tracking-wide">
          The Noble Quran
        </span>
      </div>

      <div className="flex h-full">
        <button
          onClick={() => appWindow.minimize()}
          className="flex h-full w-[46px] items-center justify-center text-[var(--h-ink-muted)] transition-colors hover:bg-[var(--h-bone-dark)] hover:text-[var(--h-ink)]"
          aria-label="Minimize"
        >
          <Minus size={16} />
        </button>
        <button
          onClick={() => appWindow.toggleMaximize()}
          className="flex h-full w-[46px] items-center justify-center text-[var(--h-ink-muted)] transition-colors hover:bg-[var(--h-bone-dark)] hover:text-[var(--h-ink)]"
          aria-label="Maximize"
        >
          {isMaximized ? (
            <div className="relative h-[12px] w-[12px]">
              <div className="absolute right-0 top-0 h-[10px] w-[10px] border-[1.5px] border-current" />
              <div className="absolute bottom-0 left-0 h-[10px] w-[10px] border-[1.5px] border-current bg-[var(--color-paper)]" />
            </div>
          ) : (
            <Square size={14} />
          )}
        </button>
        <button
          onClick={() => appWindow.close()}
          className="flex h-full w-[46px] items-center justify-center text-[var(--h-ink-muted)] transition-colors hover:bg-red-500 hover:text-white"
          aria-label="Close"
        >
          <X size={18} />
        </button>
      </div>
    </div>
  );
}
