package com.openbiblescholar.data.repository

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.openbiblescholar.data.db.dao.*
import com.openbiblescholar.data.db.entity.*
import com.openbiblescholar.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BibleRepository @Inject constructor(
    private val verseDao: VerseDao,
    private val highlightDao: HighlightDao,
    private val noteDao: NoteDao,
    private val bookmarkDao: BookmarkDao,
    private val settingsDao: SettingsDao
) {
    private val gson = Gson()

    // ── Reading ──────────────────────────────────────────────────────────────

    fun getChapter(translation: String, book: String, chapter: Int): Flow<List<BibleVerse>> =
        verseDao.getChapter(translation, book, chapter).map { entities ->
            entities.map { it.toModel() }
        }

    suspend fun getVerse(translation: String, book: String, chapter: Int, verse: Int): BibleVerse? =
        withContext(Dispatchers.IO) {
            verseDao.getVerse(translation, book, chapter, verse)?.toModel()
        }

    suspend fun getBooks(translation: String): List<BibleBook> =
        withContext(Dispatchers.IO) {
            verseDao.getBooks(translation).mapNotNull { row ->
                BOOK_METADATA[row.book]
            }
        }

    suspend fun getChapterCount(translation: String, book: String): Int =
        withContext(Dispatchers.IO) {
            verseDao.getChapterCount(translation, book)
        }

    suspend fun getVerseCount(translation: String, book: String, chapter: Int): Int =
        withContext(Dispatchers.IO) {
            verseDao.getVerseCount(translation, book, chapter)
        }

    suspend fun searchVerses(translation: String, query: String): List<BibleVerse> =
        withContext(Dispatchers.IO) {
            verseDao.searchVerses(translation, query).map { it.toModel() }
        }

    // ── Highlights ───────────────────────────────────────────────────────────

    fun getHighlightsForChapter(book: String, chapter: Int): Flow<List<Highlight>> =
        highlightDao.getHighlightsForChapter(book, chapter).map { list ->
            list.map { it.toModel() }
        }

    fun getAllHighlights(): Flow<List<Highlight>> =
        highlightDao.getAllHighlights().map { list -> list.map { it.toModel() } }

    suspend fun toggleHighlight(book: String, chapter: Int, verse: Int, color: HighlightColor) {
        withContext(Dispatchers.IO) {
            val existing = highlightDao.getHighlightForVerse(book, chapter, verse)
            if (existing != null && existing.color == color.hex) {
                highlightDao.deleteForVerse(book, chapter, verse)
            } else {
                highlightDao.upsert(
                    HighlightEntity(
                        book = book, chapter = chapter, verse = verse,
                        color = color.hex
                    )
                )
            }
        }
    }

    suspend fun removeHighlight(book: String, chapter: Int, verse: Int) =
        withContext(Dispatchers.IO) {
            highlightDao.deleteForVerse(book, chapter, verse)
        }

    // ── Notes ────────────────────────────────────────────────────────────────

    fun getNotesForChapter(book: String, chapter: Int): Flow<List<BibleNote>> =
        noteDao.getNotesForChapter(book, chapter).map { list -> list.map { it.toModel() } }

    fun getNotesForVerse(book: String, chapter: Int, verse: Int): Flow<List<BibleNote>> =
        noteDao.getNotesForVerse(book, chapter, verse).map { list -> list.map { it.toModel() } }

    fun getAllNotes(): Flow<List<BibleNote>> =
        noteDao.getAllNotes().map { list -> list.map { it.toModel() } }

    suspend fun saveNote(note: BibleNote): Long =
        withContext(Dispatchers.IO) {
            noteDao.upsert(note.toEntity())
        }

    suspend fun deleteNote(id: Long) =
        withContext(Dispatchers.IO) {
            noteDao.deleteById(id)
        }

    suspend fun searchNotes(query: String): List<BibleNote> =
        withContext(Dispatchers.IO) {
            noteDao.searchNotes(query).map { it.toModel() }
        }

    // ── Bookmarks ────────────────────────────────────────────────────────────

    fun getAllBookmarks(): Flow<List<Bookmark>> =
        bookmarkDao.getAllBookmarks().map { list -> list.map { it.toModel() } }

    suspend fun toggleBookmark(book: String, chapter: Int, verse: Int, label: String = "") {
        withContext(Dispatchers.IO) {
            val existing = bookmarkDao.getBookmark(book, chapter, verse)
            if (existing != null) {
                bookmarkDao.deleteForVerse(book, chapter, verse)
            } else {
                bookmarkDao.upsert(BookmarkEntity(book = book, chapter = chapter, verse = verse, label = label))
            }
        }
    }

    suspend fun isBookmarked(book: String, chapter: Int, verse: Int): Boolean =
        withContext(Dispatchers.IO) {
            bookmarkDao.getBookmark(book, chapter, verse) != null
        }

    // ── Settings ─────────────────────────────────────────────────────────────

    fun getSettings(): Flow<AppSettings> =
        settingsDao.getSettings().map { it?.toModel() ?: AppSettings() }

    suspend fun saveSettings(settings: AppSettings) =
        withContext(Dispatchers.IO) {
            settingsDao.save(settings.toEntity())
        }

    // ── Entity/Model conversions ──────────────────────────────────────────────

    private fun VerseEntity.toModel(): BibleVerse {
        val words = try {
            val type = object : TypeToken<List<VerseWord>>() {}.type
            gson.fromJson<List<VerseWord>>(strongsJson, type) ?: emptyList()
        } catch (e: Exception) { emptyList() }
        return BibleVerse(book, chapter, verse, text, translation, words)
    }

    private fun HighlightEntity.toModel() = Highlight(id, book, chapter, verse,
        HighlightColor.values().find { it.hex == color } ?: HighlightColor.YELLOW, tag, createdAt)

    private fun NoteEntity.toModel(): BibleNote {
        val tags = try { gson.fromJson<List<String>>(tagsJson, object : TypeToken<List<String>>() {}.type) ?: emptyList() } catch (e: Exception) { emptyList() }
        val linked = try { gson.fromJson<List<VerseRef>>(linkedVersesJson, object : TypeToken<List<VerseRef>>() {}.type) ?: emptyList() } catch (e: Exception) { emptyList() }
        return BibleNote(id, book, chapter, verse, content, isPrivate, tags, linked, createdAt, updatedAt)
    }

    private fun BibleNote.toEntity() = NoteEntity(
        id = id, book = book, chapter = chapter, verse = verse, content = content,
        isPrivate = isPrivate,
        tagsJson = gson.toJson(tags),
        linkedVersesJson = gson.toJson(linkedVerses),
        createdAt = createdAt, updatedAt = System.currentTimeMillis()
    )

    private fun BookmarkEntity.toModel() = Bookmark(id, book, chapter, verse, label, color, createdAt)

    private fun AppSettingsEntity.toModel() = AppSettings(
        fontSize = fontSize,
        fontFamily = try { ReaderFont.valueOf(fontFamily) } catch (e: Exception) { ReaderFont.SERIF },
        readerTheme = readerTheme,
        showVerseNumbers = showVerseNumbers,
        showSectionHeadings = showHeadings,
        paragraphMode = paragraphMode,
        defaultTranslation = defaultTranslation,
        parallelTranslation = parallelTranslation,
        ttsSpeed = ttsSpeed,
        ttsPitch = ttsPitch,
        ttsVoice = ttsVoice,
        showStrongsOnTap = showStrongsOnTap,
        aiEnabled = aiEnabled
    )

    private fun AppSettings.toEntity() = AppSettingsEntity(
        fontSize = fontSize,
        fontFamily = fontFamily.name,
        readerTheme = readerTheme,
        showVerseNumbers = showVerseNumbers,
        showHeadings = showSectionHeadings,
        paragraphMode = paragraphMode,
        defaultTranslation = defaultTranslation,
        parallelTranslation = parallelTranslation,
        ttsSpeed = ttsSpeed,
        ttsPitch = ttsPitch,
        ttsVoice = ttsVoice,
        showStrongsOnTap = showStrongsOnTap,
        aiEnabled = aiEnabled
    )

    companion object {
        // Canonical book metadata keyed by SWORD-style abbreviation
        val BOOK_METADATA: Map<String, BibleBook> = mapOf(
            "Genesis" to BibleBook("GEN","Genesis","Gen",Testament.OLD,50,BookCategory.LAW),
            "Exodus" to BibleBook("EXO","Exodus","Exod",Testament.OLD,40,BookCategory.LAW),
            "Leviticus" to BibleBook("LEV","Leviticus","Lev",Testament.OLD,27,BookCategory.LAW),
            "Numbers" to BibleBook("NUM","Numbers","Num",Testament.OLD,36,BookCategory.LAW),
            "Deuteronomy" to BibleBook("DEU","Deuteronomy","Deut",Testament.OLD,34,BookCategory.LAW),
            "Joshua" to BibleBook("JOS","Joshua","Josh",Testament.OLD,24,BookCategory.HISTORY),
            "Judges" to BibleBook("JDG","Judges","Judg",Testament.OLD,21,BookCategory.HISTORY),
            "Ruth" to BibleBook("RUT","Ruth","Ruth",Testament.OLD,4,BookCategory.HISTORY),
            "1 Samuel" to BibleBook("1SA","1 Samuel","1 Sam",Testament.OLD,31,BookCategory.HISTORY),
            "2 Samuel" to BibleBook("2SA","2 Samuel","2 Sam",Testament.OLD,24,BookCategory.HISTORY),
            "1 Kings" to BibleBook("1KI","1 Kings","1 Kgs",Testament.OLD,22,BookCategory.HISTORY),
            "2 Kings" to BibleBook("2KI","2 Kings","2 Kgs",Testament.OLD,25,BookCategory.HISTORY),
            "1 Chronicles" to BibleBook("1CH","1 Chronicles","1 Chr",Testament.OLD,29,BookCategory.HISTORY),
            "2 Chronicles" to BibleBook("2CH","2 Chronicles","2 Chr",Testament.OLD,36,BookCategory.HISTORY),
            "Ezra" to BibleBook("EZR","Ezra","Ezra",Testament.OLD,10,BookCategory.HISTORY),
            "Nehemiah" to BibleBook("NEH","Nehemiah","Neh",Testament.OLD,13,BookCategory.HISTORY),
            "Esther" to BibleBook("EST","Esther","Esth",Testament.OLD,10,BookCategory.HISTORY),
            "Job" to BibleBook("JOB","Job","Job",Testament.OLD,42,BookCategory.POETRY),
            "Psalms" to BibleBook("PSA","Psalms","Ps",Testament.OLD,150,BookCategory.POETRY),
            "Proverbs" to BibleBook("PRO","Proverbs","Prov",Testament.OLD,31,BookCategory.POETRY),
            "Ecclesiastes" to BibleBook("ECC","Ecclesiastes","Eccl",Testament.OLD,12,BookCategory.POETRY),
            "Song of Solomon" to BibleBook("SNG","Song of Solomon","Song",Testament.OLD,8,BookCategory.POETRY),
            "Isaiah" to BibleBook("ISA","Isaiah","Isa",Testament.OLD,66,BookCategory.MAJOR_PROPHETS),
            "Jeremiah" to BibleBook("JER","Jeremiah","Jer",Testament.OLD,52,BookCategory.MAJOR_PROPHETS),
            "Lamentations" to BibleBook("LAM","Lamentations","Lam",Testament.OLD,5,BookCategory.MAJOR_PROPHETS),
            "Ezekiel" to BibleBook("EZK","Ezekiel","Ezek",Testament.OLD,48,BookCategory.MAJOR_PROPHETS),
            "Daniel" to BibleBook("DAN","Daniel","Dan",Testament.OLD,12,BookCategory.MAJOR_PROPHETS),
            "Hosea" to BibleBook("HOS","Hosea","Hos",Testament.OLD,14,BookCategory.MINOR_PROPHETS),
            "Joel" to BibleBook("JOL","Joel","Joel",Testament.OLD,3,BookCategory.MINOR_PROPHETS),
            "Amos" to BibleBook("AMO","Amos","Amos",Testament.OLD,9,BookCategory.MINOR_PROPHETS),
            "Obadiah" to BibleBook("OBA","Obadiah","Obad",Testament.OLD,1,BookCategory.MINOR_PROPHETS),
            "Jonah" to BibleBook("JNA","Jonah","Jonah",Testament.OLD,4,BookCategory.MINOR_PROPHETS),
            "Micah" to BibleBook("MIC","Micah","Mic",Testament.OLD,7,BookCategory.MINOR_PROPHETS),
            "Nahum" to BibleBook("NAM","Nahum","Nah",Testament.OLD,3,BookCategory.MINOR_PROPHETS),
            "Habakkuk" to BibleBook("HAB","Habakkuk","Hab",Testament.OLD,3,BookCategory.MINOR_PROPHETS),
            "Zephaniah" to BibleBook("ZEP","Zephaniah","Zeph",Testament.OLD,3,BookCategory.MINOR_PROPHETS),
            "Haggai" to BibleBook("HAG","Haggai","Hag",Testament.OLD,2,BookCategory.MINOR_PROPHETS),
            "Zechariah" to BibleBook("ZEC","Zechariah","Zech",Testament.OLD,14,BookCategory.MINOR_PROPHETS),
            "Malachi" to BibleBook("MAL","Malachi","Mal",Testament.OLD,4,BookCategory.MINOR_PROPHETS),
            "Matthew" to BibleBook("MAT","Matthew","Matt",Testament.NEW,28,BookCategory.GOSPELS),
            "Mark" to BibleBook("MRK","Mark","Mark",Testament.NEW,16,BookCategory.GOSPELS),
            "Luke" to BibleBook("LUK","Luke","Luke",Testament.NEW,24,BookCategory.GOSPELS),
            "John" to BibleBook("JHN","John","John",Testament.NEW,21,BookCategory.GOSPELS),
            "Acts" to BibleBook("ACT","Acts","Acts",Testament.NEW,28,BookCategory.ACTS),
            "Romans" to BibleBook("ROM","Romans","Rom",Testament.NEW,16,BookCategory.PAULINE_EPISTLES),
            "1 Corinthians" to BibleBook("1CO","1 Corinthians","1 Cor",Testament.NEW,16,BookCategory.PAULINE_EPISTLES),
            "2 Corinthians" to BibleBook("2CO","2 Corinthians","2 Cor",Testament.NEW,13,BookCategory.PAULINE_EPISTLES),
            "Galatians" to BibleBook("GAL","Galatians","Gal",Testament.NEW,6,BookCategory.PAULINE_EPISTLES),
            "Ephesians" to BibleBook("EPH","Ephesians","Eph",Testament.NEW,6,BookCategory.PAULINE_EPISTLES),
            "Philippians" to BibleBook("PHP","Philippians","Phil",Testament.NEW,4,BookCategory.PAULINE_EPISTLES),
            "Colossians" to BibleBook("COL","Colossians","Col",Testament.NEW,4,BookCategory.PAULINE_EPISTLES),
            "1 Thessalonians" to BibleBook("1TH","1 Thessalonians","1 Thess",Testament.NEW,5,BookCategory.PAULINE_EPISTLES),
            "2 Thessalonians" to BibleBook("2TH","2 Thessalonians","2 Thess",Testament.NEW,3,BookCategory.PAULINE_EPISTLES),
            "1 Timothy" to BibleBook("1TI","1 Timothy","1 Tim",Testament.NEW,6,BookCategory.PAULINE_EPISTLES),
            "2 Timothy" to BibleBook("2TI","2 Timothy","2 Tim",Testament.NEW,4,BookCategory.PAULINE_EPISTLES),
            "Titus" to BibleBook("TIT","Titus","Titus",Testament.NEW,3,BookCategory.PAULINE_EPISTLES),
            "Philemon" to BibleBook("PHM","Philemon","Philem",Testament.NEW,1,BookCategory.PAULINE_EPISTLES),
            "Hebrews" to BibleBook("HEB","Hebrews","Heb",Testament.NEW,13,BookCategory.GENERAL_EPISTLES),
            "James" to BibleBook("JAS","James","Jas",Testament.NEW,5,BookCategory.GENERAL_EPISTLES),
            "1 Peter" to BibleBook("1PE","1 Peter","1 Pet",Testament.NEW,5,BookCategory.GENERAL_EPISTLES),
            "2 Peter" to BibleBook("2PE","2 Peter","2 Pet",Testament.NEW,3,BookCategory.GENERAL_EPISTLES),
            "1 John" to BibleBook("1JN","1 John","1 John",Testament.NEW,5,BookCategory.GENERAL_EPISTLES),
            "2 John" to BibleBook("2JN","2 John","2 John",Testament.NEW,1,BookCategory.GENERAL_EPISTLES),
            "3 John" to BibleBook("3JN","3 John","3 John",Testament.NEW,1,BookCategory.GENERAL_EPISTLES),
            "Jude" to BibleBook("JUD","Jude","Jude",Testament.NEW,1,BookCategory.GENERAL_EPISTLES),
            "Revelation" to BibleBook("REV","Revelation","Rev",Testament.NEW,22,BookCategory.REVELATION)
        )
    }
}
