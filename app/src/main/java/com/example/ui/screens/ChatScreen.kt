package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import com.example.data.AiProviderInfo
import com.example.data.ChatMessage
import com.example.data.ChatSession
import com.example.data.ProviderRegistry
import com.example.ui.util.AttachmentCategory
import com.example.ui.util.FileAttachmentHelper
import com.example.ui.util.ImageDownloadHelper
import com.example.ui.util.ImageHelper
import com.example.ui.util.MarkdownContent
import com.example.ui.util.MarkdownFormatter
import com.example.ui.util.ProcessedAttachment
import com.example.ui.util.VoiceManager
import com.example.ui.viewmodel.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ChatScreen(
    viewModel: MainViewModel,
    onNavigateToSettings: () -> Unit,
    onNavigateToLocalHost: () -> Unit = {}
) {
    val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val chatSessions by viewModel.chatSessions.collectAsStateWithLifecycle()
    val currentSessionId by viewModel.currentSessionId.collectAsStateWithLifecycle()
    val activeConfig by viewModel.activeApiConfig.collectAsStateWithLifecycle()
    val apiConfigs by viewModel.apiConfigs.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val isDarkTheme by viewModel.isDarkTheme.collectAsStateWithLifecycle()
    val isIncognitoMode by viewModel.isIncognitoMode.collectAsStateWithLifecycle()

    val isServerRunning by com.example.server.LocalHostManager.isRunning.collectAsStateWithLifecycle()

    val isWebSearchEnabled by viewModel.isWebSearchEnabled.collectAsStateWithLifecycle()
    val activeSearchEngineId by viewModel.activeSearchEngineId.collectAsStateWithLifecycle()
    val searchEngines by viewModel.searchEngines.collectAsStateWithLifecycle()
    val activeSearchEngine = searchEngines.firstOrNull { it.id == activeSearchEngineId }
        ?: searchEngines.firstOrNull { it.id == "google" }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    var inputText by remember { mutableStateOf("") }
    var attachedFile by remember { mutableStateOf<ProcessedAttachment?>(null) }
    var isProcessingFile by remember { mutableStateOf(false) }
    var fileProcessingError by remember { mutableStateOf<String?>(null) }
    var showAttachmentSheet by remember { mutableStateOf(false) }
    var viewAttachmentMessage by remember { mutableStateOf<ChatMessage?>(null) }

    // TTS & Voice Input states
    val isTtsEnabled by viewModel.isTtsEnabled.collectAsStateWithLifecycle()
    val isAutoSpeakEnabled by viewModel.isAutoSpeakEnabled.collectAsStateWithLifecycle()
    val ttsSpeechRate by viewModel.ttsSpeechRate.collectAsStateWithLifecycle()

    val voiceManager = remember { VoiceManager(context) }
    DisposableEffect(Unit) {
        onDispose {
            voiceManager.release()
        }
    }
    val isSpeaking by voiceManager.isSpeaking.collectAsStateWithLifecycle()
    val speakingMessageId by voiceManager.speakingMessageId.collectAsStateWithLifecycle()
    val isListening by voiceManager.isListening.collectAsStateWithLifecycle()

    var micPermissionGranted by remember {
        mutableStateOf(
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.RECORD_AUDIO
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }
    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        micPermissionGranted = granted
        if (granted) {
            voiceManager.startListening(
                onResult = { recognizedText ->
                    if (recognizedText.isNotBlank()) {
                        inputText = if (inputText.isBlank()) recognizedText else "$inputText $recognizedText"
                    }
                },
                onError = { err ->
                    Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                }
            )
        } else {
            Toast.makeText(context, "Audio permission is required for voice typing", Toast.LENGTH_SHORT).show()
        }
    }

    val processFileUri = { uri: Uri ->
        coroutineScope.launch {
            isProcessingFile = true
            try {
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (e: Exception) {}

                val processed = FileAttachmentHelper.processAttachment(context, uri)
                attachedFile = processed
            } catch (e: Exception) {
                fileProcessingError = "Error reading file: ${e.localizedMessage ?: "Unknown error"}"
            } finally {
                isProcessingFile = false
            }
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            processFileUri(uri)
        }
    }

    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            processFileUri(uri)
        }
    }

    val listState = rememberLazyListState()
    var showModelSwitcherSheet by remember { mutableStateOf(false) }
    var providerForSetupDialog by remember { mutableStateOf<AiProviderInfo?>(null) }
    var sessionToDelete by remember { mutableStateOf<ChatSession?>(null) }
    var previewImageUrl by remember { mutableStateOf<String?>(null) }
    var isDownloadingPreview by remember { mutableStateOf(false) }

    LaunchedEffect(chatMessages.size, isLoading) {
        if (chatMessages.isNotEmpty()) {
            val targetIndex = if (isLoading) chatMessages.size else chatMessages.size - 1
            if (targetIndex >= 0) {
                listState.animateScrollToItem(targetIndex)
            }
        }
    }

    // Auto-Speak feature: when enabled, read aloud the latest assistant response when it finishes generating
    var lastSpokenMessageId by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(chatMessages.size, isLoading, isAutoSpeakEnabled, isTtsEnabled) {
        if (isAutoSpeakEnabled && isTtsEnabled && !isLoading && chatMessages.isNotEmpty()) {
            val latest = chatMessages.lastOrNull()
            if (latest != null && latest.role != "user" && latest.id != lastSpokenMessageId && latest.content.isNotBlank()) {
                lastSpokenMessageId = latest.id
                voiceManager.speak(latest.content, latest.id, ttsSpeechRate)
            }
        }
    }

    // Fullscreen Image Preview Dialog with Download action
    if (previewImageUrl != null) {
        Dialog(
            onDismissRequest = { previewImageUrl = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.92f))
            ) {
                SubcomposeAsyncImage(
                    model = previewImageUrl,
                    contentDescription = "Full Image Preview",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    loading = {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    }
                )

                // Top action bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { previewImageUrl = null },
                        colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close preview")
                    }

                    FilledTonalButton(
                        onClick = {
                            if (!isDownloadingPreview && previewImageUrl != null) {
                                isDownloadingPreview = true
                                coroutineScope.launch(Dispatchers.Main) {
                                    ImageDownloadHelper.downloadAndSaveImage(context, previewImageUrl!!)
                                    isDownloadingPreview = false
                                }
                            }
                        },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        if (isDownloadingPreview) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Icon(
                                Icons.Default.Download,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Download")
                    }
                }
            }
        }
    }

    if (fileProcessingError != null) {
        AlertDialog(
            onDismissRequest = { fileProcessingError = null },
            confirmButton = {
                TextButton(onClick = { fileProcessingError = null }) {
                    Text("OK", color = MaterialTheme.colorScheme.primary)
                }
            },
            title = { Text("File Notice") },
            text = { Text(fileProcessingError!!) },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurface
        )
    }

    if (viewAttachmentMessage != null) {
        val attMsg = viewAttachmentMessage!!
        AlertDialog(
            onDismissRequest = { viewAttachmentMessage = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = getCategoryIconByName(attMsg.attachmentType),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = attMsg.attachmentName ?: "Attached File",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.6f)
                        .verticalScroll(rememberScrollState())
                ) {
                    MarkdownContent(
                        content = attMsg.attachmentText ?: "No extracted text preview available.",
                        textColor = MaterialTheme.colorScheme.onSurface,
                        primaryColor = MaterialTheme.colorScheme.primary,
                        isUser = false
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { viewAttachmentMessage = null }) {
                    Text("Close", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(20.dp)
        )
    }

    if (showAttachmentSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAttachmentSheet = false },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    text = "Attach to Message",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Extract content from documents, tables, or send images",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(18.dp))

                AttachmentOptionItem(
                    icon = Icons.AutoMirrored.Filled.Article,
                    iconTint = Color(0xFF1976D2),
                    title = "Documents & Text",
                    description = "PDF, Word (.docx, .doc), OpenDoc (.odt), RTF, TXT, Code",
                    onClick = {
                        showAttachmentSheet = false
                        documentPickerLauncher.launch(
                            arrayOf(
                                "application/pdf",
                                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                                "application/msword",
                                "application/vnd.oasis.opendocument.text",
                                "application/rtf",
                                "text/rtf",
                                "text/plain",
                                "text/markdown",
                                "application/json",
                                "application/xml",
                                "text/xml"
                            )
                        )
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))

                AttachmentOptionItem(
                    icon = Icons.Default.TableChart,
                    iconTint = Color(0xFF2E7D32),
                    title = "Spreadsheets & Tabular Data",
                    description = "Excel (.xlsx, .xls), OpenDoc (.ods), CSV, TSV",
                    onClick = {
                        showAttachmentSheet = false
                        documentPickerLauncher.launch(
                            arrayOf(
                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                "application/vnd.ms-excel",
                                "application/vnd.oasis.opendocument.spreadsheet",
                                "text/csv",
                                "text/comma-separated-values",
                                "text/tab-separated-values"
                            )
                        )
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))

                AttachmentOptionItem(
                    icon = Icons.Default.Slideshow,
                    iconTint = Color(0xFFE65100),
                    title = "Presentations",
                    description = "PowerPoint (.pptx, .ppt), OpenDoc (.odp) slides",
                    onClick = {
                        showAttachmentSheet = false
                        documentPickerLauncher.launch(
                            arrayOf(
                                "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                                "application/vnd.ms-powerpoint",
                                "application/vnd.oasis.opendocument.presentation"
                            )
                        )
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))

                AttachmentOptionItem(
                    icon = Icons.Default.Image,
                    iconTint = Color(0xFF8E24AA),
                    title = "Photos & Graphics",
                    description = "JPG, PNG, GIF, SVG, TIFF, WebP",
                    onClick = {
                        showAttachmentSheet = false
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))

                AttachmentOptionItem(
                    icon = Icons.Default.FolderOpen,
                    iconTint = MaterialTheme.colorScheme.primary,
                    title = "Browse All Supported Files",
                    description = "Select any file from device storage",
                    onClick = {
                        showAttachmentSheet = false
                        documentPickerLauncher.launch(arrayOf("*/*"))
                    }
                )
            }
        }
    }

    // Direct Provider Setup Dialog from Model Switcher
    if (providerForSetupDialog != null) {
        val prov = providerForSetupDialog!!
        ProviderModelDialog(
            provider = prov,
            existingConfig = null,
            onDismiss = { providerForSetupDialog = null },
            onTestConnection = { endpoint, model, apiKey ->
                viewModel.testApiConnection(endpoint, model, apiKey)
            },
            onSaveConfig = { name, endpoint, model, apiKey, activate ->
                if (activate) {
                    viewModel.saveAndActivateApiConfig(null, name, endpoint, model, apiKey)
                    Toast.makeText(context, "Saved & activated $name ($model)", Toast.LENGTH_SHORT).show()
                } else {
                    viewModel.addApiConfig(name, endpoint, model, apiKey)
                    Toast.makeText(context, "Saved $name ($model)", Toast.LENGTH_SHORT).show()
                }
                providerForSetupDialog = null
            }
        )
    }

    // Modern Model Switcher Bottom Sheet
    if (showModelSwitcherSheet) {
        ModelSwitcherBottomSheet(
            apiConfigs = apiConfigs,
            activeConfig = activeConfig,
            onSelectConfig = { configId ->
                viewModel.setActiveConfig(configId)
                showModelSwitcherSheet = false
                val selected = apiConfigs.find { it.id == configId }
                if (selected != null) {
                    Toast.makeText(context, "Switched to ${selected.name} (${selected.modelName})", Toast.LENGTH_SHORT).show()
                }
            },
            onOpenSetupDialog = { prov ->
                showModelSwitcherSheet = false
                providerForSetupDialog = prov
            },
            onNavigateToSettings = {
                showModelSwitcherSheet = false
                onNavigateToSettings()
            },
            onDismiss = { showModelSwitcherSheet = false }
        )
    }

    if (errorMessage != null) {
        AlertDialog(
            onDismissRequest = { viewModel.clearError() },
            confirmButton = {
                TextButton(onClick = { viewModel.clearError() }) {
                    Text("OK", color = MaterialTheme.colorScheme.primary)
                }
            },
            title = { Text("Error") },
            text = { Text(errorMessage!!) },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurface
        )
    }

    if (sessionToDelete != null) {
        AlertDialog(
            onDismissRequest = { sessionToDelete = null },
            title = { Text("Delete Conversation", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to permanently delete \"${sessionToDelete?.title}\"? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        sessionToDelete?.let { viewModel.deleteSession(it.id) }
                        sessionToDelete = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { sessionToDelete = null }) {
                    Text("Cancel")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(20.dp)
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = true,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.fillMaxWidth(0.85f).widthIn(max = 320.dp),
                drawerContainerColor = if (isDarkTheme) Color(0xFF1E1E1E) else Color(0xFFFAFAFA)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .padding(vertical = 12.dp)
                ) {
                    // Header with title and Start New Chat action
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Chat History",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        IconButton(
                            onClick = {
                                viewModel.startNewChat()
                                coroutineScope.launch { drawerState.close() }
                            }
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "New Chat",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Button: New Chat
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                viewModel.startNewChat()
                                coroutineScope.launch { drawerState.close() }
                            },
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "New Chat",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                    )

                    // Sessions list
                    if (chatSessions.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No past chats yet.\nStart messaging to create history!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(chatSessions, key = { it.id }) { session ->
                                val isSelected = session.id == currentSessionId
                                val dateFormat = remember { SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()) }
                                val formattedDate = remember(session.updatedAt) {
                                    dateFormat.format(Date(session.updatedAt))
                                }
                                
                                val dismissState = rememberSwipeToDismissBoxState(
                                    confirmValueChange = {
                                        if (it == SwipeToDismissBoxValue.EndToStart) {
                                            viewModel.deleteSession(session.id)
                                            true
                                        } else {
                                            false
                                        }
                                    }
                                )

                                SwipeToDismissBox(
                                    state = dismissState,
                                    enableDismissFromStartToEnd = false,
                                    enableDismissFromEndToStart = true,
                                    backgroundContent = {
                                        val color = when (dismissState.targetValue) {
                                            SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.error
                                            else -> Color.Transparent
                                        }
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(color)
                                                .padding(horizontal = 20.dp),
                                            contentAlignment = Alignment.CenterEnd
                                        ) {
                                            if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) {
                                                Icon(
                                                    Icons.Default.Delete,
                                                    contentDescription = "Delete Session",
                                                    tint = MaterialTheme.colorScheme.onError
                                                )
                                            }
                                        }
                                    }
                                ) {
                                    Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable {
                                            viewModel.selectSession(session.id)
                                            coroutineScope.launch { drawerState.close() }
                                        },
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                                    } else {
                                        Color.Transparent
                                    },
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.AutoMirrored.Filled.Chat,
                                            contentDescription = null,
                                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = session.title,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = formattedDate,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                                            )
                                        }
                                        IconButton(
                                            onClick = { sessionToDelete = session },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.DeleteOutline,
                                                contentDescription = "Delete chat",
                                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                                }
                            }
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                    )

                    // Local Host Server entry
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 2.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                onNavigateToLocalHost()
                                coroutineScope.launch { drawerState.close() }
                            }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Dns,
                                contentDescription = null,
                                tint = if (isServerRunning) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Local Host Server",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isServerRunning) Color(0xFF10B981).copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = if (isServerRunning) "ONLINE" else "OFF",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isServerRunning) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    // Footer settings entry
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 2.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                focusManager.clearFocus()
                                onNavigateToSettings()
                                coroutineScope.launch { drawerState.close() }
                            }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Settings",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                coroutineScope.launch {
                                    if (drawerState.isClosed) drawerState.open() else drawerState.close()
                                }
                            }
                        ) {
                            Icon(
                                Icons.Default.Menu,
                                contentDescription = "Chat History",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    title = {
                        Surface(
                            onClick = { showModelSwitcherSheet = true },
                            shape = RoundedCornerShape(14.dp),
                            color = if (activeConfig != null) {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                            } else {
                                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
                            },
                            border = BorderStroke(
                                1.dp,
                                if (activeConfig != null) MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                                else MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier.testTag("model_switcher_top_pill")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (activeConfig != null) {
                                    AiProviderBadge(
                                        endpoint = activeConfig!!.endpoint,
                                        name = activeConfig!!.name,
                                        size = 24.dp,
                                        shape = RoundedCornerShape(6.dp),
                                        isActive = true
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = activeConfig!!.name,
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.widthIn(max = 130.dp)
                                            )
                                            Spacer(modifier = Modifier.width(5.dp))
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFF10B981))
                                            )
                                        }
                                        Text(
                                            text = activeConfig!!.modelName,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.widthIn(max = 140.dp)
                                        )
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.AutoAwesome,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "LLM#1",
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.width(5.dp))
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFFE65100))
                                            )
                                        }
                                        Text(
                                            text = "Tap to choose API",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Switch Model",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                val newState = !isWebSearchEnabled
                                viewModel.setWebSearchEnabled(newState)
                            }
                        ) {
                            Icon(
                                Icons.Default.TravelExplore,
                                contentDescription = "Toggle Web Search",
                                tint = if (isWebSearchEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            )
                        }
                        IconButton(
                            onClick = { viewModel.startNewChat() }
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "New Chat",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(
                            onClick = {
                                viewModel.toggleIncognitoMode()
                            }
                        ) {
                            Icon(
                                imageVector = if (isIncognitoMode) androidx.compose.material.icons.Icons.Default.VisibilityOff else androidx.compose.material.icons.Icons.Default.Visibility,
                                contentDescription = "Incognito Mode",
                                tint = if (isIncognitoMode) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = {
                                focusManager.clearFocus()
                                onNavigateToSettings()
                            }
                        ) {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            },
            contentWindowInsets = WindowInsets.statusBars,
            containerColor = MaterialTheme.colorScheme.background
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .imePadding()
                    .navigationBarsPadding()
            ) {
                AnimatedVisibility(visible = isIncognitoMode) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Incognito Mode Active: Chats will not be saved",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                    }
                }
                if (chatMessages.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 420.dp),
                            shape = RoundedCornerShape(24.dp),
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 2.dp,
                            shadowElevation = 0.dp,
                            border = androidx.compose.foundation.BorderStroke(
                                1.25.dp,
                                if (isDarkTheme) Color(0xFF383838) else Color(0xFFCBD5E1)
                            )
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 28.dp)
                            ) {
                                if (activeConfig != null) {
                                    AiProviderBadge(
                                        endpoint = activeConfig!!.endpoint,
                                        name = activeConfig!!.name,
                                        size = 64.dp,
                                        shape = CircleShape,
                                        isActive = true
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(64.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.AutoAwesome,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = if (activeConfig != null) "Ready to chat with ${activeConfig!!.name}" else "Set up your API endpoint to start chatting",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                if (activeConfig == null) {
                                    Text(
                                        text = "Connect a local or cloud LLM endpoint to start sending messages.",
                                        style = MaterialTheme.typography.bodySmall,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                                    )
                                    Spacer(modifier = Modifier.height(18.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Button(modifier = Modifier.weight(1f),
                                            onClick = { showModelSwitcherSheet = true },
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                            shape = RoundedCornerShape(14.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
                                        ) {
                                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "Select AI Model",
                                                fontWeight = FontWeight.SemiBold,
                                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        OutlinedButton(modifier = Modifier.weight(1f),
                                            onClick = {
                                                focusManager.clearFocus()
                                                onNavigateToSettings()
                                            },
                                            shape = RoundedCornerShape(14.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
                                        ) {
                                            Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Settings", fontWeight = FontWeight.Medium, maxLines = 1)
                                        }
                                    }
                                } else {
                                    // Pill badge with model name
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = activeConfig!!.modelName,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = activeConfig!!.endpoint,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )

                                }
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(chatMessages, key = { it.id }) { message ->
                            MessageBubble(
                                message = message,
                                isTtsEnabled = isTtsEnabled,
                                isSpeakingThis = isSpeaking && speakingMessageId == message.id,
                                onSpeakClick = {
                                    if (isSpeaking && speakingMessageId == message.id) {
                                        voiceManager.stopSpeaking()
                                    } else {
                                        voiceManager.speak(message.content, message.id, ttsSpeechRate)
                                    }
                                },
                                onImageClick = { url -> previewImageUrl = url },
                                onViewAttachment = { msg -> viewAttachmentMessage = msg },
                                modifier = Modifier.animateItem()
                            )
                        }
                        if (isLoading) {
                            item(key = "loading") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp)
                                        .animateItem(),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Default.AutoAwesome,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Surface(
                                            shape = RoundedCornerShape(20.dp, 20.dp, 20.dp, 4.dp),
                                            color = MaterialTheme.colorScheme.surface,
                                            shadowElevation = 1.dp,
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                                        ) {
                                            TypingIndicator()
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Input area
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.background,
                    tonalElevation = 0.dp
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        AnimatedVisibility(
                            visible = attachedFile != null,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            if (attachedFile != null) {
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 2.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (attachedFile!!.localPreviewUri != null) {
                                            AsyncImage(
                                                model = attachedFile!!.localPreviewUri,
                                                contentDescription = attachedFile!!.fileName,
                                                modifier = Modifier
                                                    .size(48.dp)
                                                    .clip(RoundedCornerShape(10.dp)),
                                                contentScale = ContentScale.Crop
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .size(48.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = getCategoryIcon(attachedFile!!.category),
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(26.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                        }

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = attachedFile!!.fileName,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(top = 2.dp)
                                            ) {
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                                ) {
                                                    Text(
                                                        text = attachedFile!!.category.title.uppercase(),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.primary,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                                if (attachedFile!!.fileSizeBytes > 0) {
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = formatFileSize(attachedFile!!.fileSizeBytes),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                                    )
                                                }
                                            }
                                        }

                                        IconButton(
                                            onClick = { attachedFile = null },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Close,
                                                contentDescription = "Remove attached file",
                                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // File processing loading indicator
                        AnimatedVisibility(
                            visible = isProcessingFile,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Processing and extracting file content...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Add attachment (+) button supporting all documents, spreadsheets, images
                            OutlinedIconButton(
                                onClick = { showAttachmentSheet = true },
                                enabled = !isLoading && !isProcessingFile,
                                shape = CircleShape,
                                modifier = Modifier.size(48.dp),
                                border = BorderStroke(
                                    1.dp,
                                    if (attachedFile != null) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                    }
                                ),
                                colors = IconButtonDefaults.outlinedIconButtonColors(
                                    containerColor = if (attachedFile != null) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.surface
                                    },
                                    contentColor = if (attachedFile != null) {
                                        MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        MaterialTheme.colorScheme.primary
                                    },
                                    disabledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                                    disabledContentColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.38f)
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Attach file, document, or image",
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            OutlinedTextField(
                                value = inputText,
                                onValueChange = { inputText = it },
                                modifier = Modifier.weight(1f),
                                placeholder = {
                                    Text(
                                        if (attachedFile != null) "Ask about ${attachedFile!!.fileName}..."
                                        else "Message LLM#1..."
                                    )
                                },
                                shape = RoundedCornerShape(24.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                ),
                                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                                maxLines = 4
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            // Microphone button for voice input
                            IconButton(
                                onClick = {
                                    if (isListening) {
                                        voiceManager.stopListening()
                                    } else {
                                        if (micPermissionGranted) {
                                            voiceManager.startListening(
                                                onResult = { recognizedText ->
                                                    if (recognizedText.isNotBlank()) {
                                                        inputText = if (inputText.isBlank()) recognizedText else "$inputText $recognizedText"
                                                    }
                                                },
                                                onError = { err ->
                                                    Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                                                }
                                            )
                                        } else {
                                            audioPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                                        }
                                    }
                                },
                                modifier = Modifier.size(40.dp),
                                colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = if (isListening) MaterialTheme.colorScheme.errorContainer else Color.Transparent,
                                    contentColor = if (isListening) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            ) {
                                Icon(
                                    imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                                    contentDescription = if (isListening) "Stop voice input" else "Voice input"
                                )
                            }

                            Spacer(modifier = Modifier.width(4.dp))

                            val canSend = (inputText.isNotBlank() || attachedFile != null) && !isLoading && !isProcessingFile

                            IconButton(
                                onClick = {
                                    if (canSend && (inputText.isNotBlank() || attachedFile != null)) {
                                        val text = inputText
                                        val file = attachedFile
                                        inputText = ""
                                        attachedFile = null
                                        viewModel.sendMessage(
                                            content = text,
                                            imageUri = file?.localPreviewUri,
                                            imageBase64 = file?.base64Image,
                                            attachmentName = file?.fileName,
                                            attachmentType = file?.category?.title,
                                            attachmentText = file?.extractedText
                                        )
                                    }
                                },
                                enabled = canSend,
                                colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = if (canSend) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    contentColor = if (canSend) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                )
                            ) {
                                if (isProcessingFile) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                } else {
                                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send Message")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
    message: ChatMessage,
    modifier: Modifier = Modifier,
    isTtsEnabled: Boolean = false,
    isSpeakingThis: Boolean = false,
    onSpeakClick: (() -> Unit)? = null,
    onImageClick: ((String) -> Unit)? = null,
    onViewAttachment: ((ChatMessage) -> Unit)? = null
) {
    val isUser = message.role == "user"
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var showCopy by remember { mutableStateOf(false) }

    val copyMessageToClipboard = {
        val textToCopy = when {
            message.content.isNotBlank() -> message.content
            !message.attachmentText.isNullOrBlank() -> message.attachmentText
            !message.attachmentName.isNullOrBlank() -> message.attachmentName
            else -> ""
        }
        if (textToCopy.isNotBlank()) {
            clipboardManager.setText(AnnotatedString(textToCopy))
            try {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            } catch (ignored: Exception) {}
            Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (!isUser) {
            // Assistant Avatar
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        val maxBubbleWidth = if (isUser) 310.dp else 360.dp
        val bubbleShape = RoundedCornerShape(
            topStart = 20.dp,
            topEnd = 20.dp,
            bottomStart = if (isUser) 20.dp else 4.dp,
            bottomEnd = if (isUser) 4.dp else 20.dp
        )

        Column(
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = maxBubbleWidth)
        ) {
            Surface(
                color = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                shape = bubbleShape,
                shadowElevation = if (isUser) 0.dp else 1.dp,
                modifier = Modifier
                    .clip(bubbleShape)
                    .combinedClickable(
                        onClick = { showCopy = !showCopy },
                        onLongClick = { copyMessageToClipboard() }
                    )
            ) {
                Column {
                    if (!message.attachmentName.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isUser) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.18f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            border = BorderStroke(
                                1.dp,
                                if (isUser) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.25f)
                                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                                .clickable(enabled = !message.attachmentText.isNullOrBlank()) {
                                    onViewAttachment?.invoke(message)
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (isUser) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f)
                                            else MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = getCategoryIconByName(message.attachmentType),
                                        contentDescription = null,
                                        tint = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = message.attachmentName,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = message.attachmentType ?: "File",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isUser) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                                if (!message.attachmentText.isNullOrBlank()) {
                                    Text(
                                        text = "View",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(start = 6.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (!message.imageUri.isNullOrBlank()) {
                        AsyncImage(
                            model = message.imageUri,
                            contentDescription = "Attached image",
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(4f/3f, matchHeightConstraintsFirst = false)
                                .clip(
                                    RoundedCornerShape(
                                        topStart = 18.dp,
                                        topEnd = 18.dp,
                                        bottomStart = if (message.content.isBlank()) (if (isUser) 18.dp else 4.dp) else 6.dp,
                                        bottomEnd = if (message.content.isBlank()) (if (isUser) 4.dp else 18.dp) else 6.dp
                                    )
                                )
                                .padding(4.dp)
                                .clickable {
                                    onImageClick?.invoke(message.imageUri)
                                },
                            contentScale = ContentScale.Crop
                        )
                    }

                    if (message.content.isNotBlank()) {
                        val textColor = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                        val primaryColor = MaterialTheme.colorScheme.primary

                        MarkdownContent(
                            content = message.content,
                            textColor = textColor,
                            primaryColor = primaryColor,
                            isUser = isUser,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            onImageClick = onImageClick
                        )
                    }
                }
            }
            // Message actions row: Speaker (TTS) & Copy buttons
            val showTts = !isUser && isTtsEnabled && message.content.isNotBlank()
            if (showTts || showCopy || !isUser) {
                Row(
                    modifier = Modifier
                        .padding(top = 4.dp, start = if (!isUser) 4.dp else 0.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (showTts) {
                        Surface(
                            onClick = { onSpeakClick?.invoke() },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSpeakingThis) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            contentColor = if (isSpeakingThis) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.height(28.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isSpeakingThis) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                    contentDescription = if (isSpeakingThis) "Stop reading message" else "Listen to response",
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isSpeakingThis) "Stop" else "Listen",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSpeakingThis) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }

                    // Copy action button
                    Surface(
                        onClick = {
                            copyMessageToClipboard()
                            showCopy = false
                        },
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.height(28.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.ContentCopy,
                                contentDescription = "Copy message",
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "Copy",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TypingIndicator(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "typing_indicator")

    val dot0Bounce by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -7f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1000
                0f at 0 using FastOutSlowInEasing
                -7f at 250 using FastOutSlowInEasing
                0f at 500 using FastOutSlowInEasing
                0f at 1000
            },
            repeatMode = RepeatMode.Restart,
            initialStartOffset = StartOffset(0)
        ),
        label = "dot0_bounce"
    )
    val dot1Bounce by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -7f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1000
                0f at 0 using FastOutSlowInEasing
                -7f at 250 using FastOutSlowInEasing
                0f at 500 using FastOutSlowInEasing
                0f at 1000
            },
            repeatMode = RepeatMode.Restart,
            initialStartOffset = StartOffset(180)
        ),
        label = "dot1_bounce"
    )
    val dot2Bounce by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -7f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1000
                0f at 0 using FastOutSlowInEasing
                -7f at 250 using FastOutSlowInEasing
                0f at 500 using FastOutSlowInEasing
                0f at 1000
            },
            repeatMode = RepeatMode.Restart,
            initialStartOffset = StartOffset(360)
        ),
        label = "dot2_bounce"
    )

    val dot0Alpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1000
                0.35f at 0
                1f at 250
                0.35f at 500
                0.35f at 1000
            },
            repeatMode = RepeatMode.Restart,
            initialStartOffset = StartOffset(0)
        ),
        label = "dot0_alpha"
    )
    val dot1Alpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1000
                0.35f at 0
                1f at 250
                0.35f at 500
                0.35f at 1000
            },
            repeatMode = RepeatMode.Restart,
            initialStartOffset = StartOffset(180)
        ),
        label = "dot1_alpha"
    )
    val dot2Alpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1000
                0.35f at 0
                1f at 250
                0.35f at 500
                0.35f at 1000
            },
            repeatMode = RepeatMode.Restart,
            initialStartOffset = StartOffset(360)
        ),
        label = "dot2_alpha"
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier.padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val dots = listOf(
            Pair(dot0Bounce, dot0Alpha),
            Pair(dot1Bounce, dot1Alpha),
            Pair(dot2Bounce, dot2Alpha)
        )
        dots.forEach { (bounce, alpha) ->
            Box(
                modifier = Modifier
                    .offset(y = bounce.dp)
                    .size(7.5.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = alpha))
            )
        }
    }
}

@Composable
fun AttachmentOptionItem(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(iconTint.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                )
            }
        }
    }
}

