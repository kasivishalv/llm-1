package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.AiPersona
import com.example.data.AiProviderInfo
import com.example.data.ApiConfig
import com.example.data.AppRepository
import com.example.data.ProviderRegistry
import com.example.data.RestoreSummary
import com.example.data.TokenUsageStats
import com.example.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import android.content.Intent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    initialTab: Int = 0
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()

    val apiConfigs by viewModel.apiConfigs.collectAsStateWithLifecycle()
    val activeConfig by viewModel.activeApiConfig.collectAsStateWithLifecycle()
    val detailedResponses by viewModel.detailedResponses.collectAsStateWithLifecycle()
    val systemPrompt by viewModel.systemPrompt.collectAsStateWithLifecycle()
    val isDarkTheme by viewModel.isDarkTheme.collectAsStateWithLifecycle()

    val isWebSearchEnabled by viewModel.isWebSearchEnabled.collectAsStateWithLifecycle()
    val activeSearchEngineId by viewModel.activeSearchEngineId.collectAsStateWithLifecycle()
    val searchEngines by viewModel.searchEngines.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(initialTab) }

    // Dialog states for Providers & Endpoints
    var providerForDialog by remember { mutableStateOf<AiProviderInfo?>(null) }
    var configForProviderDialog by remember { mutableStateOf<ApiConfig?>(null) }

    var showCustomEndpointDialog by remember { mutableStateOf(false) }
    var editingCustomConfig by remember { mutableStateOf<ApiConfig?>(null) }
    var configToDelete by remember { mutableStateOf<ApiConfig?>(null) }

    // Dialog states for Search Services
    var showAddSearchEngineDialog by remember { mutableStateOf(false) }

    // Dialog states for preferences
    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var showSystemPromptDialog by remember { mutableStateOf(false) }

    // Active model test connection state
    var isTestingActiveModel by remember { mutableStateOf(false) }
    var activeModelTestResult by remember { mutableStateOf<Pair<Boolean, String>?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Settings & Providers",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            focusManager.clearFocus()
                            onNavigateBack()
                        }
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            if (selectedTab == 1) {
                ExtendedFloatingActionButton(
                    onClick = {
                        editingCustomConfig = null
                        showCustomEndpointDialog = true
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    icon = { Icon(Icons.Default.Add, contentDescription = "Add Endpoint") },
                    text = { Text("Add Endpoint", fontWeight = FontWeight.SemiBold) }
                )
            } else if (selectedTab == 2) {
                ExtendedFloatingActionButton(
                    onClick = {
                        showAddSearchEngineDialog = true
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    icon = { Icon(Icons.Default.Add, contentDescription = "Add Search Service") },
                    text = { Text("Add Search Service", fontWeight = FontWeight.SemiBold) }
                )
            }
        },
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Top Navigation Tabs
            PrimaryScrollableTabRow(
                selectedTabIndex = selectedTab,
                edgePadding = 8.dp,
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.primary,
                divider = {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    selectedContentColor = MaterialTheme.colorScheme.primary,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    text = { Text("Providers", maxLines = 1, fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal) },
                    icon = { Icon(Icons.Default.Hub, contentDescription = null, modifier = Modifier.size(20.dp)) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    selectedContentColor = MaterialTheme.colorScheme.primary,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    text = { Text("Endpoints", maxLines = 1, fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal) },
                    icon = { Icon(Icons.Default.Dns, contentDescription = null, modifier = Modifier.size(20.dp)) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    selectedContentColor = MaterialTheme.colorScheme.primary,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    text = { Text("Search", maxLines = 1, fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal) },
                    icon = { Icon(Icons.Default.TravelExplore, contentDescription = null, modifier = Modifier.size(20.dp)) }
                )
                Tab(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    selectedContentColor = MaterialTheme.colorScheme.primary,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    text = { Text("General", maxLines = 1, fontWeight = if (selectedTab == 3) FontWeight.Bold else FontWeight.Normal) },
                    icon = { Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(20.dp)) }
                )
                Tab(
                    selected = selectedTab == 4,
                    onClick = { selectedTab = 4 },
                    selectedContentColor = MaterialTheme.colorScheme.primary,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    text = { Text("Local Host", maxLines = 1, fontWeight = if (selectedTab == 4) FontWeight.Bold else FontWeight.Normal) },
                    icon = { Icon(Icons.Default.Dns, contentDescription = null, modifier = Modifier.size(20.dp)) }
                )
            }

            // Tab Content
            when (selectedTab) {
                0 -> {
                    // TAB 0: AI PROVIDERS & API MODEL INTERFACE
                    ProvidersTabContent(
                        apiConfigs = apiConfigs,
                        activeConfig = activeConfig,
                        isDarkTheme = isDarkTheme,
                        isTestingActive = isTestingActiveModel,
                        activeTestResult = activeModelTestResult,
                        onTestActive = {
                            activeConfig?.let { cfg ->
                                isTestingActiveModel = true
                                activeModelTestResult = null
                                coroutineScope.launch {
                                    val res = viewModel.testApiConnection(cfg.endpoint, cfg.modelName, cfg.apiKey)
                                    isTestingActiveModel = false
                                    activeModelTestResult = res
                                }
                            }
                        },
                        onOpenProviderDialog = { provider, config ->
                            providerForDialog = provider
                            configForProviderDialog = config
                        },
                        onSetActiveConfig = { id ->
                            viewModel.setActiveConfig(id)
                            Toast.makeText(context, "Active provider updated", Toast.LENGTH_SHORT).show()
                        }
                    )
                }

                1 -> {
                    // TAB 1: SAVED CUSTOM ENDPOINTS
                    SavedEndpointsTabContent(
                        apiConfigs = apiConfigs,
                        isDarkTheme = isDarkTheme,
                        onSelectConfig = { viewModel.setActiveConfig(it) },
                        onTestConnection = { endpoint, model, apiKey ->
                            viewModel.testApiConnection(endpoint, model, apiKey)
                        },
                        onEditConfig = { config ->
                            editingCustomConfig = config
                            showCustomEndpointDialog = true
                        },
                        onDeleteConfig = { config ->
                            configToDelete = config
                        },
                        onAddNew = {
                            editingCustomConfig = null
                            showCustomEndpointDialog = true
                        }
                    )
                }

                2 -> {
                    // TAB 2: SEARCH SERVICES FOR API MODEL SUPPORT (GOOGLE, BING, YAHOO, DUCKDUCKGO, UNLIMITED MANUAL ADD)
                    SearchServicesTabContent(
                        viewModel = viewModel,
                        isWebSearchEnabled = isWebSearchEnabled,
                        activeSearchEngineId = activeSearchEngineId,
                        searchEngines = searchEngines,
                        isDarkTheme = isDarkTheme,
                        onAddNewService = { showAddSearchEngineDialog = true }
                    )
                }

                3 -> {
                    // TAB 3: PREFERENCES (THEME, PERSONAS, MEMORY, TTS, TOKENS, BACKUP, SYSTEM PROMPT)
                    PreferencesTabContent(
                        viewModel = viewModel,
                        isDarkTheme = isDarkTheme,
                        detailedResponses = detailedResponses,
                        isWebSearchEnabled = isWebSearchEnabled,
                        onThemeChange = { viewModel.setDarkTheme(it) },
                        onDetailedResponsesChange = { viewModel.setDetailedResponses(it) },
                        onWebSearchChange = { viewModel.setWebSearchEnabled(it) },
                        onNavigateToSearchServices = { selectedTab = 2 },
                        onNavigateToLocalHost = { selectedTab = 4 },
                        onOpenSystemPrompt = { showSystemPromptDialog = true },
                        onOpenClearChat = { showClearConfirmDialog = true }
                    )
                }
                4 -> {
                    LocalHostTabContent(
                        repository = viewModel.repository,
                        activeConfig = activeConfig,
                        onSwitchToProvidersTab = { selectedTab = 0 }
                    )
                }
            }
        }
    }

    // Provider API Model Interface Dialog
    if (providerForDialog != null) {
        val provider = providerForDialog!!
        ProviderModelDialog(
            provider = provider,
            existingConfig = configForProviderDialog,
            isDarkTheme = isDarkTheme,
            onDismiss = {
                providerForDialog = null
                configForProviderDialog = null
            },
            onTestConnection = { endpoint, model, apiKey ->
                viewModel.testApiConnection(endpoint, model, apiKey)
            },
            onSaveConfig = { name, endpoint, model, apiKey, activate ->
                val existingId = configForProviderDialog?.id
                if (activate) {
                    viewModel.saveAndActivateApiConfig(existingId, name, endpoint, model, apiKey)
                    Toast.makeText(context, "Saved and activated $name ($model)", Toast.LENGTH_SHORT).show()
                } else {
                    if (existingId != null) {
                        viewModel.updateApiConfig(existingId, name, endpoint, model, apiKey)
                    } else {
                        viewModel.addApiConfig(name, endpoint, model, apiKey)
                    }
                    Toast.makeText(context, "Saved $name ($model)", Toast.LENGTH_SHORT).show()
                }
                providerForDialog = null
                configForProviderDialog = null
            }
        )
    }

    // Add / Edit Custom Endpoint Dialog with Verify API Model & API Model Interface
    if (showCustomEndpointDialog) {
        ApiConfigDialog(
            initialConfig = editingCustomConfig,
            isDarkTheme = isDarkTheme,
            onDismiss = {
                showCustomEndpointDialog = false
                editingCustomConfig = null
            },
            onTestConnection = { endpoint, model, apiKey ->
                viewModel.testApiConnection(endpoint, model, apiKey)
            },
            onSave = { name, endpoint, modelName, apiKey, activate ->
                val existingId = editingCustomConfig?.id
                if (activate) {
                    viewModel.saveAndActivateApiConfig(existingId, name, endpoint, modelName, apiKey)
                    Toast.makeText(context, "Saved and activated $name ($modelName)", Toast.LENGTH_SHORT).show()
                } else {
                    if (existingId != null) {
                        viewModel.updateApiConfig(existingId, name, endpoint, modelName, apiKey)
                    } else {
                        viewModel.addApiConfig(name, endpoint, modelName, apiKey)
                    }
                    Toast.makeText(context, "Saved $name ($modelName)", Toast.LENGTH_SHORT).show()
                }
                showCustomEndpointDialog = false
                editingCustomConfig = null
            }
        )
    }

    // Delete Endpoint Confirmation Dialog
    if (configToDelete != null) {
        AlertDialog(
            onDismissRequest = { configToDelete = null },
            title = { Text("Delete Endpoint") },
            text = { Text("Are you sure you want to delete \"${configToDelete?.name}\"?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        configToDelete?.let { viewModel.deleteApiConfig(it.id) }
                        configToDelete = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { configToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Clear Chat History Confirmation Dialog
    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            icon = {
                Icon(
                    Icons.Default.WarningAmber,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Permanently Delete History?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to permanently delete all chat history and conversations? This action cannot be undone."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearChat()
                        showClearConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text("Yes, Clear All", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showClearConfirmDialog = false }
                ) {
                    Text("Cancel", fontWeight = FontWeight.Medium)
                }
            }
        )
    }

    // AI Instructions / System Prompt Dialog
    if (showSystemPromptDialog) {
        var promptDraft by remember(systemPrompt) { mutableStateOf(systemPrompt) }
        AlertDialog(
            onDismissRequest = { showSystemPromptDialog = false },
            title = {
                Text("AI System Instructions", fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text(
                        "These instructions tell the AI model how to generate answers for every chat:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = promptDraft,
                        onValueChange = { promptDraft = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = {
                            promptDraft = AppRepository.DEFAULT_SYSTEM_PROMPT
                        }
                    ) {
                        Text("Reset to Default Detailed Prompt")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.setSystemPrompt(promptDraft)
                        showSystemPromptDialog = false
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSystemPromptDialog = false }) {
                    Text("Cancel")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Add / Edit Custom Search Service Dialog
    if (showAddSearchEngineDialog) {
        AddEditSearchEngineDialog(
            initialEngine = null,
            onDismiss = { showAddSearchEngineDialog = false },
            onSave = { newEngine ->
                viewModel.addCustomSearchEngine(
                    name = newEngine.name,
                    searchUrlTemplate = newEngine.searchUrlTemplate,
                    description = newEngine.description,
                    apiKey = newEngine.apiKey
                )
                showAddSearchEngineDialog = false
                Toast.makeText(context, "Search service '${newEngine.name}' added", Toast.LENGTH_SHORT).show()
            },
            viewModel = viewModel
        )
    }
}

// -------------------------------------------------------------------------------------------------
// TAB 0: PROVIDERS TAB CONTENT
// -------------------------------------------------------------------------------------------------
@Composable
private fun ProvidersTabContent(
    apiConfigs: List<ApiConfig>,
    activeConfig: ApiConfig?,
    isDarkTheme: Boolean,
    isTestingActive: Boolean,
    activeTestResult: Pair<Boolean, String>?,
    onTestActive: () -> Unit,
    onOpenProviderDialog: (AiProviderInfo, ApiConfig?) -> Unit,
    onSetActiveConfig: (Int) -> Unit
) {
    var filterCategory by remember { mutableStateOf("all") }

    val filteredProviders = remember(filterCategory) {
        when (filterCategory) {
            "cloud" -> ProviderRegistry.PRESET_PROVIDERS.filter { it.requiresApiKey }
            "local" -> ProviderRegistry.PRESET_PROVIDERS.filter { !it.requiresApiKey }
            else -> ProviderRegistry.PRESET_PROVIDERS
        }
    }

    val existingConfigMap = remember(apiConfigs) {
        apiConfigs.associateBy { cfg -> ProviderRegistry.matchProvider(cfg.endpoint, cfg.name).id }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Active Provider Hero Card
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (activeConfig != null) {
                        if (isDarkTheme) Color(0xFF261D12) else Color(0xFFFFF7ED)
                    } else {
                        if (isDarkTheme) Color(0xFF1E1E1E) else Color.White
                    }
                ),
                border = BorderStroke(
                    1.5.dp,
                    if (activeConfig != null) MaterialTheme.colorScheme.primary
                    else if (isDarkTheme) Color(0xFF383838) else Color(0xFFCBD5E1)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (activeConfig != null) Icons.Default.CheckCircle else Icons.Default.Info,
                                contentDescription = null,
                                tint = if (activeConfig != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Active Model for Chat",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        if (activeConfig != null) {
                            ActivePillBadge()
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (activeConfig != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AiProviderBadge(
                                endpoint = activeConfig.endpoint,
                                name = activeConfig.name,
                                size = 44.dp,
                                isActive = true
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${activeConfig.name} • ${activeConfig.modelName}",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = activeConfig.endpoint,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilledTonalButton(
                                onClick = onTestActive,
                                enabled = !isTestingActive,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .defaultMinSize(minHeight = 42.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    if (isTestingActive) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(14.dp),
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Testing...",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.NetworkCheck,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Test Connection",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            
                                        )
                                    }
                                }
                            }

                            OutlinedButton(
                                onClick = {
                                    val matched = ProviderRegistry.matchProvider(activeConfig.endpoint, activeConfig.name)
                                    onOpenProviderDialog(matched, activeConfig)
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .defaultMinSize(minHeight = 42.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Tune,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Change Model",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        
                                    )
                                }
                            }
                        }

                        AnimatedVisibility(visible = activeTestResult != null) {
                            activeTestResult?.let { (success, message) ->
                                Column {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (success) {
                                        if (isDarkTheme) Color(0xFF064E3B).copy(alpha = 0.45f) else Color(0xFFECFDF5)
                                    } else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f),
                                    border = BorderStroke(
                                        1.dp,
                                        if (success) {
                                            if (isDarkTheme) Color(0xFF059669) else Color(0xFF10B981)
                                        } else MaterialTheme.colorScheme.error
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = if (success) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                                            contentDescription = null,
                                            tint = if (success) {
                                                if (isDarkTheme) Color(0xFF34D399) else Color(0xFF059669)
                                            } else MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = message,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium,
                                            textAlign = TextAlign.Center,
                                            color = if (success) {
                                                if (isDarkTheme) Color(0xFF6EE7B7) else Color(0xFF065F46)
                                            } else MaterialTheme.colorScheme.onErrorContainer,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                    }
                                }
                                }
                            }
                        }
                    } else {
                        Text(
                            text = "No AI provider is currently active. Tap any provider below to set your API credentials or model identifier.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Filter Chips Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = filterCategory == "all",
                    onClick = { filterCategory = "all" },
                    label = { Text("All Providers (${ProviderRegistry.PRESET_PROVIDERS.size})") }
                )
                FilterChip(
                    selected = filterCategory == "cloud",
                    onClick = { filterCategory = "cloud" },
                    label = { Text("Cloud APIs") }
                )
                FilterChip(
                    selected = filterCategory == "local",
                    onClick = { filterCategory = "local" },
                    label = { Text("Local & Offline") }
                )
            }
        }

        // Section Title
        item {
            Text(
                text = "Available AI Providers",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // Providers List
        items(filteredProviders, key = { it.id }) { provider ->
            val existingConfig = existingConfigMap[provider.id]
            val isConfigActive = existingConfig?.isActive == true

            ProviderCard(
                provider = provider,
                existingConfig = existingConfig,
                isActive = isConfigActive,
                isDarkTheme = isDarkTheme,
                onOpenDialog = { onOpenProviderDialog(provider, existingConfig) },
                onSetActive = {
                    existingConfig?.let { onSetActiveConfig(it.id) }
                }
            )
        }
    }
}

@Composable
private fun ProviderCard(
    provider: AiProviderInfo,
    existingConfig: ApiConfig?,
    isActive: Boolean,
    isDarkTheme: Boolean,
    onOpenDialog: () -> Unit,
    onSetActive: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenDialog() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) {
                if (isDarkTheme) Color(0xFF241B10) else Color(0xFFFFF8EE)
            } else {
                if (isDarkTheme) Color(0xFF1E1E1E) else Color.White
            }
        ),
        border = if (isActive) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else {
            BorderStroke(1.25.dp, if (isDarkTheme) Color(0xFF383838) else Color(0xFFCBD5E1))
        }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Provider Icon with Real Authentic Image
                AiProviderBadge(
                    provider = provider,
                    size = 46.dp,
                    isActive = isActive
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = provider.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (provider.badge != null) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = provider.badge,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = provider.tagline,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (isActive) {
                    ActivePillBadge()
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Configuration Info Box
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = if (isDarkTheme) Color(0xFF282828) else Color(0xFFF1F5F9),
                border = BorderStroke(1.dp, if (isDarkTheme) Color(0xFF383838) else Color(0xFFE2E8F0))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (existingConfig != null) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Model: ${existingConfig.modelName}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (existingConfig.apiKey.isNotBlank()) "API Key: Saved • Configured" else "Local server (no key required)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Not configured yet",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Tap Setup to configure API key & model",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (existingConfig != null && !isActive) {
                    TextButton(onClick = onSetActive) {
                        Text("Set Active", fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                }

                Button(
                    onClick = onOpenDialog,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = if (existingConfig != null) Icons.Default.Tune else Icons.Default.AddCircleOutline,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (existingConfig != null) "Configure Model" else "Setup & Select Model",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// TAB 1: SAVED CUSTOM ENDPOINTS CONTENT
// -------------------------------------------------------------------------------------------------
@Composable
private fun SavedEndpointsTabContent(
    apiConfigs: List<ApiConfig>,
    isDarkTheme: Boolean,
    onSelectConfig: (Int) -> Unit,
    onTestConnection: suspend (String, String, String) -> Pair<Boolean, String>,
    onEditConfig: (ApiConfig) -> Unit,
    onDeleteConfig: (ApiConfig) -> Unit,
    onAddNew: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var testingConfigId by remember { mutableStateOf<Int?>(null) }
    val testResults = remember { mutableStateMapOf<Int, Pair<Boolean, String>>() }

    if (apiConfigs.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.Dns,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                    modifier = Modifier.size(52.dp)
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "No custom endpoints saved",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "You can add custom endpoints for local servers (Ollama, LM Studio, vLLM) or remote providers.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onAddNew) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add Custom Endpoint")
                }
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDarkTheme) Color(0xFF1E1E1E) else Color.White
                    ),
                    border = BorderStroke(1.25.dp, if (isDarkTheme) Color(0xFF383838) else Color(0xFFCBD5E1)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Hub,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Tap any card to activate it. You can verify and test model connectivity directly.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            items(apiConfigs, key = { it.id }) { config ->
                val isActive = config.isActive
                val isTestingThis = testingConfigId == config.id
                val testRes = testResults[config.id]

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectConfig(config.id) },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isActive) {
                            if (isDarkTheme) Color(0xFF261D12) else Color(0xFFFFF7ED)
                        } else {
                            if (isDarkTheme) Color(0xFF1E1E1E) else Color.White
                        }
                    ),
                    border = if (isActive) {
                        BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                    } else {
                        BorderStroke(1.25.dp, if (isDarkTheme) Color(0xFF383838) else Color(0xFFCBD5E1))
                    }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                AiProviderBadge(
                                    endpoint = config.endpoint,
                                    name = config.name,
                                    size = 42.dp,
                                    isActive = isActive
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = config.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (isActive) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            ActivePillBadge()
                                        }
                                    }
                                    Text(
                                        text = config.modelName,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Row {
                                IconButton(
                                    onClick = { onEditConfig(config) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = "Edit Endpoint & Model",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { onDeleteConfig(config) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Delete Endpoint",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = if (isActive) {
                                if (isDarkTheme) Color(0xFF332717) else Color(0xFFFED7AA).copy(alpha = 0.35f)
                            } else {
                                if (isDarkTheme) Color(0xFF2A2A2A) else Color(0xFFF1F5F9)
                            },
                            border = BorderStroke(
                                1.dp,
                                if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                                else if (isDarkTheme) Color(0xFF383838) else Color(0xFFE2E8F0)
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                Text(
                                    text = "Model: ${config.modelName}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Link,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = config.endpoint,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Bottom Actions: Test Connection & Set Active
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = {
                                    coroutineScope.launch {
                                        testingConfigId = config.id
                                        val res = onTestConnection(config.endpoint, config.modelName, config.apiKey)
                                        testResults[config.id] = res
                                        testingConfigId = null
                                    }
                                },
                                enabled = !isTestingThis,
                                shape = RoundedCornerShape(10.dp),
                                modifier = if (isActive) {
                                    Modifier
                                        .fillMaxWidth()
                                        .defaultMinSize(minHeight = 40.dp)
                                } else {
                                    Modifier
                                        .weight(1f)
                                        .defaultMinSize(minHeight = 40.dp)
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    if (isTestingThis) {
                                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Testing...", fontSize = 13.sp)
                                    } else {
                                        Icon(Icons.Default.NetworkCheck, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Test Connection", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }

                            if (!isActive) {
                                Button(
                                    onClick = { onSelectConfig(config.id) },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .defaultMinSize(minHeight = 40.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Set Active", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        // Inline test result if tested
                        AnimatedVisibility(visible = testRes != null) {
                            testRes?.let { (success, message) ->
                                Column {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (success) {
                                        if (isDarkTheme) Color(0xFF064E3B).copy(alpha = 0.45f) else Color(0xFFECFDF5)
                                    } else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f),
                                    border = BorderStroke(
                                        1.dp,
                                        if (success) {
                                            if (isDarkTheme) Color(0xFF059669) else Color(0xFF10B981)
                                        } else MaterialTheme.colorScheme.error
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = if (success) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                                            contentDescription = null,
                                            tint = if (success) {
                                                if (isDarkTheme) Color(0xFF34D399) else Color(0xFF059669)
                                            } else MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = message,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium,
                                            textAlign = TextAlign.Center,
                                            color = if (success) {
                                                if (isDarkTheme) Color(0xFF6EE7B7) else Color(0xFF065F46)
                                            } else MaterialTheme.colorScheme.onErrorContainer,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                    }
                                }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// TAB 3: PREFERENCES TAB CONTENT (THEME, PERSONAS, MEMORY, TTS, TOKENS, BACKUP)
// -------------------------------------------------------------------------------------------------
@Composable
private fun PreferencesTabContent(
    viewModel: MainViewModel,
    isDarkTheme: Boolean,
    detailedResponses: Boolean,
    isWebSearchEnabled: Boolean,
    onThemeChange: (Boolean) -> Unit,
    onDetailedResponsesChange: (Boolean) -> Unit,
    onWebSearchChange: (Boolean) -> Unit,
    onNavigateToSearchServices: () -> Unit,
    onNavigateToLocalHost: () -> Unit,
    onOpenSystemPrompt: () -> Unit,
    onOpenClearChat: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()

    val activePersonaId by viewModel.activePersonaId.collectAsStateWithLifecycle()
    val customPersonaPrompt by viewModel.customPersonaPrompt.collectAsStateWithLifecycle()
    val contextWindowLimit by viewModel.contextWindowLimit.collectAsStateWithLifecycle()
    val isTtsEnabled by viewModel.isTtsEnabled.collectAsStateWithLifecycle()
    val isAutoSpeakEnabled by viewModel.isAutoSpeakEnabled.collectAsStateWithLifecycle()
    val ttsSpeechRate by viewModel.ttsSpeechRate.collectAsStateWithLifecycle()
    val tokenUsageStats by viewModel.tokenUsageStats.collectAsStateWithLifecycle()

    var showCustomPersonaDialog by remember { mutableStateOf(false) }
    var tempCustomPrompt by remember { mutableStateOf(customPersonaPrompt) }

    var showExportDialog by remember { mutableStateOf(false) }
    var exportJsonText by remember { mutableStateOf("") }
    var isExporting by remember { mutableStateOf(false) }

    var showRestoreDialog by remember { mutableStateOf(false) }
    var restoreJsonInput by remember { mutableStateOf("") }
    var restoreSummary by remember { mutableStateOf<RestoreSummary?>(null) }
    var isRestoring by remember { mutableStateOf(false) }

    var showResetTokensDialog by remember { mutableStateOf(false) }

    val activePersona = remember(activePersonaId) {
        AiPersona.findById(activePersonaId)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. App Theme Selector
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDarkTheme) Color(0xFF1E1E1E) else Color.White
            ),
            border = BorderStroke(1.25.dp, if (isDarkTheme) Color(0xFF383838) else Color(0xFFCBD5E1)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "App Theme",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (isDarkTheme) "Dark Mode active" else "Light Mode active",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row {
                    IconButton(
                        onClick = { onThemeChange(false) },
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        Icon(
                            Icons.Default.LightMode,
                            contentDescription = "Light Theme",
                            tint = if (!isDarkTheme) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    }
                    IconButton(
                        onClick = { onThemeChange(true) }
                    ) {
                        Icon(
                            Icons.Default.DarkMode,
                            contentDescription = "Dark Theme",
                            tint = if (isDarkTheme) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    }
                }
            }
        }

        // 2. AI Persona & Role Presets Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDarkTheme) Color(0xFF1E1E1E) else Color.White
            ),
            border = BorderStroke(1.25.dp, if (isDarkTheme) Color(0xFF383838) else Color(0xFFCBD5E1)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "AI Persona Presets",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Switch system behavior between balanced assistance, expert coding, concise summaries, or scholarly research.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Active Persona Banner
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Active: ${activePersona.name} (${activePersona.title})",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = activePersona.description,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (activePersonaId == "custom") {
                            TextButton(onClick = {
                                tempCustomPrompt = customPersonaPrompt
                                showCustomPersonaDialog = true
                            }) {
                                Text("Edit Prompt")
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Persona Selection Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AiPersona.ALL_PERSONAS.forEach { persona ->
                        val isSelected = persona.id == activePersonaId
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                if (persona.id == "custom" && customPersonaPrompt.isBlank()) {
                                    tempCustomPrompt = ""
                                    showCustomPersonaDialog = true
                                } else {
                                    viewModel.setActivePersona(persona.id)
                                    Toast.makeText(context, "Activated: ${persona.name}", Toast.LENGTH_SHORT).show()
                                }
                            },
                            label = { Text(persona.name, maxLines = 1, softWrap = false) },
                            leadingIcon = if (isSelected) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null
                        )
                    }
                }
            }
        }

        // 3. Context Window & Memory Truncation Limit Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDarkTheme) Color(0xFF1E1E1E) else Color.White
            ),
            border = BorderStroke(1.25.dp, if (isDarkTheme) Color(0xFF383838) else Color(0xFFCBD5E1)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.History,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Context Window Memory Limit",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Controls how many recent messages are sent to the model on each prompt. Truncating long threads saves tokens and prevents hitting context length limits.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val limits = listOf(
                        0 to "Unlimited (Full)",
                        10 to "10 Msgs",
                        20 to "20 Msgs (Default)",
                        40 to "40 Msgs",
                        60 to "60 Msgs"
                    )

                    limits.forEach { (limit, label) ->
                        val isSelected = contextWindowLimit == limit
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                viewModel.setContextWindowLimit(limit)
                                Toast.makeText(
                                    context,
                                    if (limit == 0) "Context window: Full history" else "Context window limit set to $limit messages",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            label = { Text(label, maxLines = 1, softWrap = false) },
                            leadingIcon = if (isSelected) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null
                        )
                    }
                }
            }
        }

        // 4. Voice Dictation & Text-to-Speech (TTS) Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDarkTheme) Color(0xFF1E1E1E) else Color.White
            ),
            border = BorderStroke(1.25.dp, if (isDarkTheme) Color(0xFF383838) else Color(0xFFCBD5E1)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.VolumeUp,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Voice & Text-to-Speech (TTS)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Listen to AI model responses aloud, dictate prompts via microphone, and adjust speech playback speed.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Enable TTS Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text(
                            text = "Enable Text-to-Speech Button",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Show listen button on assistant messages",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isTtsEnabled,
                        onCheckedChange = { viewModel.setTtsEnabled(it) }
                    )
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                )

                // Auto-Speak Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text(
                            text = "Auto-Speak New Replies",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Automatically read out assistant messages as soon as generation completes",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isAutoSpeakEnabled,
                        onCheckedChange = { viewModel.setAutoSpeakEnabled(it) },
                        enabled = isTtsEnabled
                    )
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                )

                // TTS Speech Speed Rate
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Speech Rate Speed",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = "${"%.2f".format(ttsSpeechRate)}x",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Fine-tuning Interactive Slider
                    Slider(
                        value = ttsSpeechRate.coerceIn(0.5f, 2.0f),
                        onValueChange = { newRate ->
                            val rounded = kotlin.math.round(newRate * 20f) / 20f
                            viewModel.setTtsSpeechRate(rounded)
                        },
                        valueRange = 0.5f..2.0f,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // 4 Equal-Width Presets with perfect alignment
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val presets = listOf(
                            0.8f to "0.8x",
                            1.0f to "1.0x",
                            1.25f to "1.25x",
                            1.5f to "1.5x"
                        )
                        val subLabels = listOf("Slow", "Normal", "Fast", "Turbo")

                        presets.forEachIndexed { index, (rate, label) ->
                            val isSelected = (ttsSpeechRate - rate).let { it > -0.04f && it < 0.04f }
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { viewModel.setTtsSpeechRate(rate) }
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp, horizontal = 2.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                    Text(
                                        text = subLabels[index],
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 10.sp,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. Token Usage & API Activity Statistics Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDarkTheme) Color(0xFF1E1E1E) else Color.White
            ),
            border = BorderStroke(1.25.dp, if (isDarkTheme) Color(0xFF383838) else Color(0xFFCBD5E1)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Assessment,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Token Usage & API Metrics",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    TextButton(onClick = { showResetTokensDialog = true }) {
                        Text("Reset", style = MaterialTheme.typography.labelSmall)
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Track cumulative tokens consumed and total API model requests made from this device.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Token Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Total Tokens",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "%,d".format(tokenUsageStats.totalTokens),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Total Requests",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "%,d".format(tokenUsageStats.totalRequests),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Prompt Input Tokens",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "%,d".format(tokenUsageStats.totalPromptTokens),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Completion Output Tokens",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "%,d".format(tokenUsageStats.totalCompletionTokens),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // 6. Web Search Integration Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDarkTheme) Color(0xFF1E1E1E) else Color.White
            ),
            border = BorderStroke(1.25.dp, if (isDarkTheme) Color(0xFF383838) else Color(0xFFCBD5E1)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.TravelExplore,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Web Search for Models",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isWebSearchEnabled) {
                                "Active: AI model queries search engines (Google, Bing, Yahoo, DDG, custom) in real-time."
                            } else {
                                "Disabled: AI model will answer only from static trained weights."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isWebSearchEnabled,
                        onCheckedChange = onWebSearchChange
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onNavigateToSearchServices,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Configure Search Services (Google, Bing, Yahoo, Custom...)", style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        // 7. Detailed Responses Style
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDarkTheme) Color(0xFF1E1E1E) else Color.White
            ),
            border = BorderStroke(1.25.dp, if (isDarkTheme) Color(0xFF383838) else Color(0xFFCBD5E1)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Detailed Responses",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (detailedResponses) {
                                "AI provides comprehensive explanations with rich markdown formatting."
                            } else {
                                "Standard response style without extended prompting."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = detailedResponses,
                        onCheckedChange = onDetailedResponsesChange
                    )
                }

                if (detailedResponses) {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = onOpenSystemPrompt,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Customize AI System Instructions", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

        // 8. Local Host Web Gateway Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDarkTheme) Color(0xFF1E1E1E) else Color.White
            ),
            border = BorderStroke(1.25.dp, if (isDarkTheme) Color(0xFF383838) else Color(0xFFCBD5E1)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF10B981).copy(alpha = 0.15f),
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Dns,
                                contentDescription = null,
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Local Host Server",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Chat from any web browser on your Wi-Fi network",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Button(
                    onClick = onNavigateToLocalHost,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Open")
                }
            }
        }

        // 9. Data Backup & Restore Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDarkTheme) Color(0xFF1E1E1E) else Color.White
            ),
            border = BorderStroke(1.25.dp, if (isDarkTheme) Color(0xFF383838) else Color(0xFFCBD5E1)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.FolderOpen,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Data Backup & Restore",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Export all chat history, configured endpoints, custom search engines, and preferences to a portable JSON backup file, or restore on another device.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            isExporting = true
                            viewModel.exportBackup { json ->
                                exportJsonText = json
                                isExporting = false
                                showExportDialog = true
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        if (isExporting) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export (JSON)")
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            restoreJsonInput = ""
                            restoreSummary = null
                            showRestoreDialog = true
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Restore Data")
                    }
                }
            }
        }

        // 10. Clear Chat History Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDarkTheme) Color(0xFF1E1E1E) else Color.White
            ),
            border = BorderStroke(1.25.dp, if (isDarkTheme) Color(0xFF383838) else Color(0xFFCBD5E1)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Clear Chat History",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Permanently wipe all conversation sessions and messages from local storage.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onOpenClearChat,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDarkTheme) Color(0xFFD32F2F) else MaterialTheme.colorScheme.error,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Clear All Chat Messages", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }

    // Dialog: Custom Persona Prompt Editor
    if (showCustomPersonaDialog) {
        AlertDialog(
            onDismissRequest = { showCustomPersonaDialog = false },
            title = { Text("Custom AI Persona Prompt") },
            text = {
                Column {
                    Text(
                        text = "Provide personalized instructions that will shape every answer and behavior of the AI model.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = tempCustomPrompt,
                        onValueChange = { tempCustomPrompt = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 140.dp, max = 260.dp),
                        placeholder = { Text("e.g. You are a financial analyst specializing in SaaS unit economics...") }
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val prompt = tempCustomPrompt.trim()
                    viewModel.setActivePersona("custom", prompt)
                    showCustomPersonaDialog = false
                    Toast.makeText(context, "Custom persona activated", Toast.LENGTH_SHORT).show()
                }) {
                    Text("Save & Activate")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomPersonaDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Dialog: Export Data Backup (JSON)
    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Download, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Data Backup Exported")
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "All chat history, sessions, endpoints, and settings have been exported into JSON. Copy to clipboard or share via Android intent.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = exportJsonText,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp),
                        textStyle = MaterialTheme.typography.labelSmall
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(exportJsonText))
                    Toast.makeText(context, "Backup copied to clipboard!", Toast.LENGTH_SHORT).show()
                }) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copy JSON")
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "application/json"
                            putExtra(Intent.EXTRA_SUBJECT, "LLM1_Backup.json")
                            putExtra(Intent.EXTRA_TEXT, exportJsonText)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share LLM#1 Backup"))
                    }) {
                        Text("Share")
                    }
                    TextButton(onClick = { showExportDialog = false }) {
                        Text("Done")
                    }
                }
            }
        )
    }

    // Dialog: Restore Data from Backup (JSON)
    if (showRestoreDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Upload, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Restore from Backup")
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Paste a valid LLM#1 backup JSON payload below to restore chat sessions, endpoints, and preferences.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = restoreJsonInput,
                        onValueChange = { restoreJsonInput = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp, max = 220.dp),
                        placeholder = { Text("Paste JSON backup here...") },
                        textStyle = MaterialTheme.typography.labelSmall
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedButton(
                        onClick = {
                            val clip = clipboardManager.getText()?.text
                            if (!clip.isNullOrBlank()) {
                                restoreJsonInput = clip
                            } else {
                                Toast.makeText(context, "Clipboard is empty", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.align(Alignment.End),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("Paste from Clipboard", fontSize = 12.sp)
                    }

                    if (restoreSummary != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (restoreSummary!!.success) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = restoreSummary!!.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (restoreSummary!!.success) Color(0xFF2E7D32) else Color(0xFFC62828),
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (restoreJsonInput.isNotBlank()) {
                            isRestoring = true
                            viewModel.restoreBackup(restoreJsonInput) { res ->
                                isRestoring = false
                                restoreSummary = res
                                if (res.success) {
                                    Toast.makeText(context, "Data restored successfully!", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    },
                    enabled = restoreJsonInput.isNotBlank() && !isRestoring
                ) {
                    if (isRestoring) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Restore Now")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Dialog: Reset Token Statistics Confirmation
    if (showResetTokensDialog) {
        AlertDialog(
            onDismissRequest = { showResetTokensDialog = false },
            title = { Text("Reset Token Statistics?") },
            text = { Text("This will clear prompt, completion, and request counters back to zero. Chat messages will remain untouched.") },
            confirmButton = {
                Button(onClick = {
                    viewModel.resetTokenStats()
                    showResetTokensDialog = false
                    Toast.makeText(context, "Token counters reset to 0", Toast.LENGTH_SHORT).show()
                }) {
                    Text("Reset")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetTokensDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// -------------------------------------------------------------------------------------------------
// CUSTOM ENDPOINT DIALOG WITH VERIFY API MODEL & API MODEL INTERFACE
// -------------------------------------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ApiConfigDialog(
    initialConfig: ApiConfig?,
    isDarkTheme: Boolean = false,
    onDismiss: () -> Unit,
    onTestConnection: suspend (endpoint: String, modelName: String, apiKey: String) -> Pair<Boolean, String>,
    onSave: (name: String, endpoint: String, modelName: String, apiKey: String, activate: Boolean) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    var name by remember { mutableStateOf(initialConfig?.name ?: "") }
    var endpoint by remember { mutableStateOf(initialConfig?.endpoint ?: "") }
    var modelName by remember { mutableStateOf(initialConfig?.modelName ?: "") }
    var apiKey by remember { mutableStateOf(initialConfig?.apiKey ?: "") }
    var showApiKey by remember { mutableStateOf(false) }

    var isTesting by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<Pair<Boolean, String>?>(null) }
    var submitted by remember { mutableStateOf(false) }

    val isNameError = submitted && name.isBlank()
    val isEndpointError = submitted && endpoint.isBlank()
    val isModelError = submitted && modelName.isBlank()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.92f)
                .clip(RoundedCornerShape(26.dp)),
            shape = RoundedCornerShape(26.dp),
            color = if (isDarkTheme) Color(0xFF1E1E1E) else Color.White,
            tonalElevation = 0.dp,
            border = BorderStroke(1.dp, if (isDarkTheme) Color(0xFF383838) else Color(0xFFCBD5E1))
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Top Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .background(
                                    if (isDarkTheme) Color(0xFF332717) else Color(0xFFFFF7ED),
                                    CircleShape
                                )
                                .border(
                                    1.dp,
                                    if (isDarkTheme) Color(0xFF5A3D1E) else Color(0xFFFED7AA),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (initialConfig == null) Icons.Default.AddLink else Icons.Default.Edit,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (initialConfig == null) "Add AI Endpoint" else "Edit AI Endpoint",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "OpenAI-compatible URL & custom models",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(34.dp)
                            .background(
                                if (isDarkTheme) Color(0xFF2E2E2E) else Color(0xFFF1F5F9),
                                CircleShape
                            )
                            .border(
                                1.dp,
                                if (isDarkTheme) Color(0xFF3D3D3D) else Color(0xFFCBD5E1),
                                CircleShape
                            )
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                HorizontalDivider(color = if (isDarkTheme) Color(0xFF333333) else Color(0xFFE2E8F0))

                // Scrollable Form Body
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Live Provider Preview Card
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isDarkTheme) Color(0xFF262626) else Color(0xFFF8FAFC),
                        border = BorderStroke(
                            1.dp,
                            if (testResult?.first == true) Color(0xFF10B981).copy(alpha = 0.6f)
                            else if (isDarkTheme) Color(0xFF383838) else Color(0xFFE2E8F0)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AiProviderBadge(
                                endpoint = endpoint,
                                name = name,
                                size = 44.dp,
                                shape = RoundedCornerShape(12.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = if (name.isNotBlank()) name else "Custom Provider",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (testResult?.first == true) {
                                        Surface(
                                            color = Color(0xFF10B981).copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = "Verified",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color(0xFF10B981),
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (modelName.isNotBlank()) modelName else "Model not configured",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (endpoint.isNotBlank()) endpoint else "Select a preset below or enter custom URL",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    // Quick Presets Section
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Default.Bolt,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Quick Presets",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "• Tap to fill",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PresetChip(
                                title = "OpenRouter",
                                providerId = "openrouter",
                                endpointUrl = "https://openrouter.ai/api/v1",
                                isSelected = endpoint.contains("openrouter.ai"),
                                isDarkTheme = isDarkTheme,
                                onClick = {
                                    name = "OpenRouter"
                                    endpoint = "https://openrouter.ai/api/v1"
                                    modelName = "meta-llama/llama-3.3-70b-instruct"
                                    testResult = null
                                }
                            )
                            PresetChip(
                                title = "OpenAI",
                                providerId = "openai",
                                endpointUrl = "https://api.openai.com/v1",
                                isSelected = endpoint.contains("api.openai.com"),
                                isDarkTheme = isDarkTheme,
                                onClick = {
                                    name = "OpenAI"
                                    endpoint = "https://api.openai.com/v1"
                                    modelName = "gpt-4o-mini"
                                    testResult = null
                                }
                            )
                            PresetChip(
                                title = "Gemini",
                                providerId = "gemini",
                                endpointUrl = "https://generativelanguage.googleapis.com/v1beta/openai",
                                isSelected = endpoint.contains("generativelanguage.googleapis.com"),
                                isDarkTheme = isDarkTheme,
                                onClick = {
                                    name = "Google Gemini"
                                    endpoint = "https://generativelanguage.googleapis.com/v1beta/openai"
                                    modelName = "gemini-1.5-flash"
                                    testResult = null
                                }
                            )
                            PresetChip(
                                title = "Groq",
                                providerId = "groq",
                                endpointUrl = "https://api.groq.com/openai/v1",
                                isSelected = endpoint.contains("api.groq.com"),
                                isDarkTheme = isDarkTheme,
                                onClick = {
                                    name = "Groq"
                                    endpoint = "https://api.groq.com/openai/v1"
                                    modelName = "llama-3.3-70b-versatile"
                                    testResult = null
                                }
                            )
                            PresetChip(
                                title = "DeepSeek",
                                providerId = "deepseek",
                                endpointUrl = "https://api.deepseek.com/v1",
                                isSelected = endpoint.contains("deepseek.com"),
                                isDarkTheme = isDarkTheme,
                                onClick = {
                                    name = "DeepSeek"
                                    endpoint = "https://api.deepseek.com/v1"
                                    modelName = "deepseek-chat"
                                    testResult = null
                                }
                            )
                            PresetChip(
                                title = "Claude",
                                providerId = "anthropic",
                                endpointUrl = "https://api.anthropic.com/v1",
                                isSelected = endpoint.contains("anthropic.com"),
                                isDarkTheme = isDarkTheme,
                                onClick = {
                                    name = "Anthropic (Claude)"
                                    endpoint = "https://api.anthropic.com/v1"
                                    modelName = "claude-3-5-sonnet-latest"
                                    testResult = null
                                }
                            )
                            PresetChip(
                                title = "Perplexity",
                                providerId = "perplexity",
                                endpointUrl = "https://api.perplexity.ai",
                                isSelected = endpoint.contains("perplexity.ai"),
                                isDarkTheme = isDarkTheme,
                                onClick = {
                                    name = "Perplexity AI"
                                    endpoint = "https://api.perplexity.ai"
                                    modelName = "sonar-pro"
                                    testResult = null
                                }
                            )
                            PresetChip(
                                title = "Ollama Local",
                                providerId = "ollama",
                                endpointUrl = "http://10.0.2.2:11434/v1",
                                isSelected = endpoint.contains("11434") || name.contains("Ollama"),
                                isDarkTheme = isDarkTheme,
                                onClick = {
                                    name = "Ollama (Local)"
                                    endpoint = "http://10.0.2.2:11434/v1"
                                    modelName = "llama3"
                                    testResult = null
                                }
                            )
                            PresetChip(
                                title = "LM Studio",
                                providerId = "lmstudio",
                                endpointUrl = "http://10.0.2.2:1234/v1",
                                isSelected = endpoint.contains("1234"),
                                isDarkTheme = isDarkTheme,
                                onClick = {
                                    name = "LM Studio"
                                    endpoint = "http://10.0.2.2:1234/v1"
                                    modelName = "local-model"
                                    testResult = null
                                }
                            )
                        }
                    }

                    // Input Fields
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        // Endpoint Name field
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Endpoint Name *") },
                            placeholder = { Text("e.g. My LLM Server, Custom Proxy") },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Badge,
                                    contentDescription = null,
                                    tint = if (name.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = if (isDarkTheme) Color(0xFF383838) else Color(0xFFCBD5E1),
                                focusedLabelColor = MaterialTheme.colorScheme.primary,
                                unfocusedLabelColor = if (isDarkTheme) Color(0xFF9E9E9E) else Color(0xFF64748B),
                                cursorColor = MaterialTheme.colorScheme.primary,
                                focusedContainerColor = if (isDarkTheme) Color(0xFF262626) else Color(0xFFF8FAFC),
                                unfocusedContainerColor = if (isDarkTheme) Color(0xFF202020) else Color(0xFFF8FAFC)
                            ),
                            isError = isNameError,
                            supportingText = if (isNameError) {
                                { Text("Endpoint name is required", color = MaterialTheme.colorScheme.error) }
                            } else null,
                            trailingIcon = {
                                if (name.isNotEmpty()) {
                                    IconButton(onClick = { name = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear name")
                                    }
                                }
                            }
                        )

                        // API Endpoint URL field
                        OutlinedTextField(
                            value = endpoint,
                            onValueChange = {
                                endpoint = it
                                testResult = null
                            },
                            label = { Text("API Endpoint URL *") },
                            placeholder = { Text("e.g. https://api.openai.com/v1") },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Dns,
                                    contentDescription = null,
                                    tint = if (endpoint.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 2,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = if (isDarkTheme) Color(0xFF383838) else Color(0xFFCBD5E1),
                                focusedLabelColor = MaterialTheme.colorScheme.primary,
                                unfocusedLabelColor = if (isDarkTheme) Color(0xFF9E9E9E) else Color(0xFF64748B),
                                cursorColor = MaterialTheme.colorScheme.primary,
                                focusedContainerColor = if (isDarkTheme) Color(0xFF262626) else Color(0xFFF8FAFC),
                                unfocusedContainerColor = if (isDarkTheme) Color(0xFF202020) else Color(0xFFF8FAFC)
                            ),
                            isError = isEndpointError,
                            supportingText = if (isEndpointError) {
                                { Text("Endpoint URL is required", color = MaterialTheme.colorScheme.error) }
                            } else {
                                { Text("Tip: For PC localhost on emulator use 10.0.2.2:port") }
                            },
                            trailingIcon = {
                                if (endpoint.isNotEmpty()) {
                                    IconButton(onClick = { endpoint = ""; testResult = null }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear endpoint URL")
                                    }
                                }
                            }
                        )

                        // Model Name field
                        OutlinedTextField(
                            value = modelName,
                            onValueChange = {
                                modelName = it
                                testResult = null
                            },
                            label = { Text("Model Identifier *") },
                            placeholder = { Text("e.g. gpt-4o-mini, llama-3.3-70b") },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.SmartToy,
                                    contentDescription = null,
                                    tint = if (modelName.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = if (isDarkTheme) Color(0xFF383838) else Color(0xFFCBD5E1),
                                focusedLabelColor = MaterialTheme.colorScheme.primary,
                                unfocusedLabelColor = if (isDarkTheme) Color(0xFF9E9E9E) else Color(0xFF64748B),
                                cursorColor = MaterialTheme.colorScheme.primary,
                                focusedContainerColor = if (isDarkTheme) Color(0xFF262626) else Color(0xFFF8FAFC),
                                unfocusedContainerColor = if (isDarkTheme) Color(0xFF202020) else Color(0xFFF8FAFC)
                            ),
                            isError = isModelError,
                            supportingText = if (isModelError) {
                                { Text("Model identifier is required", color = MaterialTheme.colorScheme.error) }
                            } else null,
                            trailingIcon = {
                                if (modelName.isNotEmpty()) {
                                    IconButton(onClick = { modelName = ""; testResult = null }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear model")
                                    }
                                }
                            }
                        )

                        // API Key field with Show/Hide and Paste
                        OutlinedTextField(
                            value = apiKey,
                            onValueChange = {
                                apiKey = it
                                testResult = null
                            },
                            label = { Text("API Key") },
                            placeholder = { Text("Optional for local servers") },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Key,
                                    contentDescription = null,
                                    tint = if (apiKey.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = if (isDarkTheme) Color(0xFF383838) else Color(0xFFCBD5E1),
                                focusedLabelColor = MaterialTheme.colorScheme.primary,
                                unfocusedLabelColor = if (isDarkTheme) Color(0xFF9E9E9E) else Color(0xFF64748B),
                                cursorColor = MaterialTheme.colorScheme.primary,
                                focusedContainerColor = if (isDarkTheme) Color(0xFF262626) else Color(0xFFF8FAFC),
                                unfocusedContainerColor = if (isDarkTheme) Color(0xFF202020) else Color(0xFFF8FAFC)
                            ),
                            visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                            supportingText = {
                                Text("Stored securely on device. Optional for local Ollama / LM Studio.")
                            },
                            trailingIcon = {
                                Row {
                                    IconButton(
                                        onClick = {
                                            val clip = clipboardManager.getText()?.text
                                            if (!clip.isNullOrBlank()) {
                                                apiKey = clip.trim()
                                                testResult = null
                                                Toast.makeText(context, "API Key pasted", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Default.ContentPaste, contentDescription = "Paste API Key")
                                    }
                                    IconButton(onClick = { showApiKey = !showApiKey }) {
                                        Icon(
                                            imageVector = if (showApiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = if (showApiKey) "Hide API Key" else "Show API Key"
                                        )
                                    }
                                }
                            }
                        )
                    }

                    // Test Connection Section
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isDarkTheme) Color(0xFF262626) else Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, if (isDarkTheme) Color(0xFF383838) else Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Test Connection",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Verify API key & model response",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                FilledTonalButton(
                                    onClick = {
                                        if (endpoint.isBlank() || modelName.isBlank()) {
                                            Toast.makeText(context, "Please enter endpoint and model first", Toast.LENGTH_SHORT).show()
                                            return@FilledTonalButton
                                        }
                                        isTesting = true
                                        testResult = null
                                        coroutineScope.launch {
                                            val res = onTestConnection(endpoint.trim(), modelName.trim(), apiKey.trim())
                                            isTesting = false
                                            testResult = res
                                        }
                                    },
                                    enabled = !isTesting && endpoint.isNotBlank() && modelName.isNotBlank(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = if (isDarkTheme) Color(0xFF332717) else Color(0xFFFFF7ED),
                                        contentColor = MaterialTheme.colorScheme.primary
                                    ),
                                    border = BorderStroke(1.dp, if (isDarkTheme) Color(0xFF5A3D1E) else Color(0xFFFED7AA))
                                ) {
                                    if (isTesting) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(14.dp),
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Testing...")
                                    } else {
                                        Icon(Icons.Default.NetworkCheck, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Verify Model", fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }

                            // Test Result Banner
                            AnimatedVisibility(visible = testResult != null) {
                                testResult?.let { (success, message) ->
                                    Column {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (success) {
                                            if (isDarkTheme) Color(0xFF064E3B).copy(alpha = 0.45f) else Color(0xFFECFDF5)
                                        } else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f),
                                        border = BorderStroke(
                                            1.dp,
                                            if (success) {
                                                if (isDarkTheme) Color(0xFF059669) else Color(0xFF10B981)
                                            } else MaterialTheme.colorScheme.error
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 14.dp, vertical = 10.dp),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = if (success) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                                                contentDescription = null,
                                                tint = if (success) {
                                                    if (isDarkTheme) Color(0xFF34D399) else Color(0xFF059669)
                                                } else MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = message,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Medium,
                                                textAlign = TextAlign.Center,
                                                color = if (success) {
                                                    if (isDarkTheme) Color(0xFF6EE7B7) else Color(0xFF065F46)
                                                } else MaterialTheme.colorScheme.onErrorContainer,
                                                modifier = Modifier.weight(1f, fill = false)
                                            )
                                        }
                                    }
                                    }
                                }
                            }
                        }
                    }
                }

                // Bottom Action Bar
                HorizontalDivider(color = if (isDarkTheme) Color(0xFF333333) else Color(0xFFE2E8F0))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            submitted = true
                            if (name.isNotBlank() && endpoint.isNotBlank() && modelName.isNotBlank()) {
                                onSave(name.trim(), endpoint.trim(), modelName.trim(), apiKey.trim(), false)
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 46.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.25.dp, if (isDarkTheme) Color(0xFF383838) else Color(0xFFCBD5E1))
                    ) {
                        Icon(Icons.Default.BookmarkBorder, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save Only", fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = {
                            submitted = true
                            if (name.isNotBlank() && endpoint.isNotBlank() && modelName.isNotBlank()) {
                                onSave(name.trim(), endpoint.trim(), modelName.trim(), apiKey.trim(), true)
                            }
                        },
                        modifier = Modifier
                            .weight(1.3f)
                            .defaultMinSize(minHeight = 46.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.FlashOn, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save & Activate", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun PresetChip(
    title: String,
    providerId: String,
    endpointUrl: String,
    isSelected: Boolean,
    isDarkTheme: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) {
            if (isDarkTheme) Color(0xFF332717) else Color(0xFFFFF7ED)
        } else {
            if (isDarkTheme) Color(0xFF262626) else Color(0xFFF8FAFC)
        },
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary
            else if (isDarkTheme) Color(0xFF383838) else Color(0xFFCBD5E1)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            AiProviderBadge(
                providerId = providerId,
                endpoint = endpointUrl,
                name = title,
                size = 20.dp,
                shape = RoundedCornerShape(5.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

