package com.gearui.desktop

import androidx.compose.runtime.Composable
import com.gearui.components.icon.IconSource
import com.gearui.components.icon.Icons
import com.gearui.components.icon.minus
import com.gearui.components.icon.square
import com.gearui.components.icon.x
import com.gearui.foundation.border.BorderWidth
import com.gearui.foundation.control.ControlGeometry
import com.gearui.foundation.interaction.PressableFeedback
import com.gearui.foundation.layout.Spacing
import com.gearui.foundation.primitives.Icon
import com.gearui.foundation.primitives.Text
import com.gearui.foundation.typography.IconSizes
import com.gearui.i18n.I18n
import com.gearui.theme.Theme
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.fillMaxHeight
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.layout.width
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.text.style.TextOverflow

/**
 * Which window frame to draw around a multi-pane scene.
 *
 * [Platform] follows [desktopPlatform]. [Other] uses [Mac]: the lights are not
 * drawn, only the gutter they occupy. [Windows] draws the three caption buttons
 * and calls [DesktopWindowCommands]; on a phone target those calls do nothing.
 */
enum class DesktopChrome {
    Platform,
    Mac,
    Windows,
}

internal enum class DesktopSkin { Mac, Windows }

internal fun resolveSkin(chrome: DesktopChrome): DesktopSkin = when (chrome) {
    DesktopChrome.Mac -> DesktopSkin.Mac
    DesktopChrome.Windows -> DesktopSkin.Windows
    DesktopChrome.Platform -> when (desktopPlatform()) {
        DesktopPlatform.Mac -> DesktopSkin.Mac
        DesktopPlatform.Windows -> DesktopSkin.Windows
        DesktopPlatform.Other -> DesktopSkin.Mac
    }
}

/**
 * The strip above the panes. Drawn only when at least two partitions fit.
 * Traffic lights are the host's; this reserves their space and does not paint them.
 */
@Composable
internal fun DesktopTitleBar(skin: DesktopSkin, title: String) {
    val colors = Theme.colors
    val height = when (skin) {
        DesktopSkin.Mac -> ControlGeometry.desktopMacTitleBarHeight
        DesktopSkin.Windows -> ControlGeometry.desktopWindowsTitleBarHeight
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .background(colors.surface),
    ) {
        when (skin) {
            DesktopSkin.Mac -> {
                Text(
                    text = title,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = ControlGeometry.desktopMacTrafficLeading),
                    style = Theme.typography.bodySmall,
                    color = colors.foreground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            DesktopSkin.Windows -> {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = title,
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = Spacing.lg),
                        style = Theme.typography.bodySmall,
                        color = colors.foreground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    val common = I18n.strings.common
                    CaptionButton(Icons.minus, common.minimize, DesktopWindowCommands::minimize)
                    CaptionButton(Icons.square, common.maximize, DesktopWindowCommands::maximize)
                    CaptionButton(Icons.x, common.close, DesktopWindowCommands::close)
                }
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(BorderWidth.thin)
                .background(colors.border),
        )
    }
}

@Composable
private fun CaptionButton(icon: IconSource, label: String, onClick: () -> Unit) {
    val colors = Theme.colors
    PressableFeedback(
        onClick = onClick,
        modifier = Modifier
            .width(ControlGeometry.desktopWindowsCaptionWidth)
            .fillMaxHeight(),
        shape = Theme.shapes.none,
        contentDescription = label,
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(icon, size = IconSizes.Default.sm, tint = colors.foreground)
        }
    }
}
