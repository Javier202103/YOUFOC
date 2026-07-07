package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey val id: Int = 1, // Single user, always id=1
    val nickname: String = "FocusWarrior",
    val avatarIndex: Int = 0,
    val gender: String = "neutral", // "male", "female", "neutral"
    val language: String = "es",
    val currentXp: Int = 0,
    val level: Int = 1,
    val totalFocusedSeconds: Long = 0,
    val totalSessionsCompleted: Int = 0,
    val totalSessionsFailed: Int = 0,
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val lastSessionDate: Long? = null,
    val quoteStyleStrict: Boolean = false,
    val interests: String = "Programación 💻,Diseño Gráfico 🎨,Lectura 📚,Deporte y Salud 🏃", // CSV
    val longTermGoals: String = "", // CSV of title|completed pairs
    val isLoggedIn: Boolean = false,
    val pinHash: String = "",
    val isRegistered: Boolean = false,
    val customAvatarUri: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    // Bloqueo de datos de cuenta (nickname/email/pin) tras registro
    val isAccountLocked: Boolean = false,
    val email: String = "",
    // Para cuentas de invitado: timestamp de expiración (null = cuenta normal)
    val guestExpiryDate: Long? = null
)
