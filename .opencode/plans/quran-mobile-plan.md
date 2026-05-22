# Quran Mobile — Implementation Plan

## Project Status
- ✅ Expo project created at `/home/iredox/Desktop/personal-apps/quran-mobile`
- ✅ Core dependencies installed (expo-router, expo-sqlite, zustand, mmkv, nativewind, etc.)
- ✅ Directory structure created

---

## Step 1: Configuration Files

### `tailwind.config.js`
```js
/** @type {import('tailwindcss').Config} */
module.exports = {
  content: ['./app/**/*.{js,jsx,ts,tsx}', './src/**/*.{js,jsx,ts,tsx}'],
  presets: [require('nativewind/preset')],
  theme: {
    extend: {
      colors: {
        primary: {
          50: '#e0f2f1', 100: '#b2dfdb', 200: '#80cbc4',
          300: '#4db6ac', 400: '#26a69a', 500: '#009688',
          600: '#00897b', 700: '#00796b', 800: '#00695c', 900: '#004d40',
        },
      },
    },
  },
  plugins: [],
};
```

### `babel.config.js` (replace existing)
```js
module.exports = function (api) {
  api.cache(true);
  return {
    presets: ['babel-preset-expo'],
    plugins: [
      'nativewind/babel',
      'react-native-reanimated/plugin',
    ],
  };
};
```

### `metro.config.js` (create new)
```js
const { getDefaultConfig } = require('expo/metro-config');
const { withNativeWind } = require('nativewind/metro');

const config = getDefaultConfig(__dirname);
module.exports = withNativeWind(config, { input: './src/global.css' });
```

### `src/global.css` (create new)
```css
@tailwind base;
@tailwind components;
@tailwind utilities;
```

### `app.json` (update)
```json
{
  "expo": {
    "name": "The Noble Quran",
    "slug": "quran-mobile",
    "version": "1.0.0",
    "orientation": "portrait",
    "icon": "./assets/icon.png",
    "scheme": "quran-mobile",
    "userInterfaceStyle": "automatic",
    "newArchEnabled": true,
    "splash": {
      "image": "./assets/splash-icon.png",
      "resizeMode": "contain",
      "backgroundColor": "#004d40"
    },
    "ios": {
      "supportsTablet": true,
      "bundleIdentifier": "com.quran.mobile"
    },
    "android": {
      "adaptiveIcon": {
        "foregroundImage": "./assets/adaptive-icon.png",
        "backgroundColor": "#004d40"
      },
      "package": "com.quran.mobile",
      "edgeToEdgeEnabled": true
    },
    "plugins": [
      "expo-router",
      "expo-sqlite",
      "expo-font",
      "expo-av"
    ]
  }
}
```

---

## Step 2: Quran Database Generation Script

