package com.example.data

data class AiProviderInfo(
    val id: String,
    val name: String,
    val tagline: String,
    val defaultEndpoint: String,
    val defaultModel: String,
    val popularModels: List<String>,
    val requiresApiKey: Boolean = true,
    val badge: String? = null,
    val docsUrl: String? = null,
    val iconUrl: String? = null
) {
    fun getCleanDomain(): String {
        val target = when {
            defaultEndpoint.isNotBlank() -> defaultEndpoint
            !docsUrl.isNullOrBlank() -> docsUrl
            else -> ""
        }.lowercase().trim()

        if (target.isBlank()) return ""
        val withoutProtocol = target.removePrefix("https://").removePrefix("http://")
        val host = withoutProtocol.substringBefore("/").substringBefore(":")
        return when {
            host.startsWith("www.") -> host.removePrefix("www.")
            host.startsWith("api.") -> host.removePrefix("api.")
            host.startsWith("platform.") -> host.removePrefix("platform.")
            host.startsWith("console.") -> host.removePrefix("console.")
            host.startsWith("generativelanguage.") -> "gemini.google.com"
            else -> host
        }
    }

    fun getIconUrlCandidates(): List<String> {
        val list = mutableListOf<String>()
        if (!iconUrl.isNullOrBlank()) {
            list.add(iconUrl.trim())
        }
        val cleanDomain = getCleanDomain()
        if (cleanDomain.isNotBlank() && cleanDomain != "10.0.2.2" && cleanDomain != "localhost" && cleanDomain != "127.0.0.1") {
            list.add("https://t3.gstatic.com/faviconV2?client=SOCIAL&type=FAVICON&fallback_opts=TYPE,SIZE,URL&url=https://$cleanDomain&size=128")
            list.add("https://www.google.com/s2/favicons?domain=$cleanDomain&sz=128")
            list.add("https://icons.duckduckgo.com/ip3/$cleanDomain.ico")
            list.add("https://$cleanDomain/favicon.ico")
        }
        return list.distinct()
    }
}

