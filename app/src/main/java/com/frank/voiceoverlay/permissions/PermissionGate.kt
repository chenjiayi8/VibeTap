package com.frank.voiceoverlay.permissions

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

class PermissionGate(
    private val context: Context,
) {
    fun hasOverlayPermission(): Boolean = Settings.canDrawOverlays(context)

    fun openOverlaySettings() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}"),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    fun openAccessibilitySettings() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}