### `scripts/generate-quran-db.js`
```js
const Database = require('better-sqlite3');
const fs = require('fs');
const path = require('path');
const https = require('https');

const DB_PATH = path.join(__dirname, '../assets/quran-data/quran.db');
const QURAN_API = 'https://api.quran.com/api/v4';

function fetchJSON(url) {
  return new Promise((resolve, reject) => {
    https.get(url, (res) => {
      let data = '';
      res.on('data', (chunk) => (data += chunk));
      res.on('end', () => resolve(JSON.parse(data)));
    }).on('error', reject);
  });
}

async function fetchAll(endpoint, param = '') {
  const url = `${QURAN_API}${endpoint}${param}`;
  const res = await fetchJSON(url);
  return res;
}

async function generate() {
  console.log('📖 Generating Quran database...');

  // Remove existing DB
  if (fs.existsSync(DB_PATH)) fs.unlinkSync(DB_PATH);

  const db = new Database(DB_PATH);

  // Enable WAL mode for better performance
  db.pragma('journal_mode = WAL');

  // Create tables
  db.exec(`
    CREATE TABLE IF NOT EXISTS chapters (
      id INTEGER PRIMARY KEY,
      revelation_place TEXT,
      verses_count INTEGER,
      name_simple TEXT,
      name_complex TEXT,
      name_arabic TEXT
    );

    CREATE TABLE IF NOT EXISTS verses (
      id INTEGER PRIMARY KEY,
      chapter_id INTEGER,
      verse_number INTEGER,
      verse_key TEXT UNIQUE,
      text_uthmani TEXT,
      text_uthmani_simple TEXT,
      juz_number INTEGER,
      hizb_number INTEGER,
      rub_number INTEGER,
      page_number INTEGER,
      FOREIGN KEY (chapter_id) REFERENCES chapters(id)
    );

    CREATE TABLE IF NOT EXISTS translations (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      verse_key TEXT,
      resource_id INTEGER,
      resource_name TEXT,
      language TEXT,
      text TEXT,
      FOREIGN KEY (verse_key) REFERENCES verses(verse_key)
    );

    CREATE TABLE IF NOT EXISTS tajweed (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      verse_key TEXT,
      text TEXT,
      FOREIGN KEY (verse_key) REFERENCES verses(verse_key)
    );

    CREATE INDEX IF NOT EXISTS idx_verses_chapter ON verses(chapter_id);
    CREATE INDEX IF NOT EXISTS idx_verses_key ON verses(verse_key);
    CREATE INDEX IF NOT EXISTS idx_translations_verse ON translations(verse_key);
    CREATE INDEX IF NOT EXISTS idx_translations_resource ON translations(resource_id);
  `);

  const insertChapter = db.prepare(
    'INSERT OR REPLACE INTO chapters (id, revelation_place, verses_count, name_simple, name_complex, name_arabic) VALUES (?, ?, ?, ?, ?, ?)'
  );
  const insertVerse = db.prepare(
    'INSERT OR REPLACE INTO verses (id, chapter_id, verse_number, verse_key, text_uthmani, text_uthmani_simple, juz_number, hizb_number, rub_number, page_number) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)'
  );
  const insertTranslation = db.prepare(
    'INSERT OR REPLACE INTO translations (verse_key, resource_id, resource_name, language, text) VALUES (?, ?, ?, ?, ?)'
  );
  const insertTajweed = db.prepare(
    'INSERT OR REPLACE INTO tajweed (verse_key, text) VALUES (?, ?)'
  );

  const insertChapterTx = db.transaction((chapters) => {
    for (const c of chapters) insertChapter.run(c);
  });
  const insertVerseTx = db.transaction((verses) => {
    for (const v of verses) insertVerse.run(v);
  });
  const insertTranslationTx = db.transaction((rows) => {
    for (const r of rows) insertTranslation.run(r);
  });
  const insertTajweedTx = db.transaction((rows) => {
    for (const r of rows) insertTajweed.run(r);
  });

  // 1. Fetch all chapters
  console.log('📋 Fetching chapters...');
  const chaptersRes = await fetchAll('/chapters');
  const chapters = chaptersRes.chapters.map((c) => [
    c.id,
    c.revelation_place,
    c.verses_count,
    c.name_simple,
    c.name_complex,
    c.name_arabic,
  ]);
  insertChapterTx(chapters);
  console.log(`   ✅ ${chapters.length} chapters saved`);

  // 2. Fetch all verses (batched by chapter)
  console.log('📝 Fetching verses...');
  const allVerses = [];
  for (let i = 1; i <= 114; i++) {
    process.stdout.write(`   Chapter ${i}/114\r`);
    const res = await fetchAll(`/verses/by_chapter/${i}`,
      '?language=en&words=false&translations=false&fields=text_uthmani,text_uthmani_simple,verse_key,juz_number,hizb_number,rub_el_hizb_number,page_number'
    );
    for (const v of res.verses) {
      allVerses.push([
        v.id,
        v.chapter_id,
        v.verse_number,
        v.verse_key,
        v.text_uthmani,
        v.text_uthmani_simple || v.text_uthmani,
        v.juz_number,
        v.hizb_number,
        v.rub_el_hizb_number,
        v.page_number,
      ]);
    }
  }
  console.log(`\n   ✅ ${allVerses.length} verses saved`);
  insertVerseTx(allVerses);

  // 3. Fetch translation (Abdel Haleem = 85)
  console.log('🌐 Fetching translations (Abdel Haleem)...');
  const allTranslations = [];
  for (let i = 1; i <= 114; i++) {
    process.stdout.write(`   Chapter ${i}/114\r`);
    const res = await fetchAll(`/verses/by_chapter/${i}`,
      '?language=en&words=false&translations=85&fields=text_uthmani'
    );
    for (const v of res.verses) {
      if (v.translations && v.translations.length > 0) {
        allTranslations.push([
          v.verse_key,
          85,
          'Abdel Haleem',
          'en',
          v.translations[0].text,
        ]);
      }
    }
  }
  console.log(`\n   ✅ ${allTranslations.length} translations saved`);
  insertTranslationTx(allTranslations);

  // 4. Fetch tajweed text
  console.log('🎨 Fetching tajweed text...');
  const allTajweed = [];
  for (let i = 1; i <= 114; i++) {
    process.stdout.write(`   Chapter ${i}/114\r`);
    const res = await fetchAll(`/verses/by_chapter/${i}`,
      '?language=en&words=false&translations=false&fields=text_uthmani_tajweed'
    );
    for (const v of res.verses) {
      if (v.text_uthmani_tajweed) {
        allTajweed.push([v.verse_key, v.text_uthmani_tajweed]);
      }
    }
  }
  console.log(`\n   ✅ ${allTajweed.length} tajweed entries saved`);
  insertTajweedTx(allTajweed);

  db.close();

  const sizeMB = (fs.statSync(DB_PATH).size / (1024 * 1024)).toFixed(2);
  console.log(`\n✅ Database generated: ${DB_PATH} (${sizeMB} MB)`);
}

generate().catch((err) => {
  console.error('❌ Error:', err);
  process.exit(1);
});
```

---

## Step 3: Database Service

