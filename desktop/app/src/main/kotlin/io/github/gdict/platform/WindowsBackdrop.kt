package io.github.gdict.platform

import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.platform.win32.WinDef.HWND
import com.sun.jna.win32.StdCallLibrary
import com.sun.jna.win32.W32APIOptions
import java.awt.Window

/**
 * Windows 11 DWM backdrop helper for Mica/Acrylic effects.
 */
object WindowsBackdrop {

    private const val DWMWA_USE_IMMERSIVE_DARK_MODE = 20
    private const val DWMWA_MICA_EFFECT = 1029
    private const val DWMWA_SYSTEMBACKDROP_TYPE = 38
    private const val DWMWA_WINDOW_CORNER_PREFERENCE = 33

    private const val DWMSBT_AUTO = 0
    private const val DWMSBT_NONE = 1
    private const val DWMSBT_MAINWINDOW = 2  // Mica
    private const val DWMSBT_TRANSIENTWINDOW = 3  // Acrylic
    private const val DWMSBT_TABBEDWINDOW = 4  // Tabbed

    private const val DWMWCP_DEFAULT = 0
    private const val DWMWCP_DONOTROUND = 1
    private const val DWMWCP_ROUND = 2
    private const val DWMWCP_ROUNDSMALL = 3

    private val dwmApi: DwmApi? = try {
        Native.load("dwmapi", DwmApi::class.java, W32APIOptions.DEFAULT_OPTIONS)
    } catch (_: Throwable) {
        null
    }

    private interface DwmApi : StdCallLibrary {
        fun DwmSetWindowAttribute(hwnd: HWND, dwAttribute: Int, pvAttribute: IntArray, cbAttribute: Int): Int
    }

    private fun Window.getHwnd(): HWND? = try {
        val peerField = java.awt.Component::class.java.getDeclaredField("peer")
        peerField.isAccessible = true
        val peer = peerField.get(this)
        if (peer == null) {
            null
        } else {
            val hwndMethod = peer::class.java.getDeclaredMethod("getHWnd")
            hwndMethod.isAccessible = true
            HWND(Pointer(hwndMethod.invoke(peer) as Long))
        }
    } catch (_: Throwable) {
        null
    }

    fun applyMica(window: Window, darkMode: Boolean = false) {
        val hwnd = window.getHwnd() ?: return
        val dwm = dwmApi ?: return

        // Enable dark mode if requested
        if (darkMode) {
            dwm.DwmSetWindowAttribute(hwnd, DWMWA_USE_IMMERSIVE_DARK_MODE, intArrayOf(1), 4)
        } else {
            dwm.DwmSetWindowAttribute(hwnd, DWMWA_USE_IMMERSIVE_DARK_MODE, intArrayOf(0), 4)
        }

        // Round corners
        dwm.DwmSetWindowAttribute(hwnd, DWMWA_WINDOW_CORNER_PREFERENCE, intArrayOf(DWMWCP_ROUND), 4)

        // Prefer DWMWA_SYSTEMBACKDROP_TYPE for Windows 11 22H2+
        val result = dwm.DwmSetWindowAttribute(hwnd, DWMWA_SYSTEMBACKDROP_TYPE, intArrayOf(DWMSBT_MAINWINDOW), 4)
        if (result != 0) {
            // Fallback to DWMWA_MICA_EFFECT for older Windows 11
            dwm.DwmSetWindowAttribute(hwnd, DWMWA_MICA_EFFECT, intArrayOf(1), 4)
        }
    }

    fun applyAcrylic(window: Window, darkMode: Boolean = false) {
        val hwnd = window.getHwnd() ?: return
        val dwm = dwmApi ?: return

        if (darkMode) {
            dwm.DwmSetWindowAttribute(hwnd, DWMWA_USE_IMMERSIVE_DARK_MODE, intArrayOf(1), 4)
        } else {
            dwm.DwmSetWindowAttribute(hwnd, DWMWA_USE_IMMERSIVE_DARK_MODE, intArrayOf(0), 4)
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
