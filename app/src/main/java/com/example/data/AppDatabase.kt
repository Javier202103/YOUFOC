package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        Goal::class,
        FocusSession::class,
        UserProfile::class,
        UserSettings::class,
        Duel::class,
        Squad::class,
        SyncAction::class
    ],
    version = 7,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun goalDao(): GoalDao
    abstract fun focusSessionDao(): FocusSessionDao
    abstract fun userProfileDao(): UserProfileDao
    abstract fun userSettingsDao(): UserSettingsDao
    abstract fun duelDao(): DuelDao
    abstract fun squadDao(): SquadDao
    abstract fun syncQueueDao(): SyncQueueDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "focus_lock_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