### `src/services/database.js`
```js
import * as SQLite from 'expo-sqlite';
import * as FileSystem from 'expo-file-system';
import * as Asset from 'expo-asset';

let db = null;

export async function initDatabase() {
  if (db) return db;

  // Copy bundled DB from assets to documents directory
  const bundledDb = Asset.fromModule(require('../../assets/quran-data/quran.db'));
  await bundledDb.downloadAsync();

  const dbPath = FileSystem.documentDirectory + 'quran.db';
  const dbInfo = await FileSystem.getInfoAsync(dbPath);

  if (!dbInfo.exists) {
    await FileSystem.copyAsync({
      from: bundledDb.localUri || bundledDb.uri,
      to: dbPath,
    });
  }

  db = await SQLite.openDatabaseAsync('quran.db');
  return db;
}

export async function getChapter(id) {
  const database = await initDatabase();
  const result = await database.getFirstAsync(
    'SELECT * FROM chapters WHERE id = ?',
    [id]
  );
  return result;
}

export async function getAllChapters() {
  const database = await initDatabase();
  return await database.getAllAsync('SELECT * FROM chapters ORDER BY id');
}

export async function getVersesByChapter(chapterId, limit = 50, offset = 0) {
  const database = await initDatabase();
  return await database.getAllAsync(
    'SELECT * FROM verses WHERE chapter_id = ? ORDER BY verse_number LIMIT ? OFFSET ?',
    [chapterId, limit, offset]
  );
}

export async function getVerseByKey(verseKey) {
  const database = await initDatabase();
  return await database.getFirstAsync(
    'SELECT * FROM verses WHERE verse_key = ?',
    [verseKey]
  );
}

export async function getTranslation(verseKey, resourceId = 85) {
  const database = await initDatabase();
  return await database.getFirstAsync(
    'SELECT text FROM translations WHERE verse_key = ? AND resource_id = ?',
    [verseKey, resourceId]
  );
}

export async function getTajweed(verseKey) {
  const database = await initDatabase();
  const result = await database.getFirstAsync(
    'SELECT text FROM tajweed WHERE verse_key = ?',
    [verseKey]
  );
  return result?.text || null;
}

export async function getVersesWithTranslation(chapterId, resourceId = 85, limit = 50, offset = 0) {
  const database = await initDatabase();
  return await database.getAllAsync(
    `SELECT v.*, t.text as translation_text
     FROM verses v
     LEFT JOIN translations t ON v.verse_key = t.verse_key AND t.resource_id = ?
     WHERE v.chapter_id = ?
     ORDER BY v.verse_number
     LIMIT ? OFFSET ?`,
    [resourceId, chapterId, limit, offset]
  );
}

export async function searchVerses(query) {
  const database = await initDatabase();
  return await database.getAllAsync(
    `SELECT v.*, t.text as translation_text
     FROM verses v
     LEFT JOIN translations t ON v.verse_key = t.verse_key
     WHERE v.text_uthmani_simple LIKE ? OR t.text LIKE ?
     LIMIT 50`,
    [`%${query}%`, `%${query}%`]
  );
}

export async function getChapterStats(chapterId) {
  const database = await initDatabase();
  return await database.getFirstAsync(
    'SELECT COUNT(*) as verse_count, MIN(page_number) as first_page, MAX(page_number) as last_page FROM verses WHERE chapter_id = ?',
    [chapterId]
  );
}
```

---

## Step 4: Zustand Store (ported from web app)

