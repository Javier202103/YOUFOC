package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface SyncQueueDao {
    @Insert
    suspend fun insertSyncAction(action: SyncAction)

    @Query("SELECT * FROM sync_queue WHERE status = 'PENDING' ORDER BY createdAt ASC")
    suspend fun getPendingActions(): List<SyncAction>

    @Update
    suspend fun updateSyncAction(action: SyncAction)

    @Query("DELETE FROM sync_queue WHERE status = 'COMPLETED'")
    suspend fun deleteCompletedActions()
}
