package inventory.android

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import inventory.core.Inventory
import inventory.core.InventoryItem

private enum class Section(val label: String) {
    Stock("Stock"),
    Orders("Orders"),
    Notes("Notes"),
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val items = Inventory.items()
        setContent {
            val context = LocalContext.current
            var section by rememberSaveable { mutableStateOf(Section.Stock) }
            var openId by rememberSaveable { mutableStateOf<String?>(null) }
            val open = items.firstOrNull { it.id == openId }

            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        AnimatedContent(
                            targetState = open to section,
                            modifier = Modifier.fillMaxSize(),
                            transitionSpec = {
                                val opening = initialState.first == null && targetState.first != null
                                val closing = initialState.first != null && targetState.first == null
                                when {
                                    opening -> slideInHorizontally { it } + fadeIn() togetherWith
                                        slideOutHorizontally { -it / 4 } + fadeOut()
                                    closing -> slideInHorizontally { -it / 4 } + fadeIn() togetherWith
                                        slideOutHorizontally { it } + fadeOut()
                                    targetState.second.ordinal >= initialState.second.ordinal ->
                                        slideInHorizontally { it } + fadeIn() togetherWith
                                            slideOutHorizontally { -it } + fadeOut()
                                    else -> slideInHorizontally { -it } + fadeIn() togetherWith
                                        slideOutHorizontally { it } + fadeOut()
                                }
                            },
                            label = "screen",
                        ) { (item, current) ->
                            if (item == null) {
                                ItemList(current, items) { chosen ->
                                    InventoryHaptics.open(context)
                                    openId = chosen.id
                                }
                            } else {
                                ItemDetail(item) {
                                    openId = null
                                }
                            }
                        }
                        if (open == null) {
                            NavPill(
                                section = section,
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .navigationBarsPadding()
                                    .padding(bottom = 18.dp),
                            ) { next ->
                                if (next != section) {
                                    InventoryHaptics.section(context)
                                    section = next
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun ItemList(section: Section, items: List<InventoryItem>, onOpen: (InventoryItem) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(bottom = 96.dp)) {
        item {
            Text(
                section.label,
                modifier = Modifier.padding(start = 20.dp, top = 28.dp, bottom = 8.dp),
                style = MaterialTheme.typography.headlineSmall,
            )
        }
        items(items, key = { it.id }) { item ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpen(item) }
                    .padding(horizontal = 20.dp, vertical = 14.dp),
            ) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(item.name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                    Text(item.quantity.toString(), style = MaterialTheme.typography.titleMedium)
                }
            }
            HorizontalDivider()
        }
    }
}

@androidx.compose.runtime.Composable
private fun ItemDetail(item: InventoryItem, onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TextButton(onClick = onBack) { Text("Back") }
        Text(item.name, style = MaterialTheme.typography.headlineMedium)
        Text(item.id, style = MaterialTheme.typography.bodyMedium)
        Text("Quantity ${item.quantity}", style = MaterialTheme.typography.titleLarge)
    }
}

@androidx.compose.runtime.Composable
private fun NavPill(section: Section, modifier: Modifier = Modifier, onSelect: (Section) -> Unit) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.inverseSurface,
        shadowElevation = 8.dp,
    ) {
        Row(modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)) {
            Section.entries.forEach { entry ->
                val selected = entry == section
                Text(
                    entry.label,
                    color = if (selected) {
                        MaterialTheme.colorScheme.inversePrimary
                    } else {
                        MaterialTheme.colorScheme.inverseOnSurface
                    },
                    modifier = Modifier
                        .clip(RoundedCornerShape(22.dp))
                        .clickable { onSelect(entry) }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                )
            }
        }
    }
}

private object InventoryHaptics {
    fun section(context: Context) = play(context, longArrayOf(0, 12, 34, 18), intArrayOf(0, 70, 0, 160))

    fun open(context: Context) = play(context, longArrayOf(0, 16, 28, 30), intArrayOf(0, 180, 0, 90))

    private fun play(context: Context, timings: LongArray, amplitudes: IntArray) {
        val vibrator = if (Build.VERSION.SDK_INT >= 31) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        } ?: return
        if (!vibrator.hasVibrator()) return
        vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
    }
}