### `src/store/useAppStore.js`
```js
import { create } from 'zustand';
import { MMKV } from 'react-native-mmkv';

const storage = new MMKV();

// Persist middleware for MMKV
const mmkvPersist = (config, options) => {
  const setState = config;

  // Load persisted state
  try {
    const saved = storage.getString(options.name);
    if (saved) {
      const parsed = JSON.parse(saved);
      if (options.merge) {
        return options.merge(parsed, setState);
      }
      return { ...setState, ...parsed };
    }
  } catch (e) {
    console.warn('Failed to load persisted state:', e);
  }

  return setState;
};

export const useAppStore = create((set, get) => ({
  // Theme
  theme: 'light',
  toggleTheme: () => set((state) => ({
    theme: state.theme === 'light' ? 'dark' : 'light',
  })),

  // Reading settings
  translationId: 85,
  reciterId: 7,
  fontSize: 2,
  translationFontSize: 2,
  readingMode: false,
  tajweedEnabled: false,
  tafsirId: 169,

  setTranslation: (id) => set({ translationId: id }),
  setReciter: (id) => set({ reciterId: id }),
  setFontSize: (size) => set({ fontSize: size }),
  setTranslationFontSize: (size) => set({ translationFontSize: size }),
  setReadingMode: (mode) => set({ readingMode: mode }),
  setTajweed: (enabled) => set({ tajweedEnabled: enabled }),
  setTafsirId: (id) => set({ tafsirId: id }),

  // User data
  bookmarks: [],
  memorizedAyahs: [],
  memorizedSurahs: [],
  collections: [],
  recentlyRead: [],
  readingSessions: [],

  toggleBookmark: (verseKey, surahName, chapterId = null) => set((state) => {
    const exists = state.bookmarks.find((b) => b.verseKey === verseKey);
    if (exists) {
      return { bookmarks: state.bookmarks.filter((b) => b.verseKey !== verseKey) };
    }
    return { bookmarks: [...state.bookmarks, { verseKey, surahName, chapterId }] };
  }),

  toggleMemorizedAyah: (verseKey) => set((state) => {
    const isMemorized = state.memorizedAyahs.includes(verseKey);
    if (isMemorized) {
      return { memorizedAyahs: state.memorizedAyahs.filter((k) => k !== verseKey) };
    }
    return { memorizedAyahs: [...state.memorizedAyahs, verseKey] };
  }),

  toggleMemorizedSurah: (chapterId) => set((state) => {
    const isMemorized = state.memorizedSurahs.includes(chapterId);
    if (isMemorized) {
      return { memorizedSurahs: state.memorizedSurahs.filter((id) => id !== chapterId) };
    }
    return { memorizedSurahs: [...state.memorizedSurahs, chapterId] };
  }),

  addRecentlyRead: (chapterId, chapterName, verseKey = null) => set((state) => {
    const filtered = state.recentlyRead.filter((r) => r.chapterId !== chapterId);
    const newList = [{ chapterId, chapterName, verseKey, timestamp: Date.now() }, ...filtered].slice(0, 5);
    return { recentlyRead: newList };
  }),

  logReadingSession: (duration, type = 'reading', chapterId = null) => set((state) => {
    const today = new Date().toISOString().split('T')[0];
    const session = { date: today, duration, type, chapterId, timestamp: Date.now() };
    return { readingSessions: [...state.readingSessions, session].slice(-500) };
  }),

  // Audio state
  audioPlaylist: [],
  audioTrackIndex: 0,
  isPlaying: false,
  isPlayerVisible: false,
  audioSettings: {
    startRange: null,
    endRange: null,
    reciterId: 7,
    repeatSelection: 1,
    repeatAya: 1,
    delayBetweenAyas: 0,
    playbackSpeed: 1.0,
  },

  setAudioPlaylist: (playlist, startIndex = 0) => set({
    audioPlaylist: playlist,
    audioTrackIndex: startIndex,
  }),
  setAudioTrackIndex: (index) => set({ audioTrackIndex: index }),
  setIsPlaying: (playing) => set({ isPlaying: playing }),
  setIsPlayerVisible: (val) => set({ isPlayerVisible: val }),
  updateAudioSettings: (newSettings) => set((state) => ({
    audioSettings: { ...state.audioSettings, ...newSettings },
  })),

  // Planner
  planners: [],
  activePlannerId: null,
  planner: null,

  setPlanner: (planner) => set((state) => {
    const planners = state.planners;
    const existingIndex = planners.findIndex((item) => item.id === planner.id);
    const nextPlanners = existingIndex >= 0
      ? planners.map((item, index) => index === existingIndex ? planner : item)
      : [planner, ...planners];
    const activePlanner = nextPlanners.find((p) => p.id === planner.id) || nextPlanners[0] || null;
    return { planners: nextPlanners, activePlannerId: planner.id, planner: activePlanner };
  }),

  setActivePlanner: (plannerId) => set((state) => {
    const planner = state.planners.find((p) => p.id === plannerId) || null;
    return { activePlannerId: plannerId, planner };
  }),

  deletePlanner: (plannerId) => set((state) => {
    const nextPlanners = state.planners.filter((p) => p.id !== plannerId);
    const nextActiveId = state.activePlannerId === plannerId ? nextPlanners[0]?.id || null : state.activePlannerId;
    const nextPlanner = nextPlanners.find((p) => p.id === nextActiveId) || null;
    return { planners: nextPlanners, activePlannerId: nextActiveId, planner: nextPlanner };
  }),

  // Pomodoro
  pomodoroProfiles: [
    { id: 'pomodoro-1', name: 'Reading Focus', focusMinutes: 25, breakMinutes: 5 },
    { id: 'pomodoro-2', name: 'Deep Study', focusMinutes: 45, breakMinutes: 10 },
    { id: 'pomodoro-3', name: 'Hifz Sprint', focusMinutes: 15, breakMinutes: 3 },
  ],
  activePomodoroProfileId: 'pomodoro-1',
  pomodoroHistory: [],
  pomodoroMode: 'focus',
  pomodoroSound: 'allahu-akbar',
  pomodoroAutoStartBreaks: false,
  pomodoroAutoStartFocus: false,
  pomodoroDailyGoal: 4,
  pomodoroIsRunning: false,
  pomodoroSecondsLeft: 25 * 60,
  pomodoroCompletedFocusCount: 0,
  showGlobalPomodoro: false,

  togglePomodoroRunning: () => set((state) => ({
    pomodoroIsRunning: !state.pomodoroIsRunning,
    showGlobalPomodoro: !state.pomodoroIsRunning ? true : state.showGlobalPomodoro,
  })),

  resetPomodoroSession: () => set((state) => {
    const profile = state.pomodoroProfiles.find((p) => p.id === state.activePomodoroProfileId);
    return {
      pomodoroIsRunning: false,
      pomodoroSecondsLeft: (profile?.focusMinutes || 25) * 60,
    };
  }),

  tickPomodoro: () => {
    const state = get();
    if (!state.pomodoroIsRunning) return;

    if (state.pomodoroSecondsLeft <= 1) {
      const profile = state.pomodoroProfiles.find((p) => p.id === state.activePomodoroProfileId);
      const duration = (state.pomodoroMode === 'focus' ? profile?.focusMinutes : profile?.breakMinutes || 5) * 60;
      const nextMode = state.pomodoroMode === 'focus' ? 'break' : 'focus';

      set({
        pomodoroHistory: [...state.pomodoroHistory, {
          date: new Date().toISOString().split('T')[0],
          duration,
          mode: state.pomodoroMode,
          completedAt: Date.now(),
        }].slice(-500),
        pomodoroMode: nextMode,
        pomodoroIsRunning: state.pomodoroMode === 'focus' ? state.pomodoroAutoStartBreaks : state.pomodoroAutoStartFocus,
        pomodoroSecondsLeft: nextMode === 'focus' ? (profile?.focusMinutes || 25) * 60 : (profile?.breakMinutes || 5) * 60,
        pomodoroCompletedFocusCount: state.pomodoroMode === 'focus' ? state.pomodoroCompletedFocusCount + 1 : state.pomodoroCompletedFocusCount,
      });
      return;
    }

    set({ pomodoroSecondsLeft: state.pomodoroSecondsLeft - 1 });
  },

  // Cloud sync
  lastSyncAt: 0,
  setLastSyncAt: (timestamp) => set({ lastSyncAt: timestamp }),

  // Persist to MMKV
  _persist: () => {
    const state = get();
    const syncableState = {
      theme: state.theme,
      translationId: state.translationId,
      reciterId: state.reciterId,
      fontSize: state.fontSize,
      translationFontSize: state.translationFontSize,
      readingMode: state.readingMode,
      tajweedEnabled: state.tajweedEnabled,
      tafsirId: state.tafsirId,
      bookmarks: state.bookmarks,
      memorizedAyahs: state.memorizedAyahs,
      memorizedSurahs: state.memorizedSurahs,
      collections: state.collections,
      recentlyRead: state.recentlyRead,
      readingSessions: state.readingSessions,
      planners: state.planners,
      activePlannerId: state.activePlannerId,
      pomodoroProfiles: state.pomodoroProfiles,
      activePomodoroProfileId: state.activePomodoroProfileId,
      pomodoroHistory: state.pomodoroHistory,
      pomodoroMode: state.pomodoroMode,
      pomodoroSound: state.pomodoroSound,
      pomodoroAutoStartBreaks: state.pomodoroAutoStartBreaks,
      pomodoroAutoStartFocus: state.pomodoroAutoStartFocus,
      pomodoroDailyGoal: state.pomodoroDailyGoal,
      pomodoroCompletedFocusCount: state.pomodoroCompletedFocusCount,
      audioSettings: state.audioSettings,
      lastSyncAt: state.lastSyncAt,
    };
    storage.set('quran-app-storage', JSON.stringify(syncableState));
  },
}));

// Auto-persist on state changes
useAppStore.subscribe(() => {
  useAppStore.getState()._persist();
});
```

