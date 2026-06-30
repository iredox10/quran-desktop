import { Store } from '@tauri-apps/plugin-store';

// We lazy initialize the store so it doesn't break SSR or non-Tauri environments
let nativeStore = null;

async function getNativeStore() {
    if (!nativeStore) {
        // Automatically save on every set in the background
        nativeStore = new Store('app_state.bin');
    }
    return nativeStore;
}

export const tauriStorage = {
    getItem: async (name) => {
        if (!window.__TAURI_INTERNALS__) return localStorage.getItem(name);
        try {
            const store = await getNativeStore();
            const value = await store.get(name);
            return value || null;
        } catch (e) {
            console.error('Failed to get from Tauri Store', e);
            return null;
        }
    },
    setItem: async (name, value) => {
        if (!window.__TAURI_INTERNALS__) {
            localStorage.setItem(name, value);
            return;
        }
        try {
            const store = await getNativeStore();
            await store.set(name, value);
            await store.save();
        } catch (e) {
            console.error('Failed to save to Tauri Store', e);
        }
    },
    removeItem: async (name) => {
        if (!window.__TAURI_INTERNALS__) {
            localStorage.removeItem(name);
            return;
        }
        try {
            const store = await getNativeStore();
            await store.delete(name);
            await store.save();
        } catch (e) {
            console.error('Failed to delete from Tauri Store', e);
        }
    }
};
