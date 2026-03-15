package com.openbiblescholar.ui.screens.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openbiblescholar.data.model.BibleVerse
import com.openbiblescholar.data.repository.BibleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ParallelViewUiState(
    val book: String = "John",
    val chapter: Int = 1,
    val columns: List<TranslationColumn> = emptyList(),
    val isLoading: Boolean = false
)

data class TranslationColumn(
    val translation: String,
    val verses: List<BibleVerse>
)

@HiltViewModel
class ParallelBibleViewModel @Inject constructor(
    private val bibleRepo: BibleRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ParallelViewUiState())
    val uiState: StateFlow<ParallelViewUiState> = _uiState.asStateFlow()

    private val activeTranslations = mutableListOf("KJV")

    fun load(book: String, chapter: Int) {
        _uiState.update { it.copy(book = book, chapter = chapter, isLoading = true) }
        viewModelScope.launch {
            val columns = activeTranslations.map { trans ->
                val verses = bibleRepo.getChapter(trans, book, chapter).first()
                TranslationColumn(trans, verses)
            }
            _uiState.update { it.copy(columns = columns, isLoading = false) }
        }
    }

    fun addTranslation(trans: String) {
        if (trans !in activeTranslations && activeTranslations.size < 4) {
            activeTranslations.add(trans)
            load(_uiState.value.book, _uiState.value.chapter)
        }
    }

    fun removeTranslation(trans: String) {
        if (activeTranslations.size > 1) {
            activeTranslations.remove(trans)
            _uiState.update { s ->
                s.copy(columns = s.columns.filter { it.translation != trans })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParallelBibleScreen(
    book: String,
    chapter: Int,
    onNavigateUp: () -> Unit,
    viewModel: ParallelBibleViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    LaunchedEffect(book, chapter) { viewModel.load(book, chapter) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Parallel Bibles", fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.titleMedium)
                        Text("$book $chapter",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) { Icon(Icons.Filled.ArrowBack, "Back") }
                },
                actions = {
                    if (uiState.columns.size < 4) {
                        IconButton(onClick = { showAddDialog = true }) {
                            Icon(Icons.Filled.AddCircleOutline, "Add translation")
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        val maxVerseCount = uiState.columns.maxOfOrNull { it.verses.size } ?: 0

        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // Column headers
            item {
                Row(modifier = Modifier.fillMaxWidth()) {
                    uiState.columns.forEach { col ->
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(col.translation,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold)
                            if (uiState.columns.size > 1) {
                                IconButton(
                                    onClick = { viewModel.removeTranslation(col.translation) },
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Icon(Icons.Filled.Close, "Remove",
                                        modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                        if (col != uiState.columns.last()) {
                            Divider(modifier = Modifier.width(1.dp).fillMaxHeight())
                        }
                    }
                }
                Divider()
            }

            // Verse rows
            items(maxVerseCount) { idx ->
                val verseNum = idx + 1
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (idx % 2 == 0) MaterialTheme.colorScheme.surface
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                ) {
                    uiState.columns.forEach { col ->
                        val verse = col.verses.getOrNull(idx)
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Text("$verseNum ",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold)
                            Text(
                                verse?.text ?: "—",
                                style = MaterialTheme.typography.bodySmall,
                                lineHeight = 20.sp
                            )
                        }
                        if (col != uiState.columns.last()) {
                            Divider(modifier = Modifier
                                .width(1.dp)
                                .fillMaxHeight()
                                .padding(vertical = 4.dp))
                        }
                    }
                }
            }
        }

        if (showAddDialog) {
            val available = listOf("KJV","ASV","WEB","YLT","Darby","Webster")
                .filter { t -> uiState.columns.none { it.translation == t } }
            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                title = { Text("Add Translation") },
                text = {
                    Column {
                        available.forEach { t ->
                            TextButton(
                                onClick = { viewModel.addTranslation(t); showAddDialog = false },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text(t) }
                        }
                        if (available.isEmpty()) {
                            Text("All available translations are already shown, or maximum 4 columns reached.",
                                style = MaterialTheme.typography.bodySmall)
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showAddDialog = false }) { Text("Cancel") }
                }
            )
        }
    }
}