---

## Step 5: App Routing (Expo Router)

### `app/_layout.jsx`
```jsx
import { useEffect } from 'react';
import { Stack } from 'expo-router';
import { StatusBar } from 'expo-status-bar';
import { GestureHandlerRootView } from 'react-native-gesture-handler';
import { useAppStore } from '../src/store/useAppStore';
import { initDatabase } from '../src/services/database';
import '../src/global.css';

export default function RootLayout() {
  const theme = useAppStore((state) => state.theme);

  useEffect(() => {
    initDatabase().catch((err) => console.error('DB init error:', err));
  }, []);

  return (
    <GestureHandlerRootView style={{ flex: 1 }}>
      <StatusBar style={theme === 'dark' ? 'light' : 'dark'} />
      <Stack screenOptions={{ headerShown: false }}>
        <Stack.Screen name="(tabs)" options={{ headerShown: false }} />
      </Stack>
    </GestureHandlerRootView>
  );
}
```

### `app/(tabs)/_layout.jsx`
```jsx
import { Tabs } from 'expo-router';
import { BookOpen, Bookmark, Library, Calendar, BarChart3 } from 'lucide-react-native';
import { useAppStore } from '../../src/store/useAppStore';
import { Platform } from 'react-native';

export default function TabLayout() {
  const theme = useAppStore((state) => state.theme);
  const isDark = theme === 'dark';

  const tabIcon = (Icon, focused) => (
    <Icon
      size={24}
      color={focused ? '#009688' : isDark ? '#888' : '#666'}
    />
  );

  return (
    <Tabs
      screenOptions={{
        headerShown: false,
        tabBarActiveTintColor: '#009688',
        tabBarInactiveTintColor: isDark ? '#888' : '#666',
        tabBarStyle: {
          backgroundColor: isDark ? '#1a1a2e' : '#fff',
          borderTopColor: isDark ? '#333' : '#e0e0e0',
          paddingBottom: Platform.OS === 'ios' ? 20 : 8,
          paddingTop: 8,
          height: Platform.OS === 'ios' ? 88 : 68,
        },
        tabBarLabelStyle: {
          fontSize: 11,
          fontWeight: '600',
        },
      }}
    >
      <Tabs.Screen
        name="index"
        options={{
          title: 'Home',
          tabBarIcon: ({ focused }) => tabIcon(BookOpen, focused),
        }}
      />
      <Tabs.Screen
        name="memorize/index"
        options={{
          title: 'Memorize',
          tabBarIcon: ({ focused }) => tabIcon(Bookmark, focused),
        }}
      />
      <Tabs.Screen
        name="library/index"
        options={{
          title: 'Library',
          tabBarIcon: ({ focused }) => tabIcon(Library, focused),
        }}
      />
      <Tabs.Screen
        name="planner/index"
        options={{
          title: 'Planner',
          tabBarIcon: ({ focused }) => tabIcon(Calendar, focused),
        }}
      />
      <Tabs.Screen
        name="progress/index"
        options={{
          title: 'Progress',
          tabBarIcon: ({ focused }) => tabIcon(BarChart3, focused),
        }}
      />
      <Tabs.Screen
        name="surah/[id]"
        options={{
          href: null,
          headerShown: false,
        }}
      />
    </Tabs>
  );
}
```

