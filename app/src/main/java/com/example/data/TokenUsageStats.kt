package com.example.data

data class TokenUsageStats(
    val totalPromptTokens: Long = 0,
    val totalCompletionTokens: Long = 0,
    val totalRequests: Long = 0,
    val lastRequestTokens: Long = 0
) {
    val totalTokens: Long get() = totalPromptTokens + totalCompletionTokens
}
