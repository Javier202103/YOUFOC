package com.example.service

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import kotlinx.coroutines.runBlocking
import com.example.data.AppDatabase

class AdminReceiver : DeviceAdminReceiver() {

    companion object {
        private const val FIFTEEN_DAYS_MS = 15L * 24 * 60 * 60 * 1000
    }

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Toast.makeText(context, "🔐 Blindaje de desinstalación activado", Toast.LENGTH_SHORT).show()
    }

    override fun onDisableRequested(context: Context, intent: Intent): CharSequence {
        // Check if 15 days have passed since account creation
        val profile = runBlocking {
            AppDatabase.getDatabase(context).userProfileDao().getProfileOnce()
        }
        val createdAt = profile?.createdAt ?: System.currentTimeMillis()
        val elapsed = System.currentTimeMillis() - createdAt
        val daysRemaining = ((FIFTEEN_DAYS_MS - elapsed) / (24 * 60 * 60 * 1000)).coerceAtLeast(0)

        return if (elapsed < FIFTEEN_DAYS_MS) {
            "⛔ No puedes desinstalar FocusLock todavía. Faltan $daysRemaining días de tu compromiso de 15 días."
        } else {
            "Puedes desactivar el administrador de dispositivo."
        }
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        // Check 15-day rule
        val profile = runBlocking {
            AppDatabase.getDatabase(context).userProfileDao().getProfileOnce()
        }
        val createdAt = profile?.createdAt ?: System.currentTimeMillis()
        val elapsed = System.currentTimeMillis() - createdAt

        if (elapsed < FIFTEEN_DAYS_MS) {
            // Re-enable admin — cannot be done here programmatically, but we show a message
            Toast.makeText(
                context,
                "⛔ El blindaje sigue activo. No podrás desinstalar la app hasta completar 15 días.",
                Toast.LENGTH_LONG
            ).show()
        } else {
            Toast.makeText(context, "Administrador de Dispositivo Desactivado", Toast.LENGTH_SHORT).show()
        }
    }
}
