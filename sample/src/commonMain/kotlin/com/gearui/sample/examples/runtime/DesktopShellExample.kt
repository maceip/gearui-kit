package com.gearui.sample.examples.runtime

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.gearui.desktop.DesktopChrome
import com.gearui.desktop.SceneKind
import com.gearui.primitives.Tab
import com.gearui.sample.config.ComponentInfo
import com.gearui.sample.pages.ExamplePage
import com.gearui.sample.pages.ExampleSection
import com.gearui.sample.pages.SectionSurface
import com.gearui.theme.Theme
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.draw.clip
import com.tencent.kuikly.compose.ui.unit.dp

/**
 * List-detail and the supporting pane, forced through the widths a phone page
 * cannot reach. The shell is the window. The pages are the same stack a phone uses.
 */
@Composable
fun DesktopShellExample(
    component: ComponentInfo,
    onBack: () -> Unit,
) {
    var kind by remember { mutableStateOf(SceneKind.ListDetail) }
    var partitions by remember { mutableStateOf(2) }
    var windows by remember { mutableStateOf(false) }

    ExamplePage(component = component, onBack = onBack) {
        ExampleSection(
            title = "列表-详情与支持面板",
            description = "手机只显示栈顶。两栏把下面那条放在旁边。三栏还要一条补充记录。支持面板是两页并排，没有空列。",
            surface = SectionSurface.Plain,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Tab(
                    text = "列表-详情",
                    selected = kind == SceneKind.ListDetail,
                    onClick = { kind = SceneKind.ListDetail },
                    modifier = Modifier.weight(1f),
                )
                Tab(
                    text = "支持面板",
                    selected = kind == SceneKind.Supporting,
                    onClick = { kind = SceneKind.Supporting },
                    modifier = Modifier.weight(1f),
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                listOf(1 to "手机", 2 to "两栏", 3 to "三栏").forEach { (count, label) ->
                    Tab(
                        text = label,
                        selected = partitions == count,
                        onClick = { partitions = count },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Tab(
                    text = "Mac",
                    selected = !windows,
                    onClick = { windows = false },
                    modifier = Modifier.weight(1f),
                )
                Tab(
                    text = "Windows",
                    selected = windows,
                    onClick = { windows = true },
                    modifier = Modifier.weight(1f),
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(480.dp)
                    .clip(Theme.shapes.lg),
            ) {
                MailDesktopWindow(
                    kind = kind,
                    forcedPartitions = partitions,
                    chrome = if (windows) DesktopChrome.Windows else DesktopChrome.Mac,
                    systemTitleBar = false,
                )
            }
        }
    }
}
