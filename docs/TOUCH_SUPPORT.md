# Touchscreen support on desktop (Linux)

## Summary

Touch input on this machine does **not** reach the app, and no amount of
Compose/UI code can change that. The failure is in the OS input stack, below
the application.

## Verified environment

| Layer | Finding |
|---|---|
| Kernel | `ELAN2513:00 04F3:2DA0` touchscreen, `PROP=2` (INPUT_PROP_DIRECT = real touchscreen), `Handlers=mouse2 event8` |
| Device nodes | `/dev/input/event8`..`event11` are `root:input` mode `0660` |
| User groups | `iredox wheel docker` — **NOT in `input` group** (`/etc/group`: `input:x:104:root`) |
| Session | Hyprland 0.55.4 (Wayland). `Hyprland` and `Xwayland` run as uid 1000, groups `10 963 1000` — also **not** in `input` |
| App | Java AWT is X11-only -> runs as an **Xwayland** client (`DISPLAY=:0`, `WAYLAND_DISPLAY=wayland-1`) |
| X server devices | `xwayland-touch:1` (id 10) is `type=Pointer`, 2 buttons, `num_touches=20` -> X **does** emulate a pointer for touch |
| JDK 17 AWT | No `sun/awt/X11/XInput`/`XInput2`/`XTouch` classes — AWT only consumes core pointer (button) events |

## Ground truth test

`xdotool getmouselocation` while touching the screen:

```
while touching:  x:500 y:400   (unchanged for 10s)
after mousemove: x:200 y:200   (mouse moves instantly)
```

Touch produces **zero** pointer events. A minimal AWT/Swing probe
(`TouchProbe.java`) received `MOUSE_PRESSED` from a synthetic `xdotool` click
but **nothing** from finger taps — proving AWT is fine and the touch event
never arrives.

## Root cause

The touchscreen is readable only by members of the `input` group, and neither
the user nor the compositor sessions are in it. `~/.config/hypr/setup-touch.sh`
runs `usermod -aG input "$USER"` and prints "log out and log back in" — that
re-login never happened, so the group change never took effect. libinput in
the session therefore cannot open the touchscreen device, so no touch events
are ever produced.

## Fix (OS level, one command)

```bash
sudo usermod -aG input "$USER"    # then log out and back in
```

Verify with `id -Gn` showing `input`, and:

```bash
libinput debug-events --device /dev/input/event8
```

## Framework ceiling & Drag-to-Scroll Mechanics

### 1. Why standard Compose Desktop never drag-scrolls with mouse or touchscreen

Compose Multiplatform desktop has **no touch-event API**. From
[JetBrains/compose-multiplatform#1555](https://github.com/JetBrains/compose-multiplatform/issues/1555)
(a maintainer): *"There are no immediate plans for it."* The framework consumes
AWT mouse events only.

In `androidx.compose.foundation.gestures.ScrollableKt`:
```kotlin
private val CanDragCalculation: (PointerInputChange) -> Boolean = { change ->
    change.type != PointerType.Mouse
}
```
`Modifier.verticalScroll`, `Modifier.horizontalScroll`, `LazyColumn`, and `LazyRow`
all delegate to `scrollable`, which hard-rejects any pointer with `PointerType.Mouse`.
Because Linux X11/Xwayland touchscreen drivers and [TouchBridge] report pointer
events through AWT, every touch arrives as `PointerType.Mouse`. Compose's built-in
scroll containers therefore completely ignore drag gestures on desktop!

### 2. The `mouseSlop` trap with standard `detectVerticalDragGestures`

Attempting to fix this by blindly applying `detectVerticalDragGestures` introduces a
critical bug: Compose Desktop assigns a tiny `mouseSlop = 0.125dp` to mouse pointers.
When a user taps the screen with a finger, natural skin deformation moves the
contact point by 1–3 pixels.
With `0.125dp` slop, Compose immediately classifies this tiny movement as an
active drag, calls `change.consume()`, and **cancels child clickables**!
As a result, verses, ayah bookmark buttons, and controls become impossible to click.

### 3. The Solution: `Modifier.dragToScroll(state, orientation)`

Implemented in [TouchInput.kt](file:///home/iredox/Desktop/personal-apps/quran-desktop-kotlin/desktop/src/main/kotlin/com/nur/quran/desktop/ui/input/TouchInput.kt):

- Uses `awaitVerticalTouchSlopOrCancellation` / `awaitHorizontalTouchSlopOrCancellation`,
  which explicitly enforce full touch slop (18dp) regardless of pointer type.
- Taps and slight finger wobbles pass unconsumed to child clickables, preserving normal clicks.
- Once touch slop is passed, it locks into drag, consumes pointer movement, and
  smoothly dispatches scroll deltas (`-delta`) to any `ScrollableState` (`LazyListState`, `ScrollState`).
- Tracks release velocity with `VelocityTracker` and performs natural inertial fling
  via `ScrollableDefaults.flingBehavior()`.
- Halts ongoing fling animations instantly when a new touch down is detected.
- Supports both `Orientation.Vertical` and `Orientation.Horizontal`.

### 4. Integration Guide Across Screens

To enable drag-scrolling on any screen:

**For LazyColumn / LazyRow:**
```kotlin
val listState = rememberLazyListState()
LazyColumn(
    state = listState,
    modifier = Modifier.fillMaxSize().dragToScroll(listState)
) { ... }
```

**For Column with verticalScroll:**
```kotlin
val scrollState = rememberScrollState()
Column(
    modifier = Modifier
        .verticalScroll(scrollState)
        .dragToScroll(scrollState)
) { ... }
```

**For Horizontal scroll containers (e.g. RecentlyReadRow):**
```kotlin
val scrollState = rememberScrollState()
Row(
    modifier = Modifier
        .horizontalScroll(scrollState)
        .dragToScroll(scrollState, orientation = Orientation.Horizontal)
) { ... }
```

Integrated screens:
- `SurahScreenDesktop.kt` (Standard verse list & continuous reading mode)
- `PageScreenDesktop.kt` (Verse list & mushaf column)
- `PlannerScreenDesktop.kt` (Intention view, dashboard LazyColumn, settings dialog)
- `PlannerReaderScreenDesktop.kt` (Verse list & mushaf view)
- `LibraryScreenDesktop.kt` (Bookmarks & collections list)
- `App.kt` (Home screen chapter directory)
- `RecentlyReadRow.kt` (Horizontal recent reads carousel)
- `HistoryScreenDesktop.kt` (Recitation history log)
- `DownloadsScreenDesktop.kt` (Reciter and translation downloads)
- `AnalyticsScreenDesktop.kt` (Reading stats and charts)
- `MemorizeScreenDesktop.kt` (Hifdh test list)
- `SaukaScreensDesktop.kt` (Khatmah creation and group session views)
- `SettingsDrawerDesktop.kt` (App settings dialog)
