package com.frank.voiceoverlay.overlay;

import android.view.View;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelStoreOwner;
import androidx.lifecycle.ViewTreeLifecycleOwner;
import androidx.lifecycle.ViewTreeViewModelStoreOwner;
import androidx.savedstate.SavedStateRegistryOwner;
import androidx.savedstate.ViewTreeSavedStateRegistryOwner;

final class OverlayViewTreeOwnerBridge {
    private OverlayViewTreeOwnerBridge() {}

    static void attach(
            View view,
            LifecycleOwner lifecycleOwner,
            SavedStateRegistryOwner savedStateRegistryOwner,
            ViewModelStoreOwner viewModelStoreOwner
    ) {
        ViewTreeLifecycleOwner.set(view, lifecycleOwner);
        ViewTreeSavedStateRegistryOwner.set(view, savedStateRegistryOwner);
        ViewTreeViewModelStoreOwner.set(view, viewModelStoreOwner);
    }

    static LifecycleOwner lifecycleOwner(View view) {
        return ViewTreeLifecycleOwner.get(view);
    }

    static SavedStateRegistryOwner savedStateRegistryOwner(View view) {
        return ViewTreeSavedStateRegistryOwner.get(view);
    }

    static ViewModelStoreOwner viewModelStoreOwner(View view) {
        return ViewTreeViewModelStoreOwner.get(view);
    }
}
