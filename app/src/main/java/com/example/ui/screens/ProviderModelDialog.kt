package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.ApiConfig
import com.example.data.AiProviderInfo
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderModelDialog(
    provider: AiProviderInfo,
    existingConfig: ApiConfig?,
    isDarkTheme: Boolean = false,
    onDismiss: () -> Unit,
    onTestConnection: suspend (endpoint: String, model: String, apiKey: String) -> Pair<Boolean, String>,
    onSaveConfig: (name: String, endpoint: String, model: String, apiKey: String, activate: Boolean) -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()

    var name by remember {
        mutableStateOf(existingConfig?.name ?: provider.name)
    }
    var endpoint by remember {
        mutableStateOf(existingConfig?.endpoint?.ifBlank { null } ?: provider.defaultEndpoint)
    }
    var modelName by remember {
        mutableStateOf(existingConfig?.modelName?.ifBlank { null } ?: provider.defaultModel)
    }
    var apiKey by remember {
        mutableStateOf(existingConfig?.apiKey ?: "")
    }

    var showApiKey by remember { mutableStateOf(false) }
    var isTesting by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<Pair<Boolean, String>?>(null) }
    var submitted by remember { mutableStateOf(false) }

    val isNameError = submitted && name.isBlank()
    val isEndpointError = submitted && endpoint.isBlank()
    val isModelError = submitted && modelName.isBlank() && provider.defaultModel.isBlank()
    val isApiKeyError = submitted && provider.requiresApiKey && apiKey.isBlank()

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
                // Header Bar
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
                        AiProviderBadge(
                            provider = provider,
                            size = 42.dp,
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = provider.name,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (provider.badge != null) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        color = if (isDarkTheme) Color(0xFF332717) else Color(0xFFFFF7ED),
                                        shape = RoundedCornerShape(6.dp),
                                        border = BorderStroke(1.dp, if (isDarkTheme) Color(0xFF5A3D1E) else Color(0xFFFED7AA))
                                    ) {
                                        Text(
                                            text = provider.badge,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
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
                    // API Key Section
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Default.Key,
                                    contentDescription = null,
                                    tint = if (apiKey.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = if (provider.requiresApiKey) "API Key *" else "API Key (Optional)",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            if (!provider.docsUrl.isNullOrBlank()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(bottom = 2.dp)
                                ) {
                                    Icon(
                                        Icons.Default.OpenInNew,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = provider.docsUrl,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = apiKey,
                            onValueChange = {
                                apiKey = it
                                testResult = null
                            },
                            placeholder = {
                                Text(
                                    if (provider.requiresApiKey) "Paste your ${provider.name} API key..."
                                    else "Not needed for local models"
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
                            isError = isApiKeyError,
                            supportingText = if (isApiKeyError) {
                                { Text("API Key is required for ${provider.name}", color = MaterialTheme.colorScheme.error) }
                            } else null,
                            visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            trailingIcon = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (apiKey.isEmpty()) {
                                        IconButton(
                                            onClick = {
                                                val clip = clipboardManager.getText()?.text
                                                if (!clip.isNullOrBlank()) {
                                                    apiKey = clip.trim()
                                                    Toast.makeText(context, "API Key pasted", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        ) {
                                            Icon(Icons.Default.ContentPaste, contentDescription = "Paste API key", modifier = Modifier.size(18.dp))
                                        }
                                    }
                                    IconButton(onClick = { showApiKey = !showApiKey }) {
                                        Icon(
                                            imageVector = if (showApiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = if (showApiKey) "Hide API key" else "Show API key",
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        )
                    }

                    // Model Identifier
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Default.SmartToy,
                                contentDescription = null,
                                tint = if (modelName.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Model Identifier",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        OutlinedTextField(
                            value = modelName,
                            onValueChange = {
                                modelName = it
                                testResult = null
                            },
                            placeholder = { Text(if (provider.defaultModel.isNotBlank()) "e.g. ${provider.defaultModel}" else "Enter model ID") },
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
                            } else if (provider.defaultModel.isNotBlank()) {
                                { Text("Default: ${provider.defaultModel}") }
                            } else null,
                            trailingIcon = {
                                if (modelName.isNotEmpty()) {
                                    IconButton(onClick = { modelName = ""; testResult = null }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear model name", modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        )
                    }

                    // API Endpoint URL
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Default.Dns,
                                contentDescription = null,
                                tint = if (endpoint.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "API Base Endpoint URL *",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        OutlinedTextField(
                            value = endpoint,
                            onValueChange = {
                                endpoint = it
                                testResult = null
                            },
                            placeholder = { Text("e.g. ${provider.defaultEndpoint}") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = false,
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
                                { Text("Standard OpenAI-compatible API base URL") }
                            },
                            trailingIcon = {
                                if (endpoint != provider.defaultEndpoint && provider.defaultEndpoint.isNotBlank()) {
                                    IconButton(onClick = { endpoint = provider.defaultEndpoint }) {
                                        Icon(Icons.Default.Refresh, contentDescription = "Reset to default endpoint", modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        )
                    }

                    // Display Name field
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Default.Badge,
                                contentDescription = null,
                                tint = if (name.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Config / Display Name *",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            placeholder = { Text("e.g. ${provider.name}") },
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
                                { Text("Config name is required", color = MaterialTheme.colorScheme.error) }
                            } else null
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
                                        text = "Verify credentials & model response",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                FilledTonalButton(
                                    onClick = {
                                        val effectiveModel = modelName.trim().ifBlank { provider.defaultModel }
                                        if (endpoint.isBlank() || effectiveModel.isBlank()) {
                                            submitted = true
                                            Toast.makeText(context, "Please enter endpoint and model first", Toast.LENGTH_SHORT).show()
                                            return@FilledTonalButton
                                        }
                                        isTesting = true
                                        testResult = null
                                        coroutineScope.launch {
                                            val res = onTestConnection(endpoint.trim(), effectiveModel, apiKey.trim())
                                            isTesting = false
                                            testResult = res
                                        }
                                    },
                                    enabled = !isTesting,
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
                                                    .padding(horizontal = 12.dp, vertical = 10.dp),
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
                                                    color = if (success) {
                                                        if (isDarkTheme) Color(0xFF6EE7B7) else Color(0xFF065F46)
                                                    } else MaterialTheme.colorScheme.onErrorContainer,
                                                    modifier = Modifier.weight(1f)
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
                            val effectiveModel = modelName.trim().ifBlank { provider.defaultModel }
                            if (name.isBlank() || endpoint.isBlank() || effectiveModel.isBlank()) {
                                Toast.makeText(context, "Please fill in all required fields", Toast.LENGTH_SHORT).show()
                                return@OutlinedButton
                            }
                            onSaveConfig(name.trim(), endpoint.trim(), effectiveModel, apiKey.trim(), false)
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
                            val effectiveModel = modelName.trim().ifBlank { provider.defaultModel }
                            if (name.isBlank() || endpoint.isBlank() || effectiveModel.isBlank()) {
                                Toast.makeText(context, "Please fill in all required fields", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (provider.requiresApiKey && apiKey.isBlank()) {
                                Toast.makeText(context, "API Key is required for ${provider.name}", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            onSaveConfig(name.trim(), endpoint.trim(), effectiveModel, apiKey.trim(), true)
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
