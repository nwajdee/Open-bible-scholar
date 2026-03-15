package com.openbiblescholar.services

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.openbiblescholar.data.model.BibleNote
import com.openbiblescholar.data.model.Bookmark
import com.openbiblescholar.data.model.Highlight
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExportService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    private val dateStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())

    fun exportNotesAsMarkdown(notes: List<BibleNote>): Intent {
        val content = buildMarkdownNotes(notes)
        val file = writeToFile(content, "notes_${dateStamp.format(Date())}.md")
        return buildShareIntent(file, "text/markdown")
    }

    fun exportHighlightsAsMarkdown(highlights: List<Highlight>): Intent {
        val sb = StringBuilder()
        sb.appendLine("# My Bible Highlights")
        sb.appendLine("Exported from OpenBible Scholar — ${sdf.format(Date())}")
        sb.appendLine()
        highlights.groupBy { it.color }.forEach { (color, list) ->
            sb.appendLine("## ${color.label} Highlights")
            list.forEach { h ->
                sb.appendLine("- **${h.book} ${h.chapter}:${h.verse}** — _(${sdf.format(Date(h.createdAt))})_")
            }
            sb.appendLine()
        }
        val file = writeToFile(sb.toString(), "highlights_${dateStamp.format(Date())}.md")
        return buildShareIntent(file, "text/markdown")
    }

    fun exportBookmarksAsMarkdown(bookmarks: List<Bookmark>): Intent {
        val sb = StringBuilder()
        sb.appendLine("# My Bible Bookmarks")
        sb.appendLine("Exported from OpenBible Scholar — ${sdf.format(Date())}")
        sb.appendLine()
        bookmarks.forEach { bm ->
            val label = if (bm.label.isNotBlank()) " — ${bm.label}" else ""
            sb.appendLine("- **${bm.book} ${bm.chapter}:${bm.verse}**$label _(${sdf.format(Date(bm.createdAt))})_")
        }
        val file = writeToFile(sb.toString(), "bookmarks_${dateStamp.format(Date())}.md")
        return buildShareIntent(file, "text/markdown")
    }

    fun exportAllAsMarkdown(
        notes: List<BibleNote>,
        highlights: List<Highlight>,
        bookmarks: List<Bookmark>
    ): Intent {
        val sb = StringBuilder()
        sb.appendLine("# OpenBible Scholar Study Export")
        sb.appendLine("Exported: ${sdf.format(Date())}")
        sb.appendLine()
        sb.appendLine("---")
        sb.appendLine()
        sb.append(buildMarkdownNotes(notes))
        val file = writeToFile(sb.toString(), "study_export_${dateStamp.format(Date())}.md")
        return buildShareIntent(file, "text/markdown")
    }

    private fun buildMarkdownNotes(notes: List<BibleNote>): String {
        val sb = StringBuilder()
        sb.appendLine("# My Bible Study Notes")
        sb.appendLine()
        notes.groupBy { "${it.book} ${it.chapter}" }.forEach { (chapter, chNotes) ->
            sb.appendLine("## $chapter")
            sb.appendLine()
            chNotes.sortedBy { it.verse }.forEach { note ->
                sb.appendLine("### Verse ${note.verse}")
                sb.appendLine()
                sb.appendLine(note.content)
                if (note.tags.isNotEmpty()) {
                    sb.appendLine()
                    sb.appendLine("**Tags:** ${note.tags.joinToString(", ")}")
                }
                sb.appendLine()
                sb.appendLine("_Updated: ${sdf.format(Date(note.updatedAt))}_")
                sb.appendLine()
                sb.appendLine("---")
                sb.appendLine()
            }
        }
        return sb.toString()
    }

    private fun writeToFile(content: String, filename: String): File {
        val exportDir = File(context.filesDir, "exports").also { it.mkdirs() }
        val file = File(exportDir, filename)
        file.writeText(content, Charsets.UTF_8)
        return file
    }

    private fun buildShareIntent(file: File, mimeType: String): Intent {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )
        return Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
