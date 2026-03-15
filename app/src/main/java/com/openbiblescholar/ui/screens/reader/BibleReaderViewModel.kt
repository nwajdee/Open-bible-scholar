package com.openbiblescholar.ui.screens.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openbiblescholar.data.model.*
import com.openbiblescholar.data.repository.BibleRepository
import com.openbiblescholar.di.ApiKeyStore
import com.openbiblescholar.services.ai.AiStudyService
import com.openbiblescholar.services.tts.BibleTtsService
import com.openbiblescholar.services.tts.TtsPlaybackState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReaderUiState(
    val currentBook: String = "Genesis",
    val currentChapter: Int = 1,
    val currentVerse: Int = 1,
    val translation: String = "KJV",
    val parallelTranslation: String = "",
    val verses: List<BibleVerse> = emptyList(),
    val parallelVerses: List<BibleVerse> = emptyList(),
    val highlights: Map<Int, Highlight> = emptyMap(),   // verse -> highlight
    val notes: Map<Int, List<BibleNote>> = emptyMap(),
    val bookmarks: Set<Int> = emptySet(),
    val books: List<BibleBook> = emptyList(),
    val chapterCount: Int = 1,
    val settings: AppSettings = AppSettings(),
    val selectedVerse: BibleVerse? = null,
    val isLoading: Boolean = true,
    val showBottomSheet: Boolean = false,
    val bottomSheetContent: BottomSheetContent = BottomSheetContent.VERSE_OPTIONS,
    val showParallel: Boolean = false,
    val splitMode: SplitMode = SplitMode.READER_ONLY,
    val aiInsight: AiInsightUiState = AiInsightUiState(),
    val ttsState: TtsPlaybackState = TtsPlaybackState()
)

data class AiInsightUiState(
    val content: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val type: AiInsightType? = null
)

enum class BottomSheetContent {
    VERSE_OPTIONS, HIGHLIGHT_PICKER, NOTE_EDITOR, COMMENTARY, STRONGS_POPUP,
    AI_INSIGHT, TTS_CONTROLS, SHARE_OPTIONS
}

enum class SplitMode { READER_ONLY, READER_COMMENTARY, READER_NOTES, PARALLEL_BIBLES }

