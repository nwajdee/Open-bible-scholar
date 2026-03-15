package com.openbiblescholar.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openbiblescholar.data.model.*
import com.openbiblescholar.data.repository.BibleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class HomeUiState(
    val dailyVerse: BibleVerse? = null,
    val lastRead: VerseRef? = null,
    val recentBookmarks: List<Bookmark> = emptyList(),
    val isLoading: Boolean = true
)

// Curated daily verses (one per day of year, cycling)
val DAILY_VERSES = listOf(
    Triple("John", 3, 16),
    Triple("Psalms", 23, 1),
    Triple("Proverbs", 3, 5),
    Triple("Philippians", 4, 13),
    Triple("Romans", 8, 28),
    Triple("Isaiah", 40, 31),
    Triple("Joshua", 1, 9),
    Triple("Jeremiah", 29, 11),
    Triple("Matthew", 6, 33),
    Triple("2 Timothy", 1, 7),
    Triple("Hebrews", 11, 1),
    Triple("James", 1, 5),
    Triple("1 Corinthians", 13, 4),
    Triple("Ephesians", 2, 8),
    Triple("Romans", 12, 2),
    Triple("Galatians", 5, 22),
    Triple("Psalms", 46, 1),
    Triple("Isaiah", 41, 10),
    Triple("Matthew", 11, 28),
    Triple("John", 14, 6),
    Triple("Psalms", 119, 105),
    Triple("Proverbs", 22, 6),
    Triple("2 Corinthians", 5, 17),
    Triple("Colossians", 3, 23),
    Triple("1 Peter", 5, 7),
    Triple("Philippians", 4, 6),
    Triple("Romans", 10, 17),
    Triple("Hebrews", 4, 12),
    Triple("John", 15, 5),
    Triple("Psalms", 27, 1),
    Triple("Lamentations", 3, 22)
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val bibleRepo: BibleRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadDailyVerse()
        observeBookmarks()
    }

    private fun loadDailyVerse() {
        viewModelScope.launch {
            val settings = bibleRepo.getSettings().first()
            val dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
            val (book, chapter, verse) = DAILY_VERSES[(dayOfYear - 1) % DAILY_VERSES.size]
            val v = bibleRepo.getVerse(settings.defaultTranslation, book, chapter, verse)
            _uiState.update { it.copy(dailyVerse = v, isLoading = false) }
        }
    }

    private fun observeBookmarks() {
        bibleRepo.getAllBookmarks().onEach { bookmarks ->
            _uiState.update { it.copy(recentBookmarks = bookmarks.take(5)) }
        }.launchIn(viewModelScope)
    }
}
