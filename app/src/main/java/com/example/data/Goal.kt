package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "goals")
data class Goal(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String = "",
    val description: String = "",
    val durationMinutes: Int = 25,
    val isPomodoro: Boolean = false,
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val category: String = "general",
    val xpReward: Int = 50,
    val targetAppPackage: String? = null,
    // true = usuario puede terminar antes con botón; false = el cronómetro dicta el fin
    val allowEarlyComplete: Boolean = true
)
