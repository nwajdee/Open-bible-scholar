package com.openbiblescholar.ui.screens.reader

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.openbiblescholar.data.model.*
import com.openbiblescholar.ui.components.*
import com.openbiblescholar.ui.theme.OBSColors
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BibleReaderScreen(
    initialBook: String,
    initialChapter: Int,
    initialVerse: Int,
    onNavigateUp: () -> Unit,
    onOpenStudyCenter: (String, Int, Int) -> Unit,
    onOpenWordStudy: (String, String) -> Unit,
    onOpenPassageGuide: (String, Int) -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: BibleReaderViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(initialBook, initialChapter, initialVerse) {
        viewModel.initialize(initialBook, initialChapter, initialVerse)
    }

    Scaffold(
        topBar = {
            ReaderTopBar(
                book = uiState.currentBook,
                chapter = uiState.currentChapter,
                translation = uiState.translation,
                books = uiState.books,
                onBookChapterSelected = { book, ch -> viewModel.navigateToChapter(book, ch) },
                onTranslationChange = { viewModel.changeTranslation(it) },
                onOpenStudyCenter = { onOpenStudyCenter(uiState.currentBook, uiState.currentChapter, uiState.currentVerse) },
                onOpenPassageGuide = { onOpenPassageGuide(uiState.currentBook, uiState.currentChapter) },
                onOpenSettings = onOpenSettings,
                onToggleTts = {
                    if (uiState.ttsState.isPlaying) viewModel.pauseTts()
                    else viewModel.readChapter()
                },
                isTtsPlaying = uiState.ttsState.isPlaying,
                onNavigateUp = onNavigateUp
            )
        },
        bottomBar = {
            ReaderBottomBar(
                onPreviousChapter = { viewModel.previousChapter() },
                onNextChapter = { viewModel.nextChapter() },
                ttsState = uiState.ttsState,
                onPlayPause = {
                    if (uiState.ttsState.isPlaying) viewModel.pauseTts()
                    else viewModel.readChapter()
                },
                onStop = { viewModel.stopTts() },
                onShowTtsControls = { viewModel.showBottomSheet(BottomSheetContent.TTS_CONTROLS) }
            )
        }
    ) { paddingValues ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (uiState.verses.isEmpty()) {
                EmptyChapterView(
                    book = uiState.currentBook,
                    chapter = uiState.currentChapter
                )
            } else {
                ChapterView(
                    verses = uiState.verses,
                    highlights = uiState.highlights,
                    notes = uiState.notes,
                    bookmarks = uiState.bookmarks,
                    settings = uiState.settings,
                    ttsState = uiState.ttsState,
                    onVerseTap = { verse ->
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.selectVerse(verse)
                    },
                    onWordTap = { word, strongs ->
                        if (strongs != null && uiState.settings.showStrongsOnTap) {
                            onOpenWordStudy(word, strongs)
                        }
                    }
                )
            }
        }

        // Bottom sheet for verse actions, notes, AI, TTS, etc.
        if (uiState.showBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = { viewModel.dismissBottomSheet() },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surface,
                dragHandle = { BottomSheetDefaults.DragHandle() }
            ) {
                when (uiState.bottomSheetContent) {
                    BottomSheetContent.VERSE_OPTIONS -> VerseOptionsSheet(
                        verse = uiState.selectedVerse,
                        isBookmarked = uiState.selectedVerse?.let { uiState.bookmarks.contains(it.verse) } ?: false,
                        highlight = uiState.selectedVerse?.let { uiState.highlights[it.verse] },
                        onHighlight = { color ->
                            uiState.selectedVerse?.let { viewModel.toggleHighlight(it.verse, color) }
                        },
                        onBookmark = { uiState.selectedVerse?.let { viewModel.toggleBookmark(it.verse) } },
                        onNote = { viewModel.showBottomSheet(BottomSheetContent.NOTE_EDITOR) },
                        onReadAloud = { viewModel.readCurrentVerse() },
                        onAiInsight = { viewModel.showBottomSheet(BottomSheetContent.AI_INSIGHT) },
                        onStudyCenter = {
                            uiState.selectedVerse?.let {
                                onOpenStudyCenter(it.book, it.chapter, it.verse)
                            }
                            viewModel.dismissBottomSheet()
                        },
                        onShare = { viewModel.showBottomSheet(BottomSheetContent.SHARE_OPTIONS) }
                    )
                    BottomSheetContent.HIGHLIGHT_PICKER -> HighlightPickerSheet(
                        currentColor = uiState.selectedVerse?.let { uiState.highlights[it.verse]?.color },
                        onColorSelected = { color ->
                            uiState.selectedVerse?.let { viewModel.toggleHighlight(it.verse, color) }
                            scope.launch { sheetState.hide() }
                        },
                        onRemove = {
                            uiState.selectedVerse?.let { viewModel.toggleHighlight(it.verse, HighlightColor.YELLOW) }
                        }
                    )
                    BottomSheetContent.NOTE_EDITOR -> NoteEditorSheet(
                        verse = uiState.selectedVerse,
                        existingNote = uiState.selectedVerse?.let {
                            uiState.notes[it.verse]?.firstOrNull()
                        },
                        onSave = { content ->
                            uiState.selectedVerse?.let { viewModel.saveNote(it.verse, content) }
                            scope.launch { sheetState.hide() }
                        },
                        onDismiss = { viewModel.dismissBottomSheet() }
                    )
                    BottomSheetContent.AI_INSIGHT -> AiInsightSheet(
                        verse = uiState.selectedVerse,
                        aiState = uiState.aiInsight,
                        onLoadInsight = { type -> viewModel.loadAiInsight(type) },
                        onLoadPassageGuide = { viewModel.loadPassageAiInsight() }
                    )
                    BottomSheetContent.TTS_CONTROLS -> TtsControlsSheet(
                        ttsState = uiState.ttsState,
                        onPlayPause = {
                            if (uiState.ttsState.isPlaying) viewModel.pauseTts()
                            else viewModel.readChapter()
                        },
                        onStop = { viewModel.stopTts() },
                        onSpeedChange = { viewModel.setTtsSpeed(it) },
                        onPitchChange = { viewModel.setTtsPitch(it) }
                    )
                    BottomSheetContent.SHARE_OPTIONS -> ShareSheet(
                        verse = uiState.selectedVerse,
                        onDismiss = { viewModel.dismissBottomSheet() }
                    )
                    else -> {}
                }
            }
        }
    }
}

