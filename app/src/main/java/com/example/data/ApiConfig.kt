package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "api_configs")
data class ApiConfig(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val endpoint: String,
    val modelName: String,
    val apiKey: String,
    val isActive: Boolean = false
)
