package com.openbiblescholar.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.openbiblescholar.data.db.Converters

// ─── Bible Verse Cache ────────────────────────────────────────────────────────
@Entity(
    tableName = "bible_verses",
    indices = [
        Index(value = ["translation", "book", "chapter", "verse"], unique = true),
        Index(value = ["book", "chapter"])
    ]
)
data class VerseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val translation: String,
    val book: String,
    @ColumnInfo(name = "book_order") val bookOrder: Int,
    val chapter: Int,
    val verse: Int,
    val text: String,
    @ColumnInfo(name = "text_plain") val textPlain: String = text,
    @ColumnInfo(name = "strongs_json") val strongsJson: String = "[]"   // serialized VerseWords
)

// ─── Highlights ───────────────────────────────────────────────────────────────
@Entity(
    tableName = "highlights",
    indices = [Index(value = ["book", "chapter", "verse"])]
)
data class HighlightEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val book: String,
    val chapter: Int,
    val verse: Int,
    val color: String,   // hex string
    val tag: String = "",
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)

// ─── Notes ────────────────────────────────────────────────────────────────────
@Entity(
    tableName = "notes",
    indices = [Index(value = ["book", "chapter", "verse"])]
)
@TypeConverters(Converters::class)
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val book: String,
    val chapter: Int,
    val verse: Int,
    val content: String,
    @ColumnInfo(name = "is_private") val isPrivate: Boolean = true,
    @ColumnInfo(name = "tags_json") val tagsJson: String = "[]",
    @ColumnInfo(name = "linked_verses_json") val linkedVersesJson: String = "[]",
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis()
)

// ─── Bookmarks ────────────────────────────────────────────────────────────────
@Entity(
    tableName = "bookmarks",
    indices = [Index(value = ["book", "chapter", "verse"], unique = true)]
)
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val book: String,
    val chapter: Int,
    val verse: Int,
    val label: String = "",
    val color: String = "#C8922A",
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)

// ─── Downloaded Modules ────────────────────────────────────────────────────────
@Entity(tableName = "sword_modules")
data class SwordModuleEntity(
    @PrimaryKey val name: String,
    val description: String,
    @ColumnInfo(name = "module_type") val moduleType: String,
    val language: String,
    val version: String,
    val license: String,
    @ColumnInfo(name = "is_downloaded") val isDownloaded: Boolean = false,
    @ColumnInfo(name = "download_size_mb") val downloadSizeMb: Float = 0f,
    @ColumnInfo(name = "installed_size_mb") val installedSizeMb: Float = 0f,
    @ColumnInfo(name = "is_public_domain") val isPublicDomain: Boolean = true,
    @ColumnInfo(name = "repo_url") val repoUrl: String = "",
    @ColumnInfo(name = "installed_at") val installedAt: Long? = null
)

// ─── AI Insight Cache ─────────────────────────────────────────────────────────
@Entity(
    tableName = "ai_insights",
    indices = [Index(value = ["book", "chapter", "verse", "insight_type"])]
)
data class AiInsightEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val book: String,
    val chapter: Int,
    val verse: Int,
    @ColumnInfo(name = "insight_type") val insightType: String,
    val content: String,
    @ColumnInfo(name = "model_used") val modelUsed: String,
    val timestamp: Long = System.currentTimeMillis()
)

// ─── Reading Plan Progress ─────────────────────────────────────────────────────
@Entity(tableName = "reading_plan_progress")
@TypeConverters(Converters::class)
data class ReadingPlanProgressEntity(
    @PrimaryKey val planId: String,
    @ColumnInfo(name = "current_day") val currentDay: Int = 1,
    @ColumnInfo(name = "started_at") val startedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "completed_days_json") val completedDaysJson: String = "[]",
    @ColumnInfo(name = "reminder_time") val reminderTime: String = "08:00",
    @ColumnInfo(name = "is_active") val isActive: Boolean = true
)

// ─── App Settings ─────────────────────────────────────────────────────────────
@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey val id: Int = 1,   // singleton row
    @ColumnInfo(name = "font_size") val fontSize: Float = 18f,
    @ColumnInfo(name = "font_family") val fontFamily: String = "SERIF",
    @ColumnInfo(name = "reader_theme") val readerTheme: String = "light",
    @ColumnInfo(name = "show_verse_numbers") val showVerseNumbers: Boolean = true,
    @ColumnInfo(name = "show_headings") val showHeadings: Boolean = true,
    @ColumnInfo(name = "paragraph_mode") val paragraphMode: Boolean = false,
    @ColumnInfo(name = "default_translation") val defaultTranslation: String = "KJV",
    @ColumnInfo(name = "parallel_translation") val parallelTranslation: String = "",
    @ColumnInfo(name = "tts_speed") val ttsSpeed: Float = 1.0f,
    @ColumnInfo(name = "tts_pitch") val ttsPitch: Float = 1.0f,
    @ColumnInfo(name = "tts_voice") val ttsVoice: String = "",
    @ColumnInfo(name = "show_strongs_on_tap") val showStrongsOnTap: Boolean = true,
    @ColumnInfo(name = "ai_enabled") val aiEnabled: Boolean = false
)
