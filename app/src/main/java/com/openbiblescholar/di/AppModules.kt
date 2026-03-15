package com.openbiblescholar.di

import android.content.Context
import androidx.room.Room
import com.openbiblescholar.data.db.OpenBibleDatabase
import com.openbiblescholar.data.db.dao.*
import com.openbiblescholar.data.repository.BibleRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): OpenBibleDatabase =
        Room.databaseBuilder(
            context,
            OpenBibleDatabase::class.java,
            OpenBibleDatabase.DATABASE_NAME
        )
        .fallbackToDestructiveMigration()
        .build()

    @Provides fun provideVerseDao(db: OpenBibleDatabase): VerseDao = db.verseDao()
    @Provides fun provideHighlightDao(db: OpenBibleDatabase): HighlightDao = db.highlightDao()
    @Provides fun provideNoteDao(db: OpenBibleDatabase): NoteDao = db.noteDao()
    @Provides fun provideBookmarkDao(db: OpenBibleDatabase): BookmarkDao = db.bookmarkDao()
    @Provides fun provideSwordModuleDao(db: OpenBibleDatabase): SwordModuleDao = db.swordModuleDao()
    @Provides fun provideAiInsightDao(db: OpenBibleDatabase): AiInsightDao = db.aiInsightDao()
    @Provides fun provideReadingPlanDao(db: OpenBibleDatabase): ReadingPlanDao = db.readingPlanDao()
    @Provides fun provideSettingsDao(db: OpenBibleDatabase): SettingsDao = db.settingsDao()
}

// ── Security / Keystore ───────────────────────────────────────────────────────

@Module
@InstallIn(SingletonComponent::class)
object SecurityModule {

    @Provides
    @Singleton
    fun provideApiKeyStore(@ApplicationContext context: Context): ApiKeyStore =
        ApiKeyStore(context)
}
