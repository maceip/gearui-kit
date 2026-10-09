package com.gearui.desktop

/**
 * Which desktop frame the host is.
 *
 * Phone targets report [Other]. A wide window on [Other] still uses the Mac
 * skin: a neutral desktop frame, not a claim that the device is a Mac.
 * GearUI does not open a native window; caption buttons call [DesktopWindowCommands],
 * which is a no-op until a host handles them.
 */
internal enum class DesktopPlatform {
    Mac,
    Windows,
    Other,
}

internal expect fun desktopPlatform(): DesktopPlatform

/** Minimize, maximize and close. No-op where the host owns the window. */
internal expect object DesktopWindowCommands {
    fun minimize()
    fun maximize()
    fun close()
}
