package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "learning_goals")
data class Goal(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val durationMinutes: Int,
    val isCompleted: Boolean = false,
    val isPomodoro: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
