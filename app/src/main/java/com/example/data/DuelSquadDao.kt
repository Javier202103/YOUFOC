package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DuelDao {
    @Query("SELECT * FROM duels ORDER BY createdAt DESC")
    fun getAllDuels(): Flow<List<Duel>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDuel(duel: Duel)

    @Update
    suspend fun updateDuel(duel: Duel)

    @Query("UPDATE duels SET status = :status, playerProgress = :progress WHERE id = :id")
    suspend fun updateDuelStatus(id: Int, status: String, progress: Float)

    @Query("SELECT COUNT(*) FROM duels WHERE status = 'Won'")
    suspend fun getWonDuelsCount(): Int
}

@Dao
interface SquadDao {
    @Query("SELECT * FROM squads ORDER BY createdAt DESC")
    fun getAllSquads(): Flow<List<Squad>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSquad(squad: Squad)

    @Update
    suspend fun updateSquad(squad: Squad)

    @Query("UPDATE squads SET health = :health, status = :status WHERE id = :id")
    suspend fun updateSquadHealth(id: Int, health: Int, status: String)
}
