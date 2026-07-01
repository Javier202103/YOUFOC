package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_settings")
data class UserSettings(
    @PrimaryKey val id: Int = 1, // Single user settings
    val vpnShieldActive: Boolean = false,
    val accessibilityLockerActive: Boolean = false,
    val waTimerMinutes: Int = 5,
    val focusSleepEnabled: Boolean = true,
    val forceSleepSimulation: Boolean = false,
    val allowedApps: String = "" // Comma-separated package names
)