// ── Chapter Content ───────────────────────────────────────────────────────────

@Composable
private fun ChapterView(
    verses: List<BibleVerse>,
    highlights: Map<Int, Highlight>,
    notes: Map<Int, List<BibleNote>>,
    bookmarks: Set<Int>,
    settings: AppSettings,
    ttsState: com.openbiblescholar.services.tts.TtsPlaybackState,
    onVerseTap: (BibleVerse) -> Unit,
    onWordTap: (String, String?) -> Unit
) {
    val listState = rememberLazyListState()
    val fontSize = settings.fontSize.sp

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        // Chapter heading
        item {
            Text(
                text = "${verses.firstOrNull()?.book ?: ""} ${verses.firstOrNull()?.chapter ?: ""}",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                textAlign = TextAlign.Center
            )
        }

        items(verses, key = { it.verse }) { verse ->
            val highlight = highlights[verse.verse]
            val hasNote = notes[verse.verse]?.isNotEmpty() == true
            val isBookmarked = bookmarks.contains(verse.verse)
            val isSpeaking = ttsState.isSpeaking &&
                ttsState.currentUtteranceId.contains("${verse.book}_${verse.chapter}_${verse.verse}")

            VerseItem(
                verse = verse,
                highlight = highlight,
                hasNote = hasNote,
                isBookmarked = isBookmarked,
                isSpeaking = isSpeaking,
                showVerseNumbers = settings.showVerseNumbers,
                fontSize = fontSize,
                onTap = { onVerseTap(verse) },
                onWordTap = onWordTap
            )
        }

        item { Spacer(modifier = Modifier.height(100.dp)) }
    }
}

@Composable
private fun VerseItem(
    verse: BibleVerse,
    highlight: Highlight?,
    hasNote: Boolean,
    isBookmarked: Boolean,
    isSpeaking: Boolean,
    showVerseNumbers: Boolean,
    fontSize: TextUnit,
    onTap: () -> Unit,
    onWordTap: (String, String?) -> Unit
) {
    val bgColor = when {
        isSpeaking -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        highlight != null -> Color(android.graphics.Color.parseColor(highlight.color)).copy(alpha = 0.3f)
        else -> Color.Transparent
    }

    val animatedBg by animateColorAsState(
        targetValue = bgColor,
        animationSpec = tween(300),
        label = "verseBg"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(animatedBg)
            .clickable(onClick = onTap)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Verse indicators column
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(36.dp)
        ) {
            if (showVerseNumbers) {
                Text(
                    text = verse.verse.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            if (isBookmarked) {
                Icon(
                    Icons.Filled.Bookmark,
                    contentDescription = "Bookmarked",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp)
                )
            }
            if (hasNote) {
                Icon(
                    Icons.Outlined.StickyNote2,
                    contentDescription = "Has note",
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(12.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Verse text
        Text(
            text = verse.text,
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = fontSize),
            color = MaterialTheme.colorScheme.onBackground,
            lineHeight = (fontSize.value * 1.7f).sp,
            modifier = Modifier.weight(1f)
        )

        // Speaking indicator
        if (isSpeaking) {
            Box(
                modifier = Modifier
                    .padding(start = 4.dp, top = 6.dp)
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
    }
}

@Composable
private fun EmptyChapterView(book: String, chapter: Int) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Outlined.MenuBook,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "$book $chapter",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            "This translation hasn't been downloaded yet.\nGo to Library to download it.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp)
        )
    }
}
