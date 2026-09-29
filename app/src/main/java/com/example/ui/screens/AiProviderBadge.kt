package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.luminance
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.data.AiProviderInfo
import com.example.data.ProviderRegistry

fun getLocalAiProviderLogoRes(
    id: String,
    endpoint: String = "",
    name: String = ""
): Int? {
    val lowerId = id.lowercase().trim()
    val lowerUrl = endpoint.lowercase().trim()
    val lowerName = name.lowercase().trim()
    return when {
        lowerId == "openrouter" || lowerUrl.contains("openrouter.ai") || lowerName.contains("openrouter") -> R.drawable.real_openrouter
        lowerId == "openai" || (lowerUrl.contains("api.openai.com") && !lowerUrl.contains("googleapis")) || (lowerName.contains("openai") && !lowerName.contains("gemini")) -> R.drawable.real_openai
        lowerId == "gemini" || lowerUrl.contains("generativelanguage.googleapis.com") || lowerName.contains("gemini") || lowerName.contains("google") -> R.drawable.real_gemini
        lowerId == "groq" || lowerUrl.contains("groq.com") || lowerName.contains("groq") -> R.drawable.real_groq
        lowerId == "deepseek" || lowerUrl.contains("deepseek.com") || lowerName.contains("deepseek") -> R.drawable.real_deepseek
        lowerId == "mistral" || lowerUrl.contains("mistral.ai") || lowerName.contains("mistral") -> R.drawable.real_mistral
        lowerId == "anthropic" || lowerId == "claude" || lowerUrl.contains("anthropic.com") || lowerName.contains("anthropic") || lowerName.contains("claude") -> R.drawable.real_anthropic
        lowerId == "perplexity" || lowerUrl.contains("perplexity.ai") || lowerName.contains("perplexity") -> R.drawable.real_perplexity
        lowerId == "ollama" || lowerUrl.contains("11434") || lowerName.contains("ollama") -> R.drawable.real_ollama
        lowerId == "lmstudio" || lowerUrl.contains("1234") || lowerName.contains("lm studio") -> R.drawable.real_lmstudio
        else -> null
    }
}

fun extractEndpointDomain(url: String): String {
    val trimmed = url.trim().lowercase().removePrefix("https://").removePrefix("http://")
    val host = trimmed.substringBefore("/").substringBefore(":")
    return when {
        host.startsWith("www.") -> host.removePrefix("www.")
        host.startsWith("api.") -> host.removePrefix("api.")
        host.startsWith("platform.") -> host.removePrefix("platform.")
        host.startsWith("console.") -> host.removePrefix("console.")
        host.startsWith("generativelanguage.") -> "gemini.google.com"
        else -> host
    }
}