### `app/(tabs)/index.jsx` (Home)
```jsx
import { useState, useEffect } from 'react';
import {
  View, Text, FlatList, TouchableOpacity,
  SafeAreaView, ActivityIndicator, TextInput
} from 'react-native';
import { useRouter } from 'expo-router';
import { Search, Moon, Sun, Settings } from 'lucide-react-native';
import { useAppStore } from '../../src/store/useAppStore';
import { getAllChapters } from '../../src/services/database';

export default function Home() {
  const router = useRouter();
  const theme = useAppStore((state) => state.theme);
  const toggleTheme = useAppStore((state) => state.toggleTheme);
  const recentlyRead = useAppStore((state) => state.recentlyRead);

  const [chapters, setChapters] = useState([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');

  useEffect(() => {
    getAllChapters()
      .then((data) => setChapters(data))
      .catch((err) => console.error(err))
      .finally(() => setLoading(false));
  }, []);

  const filtered = chapters.filter((c) =>
    c.name_simple.toLowerCase().includes(search.toLowerCase()) ||
    c.name_arabic.includes(search) ||
    String(c.id).includes(search)
  );

  const isDark = theme === 'dark';

  return (
    <SafeAreaView className={`flex-1 ${isDark ? 'bg-gray-950' : 'bg-gray-50'}`}>
      {/* Header */}
      <View className={`px-4 pt-4 pb-3 ${isDark ? 'bg-gray-900' : 'bg-white'}`}>
        <View className="flex-row items-center justify-between mb-4">
          <Text className={`text-2xl font-bold ${isDark ? 'text-white' : 'text-gray-900'}`}>
            The Noble Quran
          </Text>
          <View className="flex-row gap-3">
            <TouchableOpacity onPress={toggleTheme}>
              {isDark ? <Sun size={22} color="#fff" /> : <Moon size={22} color="#333" />}
            </TouchableOpacity>
          </View>
        </View>

        {/* Search */}
        <View className={`flex-row items-center px-3 py-2 rounded-xl ${isDark ? 'bg-gray-800' : 'bg-gray-100'}`}>
          <Search size={18} color={isDark ? '#888' : '#666'} />
          <TextInput
            className={`flex-1 ml-2 text-base ${isDark ? 'text-white' : 'text-gray-900'}`}
            placeholder="Search surah..."
            placeholderTextColor={isDark ? '#666' : '#999'}
            value={search}
            onChangeText={setSearch}
          />
        </View>
      </View>

      {/* Recently Read */}
      {recentlyRead.length > 0 && !search && (
        <View className={`mx-4 mt-4 p-3 rounded-xl ${isDark ? 'bg-gray-900' : 'bg-white'}`}>
          <Text className={`text-sm font-semibold mb-2 ${isDark ? 'text-gray-400' : 'text-gray-500'}`}>
            Recently Read
          </Text>
          <View className="flex-row flex-wrap gap-2">
            {recentlyRead.map((item) => (
              <TouchableOpacity
                key={item.chapterId}
                className={`px-3 py-1.5 rounded-lg ${isDark ? 'bg-primary-900' : 'bg-primary-50'}`}
                onPress={() => router.push(`/(tabs)/surah/${item.chapterId}`)}
              >
                <Text className={`text-sm ${isDark ? 'text-primary-200' : 'text-primary-700'}`}>
                  {item.chapterName}
                </Text>
              </TouchableOpacity>
            ))}
          </View>
        </View>
      )}

      {/* Surah List */}
      {loading ? (
        <View className="flex-1 items-center justify-center">
          <ActivityIndicator size="large" color="#009688" />
        </View>
      ) : (
        <FlatList
          data={filtered}
          keyExtractor={(item) => String(item.id)}
          className="flex-1 px-4 pt-3"
          renderItem={({ item }) => (
            <TouchableOpacity
              className={`flex-row items-center p-4 mb-2 rounded-xl ${isDark ? 'bg-gray-900' : 'bg-white'}`}
              onPress={() => router.push(`/(tabs)/surah/${item.id}`)}
            >
              <View className={`w-10 h-10 rounded-lg items-center justify-center mr-3 ${isDark ? 'bg-primary-900' : 'bg-primary-50'}`}>
                <Text className={`text-sm font-bold ${isDark ? 'text-primary-300' : 'text-primary-700'}`}>
                  {item.id}
                </Text>
              </View>
              <View className="flex-1">
                <Text className={`text-base font-semibold ${isDark ? 'text-white' : 'text-gray-900'}`}>
                  {item.name_simple}
                </Text>
                <Text className={`text-xs ${isDark ? 'text-gray-500' : 'text-gray-400'}`}>
                  {item.verses_count} verses • {item.revelation_place === 'meccan' ? 'Meccan' : 'Medinan'}
                </Text>
              </View>
              <Text className="text-xl font-uthmani text-primary-600 ml-2">
                {item.name_arabic}
              </Text>
            </TouchableOpacity>
          )}
        />
      )}
    </SafeAreaView>
  );
}
```

