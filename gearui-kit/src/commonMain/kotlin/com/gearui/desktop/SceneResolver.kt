package com.gearui.desktop

import com.gearui.foundation.control.ControlGeometry

/**
 * What a stack entry is, for one scene strategy.
 *
 * [Single] is an ordinary page. It ends a run of pane entries, so a wide window
 * does not reach back through it to an older list. [List], [Detail] and [Extra]
 * belong to list-detail. [Main] and [Supporting] belong to the supporting-pane
 * scene. [Extra] is the third pane of whichever strategy is active.
 */
enum class SceneRole {
    Single,
    List,
    Detail,
    Extra,
    Main,
    Supporting,
}

/**
 * Which pairing to try.
 *
 * [ListDetail] is the one that fits a phone stack: the phone shows only the top
 * entry, and a wide window places the entry under it in a second column.
 * [Supporting] is two peer panes. It has no placeholder, and on a phone it gives
 * the window back to the single top entry. It is not the default.
 */
enum class SceneKind {
    ListDetail,
    Supporting,
}

/** One stack entry, bottom to top, as the resolver sees it. */
data class SceneEntry(
    val key: String,
    val routeName: String,
    val role: SceneRole,
)

/**
 * What the window should show for the current stack.
 *
 * A [Single] result means the strategy declined. The window shows that entry
 * and nothing beside it. [ListDetail.placeholder] is an empty detail column,
 * not a route: the list is on top and a wide window still keeps the second pane.
 */
sealed interface ResolvedScene {
    data class Single(val entry: SceneEntry) : ResolvedScene

    data class ListDetail(
        val list: SceneEntry?,
        val detail: SceneEntry?,
        val extra: SceneEntry?,
        val placeholder: Boolean,
    ) : ResolvedScene

    data class Supporting(
        val main: SceneEntry?,
        val supporting: SceneEntry?,
        val extra: SceneEntry?,
    ) : ResolvedScene
}

/**
 * How many panes can sit side by side.
 *
 * One below [ControlGeometry.desktopTwoPaneMin]: the phone, and the strategy
 * declines. Two from there until [ControlGeometry.desktopThreePaneMin]: list and
 * detail, which is the desktop default. Three only at that wider bound, and only
 * when an extra entry is on the stack.
 */
fun partitionsForWidth(widthDp: Float): Int = when {
    widthDp < ControlGeometry.desktopTwoPaneMin.value -> 1
    widthDp < ControlGeometry.desktopThreePaneMin.value -> 2
    else -> 3
}

/**
 * Which panes of [stack] (bottom to top) are on screen.
 *
 * The run is the consecutive pane entries at the top of the stack. A [SceneRole.Single]
 * entry, or an entry from the other strategy, ends the run. An older list is not
 * pulled back across that gap.
 *
 * Slots go to the top entry first, then to detail / list / extra (list-detail) or
 * main / supporting / extra (supporting). A pane that does not get a slot is
 * hidden, not stacked underneath the others. With one slot the result is always
 * [ResolvedScene.Single] of the top entry: the phone shows the detail, not the list.
 *
 * Back is one entry. Navigation 3's default pops until the set of visible panes
 * changes, which skips a previous detail when two details in a row leave the same
 * panes on screen. [scaffoldSignature] is equal in that case. Callers pop once.
 */
fun resolveScene(
    stack: List<SceneEntry>,
    partitions: Int,
    kind: SceneKind,
): ResolvedScene {
    require(stack.isNotEmpty()) { "a scene needs a stack" }
    val top = stack.last()
    if (partitions < 2) return ResolvedScene.Single(top)

    val family = when (kind) {
        SceneKind.ListDetail -> listDetailRoles
        SceneKind.Supporting -> supportingRoles
    }
    val run = ArrayList<SceneEntry>(stack.size)
    for (index in stack.lastIndex downTo 0) {
        val entry = stack[index]
        if (entry.role !in family) break
        run.add(0, entry)
    }
    if (run.isEmpty()) return ResolvedScene.Single(top)

    fun latest(role: SceneRole): SceneEntry? = run.lastOrNull { it.role == role }

    val current = run.last()
    val order = when (kind) {
        SceneKind.ListDetail -> listOf(SceneRole.Detail, SceneRole.List, SceneRole.Extra)
        SceneKind.Supporting -> listOf(SceneRole.Main, SceneRole.Supporting, SceneRole.Extra)
    }
    val visible = LinkedHashMap<SceneRole, SceneEntry>()
    var slots = partitions

    fun take(role: SceneRole, entry: SceneEntry?) {
        if (slots <= 0 || entry == null || role in visible) return
        visible[role] = entry
        slots -= 1
    }

    take(current.role, current)
    for (role in order) take(role, latest(role))

    val detailEntry = latest(SceneRole.Detail)
    val placeholder = kind == SceneKind.ListDetail &&
        SceneRole.List in visible &&
        detailEntry == null &&
        slots > 0
    val paneCount = visible.size + if (placeholder) 1 else 0
    if (paneCount <= 1) return ResolvedScene.Single(top)

    return when (kind) {
        SceneKind.ListDetail -> ResolvedScene.ListDetail(
            list = visible[SceneRole.List],
            detail = visible[SceneRole.Detail],
            extra = visible[SceneRole.Extra],
            placeholder = placeholder,
        )
        SceneKind.Supporting -> ResolvedScene.Supporting(
            main = visible[SceneRole.Main],
            supporting = visible[SceneRole.Supporting],
            extra = visible[SceneRole.Extra],
        )
    }
}

/**
 * Which pane roles are expanded, ignoring which entry fills them.
 *
 * Two details in a row produce the same signature, so a back policy that pops
 * until the signature changes removes both. GearUI does not do that.
 */
fun scaffoldSignature(scene: ResolvedScene): String = when (scene) {
    is ResolvedScene.Single -> "single"
    is ResolvedScene.ListDetail ->
        "list-detail:${scene.list != null}:${scene.detail != null}:${scene.extra != null}:${scene.placeholder}"
    is ResolvedScene.Supporting ->
        "supporting:${scene.main != null}:${scene.supporting != null}:${scene.extra != null}"
}

private val listDetailRoles = setOf(SceneRole.List, SceneRole.Detail, SceneRole.Extra)
private val supportingRoles = setOf(SceneRole.Main, SceneRole.Supporting, SceneRole.Extra)
