package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "focus_sessions")
data class FocusSession(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val goalId: Int? = null,
    val goalTitle: String = "",
    val startTime: Long = System.currentTimeMillis(),
    val endTime: Long? = null,
    val durationSeconds: Int = 0,
    val isSuccess: Boolean = false,
    val earnedXp: Int = 0,
    val dayOfWeek: Int = 0,  // 0=Sun, 6=Sat
    val hourOfDay: Int = 0
)