### `app/(tabs)/surah/[id].jsx` (Surah Reader)
```jsx
import { useState, useEffect, useCallback } from 'react';
import {
  View, Text, FlatList, TouchableOpacity,
  SafeAreaView, ActivityIndicator, Share
} from 'react-native';
import { useLocalSearchParams, useRouter } from 'expo-router';
import { ArrowLeft, Bookmark, Share2, Settings } from 'lucide-react-native';
import { useAppStore } from '../../../src/store/useAppStore';
import { getChapter, getVersesWithTranslation } from '../../../src/services/database';

const BATCH_SIZE = 20;

export default function SurahReader() {
  const { id } = useLocalSearchParams();
  const router = useRouter();
  const chapterId = parseInt(id);

  const theme = useAppStore((state) => state.theme);
  const translationId = useAppStore((state) => state.translationId);
  const readingMode = useAppStore((state) => state.readingMode);
  const tajweedEnabled = useAppStore((state) => state.tajweedEnabled);
  const fontSize = useAppStore((state) => state.fontSize);
  const toggleBookmark = useAppStore((state) => state.toggleBookmark);
  const bookmarks = useAppStore((state) => state.bookmarks);
  const addRecentlyRead = useAppStore((state) => state.addRecentlyRead);

  const [chapter, setChapter] = useState(null);
  const [verses, setVerses] = useState([]);
  const [loading, setLoading] = useState(true);
  const [offset, setOffset] = useState(0);
  const [hasMore, setHasMore] = useState(true);

  const isDark = theme === 'dark';
  const arabicFontSize = [16, 20, 24, 28][fontSize - 1] || 22;

  const loadVerses = useCallback(async (currentOffset = 0) => {
    const data = await getVersesWithTranslation(chapterId, translationId, BATCH_SIZE, currentOffset);
    if (data.length < BATCH_SIZE) setHasMore(false);
    setVerses((prev) => currentOffset === 0 ? data : [...prev, ...data]);
    setOffset(currentOffset + data.length);
  }, [chapterId, translationId]);

  useEffect(() => {
    Promise.all([
      getChapter(chapterId),
      loadVerses(0),
    ]).then(([ch]) => {
      setChapter(ch);
      addRecentlyRead(ch.id, ch.name_simple);
    }).finally(() => setLoading(false));
  }, [chapterId, loadVerses, addRecentlyRead]);

  const isBookmarked = (verseKey) => bookmarks.some((b) => b.verseKey === verseKey);

  const handleShare = async (verse) => {
    await Share.share({
      message: `${verse.text_uthmani}\n\n${verse.translation_text || ''}\n— ${chapter?.name_simple} (${verse.verse_key})`,
    });
  };

  if (loading) {
    return (
      <SafeAreaView className={`flex-1 items-center justify-center ${isDark ? 'bg-gray-950' : 'bg-gray-50'}`}>
        <ActivityIndicator size="large" color="#009688" />
      </SafeAreaView>
    );
  }

  return (
    <SafeAreaView className={`flex-1 ${isDark ? 'bg-gray-950' : 'bg-gray-50'}`}>
      {/* Header */}
      <View className={`flex-row items-center justify-between px-4 py-3 ${isDark ? 'bg-gray-900' : 'bg-white'}`}>
        <TouchableOpacity onPress={() => router.back()} className="p-2">
          <ArrowLeft size={22} color={isDark ? '#fff' : '#333'} />
        </TouchableOpacity>
        <View className="items-center">
          <Text className={`text-lg font-bold ${isDark ? 'text-white' : 'text-gray-900'}`}>
            {chapter?.name_simple}
          </Text>
          <Text className="text-base text-primary-600 font-uthmani">
            {chapter?.name_arabic}
          </Text>
        </View>
        <View className="flex-row gap-2">
          <TouchableOpacity className="p-2">
            <Settings size={20} color={isDark ? '#fff' : '#333'} />
          </TouchableOpacity>
        </View>
      </View>

      {/* Bismillah */}
      {chapterId !== 1 && chapterId !== 9 && (
        <View className="items-center py-6">
          <Text className="text-2xl text-primary-600 font-uthmani">
            بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ
          </Text>
        </View>
      )}

      {/* Verses */}
      <FlatList
        data={verses}
        keyExtractor={(item) => item.verse_key}
        className="flex-1 px-4"
        renderItem={({ item }) => (
          <View className={`py-4 border-b ${isDark ? 'border-gray-800' : 'border-gray-100'}`}>
            <View className="flex-row items-start gap-3">
              <View className={`w-8 h-8 rounded-full items-center justify-center ${isDark ? 'bg-primary-900' : 'bg-primary-50'}`}>
                <Text className={`text-xs font-bold ${isDark ? 'text-primary-300' : 'text-primary-700'}`}>
                  {item.verse_number}
                </Text>
              </View>
              <View className="flex-1">
                <Text
                  className={`text-right leading-10 ${isDark ? 'text-white' : 'text-gray-900'}`}
                  style={{ fontSize: arabicFontSize, fontFamily: 'Uthmani' }}
                >
                  {item.text_uthmani} ﴿{item.verse_number}﴾
                </Text>
                {!readingMode && item.translation_text && (
                  <Text className={`mt-2 text-base leading-6 ${isDark ? 'text-gray-400' : 'text-gray-600'}`}>
                    {item.translation_text}
                  </Text>
                )}
              </View>
            </View>

            {/* Actions */}
            <View className="flex-row gap-3 mt-2 ml-11">
              <TouchableOpacity
                onPress={() => toggleBookmark(item.verse_key, chapter?.name_simple, chapterId)}
              >
                <Bookmark
                  size={16}
                  color={isBookmarked(item.verse_key) ? '#009688' : isDark ? '#666' : '#999'}
                  fill={isBookmarked(item.verse_key) ? '#009688' : 'none'}
                />
              </TouchableOpacity>
              <TouchableOpacity onPress={() => handleShare(item)}>
                <Share2 size={16} color={isDark ? '#666' : '#999'} />
              </TouchableOpacity>
            </View>
          </View>
        )}
        onEndReached={() => hasMore && loadVerses(offset)}
        onEndReachedThreshold={0.3}
        ListFooterComponent={hasMore ? (
          <View className="py-4 items-center">
            <ActivityIndicator color="#009688" />
          </View>
        ) : null}
      />
    </SafeAreaView>
  );
}
```

### Placeholder tab screens

### `app/(tabs)/memorize/index.jsx`
```jsx
import { View, Text, SafeAreaView } from 'react-native';
import { useAppStore } from '../../../src/store/useAppStore';

export default function MemorizeIndex() {
  const theme = useAppStore((state) => state.theme);
  const isDark = theme === 'dark';

  return (
    <SafeAreaView className={`flex-1 items-center justify-center ${isDark ? 'bg-gray-950' : 'bg-gray-50'}`}>
      <Text className={`text-xl ${isDark ? 'text-white' : 'text-gray-900'}`}>Memorization</Text>
      <Text className={`text-sm mt-2 ${isDark ? 'text-gray-500' : 'text-gray-400'}`}>Coming soon</Text>
    </SafeAreaView>
  );
}
```

