import { fetch as tauriFetch } from '@tauri-apps/plugin-http';
import { writeFile, exists, mkdir, BaseDirectory } from '@tauri-apps/plugin-fs';
import { convertFileSrc } from '@tauri-apps/api/core';
import { join, appDataDir } from '@tauri-apps/api/path';

export const isTauri = () => !!window.__TAURI_INTERNALS__;

export const downloadAudioFile = async (url, surahId, reciterId) => {
    if (!isTauri()) return false;

    try {
        const audioDirExists = await exists('audio', { baseDir: BaseDirectory.AppData });
        if (!audioDirExists) {
            await mkdir('audio', { baseDir: BaseDirectory.AppData });
        }

        const fileName = `audio/${surahId}_${reciterId}.mp3`;
        const fileExists = await exists(fileName, { baseDir: BaseDirectory.AppData });
        
        if (fileExists) {
            return true; // Already downloaded
        }

        const res = await tauriFetch(url);
        if (res.ok) {
            const buffer = await res.arrayBuffer();
            await writeFile(fileName, new Uint8Array(buffer), { baseDir: BaseDirectory.AppData });
            return true;
        }
        return false;
    } catch (e) {
        console.error("Failed to download audio via Tauri", e);
        return false;
    }
};

export const getTauriAudioUrl = async (surahId, reciterId) => {
    if (!isTauri()) return null;

    try {
        const fileName = `audio/${surahId}_${reciterId}.mp3`;
        const fileExists = await exists(fileName, { baseDir: BaseDirectory.AppData });
        
        if (fileExists) {
            const dataDir = await appDataDir();
            const filePath = await join(dataDir, fileName);
            return convertFileSrc(filePath);
        }
    } catch (e) {
        console.error("Failed to get Tauri audio URL", e);
    }
    return null;
};
