package com.openbiblescholar.ui.screens.study

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun StudyCenterScreen(
    book: String,
    chapter: Int,
    verse: Int,
    onNavigateUp: () -> Unit,
    onOpenWordStudy: (String, String) -> Unit,
    viewModel: StudyCenterViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val tabs = listOf("Commentary", "Notes", "Cross-Refs", "Dictionary", "Church Fathers")
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val scope = rememberCoroutineScope()

    LaunchedEffect(book, chapter, verse) {
        viewModel.load(book, chapter, verse)
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Column {
                            Text("Study Center", style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold)
                            Text("$book $chapter:$verse",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateUp) {
                            Icon(Icons.Filled.ArrowBack, "Back")
                        }
                    }
                )
                ScrollableTabRow(
                    selectedTabIndex = pagerState.currentPage,
                    edgePadding = 16.dp
                ) {
                    tabs.forEachIndexed { i, title ->
                        Tab(
                            selected = pagerState.currentPage == i,
                            onClick = {
                                scope.launch { pagerState.animateScrollToPage(i) }
                            },
                            text = { Text(title, style = MaterialTheme.typography.labelMedium) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.padding(padding)
        ) { page ->
            when (page) {
                0 -> CommentaryTab(uiState.commentaryEntries)
                1 -> NotesTab(uiState.notes, book, chapter, verse)
                2 -> CrossReferencesTab(uiState.crossReferences)
                3 -> DictionaryTab(uiState.dictionaryEntries)
                4 -> ChurchFathersTab(uiState.churchFathersEntries)
            }
        }
    }
}

@Composable
private fun CommentaryTab(entries: List<CommentaryItem>) {
    if (entries.isEmpty()) {
        EmptyStudyTab(
            icon = Icons.Outlined.MenuBook,
            title = "No Commentaries",
            subtitle = "Download commentaries from Library to see them here."
        )
        return
    }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(entries) { entry ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(entry.moduleName,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    Text(entry.text, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun NotesTab(
    notes: List<NoteItem>,
    book: String, chapter: Int, verse: Int
) {
    if (notes.isEmpty()) {
        EmptyStudyTab(
            icon = Icons.Outlined.StickyNote2,
            title = "No Notes Yet",
            subtitle = "Long-press any verse in the reader to add a note."
        )
        return
    }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(notes) { note ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.StickyNote2, null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("${note.book} ${note.chapter}:${note.verse}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.secondary)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(note.content, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun CrossReferencesTab(refs: List<CrossRef>) {
    if (refs.isEmpty()) {
        EmptyStudyTab(
            icon = Icons.Outlined.AccountTree,
            title = "No Cross References",
            subtitle = "Cross references will appear here when available."
        )
        return
    }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(refs) { ref ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Text(ref.reference,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.width(100.dp))
                Spacer(Modifier.width(8.dp))
                Text(ref.verseText, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun DictionaryTab(entries: List<DictionaryItem>) {
    if (entries.isEmpty()) {
        EmptyStudyTab(
            icon = Icons.Outlined.Search,
            title = "No Dictionary Entries",
            subtitle = "Download Easton's or Smith's Bible Dictionary from Library."
        )
        return
    }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(entries) { item ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(item.term,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary)
                    Text(item.source,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline)
                    Spacer(Modifier.height(6.dp))
                    Text(item.definition, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun ChurchFathersTab(entries: List<ChurchFatherEntry>) {
    if (entries.isEmpty()) {
        EmptyStudyTab(
            icon = Icons.Outlined.HistoryEdu,
            title = "No Church Fathers Entries",
            subtitle = "Download the Ante-Nicene or Nicene Fathers modules from Library."
        )
        return
    }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(entries) { entry ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(entry.author,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Bold)
                    Text(entry.workTitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline)
                    Spacer(Modifier.height(8.dp))
                    Text(entry.excerpt, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun EmptyStudyTab(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(56.dp))
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(6.dp))
        Text(subtitle, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

// Local DTOs for StudyCenter
data class CommentaryItem(val moduleName: String, val text: String)
data class NoteItem(val book: String, val chapter: Int, val verse: Int, val content: String)
data class CrossRef(val reference: String, val verseText: String)
data class DictionaryItem(val term: String, val source: String, val definition: String)
data class ChurchFatherEntry(val author: String, val workTitle: String, val excerpt: String)
