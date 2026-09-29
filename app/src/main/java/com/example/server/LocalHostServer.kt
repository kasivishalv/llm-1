package com.example.server

import com.example.data.ApiConfig
import com.example.data.AppRepository
import com.example.network.ChatMessageDto
import com.example.network.ChatRequestDto
import com.example.network.NetworkModule
import com.example.network.WebSearchService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket
import java.net.URLDecoder

class LocalHostServer(
    val port: Int,
    val deviceIp: String,
    private val repository: AppRepository,
    private val onRequestHandled: () -> Unit,
    private val onError: (String) -> Unit
) {
    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    fun start() {
        val ss = ServerSocket(port).apply {
            reuseAddress = true
        }
        serverSocket = ss

        serverJob = scope.launch {
            while (isActive && !ss.isClosed) {
                try {
                    val clientSocket = ss.accept()
                    launch {
                        handleClient(clientSocket)
                    }
                } catch (e: Exception) {
                    if (!ss.isClosed) {
                        onError("Socket accept error: ${e.message}")
                    }
                }
            }
        }
    }

    fun stop() {
        try {
            serverJob?.cancel()
            serverSocket?.close()
        } catch (_: Exception) {}
        serverSocket = null
    }

    private suspend fun handleClient(socket: Socket) = withContext(Dispatchers.IO) {
        try {
            socket.soTimeout = 30000
            val input = socket.getInputStream()
            val reader = BufferedReader(InputStreamReader(input, Charsets.UTF_8))
            val output = socket.getOutputStream()

            val requestLine = reader.readLine() ?: return@withContext
            val parts = requestLine.split(" ")
            if (parts.size < 2) return@withContext

            val method = parts[0].uppercase()
            val rawUri = parts[1]

            val uriParts = rawUri.split("?", limit = 2)
            val path = uriParts[0]
            val queryString = if (uriParts.size > 1) uriParts[1] else null
            val queryParams = parseQueryParams(queryString)

            // Read HTTP headers
            var contentLength = 0
            var authHeader: String? = null
            var xPasscodeHeader: String? = null
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                if (line.isNullOrBlank()) break
                val header = line!!
                val colonIdx = header.indexOf(':')
                if (colonIdx > 0) {
                    val key = header.substring(0, colonIdx).trim()
                    val value = header.substring(colonIdx + 1).trim()
                    if (key.equals("Content-Length", ignoreCase = true)) {
                        contentLength = value.toIntOrNull() ?: 0
                    } else if (key.equals("Authorization", ignoreCase = true)) {
                        authHeader = value
                    } else if (key.equals("X-Passcode", ignoreCase = true)) {
                        xPasscodeHeader = value
                    }
                }
            }

            // Read body if content length > 0
            val body = if (contentLength > 0) {
                val buf = CharArray(contentLength)
                var readTotal = 0
                while (readTotal < contentLength) {
                    val read = reader.read(buf, readTotal, contentLength - readTotal)
                    if (read == -1) break
                    readTotal += read
                }
                String(buf, 0, readTotal)
            } else {
                ""
            }

            onRequestHandled()

            // Handle CORS preflight
            if (method == "OPTIONS") {
                sendCorsPreflight(output)
                return@withContext
            }

            val requestStartTime = System.currentTimeMillis()
            val clientIp = socket.inetAddress?.hostAddress ?: "Unknown"

            // Helper for logging and responding
            fun respond(status: Int, statusText: String, contentType: String, respBody: String, detail: String? = null) {
                val duration = System.currentTimeMillis() - requestStartTime
                LocalHostManager.logRequest(
                    ServerLogEntry(
                        clientIp = clientIp,
                        method = method,
                        path = path,
                        statusCode = status,
                        durationMs = duration,
                        detail = detail
                    )
                )
                sendResponse(output, status, statusText, contentType, respBody)
            }

            fun respondJson(status: Int, json: String, detail: String? = null) {
                val duration = System.currentTimeMillis() - requestStartTime
                LocalHostManager.logRequest(
                    ServerLogEntry(
                        clientIp = clientIp,
                        method = method,
                        path = path,
                        statusCode = status,
                        durationMs = duration,
                        detail = detail
                    )
                )
                sendJsonResponse(output, status, json)
            }

            // Feature 5: IP Access Restriction (Whitelist) check
            if (!LocalHostManager.isIpAllowed(clientIp)) {
                respond(403, "Forbidden", "text/plain; charset=utf-8", "Access Denied: IP address ($clientIp) is not whitelisted.", "IP Blocked: $clientIp")
                return@withContext
            }

            // Route Requests
            when {
                // Serve Web Interface
                (method == "GET" && (path == "/" || path == "/index.html")) -> {
                    val activeConfig = repository.activeApiConfig.firstOrNull()
                    val activeModel = activeConfig?.modelName ?: "No model configured"
                    val isProtected = LocalHostManager.isPasscodeEnabled.value
                    val html = LocalHostWebPage.getHtml(deviceIp, port, activeModel, isProtected)
                    respond(200, "OK", "text/html; charset=utf-8", html, "Web App Served")
                }

                // Auth Verify API (Passcode)
                (method == "POST" && path == "/api/auth/verify") -> {
                    val json = JSONObject(body.ifBlank { "{}" })
                    val submittedPin = json.optString("passcode", "").trim()
                    val actualPin = LocalHostManager.passcode.value.trim()
                    if (!LocalHostManager.isPasscodeEnabled.value || submittedPin == actualPin) {
                        respondJson(200, """{"success":true,"message":"Authenticated"}""", "Passcode Validated")
                    } else {
                        respondJson(401, """{"success":false,"error":"Invalid passcode"}""", "Passcode Rejected")
                    }
                }

                // QR Code Image endpoint
                (method == "GET" && path == "/api/qr") -> {
                    val textToEncode = queryParams["url"] ?: "http://$deviceIp:$port"
                    val bitmap = QrCodeGenerator.generateQrBitmap(textToEncode, 400)
                    val stream = java.io.ByteArrayOutputStream()
                    bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, stream)
                    val pngBytes = stream.toByteArray()
                    val header = "HTTP/1.1 200 OK\r\n" +
                            "Content-Type: image/png\r\n" +
                            "Content-Length: ${pngBytes.size}\r\n" +
                            "Access-Control-Allow-Origin: *\r\n" +
                            "Connection: close\r\n\r\n"
                    output.write(header.toByteArray(Charsets.UTF_8))
                    output.write(pngBytes)
                    output.flush()
                    LocalHostManager.logRequest(
                        ServerLogEntry(
                            clientIp = clientIp,
                            method = method,
                            path = path,
                            statusCode = 200,
                            durationMs = System.currentTimeMillis() - requestStartTime,
                            detail = "QR Code Generated"
                        )
                    )
                }

                // Status API
                (method == "GET" && path == "/api/status") -> {
                    val activeConfig = repository.activeApiConfig.firstOrNull()
                    val json = JSONObject().apply {
                        put("status", "online")
                        put("ip", deviceIp)
                        put("port", port)
                        put("activeModel", activeConfig?.modelName ?: "None")
                        put("activeProvider", activeConfig?.endpoint ?: "None")
                        put("isPasscodeProtected", LocalHostManager.isPasscodeEnabled.value)
                        put("totalRequests", LocalHostManager.requestCount.value)
                    }
                    respondJson(200, json.toString(), "Status Query")
                }

                // Live Server Logs API
                (method == "GET" && path == "/api/logs") -> {
                    val logs = LocalHostManager.serverLogs.value
                    val arr = JSONArray()
                    logs.forEach { log ->
                        arr.put(JSONObject().apply {
                            put("id", log.id)
                            put("timestamp", log.timestamp)
                            put("clientIp", log.clientIp)
                            put("method", log.method)
                            put("path", log.path)
                            put("statusCode", log.statusCode)
                            put("durationMs", log.durationMs)
                            put("detail", log.detail ?: "")
                        })
                    }
                    val res = JSONObject().apply {
                        put("logs", arr)
                    }
                    respondJson(200, res.toString(), "Fetched Logs")
                }

                // Clear Logs API
                (method == "POST" && path == "/api/logs/clear") -> {
                    LocalHostManager.clearLogs()
                    respondJson(200, """{"success":true}""", "Cleared Logs")
                }

                // Models API
                (method == "GET" && path == "/api/models") -> {
                    // Check passcode if protected
                    if (isPasscodeRequired(xPasscodeHeader)) {
                        respondJson(401, """{"error":"Unauthorized: Passcode required"}""", "Unauthorized")
                        return@withContext
                    }
                    val configs = repository.apiConfigs.first()
                    val activeConfig = repository.activeApiConfig.firstOrNull()
                    val jsonArray = JSONArray()
                    configs.forEach { cfg ->
                        val obj = JSONObject().apply {
                            put("id", cfg.id)
                            put("displayName", "${cfg.modelName} (${cfg.endpoint})")
                            put("modelName", cfg.modelName)
                            put("endpoint", cfg.endpoint)
                            put("isActive", cfg.id == activeConfig?.id || cfg.isActive)
                        }
                        jsonArray.put(obj)
                    }
                    val res = JSONObject().apply {
                        put("activeId", activeConfig?.id ?: -1)
                        put("models", jsonArray)
                    }
                    respondJson(200, res.toString(), "Models List")
                }

                // Select Model API
                (method == "POST" && path == "/api/models/select") -> {
                    if (isPasscodeRequired(xPasscodeHeader)) {
                        respondJson(401, """{"error":"Unauthorized: Passcode required"}""", "Unauthorized")
                        return@withContext
                    }
                    val json = JSONObject(body.ifBlank { "{}" })
                    val id = json.optInt("id", -1)
                    if (id > 0) {
                        repository.setActiveConfig(id)
                        respondJson(200, """{"success":true,"selectedId":$id}""", "Model Selected: $id")
                    } else {
                        respondJson(400, """{"error":"Invalid model id"}""", "Invalid Model ID")
                    }
                }

                // Sessions list
                (method == "GET" && path == "/api/sessions") -> {
                    if (isPasscodeRequired(xPasscodeHeader)) {
                        respondJson(401, """{"error":"Unauthorized: Passcode required"}""", "Unauthorized")
                        return@withContext
                    }
                    val sessions = repository.chatSessions.first()
                    val arr = JSONArray()
                    sessions.forEach { s ->
                        val obj = JSONObject().apply {
                            put("id", s.id)
                            put("title", s.title)
                            put("createdAt", s.createdAt)
                            put("updatedAt", s.updatedAt)
                        }
                        arr.put(obj)
                    }
                    val res = JSONObject().apply {
                        put("sessions", arr)
                    }
                    respondJson(200, res.toString(), "Sessions Count: ${sessions.size}")
                }

                // Create Session
                (method == "POST" && path == "/api/sessions") -> {
                    if (isPasscodeRequired(xPasscodeHeader)) {
                        respondJson(401, """{"error":"Unauthorized: Passcode required"}""", "Unauthorized")
                        return@withContext
                    }
                    val json = JSONObject(body.ifBlank { "{}" })
                    val title = json.optString("title", "Web Chat")
                    val newId = repository.createChatSession(title)
                    respondJson(200, """{"id":$newId,"title":"$title"}""", "Session Created: $newId")
                }

                // Delete Session
                (method == "DELETE" && path == "/api/sessions") -> {
                    if (isPasscodeRequired(xPasscodeHeader)) {
                        respondJson(401, """{"error":"Unauthorized: Passcode required"}""", "Unauthorized")
                        return@withContext
                    }
                    val id = queryParams["id"]?.toIntOrNull()
                    if (id != null) {
                        repository.deleteChatSession(id)
                        respondJson(200, """{"success":true}""", "Session Deleted: $id")
                    } else {
                        respondJson(400, """{"error":"Missing session id"}""", "Missing Session ID")
                    }
                }

                // Export Session Chat (JSON or Markdown)
                (method == "GET" && path == "/api/export") -> {
                    if (isPasscodeRequired(xPasscodeHeader)) {
                        respondJson(401, """{"error":"Unauthorized: Passcode required"}""", "Unauthorized")
                        return@withContext
                    }
                    val sessionId = queryParams["sessionId"]?.toIntOrNull()
                    val format = queryParams["format"] ?: "markdown"
                    if (sessionId == null) {
                        respondJson(400, """{"error":"Missing sessionId"}""", "Missing Session ID")
                    } else {
                        val messages = repository.getMessagesForSession(sessionId).first()
                        val sessions = repository.chatSessions.first()
                        val currentSession = sessions.firstOrNull { it.id == sessionId }
                        val sessionTitle = currentSession?.title ?: "Chat Export"

                        if (format.equals("json", ignoreCase = true)) {
                            val arr = JSONArray()
                            messages.forEach { m ->
                                arr.put(JSONObject().apply {
                                    put("role", m.role)
                                    put("content", m.content)
                                    put("attachmentName", m.attachmentName)
                                    put("timestamp", m.timestamp)
                                })
                            }
                            val exportJson = JSONObject().apply {
                                put("title", sessionTitle)
                                put("sessionId", sessionId)
                                put("exportedAt", System.currentTimeMillis())
                                put("messages", arr)
                            }
                            val rawJson = exportJson.toString(2)
                            val filename = "chat_export_${sessionId}.json"
                            val bodyBytes = rawJson.toByteArray(Charsets.UTF_8)
                            val header = "HTTP/1.1 200 OK\r\n" +
                                    "Content-Type: application/json; charset=utf-8\r\n" +
                                    "Content-Disposition: attachment; filename=\"$filename\"\r\n" +
                                    "Content-Length: ${bodyBytes.size}\r\n" +
                                    "Access-Control-Allow-Origin: *\r\n" +
                                    "Connection: close\r\n\r\n"
                            output.write(header.toByteArray(Charsets.UTF_8))
                            output.write(bodyBytes)
                            output.flush()
                            LocalHostManager.logRequest(
                                ServerLogEntry(
                                    clientIp = clientIp,
                                    method = method,
                                    path = path,
                                    statusCode = 200,
                                    durationMs = System.currentTimeMillis() - requestStartTime,
                                    detail = "Exported JSON ($sessionId)"
                                )
                            )
                        } else {
                            // Markdown format
                            val md = buildString {
                                appendLine("# $sessionTitle")
                                appendLine()
                                appendLine("> Exported from OpenLLM Local Gateway")
                                appendLine()
                                messages.forEach { m ->
                                    val speaker = if (m.role == "user") "👤 **User**" else "🤖 **Assistant**"
                                    appendLine("### $speaker")
                                    if (!m.attachmentName.isNullOrBlank()) {
                                        appendLine("📎 *Attachment: ${m.attachmentName}*")
                                        appendLine()
                                    }
                                    appendLine(m.content)
                                    appendLine()
                                    appendLine("---")
                                    appendLine()
                                }
                            }
                            val filename = "chat_export_${sessionId}.md"
                            val bodyBytes = md.toByteArray(Charsets.UTF_8)
                            val header = "HTTP/1.1 200 OK\r\n" +
                                    "Content-Type: text/markdown; charset=utf-8\r\n" +
                                    "Content-Disposition: attachment; filename=\"$filename\"\r\n" +
                                    "Content-Length: ${bodyBytes.size}\r\n" +
                                    "Access-Control-Allow-Origin: *\r\n" +
                                    "Connection: close\r\n\r\n"
                            output.write(header.toByteArray(Charsets.UTF_8))
                            output.write(bodyBytes)
                            output.flush()
                            LocalHostManager.logRequest(
                                ServerLogEntry(
                                    clientIp = clientIp,
                                    method = method,
                                    path = path,
                                    statusCode = 200,
                                    durationMs = System.currentTimeMillis() - requestStartTime,
                                    detail = "Exported Markdown ($sessionId)"
                                )
                            )
                        }
                    }
                }

                // Get Messages for Session
                (method == "GET" && path == "/api/messages") -> {
                    if (isPasscodeRequired(xPasscodeHeader)) {
                        respondJson(401, """{"error":"Unauthorized: Passcode required"}""", "Unauthorized")
                        return@withContext
                    }
                    val sessionId = queryParams["sessionId"]?.toIntOrNull()
                    if (sessionId == null) {
                        respondJson(400, """{"error":"Missing sessionId"}""", "Missing Session ID")
                    } else {
                        val messages = repository.getMessagesForSession(sessionId).first()
                        val arr = JSONArray()
                        messages.forEach { m ->
                            val obj = JSONObject().apply {
                                put("id", m.id)
                                put("role", m.role)
                                put("content", m.content)
                                put("attachmentName", m.attachmentName)
                                put("attachmentType", m.attachmentType)
                                put("attachmentText", m.attachmentText)
                                put("imageUri", m.imageUri)
                            }
                            arr.put(obj)
                        }
                        val res = JSONObject().apply {
                            put("sessionId", sessionId)
                            put("messages", arr)
                        }
                        respondJson(200, res.toString(), "Fetched ${messages.size} Messages")
                    }
                }

                // Web Gateway Chat API
                (method == "POST" && path == "/api/chat") -> {
                    if (isPasscodeRequired(xPasscodeHeader)) {
                        respondJson(401, """{"error":"Unauthorized: Passcode required"}""", "Unauthorized")
                        return@withContext
                    }
                    handleWebChat(output, body, clientIp, requestStartTime)
                }

                // OpenAI Standard: GET /v1/models
                (method == "GET" && path == "/v1/models") -> {
                    val configs = repository.apiConfigs.first()
                    val active = repository.activeApiConfig.firstOrNull()
                    val dataArr = JSONArray()
                    configs.forEach { cfg ->
                        dataArr.put(JSONObject().apply {
                            put("id", cfg.modelName)
                            put("object", "model")
                            put("created", System.currentTimeMillis() / 1000)
                            put("owned_by", "openllm-local")
                        })
                    }
                    if (dataArr.length() == 0 && active != null) {
                        dataArr.put(JSONObject().apply {
                            put("id", active.modelName)
                            put("object", "model")
                            put("created", System.currentTimeMillis() / 1000)
                            put("owned_by", "openllm-local")
                        })
                    }
                    val res = JSONObject().apply {
                        put("object", "list")
                        put("data", dataArr)
                    }
                    respondJson(200, res.toString(), "OpenAI Models List")
                }

                // OpenAI Standard: POST /v1/chat/completions
                (method == "POST" && path == "/v1/chat/completions") -> {
                    handleOpenAiChatCompletions(output, body, authHeader)
                    LocalHostManager.logRequest(
                        ServerLogEntry(
                            clientIp = clientIp,
                            method = method,
                            path = path,
                            statusCode = 200,
                            durationMs = System.currentTimeMillis() - requestStartTime,
                            detail = "OpenAI Chat Completion"
                        )
                    )
                }

                else -> {
                    respondJson(404, """{"error":"Route not found: $path"}""", "Not Found")
                }
            }

        } catch (e: Exception) {
            try {
                sendJsonResponse(socket.getOutputStream(), 500, """{"error":"${e.message?.replace("\"", "\\\"")}"}""")
            } catch (_: Exception) {}
        } finally {
            try {
                socket.close()
            } catch (_: Exception) {}
        }
    }

    private fun isPasscodeRequired(xPasscodeHeader: String?): Boolean {
        if (!LocalHostManager.isPasscodeEnabled.value) return false
        val expected = LocalHostManager.passcode.value.trim()
        return xPasscodeHeader?.trim() != expected
    }

    private suspend fun handleWebChat(output: OutputStream, body: String, clientIp: String, requestStartTime: Long) {
        val req = JSONObject(body.ifBlank { "{}" })
        val userPrompt = req.optString("message", "").trim()
        val webSearchRequested = req.optBoolean("webSearch", true)
        var sId = req.optInt("sessionId", -1)

        // Attachment info (Feature 5)
        val attachmentName = req.optString("attachmentName", "").takeIf { it.isNotBlank() }
        val attachmentType = req.optString("attachmentType", "").takeIf { it.isNotBlank() }
        val attachmentText = req.optString("attachmentText", "").takeIf { it.isNotBlank() }
        val imageBase64 = req.optString("imageBase64", "").takeIf { it.isNotBlank() }

        if (userPrompt.isBlank() && attachmentText.isNullOrBlank() && imageBase64.isNullOrBlank()) {
            sendJsonResponse(output, 400, """{"error":"Message is empty"}""")
            return
        }

        val config = repository.activeApiConfig.firstOrNull()
        if (config == null) {
            sendJsonResponse(output, 400, """{"error":"No active model configured in phone app. Open the app to configure an API key."}""")
            return
        }

        // Create session if not provided
        if (sId <= 0) {
            val preview = if (userPrompt.isNotBlank()) userPrompt else (attachmentName ?: "Web Attachment Chat")
            val title = if (preview.length > 28) preview.take(28) + "..." else preview
            sId = repository.createChatSession(title).toInt()
        }

        // Combine prompt with attached document context if present
        val effectivePrompt = buildString {
            if (!attachmentName.isNullOrBlank() && !attachmentText.isNullOrBlank()) {
                appendLine("[Attached File: $attachmentName]")
                appendLine("```")
                appendLine(attachmentText.take(12000))
                appendLine("```")
                appendLine()
            }
            if (userPrompt.isNotBlank()) {
                append(userPrompt)
            } else if (!attachmentName.isNullOrBlank()) {
                append("Please analyze the attached file '$attachmentName' above.")
            }
        }

        // Real-time web search grounding if enabled
        var webGroundingContext: String? = null
        if (webSearchRequested && repository.isWebSearchEnabled.value && userPrompt.isNotBlank()) {
            try {
                val activeEngine = repository.getActiveSearchEngine()
                val searchRes = WebSearchService.search(activeEngine, userPrompt, maxResults = 4)
                if (searchRes.results.isNotEmpty()) {
                    webGroundingContext = buildString {
                        appendLine("[Live Web Search Results from ${activeEngine.name}]")
                        appendLine("Query: \"$userPrompt\"")
                        appendLine("Search Source: ${searchRes.searchUrl}")
                        appendLine("--- Web Results ---")
                        searchRes.results.forEachIndexed { index, item ->
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

        // Build history from DB (respecting Context Window Limit - Feature 6)
        val allSessionMessages = repository.getMessagesForSession(sId).first()
        val limit = repository.contextWindowLimit.value
        val existingMessages = if (limit > 0 && allSessionMessages.size > limit) {
            allSessionMessages.takeLast(limit)
        } else {
            allSessionMessages
        }

        val isDetailed = repository.detailedResponses.value
        val activeSystemPrompt = if (isDetailed) {
            repository.getEffectiveSystemPrompt()
        } else {
            null
        }

        val history = buildList {
            if (!activeSystemPrompt.isNullOrBlank()) {
                add(ChatMessageDto(role = "system", content = activeSystemPrompt))
            }
            if (!webGroundingContext.isNullOrBlank()) {
                add(ChatMessageDto(role = "system", content = webGroundingContext))
            }
            existingMessages.forEach {
                add(ChatMessageDto(role = it.role, content = it.content))
            }
            add(ChatMessageDto(role = "user", content = effectivePrompt))
        }

        // Save user message to Room DB
        repository.addChatMessage(
            sessionId = sId,
            role = "user",
            content = userPrompt.ifBlank { "Analyzed attachment: ${attachmentName ?: "file"}" },
            attachmentName = attachmentName,
            attachmentType = attachmentType,
            attachmentText = attachmentText,
            imageUri = imageBase64
        )

        val request = ChatRequestDto(
            model = config.modelName,
            messages = history,
            maxTokens = if (isDetailed) 4000 else null
        )

        val finalUrl = normalizeEndpoint(config.endpoint)
        val auth = if (config.apiKey.isNotBlank()) "Bearer ${config.apiKey.trim()}" else null
        val isOpenRouter = finalUrl.contains("openrouter.ai", ignoreCase = true)

        val response = NetworkModule.apiService.createChatCompletion(
            url = finalUrl,
            authorization = auth,
            request = request,
            httpReferer = if (isOpenRouter) "https://openrouter.ai" else null,
            xTitle = if (isOpenRouter) "OpenLLM Local Gateway" else null
        )

        val reply = response.choices?.firstOrNull()?.message?.content ?: "(No response generated)"

        // Feature 8: Record Token Usage
        val promptTokens = response.usage?.promptTokens?.toLong() 
            ?: (effectivePrompt.length / 4L + 50L)
        val completionTokens = response.usage?.completionTokens?.toLong()
            ?: (reply.length / 4L).coerceAtLeast(1L)
        repository.recordTokenUsage(promptTokens, completionTokens)

        // Save assistant reply to Room DB
        repository.addChatMessage(
            sessionId = sId,
            role = "assistant",
            content = reply
        )

        val duration = System.currentTimeMillis() - requestStartTime
        LocalHostManager.logRequest(
            ServerLogEntry(
                clientIp = clientIp,
                method = "POST",
                path = "/api/chat",
                statusCode = 200,
                durationMs = duration,
                detail = "Prompt: ${userPrompt.take(30)}"
            )
        )

        val resJson = JSONObject().apply {
            put("role", "assistant")
            put("content", reply)
            put("sessionId", sId)
            put("model", config.modelName)
        }
        sendJsonResponse(output, 200, resJson.toString())
    }

    private suspend fun handleOpenAiChatCompletions(output: OutputStream, body: String, clientAuth: String?) {
        val reqJson = JSONObject(body.ifBlank { "{}" })
        val requestedModel = reqJson.optString("model", "")
        val messagesJson = reqJson.optJSONArray("messages") ?: JSONArray()
        val maxTokens = if (reqJson.has("max_tokens")) reqJson.optInt("max_tokens") else null

        val activeConfig = repository.activeApiConfig.firstOrNull()
        val allConfigs = repository.apiConfigs.first()

        // Match config by model name or fallback to active
        val targetConfig = allConfigs.firstOrNull { it.modelName.equals(requestedModel, ignoreCase = true) }
            ?: activeConfig

        if (targetConfig == null) {
            sendJsonResponse(output, 400, """{"error":{"message":"No API model configured on phone","type":"invalid_request_error"}}""")
            return
        }

        val messagesList = mutableListOf<ChatMessageDto>()
        for (i in 0 until messagesJson.length()) {
            val m = messagesJson.getJSONObject(i)
            messagesList.add(
                ChatMessageDto(
                    role = m.optString("role", "user"),
                    content = m.optString("content", "")
                )
            )
        }

        val request = ChatRequestDto(
            model = if (requestedModel.isNotBlank()) requestedModel else targetConfig.modelName,
            messages = messagesList,
            maxTokens = maxTokens ?: 2048
        )

        val finalUrl = normalizeEndpoint(targetConfig.endpoint)
        val auth = if (targetConfig.apiKey.isNotBlank()) "Bearer ${targetConfig.apiKey.trim()}" else clientAuth
        val isOpenRouter = finalUrl.contains("openrouter.ai", ignoreCase = true)

        val response = NetworkModule.apiService.createChatCompletion(
            url = finalUrl,
            authorization = auth,
            request = request,
            httpReferer = if (isOpenRouter) "https://openrouter.ai" else null,
            xTitle = if (isOpenRouter) "OpenLLM Local Gateway" else null
        )

        val reply = response.choices?.firstOrNull()?.message?.content ?: ""

        val resJson = JSONObject().apply {
            put("id", "chatcmpl-${System.currentTimeMillis()}")
            put("object", "chat.completion")
            put("created", System.currentTimeMillis() / 1000)
            put("model", targetConfig.modelName)
            val choices = JSONArray().apply {
                put(JSONObject().apply {
                    put("index", 0)
                    put("message", JSONObject().apply {
                        put("role", "assistant")
                        put("content", reply)
                    })
                    put("finish_reason", "stop")
                })
            }
            put("choices", choices)
            put("usage", JSONObject().apply {
                put("prompt_tokens", 0)
                put("completion_tokens", 0)
                put("total_tokens", 0)
            })
        }

        sendJsonResponse(output, 200, resJson.toString())
    }

    private fun normalizeEndpoint(raw: String): String {
        val trimmed = raw.trim()
        val withProtocol = if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
            "https://$trimmed"
        } else {
            trimmed
        }
        val clean = withProtocol.trimEnd('/')
        return if (clean.endsWith("/chat/completions")) {
            clean
        } else {
            "$clean/chat/completions"
        }
    }

    private fun parseQueryParams(queryString: String?): Map<String, String> {
        if (queryString.isNullOrBlank()) return emptyMap()
        val result = mutableMapOf<String, String>()
        val pairs = queryString.split("&")
        for (pair in pairs) {
            val idx = pair.indexOf("=")
            if (idx > 0) {
                val key = URLDecoder.decode(pair.substring(0, idx), "UTF-8")
                val value = URLDecoder.decode(pair.substring(idx + 1), "UTF-8")
                result[key] = value
            }
        }
        return result
    }

    private fun sendCorsPreflight(output: OutputStream) {
        val header = "HTTP/1.1 204 No Content\r\n" +
                "Access-Control-Allow-Origin: *\r\n" +
                "Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS\r\n" +
                "Access-Control-Allow-Headers: *\r\n" +
                "Access-Control-Max-Age: 86400\r\n" +
                "Connection: close\r\n\r\n"
        output.write(header.toByteArray(Charsets.UTF_8))
        output.flush()
    }

    private fun sendJsonResponse(output: OutputStream, status: Int, json: String) {
        sendResponse(output, status, if (status == 200) "OK" else "Error", "application/json; charset=utf-8", json)
    }

    private fun sendResponse(output: OutputStream, status: Int, statusText: String, contentType: String, body: String) {
        val bodyBytes = body.toByteArray(Charsets.UTF_8)
        val header = "HTTP/1.1 $status $statusText\r\n" +
                "Content-Type: $contentType\r\n" +
                "Content-Length: ${bodyBytes.size}\r\n" +
                "Access-Control-Allow-Origin: *\r\n" +
                "Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS\r\n" +
                "Access-Control-Allow-Headers: *\r\n" +
                "Connection: close\r\n\r\n"
        output.write(header.toByteArray(Charsets.UTF_8))
        output.write(bodyBytes)
        output.flush()
    }
}
