package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profile WHERE id = 1")
    fun getProfile(): Flow<UserProfile?>

    @Query("SELECT * FROM user_profile WHERE id = 1")
    suspend fun getProfileOnce(): UserProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: UserProfile)

    @Update
    suspend fun updateProfile(profile: UserProfile)

    @Query("UPDATE user_profile SET currentXp = :xp, level = :level WHERE id = 1")
    suspend fun updateXpAndLevel(xp: Int, level: Int)

    @Query("UPDATE user_profile SET avatarIndex = :index WHERE id = 1")
    suspend fun updateAvatar(index: Int)

    @Query("UPDATE user_profile SET gender = :gender WHERE id = 1")
    suspend fun updateGender(gender: String)

    @Query("UPDATE user_profile SET language = :lang WHERE id = 1")
    suspend fun updateLanguage(lang: String)

    @Query("UPDATE user_profile SET quoteStyleStrict = :strict WHERE id = 1")
    suspend fun updateQuoteStyle(strict: Boolean)

    @Query("UPDATE user_profile SET interests = :interests WHERE id = 1")
    suspend fun updateInterests(interests: String)

    @Query("UPDATE user_profile SET totalFocusedSeconds = :seconds, totalSessionsCompleted = :completed WHERE id = 1")
    suspend fun updateFocusStats(seconds: Long, completed: Int)

    @Query("UPDATE user_profile SET currentStreak = :streak, bestStreak = :best, lastSessionDate = :lastDate WHERE id = 1")
    suspend fun updateStreak(streak: Int, best: Int, lastDate: Long)

    @Query("UPDATE user_profile SET isLoggedIn = :loggedIn WHERE id = 1")
    suspend fun updateLoginStatus(loggedIn: Boolean)

    @Query("UPDATE user_profile SET pinHash = :pin WHERE id = 1")
    suspend fun updatePin(pin: String)

    @Query("UPDATE user_profile SET nickname = :name WHERE id = 1")
    suspend fun updateNickname(name: String)

    @Query("UPDATE user_profile SET customAvatarUri = :uri WHERE id = 1")
    suspend fun updateCustomAvatarUri(uri: String?)
}
