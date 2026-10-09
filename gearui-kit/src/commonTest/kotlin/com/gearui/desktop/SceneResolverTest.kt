package com.gearui.desktop

import com.gearui.foundation.control.ControlGeometry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The two scenes Android's Navigation 3 added for wide windows, and the cases
 * where they do not become three peer columns.
 *
 * List-detail: the phone shows only the top entry. A wide window puts the entry
 * under it in a second column. That is two columns, not two views the phone had
 * on screen together.
 *
 * Supporting pane: main and supporting are peers, which is closer to two mobile
 * views side by side. On a phone the strategy declines and only the top entry
 * remains. There is no placeholder. It is not the default.
 */
class SceneResolverTest {

    private val list = SceneEntry("l", "list", SceneRole.List)
    private val detailA = SceneEntry("a", "detailA", SceneRole.Detail)
    private val detailB = SceneEntry("b", "detailB", SceneRole.Detail)
    private val extra = SceneEntry("e", "extra", SceneRole.Extra)
    private val single = SceneEntry("s", "single", SceneRole.Single)
    private val main = SceneEntry("m", "main", SceneRole.Main)
    private val supporting = SceneEntry("p", "supporting", SceneRole.Supporting)

    @Test
    fun twoPanesIsTheDesktopDefaultAndThreeNeedsTheWideBound() {
        val two = ControlGeometry.desktopTwoPaneMin.value
        val three = ControlGeometry.desktopThreePaneMin.value
        assertEquals(1, partitionsForWidth(two - 1f))
        assertEquals(2, partitionsForWidth(two))
        assertEquals(2, partitionsForWidth(three - 1f))
        assertEquals(3, partitionsForWidth(three))
    }

    @Test
    fun phoneShowsOnlyTheDetail() {
        val scene = resolveScene(listOf(list, detailA), partitions = 1, SceneKind.ListDetail)
        val singlePane = assertIs<ResolvedScene.Single>(scene)
        assertEquals("detailA", singlePane.entry.routeName)
    }

    @Test
    fun wideWindowPlacesTheListBesideTheDetail() {
        val scene = resolveScene(listOf(list, detailA), partitions = 2, SceneKind.ListDetail)
        val panes = assertIs<ResolvedScene.ListDetail>(scene)
        assertEquals("list", panes.list?.routeName)
        assertEquals("detailA", panes.detail?.routeName)
        assertEquals(false, panes.placeholder)
        assertNull(panes.extra)
    }

    @Test
    fun listAloneKeepsAnEmptyDetailColumn() {
        val scene = resolveScene(listOf(list), partitions = 2, SceneKind.ListDetail)
        val panes = assertIs<ResolvedScene.ListDetail>(scene)
        assertEquals("list", panes.list?.routeName)
        assertNull(panes.detail)
        assertTrue(panes.placeholder)
    }

    @Test
    fun aNormalPageBlocksTheListBehindIt() {
        val scene = resolveScene(
            listOf(list, single, detailA),
            partitions = 2,
            SceneKind.ListDetail,
        )
        val only = assertIs<ResolvedScene.Single>(scene)
        assertEquals("detailA", only.entry.routeName)
    }

    @Test
    fun detailWithNoListIsStillOnePage() {
        val scene = resolveScene(listOf(detailA), partitions = 2, SceneKind.ListDetail)
        assertIs<ResolvedScene.Single>(scene)
    }

    @Test
    fun twoPartitionsHideTheListWhenExtraIsOnTop() {
        val scene = resolveScene(
            listOf(list, detailA, extra),
            partitions = 2,
            SceneKind.ListDetail,
        )
        val panes = assertIs<ResolvedScene.ListDetail>(scene)
        assertNull(panes.list)
        assertEquals("detailA", panes.detail?.routeName)
        assertEquals("extra", panes.extra?.routeName)
        assertEquals(false, panes.placeholder)
    }

    @Test
    fun threePartitionsShowListDetailAndExtra() {
        val scene = resolveScene(
            listOf(list, detailA, extra),
            partitions = 3,
            SceneKind.ListDetail,
        )
        val panes = assertIs<ResolvedScene.ListDetail>(scene)
        assertEquals("list", panes.list?.routeName)
        assertEquals("detailA", panes.detail?.routeName)
        assertEquals("extra", panes.extra?.routeName)
    }

    @Test
    fun extraWideListAloneDoesNotInventAThirdColumn() {
        val scene = resolveScene(listOf(list), partitions = 3, SceneKind.ListDetail)
        val panes = assertIs<ResolvedScene.ListDetail>(scene)
        assertTrue(panes.placeholder)
        assertNull(panes.extra)
    }

    @Test
    fun onePopKeepsThePreviousDetail() {
        val stack = listOf(list, detailA, detailB)
        val before = resolveScene(stack, partitions = 2, SceneKind.ListDetail)
        val after = resolveScene(stack.dropLast(1), partitions = 2, SceneKind.ListDetail)
        val panes = assertIs<ResolvedScene.ListDetail>(after)
        assertEquals("detailA", panes.detail?.routeName)
        assertEquals("list", panes.list?.routeName)
        assertEquals(
            scaffoldSignature(before),
            scaffoldSignature(after),
            "the set of panes did not change, which is why popping until it does skips a detail",
        )
    }

    @Test
    fun poppingUntilThePaneSetChangesRemovesBothDetails() {
        var stack = listOf(list, detailA, detailB)
        val signature = scaffoldSignature(
            resolveScene(stack, partitions = 2, SceneKind.ListDetail),
        )
        while (stack.size > 1 &&
            scaffoldSignature(resolveScene(stack, 2, SceneKind.ListDetail)) == signature
        ) {
            stack = stack.dropLast(1)
        }
        assertEquals(listOf("list"), stack.map { it.routeName })
    }

    @Test
    fun supportingPeersSitSideBySideOnlyWhenBothFit() {
        val wide = resolveScene(listOf(main, supporting), partitions = 2, SceneKind.Supporting)
        val panes = assertIs<ResolvedScene.Supporting>(wide)
        assertEquals("main", panes.main?.routeName)
        assertEquals("supporting", panes.supporting?.routeName)

        val phone = resolveScene(listOf(main, supporting), partitions = 1, SceneKind.Supporting)
        val only = assertIs<ResolvedScene.Single>(phone)
        assertEquals("supporting", only.entry.routeName)
    }

    @Test
    fun supportingPaneHasNoPlaceholder() {
        val scene = resolveScene(listOf(main), partitions = 2, SceneKind.Supporting)
        assertIs<ResolvedScene.Single>(scene)
    }

    @Test
    fun theTwoStrategiesDoNotBorrowEachOthersEntries() {
        val asSupporting = resolveScene(listOf(list, detailA), partitions = 2, SceneKind.Supporting)
        assertEquals("detailA", assertIs<ResolvedScene.Single>(asSupporting).entry.routeName)

        val asList = resolveScene(listOf(main, supporting), partitions = 2, SceneKind.ListDetail)
        assertEquals("supporting", assertIs<ResolvedScene.Single>(asList).entry.routeName)
    }

    @Test
    fun anEmptyStackIsRefused() {
        assertFailsWith<IllegalArgumentException> {
            resolveScene(emptyList(), partitions = 2, SceneKind.ListDetail)
        }
    }
}
