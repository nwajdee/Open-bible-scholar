package com.openbiblescholar.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.openbiblescholar.data.model.AiProvider
import com.openbiblescholar.data.model.AppSettings
import com.openbiblescholar.data.model.ReaderFont

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateUp: () -> Unit,
    onOpenApiKeys: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsState()
    val uriHandler = LocalUriHandler.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {

            // Reading
            SettingsSectionHeader("Reading")
            SettingsSlider(
                title = "Font Size",
                value = settings.fontSize,
                valueRange = 12f..30f,
                steps = 17,
                displayValue = "${settings.fontSize.toInt()}sp",
                onValueChange = { viewModel.update(settings.copy(fontSize = it)) }
            )
            SettingsToggle(
                title = "Show Verse Numbers",
                checked = settings.showVerseNumbers,
                onCheckedChange = { viewModel.update(settings.copy(showVerseNumbers = it)) }
            )
            SettingsToggle(
                title = "Show Section Headings",
                checked = settings.showSectionHeadings,
                onCheckedChange = { viewModel.update(settings.copy(showSectionHeadings = it)) }
            )
            SettingsToggle(
                title = "Paragraph Mode",
                subtitle = "Flow text without verse breaks",
                checked = settings.paragraphMode,
                onCheckedChange = { viewModel.update(settings.copy(paragraphMode = it)) }
            )

            Divider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))

            // Text to Speech
            SettingsSectionHeader("Text-to-Speech")
            SettingsToggle(
                title = "Show Strong's on Tap",
                subtitle = "Tap a word to see its Strong's number",
                checked = settings.showStrongsOnTap,
                onCheckedChange = { viewModel.update(settings.copy(showStrongsOnTap = it)) }
            )
            SettingsSlider(
                title = "Reading Speed",
                value = settings.ttsSpeed,
                valueRange = 0.5f..2.0f,
                displayValue = "${String.format("%.1f", settings.ttsSpeed)}×",
                onValueChange = { viewModel.update(settings.copy(ttsSpeed = it)) }
            )

            Divider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))

            // AI
            SettingsSectionHeader("AI Study Tools")
            SettingsToggle(
                title = "Enable AI Features",
                subtitle = "Requires internet & API key",
                checked = settings.aiEnabled,
                onCheckedChange = { viewModel.update(settings.copy(aiEnabled = it)) }
            )
            SettingsNavigationItem(
                title = "Configure API Keys",
                subtitle = "Set your free Groq or OpenRouter API key",
                icon = Icons.Outlined.Key,
                onClick = onOpenApiKeys
            )

            Divider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))

            // Default Translation
            SettingsSectionHeader("Bible Translation")
            var showTranslationDialog by remember { mutableStateOf(false) }
            SettingsNavigationItem(
                title = "Default Translation",
                subtitle = settings.defaultTranslation,
                icon = Icons.Outlined.Translate,
                onClick = { showTranslationDialog = true }
            )
            if (showTranslationDialog) {
                val translations = listOf("KJV", "ASV", "WEB", "YLT", "Darby")
                AlertDialog(
                    onDismissRequest = { showTranslationDialog = false },
                    title = { Text("Default Translation") },
                    text = {
                        Column {
                            translations.forEach { t ->
                                Row(
                                    modifier = Modifier.fillMaxWidth()
                                        .padding(vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = settings.defaultTranslation == t,
                                        onClick = {
                                            viewModel.update(settings.copy(defaultTranslation = t))
                                            showTranslationDialog = false
                                        }
                                    )
                                    Text(t, modifier = Modifier.padding(start = 8.dp))
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showTranslationDialog = false }) { Text("Done") }
                    }
                )
            }

            Divider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))

            // About
            SettingsSectionHeader("About")
            SettingsNavigationItem(
                title = "OpenBible Scholar v1.1",
                subtitle = "Free, offline, open-source Bible study",
                icon = Icons.Outlined.Info,
                onClick = {}
            )
            SettingsNavigationItem(
                title = "Support Development",
                subtitle = "Buy Me a Coffee ☕",
                icon = Icons.Outlined.Favorite,
                onClick = { uriHandler.openUri("https://buymeacoffee.com") }
            )
            SettingsNavigationItem(
                title = "Privacy Policy",
                subtitle = "No data collected. All data stays on device.",
                icon = Icons.Outlined.Lock,
                onClick = {}
            )

            Spacer(Modifier.height(32.dp))
        }
    }
}

