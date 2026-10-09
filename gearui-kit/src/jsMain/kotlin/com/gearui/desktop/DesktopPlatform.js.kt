package com.gearui.desktop

private external interface JsNavigator {
    val userAgent: String
}

private external val navigator: JsNavigator

internal actual fun desktopPlatform(): DesktopPlatform {
    val agent = navigator.userAgent.lowercase()
    return when {
        "android" in agent || "iphone" in agent || "ipad" in agent -> DesktopPlatform.Other
        "windows" in agent -> DesktopPlatform.Windows
        "mac" in agent -> DesktopPlatform.Mac
        else -> DesktopPlatform.Other
    }
}

internal actual object DesktopWindowCommands {
    actual fun minimize() = Unit
    actual fun maximize() = Unit
    actual fun close() = Unit
}
