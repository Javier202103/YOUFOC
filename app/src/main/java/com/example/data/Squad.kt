package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "squads")
data class Squad(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val membersCount: Int = 4,
    val cumulativeFocusHours: Float = 0f,
    val penaltyXp: Int = 200,
    val health: Int = 100,
    val status: String = "Active", // "Active", "Failed", "Succeeded"
    val createdAt: Long = System.currentTimeMillis()
)