// ─── Settings components ──────────────────────────────────────────────────────

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        title.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
    )
}

@Composable
fun SettingsToggle(
    title: String,
    subtitle: String = "",
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            if (subtitle.isNotBlank()) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline)
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun SettingsSlider(
    title: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    displayValue: String,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            Text(displayValue, style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps
        )
    }
}

@Composable
fun SettingsNavigationItem(
    title: String,
    subtitle: String = "",
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            if (subtitle.isNotBlank()) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline)
            }
        }
        Icon(Icons.Filled.ChevronRight, null, tint = MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(18.dp))
    }
}

// ─── API Key Settings ─────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApiKeySettingsScreen(
    onNavigateUp: () -> Unit,
    viewModel: ApiKeyViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val uriHandler = LocalUriHandler.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI API Keys", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) { Icon(Icons.Filled.ArrowBack, "Back") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).padding(20.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Info card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
                    Icon(Icons.Outlined.Info, null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "API keys are stored encrypted on your device using Android Keystore. They are never sent to us or any third party — only to the AI provider you choose.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            // Provider selection
            Text("Select Provider", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            AiProvider.values().forEach { provider ->
                ProviderCard(
                    provider = provider,
                    isSelected = uiState.selectedProvider == provider,
                    apiKey = uiState.apiKeys[provider] ?: "",
                    onSelect = { viewModel.selectProvider(provider) },
                    onKeyChange = { viewModel.saveApiKey(provider, it) },
                    onGetKey = {
                        val url = when (provider) {
                            AiProvider.GROQ -> "https://console.groq.com/keys"
                            AiProvider.OPENROUTER -> "https://openrouter.ai/keys"
                            else -> ""
                        }
                        if (url.isNotEmpty()) uriHandler.openUri(url)
                    }
                )
            }

            // Model selection
            Text("Model", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            val models = when (uiState.selectedProvider) {
                AiProvider.GROQ -> listOf("llama3-8b-8192", "llama3-70b-8192", "mixtral-8x7b-32768", "gemma-7b-it")
                AiProvider.OPENROUTER -> listOf("google/gemma-7b-it:free", "mistralai/mistral-7b-instruct:free", "openchat/openchat-7b:free")
                else -> listOf("llama3-8b-8192")
            }
            ExposedDropdownMenuBox(
                expanded = uiState.showModelDropdown,
                onExpandedChange = { viewModel.toggleModelDropdown() }
            ) {
                OutlinedTextField(
                    value = uiState.selectedModel,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Model") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = uiState.showModelDropdown) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = uiState.showModelDropdown,
                    onDismissRequest = { viewModel.toggleModelDropdown() }
                ) {
                    models.forEach { model ->
                        DropdownMenuItem(
                            text = { Text(model, style = MaterialTheme.typography.bodySmall) },
                            onClick = { viewModel.selectModel(model) }
                        )
                    }
                }
            }

            // Test connection
            Button(
                onClick = { viewModel.testConnection() },
                modifier = Modifier.fillMaxWidth()
            ) {
                if (uiState.isTesting) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Icon(Icons.Outlined.Wifi, null, modifier = Modifier.size(16.dp))
                }
                Spacer(Modifier.width(8.dp))
                Text(if (uiState.isTesting) "Testing..." else "Test Connection")
            }

            uiState.testResult?.let { result ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (result.startsWith("✓"))
                            MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(result, style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp))
                }
            }
        }
    }
}

@Composable
private fun ProviderCard(
    provider: AiProvider,
    isSelected: Boolean,
    apiKey: String,
    onSelect: () -> Unit,
    onKeyChange: (String) -> Unit,
    onGetKey: () -> Unit
) {
    var showKey by remember { mutableStateOf(false) }
    var keyInput by remember(apiKey) { mutableStateOf(apiKey) }

    Card(
        modifier = Modifier.fillMaxWidth()
            .clickable(onClick = onSelect),
        border = if (isSelected) CardDefaults.outlinedCardBorder() else null,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                             else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = isSelected, onClick = onSelect)
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(provider.displayName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Text(provider.baseUrl, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                }
                TextButton(onClick = onGetKey) { Text("Get free key →") }
            }
            if (isSelected) {
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = keyInput,
                    onValueChange = { keyInput = it; onKeyChange(it) },
                    label = { Text("API Key") },
                    placeholder = { Text("sk-...") },
                    singleLine = true,
                    visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showKey = !showKey }) {
                            Icon(if (showKey) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, null)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
