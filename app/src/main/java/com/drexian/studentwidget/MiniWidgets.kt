package com.drexian.studentwidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import org.json.JSONArray
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

abstract class MiniWidgetProvider : AppWidgetProvider() {
    abstract fun layout(): Int
    abstract fun render(context: Context, views: RemoteViews)

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { id ->
            try {
                val views = RemoteViews(context.packageName, layout())
                render(context, views)
                val open = PendingIntent.getActivity(
                    context, id, Intent(context, MainActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.mini_widget_root, open)
                manager.updateAppWidget(id, views)
            } catch (e: Exception) {
                android.util.Log.e("DrexianMiniWidget", "Widget update failed", e)
            }
        }
    }
}

class ClockWidgetProvider : MiniWidgetProvider() {
    override fun layout() = R.layout.widget_clock
    override fun render(context: Context, views: RemoteViews) {
        val now = LocalDateTime.now()
        views.setTextViewText(R.id.mini_primary, now.format(DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())))
        views.setTextViewText(R.id.mini_secondary, now.format(DateTimeFormatter.ofPattern("EEE, d MMM", Locale.ENGLISH)).uppercase(Locale.ENGLISH))
    }
}

class NextClassWidgetProvider : MiniWidgetProvider() {
    override fun layout() = R.layout.widget_next_class
    override fun render(context: Context, views: RemoteViews) {
        views.setTextViewText(R.id.mini_primary, "NEXT CLASS")
        views.setTextViewText(R.id.mini_secondary, "Connect your verified timetable in the app")
    }
}

class TodayWidgetProvider : MiniWidgetProvider() {
    override fun layout() = R.layout.widget_today
    override fun render(context: Context, views: RemoteViews) {
        views.setTextViewText(R.id.mini_primary, "TODAY'S CLASSES")
        views.setTextViewText(R.id.mini_secondary, "Add your weekly class schedule in the app")
    }
}

class AssignmentWidgetProvider : MiniWidgetProvider() {
    override fun layout() = R.layout.widget_assignments
    override fun render(context: Context, views: RemoteViews) {
        val data = runCatching {
            JSONArray(context.getSharedPreferences("drexian_widget", Context.MODE_PRIVATE)
                .getString("assignments_json", "[]"))
        }.getOrDefault(JSONArray())
        val lines = (0 until minOf(data.length(), 2)).mapNotNull { i ->
            data.optJSONObject(i)?.let { "• ${it.optString("title", "Assignment")} — ${it.optString("due", "Due date not set")}" }
        }
        views.setTextViewText(R.id.mini_primary, "UPCOMING ASSIGNMENTS")
        views.setTextViewText(R.id.mini_secondary, lines.takeIf { it.isNotEmpty() }?.joinToString("\n") ?: "No assignments saved yet")
    }
}

class ExamWidgetProvider : MiniWidgetProvider() {
    override fun layout() = R.layout.widget_exams
    override fun render(context: Context, views: RemoteViews) {
        val data = runCatching {
            JSONArray(context.getSharedPreferences("drexian_widget", Context.MODE_PRIVATE)
                .getString("exams_json", "[]"))
        }.getOrDefault(JSONArray())
        val lines = (0 until minOf(data.length(), 2)).mapNotNull { i ->
            data.optJSONObject(i)?.let { "• ${it.optString("title", it.optString("course", "Exam"))} — ${it.optString("date", "Date not set")}" }
        }
        views.setTextViewText(R.id.mini_primary, "EXAM COUNTDOWN")
        views.setTextViewText(R.id.mini_secondary, lines.takeIf { it.isNotEmpty() }?.joinToString("\n") ?: "No exams saved yet")
    }
}
