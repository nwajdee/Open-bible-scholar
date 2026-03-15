package com.openbiblescholar.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
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
import com.openbiblescholar.data.model.*
import com.openbiblescholar.services.tts.TtsPlaybackState
import com.openbiblescholar.ui.screens.reader.AiInsightUiState
import com.openbiblescholar.ui.theme.OBSColors

// ─── Top Bar ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderTopBar(
    book: String,
    chapter: Int,
    translation: String,
    books: List<BibleBook>,
    onBookChapterSelected: (String, Int) -> Unit,
    onTranslationChange: (String) -> Unit,
    onOpenStudyCenter: () -> Unit,
    onOpenPassageGuide: () -> Unit,
    onOpenSettings: () -> Unit,
    onToggleTts: () -> Unit,
    isTtsPlaying: Boolean,
    onNavigateUp: () -> Unit
) {
    var showBookPicker by remember { mutableStateOf(false) }
    var showTranslationMenu by remember { mutableStateOf(false) }

    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { showBookPicker = true }
            ) {
                Column {
                    Text(
                        text = "$book $chapter",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = translation,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    Icons.Filled.ArrowDropDown,
                    contentDescription = "Select book",
                    modifier = Modifier.size(20.dp)
                )
            }
        },
        navigationIcon = {
            IconButton(onClick = onNavigateUp) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
            }
        },
        actions = {
            // TTS toggle
            IconButton(onClick = onToggleTts) {
                Icon(
                    if (isTtsPlaying) Icons.Filled.VolumeUp else Icons.Outlined.VolumeUp,
                    contentDescription = if (isTtsPlaying) "Stop reading" else "Read aloud",
                    tint = if (isTtsPlaying) MaterialTheme.colorScheme.primary else LocalContentColor.current
                )
            }
            // Study Center
            IconButton(onClick = onOpenStudyCenter) {
                Icon(Icons.Outlined.School, contentDescription = "Study Center")
            }
            // More options
            var showMenu by remember { mutableStateOf(false) }
            IconButton(onClick = { showMenu = true }) {
                Icon(Icons.Filled.MoreVert, contentDescription = "More")
            }
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Passage Guide") },
                    leadingIcon = { Icon(Icons.Outlined.AutoStories, null) },
                    onClick = { showMenu = false; onOpenPassageGuide() }
                )
                DropdownMenuItem(
                    text = { Text("Change Translation") },
                    leadingIcon = { Icon(Icons.Outlined.Translate, null) },
                    onClick = { showMenu = false; showTranslationMenu = true }
                )
                Divider()
                DropdownMenuItem(
                    text = { Text("Settings") },
                    leadingIcon = { Icon(Icons.Outlined.Settings, null) },
                    onClick = { showMenu = false; onOpenSettings() }
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )

    // Book/chapter picker dialog
    if (showBookPicker) {
        BookChapterPickerDialog(
            books = books,
            currentBook = book,
            currentChapter = chapter,
            onSelected = { b, c -> onBookChapterSelected(b, c); showBookPicker = false },
            onDismiss = { showBookPicker = false }
        )
    }

    // Translation picker
    if (showTranslationMenu) {
        TranslationPickerDialog(
            current = translation,
            onSelected = { onTranslationChange(it); showTranslationMenu = false },
            onDismiss = { showTranslationMenu = false }
        )
    }
}

// ─── Bottom Bar ───────────────────────────────────────────────────────────────

@Composable
fun ReaderBottomBar(
    onPreviousChapter: () -> Unit,
    onNextChapter: () -> Unit,
    ttsState: TtsPlaybackState,
    onPlayPause: () -> Unit,
    onStop: () -> Unit,
    onShowTtsControls: () -> Unit
) {
    Surface(
        tonalElevation = 4.dp,
        shadowElevation = 8.dp
    ) {
        Column {
            // TTS mini-controls (visible when TTS is active)
            AnimatedVisibility(visible = ttsState.isPlaying || ttsState.isPaused) {
                TtsMiniBar(
                    ttsState = ttsState,
                    onPlayPause = onPlayPause,
                    onStop = onStop,
                    onExpand = onShowTtsControls
                )
            }

            // Chapter navigation
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onPreviousChapter,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.ChevronLeft, null, modifier = Modifier.size(20.dp))
                    Text("Previous", style = MaterialTheme.typography.labelLarge)
                }

                Divider(modifier = Modifier.height(24.dp).width(1.dp))

                TextButton(
                    onClick = onNextChapter,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Next", style = MaterialTheme.typography.labelLarge)
                    Icon(Icons.Filled.ChevronRight, null, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
private fun TtsMiniBar(
    ttsState: TtsPlaybackState,
    onPlayPause: () -> Unit,
    onStop: () -> Unit,
    onExpand: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "tts_pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.6f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "tts_alpha"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Filled.GraphicEq,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary.copy(alpha = if (ttsState.isSpeaking) alpha else 0.5f),
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            "Reading aloud",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onPlayPause, modifier = Modifier.size(36.dp)) {
            Icon(
                if (ttsState.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = "Play/Pause",
                tint = MaterialTheme.colorScheme.primary
            )
        }
        IconButton(onClick = onStop, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Filled.Stop, contentDescription = "Stop", tint = MaterialTheme.colorScheme.primary)
        }
        IconButton(onClick = onExpand, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Filled.ExpandLess, contentDescription = "TTS controls", tint = MaterialTheme.colorScheme.primary)
        }
    }
}

