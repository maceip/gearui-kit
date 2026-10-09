package com.gearui.sample

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.gearui.App
import com.gearui.View
import com.gearui.desktop.DesktopChrome
import com.gearui.desktop.SceneKind
import com.gearui.sample.examples.runtime.MailDesktopWindow
import com.gearui.sample.pages.SettingsState
import com.gearui.sample.pages.ThemeStyle
import com.gearui.sample.theme.CustomThemes
import com.gearui.theme.Shapes
import com.gearui.theme.ShapesDefault
import com.gearui.theme.ThemeMode
import com.gearui.theme.Themes
import com.gearui.theme.withBrandAccent
import com.tencent.kuikly.core.annotations.Page

/**
 * The desktop window. The host is a real Windows or macOS window, so this page
 * fills the client area and leaves the title bar to the operating system.
 * Partitions follow the window width.
 */
@Page("DesktopHost")
class DesktopHost : View() {

    override fun autoWrapApp(): Boolean = false

    @Composable
    override fun Content() {
        val params = pageData.params
        val settingsState = remember {
            SettingsState().apply {
                when (params.optString("theme")) {
                    "light" -> themeStyle = ThemeStyle.LIGHT
                    "dark" -> themeStyle = ThemeStyle.DARK
                    "system" -> themeStyle = ThemeStyle.SYSTEM
                }
                val lang = params.optString("lang")
                if (lang.isNotEmpty()) languageTag = lang
            }
        }
        val isSystemDark = StatusBarControllerImpl.isSystemDarkMode()
        val (themeMode, customTheme) = when (settingsState.themeStyle) {
            ThemeStyle.LIGHT -> ThemeMode.Light to null
            ThemeStyle.DARK -> ThemeMode.Dark to null
            ThemeStyle.DARK_PURPLE -> ThemeMode.Dark to CustomThemes.DarkPurple
            ThemeStyle.SYSTEM -> ThemeMode.System to null
        }
        val baseTheme = customTheme ?: if (
            themeMode == ThemeMode.Dark || (themeMode == ThemeMode.System && isSystemDark)
        ) Themes.Dark else Themes.Light
        val brandedTheme = settingsState.brandAccent.color?.let { baseTheme.withBrandAccent(it) } ?: baseTheme
        val square = ShapesDefault.Default.none
        val shapes = if (settingsState.squareControls) Shapes(
            none = square, sm = square, md = square, lg = square,
            xl = square, full = square, controlLarge = square
        ) else ShapesDefault.Default

        App(
            themeMode = themeMode,
            isSystemDark = isSystemDark,
            theme = brandedTheme,
            shapes = shapes,
            languageTag = settingsState.languageTag,
        ) {
            MailDesktopWindow(
                kind = SceneKind.ListDetail,
                forcedPartitions = null,
                chrome = DesktopChrome.Platform,
                systemTitleBar = params.optString("systemTitleBar") == "1",
            )
        }
    }
}
