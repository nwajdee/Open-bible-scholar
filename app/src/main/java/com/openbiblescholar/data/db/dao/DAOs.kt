package com.openbiblescholar.data.db.dao

import androidx.room.*
import com.openbiblescholar.data.db.entity.*
import kotlinx.coroutines.flow.Flow

// ─── Verse DAO ────────────────────────────────────────────────────────────────
@Dao
interface VerseDao {
    @Query("SELECT * FROM bible_verses WHERE translation=:trans AND book=:book AND chapter=:chapter ORDER BY verse ASC")
    fun getChapter(trans: String, book: String, chapter: Int): Flow<List<VerseEntity>>

    @Query("SELECT * FROM bible_verses WHERE translation=:trans AND book=:book AND chapter=:chapter AND verse=:verse LIMIT 1")
    suspend fun getVerse(trans: String, book: String, chapter: Int, verse: Int): VerseEntity?

    @Query("SELECT DISTINCT book, book_order FROM bible_verses WHERE translation=:trans ORDER BY book_order ASC")
    suspend fun getBooks(trans: String): List<BookRow>

    @Query("SELECT MAX(chapter) FROM bible_verses WHERE translation=:trans AND book=:book")
    suspend fun getChapterCount(trans: String, book: String): Int

    @Query("SELECT MAX(verse) FROM bible_verses WHERE translation=:trans AND book=:book AND chapter=:chapter")
    suspend fun getVerseCount(trans: String, book: String, chapter: Int): Int

    @Query("SELECT * FROM bible_verses WHERE translation=:trans AND text_plain LIKE '%' || :query || '%' LIMIT 200")
    suspend fun searchVerses(trans: String, query: String): List<VerseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVerses(verses: List<VerseEntity>)

    @Query("DELETE FROM bible_verses WHERE translation=:trans")
    suspend fun deleteTranslation(trans: String)

    data class BookRow(val book: String, @ColumnInfo(name = "book_order") val bookOrder: Int)
}

// ─── Highlight DAO ─────────────────────────────────────────────────────────────
@Dao
interface HighlightDao {
    @Query("SELECT * FROM highlights WHERE book=:book AND chapter=:chapter")
    fun getHighlightsForChapter(book: String, chapter: Int): Flow<List<HighlightEntity>>

    @Query("SELECT * FROM highlights ORDER BY created_at DESC")
    fun getAllHighlights(): Flow<List<HighlightEntity>>

    @Query("SELECT * FROM highlights WHERE book=:book AND chapter=:chapter AND verse=:verse LIMIT 1")
    suspend fun getHighlightForVerse(book: String, chapter: Int, verse: Int): HighlightEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(highlight: HighlightEntity): Long

    @Delete
    suspend fun delete(highlight: HighlightEntity)

    @Query("DELETE FROM highlights WHERE book=:book AND chapter=:chapter AND verse=:verse")
    suspend fun deleteForVerse(book: String, chapter: Int, verse: Int)
}

// ─── Note DAO ─────────────────────────────────────────────────────────────────
@Dao
interface NoteDao {
    @Query("SELECT * FROM notes WHERE book=:book AND chapter=:chapter ORDER BY verse ASC")
    fun getNotesForChapter(book: String, chapter: Int): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE book=:book AND chapter=:chapter AND verse=:verse ORDER BY updated_at DESC")
    fun getNotesForVerse(book: String, chapter: Int, verse: Int): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes ORDER BY updated_at DESC")
    fun getAllNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE content LIKE '%' || :query || '%' ORDER BY updated_at DESC")
    suspend fun searchNotes(query: String): List<NoteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(note: NoteEntity): Long

    @Delete
    suspend fun delete(note: NoteEntity)

    @Query("DELETE FROM notes WHERE id=:id")
    suspend fun deleteById(id: Long)
}

// ─── Bookmark DAO ─────────────────────────────────────────────────────────────
@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks ORDER BY created_at DESC")
    fun getAllBookmarks(): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks WHERE book=:book AND chapter=:chapter AND verse=:verse LIMIT 1")
    suspend fun getBookmark(book: String, chapter: Int, verse: Int): BookmarkEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(bookmark: BookmarkEntity): Long

    @Delete
    suspend fun delete(bookmark: BookmarkEntity)

    @Query("DELETE FROM bookmarks WHERE book=:book AND chapter=:chapter AND verse=:verse")
    suspend fun deleteForVerse(book: String, chapter: Int, verse: Int)
}

// ─── Sword Module DAO ─────────────────────────────────────────────────────────
@Dao
interface SwordModuleDao {
    @Query("SELECT * FROM sword_modules ORDER BY module_type, name ASC")
    fun getAllModules(): Flow<List<SwordModuleEntity>>

    @Query("SELECT * FROM sword_modules WHERE is_downloaded=1 ORDER BY name ASC")
    fun getDownloadedModules(): Flow<List<SwordModuleEntity>>

    @Query("SELECT * FROM sword_modules WHERE module_type=:type ORDER BY name ASC")
    fun getModulesByType(type: String): Flow<List<SwordModuleEntity>>

    @Query("SELECT * FROM sword_modules WHERE name=:name LIMIT 1")
    suspend fun getModule(name: String): SwordModuleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(module: SwordModuleEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(modules: List<SwordModuleEntity>)
}

// ─── AI Insight DAO ───────────────────────────────────────────────────────────
@Dao
interface AiInsightDao {
    @Query("SELECT * FROM ai_insights WHERE book=:book AND chapter=:chapter AND verse=:verse AND insight_type=:type LIMIT 1")
    suspend fun getInsight(book: String, chapter: Int, verse: Int, type: String): AiInsightEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(insight: AiInsightEntity)

    @Query("DELETE FROM ai_insights WHERE timestamp < :before")
    suspend fun pruneOld(before: Long)
}

// ─── Reading Plan DAO ─────────────────────────────────────────────────────────
@Dao
interface ReadingPlanDao {
    @Query("SELECT * FROM reading_plan_progress WHERE is_active=1 ORDER BY started_at DESC LIMIT 1")
    fun getActivePlan(): Flow<ReadingPlanProgressEntity?>

    @Query("SELECT * FROM reading_plan_progress WHERE plan_id=:planId LIMIT 1")
    suspend fun getPlan(planId: String): ReadingPlanProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(progress: ReadingPlanProgressEntity)
}

// ─── Settings DAO ─────────────────────────────────────────────────────────────
@Dao
interface SettingsDao {
    @Query("SELECT * FROM app_settings WHERE id=1 LIMIT 1")
    fun getSettings(): Flow<AppSettingsEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(settings: AppSettingsEntity)
}
