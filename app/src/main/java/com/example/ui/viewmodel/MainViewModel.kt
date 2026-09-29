package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ApiConfig
import com.example.data.AppRepository
import com.example.data.ChatMessage
import com.example.data.ChatSession
import com.example.data.SearchEngine
import com.example.data.SearchResponse
import com.example.network.ChatMessageDto
import com.example.network.ChatRequestDto
import com.example.network.NetworkModule
import com.example.network.WebSearchService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(val repository: AppRepository) : ViewModel() {

    val isDarkTheme: StateFlow<Boolean> = repository.isDarkTheme
    val detailedResponses: StateFlow<Boolean> = repository.detailedResponses
    val systemPrompt: StateFlow<String> = repository.systemPrompt

    // Feature 4: Persona Presets
    val activePersonaId: StateFlow<String> = repository.activePersonaId
    val customPersonaPrompt: StateFlow<String> = repository.customPersonaPrompt

    // Feature 6: Context Window Limit
    val contextWindowLimit: StateFlow<Int> = repository.contextWindowLimit

    // Feature 7: Voice Dictation & TTS
    val isTtsEnabled: StateFlow<Boolean> = repository.isTtsEnabled
    val isAutoSpeakEnabled: StateFlow<Boolean> = repository.isAutoSpeakEnabled
    val ttsSpeechRate: StateFlow<Float> = repository.ttsSpeechRate

    // Feature 8: Token Usage Statistics
    val tokenUsageStats: StateFlow<com.example.data.TokenUsageStats> = repository.tokenUsageStats

    val apiConfigs: StateFlow<List<ApiConfig>> = repository.apiConfigs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeApiConfig: StateFlow<ApiConfig?> = repository.activeApiConfig
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val chatSessions: StateFlow<List<ChatSession>> = repository.chatSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentSessionId = MutableStateFlow<Int?>(null)
    val currentSessionId: StateFlow<Int?> = _currentSessionId.asStateFlow()

    private val _isIncognitoMode = MutableStateFlow(false)
    private var incognitoMessageId = -1
    val isIncognitoMode: StateFlow<Boolean> = _isIncognitoMode.asStateFlow()

    private val _incognitoMessages = MutableStateFlow<List<ChatMessage>>(emptyList())

    val chatMessages: StateFlow<List<ChatMessage>> = combine(
        _currentSessionId,
        _isIncognitoMode
    ) { sessionId, incognito -> Pair(sessionId, incognito) }
        .flatMapLatest { (sessionId, incognito) ->
            if (incognito) {
                _incognitoMessages
            } else {
                if (sessionId == null) flowOf(emptyList())
                else repository.getMessagesForSession(sessionId)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    private var activeChatJob: kotlinx.coroutines.Job? = null

    init {
        // Auto-select latest session or prepare
        viewModelScope.launch {
            repository.chatSessions.collect { sessions ->
                if (_currentSessionId.value == null && sessions.isNotEmpty()) {
                    _currentSessionId.value = sessions.first().id
                }
            }
        }
    }

    fun selectSession(sessionId: Int) {
        _currentSessionId.value = sessionId
    }

    fun startNewChat() {
        viewModelScope.launch {
            val newId = repository.createChatSession("New Chat")
            _currentSessionId.value = newId.toInt()
        }
    }

    fun deleteSession(sessionId: Int) {
        viewModelScope.launch {
            repository.deleteChatSession(sessionId)
            if (_currentSessionId.value == sessionId) {
                val remaining = chatSessions.value.filter { it.id != sessionId }
                if (remaining.isNotEmpty()) {
                    _currentSessionId.value = remaining.first().id
                } else {
                    _currentSessionId.value = null
                }
            }
        }
    }

    fun toggleIncognitoMode() {
        activeChatJob?.cancel()
        _isLoading.value = false
        _isIncognitoMode.value = !_isIncognitoMode.value
        if (_isIncognitoMode.value) {
            _incognitoMessages.value = emptyList() // clear on entry
            incognitoMessageId = -1
            _currentSessionId.value = null // disconnect from persistent session
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun normalizeEndpoint(raw: String): String {
        var trimmed = raw.trim()
        if (trimmed.isEmpty()) return ""
        if (!trimmed.startsWith("http://", ignoreCase = true) && !trimmed.startsWith("https://", ignoreCase = true)) {
            val isLocal = trimmed.startsWith("localhost", ignoreCase = true) ||
                    trimmed.startsWith("10.0.2.2") ||
                    trimmed.startsWith("127.0.0.1") ||
                    trimmed.startsWith("192.168.")
            trimmed = if (isLocal) "http://$trimmed" else "https://$trimmed"
        }
        trimmed = trimmed.trimEnd('/')

        return when {
            trimmed.endsWith("/chat/completions", ignoreCase = true) -> trimmed
            trimmed.endsWith("/completions", ignoreCase = true) -> trimmed
            trimmed.endsWith("/v1", ignoreCase = true) -> "$trimmed/chat/completions"
            trimmed.endsWith("/openai", ignoreCase = true) -> "$trimmed/chat/completions"
            trimmed.endsWith("/v1beta", ignoreCase = true) -> "$trimmed/chat/completions"
            trimmed.endsWith("/v1alpha", ignoreCase = true) -> "$trimmed/chat/completions"
            else -> "$trimmed/v1/chat/completions"
        }
    }

    fun addApiConfig(name: String, endpoint: String, modelName: String, apiKey: String) {
        viewModelScope.launch {
            val isFirst = apiConfigs.value.isEmpty()
            repository.addApiConfig(
                ApiConfig(
                    name = name.trim(),
                    endpoint = endpoint.trim(),
                    modelName = modelName.trim(),
                    apiKey = apiKey.trim(),
                    isActive = isFirst
                )
            )
        }
    }

    fun updateApiConfig(id: Int, name: String, endpoint: String, modelName: String, apiKey: String) {
        viewModelScope.launch {
            val current = apiConfigs.value.find { it.id == id }
            repository.updateApiConfig(
                ApiConfig(
                    id = id,
                    name = name.trim(),
                    endpoint = endpoint.trim(),
                    modelName = modelName.trim(),
                    apiKey = apiKey.trim(),
                    isActive = current?.isActive ?: false
                )
            )
        }
    }

    fun saveAndActivateApiConfig(id: Int?, name: String, endpoint: String, modelName: String, apiKey: String) {
        viewModelScope.launch {
            repository.saveAndActivateApiConfig(
                ApiConfig(
                    id = id ?: 0,
                    name = name.trim(),
                    endpoint = endpoint.trim(),
                    modelName = modelName.trim(),
                    apiKey = apiKey.trim(),
                    isActive = true
                )
            )
        }
    }

    fun setActiveConfig(id: Int) {
        viewModelScope.launch {
            repository.setActiveConfig(id)
        }
    }

    fun deleteApiConfig(id: Int) {
        viewModelScope.launch {
            val wasActive = apiConfigs.value.find { it.id == id }?.isActive == true
            repository.deleteApiConfig(id)
            if (wasActive) {
                val remaining = apiConfigs.value.filter { it.id != id }
                if (remaining.isNotEmpty()) {
                    repository.setActiveConfig(remaining.first().id)
                }
            }
        }
    }

    fun clearChat() {
        viewModelScope.launch {
            repository.clearAllChatHistory()
            _currentSessionId.value = null
        }
    }

    fun setDarkTheme(isDark: Boolean) {
        repository.setDarkTheme(isDark)
    }

    fun setDetailedResponses(enabled: Boolean) {
        repository.setDetailedResponses(enabled)
    }

    fun setSystemPrompt(prompt: String) {
        repository.setSystemPrompt(prompt)
    }

    fun resetSystemPrompt() {
        repository.setSystemPrompt(AppRepository.DEFAULT_SYSTEM_PROMPT)
    }

    // Web Search Services
    val isWebSearchEnabled: StateFlow<Boolean> = repository.isWebSearchEnabled
    val activeSearchEngineId: StateFlow<String> = repository.activeSearchEngineId
    val searchEngines: StateFlow<List<SearchEngine>> = repository.searchEngines

    fun setWebSearchEnabled(enabled: Boolean) {
        repository.setWebSearchEnabled(enabled)
    }

    fun setActiveSearchEngineId(id: String) {
        repository.setActiveSearchEngineId(id)
    }

    fun addCustomSearchEngine(
        name: String,
        searchUrlTemplate: String,
        description: String = "",
        apiKey: String = ""
    ) {
        val newEngine = SearchEngine(
            id = "custom_${System.currentTimeMillis()}",
            name = name.trim(),
            searchUrlTemplate = searchUrlTemplate.trim(),
            description = description.trim(),
            apiKey = apiKey.trim(),
            isCustom = true,
            isEnabled = true
        )
        repository.addCustomSearchEngine(newEngine)
    }

    fun updateSearchEngine(engine: SearchEngine) {
        repository.updateSearchEngine(engine)
    }

    fun deleteCustomSearchEngine(id: String) {
        repository.deleteCustomSearchEngine(id)
    }

    fun resetSearchEnginesToDefault() {
        repository.resetSearchEnginesToDefault()
    }

    // Feature 4: Persona Presets
    fun setActivePersona(personaId: String, customPrompt: String? = null) {
        repository.setActivePersona(personaId, customPrompt)
    }

    // Feature 6: Context Window
    fun setContextWindowLimit(limit: Int) {
        repository.setContextWindowLimit(limit)
    }

    // Feature 7: Voice Dictation & TTS
    fun setTtsEnabled(enabled: Boolean) {
        repository.setTtsEnabled(enabled)
    }

    fun setAutoSpeakEnabled(enabled: Boolean) {
        repository.setAutoSpeakEnabled(enabled)
    }

    fun setTtsSpeechRate(rate: Float) {
        repository.setTtsSpeechRate(rate)
    }

    // Feature 8: Token Usage Stats
    fun resetTokenStats() {
        repository.resetTokenStats()
    }

    // Feature 3: Data Backup & Restore
    fun exportBackup(onResult: (String) -> Unit) {
        viewModelScope.launch {
            val json = repository.exportAllData()
            onResult(json)
        }
    }

    fun restoreBackup(jsonStr: String, onResult: (com.example.data.RestoreSummary) -> Unit) {
        viewModelScope.launch {
            val res = repository.restoreAllData(jsonStr)
            onResult(res)
        }
    }

    suspend fun testSearch(engine: SearchEngine, query: String): SearchResponse {
        return WebSearchService.search(engine, query, maxResults = 5)
    }

    fun sendMessage(
        content: String,
        imageUri: String? = null,
        imageBase64: String? = null,
        attachmentName: String? = null,
        attachmentType: String? = null,
        attachmentText: String? = null
    ) {
        if (_isLoading.value) return
        val config = activeApiConfig.value
        if (config == null) {
            _errorMessage.value = "No active API configuration. Please set one up in Settings."
            return
        }
        val trimmedContent = content.trim()
        if (trimmedContent.isBlank() && imageBase64.isNullOrBlank() && attachmentText.isNullOrBlank()) return

        _isLoading.value = true

        val displayContent = if (trimmedContent.isNotBlank()) {
            trimmedContent
        } else if (!attachmentName.isNullOrBlank()) {
            "Analyze attached $attachmentName"
        } else {
            "Analyze this image"
        }

        val promptContentForModel = if (!attachmentText.isNullOrBlank()) {
            buildString {
                appendLine("[Attached File: ${attachmentName ?: "Document"}${if (attachmentType != null) " ($attachmentType)" else ""}]")
                appendLine("--- Begin File Content ---")
                appendLine(attachmentText)
                appendLine("--- End File Content ---")
                appendLine()
                appendLine(displayContent)
            }.trim()
        } else {
            displayContent
        }

        activeChatJob?.cancel()
        activeChatJob = viewModelScope.launch {
            val isIncognito = _isIncognitoMode.value

            if (isIncognito && _currentSessionId.value != null) {
                _currentSessionId.value = null
            }

            try {
                // Ensure a session exists
                var sId = _currentSessionId.value
                val isFirstMessageInSession: Boolean
                if (!isIncognito && sId == null) {
                    val title = if (displayContent.length > 28) displayContent.take(28) + "..." else displayContent
                    val newId = repository.createChatSession(title)
                    sId = newId.toInt()
                    _currentSessionId.value = sId
                    isFirstMessageInSession = false
                } else {
                    isFirstMessageInSession = chatMessages.value.isEmpty()
                    if (!isIncognito && isFirstMessageInSession && sId != null) {
                        val title = if (displayContent.length > 28) displayContent.take(28) + "..." else displayContent
                        repository.updateSessionTitleAndTimestamp(sId, title)
                    }
                }

                // 1. Capture conversation history prior to inserting the new user message
                val priorSessionMessages = chatMessages.value

                // 2. Insert user message into local Room DB IMMEDIATELY so it appears right away in the UI
                if (isIncognito) {
                    val userMsg = ChatMessage(
                        id = incognitoMessageId--,
                        sessionId = -1,
                        role = "user",
                        content = displayContent,
                        timestamp = System.currentTimeMillis(),
                        imageUri = imageUri,
                        attachmentName = attachmentName,
                        attachmentType = attachmentType,
                        attachmentText = attachmentText
                    )
                    _incognitoMessages.value = _incognitoMessages.value + userMsg
                } else {
                    repository.addChatMessage(
                        sessionId = sId!!,
                        role = "user",
                        content = displayContent,
                        imageUri = imageUri,
                        attachmentName = attachmentName,
                        attachmentType = attachmentType,
                        attachmentText = attachmentText
                    )
                }

                // 3. Set loading state AFTER the user message is saved to DB and in the chat list
                _isLoading.value = true

                // Perform Web Search Grounding if enabled
                var webGroundingContext: String? = null
                if (repository.isWebSearchEnabled.value && trimmedContent.isNotBlank()) {
                    try {
                        val activeEngine = repository.getActiveSearchEngine()
                        val searchResponse = WebSearchService.search(activeEngine, trimmedContent, maxResults = 4)
                        if (searchResponse.results.isNotEmpty()) {
                            webGroundingContext = buildString {
                                appendLine("[Live Web Search Results from ${activeEngine.name}]")
                                appendLine("Query: \"$trimmedContent\"")
                                appendLine("Search Source: ${searchResponse.searchUrl}")
                                appendLine("--- Web Results ---")
                                searchResponse.results.forEachIndexed { index, item ->
                                    appendLine("${index + 1}. Title: ${item.title}")
                                    appendLine("   Snippet: ${item.snippet}")
                                    appendLine("   URL: ${item.url}")
                                }
                                appendLine("--- End of Search Results ---")
                                appendLine("Instructions for Assistant: You have access to real-time live web search results above. Use this up-to-date web information to directly answer the user's question accurately. Mention relevant website links/sources if appropriate.")
                            }
                        }
                    } catch (_: Exception) {}
                }

                // Determine active system prompt
                val isDetailed = repository.detailedResponses.value
                val activeSystemPrompt = if (isDetailed) {
                    repository.getEffectiveSystemPrompt()
                } else {
                    null
                }

                // Construct history with system instruction at the root (respecting Context Window Limit - Feature 6)
                val limit = repository.contextWindowLimit.value
                val messagesToSend = if (limit > 0 && priorSessionMessages.size > limit) {
                    priorSessionMessages.takeLast(limit)
                } else {
                    priorSessionMessages
                }

                val history = buildList {
                    if (!activeSystemPrompt.isNullOrBlank()) {
                        add(ChatMessageDto(role = "system", content = activeSystemPrompt))
                    }
                    if (!webGroundingContext.isNullOrBlank()) {
                        add(ChatMessageDto(role = "system", content = webGroundingContext))
                    }
                    messagesToSend.forEach {
                        val msgContent = if (!it.attachmentText.isNullOrBlank() && it.role == "user") {
                            buildString {
                                appendLine("[Attached File: ${it.attachmentName ?: "File"}]")
                                appendLine(it.attachmentText)
                                appendLine()
                                appendLine(it.content)
                            }.trim()
                        } else {
                            it.content
                        }
                        add(ChatMessageDto(role = it.role, content = msgContent))
                    }
                    add(ChatMessageDto(role = "user", content = promptContentForModel, imageBase64 = imageBase64))
                }

                val request = ChatRequestDto(
                    model = config.modelName,
                    messages = history,
                    maxTokens = if (isDetailed) 4000 else null
                )
                
                val finalUrl = normalizeEndpoint(config.endpoint)
                val authHeader = if (config.apiKey.isNotBlank()) "Bearer ${config.apiKey.trim()}" else null
                val isOpenRouter = finalUrl.contains("openrouter.ai", ignoreCase = true)

                val response = kotlinx.coroutines.withTimeoutOrNull(30_000L) {
                    NetworkModule.apiService.createChatCompletion(
                        url = finalUrl,
                        authorization = authHeader,
                        request = request,
                        httpReferer = if (isOpenRouter) "https://openrouter.ai" else null,
                        xTitle = if (isOpenRouter) "LLM#1" else null
                    )
                }
                
                if (response == null) {
                    _errorMessage.value = "No internet connection. Please try again."
                    return@launch
                }
                
                val assistantReply = response.choices?.firstOrNull()?.message?.content
                if (assistantReply != null) {
                    // Feature 8: Record Token Usage
                    val promptTokens = response.usage?.promptTokens?.toLong()
                        ?: (promptContentForModel.length / 4L + 80L)
                    val completionTokens = response.usage?.completionTokens?.toLong()
                        ?: (assistantReply.length / 4L).coerceAtLeast(1L)
                    repository.recordTokenUsage(promptTokens, completionTokens)

                    if (isIncognito) {
                        val aiMsg = ChatMessage(
                            id = incognitoMessageId--,
                            sessionId = -1,
                            role = "assistant",
                            content = assistantReply,
                            timestamp = System.currentTimeMillis()
                        )
                        _incognitoMessages.value = _incognitoMessages.value + aiMsg
                    } else {
                        repository.addChatMessage(sId!!, "assistant", assistantReply)
                    }
                } else {
                    _errorMessage.value = "Received empty response from API."
                }

            } catch (e: retrofit2.HttpException) {
                val code = e.code()
                val errorBody = try { e.response()?.errorBody()?.string() } catch (ignored: Exception) { null }
                val parsedServerMsg = parseServerErrorMessage(errorBody)

                _errorMessage.value = when (code) {
                    401 -> parsedServerMsg?.let { "Error 401: $it" }
                        ?: "Error 401 Unauthorized: Invalid or missing API key. Please check your credentials in Settings."
                    403 -> parsedServerMsg?.let { "Error 403: $it" }
                        ?: "Error 403 Forbidden: Access denied for this model or API key."
                    404 -> {
                        if (!parsedServerMsg.isNullOrBlank()) {
                            buildString {
                                append("Provider Error (404): $parsedServerMsg\n\n")
                                if (!imageBase64.isNullOrBlank()) {
                                    append("• Note: The selected model ('${config.modelName}') appears to not support vision/image inputs. Please try selecting a multimodal/vision model (e.g., Gemini 1.5/2.0 Flash, Claude 3.5 Sonnet, GPT-4o) or text-only messages.")
                                } else {
                                    append("• Note: OpenRouter or the provider could not find an active endpoint for '${config.modelName}'. The model may be offline, deprecated, or misspelled.")
                                }
                            }
                        } else {
                            "Error 404 Not Found: Could not reach endpoint (${normalizeEndpoint(config.endpoint)}) or model '${config.modelName}' is unavailable."
                        }
                    }
                    400, 422 -> {
                        val details = parsedServerMsg ?: errorBody ?: e.message()
                        if (!imageBase64.isNullOrBlank() && (details.contains("image", ignoreCase = true) || details.contains("multimodal", ignoreCase = true) || details.contains("vision", ignoreCase = true))) {
                            "Model Error ($code): $details\n\nThis model does not accept images. Please use a vision-capable model (like Gemini, GPT-4o, or Claude)."
                        } else {
                            "Request Error ($code): $details"
                        }
                    }
                    429 -> parsedServerMsg?.let { "Error 429: $it" }
                        ?: "Error 429 Too Many Requests: Rate limit exceeded or quota exhausted. Please try again later."
                    in 500..599 -> parsedServerMsg?.let { "Server Error $code: $it" }
                        ?: "Server Error $code: The LLM provider encountered an internal error. Please try again in a moment."
                    else -> parsedServerMsg ?: "API Error $code: ${errorBody ?: e.message()}"
                }
            } catch (e: java.net.ConnectException) {
                val isLocal = config.endpoint.contains("localhost") || config.endpoint.contains("127.0.0.1")
                _errorMessage.value = if (isLocal) {
                    "Connection refused to localhost. If running on an Android Emulator, use 10.0.2.2 instead of localhost."
                } else {
                    "No internet connection. Please try again."
                }
            } catch (e: java.net.UnknownHostException) {
                _errorMessage.value = "No internet connection. Please try again."
            } catch (e: java.net.SocketTimeoutException) {
                _errorMessage.value = "No internet connection. Please try again."
            } catch (e: java.io.IOException) {
                _errorMessage.value = "No internet connection. Please try again."
            } catch (e: Exception) {
                _errorMessage.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    suspend fun testApiConnection(endpoint: String, modelName: String, apiKey: String): Pair<Boolean, String> {
        val trimmedEndpoint = endpoint.trim()
        val trimmedModel = modelName.trim()
        val trimmedKey = apiKey.trim()

        if (trimmedEndpoint.isBlank()) {
            return Pair(false, "Endpoint URL cannot be empty")
        }
        if (trimmedModel.isBlank()) {
            return Pair(false, "Model identifier cannot be empty")
        }

        return try {
            val finalUrl = normalizeEndpoint(trimmedEndpoint)
            val authHeader = if (trimmedKey.isNotBlank()) "Bearer $trimmedKey" else null
            val isOpenRouter = finalUrl.contains("openrouter.ai", ignoreCase = true)
            val testRequest = ChatRequestDto(
                model = trimmedModel,
                messages = listOf(ChatMessageDto(role = "user", content = "ping")),
                maxTokens = 5
            )
            val response = NetworkModule.apiService.createChatCompletion(
                url = finalUrl,
                authorization = authHeader,
                request = testRequest,
                httpReferer = if (isOpenRouter) "https://openrouter.ai" else null,
                xTitle = if (isOpenRouter) "LLM#1" else null
            )
            val reply = response.choices?.firstOrNull()?.message?.content
            if (reply != null) {
                Pair(true, "Connected successfully! Model responded.")
            } else {
                Pair(true, "Endpoint reached successfully.")
            }
        } catch (e: retrofit2.HttpException) {
            val code = e.code()
            val errorBody = try { e.response()?.errorBody()?.string() } catch (ignored: Exception) { null }
            val serverMsg = parseServerErrorMessage(errorBody)
            val errorDesc = when (code) {
                401 -> serverMsg ?: "Error 401 Unauthorized: Invalid API key."
                403 -> serverMsg ?: "Error 403 Forbidden: Access denied."
                404 -> serverMsg ?: "Error 404 Not Found: Model '$trimmedModel' or endpoint not found."
                429 -> serverMsg ?: "Error 429: Rate limit or quota exceeded."
                else -> serverMsg ?: "HTTP $code: ${e.message()}"
            }
            Pair(false, errorDesc)
        } catch (e: java.net.ConnectException) {
            val isLocal = trimmedEndpoint.contains("localhost") || trimmedEndpoint.contains("127.0.0.1")
            Pair(false, if (isLocal) "Connection refused. Use 10.0.2.2 instead of localhost on Android." else "Connection refused to $trimmedEndpoint")
        } catch (e: java.net.UnknownHostException) {
            Pair(false, "Unknown host: Cannot resolve address for $trimmedEndpoint")
        } catch (e: java.net.SocketTimeoutException) {
            Pair(false, "Connection timed out.")
        } catch (e: Exception) {
            Pair(false, e.localizedMessage ?: "Connection test failed")
        }
    }

    private fun parseServerErrorMessage(errorBody: String?): String? {
        if (errorBody.isNullOrBlank()) return null
        return try {
            val json = org.json.JSONObject(errorBody)
            if (json.has("error")) {
                val errObj = json.optJSONObject("error")
                if (errObj != null) {
                    errObj.optString("message", "").ifBlank { null }
                } else {
                    json.optString("error", "").ifBlank { null }
                }
            } else if (json.has("message")) {
                json.optString("message", "").ifBlank { null }
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}
