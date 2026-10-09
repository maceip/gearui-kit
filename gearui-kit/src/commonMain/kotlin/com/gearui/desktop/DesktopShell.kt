package com.gearui.desktop

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import com.gearui.components.bottomnavbar.BottomNavBar
import com.gearui.components.bottomnavbar.BottomNavItem
import com.gearui.foundation.border.BorderWidth
import com.gearui.foundation.control.ControlGeometry
import com.gearui.foundation.interaction.PressableFeedback
import com.gearui.foundation.layout.Spacing
import com.gearui.foundation.primitives.Icon
import com.gearui.foundation.primitives.Text
import com.gearui.foundation.typography.IconSizes
import com.gearui.navigation.ColumnLayout
import com.gearui.navigation.EntryScope
import com.gearui.navigation.NavEntry
import com.gearui.navigation.NavRoute
import com.gearui.navigation.Navigator
import com.gearui.navigation.NavigatorController
import com.gearui.navigation.NavigatorState
import com.gearui.navigation.SceneColumn
import com.gearui.primitives.Badge
import com.gearui.primitives.BadgeType
import com.gearui.runtime.LocalScenePlaceholder
import com.gearui.theme.Theme
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.BoxWithConstraints
import com.tencent.kuikly.compose.foundation.layout.fillMaxHeight
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.layout.width
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.text.style.TextOverflow
import com.tencent.kuikly.compose.ui.unit.Dp
import com.tencent.kuikly.compose.ui.unit.dp
import kotlin.math.max

/**
 * A wide-window frame around one [Navigator].
 *
 * The phone and the desktop do not nest. A full-screen scene is not a column,
 * and this frame is not a phone page. [roleOf] says what each route is for the
 * active [kind]. [SceneKind.ListDetail] is the default: below
 * [ControlGeometry.desktopTwoPaneMin] the window shows only the top entry, and
 * from there the entry under it sits beside it. A third pane needs
 * [ControlGeometry.desktopThreePaneMin] and an [SceneRole.Extra] entry on the
 * consecutive run. [SceneKind.Supporting] is the peer layout. It has no empty
 * column, and on one partition it yields.
 *
 * [tabs] are the phone's bottom bar. From two partitions they move to the
 * sidebar. The sidebar is chrome, not a scene column.
 *
 * Back pops one entry. It does not keep popping while the set of panes stays
 * the same. While more than one pane is showing, each pane hides its own back
 * button and swipe-back stays off; the shell's back handler still pops once.
 *
 * @param forcedPartitions overrides the width, for a probe inside a phone page.
 *   Null follows the window. Values outside 1..3 are clamped.
 * @param systemTitleBar the operating-system window already has the title bar,
 *   traffic lights, and caption buttons. The shell then draws none of its own.
 * @param placeholder the empty detail column. Not a route. List-detail only.
 */
@Composable
fun <R : NavRoute> DesktopShell(
    controller: NavigatorController<R>,
    roleOf: (R) -> SceneRole,
    kind: SceneKind = SceneKind.ListDetail,
    tabs: List<BottomNavItem> = emptyList(),
    selectedTab: String? = null,
    onSelectTab: (String) -> Unit = {},
    forcedPartitions: Int? = null,
    chrome: DesktopChrome = DesktopChrome.Platform,
    systemTitleBar: Boolean = false,
    modifier: Modifier = Modifier,
    title: String = "",
    placeholder: @Composable () -> Unit = {},
    content: @Composable EntryScope<R>.(NavEntry<R>) -> Unit,
) {
    val state = controller as NavigatorState<R>
    DisposableEffect(state) {
        onDispose { state.columnLayout = null }
    }
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val partitions = (forcedPartitions ?: partitionsForWidth(maxWidth.value)).coerceIn(1, 3)
        val entries = state.entriesForTest
        val stack = entries.map { entry ->
            SceneEntry(entry.key, entry.route.routeName, roleOf(entry.route))
        }
        val scene = resolveScene(stack, partitions, kind)
        val skin = resolveSkin(chrome)
        val showSidebar = partitions >= 2 && tabs.isNotEmpty()
        val sidebarWidth = if (showSidebar) sidebarWidth(skin, maxWidth) else 0.dp
        val well = if (showSidebar) (maxWidth - sidebarWidth).coerceAtLeast(Spacing.xxl) else maxWidth
        val columns = columnsOf(scene, well)
        if (state.columnLayout != columns) state.columnLayout = columns

        Column(modifier = Modifier.fillMaxSize()) {
            if (partitions >= 2 && !systemTitleBar) DesktopTitleBar(skin, title)
            Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                if (showSidebar) {
                    DesktopSidebar(
                        items = tabs,
                        selectedId = selectedTab,
                        onSelect = onSelectTab,
                        width = sidebarWidth,
                    )
                }
                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    CompositionLocalProvider(LocalScenePlaceholder provides placeholder) {
                        Navigator(
                            controller = controller,
                            modifier = Modifier.fillMaxSize(),
                            swipeBackEnabled = columns == null,
                            content = content,
                        )
                    }
                }
            }
            if (partitions < 2 && tabs.isNotEmpty()) {
                BottomNavBar(
                    items = tabs,
                    selectedId = selectedTab,
                    onSelect = onSelectTab,
                )
            }
        }
    }
}

