package com.openbiblescholar.ui.screens.home

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openbiblescholar.data.model.*
import com.openbiblescholar.data.repository.BibleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

data class HighlightsNotesUiState(
    val highlights: List<Highlight> = emptyList(),
    val notes: List<BibleNote> = emptyList(),
    val bookmarks: List<Bookmark> = emptyList(),
    val isLoading: Boolean = true,
    val filterColor: HighlightColor? = null
)

@HiltViewModel
class HighlightsNotesViewModel @Inject constructor(
    private val bibleRepo: BibleRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HighlightsNotesUiState())
    val uiState: StateFlow<HighlightsNotesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                bibleRepo.getAllHighlights(),
                bibleRepo.getAllNotes(),
                bibleRepo.getAllBookmarks()
            ) { highlights, notes, bookmarks ->
                HighlightsNotesUiState(
                    highlights = highlights,
                    notes = notes,
                    bookmarks = bookmarks,
                    isLoading = false
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun deleteNote(id: Long) = viewModelScope.launch { bibleRepo.deleteNote(id) }

    fun removeHighlight(book: String, chapter: Int, verse: Int) =
        viewModelScope.launch { bibleRepo.removeHighlight(book, chapter, verse) }

    fun removeBookmark(book: String, chapter: Int, verse: Int) =
        viewModelScope.launch { bibleRepo.toggleBookmark(book, chapter, verse) }

    fun filterByColor(color: HighlightColor?) {
        _uiState.update { it.copy(filterColor = color) }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HighlightsNotesScreen(
    onNavigateUp: () -> Unit,
    onVerseSelected: (String, Int, Int) -> Unit,
    viewModel: HighlightsNotesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val tabs = listOf("Highlights", "Notes", "Bookmarks")
    val pagerState = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("My Study Notes", fontWeight = FontWeight.SemiBold) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateUp) {
                            Icon(Icons.Filled.ArrowBack, "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { /* export */ }) {
                            Icon(Icons.Outlined.IosShare, "Export")
                        }
                    }
                )
                TabRow(selectedTabIndex = pagerState.currentPage) {
                    tabs.forEachIndexed { i, title ->
                        Tab(
                            selected = pagerState.currentPage == i,
                            onClick = { scope.launch { pagerState.animateScrollToPage(i) } },
                            text = { Text(title) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        HorizontalPager(state = pagerState, modifier = Modifier.padding(padding)) { page ->
            when (page) {
                0 -> HighlightsTab(
                    highlights = uiState.highlights,
                    filterColor = uiState.filterColor,
                    onFilterChange = viewModel::filterByColor,
                    onVerseSelected = onVerseSelected,
                    onDelete = { h -> viewModel.removeHighlight(h.book, h.chapter, h.verse) }
                )
                1 -> NotesTab(
                    notes = uiState.notes,
                    onVerseSelected = onVerseSelected,
                    onDelete = { viewModel.deleteNote(it.id) }
                )
                2 -> BookmarksTab(
                    bookmarks = uiState.bookmarks,
                    onVerseSelected = onVerseSelected,
                    onDelete = { b -> viewModel.removeBookmark(b.book, b.chapter, b.verse) }
                )
            }
        }
    }
}

@Composable
private fun HighlightsTab(
    highlights: List<Highlight>,
    filterColor: HighlightColor?,
    onFilterChange: (HighlightColor?) -> Unit,
    onVerseSelected: (String, Int, Int) -> Unit,
    onDelete: (Highlight) -> Unit
) {
    val filtered = if (filterColor != null) highlights.filter { it.color == filterColor }
                   else highlights

    Column {
        // Color filter chips
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = filterColor == null,
                onClick = { onFilterChange(null) },
                label = { Text("All (${highlights.size})") }
            )
            HighlightColor.values().forEach { color ->
                val count = highlights.count { it.color == color }
                if (count > 0) {
                    FilterChip(
                        selected = filterColor == color,
                        onClick = { onFilterChange(color) },
                        label = { Text("$count") },
                        leadingIcon = {
                            Box(
                                modifier = Modifier.size(12.dp).clip(CircleShape)
                                    .background(Color(android.graphics.Color.parseColor(color.hex)))
                            )
                        }
                    )
                }
            }
        }

        if (filtered.isEmpty()) {
            EmptyStateView(
                icon = Icons.Outlined.Highlight,
                message = "No highlights yet",
                hint = "Tap a verse in the reader and choose a color to highlight it"
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filtered, key = { it.id }) { highlight ->
                    HighlightCard(
                        highlight = highlight,
                        onClick = { onVerseSelected(highlight.book, highlight.chapter, highlight.verse) },
                        onDelete = { onDelete(highlight) }
                    )
                }
            }
        }
    }
}

@Composable
private fun HighlightCard(highlight: Highlight, onClick: () -> Unit, onDelete: () -> Unit) {
    val color = Color(android.graphics.Color.parseColor(highlight.color.hex))
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(3.dp, color.copy(alpha = 0.7f))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(20.dp).clip(CircleShape).background(color))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "${highlight.book} ${highlight.chapter}:${highlight.verse}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    formatDate(highlight.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
                if (highlight.tag.isNotBlank()) {
                    Text(highlight.tag,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary)
                }
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Outlined.Delete, "Delete", tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun NotesTab(
    notes: List<BibleNote>,
    onVerseSelected: (String, Int, Int) -> Unit,
    onDelete: (BibleNote) -> Unit
) {
    if (notes.isEmpty()) {
        EmptyStateView(
            icon = Icons.Outlined.StickyNote2,
            message = "No notes yet",
            hint = "Long-press a verse in the reader to add study notes"
        )
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(notes, key = { it.id }) { note ->
            NoteCard(
                note = note,
                onClick = { onVerseSelected(note.book, note.chapter, note.verse) },
                onDelete = { onDelete(note) }
            )
        }
    }
}

@Composable
private fun NoteCard(note: BibleNote, onClick: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.StickyNote2, null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    "${note.book} ${note.chapter}:${note.verse}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Text(formatDate(note.updatedAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline)
                Spacer(Modifier.width(4.dp))
                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Outlined.Delete, "Delete",
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(16.dp))
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                note.content,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 4,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            if (note.tags.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    note.tags.take(3).forEach { tag ->
                        SuggestionChip(
                            onClick = {},
                            label = { Text(tag, style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.height(24.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BookmarksTab(
    bookmarks: List<Bookmark>,
    onVerseSelected: (String, Int, Int) -> Unit,
    onDelete: (Bookmark) -> Unit
) {
    if (bookmarks.isEmpty()) {
        EmptyStateView(
            icon = Icons.Outlined.BookmarkBorder,
            message = "No bookmarks yet",
            hint = "Tap a verse and choose Bookmark to save it here"
        )
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(bookmarks, key = { it.id }) { bookmark ->
            Card(
                modifier = Modifier.fillMaxWidth().clickable {
                    onVerseSelected(bookmark.book, bookmark.chapter, bookmark.verse)
                },
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Bookmark,
                        null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("${bookmark.book} ${bookmark.chapter}:${bookmark.verse}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold)
                        if (bookmark.label.isNotBlank()) {
                            Text(bookmark.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline)
                        }
                        Text(formatDate(bookmark.createdAt),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline)
                    }
                    IconButton(onClick = { onDelete(bookmark) }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Outlined.Delete, "Delete",
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyStateView(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    message: String,
    hint: String
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(64.dp))
        Spacer(Modifier.height(16.dp))
        Text(message, style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(6.dp))
        Text(hint, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
