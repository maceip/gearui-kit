package com.gearui.desktop

internal actual fun desktopPlatform(): DesktopPlatform = DesktopPlatform.Other

internal actual object DesktopWindowCommands {
    actual fun minimize() = Unit
    actual fun maximize() = Unit
    actual fun close() = Unit
}
