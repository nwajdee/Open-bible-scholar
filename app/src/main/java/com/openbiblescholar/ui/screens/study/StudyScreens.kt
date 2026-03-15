package com.openbiblescholar.ui.screens.study

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openbiblescholar.data.model.BibleNote
import com.openbiblescholar.data.repository.BibleRepository
import com.openbiblescholar.di.ApiKeyStore
import com.openbiblescholar.services.ai.AiStudyService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StudyCenterUiState(
    val book: String = "",
    val chapter: Int = 0,
    val verse: Int = 0,
    val commentaryEntries: List<CommentaryItem> = emptyList(),
    val notes: List<NoteItem> = emptyList(),
    val crossReferences: List<CrossRef> = emptyList(),
    val dictionaryEntries: List<DictionaryItem> = emptyList(),
    val churchFathersEntries: List<ChurchFatherEntry> = emptyList(),
    val isLoading: Boolean = false
)

@HiltViewModel
class StudyCenterViewModel @Inject constructor(
    private val bibleRepo: BibleRepository,
    private val aiService: AiStudyService,
    private val apiKeyStore: ApiKeyStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(StudyCenterUiState())
    val uiState: StateFlow<StudyCenterUiState> = _uiState.asStateFlow()

    fun load(book: String, chapter: Int, verse: Int) {
        _uiState.update { it.copy(book = book, chapter = chapter, verse = verse, isLoading = true) }

        viewModelScope.launch {
            // Notes from DB
            bibleRepo.getNotesForVerse(book, chapter, verse).collect { notes ->
                val noteItems = notes.map { NoteItem(it.book, it.chapter, it.verse, it.content) }
                _uiState.update { it.copy(notes = noteItems, isLoading = false) }
            }
        }

        // Stub cross-references (in production: query SWORD cross-ref module)
        val crossRefs = buildStubCrossRefs(book, chapter, verse)
        _uiState.update { it.copy(crossReferences = crossRefs) }
    }

    private fun buildStubCrossRefs(book: String, chapter: Int, verse: Int): List<CrossRef> {
        // In production these would come from the Treasury of Scripture Knowledge SWORD module
        return listOf(
            CrossRef("John 1:1", "In the beginning was the Word, and the Word was with God..."),
            CrossRef("Genesis 1:1", "In the beginning God created the heaven and the earth."),
            CrossRef("Colossians 1:16", "For by him were all things created...")
        )
    }
}

// ─── Word Study Screen ────────────────────────────────────────────────────────

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordStudyScreen(
    word: String,
    strongsNumber: String,
    onNavigateUp: () -> Unit,
    viewModel: WordStudyViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(word, strongsNumber) {
        viewModel.loadWordStudy(word, strongsNumber)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Word Study", style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold)
                        if (strongsNumber.isNotBlank()) {
                            Text(strongsNumber, style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary)
                        }
                    }
                },
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
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Word header
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(word,
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary)
                    if (strongsNumber.isNotBlank()) {
                        Text(strongsNumber,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                    uiState.entry?.let { entry ->
                        Spacer(Modifier.height(8.dp))
                        Text("${entry.transliteration} (${entry.pronunciation})",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }

            // Strong's definition
            uiState.entry?.let { entry ->
                SectionCard(title = "Definition") {
                    Text(entry.definition, style = MaterialTheme.typography.bodyMedium)
                }
                if (entry.origin.isNotBlank()) {
                    SectionCard(title = "Origin") {
                        Text(entry.origin, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                SectionCard(title = "KJV Usage (${entry.occurrences} occurrences)") {
                    Text(entry.kjvUsage, style = MaterialTheme.typography.bodyMedium)
                }
            }

            // AI word study
            if (uiState.aiContent.isNotEmpty() || uiState.isLoadingAi) {
                SectionCard(title = "AI Deep Study") {
                    if (uiState.isLoadingAi) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    } else {
                        Text(uiState.aiContent, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            if (uiState.entry == null && !uiState.isLoading) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Strong's lexicon not downloaded. Go to Library → Dictionaries to download Strong's Greek or Hebrew Lexicon.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

// ─── Passage Guide Screen ──────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PassageGuideScreen(
    book: String,
    chapter: Int,
    onNavigateUp: () -> Unit,
    viewModel: PassageGuideViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(book, chapter) { viewModel.load(book, chapter) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Passage Guide", style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold)
                        Text("$book $chapter", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) { Icon(Icons.Filled.ArrowBack, "Back") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            when {
                uiState.isLoading -> {
                    Box(modifier = Modifier.fillMaxWidth().padding(64.dp),
                        contentAlignment = androidx.compose.ui.Alignment.Center) {
                        Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(Modifier.height(12.dp))
                            Text("Generating passage guide with AI...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline)
                        }
                    }
                }
                uiState.error != null -> {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Could not generate passage guide",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.height(4.dp))
                            Text(uiState.error,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                }
                uiState.guide.isNotEmpty() -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Text(uiState.guide,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(20.dp))
                    }
                }
                else -> {
                    Button(
                        onClick = { viewModel.generateGuide() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Outlined.AutoAwesome, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Generate AI Passage Guide")
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Requires internet + free API key. Configure in Settings → AI Settings.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}
