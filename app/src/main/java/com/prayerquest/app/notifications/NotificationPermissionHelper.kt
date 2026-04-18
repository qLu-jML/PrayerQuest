package com.prayerquest.app.notifications

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.compose.ui.res.stringResource
import com.prayerquest.app.R

/**
 * Android 13 (API 33) introduced POST_NOTIFICATIONS as a runtime permission.
 * Prior to 33 the permission is auto-granted at install time, so
 * [isNotificationPermissionGranted] simply returns true.
 *
 * We check on every schedule path rather than assuming one grant sticks
 * forever — the user can revoke it in system settings at any moment, and we
 * want to silently no-op (not crash) if they do.
 */
object NotificationPermissionHelper {

    /**
     * True if we can post notifications right now. On API < 33 this is
     * always true; on API 33+ it reflects the live permission state.
     */
    fun isNotificationPermissionGranted(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * True iff the device is new enough to *need* a runtime prompt. Used by
     * the onboarding screen to decide whether to show the permission step at
     * all — no point asking on Android 12 where it's already granted.
     */
    fun requiresRuntimePermission(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
}

/**
 * Composable helper that remembers a permission-result launcher for
 * POST_NOTIFICATIONS, tracks the current grant state, and exposes a simple
 * `request()` callback.
 *
 * Usage (typical onboarding step):
 * ```
 * val permission = rememberNotificationPermissionState()
 * Button(onClick = { permission.request() }) { Text(stringResource(R.string.common_allow_notifications)) }
 * if (permission.isGranted) { /* show "You're all set" */ }
 * ```
 *
 * On devices < API 33 [NotificationPermissionState.isGranted] starts as true
 * and [NotificationPermissionState.request] is a no-op, so callers can use
 * the same flow on any API level.
 */
@Composable
fun rememberNotificationPermissionState(): NotificationPermissionState {
    val context = LocalContext.current
    var granted by remember {
        mutableStateOf(NotificationPermissionHelper.isNotificationPermissionGranted(context))
    }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { result ->
        granted = result || !NotificationPermissionHelper.requiresRuntimePermission()
    }
    return remember(launcher) {
        object : NotificationPermissionState {
            override val isGranted: Boolean get() = granted
            override fun request() {
                if (!NotificationPermissionHelper.requiresRuntimePermission()) {
                    granted = true
                    return
                }
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}

/**
 * Minimal contract exposed to UI — callers only need to read [isGranted]
 * and invoke [request]. Kept as an interface so the whole thing can be
 * swapped for a fake in previews / tests.
 */
interface NotificationPermissionState {
    val isGranted: Boolean
    fun request()
}