@Composable
fun AiProviderBadge(
    modifier: Modifier = Modifier,
    provider: AiProviderInfo? = null,
    providerId: String = provider?.id ?: "",
    name: String = provider?.name ?: "",
    endpoint: String = provider?.defaultEndpoint ?: "",
    iconUrl: String? = provider?.iconUrl,
    size: Dp = 44.dp,
    shape: Shape = RoundedCornerShape(12.dp),
    isActive: Boolean = false
) {
    val context = LocalContext.current
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    // Determine resolved provider if needed
    val resolvedProvider = remember(provider, providerId, endpoint, name) {
        provider ?: if (endpoint.isNotBlank() || name.isNotBlank()) {
            ProviderRegistry.matchProvider(endpoint, name)
        } else {
            null
        }
    }

    val effId = resolvedProvider?.id ?: providerId
    val effName = resolvedProvider?.name ?: name
    val effEndpoint = resolvedProvider?.defaultEndpoint?.ifBlank { endpoint } ?: endpoint

    val localRes = remember(effId, effEndpoint, effName) {
        getLocalAiProviderLogoRes(effId, effEndpoint, effName)
    }

    val candidates = remember(localRes, resolvedProvider, iconUrl, effEndpoint, effName) {
        if (localRes != null) {
            emptyList()
        } else {
            val list = mutableListOf<String>()
            if (resolvedProvider != null) {
                list.addAll(resolvedProvider.getIconUrlCandidates())
            }
            if (!iconUrl.isNullOrBlank() && !list.contains(iconUrl.trim())) {
                list.add(0, iconUrl.trim())
            }
            val cleanDomain = extractEndpointDomain(effEndpoint)
            if (cleanDomain.isNotBlank() && cleanDomain != "10.0.2.2" && cleanDomain != "localhost" && cleanDomain != "127.0.0.1") {
                val autoUrl = "https://t3.gstatic.com/faviconV2?client=SOCIAL&type=FAVICON&fallback_opts=TYPE,SIZE,URL&url=https://$cleanDomain&size=128"
                if (!list.contains(autoUrl)) list.add(autoUrl)
                val s2Url = "https://www.google.com/s2/favicons?domain=$cleanDomain&sz=128"
                if (!list.contains(s2Url)) list.add(s2Url)
                val ddgUrl = "https://icons.duckduckgo.com/ip3/$cleanDomain.ico"
                if (!list.contains(ddgUrl)) list.add(ddgUrl)
            }
            list.distinct()
        }
    }

    var candidateIndex by remember(candidates) { mutableIntStateOf(0) }
    val currentUrl = candidates.getOrNull(candidateIndex) ?: ""

    val containerBg = when {
        isActive -> if (isDark) Color(0xFF2E2114) else Color(0xFFFFF7ED)
        isDark -> Color(0xFF222226)
        else -> Color(0xFFF3F4F6)
    }

    val borderColor = when {
        isActive -> MaterialTheme.colorScheme.primary
        isDark -> Color(0xFF38383E)
        else -> Color(0xFFCBD5E1)
    }

    Surface(
        shape = shape,
        color = containerBg,
        border = BorderStroke(if (isActive) 1.5.dp else 1.dp, borderColor),
        modifier = modifier.size(size)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (size <= 28.dp) 3.dp else 6.dp),
            contentAlignment = Alignment.Center
        ) {
            if (localRes != null) {
                Image(
                    painter = painterResource(id = localRes),
                    contentDescription = "$effName logo",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            } else if (currentUrl.isNotBlank()) {
                var loadFailed by remember(currentUrl) { mutableStateOf(false) }
                if (loadFailed && candidateIndex + 1 < candidates.size) {
                    LaunchedEffect(currentUrl) {
                        candidateIndex++
                        loadFailed = false
                    }
                }

                if (!loadFailed || candidateIndex + 1 < candidates.size) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(currentUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "$effName logo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit,
                        onError = { loadFailed = true }
                    )
                } else {
                    AiProviderFallbackIcon(
                        id = effId,
                        name = effName,
                        isActive = isActive
                    )
                }
            } else {
                AiProviderFallbackIcon(
                    id = effId,
                    name = effName,
                    isActive = isActive
                )
            }
        }
    }
}

@Composable
fun AiProviderFallbackIcon(
    id: String,
    name: String,
    isActive: Boolean
) {
    val vector = when (id.lowercase()) {
        "openrouter" -> Icons.Default.Hub
        "openai" -> Icons.Default.AutoAwesome
        "gemini" -> Icons.Default.FlashOn
        "groq" -> Icons.Default.Speed
        "deepseek" -> Icons.Default.Psychology
        "ollama", "lmstudio" -> Icons.Default.Computer
        else -> Icons.Default.Dns
    }

    if (name.isNotBlank() && vector == Icons.Default.Dns) {
        val letter = name.first().uppercase()
        Text(
            text = letter,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    } else {
        Icon(
            imageVector = vector,
            contentDescription = name,
            tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxSize(0.65f)
        )
    }
}

@Composable
fun ActivePillBadge(
    modifier: Modifier = Modifier,
    text: String = "Active"
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
        shape = RoundedCornerShape(percent = 50),
        border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.65f))
    ) {
        Text(
            text = text,
            color = MaterialTheme.colorScheme.primary,
            fontSize = 9.5.sp,
            lineHeight = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.2.sp,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.5.dp)
        )
    }
}
