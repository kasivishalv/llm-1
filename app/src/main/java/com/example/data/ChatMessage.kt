package com.example.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "chat_messages",
    indices = [Index(value = ["sessionId"])]
)
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sessionId: Int = 0,
    val role: String, // "user", "assistant", or "system"
    val content: String,
    val imageUri: String? = null,
    val attachmentName: String? = null,
    val attachmentType: String? = null,
    val attachmentText: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
