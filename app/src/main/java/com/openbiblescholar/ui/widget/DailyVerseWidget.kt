package com.openbiblescholar.ui.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews

class DailyVerseWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        // Simple widget layout using RemoteViews
        // In production: query DB for today's verse
        val widgetText = "\"For God so loved the world...\"\n— John 3:16 (KJV)"

        val views = RemoteViews(context.packageName, android.R.layout.simple_list_item_2).apply {
            setTextViewText(android.R.id.text1, "📖 Verse of the Day")
            setTextViewText(android.R.id.text2, widgetText)
        }

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}
