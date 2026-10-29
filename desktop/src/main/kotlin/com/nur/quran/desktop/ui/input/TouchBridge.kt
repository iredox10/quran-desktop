package com.nur.quran.desktop.ui.input

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.awt.Robot
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Linux touch-to-pointer bridge.
 *
 * ## Why this exists
 *
 * On a Wayland session (Hyprland) a Java/Swing/Compose app runs as an
 * **Xwayland** client. Java AWT consumes only core *pointer* (button) events
 * and has no XI2 touch support (JDK 17 ships no `sun.awt.X11.XInput2` /
 * `XTouch` classes). Xwayland *can* expose the touchscreen as a
 * pointer-emulating device, but only when the compositor's libinput can open
 * the touchscreen's `/dev/input/eventN` node — which requires membership of
 * the `input` group.
 *
 * When that membership is missing, the compositor produces no touch events at
 * all: the pointer never moves and no click is generated. No UI code can
 * recover from that, because the events do not exist.
 *
 * This bridge closes the gap from the app side: it opens the touchscreen's
 * evdev node directly and synthesises pointer motion plus button presses via
 * [Robot], which AWT delivers exactly like a real mouse. Every existing
 * `clickable` and drag handler then works with a finger.
 *
 * ## Requirements
 *
 * The evdev node is `root:input` mode `0660`, so the user must be in the
 * `input` group (see `docs/TOUCH_SUPPORT.md`). If the node cannot be opened
 * the bridge stays dormant and the app behaves normally — it never crashes or
 * blocks startup.
 *
 * Multi-touch is deliberately not emulated: Compose Desktop cannot consume
 * touch events, so gestures are approximated with single-pointer taps and
 * drags.
 */
object TouchBridge {

    private const val EV_SYN = 0x00
    private const val EV_KEY = 0x01
    private const val EV_ABS = 0x03
    private const val SYN_REPORT = 0
    private const val ABS_MT_POSITION_X = 0x35
    private const val ABS_MT_POSITION_Y = 0x36
    private const val ABS_MT_TRACKING_ID = 0x39
    private const val BTN_TOUCH = 0x14a
    private const val BTN_TOOL_FINGER = 0x145

    /** evdev `input_event`: timeval(16) + type(2) + code(2) + value(4). */
    private const val EVENT_SIZE = 24

    private var job: Job? = null

    /** True while the bridge is pumping events. */
    @Volatile
    var active: Boolean = false
        private set

    /** Human-readable reason the bridge is inactive, for diagnostics/UI. */
    @Volatile
    var status: String = "not started"
        private set

    /**
     * Starts the bridge when a touchscreen node is readable.
     *
     * @param scale maps raw digitiser coordinates to screen pixels.
     * @return true when the bridge started.
     */
    fun start(
        scope: CoroutineScope,
        screenWidth: Int,
        screenHeight: Int,
        screenTop: Int = 0,
        rawMaxX: Int = 0,
        rawMaxY: Int = 0,
    ): Boolean {
        if (job?.isActive == true) return true
        val node = findTouchscreenNode()
        if (node == null) {
            status = "touchscreen node not readable (input group)"
            return false
        }
        val maxX = if (rawMaxX > 0) rawMaxX else screenWidth
        val maxY = if (rawMaxY > 0) rawMaxY else screenHeight
        job = scope.launch(Dispatchers.IO) {
            try {
                status = "active"
                active = true
                pump(node, screenWidth, screenHeight, screenTop, maxX, maxY)
            } catch (e: Exception) {
                status = "stopped: ${e.javaClass.simpleName}"
            } finally {
                active = false
            }
        }
        return true
    }

    /** Stops the bridge. */
    fun stop() {
        job?.cancel()
        job = null
        active = false
        status = "stopped"
    }

