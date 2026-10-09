package com.gearui.sample.examples.runtime

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.gearui.components.bottomnavbar.BottomNavItem
import com.gearui.components.button.Button
import com.gearui.components.button.ButtonSize
import com.gearui.components.button.ButtonType
import com.gearui.components.icon.Icons
import com.gearui.components.icon.gear
import com.gearui.components.icon.tray
import com.gearui.components.navbar.NavBar
import com.gearui.desktop.DesktopChrome
import com.gearui.desktop.DesktopShell
import com.gearui.desktop.SceneKind
import com.gearui.desktop.SceneRole
import com.gearui.foundation.interaction.PressableFeedback
import com.gearui.foundation.layout.Spacing
import com.gearui.foundation.primitives.Text
import com.gearui.navigation.NavEntry
import com.gearui.navigation.NavRoute
import com.gearui.navigation.NavigatorController
import com.gearui.navigation.rememberNavigatorController
import com.gearui.runtime.LocalRuntimeFlags
import com.gearui.theme.Theme
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.ui.Modifier

/**
 * The mail stack inside [DesktopShell]. The phone probe forces [forcedPartitions].
 * A desktop window passes null and lets the window width decide, and sets
 * [systemTitleBar] so the operating system keeps the title bar.
 */
@Composable
fun MailDesktopWindow(
    kind: SceneKind,
    forcedPartitions: Int?,
    chrome: DesktopChrome,
    systemTitleBar: Boolean,
) {
    val nav = rememberNavigatorController<MailRoute>(MailRoute.Inbox)
    val selectedTab = if (nav.current.route == MailRoute.Settings) "settings" else "inbox"
    val flags = LocalRuntimeFlags.current
    CompositionLocalProvider(
        LocalRuntimeFlags provides flags.copy(
            navBarConsumesTopSafeArea = false,
            bottomNavBarConsumesBottomSafeArea = false,
        ),
    ) {
        DesktopShell(
            controller = nav,
            roleOf = { route -> route.roleIn(kind) },
            kind = kind,
            tabs = mailTabs,
            selectedTab = selectedTab,
            onSelectTab = { id -> openSection(nav, id) },
            forcedPartitions = forcedPartitions,
            chrome = chrome,
            systemTitleBar = systemTitleBar,
            modifier = Modifier.fillMaxSize(),
            title = "邮件",
            placeholder = { EmptyDetail() },
        ) { entry ->
            MailPage(entry, nav)
        }
    }
}

private val mailTabs = listOf(
    BottomNavItem(id = "inbox", label = "收件箱", icon = Icons.tray),
    BottomNavItem(id = "settings", label = "设置", icon = Icons.gear),
)

private fun openSection(nav: NavigatorController<MailRoute>, id: String) {
    val root = if (id == "settings") MailRoute.Settings else MailRoute.Inbox
    if (nav.current.route == root) return
    if (root == MailRoute.Inbox) {
        if (!nav.popTo(MailRoute.Inbox)) nav.resetTo(MailRoute.Inbox)
        return
    }
    if (nav.current.route != MailRoute.Inbox) nav.popTo(MailRoute.Inbox)
    if (!nav.isTransitioning && nav.current.route == MailRoute.Inbox) {
        nav.push(MailRoute.Settings)
    } else if (nav.current.route != MailRoute.Settings) {
        nav.resetTo(MailRoute.Settings)
    }
}

@Composable
private fun MailPage(entry: NavEntry<MailRoute>, nav: NavigatorController<MailRoute>) {
    val colors = Theme.colors
    Column(modifier = Modifier.fillMaxSize().background(colors.background)) {
        NavBar(
            title = entry.route.title,
            useDefaultBack = nav.canPop,
            onBackClick = { nav.pop() },
        )
        when (entry.route) {
            MailRoute.Inbox -> MailRow(title = "项目周报", subtitle = "周三前") {
                nav.push(MailRoute.Message)
            }
            MailRoute.Message -> Column(modifier = Modifier.padding(Spacing.lg)) {
                Text(
                    text = "周三前看一下附件。",
                    style = Theme.typography.bodyMedium,
                    color = colors.foreground,
                )
                Button(
                    text = "附件",
                    onClick = { nav.push(MailRoute.Info) },
                    type = ButtonType.TEXT,
                    size = ButtonSize.SMALL,
                    modifier = Modifier.padding(top = Spacing.md),
                )
            }
            MailRoute.Info -> Text(
                text = "周报.pdf",
                modifier = Modifier.padding(Spacing.lg),
                style = Theme.typography.bodyMedium,
                color = colors.foreground,
            )
            MailRoute.Settings -> Text(
                text = "通知、账号",
                modifier = Modifier.padding(Spacing.lg),
                style = Theme.typography.bodyMedium,
                color = colors.foreground,
            )
        }
    }
}

@Composable
private fun MailRow(title: String, subtitle: String, onClick: () -> Unit) {
    val colors = Theme.colors
    PressableFeedback(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(Spacing.lg)) {
            Text(text = title, style = Theme.typography.bodyMedium, color = colors.foreground)
            Text(text = subtitle, style = Theme.typography.bodySmall, color = colors.mutedForeground)
        }
    }
}

@Composable
private fun EmptyDetail() {
    // Top of the column, not centered. On a wide window Kuikly measured a
    // centered child against the whole row, and the label sat past the edge.
    Text(
        text = "选择一封邮件",
        modifier = Modifier.padding(Spacing.lg),
        style = Theme.typography.bodyMedium,
        color = Theme.colors.mutedForeground,
    )
}

private sealed interface MailRoute : NavRoute {
    val title: String

    fun roleIn(kind: SceneKind): SceneRole

    data object Inbox : MailRoute {
        override val routeName: String = "inbox"
        override val title: String = "收件箱"
        override fun roleIn(kind: SceneKind): SceneRole =
            if (kind == SceneKind.ListDetail) SceneRole.List else SceneRole.Main
    }

    data object Message : MailRoute {
        override val routeName: String = "message"
        override val title: String = "邮件"
        override fun roleIn(kind: SceneKind): SceneRole =
            if (kind == SceneKind.ListDetail) SceneRole.Detail else SceneRole.Supporting
    }

    data object Info : MailRoute {
        override val routeName: String = "info"
        override val title: String = "附件"
        override fun roleIn(kind: SceneKind): SceneRole = SceneRole.Extra
    }

    data object Settings : MailRoute {
        override val routeName: String = "settings"
        override val title: String = "设置"
        override fun roleIn(kind: SceneKind): SceneRole = SceneRole.Single
    }
}
