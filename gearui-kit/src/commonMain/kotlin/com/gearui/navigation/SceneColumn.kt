package com.gearui.navigation

/**
 * One column of a multi-pane scene, rendered by [Navigator] instead of the
 * full-screen stack. [Placeholder] is the empty detail pane: no route, no entry.
 *
 * Null on the controller means the ordinary phone stack.
 */
internal data class ColumnLayout(
    val slots: List<SceneColumn>,
)

internal sealed interface SceneColumn {
    val weight: Float

    data class Entry(val key: String, override val weight: Float) : SceneColumn

    data class Placeholder(override val weight: Float) : SceneColumn
}
