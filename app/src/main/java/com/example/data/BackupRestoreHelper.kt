package com.example.data

import android.content.SharedPreferences
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject

data class RestoreSummary(
    val sessionsCount: Int = 0,
    val messagesCount: Int = 0,
    val configsCount: Int = 0,
    val success: Boolean = true,
    val message: String = ""
)

object BackupRestoreHelper {

    suspend fun exportAllData(appDao: AppDao, prefs: SharedPreferences): String {
        val root = JSONObject()
        root.put("version", 1)
        root.put("exportedAt", System.currentTimeMillis())

        // 1. Preferences
        val prefsObj = JSONObject()
        prefsObj.put("dark_theme", prefs.getBoolean("dark_theme", true))
        prefsObj.put("detailed_responses", prefs.getBoolean("detailed_responses", true))
        prefsObj.put("system_prompt", prefs.getString("system_prompt", AppRepository.DEFAULT_SYSTEM_PROMPT))
        prefsObj.put("active_persona_id", prefs.getString("active_persona_id", "general"))
        prefsObj.put("custom_persona_prompt", prefs.getString("custom_persona_prompt", ""))
        prefsObj.put("context_window_limit", prefs.getInt("context_window_limit", 20))
        prefsObj.put("tts_enabled", prefs.getBoolean("tts_enabled", true))
        prefsObj.put("tts_auto_speak", prefs.getBoolean("tts_auto_speak", false))
        prefsObj.put("tts_speech_rate", prefs.getFloat("tts_speech_rate", 1.0f).toDouble())
        prefsObj.put("web_search_enabled", prefs.getBoolean("web_search_enabled", true))
        prefsObj.put("active_search_engine_id", prefs.getString("active_search_engine_id", "google"))
        prefsObj.put("custom_search_engines", prefs.getString("custom_search_engines", "[]"))
        prefsObj.put("token_prompt_total", prefs.getLong("token_prompt_total", 0L))
        prefsObj.put("token_completion_total", prefs.getLong("token_completion_total", 0L))
        prefsObj.put("token_requests_total", prefs.getLong("token_requests_total", 0L))
        root.put("preferences", prefsObj)

        // 2. API Configs
        val configs = appDao.getAllApiConfigsList()
        val configsArray = JSONArray()
        for (cfg in configs) {
            val obj = JSONObject()
            obj.put("id", cfg.id)
            obj.put("name", cfg.name)
            obj.put("endpoint", cfg.endpoint)
            obj.put("modelName", cfg.modelName)
            obj.put("apiKey", cfg.apiKey)
            obj.put("isActive", cfg.isActive)
            configsArray.put(obj)
        }
        root.put("apiConfigs", configsArray)

        // 3. Chat Sessions
        val sessions = appDao.getAllChatSessionsList()
        val sessionsArray = JSONArray()
        for (s in sessions) {
            val obj = JSONObject()
            obj.put("id", s.id)
            obj.put("title", s.title)
            obj.put("createdAt", s.createdAt)
            obj.put("updatedAt", s.updatedAt)
            sessionsArray.put(obj)
        }
        root.put("chatSessions", sessionsArray)

        // 4. Chat Messages
        val messages = appDao.getAllChatMessagesList()
        val messagesArray = JSONArray()
        for (m in messages) {
            val obj = JSONObject()
            obj.put("id", m.id)
            obj.put("sessionId", m.sessionId)
            obj.put("role", m.role)
            obj.put("content", m.content)
            obj.put("timestamp", m.timestamp)
            obj.put("imageUri", m.imageUri ?: "")
            obj.put("attachmentName", m.attachmentName ?: "")
            obj.put("attachmentType", m.attachmentType ?: "")
            obj.put("attachmentText", m.attachmentText ?: "")
            messagesArray.put(obj)
        }
        root.put("chatMessages", messagesArray)

        return root.toString(2)
    }

