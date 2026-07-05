package io.github.gdict.platform

import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.platform.win32.WinDef.HWND
import com.sun.jna.win32.StdCallLibrary
import com.sun.jna.win32.W32APIOptions
import java.awt.Frame
import java.awt.Window
import java.awt.event.WindowEvent

/**
 * Windows 11 DWM backdrop helper for Mica/Acrylic effects and custom title bar.
 */
object WindowsBackdrop {

    private const val DWMWA_MICA_EFFECT = 1029
    private const val DWMWA_SYSTEMBACKDROP_TYPE = 38
    private const val DWMWA_WINDOW_CORNER_PREFERENCE = 33

    private const val SWP_NOMOVE = 0x0002
    private const val SWP_NOSIZE = 0x0001
    private const val SWP_NOZORDER = 0x0004
    private const val SWP_FRAMECHANGED = 0x0020

    private const val DWMSBT_MAINWINDOW = 2  // Mica
    private const val DWMSBT_TRANSIENTWINDOW = 3  // Acrylic
    private const val DWMSBT_NONE = 1

    private const val DWMWCP_ROUND = 2

    private val dwmApi: DwmApi? = try {
        Native.load("dwmapi", DwmApi::class.java, W32APIOptions.DEFAULT_OPTIONS)
    } catch (_: Throwable) {
        null
    }

    private val user32Ex: User32Ex? = try {
        Native.load("user32", User32Ex::class.java, W32APIOptions.DEFAULT_OPTIONS)
    } catch (_: Throwable) {
        null
    }

    private interface DwmApi : StdCallLibrary {
        fun DwmSetWindowAttribute(hwnd: HWND, dwAttribute: Int, pvAttribute: IntArray, cbAttribute: Int): Int
    }

    private interface User32Ex : StdCallLibrary {
        fun SetWindowPos(hWnd: HWND, hWndInsertAfter: HWND?, x: Int, y: Int, cx: Int, cy: Int, uFlags: Int): Boolean
    }

    private fun Window.getHwnd(): HWND? = try {
        val peerField = java.awt.Component::class.java.getDeclaredField("peer")
        peerField.isAccessible = true
        val peer = peerField.get(this) ?: return null
        val hwndMethod = try {
            peer::class.java.getDeclaredMethod("getHWnd")
        } catch (_: NoSuchMethodException) {
            peer::class.java.getDeclaredMethod("getHWND")
        }
        hwndMethod.isAccessible = true
        val raw = hwndMethod.invoke(peer)
        val value = when (raw) {
            is Long -> raw
            is Number -> raw.toLong()
            is HWND -> Pointer.nativeValue(raw.pointer)
            else -> raw.toString().toLongOrNull() ?: 0L
        }
        if (value == 0L) null else HWND(Pointer(value))
    } catch (_: Throwable) {
        null
    }

    fun minimize(window: Window) {
        if (window is Frame) {
            window.extendedState = Frame.ICONIFIED
        }
    }

    fun toggleMaximize(window: Window) {
        if (window !is Frame) return
        window.extendedState = if (window.extendedState == Frame.MAXIMIZED_BOTH) {
            Frame.NORMAL
        } else {
            Frame.MAXIMIZED_BOTH
        }
    }

    fun close(window: Window) {
        window.dispatchEvent(WindowEvent(window, WindowEvent.WINDOW_CLOSING))
    }

    fun applyMica(window: Window) {
        val hwnd = window.getHwnd() ?: return
        val dwm = dwmApi ?: return

        // Round corners
        dwm.DwmSetWindowAttribute(hwnd, DWMWA_WINDOW_CORNER_PREFERENCE, intArrayOf(DWMWCP_ROUND), 4)

        // Prefer DWMWA_SYSTEMBACKDROP_TYPE for Windows 11 22H2+
        val backdropResult = dwm.DwmSetWindowAttribute(hwnd, DWMWA_SYSTEMBACKDROP_TYPE, intArrayOf(DWMSBT_MAINWINDOW), 4)
        if (backdropResult != 0) {
            // Fallback to DWMWA_MICA_EFFECT for older Windows 11
            dwm.DwmSetWindowAttribute(hwnd, DWMWA_MICA_EFFECT, intArrayOf(1), 4)
        }

        // Refresh frame so Mica takes effect
        val u32 = user32Ex ?: return
        u32.SetWindowPos(
            hwnd, null, 0, 0, 0, 0,
            SWP_NOMOVE or SWP_NOSIZE or SWP_NOZORDER or SWP_FRAMECHANGED
        )
    }

    fun applyAcrylic(window: Window, darkMode: Boolean = false) {
        val hwnd = window.getHwnd() ?: return
        val dwm = dwmApi ?: return

        if (darkMode) {
            dwm.DwmSetWindowAttribute(hwnd, 20, intArrayOf(1), 4)
        } else {
            dwm.DwmSetWindowAttribute(hwnd, 20, intArrayOf(0), 4)
        }

        dwm.DwmSetWindowAttribute(hwnd, DWMWA_WINDOW_CORNER_PREFERENCE, intArrayOf(DWMWCP_ROUND), 4)

        val result = dwm.DwmSetWindowAttribute(hwnd, DWMWA_SYSTEMBACKDROP_TYPE, intArrayOf(DWMSBT_TRANSIENTWINDOW), 4)
        if (result != 0) {
            dwm.DwmSetWindowAttribute(hwnd, DWMWA_MICA_EFFECT, intArrayOf(1), 4)
        }
    }

    fun clearBackdrop(window: Window) {
        val hwnd = window.getHwnd() ?: return
        val dwm = dwmApi ?: return
        dwm.DwmSetWindowAttribute(hwnd, DWMWA_SYSTEMBACKDROP_TYPE, intArrayOf(DWMSBT_NONE), 4)
    }
}
