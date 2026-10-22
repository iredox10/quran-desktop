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

## Framework ceiling (why the app cannot fix this itself)

Compose Multiplatform desktop has **no touch-event API**. From
[JetBrains/compose-multiplatform#1555](https://github.com/JetBrains/compose-multiplatform/issues/1555)
(a maintainer): *"There are no immediate plans for it."* The framework consumes
AWT mouse events only.

Therefore "touch support" on desktop == OS touch->pointer emulation + adequately
sized hit targets + drag-gesture handlers. This app implements the last two:

- `ui/input/TouchInput.kt` — `touchTarget()` (48dp hit area via
  `minimumInteractiveComponentSize`), `swipeToNavigate()` (64px threshold).
- App-wide 48dp hit targets on all icon buttons and small clickables.

True multi-touch (pinch-zoom, two-finger gestures) is **not possible** with
Compose Desktop.