### `app/(tabs)/library/index.jsx`
```jsx
import { View, Text, SafeAreaView } from 'react-native';
import { useAppStore } from '../../../src/store/useAppStore';

export default function Library() {
  const theme = useAppStore((state) => state.theme);
  const isDark = theme === 'dark';

  return (
    <SafeAreaView className={`flex-1 items-center justify-center ${isDark ? 'bg-gray-950' : 'bg-gray-50'}`}>
      <Text className={`text-xl ${isDark ? 'text-white' : 'text-gray-900'}`}>Library</Text>
      <Text className={`text-sm mt-2 ${isDark ? 'text-gray-500' : 'text-gray-400'}`}>Coming soon</Text>
    </SafeAreaView>
  );
}
```

### `app/(tabs)/planner/index.jsx`
```jsx
import { View, Text, SafeAreaView } from 'react-native';
import { useAppStore } from '../../../src/store/useAppStore';

export default function Planner() {
  const theme = useAppStore((state) => state.theme);
  const isDark = theme === 'dark';

  return (
    <SafeAreaView className={`flex-1 items-center justify-center ${isDark ? 'bg-gray-950' : 'bg-gray-50'}`}>
      <Text className={`text-xl ${isDark ? 'text-white' : 'text-gray-900'}`}>Planner</Text>
      <Text className={`text-sm mt-2 ${isDark ? 'text-gray-500' : 'text-gray-400'}`}>Coming soon</Text>
    </SafeAreaView>
  );
}
```

### `app/(tabs)/progress/index.jsx`
```jsx
import { View, Text, SafeAreaView } from 'react-native';
import { useAppStore } from '../../../src/store/useAppStore';

export default function Progress() {
  const theme = useAppStore((state) => state.theme);
  const isDark = theme === 'dark';

  return (
    <SafeAreaView className={`flex-1 items-center justify-center ${isDark ? 'bg-gray-950' : 'bg-gray-50'}`}>
      <Text className={`text-xl ${isDark ? 'text-white' : 'text-gray-900'}`}>Progress</Text>
      <Text className={`text-sm mt-2 ${isDark ? 'text-gray-500' : 'text-gray-400'}`}>Coming soon</Text>
    </SafeAreaView>
  );
}
```

---

## Step 6: Appwrite Sync Service

### `src/services/appwrite.js`
```js
import { Client, Account, Databases, Query } from 'appwrite';

const endpoint = process.env.EXPO_PUBLIC_APPWRITE_ENDPOINT;
const projectId = process.env.EXPO_PUBLIC_APPWRITE_PROJECT_ID;
const databaseId = process.env.EXPO_PUBLIC_APPWRITE_DATABASE_ID || 'quran_db';
const collectionId = process.env.EXPO_PUBLIC_APPWRITE_USER_DATA_COLLECTION_ID || 'user_sync';

export const client = new Client();
if (endpoint && projectId) {
  client.setEndpoint(endpoint).setProject(projectId);
}

export const account = new Account(client);
export const databases = new Databases(client);

export const authService = {
  async getCurrentUser() {
    try { return await account.get(); }
    catch { return null; }
  },
  async login(email, password) {
    return await account.createEmailPasswordSession(email, password);
  },
  async register(email, password, name) {
    return await account.create('unique()', email, password, name);
  },
  async logout() {
    return await account.deleteSession('current');
  },
};

export const syncService = {
  async pushState(userId, stateData) {
    const result = await databases.listDocuments(databaseId, collectionId, [
      Query.equal('userId', userId),
    ]);

    const payload = { userId, stateData: JSON.stringify(stateData) };

    let updatedDoc;
    if (result.documents.length > 0) {
      updatedDoc = await databases.updateDocument(databaseId, collectionId, result.documents[0].$id, payload);
    } else {
      updatedDoc = await databases.createDocument(databaseId, collectionId, 'unique()', payload);
    }
    return { updatedAt: new Date(updatedDoc.$updatedAt).getTime() };
  },

  async pullState(userId) {
    const result = await databases.listDocuments(databaseId, collectionId, [
      Query.equal('userId', userId),
    ]);

    if (result.documents.length > 0) {
      return {
        state: JSON.parse(result.documents[0].stateData),
        updatedAt: new Date(result.documents[0].$updatedAt).getTime(),
      };
    }
    return null;
  },
};
```

---

## Step 7: `.env` file

### `.env`
```
EXPO_PUBLIC_APPWRITE_ENDPOINT=https://cloud.appwrite.io/v1
EXPO_PUBLIC_APPWRITE_PROJECT_ID=your-project-id
EXPO_PUBLIC_APPWRITE_DATABASE_ID=quran_db
EXPO_PUBLIC_APPWRITE_USER_DATA_COLLECTION_ID=user_sync
```

---

## Execution Order

1. Write all config files (tailwind, babel, metro, app.json, global.css)
2. Write the DB generation script
3. Run `node scripts/generate-quran-db.js` to create the SQLite DB
4. Write all source files (database service, store, app routing, screens)
5. Create `.env` from `.env.example`
6. Download Arabic fonts to `assets/fonts/`
7. Run `npx expo start` to verify

---

## Font Setup

Download these free fonts and place in `assets/fonts/`:
- **Uthmani**: `paka_v2.ttf` (from https://github.com/nawawy/react-native-quran)
- Or use any Uthmani-style Arabic font

Then register in `app/_layout.jsx`:
```js
import { useFonts } from 'expo-font';

const [fontsLoaded] = useFonts({
  Uthmani: require('../assets/fonts/paka_v2.ttf'),
});
```

---

## Build Commands

```bash
# Development
npx expo start

# Build Android APK
npx expo run:android --variant release

# Build for Play Store (AAB)
eas build --platform android --profile production

# Build for iOS (requires macOS)
eas build --platform ios --profile production
```