/**
 * Preferred list width when the well can hold it. Otherwise the panes share
 * the row, so a phone-sized probe that forces columns still fits.
 */
private fun paneWeights(well: Dp, narrowCount: Int): Pair<Float, Float> {
    val preferred = ControlGeometry.desktopListPaneWidth
    val count = max(narrowCount, 1)
    if (well < preferred * (count + 1)) return 2f to 3f
    val narrow = preferred.value
    val wide = (well.value - preferred.value * count).coerceAtLeast(preferred.value)
    return narrow to wide
}

private fun columnsOf(scene: ResolvedScene, well: Dp): ColumnLayout? {
    val slots: List<SceneColumn> = when (scene) {
        is ResolvedScene.Single -> return null
        is ResolvedScene.ListDetail -> {
            val (narrow, wide) = paneWeights(well, listOfNotNull(scene.list, scene.extra).size)
            buildList {
                scene.list?.let { add(SceneColumn.Entry(it.key, narrow)) }
                if (scene.placeholder) add(SceneColumn.Placeholder(wide))
                scene.detail?.let { add(SceneColumn.Entry(it.key, wide)) }
                scene.extra?.let { add(SceneColumn.Entry(it.key, narrow)) }
            }
        }
        is ResolvedScene.Supporting -> {
            val (narrow, wide) = paneWeights(well, listOfNotNull(scene.main, scene.extra).size)
            buildList {
                scene.main?.let { add(SceneColumn.Entry(it.key, narrow)) }
                scene.supporting?.let { add(SceneColumn.Entry(it.key, wide)) }
                scene.extra?.let { add(SceneColumn.Entry(it.key, narrow)) }
            }
        }
    }
    if (slots.size < 2) return null
    return ColumnLayout(slots)
}

/**
 * The token when the window can spare it. A forced multi-pane probe on a
 * phone-width page takes a quarter, so the columns still have a well.
 */
private fun sidebarWidth(skin: DesktopSkin, window: Dp): Dp {
    val token = when (skin) {
        DesktopSkin.Mac -> ControlGeometry.desktopSidebarWidthMac
        DesktopSkin.Windows -> ControlGeometry.desktopSidebarWidthWindows
    }
    if (window >= token + ControlGeometry.desktopListPaneWidth) return token
    return (window / 4).coerceAtMost(token).coerceAtLeast(Spacing.xxl)
}

@Composable
private fun DesktopSidebar(
    items: List<BottomNavItem>,
    selectedId: String?,
    onSelect: (String) -> Unit,
    width: Dp,
) {
    val colors = Theme.colors
    val selected = selectedId ?: items.firstOrNull()?.id
    Row(
        modifier = Modifier
            .width(width)
            .fillMaxHeight()
            .background(colors.surface),
    ) {
        Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
            items.forEach { item ->
                val on = item.id == selected
                val tint = when {
                    item.disabled -> colors.mutedForeground
                    on -> colors.accentForeground
                    else -> colors.foreground
                }
                PressableFeedback(
                    onClick = { onSelect(item.id) },
                    enabled = !item.disabled,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.sm, vertical = Spacing.xs),
                    shape = Theme.shapes.sm,
                    contentDescription = item.label,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (on) colors.accent else Color.Transparent)
                            .padding(Spacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            icon = if (on) item.selectedIcon ?: item.icon else item.icon,
                            size = IconSizes.Default.lg,
                            tint = tint,
                        )
                        Text(
                            text = item.label,
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = Spacing.sm),
                            style = Theme.typography.bodyMedium,
                            color = tint,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        when {
                            item.showBadgeDot -> Badge(type = BadgeType.RedPoint)
                            item.badgeCount > 0 -> Badge(count = item.badgeCount)
                        }
                    }
                }
            }
        }
        Box(
            modifier = Modifier
                .width(BorderWidth.thin)
                .fillMaxHeight()
                .background(colors.border),
        )
    }
}
