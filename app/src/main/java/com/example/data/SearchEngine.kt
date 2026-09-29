package com.example.data

import org.json.JSONArray
import org.json.JSONObject

data class SearchEngine(
    val id: String,
    val name: String,
    val searchUrlTemplate: String,
    val description: String = "",
    val apiKey: String = "",
    val iconUrl: String = "",
    val isCustom: Boolean = false,
    val isEnabled: Boolean = true
) {
    fun getSearchUrl(query: String): String {
        val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
        return when {
            searchUrlTemplate.contains("{query}") -> searchUrlTemplate.replace("{query}", encodedQuery)
            searchUrlTemplate.contains("{search}") -> searchUrlTemplate.replace("{search}", encodedQuery)
            searchUrlTemplate.contains("%s") -> searchUrlTemplate.replace("%s", encodedQuery)
            searchUrlTemplate.endsWith("=") -> searchUrlTemplate + encodedQuery
            searchUrlTemplate.contains("?") -> "$searchUrlTemplate&q=$encodedQuery"
            else -> "$searchUrlTemplate?q=$encodedQuery"
        }
    }

    fun getDomain(): String {
        return try {
            val candidate = when {
                searchUrlTemplate.startsWith("http://", ignoreCase = true) || searchUrlTemplate.startsWith("https://", ignoreCase = true) -> searchUrlTemplate
                searchUrlTemplate.isNotBlank() -> "https://$searchUrlTemplate"
                else -> ""
            }
            if (candidate.isBlank()) return ""
            val uri = java.net.URI(candidate)
            val host = uri.host ?: ""
            if (host.isNotBlank()) {
                host
            } else {
                val regex = Regex("(?:https?://)?([^/:]+)")
                regex.find(candidate)?.groupValues?.get(1) ?: ""
            }
        } catch (_: Exception) {
            val regex = Regex("(?:https?://)?([^/:]+)")
            regex.find(searchUrlTemplate)?.groupValues?.get(1) ?: ""
        }
    }

    fun getCleanDomain(): String {
        val raw = getDomain().lowercase().trim()
        val withoutProtocol = raw.removePrefix("https://").removePrefix("http://")
        val host = withoutProtocol.substringBefore("/").substringBefore(":")
        return when {
            host.startsWith("www.") -> host.removePrefix("www.")
            host.startsWith("html.") -> host.removePrefix("html.")
            host.startsWith("search.") -> host.removePrefix("search.")
            else -> host
        }
    }

    fun getIconUrlCandidates(): List<String> {
        val list = mutableListOf<String>()
        if (iconUrl.isNotBlank()) {
            list.add(iconUrl.trim())
        }
        val cleanDomain = getCleanDomain()
        val rawDomain = getDomain().lowercase().trim()
        if (cleanDomain.isNotBlank()) {
            list.add("https://t3.gstatic.com/faviconV2?client=SOCIAL&type=FAVICON&fallback_opts=TYPE,SIZE,URL&url=https://$cleanDomain&size=128")
            list.add("https://www.google.com/s2/favicons?domain=$cleanDomain&sz=128")
            list.add("https://icons.duckduckgo.com/ip3/$cleanDomain.ico")
            list.add("https://$cleanDomain/favicon.ico")
        }
        if (rawDomain.isNotBlank() && rawDomain != cleanDomain) {
            list.add("https://t3.gstatic.com/faviconV2?client=SOCIAL&type=FAVICON&fallback_opts=TYPE,SIZE,URL&url=https://$rawDomain&size=128")
        }
        return list.distinct()
    }

    fun getEffectiveIconUrl(): String {
        if (iconUrl.isNotBlank()) return iconUrl.trim()
        val cleanDomain = getCleanDomain()
        return if (cleanDomain.isNotBlank()) {
            "https://t3.gstatic.com/faviconV2?client=SOCIAL&type=FAVICON&fallback_opts=TYPE,SIZE,URL&url=https://$cleanDomain&size=128"
        } else {
            ""
        }
    }

    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("name", name)
            put("searchUrlTemplate", searchUrlTemplate)
            put("description", description)
            put("apiKey", apiKey)
            put("iconUrl", iconUrl)
            put("isCustom", isCustom)
            put("isEnabled", isEnabled)
        }
    }

    companion object {
        fun fromJson(json: JSONObject): SearchEngine {
            return SearchEngine(
                id = json.optString("id", "custom_${System.currentTimeMillis()}"),
                name = json.optString("name", "Custom Search"),
                searchUrlTemplate = json.optString("searchUrlTemplate", "https://www.google.com/search?q={query}"),
                description = json.optString("description", ""),
                apiKey = json.optString("apiKey", ""),
                iconUrl = json.optString("iconUrl", ""),
                isCustom = json.optBoolean("isCustom", false),
                isEnabled = json.optBoolean("isEnabled", true)
            )
        }

        val BUILT_IN_ENGINES = listOf(
            SearchEngine(
                id = "google",
                name = "Google",
                searchUrlTemplate = "https://www.google.com/search?q={query}",
                description = "Google Web Search engine with real-time web results",
                iconUrl = "https://t3.gstatic.com/faviconV2?client=SOCIAL&type=FAVICON&fallback_opts=TYPE,SIZE,URL&url=https://google.com&size=128",
                isCustom = false
            ),
            SearchEngine(
                id = "bing",
                name = "Bing",
                searchUrlTemplate = "https://www.bing.com/search?q={query}",
                description = "Microsoft Bing Web Search service",
                iconUrl = "https://t3.gstatic.com/faviconV2?client=SOCIAL&type=FAVICON&fallback_opts=TYPE,SIZE,URL&url=https://bing.com&size=128",
                isCustom = false
            ),
            SearchEngine(
                id = "duckduckgo",
                name = "DuckDuckGo",
                searchUrlTemplate = "https://html.duckduckgo.com/html/?q={query}",
                description = "Privacy-centric live web search with instant indexing",
                iconUrl = "https://t3.gstatic.com/faviconV2?client=SOCIAL&type=FAVICON&fallback_opts=TYPE,SIZE,URL&url=https://duckduckgo.com&size=128",
                isCustom = false
            ),
            SearchEngine(
                id = "yahoo",
                name = "Yahoo",
                searchUrlTemplate = "https://search.yahoo.com/search?p={query}",
                description = "Yahoo global web search and directory index",
                iconUrl = "https://t3.gstatic.com/faviconV2?client=SOCIAL&type=FAVICON&fallback_opts=TYPE,SIZE,URL&url=https://yahoo.com&size=128",
                isCustom = false
            ),
            SearchEngine(
                id = "brave",
                name = "Brave Search",
                searchUrlTemplate = "https://search.brave.com/search?q={query}",
                description = "Independent privacy search engine with zero user tracking",
                iconUrl = "https://t3.gstatic.com/faviconV2?client=SOCIAL&type=FAVICON&fallback_opts=TYPE,SIZE,URL&url=https://brave.com&size=128",
                isCustom = false
            ),
            SearchEngine(
                id = "searxng",
                name = "SearXNG",
                searchUrlTemplate = "https://searx.be/search?q={query}&format=json",
                description = "Open-source privacy metasearch aggregating multiple engines",
                iconUrl = "https://t3.gstatic.com/faviconV2?client=SOCIAL&type=FAVICON&fallback_opts=TYPE,SIZE,URL&url=https://searx.be&size=128",
                isCustom = false
            )
        )
    }
}

data class SearchResultItem(
    val title: String,
    val snippet: String,
    val url: String
)

data class SearchResponse(
    val engineName: String,
    val query: String,
    val results: List<SearchResultItem>,
    val searchUrl: String,
    val isSuccess: Boolean = true,
    val errorMessage: String? = null
)
