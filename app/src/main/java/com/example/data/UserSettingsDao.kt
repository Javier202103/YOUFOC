package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface UserSettingsDao {
    @Query("SELECT * FROM user_settings WHERE id = 1")
    fun getSettings(): Flow<UserSettings?>

    @Query("SELECT * FROM user_settings WHERE id = 1")
    suspend fun getSettingsOnce(): UserSettings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettings(settings: UserSettings)

    @Query("UPDATE user_settings SET vpnShieldActive = :active WHERE id = 1")
    suspend fun updateVpnShield(active: Boolean)

    @Query("UPDATE user_settings SET accessibilityLockerActive = :active WHERE id = 1")
    suspend fun updateAccessibilityLocker(active: Boolean)

    @Query("UPDATE user_settings SET waTimerMinutes = :minutes WHERE id = 1")
    suspend fun updateWaTimer(minutes: Int)

    @Query("UPDATE user_settings SET focusSleepEnabled = :enabled WHERE id = 1")
    suspend fun updateFocusSleep(enabled: Boolean)

    @Query("UPDATE user_settings SET forceSleepSimulation = :active WHERE id = 1")
    suspend fun updateForceSleepSimulation(active: Boolean)
}
