package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FocusSessionDao {
    @Query("SELECT * FROM focus_sessions ORDER BY startTime DESC")
    fun getAllSessions(): Flow<List<FocusSession>>

    @Query("SELECT * FROM focus_sessions WHERE isSuccess = 1 ORDER BY startTime DESC")
    fun getSuccessfulSessions(): Flow<List<FocusSession>>

    @Query("SELECT COUNT(*) FROM focus_sessions WHERE isSuccess = 1")
    suspend fun getSuccessfulSessionCount(): Int

    @Query("SELECT COUNT(*) FROM focus_sessions")
    suspend fun getTotalSessionCount(): Int

    @Query("SELECT COALESCE(SUM(durationSeconds), 0) FROM focus_sessions WHERE isSuccess = 1")
    suspend fun getTotalFocusedSeconds(): Long

    @Query("SELECT COUNT(*) FROM focus_sessions WHERE isSuccess = 0")
    suspend fun getFailedSessionCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: FocusSession)

    // For analytics: group by dayOfWeek and hourOfDay
    @Query("""
        SELECT dayOfWeek, hourOfDay,
               COUNT(*) as totalSessions,
               SUM(CASE WHEN isSuccess = 1 THEN 1 ELSE 0 END) as successCount,
               SUM(CASE WHEN isSuccess = 0 THEN 1 ELSE 0 END) as failedCount
        FROM focus_sessions
        GROUP BY dayOfWeek, hourOfDay
        ORDER BY dayOfWeek, hourOfDay
    """)
    suspend fun getSessionAnalytics(): List<SessionAnalytics>

    // For weekly chart data
    @Query("""
        SELECT dayOfWeek,
               SUM(CASE WHEN isSuccess = 1 THEN 1 ELSE 0 END) as successCount,
               SUM(CASE WHEN isSuccess = 0 THEN 1 ELSE 0 END) as failedCount
        FROM focus_sessions
        GROUP BY dayOfWeek
        ORDER BY dayOfWeek
    """)
    suspend fun getWeeklyStats(): List<WeeklyStats>
}

data class SessionAnalytics(
    val dayOfWeek: Int,
    val hourOfDay: Int,
    val totalSessions: Int,
    val successCount: Int,
    val failedCount: Int
)

data class WeeklyStats(
    val dayOfWeek: Int,
    val successCount: Int,
    val failedCount: Int
)