    suspend fun restoreAllData(jsonString: String, appDao: AppDao, prefs: SharedPreferences): RestoreSummary {
        return try {
            val root = JSONObject(jsonString)

            // 1. Preferences
            if (root.has("preferences")) {
                val prefsObj = root.getJSONObject("preferences")
                prefs.edit {
                    if (prefsObj.has("dark_theme")) putBoolean("dark_theme", prefsObj.getBoolean("dark_theme"))
                    if (prefsObj.has("detailed_responses")) putBoolean("detailed_responses", prefsObj.getBoolean("detailed_responses"))
                    if (prefsObj.has("system_prompt")) putString("system_prompt", prefsObj.getString("system_prompt"))
                    if (prefsObj.has("active_persona_id")) putString("active_persona_id", prefsObj.getString("active_persona_id"))
                    if (prefsObj.has("custom_persona_prompt")) putString("custom_persona_prompt", prefsObj.getString("custom_persona_prompt"))
                    if (prefsObj.has("context_window_limit")) putInt("context_window_limit", prefsObj.getInt("context_window_limit"))
                    if (prefsObj.has("tts_enabled")) putBoolean("tts_enabled", prefsObj.getBoolean("tts_enabled"))
                    if (prefsObj.has("tts_auto_speak")) putBoolean("tts_auto_speak", prefsObj.getBoolean("tts_auto_speak"))
                    if (prefsObj.has("tts_speech_rate")) putFloat("tts_speech_rate", prefsObj.getDouble("tts_speech_rate").toFloat())
                    if (prefsObj.has("web_search_enabled")) putBoolean("web_search_enabled", prefsObj.getBoolean("web_search_enabled"))
                    if (prefsObj.has("active_search_engine_id")) putString("active_search_engine_id", prefsObj.getString("active_search_engine_id"))
                    if (prefsObj.has("custom_search_engines")) putString("custom_search_engines", prefsObj.getString("custom_search_engines"))
                    if (prefsObj.has("token_prompt_total")) putLong("token_prompt_total", prefsObj.getLong("token_prompt_total"))
                    if (prefsObj.has("token_completion_total")) putLong("token_completion_total", prefsObj.getLong("token_completion_total"))
                    if (prefsObj.has("token_requests_total")) putLong("token_requests_total", prefsObj.getLong("token_requests_total"))
                }
            }

            // 2. API Configs
            var restoredConfigs = 0
            if (root.has("apiConfigs")) {
                val arr = root.getJSONArray("apiConfigs")
                val list = mutableListOf<ApiConfig>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    list.add(
                        ApiConfig(
                            id = obj.optInt("id", 0),
                            name = obj.getString("name"),
                            endpoint = obj.getString("endpoint"),
                            modelName = obj.getString("modelName"),
                            apiKey = obj.optString("apiKey", ""),
                            isActive = obj.optBoolean("isActive", false)
                        )
                    )
                }
                if (list.isNotEmpty()) {
                    appDao.insertApiConfigs(list)
                    restoredConfigs = list.size
                }
            }

            // 3. Chat Sessions
            var restoredSessions = 0
            if (root.has("chatSessions")) {
                val arr = root.getJSONArray("chatSessions")
                val list = mutableListOf<ChatSession>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    list.add(
                        ChatSession(
                            id = obj.optInt("id", 0),
                            title = obj.optString("title", "Chat"),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                            updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                        )
                    )
                }
                if (list.isNotEmpty()) {
                    appDao.insertChatSessions(list)
                    restoredSessions = list.size
                }
            }

            // 4. Chat Messages
            var restoredMessages = 0
            if (root.has("chatMessages")) {
                val arr = root.getJSONArray("chatMessages")
                val list = mutableListOf<ChatMessage>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    list.add(
                        ChatMessage(
                            id = obj.optInt("id", 0),
                            sessionId = obj.getInt("sessionId"),
                            role = obj.getString("role"),
                            content = obj.getString("content"),
                            timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                            imageUri = obj.optString("imageUri").takeIf { it.isNotBlank() },
                            attachmentName = obj.optString("attachmentName").takeIf { it.isNotBlank() },
                            attachmentType = obj.optString("attachmentType").takeIf { it.isNotBlank() },
                            attachmentText = obj.optString("attachmentText").takeIf { it.isNotBlank() }
                        )
                    )
                }
                if (list.isNotEmpty()) {
                    appDao.insertChatMessages(list)
                    restoredMessages = list.size
                }
            }

            return RestoreSummary(
                sessionsCount = restoredSessions,
                messagesCount = restoredMessages,
                configsCount = restoredConfigs,
                success = true,
                message = "Successfully restored $restoredSessions chats, $restoredMessages messages, and $restoredConfigs endpoints."
            )
        } catch (e: Exception) {
            return RestoreSummary(
                success = false,
                message = "Failed to restore backup: ${e.localizedMessage ?: "Invalid JSON format"}"
            )
        }
    }
}
