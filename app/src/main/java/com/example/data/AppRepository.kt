package com.example.data

import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppRepository(private val appDao: AppDao, private val prefs: SharedPreferences) {
    companion object {
        const val DEFAULT_SYSTEM_PROMPT =
            "You are an expert, knowledgeable, and articulate AI assistant. Always provide comprehensive, detailed, in-depth, and well-structured responses. Explain concepts thoroughly with background context, clear explanations, step-by-step reasoning, and practical examples where applicable. Use formatted markdown (headings, bullet points, code blocks) to make answers clear and readable. Never provide overly brief, superficial, or truncated answers."
    }

    private val _isDarkTheme = MutableStateFlow(prefs.getBoolean("dark_theme", true))
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    private val _detailedResponses = MutableStateFlow(prefs.getBoolean("detailed_responses", true))
    val detailedResponses: StateFlow<Boolean> = _detailedResponses.asStateFlow()

    private val _systemPrompt = MutableStateFlow(
        prefs.getString("system_prompt", DEFAULT_SYSTEM_PROMPT) ?: DEFAULT_SYSTEM_PROMPT
    )
    val systemPrompt: StateFlow<String> = _systemPrompt.asStateFlow()

    // Feature 4: Default Persona & System Prompt Presets
    private val _activePersonaId = MutableStateFlow(
        prefs.getString("active_persona_id", "general") ?: "general"
    )
    val activePersonaId: StateFlow<String> = _activePersonaId.asStateFlow()

    private val _customPersonaPrompt = MutableStateFlow(
        prefs.getString("custom_persona_prompt", "") ?: ""
    )
    val customPersonaPrompt: StateFlow<String> = _customPersonaPrompt.asStateFlow()

    // Feature 6: Context Window & History Truncation Limit (0 = Full history, or 6, 12, 24, 40)
    private val _contextWindowLimit = MutableStateFlow(
        prefs.getInt("context_window_limit", 20)
    )
    val contextWindowLimit: StateFlow<Int> = _contextWindowLimit.asStateFlow()

    // Feature 7: Voice Dictation & Text-to-Speech (TTS)
    private val _isTtsEnabled = MutableStateFlow(prefs.getBoolean("tts_enabled", true))
    val isTtsEnabled: StateFlow<Boolean> = _isTtsEnabled.asStateFlow()

    private val _isAutoSpeakEnabled = MutableStateFlow(prefs.getBoolean("tts_auto_speak", false))
    val isAutoSpeakEnabled: StateFlow<Boolean> = _isAutoSpeakEnabled.asStateFlow()

    private val _ttsSpeechRate = MutableStateFlow(prefs.getFloat("tts_speech_rate", 1.0f))
    val ttsSpeechRate: StateFlow<Float> = _ttsSpeechRate.asStateFlow()

    // Feature 8: Token Counter & Usage Statistics
    private val _tokenUsageStats = MutableStateFlow(
        TokenUsageStats(
            totalPromptTokens = prefs.getLong("token_prompt_total", 0L),
            totalCompletionTokens = prefs.getLong("token_completion_total", 0L),
            totalRequests = prefs.getLong("token_requests_total", 0L),
            lastRequestTokens = prefs.getLong("token_last_request", 0L)
        )
    )
    val tokenUsageStats: StateFlow<TokenUsageStats> = _tokenUsageStats.asStateFlow()

    // Web Search Services Management
    private val _isWebSearchEnabled = MutableStateFlow(prefs.getBoolean("web_search_enabled", true))
    val isWebSearchEnabled: StateFlow<Boolean> = _isWebSearchEnabled.asStateFlow()

    private val _activeSearchEngineId = MutableStateFlow(
        prefs.getString("active_search_engine_id", "google") ?: "google"
    )
    val activeSearchEngineId: StateFlow<String> = _activeSearchEngineId.asStateFlow()

    private val _searchEngines = MutableStateFlow(loadSearchEngines())
    val searchEngines: StateFlow<List<SearchEngine>> = _searchEngines.asStateFlow()

    private fun loadSearchEngines(): List<SearchEngine> {
        val customJsonStr = prefs.getString("custom_search_engines", null)
        val customList = mutableListOf<SearchEngine>()
        if (!customJsonStr.isNullOrBlank()) {
            try {
                val jsonArr = org.json.JSONArray(customJsonStr)
                for (i in 0 until jsonArr.length()) {
                    val obj = jsonArr.getJSONObject(i)
                    customList.add(SearchEngine.fromJson(obj))
                }
            } catch (_: Exception) {}
        }
        return SearchEngine.BUILT_IN_ENGINES + customList
    }

    private fun saveCustomSearchEngines(engines: List<SearchEngine>) {
        val customOnly = engines.filter { it.isCustom }
        val jsonArr = org.json.JSONArray()
        customOnly.forEach { jsonArr.put(it.toJson()) }
        prefs.edit { putString("custom_search_engines", jsonArr.toString()) }
        _searchEngines.value = engines
    }

    fun setWebSearchEnabled(enabled: Boolean) {
        prefs.edit { putBoolean("web_search_enabled", enabled) }
        _isWebSearchEnabled.value = enabled
    }

    fun setActiveSearchEngineId(id: String) {
        prefs.edit { putString("active_search_engine_id", id) }
        _activeSearchEngineId.value = id
    }

    fun addCustomSearchEngine(engine: SearchEngine) {
        val current = _searchEngines.value.toMutableList()
        current.add(engine.copy(isCustom = true))
        saveCustomSearchEngines(current)
    }

    fun updateSearchEngine(engine: SearchEngine) {
        val current = _searchEngines.value.toMutableList()
        val index = current.indexOfFirst { it.id == engine.id }
        if (index != -1) {
            current[index] = engine
            saveCustomSearchEngines(current)
        }
    }

    fun deleteCustomSearchEngine(id: String) {
        val current = _searchEngines.value.filterNot { it.id == id && it.isCustom }
        saveCustomSearchEngines(current)
        if (_activeSearchEngineId.value == id) {
            setActiveSearchEngineId("google")
        }
    }

    fun resetSearchEnginesToDefault() {
        prefs.edit { remove("custom_search_engines") }
        _searchEngines.value = SearchEngine.BUILT_IN_ENGINES
        setActiveSearchEngineId("google")
    }

    fun getActiveSearchEngine(): SearchEngine {
        val activeId = _activeSearchEngineId.value
        return _searchEngines.value.firstOrNull { it.id == activeId }
            ?: _searchEngines.value.firstOrNull { it.id == "google" }
            ?: SearchEngine.BUILT_IN_ENGINES.first()
    }

    fun setDarkTheme(isDark: Boolean) {
        prefs.edit { putBoolean("dark_theme", isDark) }
        _isDarkTheme.value = isDark
    }

    fun setDetailedResponses(enabled: Boolean) {
        prefs.edit { putBoolean("detailed_responses", enabled) }
        _detailedResponses.value = enabled
    }

    fun setSystemPrompt(prompt: String) {
        prefs.edit { putString("system_prompt", prompt) }
        _systemPrompt.value = prompt
    }

    fun setActivePersona(personaId: String, customPrompt: String? = null) {
        prefs.edit {
            putString("active_persona_id", personaId)
            if (customPrompt != null) {
                putString("custom_persona_prompt", customPrompt)
            }
        }
        _activePersonaId.value = personaId
        if (customPrompt != null) {
            _customPersonaPrompt.value = customPrompt
        }

        // Update active system prompt if not custom
        val persona = AiPersona.findById(personaId)
        val newPrompt = if (personaId == "custom") {
            customPrompt ?: _customPersonaPrompt.value.ifBlank { DEFAULT_SYSTEM_PROMPT }
        } else {
            persona.prompt
        }
        setSystemPrompt(newPrompt)
    }

    fun getEffectiveSystemPrompt(): String {
        val currentPrompt = _systemPrompt.value
        return if (currentPrompt.isNotBlank()) currentPrompt else DEFAULT_SYSTEM_PROMPT
    }

    fun setContextWindowLimit(limit: Int) {
        prefs.edit { putInt("context_window_limit", limit) }
        _contextWindowLimit.value = limit
    }

    fun setTtsEnabled(enabled: Boolean) {
        prefs.edit { putBoolean("tts_enabled", enabled) }
        _isTtsEnabled.value = enabled
    }

    fun setAutoSpeakEnabled(enabled: Boolean) {
        prefs.edit { putBoolean("tts_auto_speak", enabled) }
        _isAutoSpeakEnabled.value = enabled
    }

    fun setTtsSpeechRate(rate: Float) {
        prefs.edit { putFloat("tts_speech_rate", rate) }
        _ttsSpeechRate.value = rate
    }

    fun recordTokenUsage(promptTokens: Long, completionTokens: Long) {
        val current = _tokenUsageStats.value
        val newPrompt = current.totalPromptTokens + promptTokens
        val newComp = current.totalCompletionTokens + completionTokens
        val newReq = current.totalRequests + 1
        val lastTokens = promptTokens + completionTokens

        prefs.edit {
            putLong("token_prompt_total", newPrompt)
            putLong("token_completion_total", newComp)
            putLong("token_requests_total", newReq)
            putLong("token_last_request", lastTokens)
        }

        _tokenUsageStats.value = TokenUsageStats(
            totalPromptTokens = newPrompt,
            totalCompletionTokens = newComp,
            totalRequests = newReq,
            lastRequestTokens = lastTokens
        )
    }

    fun resetTokenStats() {
        prefs.edit {
            remove("token_prompt_total")
            remove("token_completion_total")
            remove("token_requests_total")
            remove("token_last_request")
        }
        _tokenUsageStats.value = TokenUsageStats()
    }

    suspend fun exportAllData(): String {
        return BackupRestoreHelper.exportAllData(appDao, prefs)
    }

    suspend fun restoreAllData(jsonString: String): RestoreSummary {
        val summary = BackupRestoreHelper.restoreAllData(jsonString, appDao, prefs)
        if (summary.success) {
            // Reload cached states
            _isDarkTheme.value = prefs.getBoolean("dark_theme", true)
            _detailedResponses.value = prefs.getBoolean("detailed_responses", true)
            _systemPrompt.value = prefs.getString("system_prompt", DEFAULT_SYSTEM_PROMPT) ?: DEFAULT_SYSTEM_PROMPT
            _activePersonaId.value = prefs.getString("active_persona_id", "general") ?: "general"
            _customPersonaPrompt.value = prefs.getString("custom_persona_prompt", "") ?: ""
            _contextWindowLimit.value = prefs.getInt("context_window_limit", 20)
            _isTtsEnabled.value = prefs.getBoolean("tts_enabled", true)
            _isAutoSpeakEnabled.value = prefs.getBoolean("tts_auto_speak", false)
            _ttsSpeechRate.value = prefs.getFloat("tts_speech_rate", 1.0f)
            _tokenUsageStats.value = TokenUsageStats(
                totalPromptTokens = prefs.getLong("token_prompt_total", 0L),
                totalCompletionTokens = prefs.getLong("token_completion_total", 0L),
                totalRequests = prefs.getLong("token_requests_total", 0L),
                lastRequestTokens = prefs.getLong("token_last_request", 0L)
            )
            _searchEngines.value = loadSearchEngines()
        }
        return summary
    }

    val apiConfigs: Flow<List<ApiConfig>> = appDao.getAllApiConfigs()
    val activeApiConfig: Flow<ApiConfig?> = appDao.getActiveApiConfig()
    val chatSessions: Flow<List<ChatSession>> = appDao.getAllChatSessions()

    fun getMessagesForSession(sessionId: Int): Flow<List<ChatMessage>> {
        return appDao.getMessagesForSession(sessionId)
    }

    suspend fun addApiConfig(apiConfig: ApiConfig): Long {
        return appDao.insertApiConfig(apiConfig)
    }

    suspend fun updateApiConfig(apiConfig: ApiConfig) {
        appDao.updateApiConfig(apiConfig)
    }

    suspend fun saveAndActivateApiConfig(apiConfig: ApiConfig) {
        appDao.deactivateAllApiConfigs()
        if (apiConfig.id != 0) {
            appDao.updateApiConfig(apiConfig.copy(isActive = true))
            appDao.activateApiConfig(apiConfig.id)
        } else {
            val newId = appDao.insertApiConfig(apiConfig.copy(isActive = true))
            appDao.activateApiConfig(newId.toInt())
        }
    }

    suspend fun setActiveConfig(id: Int) {
        appDao.deactivateAllApiConfigs()
        appDao.activateApiConfig(id)
    }

    suspend fun deleteApiConfig(id: Int) {
        appDao.deleteApiConfigById(id)
    }

    suspend fun createChatSession(title: String = "New Chat"): Long {
        return appDao.insertChatSession(
            ChatSession(
                title = title,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun updateSessionTitleAndTimestamp(sessionId: Int, title: String) {
        appDao.updateSessionTitleAndTimestamp(sessionId, title, System.currentTimeMillis())
    }

    suspend fun deleteChatSession(sessionId: Int) {
        appDao.deleteMessagesForSession(sessionId)
        appDao.deleteChatSession(sessionId)
    }

    suspend fun addChatMessage(
        sessionId: Int,
        role: String,
        content: String,
        imageUri: String? = null,
        attachmentName: String? = null,
        attachmentType: String? = null,
        attachmentText: String? = null
    ) {
        appDao.insertChatMessage(
            ChatMessage(
                sessionId = sessionId,
                role = role,
                content = content,
                imageUri = imageUri,
                attachmentName = attachmentName,
                attachmentType = attachmentType,
                attachmentText = attachmentText
            )
        )
        appDao.updateSessionTimestamp(sessionId, System.currentTimeMillis())
    }

    suspend fun clearSession(sessionId: Int) {
        appDao.clearSessionMessages(sessionId)
    }

    suspend fun clearAllChatHistory() {
        appDao.deleteAllChatMessages()
        appDao.deleteAllChatSessions()
    }
}
