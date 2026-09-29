package com.example.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [ApiConfig::class, ChatMessage::class, ChatSession::class], version = 4, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao
}
