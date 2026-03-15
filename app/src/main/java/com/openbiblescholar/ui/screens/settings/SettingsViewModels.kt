package com.openbiblescholar.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openbiblescholar.data.model.AppSettings
import com.openbiblescholar.data.model.AiProvider
import com.openbiblescholar.data.repository.BibleRepository
import com.openbiblescholar.di.ApiKeyStore
import com.openbiblescholar.services.ai.AiStudyService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val bibleRepo: BibleRepository
) : ViewModel() {

    val settings: StateFlow<AppSettings> = bibleRepo.getSettings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    fun update(settings: AppSettings) {
        viewModelScope.launch {
            bibleRepo.saveSettings(settings)
        }
    }
}

// ─── API Key ViewModel ────────────────────────────────────────────────────────

data class ApiKeyUiState(
    val selectedProvider: AiProvider = AiProvider.GROQ,
    val apiKeys: Map<AiProvider, String> = emptyMap(),
    val selectedModel: String = "llama3-8b-8192",
    val showModelDropdown: Boolean = false,
    val isTesting: Boolean = false,
    val testResult: String? = null
)

@HiltViewModel
class ApiKeyViewModel @Inject constructor(
    private val apiKeyStore: ApiKeyStore,
    private val aiService: AiStudyService
) : ViewModel() {

    private val _uiState = MutableStateFlow(ApiKeyUiState())
    val uiState: StateFlow<ApiKeyUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    private fun load() {
        val keys = AiProvider.values().associateWith { apiKeyStore.getApiKey(it) }
        _uiState.update {
            it.copy(
                selectedProvider = apiKeyStore.getSelectedProvider(),
                apiKeys = keys,
                selectedModel = apiKeyStore.getSelectedModel()
            )
        }
    }

    fun selectProvider(provider: AiProvider) {
        apiKeyStore.saveSelectedProvider(provider)
        _uiState.update { it.copy(selectedProvider = provider) }
    }

    fun saveApiKey(provider: AiProvider, key: String) {
        apiKeyStore.saveApiKey(provider, key)
        _uiState.update { it.copy(apiKeys = it.apiKeys + (provider to key)) }
    }

    fun selectModel(model: String) {
        apiKeyStore.saveSelectedModel(model)
        _uiState.update { it.copy(selectedModel = model, showModelDropdown = false) }
    }

    fun toggleModelDropdown() {
        _uiState.update { it.copy(showModelDropdown = !it.showModelDropdown) }
    }

    fun testConnection() {
        _uiState.update { it.copy(isTesting = true, testResult = null) }
        viewModelScope.launch {
            val config = apiKeyStore.getAiConfig()
            val result = aiService.askAi(
                config,
                "You are a helpful assistant.",
                "Respond with exactly: 'Connection successful!'"
            )
            result.fold(
                onSuccess = { _uiState.update { s -> s.copy(isTesting = false, testResult = "✓ $it") } },
                onFailure = { _uiState.update { s -> s.copy(isTesting = false, testResult = "✗ ${it.message}") } }
            )
        }
    }
}

// ─── Reading Plan Screen + ViewModel ─────────────────────────────────────────

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

data class ReadingPlanUiState(
    val plans: List<PlanItem> = emptyList(),
    val activePlanDay: Int = 0,
    val activePlanId: String = "",
    val completedDays: Set<Int> = emptySet(),
    val isLoading: Boolean = false
)

data class PlanItem(
    val id: String,
    val name: String,
    val description: String,
    val durationDays: Int,
    val isActive: Boolean = false
)

@HiltViewModel
class ReadingPlanViewModel @Inject constructor() : ViewModel() {
    private val _uiState = MutableStateFlow(ReadingPlanUiState(
        plans = listOf(
            PlanItem("whole_bible_1yr", "Bible in One Year", "Read through the entire Bible in 365 days", 365),
            PlanItem("nt_90", "New Testament in 90 Days", "Complete the New Testament in 90 days", 90),
            PlanItem("psalms_proverbs", "Psalms & Proverbs", "Daily wisdom from Psalms and Proverbs (150 days)", 150),
            PlanItem("gospels_30", "The Four Gospels (30 Days)", "Journey through Matthew, Mark, Luke, and John", 30),
            PlanItem("chronological", "Chronological Bible", "Read Scripture in historical order (365 days)", 365),
            PlanItem("reading_law", "The Law & Prophets", "Focus on the Torah and prophetic books (180 days)", 180)
        )
    ))
    val uiState: StateFlow<ReadingPlanUiState> = _uiState.asStateFlow()

    fun startPlan(planId: String) {
        _uiState.update { it.copy(activePlanId = planId, activePlanDay = 1) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadingPlanScreen(
    onNavigateUp: () -> Unit,
    onOpenReader: (String, Int) -> Unit,
    viewModel: ReadingPlanViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reading Plans", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) { Icon(Icons.Filled.ArrowBack, "Back") }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(padding)
        ) {
            items(uiState.plans) { plan ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (plan.id == uiState.activePlanId)
                            MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (plan.id == uiState.activePlanId) Icons.Filled.CheckCircle
                                else Icons.Outlined.CalendarMonth,
                                null,
                                tint = if (plan.id == uiState.activePlanId)
                                    MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(plan.name, style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold)
                                Text("${plan.durationDays} days",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline)
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(plan.description, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(12.dp))
                        if (plan.id == uiState.activePlanId) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                LinearProgressIndicator(
                                    progress = uiState.completedDays.size.toFloat() / plan.durationDays,
                                    modifier = Modifier.weight(1f).height(6.dp)
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Text("Day ${uiState.activePlanDay} of ${plan.durationDays}",
                                style = MaterialTheme.typography.labelSmall)
                            Spacer(Modifier.height(8.dp))
                            Button(
                                onClick = { onOpenReader("Genesis", 1) },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("Continue Reading") }
                        } else {
                            OutlinedButton(
                                onClick = { viewModel.startPlan(plan.id) },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("Start Plan") }
                        }
                    }
                }
            }
        }
    }
}
