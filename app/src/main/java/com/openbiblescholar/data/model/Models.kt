package com.openbiblescholar.data.model

import androidx.compose.ui.graphics.Color

// ─── Bible Content ───────────────────────────────────────────────────────────

data class BibleBook(
    val id: String,            // e.g. "GEN", "MAT"
    val name: String,          // "Genesis"
    val abbreviation: String,  // "Gen"
    val testament: Testament,
    val chapterCount: Int,
    val category: BookCategory
)

enum class Testament { OLD, NEW }

enum class BookCategory {
    LAW, HISTORY, POETRY, MAJOR_PROPHETS, MINOR_PROPHETS,
    GOSPELS, ACTS, PAULINE_EPISTLES, GENERAL_EPISTLES, REVELATION
}

data class BibleVerse(
    val book: String,
    val chapter: Int,
    val verse: Int,
    val text: String,
    val translation: String,
    val words: List<VerseWord> = emptyList()   // for Strong's word linking
)

data class VerseWord(
    val word: String,
    val strongs: String?,        // e.g. "H1254" or "G3056"
    val lemma: String?,
    val morphology: String?,
    val transliteration: String?,
    val gloss: String?,
    val startIndex: Int,
    val endIndex: Int
)

data class BibleChapter(
    val book: String,
    val chapter: Int,
    val translation: String,
    val verses: List<BibleVerse>,
    val headings: Map<Int, String> = emptyMap()   // verse -> section heading
)

data class VerseRef(
    val book: String,
    val chapter: Int,
    val verse: Int
) {
    override fun toString() = "$book $chapter:$verse"

    companion object {
        fun parse(ref: String): VerseRef? {
            return try {
                val parts = ref.trim().split(" ")
                val book = parts.dropLast(1).joinToString(" ")
                val cv = parts.last().split(":")
                VerseRef(book, cv[0].toInt(), cv[1].toInt())
            } catch (e: Exception) { null }
        }
    }
}

// ─── SWORD Modules ───────────────────────────────────────────────────────────

data class SwordModule(
    val name: String,
    val description: String,
    val moduleType: ModuleType,
    val language: String,
    val version: String,
    val license: String,
    val isDownloaded: Boolean = false,
    val downloadSizeMb: Float = 0f,
    val installedSizeMb: Float = 0f,
    val isPublicDomain: Boolean = true,
    val repoUrl: String = ""
)

enum class ModuleType {
    BIBLE, COMMENTARY, DICTIONARY, LEXICON, DEVOTIONAL,
    CHURCH_FATHERS, GENERAL_BOOK, MAPS
}

// ─── Notes & Highlights ──────────────────────────────────────────────────────

