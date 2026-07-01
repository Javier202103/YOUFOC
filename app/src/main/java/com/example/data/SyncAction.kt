package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_queue")
data class SyncAction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val actionType: String, // e.g., "SYNC_SESSION", "SYNC_PROFILE"
    val payload: String, // JSON payload
    val status: String = "PENDING", // PENDING, PROCESSING, FAILED
    val createdAt: Long = System.currentTimeMillis()
)