fun getCategoryIcon(category: AttachmentCategory): ImageVector {
    return when (category) {
        AttachmentCategory.IMAGE -> Icons.Default.Image
        AttachmentCategory.PDF -> Icons.Default.PictureAsPdf
        AttachmentCategory.DOCUMENT -> Icons.AutoMirrored.Filled.Article
        AttachmentCategory.SPREADSHEET -> Icons.Default.TableChart
        AttachmentCategory.PRESENTATION -> Icons.Default.Slideshow
        AttachmentCategory.TEXT -> Icons.Default.Description
    }
}

fun getCategoryIconByName(type: String?): ImageVector {
    return when (type?.uppercase()) {
        "IMAGE" -> Icons.Default.Image
        "PDF", "PDF DOCUMENT" -> Icons.Default.PictureAsPdf
        "SPREADSHEET" -> Icons.Default.TableChart
        "PRESENTATION" -> Icons.Default.Slideshow
        "TEXT", "TEXT FILE" -> Icons.Default.Description
        else -> Icons.AutoMirrored.Filled.Article
    }
}

fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return ""
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return "%.1f KB".format(kb)
    val mb = kb / 1024.0
    return "%.1f MB".format(mb)
}

// -------------------------------------------------------------------------------------------------
// MODEL SWITCHER BOTTOM SHEET
// -------------------------------------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModelSwitcherBottomSheet(
    apiConfigs: List<com.example.data.ApiConfig>,
    activeConfig: com.example.data.ApiConfig?,
    onSelectConfig: (Int) -> Unit,
    onOpenSetupDialog: (AiProviderInfo) -> Unit,
    onNavigateToSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    val surfaceColor = MaterialTheme.colorScheme.surface
    val isDarkTheme = (surfaceColor.red * 0.299f + surfaceColor.green * 0.587f + surfaceColor.blue * 0.114f) < 0.5f

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "AI Models & Endpoints",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (apiConfigs.isEmpty()) "No active model • Choose a provider below"
                            else "${apiConfigs.size} configured • Tap to switch active model",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = {
                        onNavigateToSettings()
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            if (apiConfigs.isEmpty()) {
                // Empty state card
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isDarkTheme) Color(0xFF1E1E1E) else Color(0xFFF8FAFC),
                    border = BorderStroke(1.25.dp, if (isDarkTheme) Color(0xFF383838) else Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Dns,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No API Endpoints Added",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Connect Google Gemini, OpenAI, Claude, Groq, DeepSeek, or local Ollama to begin chatting with LLM#1.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        // Quick connect provider chips
                        Text(
                            text = "Popular Quick-Start Providers:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.align(Alignment.Start)
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val popularList = remember {
                                listOf("gemini", "openai", "groq", "deepseek", "anthropic", "openrouter", "ollama")
                                    .mapNotNull { id -> ProviderRegistry.findById(id) }
                            }
                            popularList.forEach { prov ->
                                SuggestionChip(
                                    onClick = { onOpenSetupDialog(prov) },
                                    icon = {
                                        AiProviderBadge(
                                            provider = prov,
                                            size = 20.dp,
                                            shape = RoundedCornerShape(4.dp)
                                        )
                                    },
                                    label = {
                                        Text(prov.name, fontWeight = FontWeight.Medium)
                                    },
                                    shape = RoundedCornerShape(10.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    val gemini = ProviderRegistry.findById("gemini")
                                        ?: ProviderRegistry.PRESET_PROVIDERS.first()
                                    onOpenSetupDialog(gemini)
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add API Endpoint", fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = {
                                    onNavigateToSettings()
                                },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Settings", fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            } else {
                // List of configurations
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.6f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(apiConfigs, key = { it.id }) { config ->
                        val isSelected = config.isActive
                        Surface(
                            onClick = { onSelectConfig(config.id) },
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) {
                                if (isDarkTheme) Color(0xFF2B2015) else Color(0xFFFFF7ED)
                            } else {
                                if (isDarkTheme) Color(0xFF1E1E1E) else Color.White
                            },
                            border = BorderStroke(
                                if (isSelected) 2.dp else 1.25.dp,
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else if (isDarkTheme) Color(0xFF383838) else Color(0xFFE2E8F0)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AiProviderBadge(
                                    endpoint = config.endpoint,
                                    name = config.name,
                                    size = 42.dp,
                                    shape = RoundedCornerShape(10.dp),
                                    isActive = isSelected
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = config.name,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (isSelected) {
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = Color(0xFF10B981).copy(alpha = 0.15f)
                                            ) {
                                                Text(
                                                    text = "ACTIVE",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF10B981),
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = config.modelName,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = extractEndpointDomain(config.endpoint),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                if (isSelected) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = "Active",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.RadioButtonUnchecked,
                                        contentDescription = "Switch to this model",
                                        tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                        modifier = Modifier.size(24.dp)
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

