package com.openbiblescholar.ui.screens.study

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openbiblescholar.data.model.StrongsEntry
import com.openbiblescholar.data.repository.BibleRepository
import com.openbiblescholar.di.ApiKeyStore
import com.openbiblescholar.services.ai.AiStudyService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// ─── Word Study ───────────────────────────────────────────────────────────────

data class WordStudyUiState(
    val word: String = "",
    val strongsNumber: String = "",
    val entry: StrongsEntry? = null,
    val isLoading: Boolean = false,
    val aiContent: String = "",
    val isLoadingAi: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class WordStudyViewModel @Inject constructor(
    private val bibleRepo: BibleRepository,
    private val aiService: AiStudyService,
    private val apiKeyStore: ApiKeyStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(WordStudyUiState())
    val uiState: StateFlow<WordStudyUiState> = _uiState.asStateFlow()

    fun loadWordStudy(word: String, strongsNumber: String) {
        _uiState.update { it.copy(word = word, strongsNumber = strongsNumber, isLoading = true) }

        viewModelScope.launch {
            // In production: query StrongsGreek/StrongsHebrew SWORD module SQLite DB
            val stubEntry = buildStubEntry(word, strongsNumber)
            _uiState.update { it.copy(entry = stubEntry, isLoading = false) }
        }
    }

    fun loadAiWordStudy() {
        val state = _uiState.value
        _uiState.update { it.copy(isLoadingAi = true) }
        viewModelScope.launch {
            val config = apiKeyStore.getAiConfig()
            val result = aiService.askAi(
                config,
                "You are a Hebrew and Greek Biblical language expert.",
                "Provide a thorough word study for ${state.word} (Strong's ${state.strongsNumber}). " +
                "Cover etymology, semantic range, usage in both testaments, and theological significance."
            )
            result.fold(
                onSuccess = { _uiState.update { s -> s.copy(aiContent = it, isLoadingAi = false) } },
                onFailure = { _uiState.update { s -> s.copy(error = it.message, isLoadingAi = false) } }
            )
        }
    }

    private fun buildStubEntry(word: String, strongs: String): StrongsEntry {
        val isGreek = strongs.startsWith("G")
        return StrongsEntry(
            number = strongs,
            word = word,
            transliteration = if (isGreek) "logos" else "davar",
            pronunciation = if (isGreek) "log'-os" else "daw-var'",
            definition = if (isGreek)
                "something said (including the thought); by impl. a topic (subject of discourse), also reasoning (the mental faculty) or motive"
            else
                "a word; by impl. a matter (as spoken of) or thing; adverbially a cause",
            origin = if (isGreek) "from lego (G3004)" else "from dabar (H1696)",
            usageNotes = "Used throughout the New Testament to refer to Christ as the divine Word (John 1:1).",
            kjvUsage = if (isGreek) "word (218x), saying (50x), account (8x), speech (8x), Word (7x)" else "word (807x), thing (231x), matter (63x)",
            occurrences = if (isGreek) 330 else 1440
        )
    }
}

// ─── Passage Guide ────────────────────────────────────────────────────────────

data class PassageGuideUiState(
    val book: String = "",
    val chapter: Int = 0,
    val guide: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class PassageGuideViewModel @Inject constructor(
    private val bibleRepo: BibleRepository,
    private val aiService: AiStudyService,
    private val apiKeyStore: ApiKeyStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(PassageGuideUiState())
    val uiState: StateFlow<PassageGuideUiState> = _uiState.asStateFlow()

    fun load(book: String, chapter: Int) {
        _uiState.update { it.copy(book = book, chapter = chapter) }
    }

    fun generateGuide() {
        val state = _uiState.value
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val settings = bibleRepo.getSettings().first()
            val verses = bibleRepo.searchVerses(settings.defaultTranslation, "").take(0) // placeholder
            val config = apiKeyStore.getAiConfig()
            val result = aiService.getPassageGuide(config, state.book, state.chapter, emptyList())
            result.fold(
                onSuccess = { guide -> _uiState.update { it.copy(guide = guide, isLoading = false) } },
                onFailure = { err -> _uiState.update { it.copy(error = err.message, isLoading = false) } }
            )
        }
    }
}
