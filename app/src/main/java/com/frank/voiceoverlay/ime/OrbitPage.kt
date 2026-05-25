package com.frank.voiceoverlay.ime

import com.frank.voiceoverlay.shortcuts.ShortcutPreset

data class OrbitPage(
    val pageIndex: Int,
    val pageCount: Int,
    val innerRing: List<ShortcutPreset>,
    val outerRing: List<ShortcutPreset>,
) {
    val hasPreviousPage: Boolean
        get() = pageIndex > 0

    val hasNextPage: Boolean
        get() = pageIndex < pageCount - 1
}
