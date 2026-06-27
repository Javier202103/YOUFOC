package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "duels")
data class Duel(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val rivalName: String,
    val rivalAvatar: String,
    val durationHours: Int,
    val xpWager: Int,
    val playerProgress: Float = 0f,
    val rivalProgress: Float = 0f,
    val status: String = "Active", // "Active", "Won", "Lost"
    val createdAt: Long = System.currentTimeMillis()
)