data class Highlight(
    val id: Long = 0,
    val book: String,
    val chapter: Int,
    val verse: Int,
    val color: HighlightColor,
    val tag: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

enum class HighlightColor(val hex: String, val label: String) {
    YELLOW("#FFEE58", "Yellow"),
    GREEN("#A5D6A7", "Green"),
    BLUE("#90CAF9", "Blue"),
    PINK("#F48FB1", "Pink"),
    PURPLE("#CE93D8", "Purple"),
    ORANGE("#FFCC80", "Orange")
}

data class BibleNote(
    val id: Long = 0,
    val book: String,
    val chapter: Int,
    val verse: Int,
    val content: String,          // Rich text (HTML or Markdown)
    val isPrivate: Boolean = true,
    val tags: List<String> = emptyList(),
    val linkedVerses: List<VerseRef> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class Bookmark(
    val id: Long = 0,
    val book: String,
    val chapter: Int,
    val verse: Int,
    val label: String = "",
    val color: String = "#C8922A",
    val createdAt: Long = System.currentTimeMillis()
)

// ─── AI Study ────────────────────────────────────────────────────────────────

data class AiInsight(
    val verseRef: VerseRef,
    val type: AiInsightType,
    val content: String,
    val modelUsed: String,
    val timestamp: Long = System.currentTimeMillis()
)

enum class AiInsightType {
    CONTEXTUAL_EXPLANATION,
    CROSS_REFERENCES,
    WORD_STUDY,
    HISTORICAL_BACKGROUND,
    THEOLOGICAL_THEMES,
    APPLICATION,
    PASSAGE_GUIDE,
    DEVOTIONAL,
    SERMON_OUTLINE
}

data class AiConfig(
    val provider: AiProvider = AiProvider.GROQ,
    val apiKey: String = "",
    val model: String = "llama3-8b-8192",
    val temperature: Float = 0.3f,
    val maxTokens: Int = 1000,
    val streamingEnabled: Boolean = true
)

enum class AiProvider(val displayName: String, val baseUrl: String) {
    GROQ("Groq (Free)", "https://api.groq.com/openai/v1/"),
    OPENROUTER("OpenRouter (Free tier)", "https://openrouter.ai/api/v1/"),
    OLLAMA("Ollama (Local)", "http://localhost:11434/v1/"),
    CUSTOM("Custom Endpoint", "")
}

// ─── Reading Plans ────────────────────────────────────────────────────────────

data class ReadingPlan(
    val id: String,
    val name: String,
    val description: String,
    val durationDays: Int,
    val type: PlanType,
    val dailyReadings: List<DailyReading>
)

data class DailyReading(
    val day: Int,
    val passages: List<PassageRange>,
    val theme: String = "",
    val reflection: String = ""
)

data class PassageRange(
    val book: String,
    val startChapter: Int,
    val endChapter: Int,
    val startVerse: Int = 1,
    val endVerse: Int = -1   // -1 = end of chapter
)

enum class PlanType {
    WHOLE_BIBLE, NEW_TESTAMENT, PSALMS_PROVERBS,
    GOSPELS, THEMATIC, CHRONOLOGICAL
}

data class UserPlanProgress(
    val planId: String,
    val currentDay: Int,
    val startedAt: Long,
    val completedDays: Set<Int>,
    val reminderTime: String = "08:00",
    val isActive: Boolean = true
)

// ─── Strong's Lexicon ─────────────────────────────────────────────────────────

data class StrongsEntry(
    val number: String,       // "H1254" or "G3056"
    val word: String,         // Original Hebrew/Greek
    val transliteration: String,
    val pronunciation: String,
    val definition: String,
    val origin: String,
    val usageNotes: String,
    val kjvUsage: String,
    val occurrences: Int
)

// ─── Commentary ───────────────────────────────────────────────────────────────

data class CommentaryEntry(
    val moduleName: String,
    val book: String,
    val chapter: Int,
    val verse: Int,
    val text: String
)

// ─── TTS State ────────────────────────────────────────────────────────────────

data class TtsState(
    val isPlaying: Boolean = false,
    val isPaused: Boolean = false,
    val currentVerse: VerseRef? = null,
    val currentWordIndex: Int = -1,
    val speed: Float = 1.0f,
    val pitch: Float = 1.0f,
    val voiceId: String = ""
)

// ─── App Settings ─────────────────────────────────────────────────────────────

data class AppSettings(
    val fontSize: Float = 18f,
    val fontFamily: ReaderFont = ReaderFont.SERIF,
    val readerTheme: String = "light",
    val showVerseNumbers: Boolean = true,
    val showSectionHeadings: Boolean = true,
    val paragraphMode: Boolean = false,
    val defaultTranslation: String = "KJV",
    val parallelTranslation: String = "",
    val ttsSpeed: Float = 1.0f,
    val ttsPitch: Float = 1.0f,
    val ttsVoice: String = "",
    val showStrongsOnTap: Boolean = true,
    val aiEnabled: Boolean = false
)

enum class ReaderFont { SERIF, SANS_SERIF, DYSLEXIC }
