package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    @Query("SELECT * FROM api_configs")
    fun getAllApiConfigs(): Flow<List<ApiConfig>>

    @Query("SELECT * FROM api_configs WHERE isActive = 1 LIMIT 1")
    fun getActiveApiConfig(): Flow<ApiConfig?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApiConfig(apiConfig: ApiConfig): Long

    @Update
    suspend fun updateApiConfig(apiConfig: ApiConfig)

    @Query("UPDATE api_configs SET isActive = 0")
    suspend fun deactivateAllApiConfigs()

    @Query("UPDATE api_configs SET isActive = 1 WHERE id = :id")
    suspend fun activateApiConfig(id: Int)

    @Query("DELETE FROM api_configs WHERE id = :id")
    suspend fun deleteApiConfigById(id: Int)

    // Chat Sessions (History)
    @Query("SELECT * FROM chat_sessions ORDER BY updatedAt DESC")
    fun getAllChatSessions(): Flow<List<ChatSession>>

    @Query("SELECT * FROM chat_sessions WHERE id = :id LIMIT 1")
    suspend fun getChatSessionById(id: Int): ChatSession?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatSession(session: ChatSession): Long

    @Update
    suspend fun updateChatSession(session: ChatSession)

    @Query("UPDATE chat_sessions SET title = :title, updatedAt = :updatedAt WHERE id = :sessionId")
    suspend fun updateSessionTitleAndTimestamp(sessionId: Int, title: String, updatedAt: Long)

    @Query("UPDATE chat_sessions SET updatedAt = :updatedAt WHERE id = :sessionId")
    suspend fun updateSessionTimestamp(sessionId: Int, updatedAt: Long)

    @Query("DELETE FROM chat_sessions WHERE id = :sessionId")
    suspend fun deleteChatSession(sessionId: Int)

    @Query("DELETE FROM chat_messages WHERE sessionId = :sessionId")
    suspend fun deleteMessagesForSession(sessionId: Int)

    // Messages
    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId ORDER BY timestamp ASC, id ASC")
    fun getMessagesForSession(sessionId: Int): Flow<List<ChatMessage>>

    @Insert
    suspend fun insertChatMessage(message: ChatMessage)

    @Query("DELETE FROM chat_messages WHERE sessionId = :sessionId")
    suspend fun clearSessionMessages(sessionId: Int)

    @Query("DELETE FROM chat_messages")
    suspend fun deleteAllChatMessages()

    @Query("DELETE FROM chat_sessions")
    suspend fun deleteAllChatSessions()

    // Backup & Restore operations
    @Query("SELECT * FROM chat_sessions ORDER BY id ASC")
    suspend fun getAllChatSessionsList(): List<ChatSession>

    @Query("SELECT * FROM chat_messages ORDER BY id ASC")
    suspend fun getAllChatMessagesList(): List<ChatMessage>

    @Query("SELECT * FROM api_configs ORDER BY id ASC")
    suspend fun getAllApiConfigsList(): List<ApiConfig>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatSessions(sessions: List<ChatSession>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatMessages(messages: List<ChatMessage>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApiConfigs(configs: List<ApiConfig>)
}