@HiltViewModel
class BibleReaderViewModel @Inject constructor(
    private val bibleRepo: BibleRepository,
    private val aiService: AiStudyService,
    private val ttsService: BibleTtsService,
    private val apiKeyStore: ApiKeyStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReaderUiState())
    val uiState: StateFlow<ReaderUiState> = _uiState.asStateFlow()

    private var aiJob: Job? = null
    private var loadJob: Job? = null

    init {
        observeTtsState()
        observeSettings()
        bibleRepo.getAllBookmarks().onEach { bookmarks ->
            // Update bookmarks set on the current chapter
            val bookStr = _uiState.value.currentBook
            val ch = _uiState.value.currentChapter
            val versesInChapter = bookmarks
                .filter { it.book == bookStr && it.chapter == ch }
                .map { it.verse }.toSet()
            _uiState.update { it.copy(bookmarks = versesInChapter) }
        }.launchIn(viewModelScope)
    }

    private fun observeTtsState() {
        ttsService.state.onEach { ttsState ->
            _uiState.update { it.copy(ttsState = ttsState) }
        }.launchIn(viewModelScope)
    }

    private fun observeSettings() {
        bibleRepo.getSettings().onEach { settings ->
            _uiState.update { it.copy(settings = settings) }
        }.launchIn(viewModelScope)
    }

    fun initialize(book: String, chapter: Int, verse: Int) {
        viewModelScope.launch {
            val books = bibleRepo.getBooks(_uiState.value.translation)
            val chCount = bibleRepo.getChapterCount(_uiState.value.translation, book).coerceAtLeast(1)
            _uiState.update {
                it.copy(
                    currentBook = book, currentChapter = chapter, currentVerse = verse,
                    books = books, chapterCount = chCount, isLoading = true
                )
            }
            loadChapter(book, chapter)
        }
    }

    fun navigateToChapter(book: String, chapter: Int) {
        viewModelScope.launch {
            val chCount = bibleRepo.getChapterCount(_uiState.value.translation, book).coerceAtLeast(1)
            _uiState.update { it.copy(currentBook = book, currentChapter = chapter, chapterCount = chCount, isLoading = true) }
            loadChapter(book, chapter)
        }
    }

    fun nextChapter() {
        val state = _uiState.value
        val nextCh = state.currentChapter + 1
        if (nextCh <= state.chapterCount) {
            navigateToChapter(state.currentBook, nextCh)
        } else {
            // Go to next book
            val idx = state.books.indexOfFirst { it.name == state.currentBook }
            if (idx >= 0 && idx + 1 < state.books.size) {
                val nextBook = state.books[idx + 1]
                navigateToChapter(nextBook.name, 1)
            }
        }
    }

    fun previousChapter() {
        val state = _uiState.value
        val prevCh = state.currentChapter - 1
        if (prevCh >= 1) {
            navigateToChapter(state.currentBook, prevCh)
        } else {
            val idx = state.books.indexOfFirst { it.name == state.currentBook }
            if (idx > 0) {
                viewModelScope.launch {
                    val prevBook = state.books[idx - 1]
                    val prevChCount = bibleRepo.getChapterCount(state.translation, prevBook.name).coerceAtLeast(1)
                    navigateToChapter(prevBook.name, prevChCount)
                }
            }
        }
    }

    private fun loadChapter(book: String, chapter: Int) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val translation = _uiState.value.translation
            // Verses
            bibleRepo.getChapter(translation, book, chapter).collect { verses ->
                // Highlights for this chapter
                bibleRepo.getHighlightsForChapter(book, chapter).collect { highlights ->
                    val highlightMap = highlights.associateBy { it.verse }
                    // Notes
                    bibleRepo.getNotesForChapter(book, chapter).collect { notes ->
                        val notesMap = notes.groupBy { it.verse }
                        _uiState.update {
                            it.copy(
                                verses = verses,
                                highlights = highlightMap,
                                notes = notesMap,
                                isLoading = false
                            )
                        }
                    }
                }
            }
        }
    }

    fun selectVerse(verse: BibleVerse) {
        _uiState.update { it.copy(selectedVerse = verse, showBottomSheet = true, bottomSheetContent = BottomSheetContent.VERSE_OPTIONS) }
    }

    fun dismissBottomSheet() {
        _uiState.update { it.copy(showBottomSheet = false, selectedVerse = null) }
    }

    fun showBottomSheet(content: BottomSheetContent) {
        _uiState.update { it.copy(showBottomSheet = true, bottomSheetContent = content) }
    }

    fun toggleHighlight(verse: Int, color: HighlightColor) {
        val state = _uiState.value
        viewModelScope.launch {
            bibleRepo.toggleHighlight(state.currentBook, state.currentChapter, verse, color)
        }
    }

    fun toggleBookmark(verse: Int) {
        val state = _uiState.value
        viewModelScope.launch {
            bibleRepo.toggleBookmark(state.currentBook, state.currentChapter, verse)
        }
    }

    fun saveNote(verse: Int, content: String) {
        val state = _uiState.value
        viewModelScope.launch {
            bibleRepo.saveNote(
                BibleNote(
                    book = state.currentBook, chapter = state.currentChapter,
                    verse = verse, content = content
                )
            )
        }
    }

    fun changeTranslation(translation: String) {
        val state = _uiState.value
        _uiState.update { it.copy(translation = translation, isLoading = true) }
        viewModelScope.launch {
            val chCount = bibleRepo.getChapterCount(translation, state.currentBook).coerceAtLeast(1)
            _uiState.update { it.copy(chapterCount = chCount) }
            loadChapter(state.currentBook, state.currentChapter)
        }
    }

    // ── AI ────────────────────────────────────────────────────────────────────

    fun loadAiInsight(type: AiInsightType) {
        val verse = _uiState.value.selectedVerse ?: return
        val config = apiKeyStore.getAiConfig()

        aiJob?.cancel()
        _uiState.update { it.copy(aiInsight = AiInsightUiState(isLoading = true, type = type)) }

        aiJob = viewModelScope.launch {
            val result = when (type) {
                AiInsightType.CONTEXTUAL_EXPLANATION -> aiService.getVerseInsight(config, verse)
                AiInsightType.CROSS_REFERENCES -> aiService.getCrossReferences(config, verse)
                AiInsightType.HISTORICAL_BACKGROUND -> aiService.getHistoricalBackground(config, verse.book, verse.chapter)
                AiInsightType.DEVOTIONAL -> aiService.getDailyDevotional(config, verse)
                AiInsightType.SERMON_OUTLINE -> aiService.getSermonOutline(config, verse)
                else -> aiService.getVerseInsight(config, verse)
            }
            result.fold(
                onSuccess = { content ->
                    _uiState.update { it.copy(aiInsight = AiInsightUiState(content = content, type = type)) }
                },
                onFailure = { err ->
                    _uiState.update { it.copy(aiInsight = AiInsightUiState(error = err.message, type = type)) }
                }
            )
        }
    }

    fun loadPassageAiInsight() {
        val state = _uiState.value
        val config = apiKeyStore.getAiConfig()
        aiJob?.cancel()
        _uiState.update { it.copy(aiInsight = AiInsightUiState(isLoading = true, type = AiInsightType.PASSAGE_GUIDE)) }
        aiJob = viewModelScope.launch {
            val result = aiService.getPassageGuide(config, state.currentBook, state.currentChapter, state.verses)
            result.fold(
                onSuccess = { content ->
                    _uiState.update { it.copy(aiInsight = AiInsightUiState(content = content, type = AiInsightType.PASSAGE_GUIDE)) }
                },
                onFailure = { err ->
                    _uiState.update { it.copy(aiInsight = AiInsightUiState(error = err.message)) }
                }
            )
        }
    }

    // ── TTS ───────────────────────────────────────────────────────────────────

    fun readCurrentVerse() {
        val verse = _uiState.value.selectedVerse ?: _uiState.value.verses.firstOrNull() ?: return
        ttsService.initialize {
            ttsService.speak(
                "${verse.book} chapter ${verse.chapter} verse ${verse.verse}. ${verse.text}",
                "verse_${verse.book}_${verse.chapter}_${verse.verse}"
            )
        }
    }

    fun readChapter() {
        val verses = _uiState.value.verses
        if (verses.isEmpty()) return
        val items = verses.map { v ->
            "verse_${v.book}_${v.chapter}_${v.verse}" to "Verse ${v.verse}. ${v.text}"
        }
        ttsService.initialize {
            ttsService.speakQueue(items)
        }
    }

    fun pauseTts() = ttsService.pause()
    fun resumeTts() = ttsService.resume()
    fun stopTts() = ttsService.stop()
    fun setTtsSpeed(speed: Float) = ttsService.setSpeed(speed)
    fun setTtsPitch(pitch: Float) = ttsService.setPitch(pitch)

    fun updateFontSize(size: Float) {
        viewModelScope.launch {
            val newSettings = _uiState.value.settings.copy(fontSize = size)
            bibleRepo.saveSettings(newSettings)
        }
    }

    override fun onCleared() {
        super.onCleared()
        ttsService.stop()
    }
}