// ─── Book/Chapter Picker ───────────────────────────────────────────────────────

@Composable
fun BookChapterPickerDialog(
    books: List<BibleBook>,
    currentBook: String,
    currentChapter: Int,
    onSelected: (String, Int) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedBook by remember { mutableStateOf(currentBook) }
    var step by remember { mutableStateOf(if (books.isEmpty()) 0 else 1) } // 1=book, 2=chapter

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (step == 1) "Select Book" else "Select Chapter in $selectedBook")
        },
        text = {
            if (step == 1) {
                // Group by testament
                val ot = books.filter { it.testament == Testament.OLD }
                val nt = books.filter { it.testament == Testament.NEW }
                LazyColumn(modifier = Modifier.height(400.dp)) {
                    if (ot.isNotEmpty()) {
                        item {
                            Text("Old Testament", style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(vertical = 6.dp))
                        }
                        items(ot) { book ->
                            BookRow(book = book, selected = book.name == selectedBook) {
                                selectedBook = book.name
                                step = 2
                            }
                        }
                    }
                    if (nt.isNotEmpty()) {
                        item {
                            Divider(modifier = Modifier.padding(vertical = 8.dp))
                            Text("New Testament", style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(vertical = 6.dp))
                        }
                        items(nt) { book ->
                            BookRow(book = book, selected = book.name == selectedBook) {
                                selectedBook = book.name
                                step = 2
                            }
                        }
                    }
                    if (books.isEmpty()) {
                        item {
                            Text("No translations downloaded. Go to Library to download a Bible.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            } else {
                val bookMeta = books.find { it.name == selectedBook }
                val chCount = bookMeta?.chapterCount ?: 1
                LazyVerticalGrid(
                    columns = GridCells.Fixed(6),
                    modifier = Modifier.height(300.dp)
                ) {
                    items(chCount) { idx ->
                        val ch = idx + 1
                        val isCurrent = selectedBook == currentBook && ch == currentChapter
                        Box(
                            modifier = Modifier
                                .padding(4.dp)
                                .aspectRatio(1f)
                                .clip(CircleShape)
                                .background(
                                    if (isCurrent) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .clickable { onSelected(selectedBook, ch) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                ch.toString(),
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isCurrent) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (step == 2) {
                TextButton(onClick = { step = 1 }) { Text("← Books") }
            }
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun BookRow(book: BibleBook, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(book.name, style = MaterialTheme.typography.bodyMedium)
        Text("${book.chapterCount} ch", style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline)
    }
}

// ─── Translation Picker ───────────────────────────────────────────────────────

@Composable
fun TranslationPickerDialog(
    current: String,
    onSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val translations = listOf("KJV","ASV","WEB","YLT","Darby","Webster","KJVA")
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Translation") },
        text = {
            LazyColumn {
                items(translations) { t ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelected(t) }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = t == current, onClick = { onSelected(t) })
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(t, fontWeight = if (t == current) FontWeight.Bold else FontWeight.Normal)
                            Text(translationFullName(t), style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

private fun translationFullName(abbr: String) = mapOf(
    "KJV" to "King James Version (1769)",
    "ASV" to "American Standard Version (1901)",
    "WEB" to "World English Bible",
    "YLT" to "Young's Literal Translation",
    "Darby" to "Darby Bible (1890)",
    "Webster" to "Webster's Bible (1833)",
    "KJVA" to "KJV with Apocrypha"
).getOrDefault(abbr, abbr)

// ─── Verse Options Sheet ──────────────────────────────────────────────────────

@Composable
fun VerseOptionsSheet(
    verse: BibleVerse?,
    isBookmarked: Boolean,
    highlight: Highlight?,
    onHighlight: (HighlightColor) -> Unit,
    onBookmark: () -> Unit,
    onNote: () -> Unit,
    onReadAloud: () -> Unit,
    onAiInsight: () -> Unit,
    onStudyCenter: () -> Unit,
    onShare: () -> Unit
) {
    if (verse == null) return
    Column(modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 32.dp)) {
        // Verse text preview
        Text(
            "${verse.book} ${verse.chapter}:${verse.verse}",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            verse.text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 3,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        Divider(modifier = Modifier.padding(bottom = 12.dp))

        // Highlight row
        Text("Highlight", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            HighlightColor.values().forEach { color ->
                val hexColor = Color(android.graphics.Color.parseColor(color.hex))
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(hexColor)
                        .border(
                            width = if (highlight?.color == color.hex) 3.dp else 0.dp,
                            color = MaterialTheme.colorScheme.primary,
                            shape = CircleShape
                        )
                        .clickable { onHighlight(color) }
                )
            }
            if (highlight != null) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { onHighlight(highlight.color) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Close, null, modifier = Modifier.size(18.dp))
                }
            }
        }
        Spacer(Modifier.height(16.dp))

        // Action buttons grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            VerseActionButton(
                icon = if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                label = if (isBookmarked) "Unbookmark" else "Bookmark",
                tint = if (isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                onClick = onBookmark
            )
            VerseActionButton(Icons.Outlined.StickyNote2, "Add Note", onClick = onNote)
            VerseActionButton(Icons.Outlined.VolumeUp, "Read Aloud", onClick = onReadAloud)
            VerseActionButton(Icons.Outlined.Share, "Share", onClick = onShare)
        }
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            VerseActionButton(Icons.Outlined.AutoAwesome, "AI Insight", onClick = onAiInsight,
                tint = MaterialTheme.colorScheme.secondary)
            VerseActionButton(Icons.Outlined.School, "Study Center", onClick = onStudyCenter)
        }
    }
}

@Composable
private fun VerseActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(24.dp))
        Spacer(Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = tint)
    }
}

// ─── Highlight Picker ─────────────────────────────────────────────────────────

@Composable
fun HighlightPickerSheet(
    currentColor: HighlightColor?,
    onColorSelected: (HighlightColor) -> Unit,
    onRemove: () -> Unit
) {
    Column(modifier = Modifier.padding(24.dp)) {
        Text("Choose Highlight Color", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            HighlightColor.values().forEach { color ->
                val c = Color(android.graphics.Color.parseColor(color.hex))
                Box(
                    modifier = Modifier
                        .size(48.dp).clip(CircleShape).background(c)
                        .border(if (currentColor == color) 3.dp else 0.dp,
                            MaterialTheme.colorScheme.primary, CircleShape)
                        .clickable { onColorSelected(color) }
                )
            }
        }
        if (currentColor != null) {
            Spacer(Modifier.height(16.dp))
            TextButton(onClick = onRemove) {
                Icon(Icons.Filled.Close, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Remove Highlight")
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

// ─── Note Editor ─────────────────────────────────────────────────────────────

@Composable
fun NoteEditorSheet(
    verse: BibleVerse?,
    existingNote: BibleNote?,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var noteText by remember(existingNote) { mutableStateOf(existingNote?.content ?: "") }

    Column(modifier = Modifier.padding(20.dp).padding(bottom = 24.dp)) {
        Text(
            "Note for ${verse?.book} ${verse?.chapter}:${verse?.verse}",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = noteText,
            onValueChange = { noteText = it },
            modifier = Modifier.fillMaxWidth().height(180.dp),
            placeholder = { Text("Write your notes here...") },
            maxLines = 8
        )
        Spacer(Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onDismiss) { Text("Cancel") }
            Spacer(Modifier.width(8.dp))
            Button(onClick = { onSave(noteText) }, enabled = noteText.isNotBlank()) {
                Icon(Icons.Filled.Save, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Save Note")
            }
        }
    }
}

// ─── AI Insight Sheet ─────────────────────────────────────────────────────────

@Composable
fun AiInsightSheet(
    verse: BibleVerse?,
    aiState: AiInsightUiState,
    onLoadInsight: (AiInsightType) -> Unit,
    onLoadPassageGuide: () -> Unit
) {
    val insightTypes = listOf(
        AiInsightType.CONTEXTUAL_EXPLANATION to "Contextual Explanation",
        AiInsightType.CROSS_REFERENCES to "Cross References",
        AiInsightType.HISTORICAL_BACKGROUND to "Historical Background",
        AiInsightType.DEVOTIONAL to "Daily Devotional",
        AiInsightType.SERMON_OUTLINE to "Sermon Outline"
    )

    Column(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.AutoAwesome, null, tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(8.dp))
            Text("AI Study Tools", style = MaterialTheme.typography.titleMedium)
        }
        if (verse != null) {
            Text("${verse.book} ${verse.chapter}:${verse.verse} — ${verse.translation}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(top = 2.dp))
        }
        Spacer(Modifier.height(12.dp))

        // Quick-select buttons
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(insightTypes) { (type, label) ->
                FilterChip(
                    selected = aiState.type == type,
                    onClick = { onLoadInsight(type) },
                    label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                    leadingIcon = if (aiState.type == type) {
                        { Icon(Icons.Filled.Check, null, Modifier.size(14.dp)) }
                    } else null
                )
            }
            item {
                FilterChip(
                    selected = aiState.type == AiInsightType.PASSAGE_GUIDE,
                    onClick = onLoadPassageGuide,
                    label = { Text("Passage Guide") }
                )
            }
        }
        Spacer(Modifier.height(16.dp))

        // Content area
        when {
            aiState.isLoading -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth().padding(32.dp)) {
                    CircularProgressIndicator(modifier = Modifier.size(40.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("Generating insight...", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline)
                }
            }
            aiState.error != null -> {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
                        Icon(Icons.Outlined.ErrorOutline, null, tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(aiState.error, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
            }
            aiState.content.isNotEmpty() -> {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        aiState.content,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
            else -> {
                Text(
                    "Select an insight type above to get AI-powered Bible study help.\n\n" +
                    "Requires an internet connection and a free API key from Groq or OpenRouter.\n" +
                    "Configure in Settings → AI Settings.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ─── TTS Controls Sheet ───────────────────────────────────────────────────────

@Composable
fun TtsControlsSheet(
    ttsState: TtsPlaybackState,
    onPlayPause: () -> Unit,
    onStop: () -> Unit,
    onSpeedChange: (Float) -> Unit,
    onPitchChange: (Float) -> Unit
) {
    var speed by remember { mutableFloatStateOf(1.0f) }
    var pitch by remember { mutableFloatStateOf(1.0f) }

    Column(modifier = Modifier.padding(24.dp).padding(bottom = 32.dp)) {
        Text("Text-to-Speech Controls", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(20.dp))

        // Play/Pause/Stop
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilledTonalIconButton(onClick = onStop, modifier = Modifier.size(56.dp)) {
                Icon(Icons.Filled.Stop, "Stop", modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.width(16.dp))
            FilledIconButton(onClick = onPlayPause, modifier = Modifier.size(64.dp)) {
                Icon(
                    if (ttsState.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    "Play/Pause",
                    modifier = Modifier.size(32.dp)
                )
            }
        }
        Spacer(Modifier.height(24.dp))

        // Speed slider
        Text("Speed: ${String.format("%.1f", speed)}×",
            style = MaterialTheme.typography.labelLarge)
        Slider(
            value = speed,
            onValueChange = { speed = it; onSpeedChange(it) },
            valueRange = 0.5f..2.0f,
            steps = 5,
            modifier = Modifier.fillMaxWidth()
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("0.5×", style = MaterialTheme.typography.labelSmall)
            Text("2.0×", style = MaterialTheme.typography.labelSmall)
        }
        Spacer(Modifier.height(16.dp))

        // Pitch slider
        Text("Pitch: ${String.format("%.1f", pitch)}",
            style = MaterialTheme.typography.labelLarge)
        Slider(
            value = pitch,
            onValueChange = { pitch = it; onPitchChange(it) },
            valueRange = 0.5f..2.0f,
            steps = 5,
            modifier = Modifier.fillMaxWidth()
        )

        if (ttsState.errorMessage != null) {
            Spacer(Modifier.height(12.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Text(ttsState.errorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(12.dp))
            }
        }
    }
}

// ─── Share Sheet ──────────────────────────────────────────────────────────────

@Composable
fun ShareSheet(verse: BibleVerse?, onDismiss: () -> Unit) {
    if (verse == null) return
    val shareText = "\"${verse.text}\"\n— ${verse.book} ${verse.chapter}:${verse.verse} (${verse.translation})"

    Column(modifier = Modifier.padding(24.dp)) {
        Text("Share Verse", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(12.dp))
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Text(shareText, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(16.dp))
        }
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = { /* Share intent handled by Activity */ onDismiss() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Filled.Share, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Share as Text")
        }
        Spacer(Modifier.height(32.dp))
    }
}
