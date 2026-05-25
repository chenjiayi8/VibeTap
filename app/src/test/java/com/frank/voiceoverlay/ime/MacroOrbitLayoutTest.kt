package com.frank.voiceoverlay.ime

import com.frank.voiceoverlay.shortcuts.ShortcutPreset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MacroOrbitLayoutTest {
    @Test
    fun buildOrbitPages_fillsInnerRingWithFirstThreePinnedMacrosByOrder() {
        val macros = listOf(
            preset(id = "unpinned-1", order = 4, isPinned = false),
            preset(id = "pinned-2", order = 2, isPinned = true),
            preset(id = "pinned-0", order = 0, isPinned = true),
            preset(id = "unpinned-2", order = 5, isPinned = false),
            preset(id = "pinned-3", order = 3, isPinned = true),
            preset(id = "pinned-1", order = 1, isPinned = true),
        )

        val pages = buildOrbitPages(macros)

        assertEquals(1, pages.size)
        assertEquals(listOf("pinned-0", "pinned-1", "pinned-2"), pages.single().innerRing.map { it.id })
        assertEquals(listOf("pinned-3", "unpinned-1", "unpinned-2"), pages.single().outerRing.map { it.id })
    }

    @Test
    fun buildOrbitPages_keepsOverflowPinnedMacrosAheadOfUnpinnedMacrosOnOuterPages() {
        val macros = listOf(
            preset(id = "pinned-0", order = 10, isPinned = true),
            preset(id = "pinned-1", order = 11, isPinned = true),
            preset(id = "pinned-2", order = 12, isPinned = true),
            preset(id = "unpinned-0", order = 0),
            preset(id = "unpinned-1", order = 1),
            preset(id = "pinned-3", order = 13, isPinned = true),
            preset(id = "pinned-4", order = 14, isPinned = true),
        )

        val pages = buildOrbitPages(macros)

        assertEquals(1, pages.size)
        assertEquals(listOf("pinned-0", "pinned-1", "pinned-2"), pages.single().innerRing.map { it.id })
        assertEquals(listOf("pinned-3", "pinned-4", "unpinned-0", "unpinned-1"), pages.single().outerRing.map { it.id })
    }

    @Test
    fun buildOrbitPages_paginatesRemainingMacrosAcrossOuterRingPages() {
        val macros = listOf(
            preset(id = "pinned-0", order = 0, isPinned = true),
            preset(id = "pinned-1", order = 1, isPinned = true),
            preset(id = "pinned-2", order = 2, isPinned = true),
            preset(id = "outer-0", order = 3),
            preset(id = "outer-1", order = 4),
            preset(id = "outer-2", order = 5),
            preset(id = "outer-3", order = 6),
            preset(id = "outer-4", order = 7),
            preset(id = "outer-5", order = 8),
        )

        val pages = buildOrbitPages(macros)

        assertEquals(2, pages.size)

        assertEquals(0, pages[0].pageIndex)
        assertEquals(2, pages[0].pageCount)
        assertFalse(pages[0].hasPreviousPage)
        assertTrue(pages[0].hasNextPage)
        assertEquals(listOf("pinned-0", "pinned-1", "pinned-2"), pages[0].innerRing.map { it.id })
        assertEquals(listOf("outer-0", "outer-1", "outer-2", "outer-3"), pages[0].outerRing.map { it.id })

        assertEquals(1, pages[1].pageIndex)
        assertEquals(2, pages[1].pageCount)
        assertTrue(pages[1].hasPreviousPage)
        assertFalse(pages[1].hasNextPage)
        assertEquals(listOf("pinned-0", "pinned-1", "pinned-2"), pages[1].innerRing.map { it.id })
        assertEquals(listOf("outer-4", "outer-5"), pages[1].outerRing.map { it.id })
    }

    private fun preset(id: String, order: Int, isPinned: Boolean = false): ShortcutPreset =
        ShortcutPreset(
            id = id,
            label = id,
            text = id,
            order = order,
            isPinned = isPinned,
        )
}
