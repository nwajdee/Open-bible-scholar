package com.openbiblescholar.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.openbiblescholar.data.db.dao.*
import com.openbiblescholar.data.db.entity.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

@Database(
    entities = [
        VerseEntity::class,
        HighlightEntity::class,
        NoteEntity::class,
        BookmarkEntity::class,
        SwordModuleEntity::class,
        AiInsightEntity::class,
        ReadingPlanProgressEntity::class,
        AppSettingsEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class OpenBibleDatabase : RoomDatabase() {
    abstract fun verseDao(): VerseDao
    abstract fun highlightDao(): HighlightDao
    abstract fun noteDao(): NoteDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun swordModuleDao(): SwordModuleDao
    abstract fun aiInsightDao(): AiInsightDao
    abstract fun readingPlanDao(): ReadingPlanDao
    abstract fun settingsDao(): SettingsDao

    companion object {
        const val DATABASE_NAME = "open_bible_scholar.db"
    }
}

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromStringList(value: List<String>): String = gson.toJson(value)

    @TypeConverter
    fun toStringList(value: String): List<String> =
        gson.fromJson(value, object : TypeToken<List<String>>() {}.type) ?: emptyList()

    @TypeConverter
    fun fromIntSet(value: Set<Int>): String = gson.toJson(value)

    @TypeConverter
    fun toIntSet(value: String): Set<Int> =
        gson.fromJson(value, object : TypeToken<Set<Int>>() {}.type) ?: emptySet()
}
