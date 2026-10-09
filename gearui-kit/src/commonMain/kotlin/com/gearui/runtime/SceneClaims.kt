package com.gearui.runtime

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Chrome a desktop column has already taken, so the scene must not draw it too.
 *
 * Empty by default, which is the phone: the scene draws its own back button.
 * A multi-pane column sets [hideBack] because the entry underneath is on screen
 * beside it. System back still pops one entry.
 */
@Immutable
data class SceneClaims(
    val hideBack: Boolean = false,
)

val LocalSceneClaims = staticCompositionLocalOf { SceneClaims() }

/** The empty detail column. Not a route. */
val LocalScenePlaceholder = staticCompositionLocalOf<@Composable () -> Unit> { {} }