    /**
     * Locates the touchscreen's evdev node.
     *
     * A touchscreen is an absolute pointing device: `/proc/bus/input/devices`
     * marks it with `PROP=2` (`INPUT_PROP_DIRECT`). Its `Handlers=` line names
     * the event node, e.g. `mouse2 event8`.
     */
    private fun findTouchscreenNode(): File? {
        val proc = File("/proc/bus/input/devices")
        if (!proc.isFile) return null
        for (block in proc.readText().split("\n\n")) {
            if (!block.contains("PROP=2")) continue
            if (!block.contains("ABS=")) continue
            val handlers = Regex("H: Handlers=([^\\n]+)").find(block)?.groupValues?.get(1)
                ?: continue
            // Prefer the plain "eventN" node over a "mouseN" alias.
            val event = Regex("(event\\d+)").find(handlers)?.groupValues?.get(1) ?: continue
            val node = File("/dev/input/$event")
            if (node.canRead()) return node
        }
        return null
    }

    /** Reads evdev events and synthesises pointer motion and clicks. */
    private suspend fun pump(
        node: File,
        screenWidth: Int,
        screenHeight: Int,
        screenTop: Int,
        maxX: Int,
        maxY: Int,
    ) {
        val robot = Robot()
        var x = 0
        var y = 0
        var touching = false
        var pressed = false
        var lastX = Int.MIN_VALUE
        var lastY = Int.MIN_VALUE

        // FileInputStream is used directly: evdev nodes must be read with
        // blocking semantics (File.inputStream() cannot be used on a device
        // node, and a non-blocking read would spin).
        FileInputStream(node).use { stream ->
            val chunk = ByteArray(EVENT_SIZE)
            val buffer = ByteBuffer.allocate(EVENT_SIZE).order(ByteOrder.LITTLE_ENDIAN)
            while (job?.isActive == true) {
                val read = try {
                    stream.read(chunk)
                } catch (_: Exception) {
                    -1
                }
                if (read < EVENT_SIZE) {
                    delay(10)
                    continue
                }
                buffer.clear()
                buffer.put(chunk, 0, EVENT_SIZE)
                buffer.flip()
                val type = buffer.getShort(16).toInt() and 0xffff
                val code = buffer.getShort(18).toInt() and 0xffff
                val value = buffer.getInt(20)

                when (type) {
                    EV_ABS -> when (code) {
                        ABS_MT_POSITION_X -> x = value
                        ABS_MT_POSITION_Y -> y = value
                        ABS_MT_TRACKING_ID -> if (value >= 0) {
                            touching = true
                        }
                    }
                    EV_KEY -> when (code) {
                        BTN_TOUCH, BTN_TOOL_FINGER -> if (value == 1) {
                            if (touching && !pressed) {
                                lastX = Int.MIN_VALUE
                                lastY = Int.MIN_VALUE
                                movePointer(robot, x, y, screenWidth, screenHeight, screenTop, maxX, maxY)
                                robot.mousePress(java.awt.event.InputEvent.BUTTON1_DOWN_MASK)
                                pressed = true
                            }
                        } else if (value == 0) {
                            if (pressed) {
                                robot.mouseRelease(java.awt.event.InputEvent.BUTTON1_DOWN_MASK)
                                pressed = false
                            }
                            touching = false
                        }
                    }
                    EV_SYN -> if (code == SYN_REPORT && touching && pressed) {
                        if (lastX != x || lastY != y) {
                            movePointer(robot, x, y, screenWidth, screenHeight, screenTop, maxX, maxY)
                            lastX = x
                            lastY = y
                        }
                    }
                }
            }
        }
    }

    /** Maps a digitiser coordinate to screen pixels and moves the pointer. */
    private fun movePointer(
        robot: Robot,
        rawX: Int,
        rawY: Int,
        screenWidth: Int,
        screenHeight: Int,
        screenTop: Int,
        maxX: Int,
        maxY: Int,
    ) {
        val px = scale(rawX, maxX, screenWidth)
        val py = scale(rawY, maxY, screenHeight) + screenTop
        robot.mouseMove(px.coerceIn(0, screenWidth - 1), py.coerceIn(screenTop, screenTop + screenHeight - 1))
    }

    /** Scales a raw digitiser coordinate onto the screen axis. */
    private fun scale(raw: Int, rawMax: Int, screenExtent: Int): Int {
        if (rawMax <= 0) return raw
        return ((raw.toLong() * screenExtent) / rawMax).toInt()
    }
}
