package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.luminance
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.data.SearchEngine
import com.example.data.SearchResponse
import com.example.data.SearchResultItem
import com.example.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch

@Composable
fun SearchServicesTabContent(
    viewModel: MainViewModel,
    isWebSearchEnabled: Boolean,
    activeSearchEngineId: String,
    searchEngines: List<SearchEngine>,
    isDarkTheme: Boolean = MaterialTheme.colorScheme.surface.luminance() < 0.5f,
    onAddNewService: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var testSearchEngine by remember { mutableStateOf<SearchEngine?>(null) }
    var editingEngine by remember { mutableStateOf<SearchEngine?>(null) }
    var engineToDelete by remember { mutableStateOf<SearchEngine?>(null) }
    var showResetDialog by remember { mutableStateOf(false) }

    val activeEngine = searchEngines.firstOrNull { it.id == activeSearchEngineId }
        ?: searchEngines.firstOrNull { it.id == "google" }
        ?: searchEngines.firstOrNull()
    val isDark = isDarkTheme

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Master Switch Card: Web Search for AI Models (Themed to app's Orange & Dark style)
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) {
                        if (isWebSearchEnabled) Color(0xFF261D12) else Color(0xFF1E1E1E)
                    } else {
                        if (isWebSearchEnabled) Color(0xFFFFF7ED) else Color.White
                    }
                ),
                border = BorderStroke(
                    if (isWebSearchEnabled) 1.5.dp else 1.25.dp,
                    if (isWebSearchEnabled) MaterialTheme.colorScheme.primary
                    else if (isDark) Color(0xFF383838) else Color(0xFFCBD5E1)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isWebSearchEnabled) MaterialTheme.colorScheme.primary
                                    else if (isDark) Color(0xFF2C2C2C) else Color(0xFFE0E0E0)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.TravelExplore,
                                contentDescription = "Web Search",
                                tint = if (isWebSearchEnabled) Color(0xFF121212)
                                else if (isDark) Color(0xFF999999) else Color(0xFF666666),
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Web Search for AI Models",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isWebSearchEnabled) {
                                    "Active: AI model searches websites in real-time to ground answers."
                                } else {
                                    "Disabled: AI model will rely solely on its static training data."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Switch(
                            checked = isWebSearchEnabled,
                            onCheckedChange = { viewModel.setWebSearchEnabled(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = if (isDark) Color(0xFF121212) else Color.White,
                                checkedTrackColor = MaterialTheme.colorScheme.primary,
                                uncheckedTrackColor = if (isDark) Color(0xFF2C2C2C) else Color(0xFFE0E0E0),
                                uncheckedThumbColor = if (isDark) Color(0xFF888888) else Color(0xFFFFFFFF)
                            )
                        )
                    }

                    // Compact active engine preview strip directly inside the card
                    if (isWebSearchEnabled && activeEngine != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(
                            color = if (isDark) Color(0xFF382614) else Color(0xFFFFE0B2),
                            thickness = 1.dp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                SearchEngineBadge(engine = activeEngine, size = 30.dp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = activeEngine.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        ActivePillBadge()
                                    }
                                    Text(
                                        text = activeEngine.searchUrlTemplate,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            FilledTonalButton(
                                onClick = { testSearchEngine = activeEngine },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = if (isDark) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f) else Color(0xFFFFEDD5),
                                    contentColor = if (isDark) MaterialTheme.colorScheme.primary else Color(0xFFC2410C)
                                )
                            ) {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = "Test Search",
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Test Search", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }

        // Section Title & Quick Actions
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp, bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Search Engines",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Tap to select your default web search service",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = { showResetDialog = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Reset default engines",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = onAddNewService,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Add Search Service",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }

        // Search Engines List
        items(searchEngines, key = { it.id }) { engine ->
            val isActive = engine.id == activeSearchEngineId

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        viewModel.setActiveSearchEngineId(engine.id)
                        Toast.makeText(context, "${engine.name} set as default search engine", Toast.LENGTH_SHORT).show()
                    },
                border = BorderStroke(
                    if (isActive) 1.5.dp else 1.25.dp,
                    if (isActive) MaterialTheme.colorScheme.primary
                    else if (isDark) Color(0xFF2C2C2C) else Color(0xFFCBD5E1)
                ),
                colors = CardDefaults.cardColors(
                    containerColor = if (isActive) {
                        if (isDark) Color(0xFF261D12) else Color(0xFFFFF7ED)
                    } else {
                        if (isDark) Color(0xFF1E1E1E) else Color.White
                    }
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = isActive,
                        onClick = {
                            viewModel.setActiveSearchEngineId(engine.id)
                            Toast.makeText(context, "${engine.name} set as default search engine", Toast.LENGTH_SHORT).show()
                        },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = MaterialTheme.colorScheme.primary,
                            unselectedColor = if (isDark) Color(0xFF666666) else Color(0xFF999999)
                        ),
                        modifier = Modifier.size(28.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    SearchEngineBadge(engine = engine, size = 36.dp)

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = engine.name,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (isActive) {
                                Spacer(modifier = Modifier.width(6.dp))
                                ActivePillBadge()
                            }
                            if (engine.isCustom) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = if (isDark) Color(0xFF2C2C2C) else Color(0xFFEEEEEE),
                                    shape = RoundedCornerShape(percent = 50),
                                    border = BorderStroke(0.5.dp, if (isDark) Color(0xFF444444) else Color(0xFFDDDDDD))
                                ) {
                                    Text(
                                        text = "Custom",
                                        fontSize = 9.5.sp,
                                        lineHeight = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.5.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = engine.searchUrlTemplate,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Action Buttons
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { testSearchEngine = engine },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = "Test Search",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        if (engine.isCustom) {
                            IconButton(
                                onClick = { editingEngine = engine },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "Edit Engine",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            IconButton(
                                onClick = { engineToDelete = engine },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete Engine",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(30.dp))
        }
    }

    // Dialogs
    if (testSearchEngine != null) {
        LiveTestSearchDialog(
            engine = testSearchEngine!!,
            onDismiss = { testSearchEngine = null },
            viewModel = viewModel
        )
    }

    if (editingEngine != null) {
        AddEditSearchEngineDialog(
            initialEngine = editingEngine,
            onDismiss = { editingEngine = null },
            onSave = { updated ->
                viewModel.updateSearchEngine(updated)
                editingEngine = null
                Toast.makeText(context, "${updated.name} updated", Toast.LENGTH_SHORT).show()
            },
            viewModel = viewModel
        )
    }

    if (engineToDelete != null) {
        AlertDialog(
            onDismissRequest = { engineToDelete = null },
            title = { Text("Delete Search Service?") },
            text = { Text("Are you sure you want to delete '${engineToDelete?.name}'? This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        engineToDelete?.let { viewModel.deleteCustomSearchEngine(it.id) }
                        engineToDelete = null
                        Toast.makeText(context, "Search service deleted", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { engineToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset Search Services?") },
            text = { Text("This will restore default built-in search engines (Google, Bing, Yahoo, DuckDuckGo, Brave, SearXNG) and reset active service to Google.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetSearchEnginesToDefault()
                        showResetDialog = false
                        Toast.makeText(context, "Search services reset to default", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Reset")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

fun getLocalSearchEngineLogoRes(engine: SearchEngine): Int? {
    val id = engine.id.lowercase()
    val name = engine.name.lowercase()
    val url = engine.searchUrlTemplate.lowercase()
    val domain = engine.getCleanDomain().lowercase()
    return when {
        id == "google" || domain.contains("google.") || url.contains("google.com") || name == "google" -> R.drawable.real_google
        id == "duckduckgo" || domain.contains("duckduckgo.") || url.contains("duckduckgo.com") || name == "duckduckgo" -> R.drawable.real_duckduckgo
        id == "bing" || domain.contains("bing.") || url.contains("bing.com") || name == "bing" -> R.drawable.real_bing
        id == "yahoo" || domain.contains("yahoo.") || url.contains("yahoo.com") || name == "yahoo" -> R.drawable.real_yahoo
        id == "brave" || domain.contains("brave.") || url.contains("brave.com") || name == "brave" || name.contains("brave search") -> R.drawable.real_brave
        id == "searxng" || id == "searx" || domain.contains("searx") || url.contains("searx") -> R.drawable.real_searxng
        else -> null
    }
}

@Composable
fun SearchEngineBadge(
    engine: SearchEngine,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 38.dp
) {
    val context = LocalContext.current
    val localRes = remember(engine.id, engine.searchUrlTemplate, engine.name) {
        getLocalSearchEngineLogoRes(engine)
    }
    val candidates = remember(localRes, engine.iconUrl, engine.searchUrlTemplate, engine.name) {
        if (localRes != null) emptyList() else engine.getIconUrlCandidates()
    }
    var candidateIndex by remember(candidates) { mutableStateOf(0) }
    val currentUrl = candidates.getOrNull(candidateIndex) ?: ""
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    Surface(
        shape = RoundedCornerShape(if (size <= 24.dp) 6.dp else 10.dp),
        color = if (isDark) Color(0xFF22242A) else Color(0xFFF2F4F7),
        border = BorderStroke(1.dp, if (isDark) Color(0xFF38383E) else Color(0xFFCBD5E1)),
        modifier = modifier.size(size)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (size <= 24.dp) 2.dp else 5.dp),
            contentAlignment = Alignment.Center
        ) {
            if (localRes != null) {
                Image(
                    painter = painterResource(id = localRes),
                    contentDescription = "${engine.name} logo",
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
                        contentDescription = "${engine.name} icon",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit,
                        onError = { loadFailed = true }
                    )
                } else {
                    FallbackLetterBadge(engine = engine)
                }
            } else {
                FallbackLetterBadge(engine = engine)
            }
        }
    }
}

@Composable
private fun FallbackLetterBadge(engine: SearchEngine) {
    val monogram = engine.name.trim().take(2).uppercase().ifBlank { "S" }
    val (bgColor, textColor) = when (engine.id) {
        "google" -> Pair(Color(0xFF4285F4), Color.White)
        "bing" -> Pair(Color(0xFF008373), Color.White)
        "duckduckgo" -> Pair(Color(0xFFDE5833), Color.White)
        "yahoo" -> Pair(Color(0xFF6001D2), Color.White)
        "brave" -> Pair(Color(0xFFFF5722), Color.White)
        "searxng" -> Pair(Color(0xFF00796B), Color.White)
        else -> Pair(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(4.dp))
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = monogram,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            color = textColor
        )
    }
}

@Composable
fun AddEditSearchEngineDialog(
    initialEngine: SearchEngine? = null,
    onDismiss: () -> Unit,
    onSave: (SearchEngine) -> Unit,
    viewModel: MainViewModel
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var name by remember { mutableStateOf(initialEngine?.name ?: "") }
    var searchUrlTemplate by remember { mutableStateOf(initialEngine?.searchUrlTemplate ?: "") }
    var description by remember { mutableStateOf(initialEngine?.description ?: "") }
    var apiKey by remember { mutableStateOf(initialEngine?.apiKey ?: "") }
    var iconUrl by remember { mutableStateOf(initialEngine?.iconUrl ?: "") }
    var showApiKey by remember { mutableStateOf(false) }

    var isTesting by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<SearchResponse?>(null) }
    var submitted by remember { mutableStateOf(false) }

    val isNameError = submitted && name.isBlank()
    val isUrlError = submitted && searchUrlTemplate.isBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialEngine == null) "Add Custom Search Service" else "Edit Search Service",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Add any search engine or browser URL. The real icon will automatically be detected and downloaded from the domain.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Real-time Engine & Real Icon Live Preview
                val previewEngine = remember(name, searchUrlTemplate, iconUrl) {
                    SearchEngine(
                        id = initialEngine?.id ?: "preview",
                        name = name.ifBlank { "Search Engine" },
                        searchUrlTemplate = searchUrlTemplate,
                        iconUrl = iconUrl,
                        isCustom = true
                    )
                }
                val previewDomain = previewEngine.getDomain()

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SearchEngineBadge(engine = previewEngine, size = 44.dp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = name.ifBlank { "Search Service Name" },
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (previewDomain.isNotBlank()) {
                                    "Domain: $previewDomain • Real icon auto-detected"
                                } else {
                                    "Enter search URL to auto-detect real icon"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (previewDomain.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Quick Templates
                Text(
                    text = "Quick Presets / Placeholders:",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AssistChip(
                        onClick = {
                            if (!searchUrlTemplate.contains("{query}")) {
                                searchUrlTemplate += "{query}"
                            }
                        },
                        label = { Text("+ {query}") }
                    )
                    AssistChip(
                        onClick = {
                            name = "Google Web"
                            searchUrlTemplate = "https://www.google.com/search?q={query}"
                            description = "Google Web Search query format"
                            iconUrl = ""
                        },
                        label = { Text("Google") }
                    )
                    AssistChip(
                        onClick = {
                            name = "Microsoft Bing"
                            searchUrlTemplate = "https://www.bing.com/search?q={query}"
                            description = "Bing Web Search query format"
                            iconUrl = ""
                        },
                        label = { Text("Bing") }
                    )
                    AssistChip(
                        onClick = {
                            name = "DuckDuckGo HTML"
                            searchUrlTemplate = "https://html.duckduckgo.com/html/?q={query}"
                            description = "DuckDuckGo direct HTML query"
                            iconUrl = ""
                        },
                        label = { Text("DuckDuckGo") }
                    )
                    AssistChip(
                        onClick = {
                            name = "Brave Search"
                            searchUrlTemplate = "https://search.brave.com/search?q={query}"
                            description = "Independent privacy search engine"
                            iconUrl = ""
                        },
                        label = { Text("Brave") }
                    )
                    AssistChip(
                        onClick = {
                            name = "Ecosia"
                            searchUrlTemplate = "https://www.ecosia.org/search?q={query}"
                            description = "Tree planting search engine"
                            iconUrl = ""
                        },
                        label = { Text("Ecosia") }
                    )
                    AssistChip(
                        onClick = {
                            name = "Yahoo Search"
                            searchUrlTemplate = "https://search.yahoo.com/search?p={query}"
                            description = "Yahoo search query format"
                            iconUrl = ""
                        },
                        label = { Text("Yahoo") }
                    )
                    AssistChip(
                        onClick = {
                            name = "Startpage"
                            searchUrlTemplate = "https://www.startpage.com/sp/search?query={query}"
                            description = "Private search results"
                            iconUrl = ""
                        },
                        label = { Text("Startpage") }
                    )
                    AssistChip(
                        onClick = {
                            name = "Baidu"
                            searchUrlTemplate = "https://www.baidu.com/s?wd={query}"
                            description = "Baidu search engine"
                            iconUrl = ""
                        },
                        label = { Text("Baidu") }
                    )
                    AssistChip(
                        onClick = {
                            name = "SearXNG Instance"
                            searchUrlTemplate = "https://searx.be/search?q={query}&format=json"
                            description = "JSON Metasearch API format"
                            iconUrl = ""
                        },
                        label = { Text("SearXNG JSON") }
                    )
                }

                // Name field
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Service Name *") },
                    placeholder = { Text("e.g. Ecosia, Startpage, Kagi") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = isNameError,
                    supportingText = if (isNameError) {
                        { Text("Service name is required", color = MaterialTheme.colorScheme.error) }
                    } else null
                )

                // Search URL Template
                OutlinedTextField(
                    value = searchUrlTemplate,
                    onValueChange = {
                        searchUrlTemplate = it
                        testResult = null
                    },
                    label = { Text("Search URL Template *") },
                    placeholder = { Text("https://example.com/search?q={query}") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = isUrlError,
                    supportingText = {
                        if (isUrlError) {
                            Text("Search URL template is required", color = MaterialTheme.colorScheme.error)
                        } else {
                            Text("Must include {query} or parameter for search terms", fontSize = 11.sp)
                        }
                    },
                    trailingIcon = {
                        if (searchUrlTemplate.isNotEmpty()) {
                            IconButton(onClick = { searchUrlTemplate = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    }
                )

                // Icon URL (Optional) field
                OutlinedTextField(
                    value = iconUrl,
                    onValueChange = { iconUrl = it },
                    label = { Text("Icon URL (Optional)") },
                    placeholder = { Text("Leave blank to auto-fetch real icon from domain") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    supportingText = {
                        Text("Real icon is automatically retrieved from the search domain if left empty.", fontSize = 11.sp)
                    },
                    trailingIcon = {
                        if (iconUrl.isNotEmpty()) {
                            IconButton(onClick = { iconUrl = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear icon URL")
                            }
                        }
                    }
                )

                // Description field
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (Optional)") },
                    placeholder = { Text("e.g. Fast search index for tech documentation") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // API Key field (Optional)
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("API Key (Optional)") },
                    placeholder = { Text("Enter API key if required by search service") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showApiKey = !showApiKey }) {
                            Icon(
                                if (showApiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Toggle API Key visibility"
                            )
                        }
                    }
                )

                // Inline Test Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            if (searchUrlTemplate.isBlank()) {
                                Toast.makeText(context, "Please enter a Search URL first", Toast.LENGTH_SHORT).show()
                                return@OutlinedButton
                            }
                            isTesting = true
                            testResult = null
                            val tempEngine = SearchEngine(
                                id = initialEngine?.id ?: "temp",
                                name = name.ifBlank { "Test Engine" },
                                searchUrlTemplate = searchUrlTemplate.trim(),
                                apiKey = apiKey.trim(),
                                iconUrl = iconUrl.trim(),
                                isCustom = true
                            )
                            coroutineScope.launch {
                                val res = viewModel.testSearch(tempEngine, "Android Jetpack Compose")
                                isTesting = false
                                testResult = res
                            }
                        },
                        enabled = !isTesting && searchUrlTemplate.isNotBlank()
                    ) {
                        if (isTesting) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Testing...")
                        } else {
                            Icon(Icons.Default.Search, contentDescription = "Test", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Test Search")
                        }
                    }

                    if (testResult != null) {
                        Text(
                            text = if (testResult!!.isSuccess) "✓ Found ${testResult!!.results.size} items" else "⚠ Failed",
                            color = if (testResult!!.isSuccess) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (testResult != null && testResult!!.results.isNotEmpty()) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            val first = testResult!!.results.first()
                            Text(
                                text = "Sample Result:",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = first.title,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = first.snippet,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    submitted = true
                    if (name.isBlank() || searchUrlTemplate.isBlank()) return@Button

                    val engine = (initialEngine ?: SearchEngine(
                        id = "custom_${System.currentTimeMillis()}",
                        name = "",
                        searchUrlTemplate = ""
                    )).copy(
                        name = name.trim(),
                        searchUrlTemplate = searchUrlTemplate.trim(),
                        description = description.trim(),
                        apiKey = apiKey.trim(),
                        iconUrl = iconUrl.trim(),
                        isCustom = true,
                        isEnabled = true
                    )
                    onSave(engine)
                }
            ) {
                Text("Save Service")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun LiveTestSearchDialog(
    engine: SearchEngine,
    onDismiss: () -> Unit,
    viewModel: MainViewModel
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("Android development latest news") }
    var isSearching by remember { mutableStateOf(false) }
    var searchResponse by remember { mutableStateOf<SearchResponse?>(null) }

    val performSearch = {
        if (searchQuery.isNotBlank()) {
            isSearching = true
            searchResponse = null
            coroutineScope.launch {
                val res = viewModel.testSearch(engine, searchQuery)
                isSearching = false
                searchResponse = res
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SearchEngineBadge(engine = engine)
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Live Search Tester",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = engine.name,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(420.dp)
            ) {
                // Search Input
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search Query") },
                    placeholder = { Text("Type query to search websites...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { performSearch() }),
                    trailingIcon = {
                        IconButton(
                            onClick = { performSearch() },
                            enabled = !isSearching && searchQuery.isNotBlank()
                        ) {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        }
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Querying: ${engine.name}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        onClick = { performSearch() },
                        enabled = !isSearching && searchQuery.isNotBlank(),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        if (isSearching) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Searching...")
                        } else {
                            Text("Search Now")
                        }
                    }
                }

                if (isSearching) {
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(10.dp))

                // Results Container
                if (searchResponse == null && !isSearching) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Public,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outlineVariant,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Enter a query and tap 'Search Now'",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else if (searchResponse != null) {
                    val resp = searchResponse!!
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Found ${resp.results.size} Results",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            if (resp.searchUrl.isNotBlank()) {
                                TextButton(
                                    onClick = {
                                        try {
                                            val intent = Intent(Intent.ACTION_VIEW, resp.searchUrl.toUri())
                                            context.startActivity(intent)
                                        } catch (_: Exception) {
                                            Toast.makeText(context, "Could not open browser", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Open Web Page", fontSize = 11.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(resp.results) { item ->
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = item.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = item.url,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontFamily = FontFamily.Monospace,
                                            color = MaterialTheme.colorScheme.primary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (item.snippet.isNotBlank()) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = item.snippet,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End
                                        ) {
                                            FilledTonalButton(
                                                onClick = {
                                                    try {
                                                        val intent = Intent(Intent.ACTION_VIEW, item.url.toUri())
                                                        context.startActivity(intent)
                                                    } catch (_: Exception) {
                                                        Toast.makeText(context, "Could not open browser", Toast.LENGTH_SHORT).show()
                                                    }
                                                },
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(12.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Open Link", fontSize = 11.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
