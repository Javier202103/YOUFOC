package com.example.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.Button
import android.widget.TextView
import com.example.R
import com.example.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class FocusAccessibilityService : AccessibilityService() {

    private var windowManager: WindowManager? = null
    private var lockOverlayView: View? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())

    companion object {
        var isSessionActive = false
        var currentGoalTargetApp: String? = null
        var globalAllowedApps: List<String> = emptyList()
        var onBlockEvent: (() -> Unit)? = null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        val info = AccessibilityServiceInfo()
        info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
        info.flags = AccessibilityServiceInfo.DEFAULT
        this.serviceInfo = info
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        loadGlobalAllowedApps()
    }

    private fun loadGlobalAllowedApps() {
        serviceScope.launch {
            val db = AppDatabase.getDatabase(applicationContext)
            val settings = db.userSettingsDao().getSettingsOnce()
            if (settings != null && settings.allowedApps.isNotBlank()) {
                globalAllowedApps = settings.allowedApps.split(",").map { it.trim() }
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!isSessionActive) return

        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString() ?: return

            if (!isAppAllowed(packageName)) {
                Log.d("FocusAccessibility", "Blocked app: $packageName")
                showLockOverlay()
                onBlockEvent?.invoke()
                // Also send user to home screen as an extra measure
                performGlobalAction(GLOBAL_ACTION_HOME)
            } else {
                hideLockOverlay()
            }
        }
    }

    private fun isAppAllowed(packageName: String): Boolean {
        val baseAllowed = listOf(
            this.packageName,
            "com.android.systemui",
            "com.android.launcher",
            "com.google.android.apps.nexuslauncher",
            "com.sec.android.app.launcher", // Samsung launcher
            // Phone / Dialer — para poder contestar llamadas externas
            "com.android.phone",
            "com.android.server.telecom",
            "com.android.incallui",
            "com.google.android.dialer",
            "com.samsung.android.incallui",
            "com.samsung.android.dialer"
        )

        if (baseAllowed.contains(packageName)) return true
        if (globalAllowedApps.contains(packageName)) return true
        if (currentGoalTargetApp != null && packageName == currentGoalTargetApp) return true

        return false
    }

    private fun showLockOverlay() {
        if (lockOverlayView == null) {
            val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            // We will create a simple layout programmatically to avoid needing an XML layout right away, or we can use an XML.
            // Let's create an XML layout next.
            lockOverlayView = inflater.inflate(R.layout.overlay_focus_lock, null)

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                else
                    WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR,
                PixelFormat.TRANSLUCENT
            )
            params.gravity = Gravity.CENTER

            val backButton = lockOverlayView?.findViewById<Button>(R.id.btnBackToFocus)
            backButton?.setOnClickListener {
                hideLockOverlay()
                performGlobalAction(GLOBAL_ACTION_HOME)
            }

            try {
                windowManager?.addView(lockOverlayView, params)
            } catch (e: Exception) {
                Log.e("FocusAccessibility", "Error adding overlay", e)
            }
        }
    }

    private fun hideLockOverlay() {
        if (lockOverlayView != null) {
            try {
                windowManager?.removeView(lockOverlayView)
            } catch (e: Exception) {
                // View might not be attached
            }
            lockOverlayView = null
        }
    }

    override fun onInterrupt() {
        // Required by AccessibilityService
    }
}
