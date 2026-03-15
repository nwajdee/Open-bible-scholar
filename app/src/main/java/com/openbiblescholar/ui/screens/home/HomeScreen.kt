package com.openbiblescholar.ui.screens.home

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.openbiblescholar.data.model.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenReader: (String, Int, Int) -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenReadingPlan: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("OpenBible Scholar",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary)
                        Text("Free • Offline • AI-powered",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                actions = {
                    IconButton(onClick = onOpenLibrary) {
                        Icon(Icons.Outlined.LibraryBooks, "Library")
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Outlined.Settings, "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(padding)
        ) {
            // Daily Verse card
            item {
                uiState.dailyVerse?.let { verse ->
                    DailyVerseCard(verse = verse, onOpen = {
                        onOpenReader(verse.book, verse.chapter, verse.verse)
                    })
                }
            }

            // Continue Reading
            uiState.lastRead?.let { last ->
                item {
                    ContinueReadingCard(
                        book = last.book,
                        chapter = last.chapter,
                        verse = last.verse,
                        onContinue = { onOpenReader(last.book, last.chapter, last.verse) }
                    )
                }
            }

            // Quick access grid
            item {
                Text("Quick Access",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold)
            }
            item {
                QuickAccessGrid(
                    onOpenReader = { onOpenReader("Genesis", 1, 1) },
                    onOpenReadingPlan = onOpenReadingPlan,
                    onOpenLibrary = onOpenLibrary,
                    onOpenSettings = onOpenSettings
                )
            }

            // Recent bookmarks
            if (uiState.recentBookmarks.isNotEmpty()) {
                item {
                    Text("Recent Bookmarks",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold)
                }
                items(uiState.recentBookmarks.take(5)) { bm ->
                    BookmarkRow(
                        bookmark = bm,
                        onClick = { onOpenReader(bm.book, bm.chapter, bm.verse) }
                    )
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun DailyVerseCard(verse: BibleVerse, onOpen: () -> Unit) {
    Card(
        onClick = onOpen,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.WbSunny, null, tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Verse of the Day",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(12.dp))
            Text(
                "\"${verse.text}\"",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "— ${verse.book} ${verse.chapter}:${verse.verse} (${verse.translation})",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun ContinueReadingCard(book: String, chapter: Int, verse: Int, onContinue: () -> Unit) {
    Card(
        onClick = onContinue,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.MenuBook, null, tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Continue Reading",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.outline)
                Text("$book $chapter:$verse",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold)
            }
            Icon(Icons.Filled.ChevronRight, null, tint = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
private fun QuickAccessGrid(
    onOpenReader: () -> Unit,
    onOpenReadingPlan: () -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val items = listOf(
        Triple(Icons.Outlined.MenuBook, "Bible", onOpenReader),
        Triple(Icons.Outlined.CalendarMonth, "Reading Plan", onOpenReadingPlan),
        Triple(Icons.Outlined.LibraryBooks, "Library", onOpenLibrary),
        Triple(Icons.Outlined.Settings, "Settings", onOpenSettings)
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items.forEach { (icon, label, onClick) ->
            Card(
                onClick = onClick,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(icon, null, tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp))
                    Spacer(Modifier.height(4.dp))
                    Text(label, style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun BookmarkRow(bookmark: Bookmark, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.Bookmark,
            null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("${bookmark.book} ${bookmark.chapter}:${bookmark.verse}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium)
            if (bookmark.label.isNotBlank()) {
                Text(bookmark.label, style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline)
            }
        }
        Icon(Icons.Filled.ChevronRight, null, tint = MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(16.dp))
    }
}
