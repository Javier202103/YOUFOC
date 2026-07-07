package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.example.R
import kotlinx.coroutines.*

class TimerOverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var secondsRemaining = 0

    companion object {
        const val CHANNEL_ID = "timer_overlay_channel"
        const val NOTIFICATION_ID = 9002
        const val EXTRA_SECONDS = "extra_seconds"
        var isRunning = false
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        isRunning = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        secondsRemaining = intent?.getIntExtra(EXTRA_SECONDS, 0) ?: 0
        startForeground(NOTIFICATION_ID, buildNotification())
        showOverlay()
        startCountdown()
        return START_STICKY
    }

    private fun startCountdown() {
        serviceScope.launch {
            while (secondsRemaining > 0 && isRunning) {
                delay(1000L)
                secondsRemaining--
                updateOverlayTime()
            }
            if (secondsRemaining <= 0) {
                stopSelf()
            }
        }
    }

    private fun showOverlay() {
        if (overlayView != null) return
        overlayView = LayoutInflater.from(this).inflate(R.layout.overlay_timer_bubble, null)
        updateOverlayTime()

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.END
        params.x = 16
        params.y = 80

        try {
            windowManager?.addView(overlayView, params)
        } catch (e: Exception) {
            // Overlay permission not granted
        }
    }

    private fun updateOverlayTime() {
        val tv = overlayView?.findViewById<TextView>(R.id.tvOverlayTime) ?: return
        val min = secondsRemaining / 60
        val sec = secondsRemaining % 60
        tv.text = String.format("⏱ %02d:%02d", min, sec)
    }

    private fun hideOverlay() {
        overlayView?.let {
            try { windowManager?.removeView(it) } catch (_: Exception) {}
            overlayView = null
        }
    }

    private fun buildNotification(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Cronómetro de Enfoque",
                NotificationManager.IMPORTANCE_LOW
            )
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("⏱ Sesión de Enfoque Activa")
            .setContentText("El cronómetro está corriendo...")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        hideOverlay()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