object ProviderRegistry {
    val PRESET_PROVIDERS = listOf(
        AiProviderInfo(
            id = "openrouter",
            name = "OpenRouter",
            tagline = "Access 100+ models with one API key",
            defaultEndpoint = "https://openrouter.ai/api/v1",
            defaultModel = "meta-llama/llama-3.3-70b-instruct",
            popularModels = listOf(
                "meta-llama/llama-3.3-70b-instruct",
                "google/gemini-2.0-flash-exp:free",
                "deepseek/deepseek-r1",
                "anthropic/claude-3.5-sonnet",
                "openai/gpt-4o-mini",
                "mistralai/mistral-large"
            ),
            requiresApiKey = true,
            badge = "Recommended",
            docsUrl = "openrouter.ai/keys",
            iconUrl = "https://t3.gstatic.com/faviconV2?client=SOCIAL&type=FAVICON&fallback_opts=TYPE,SIZE,URL&url=https://openrouter.ai&size=128"
        ),
        AiProviderInfo(
            id = "openai",
            name = "OpenAI",
            tagline = "Official GPT-4o, GPT-4o-mini & reasoning models",
            defaultEndpoint = "https://api.openai.com/v1",
            defaultModel = "gpt-4o-mini",
            popularModels = listOf(
                "gpt-4o-mini",
                "gpt-4o",
                "o1-mini",
                "gpt-4-turbo"
            ),
            requiresApiKey = true,
            badge = "Official",
            docsUrl = "platform.openai.com/api-keys",
            iconUrl = "https://t3.gstatic.com/faviconV2?client=SOCIAL&type=FAVICON&fallback_opts=TYPE,SIZE,URL&url=https://openai.com&size=128"
        ),
        AiProviderInfo(
            id = "gemini",
            name = "Google Gemini",
            tagline = "High-speed multimodal intelligence via OpenAI endpoint",
            defaultEndpoint = "https://generativelanguage.googleapis.com/v1beta/openai",
            defaultModel = "gemini-1.5-flash",
            popularModels = listOf(
                "gemini-1.5-flash",
                "gemini-1.5-pro",
                "gemini-2.0-flash-exp"
            ),
            requiresApiKey = true,
            badge = "Fast & Free Tier",
            docsUrl = "aistudio.google.com/apikey",
            iconUrl = "https://t3.gstatic.com/faviconV2?client=SOCIAL&type=FAVICON&fallback_opts=TYPE,SIZE,URL&url=https://gemini.google.com&size=128"
        ),
        AiProviderInfo(
            id = "groq",
            name = "Groq",
            tagline = "Ultra low-latency LPU inference",
            defaultEndpoint = "https://api.groq.com/openai/v1",
            defaultModel = "llama-3.3-70b-versatile",
            popularModels = listOf(
                "llama-3.3-70b-versatile",
                "llama-3.1-8b-instant",
                "mixtral-8x7b-32768",
                "gemma2-9b-it"
            ),
            requiresApiKey = true,
            badge = "Ultra Fast",
            docsUrl = "console.groq.com/keys",
            iconUrl = "https://t3.gstatic.com/faviconV2?client=SOCIAL&type=FAVICON&fallback_opts=TYPE,SIZE,URL&url=https://groq.com&size=128"
        ),
        AiProviderInfo(
            id = "deepseek",
            name = "DeepSeek",
            tagline = "State-of-the-art coding and mathematical reasoning",
            defaultEndpoint = "https://api.deepseek.com/v1",
            defaultModel = "deepseek-chat",
            popularModels = listOf(
                "deepseek-chat",
                "deepseek-reasoner"
            ),
            requiresApiKey = true,
            badge = "Affordable",
            docsUrl = "platform.deepseek.com/api_keys",
            iconUrl = "https://t3.gstatic.com/faviconV2?client=SOCIAL&type=FAVICON&fallback_opts=TYPE,SIZE,URL&url=https://deepseek.com&size=128"
        ),
        AiProviderInfo(
            id = "mistral",
            name = "Mistral AI",
            tagline = "Open and efficient European LLMs",
            defaultEndpoint = "https://api.mistral.ai/v1",
            defaultModel = "mistral-small-latest",
            popularModels = listOf(
                "mistral-small-latest",
                "mistral-large-latest",
                "codestral-latest"
            ),
            requiresApiKey = true,
            docsUrl = "console.mistral.ai/api-keys",
            iconUrl = "https://t3.gstatic.com/faviconV2?client=SOCIAL&type=FAVICON&fallback_opts=TYPE,SIZE,URL&url=https://mistral.ai&size=128"
        ),
        AiProviderInfo(
            id = "anthropic",
            name = "Anthropic (Claude)",
            tagline = "Claude 3.5 Sonnet, Haiku & Opus models",
            defaultEndpoint = "https://api.anthropic.com/v1",
            defaultModel = "claude-3-5-sonnet-latest",
            popularModels = listOf(
                "claude-3-5-sonnet-latest",
                "claude-3-5-haiku-latest",
                "claude-3-opus-latest"
            ),
            requiresApiKey = true,
            badge = "Frontier",
            docsUrl = "console.anthropic.com/settings/keys",
            iconUrl = "https://t3.gstatic.com/faviconV2?client=SOCIAL&type=FAVICON&fallback_opts=TYPE,SIZE,URL&url=https://anthropic.com&size=128"
        ),
        AiProviderInfo(
            id = "perplexity",
            name = "Perplexity AI",
            tagline = "Real-time web search augmented reasoning models",
            defaultEndpoint = "https://api.perplexity.ai",
            defaultModel = "sonar-pro",
            popularModels = listOf(
                "sonar-pro",
                "sonar",
                "sonar-reasoning"
            ),
            requiresApiKey = true,
            badge = "Web Search",
            docsUrl = "perplexity.ai/settings/api",
            iconUrl = "https://t3.gstatic.com/faviconV2?client=SOCIAL&type=FAVICON&fallback_opts=TYPE,SIZE,URL&url=https://perplexity.ai&size=128"
        ),
        AiProviderInfo(
            id = "ollama",
            name = "Ollama (Local)",
            tagline = "Run private open-source models offline on PC",
            defaultEndpoint = "http://10.0.2.2:11434/v1",
            defaultModel = "llama3.2",
            popularModels = listOf(
                "llama3.2",
                "llama3.1",
                "mistral",
                "qwen2.5",
                "deepseek-r1"
            ),
            requiresApiKey = false,
            badge = "Offline / Local",
            iconUrl = "https://t3.gstatic.com/faviconV2?client=SOCIAL&type=FAVICON&fallback_opts=TYPE,SIZE,URL&url=https://ollama.com&size=128"
        ),
        AiProviderInfo(
            id = "lmstudio",
            name = "LM Studio",
            tagline = "Local desktop LLM server running on PC",
            defaultEndpoint = "http://10.0.2.2:1234/v1",
            defaultModel = "local-model",
            popularModels = listOf(
                "local-model"
            ),
            requiresApiKey = false,
            badge = "Local",
            iconUrl = "https://t3.gstatic.com/faviconV2?client=SOCIAL&type=FAVICON&fallback_opts=TYPE,SIZE,URL&url=https://lmstudio.ai&size=128"
        ),
        AiProviderInfo(
            id = "custom",
            name = "Custom Endpoint",
            tagline = "Any OpenAI-compatible server or proxy",
            defaultEndpoint = "",
            defaultModel = "",
            popularModels = emptyList(),
            requiresApiKey = false
        )
    )

    fun findById(id: String): AiProviderInfo? {
        return PRESET_PROVIDERS.find { it.id.equals(id, ignoreCase = true) }
    }

    fun matchProvider(endpoint: String, name: String): AiProviderInfo {
        val lowerUrl = endpoint.lowercase()
        val lowerName = name.lowercase()
        return when {
            lowerUrl.contains("openrouter.ai") || lowerName.contains("openrouter") -> PRESET_PROVIDERS.first { it.id == "openrouter" }
            lowerUrl.contains("api.openai.com") || (lowerName.contains("openai") && !lowerUrl.contains("googleapis")) -> PRESET_PROVIDERS.first { it.id == "openai" }
            lowerUrl.contains("generativelanguage.googleapis.com") || lowerName.contains("gemini") -> PRESET_PROVIDERS.first { it.id == "gemini" }
            lowerUrl.contains("api.groq.com") || lowerName.contains("groq") -> PRESET_PROVIDERS.first { it.id == "groq" }
            lowerUrl.contains("deepseek.com") || lowerName.contains("deepseek") -> PRESET_PROVIDERS.first { it.id == "deepseek" }
            lowerUrl.contains("mistral.ai") || lowerName.contains("mistral") -> PRESET_PROVIDERS.first { it.id == "mistral" }
            lowerUrl.contains("anthropic.com") || lowerName.contains("anthropic") || lowerName.contains("claude") -> PRESET_PROVIDERS.first { it.id == "anthropic" }
            lowerUrl.contains("perplexity.ai") || lowerName.contains("perplexity") -> PRESET_PROVIDERS.first { it.id == "perplexity" }
            lowerUrl.contains("11434") || lowerName.contains("ollama") -> PRESET_PROVIDERS.first { it.id == "ollama" }
            lowerUrl.contains("1234") || lowerName.contains("lm studio") -> PRESET_PROVIDERS.first { it.id == "lmstudio" }
            else -> PRESET_PROVIDERS.last()
        }
    }
}
