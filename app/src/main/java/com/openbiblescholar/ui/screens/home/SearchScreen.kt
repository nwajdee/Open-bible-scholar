package com.openbiblescholar.ui.screens.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openbiblescholar.data.model.BibleVerse
import com.openbiblescholar.data.repository.BibleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchUiState(
    val query: String = "",
    val results: List<BibleVerse> = emptyList(),
    val isSearching: Boolean = false,
    val hasSearched: Boolean = false,
    val translation: String = "KJV"
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val bibleRepo: BibleRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val queryFlow = MutableSharedFlow<String>(replay = 1)

    init {
        viewModelScope.launch {
            val settings = bibleRepo.getSettings().first()
            _uiState.update { it.copy(translation = settings.defaultTranslation) }
        }

        viewModelScope.launch {
            @OptIn(FlowPreview::class)
            queryFlow
                .debounce(400)
                .filter { it.length >= 3 }
                .collectLatest { query ->
                    _uiState.update { it.copy(isSearching = true) }
                    val results = bibleRepo.searchVerses(_uiState.value.translation, query)
                    _uiState.update { it.copy(results = results, isSearching = false, hasSearched = true) }
                }
        }
    }

    fun onQueryChange(query: String) {
        _uiState.update { it.copy(query = query) }
        viewModelScope.launch { queryFlow.emit(query) }
    }

    fun clearQuery() {
        _uiState.update { it.copy(query = "", results = emptyList(), hasSearched = false) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onVerseSelected: (String, Int, Int) -> Unit,
    onNavigateUp: () -> Unit,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    OutlinedTextField(
                        value = uiState.query,
                        onValueChange = viewModel::onQueryChange,
                        placeholder = { Text("Search the Bible...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                        trailingIcon = {
                            if (uiState.query.isNotEmpty()) {
                                IconButton(onClick = viewModel::clearQuery) {
                                    Icon(Icons.Filled.Close, "Clear")
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when {
                uiState.isSearching -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                uiState.hasSearched && uiState.results.isEmpty() -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Outlined.SearchOff, null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(56.dp))
                        Spacer(Modifier.height(16.dp))
                        Text("No results for \"${uiState.query}\"",
                            style = MaterialTheme.typography.bodyLarge)
                        Text("Try different keywords or check spelling",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline)
                    }
                }
                uiState.results.isNotEmpty() -> {
                    LazyColumn(contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            Text("${uiState.results.size} results",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.outline)
                        }
                        items(uiState.results) { verse ->
                            SearchResultCard(
                                verse = verse,
                                query = uiState.query,
                                onClick = { onVerseSelected(verse.book, verse.chapter, verse.verse) }
                            )
                        }
                    }
                }
                else -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(32.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Outlined.Search, null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(64.dp))
                        Spacer(Modifier.height(16.dp))
                        Text("Search the Bible",
                            style = MaterialTheme.typography.titleMedium)
                        Text("Type at least 3 characters to search across all downloaded translations",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultCard(verse: BibleVerse, query: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${verse.book} ${verse.chapter}:${verse.verse}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.width(8.dp))
                Text(verse.translation,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline)
            }
            Spacer(Modifier.height(6.dp))
            HighlightedText(text = verse.text, highlight = query)
        }
    }
}

@Composable
private fun HighlightedText(text: String, highlight: String) {
    if (highlight.isBlank()) {
        Text(text, style = MaterialTheme.typography.bodySmall)
        return
    }
    val lower = text.lowercase()
    val lowerHighlight = highlight.lowercase()
    val annotated = buildAnnotatedString {
        var idx = 0
        while (idx < text.length) {
            val found = lower.indexOf(lowerHighlight, idx)
            if (found == -1) {
                append(text.substring(idx))
                break
            }
            append(text.substring(idx, found))
            withStyle(SpanStyle(
                background = MaterialTheme.colorScheme.primaryContainer,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )) {
                append(text.substring(found, found + highlight.length))
            }
            idx = found + highlight.length
        }
    }
    Text(annotated, style = MaterialTheme.typography.bodySmall)
}
