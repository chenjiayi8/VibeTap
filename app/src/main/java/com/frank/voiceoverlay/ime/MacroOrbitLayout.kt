package com.frank.voiceoverlay.ime

import com.frank.voiceoverlay.shortcuts.ShortcutPreset

const val MAX_PINNED_INNER_RING = 3
const val MAX_OUTER_RING_MACROS = 4

fun buildOrbitPages(macros: List<ShortcutPreset>): List<OrbitPage> {
    val orderedMacros = macros.sortedBy(ShortcutPreset::order)
    val pinnedMacros = orderedMacros.filter(ShortcutPreset::isPinned)
    val unpinnedMacros = orderedMacros.filterNot(ShortcutPreset::isPinned)
    val innerRing = pinnedMacros.take(MAX_PINNED_INNER_RING)
    val remainingMacros = pinnedMacros.drop(MAX_PINNED_INNER_RING) + unpinnedMacros

    val outerRingPages = remainingMacros.chunked(MAX_OUTER_RING_MACROS).ifEmpty { listOf(emptyList()) }
    val pageCount = outerRingPages.size

    return outerRingPages.mapIndexed { pageIndex, outerRing ->
        OrbitPage(
            pageIndex = pageIndex,
            pageCount = pageCount,
            innerRing = innerRing,
            outerRing = outerRing,
        )
    }
}
