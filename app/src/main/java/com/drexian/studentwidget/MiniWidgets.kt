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
        WidgetAutoRefresh.schedule(context)
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
        val now = LocalDateTime.now()
        val next = StudentWidgetProvider.slots.mapNotNull { slot ->
            val date = java.time.LocalDate.now().with(java.time.temporal.TemporalAdjusters.nextOrSame(slot.day))
            val start = LocalDateTime.of(date, slot.startTime())
            val end = LocalDateTime.of(date, slot.endTime())
            val candidate = if (end.isBefore(now)) start.plusWeeks(1) else start
            if (candidate.isAfter(now)) slot to candidate else null
        }.minByOrNull { it.second }
        views.setTextViewText(R.id.mini_primary, next?.first?.subject ?: "No upcoming class")
        if (next != null) {
            val mins = java.time.Duration.between(now, next.second).toMinutes().coerceAtLeast(0)
            val day = next.second.format(DateTimeFormatter.ofPattern("EEE", Locale.ENGLISH))
            views.setTextViewText(R.id.mini_secondary, day + " · " + next.first.start + "–" + next.first.end + "\nVENUE: " + next.first.room + " · starts in " + (mins / 60) + "h " + (mins % 60) + "m")
        } else views.setTextViewText(R.id.mini_secondary, "No upcoming class in the saved timetable")
    }
}

class TodayWidgetProvider : MiniWidgetProvider() {
    override fun layout() = R.layout.widget_today
    override fun render(context: Context, views: RemoteViews) {
        val today = StudentWidgetProvider.slots.filter { it.day == LocalDateTime.now().dayOfWeek }.sortedBy { it.startTime() }
        views.setTextViewText(R.id.mini_primary, if (today.isEmpty()) "No classes today" else today.size.toString() + " classes today")
        views.setTextViewText(R.id.mini_secondary, today.take(3).joinToString("\n") { it.start + "–" + it.end + "  " + it.subject + "\n   " + it.room }.ifBlank { "Enjoy your free day" })
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

object MiniWidgetRefresh {
    fun updateAll(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        val providers = listOf(
            ClockWidgetProvider::class.java,
            NextClassWidgetProvider::class.java,
            TodayWidgetProvider::class.java,
            AssignmentWidgetProvider::class.java,
            ExamWidgetProvider::class.java
        )
        providers.forEach { type ->
            val ids = manager.getAppWidgetIds(android.content.ComponentName(context, type))
            if (ids.isNotEmpty()) {
                val intent = Intent(context, type).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                }
                context.sendBroadcast(intent)
            }
        }
    }
}


class WidgetRefreshWorker(context: Context, params: androidx.work.WorkerParameters) : androidx.work.Worker(context, params) {
 override fun doWork(): androidx.work.ListenableWorker.Result {
  return try { StudentWidgetProvider.updateAll(applicationContext); MiniWidgetRefresh.updateAll(applicationContext); androidx.work.ListenableWorker.Result.success() }
  catch (e: Exception) { android.util.Log.e("DrexianAutoRefresh", "Refresh failed", e); androidx.work.ListenableWorker.Result.retry() }
 }
}

object WidgetAutoRefresh {
 fun schedule(context: Context) {
  val request = androidx.work.PeriodicWorkRequestBuilder<WidgetRefreshWorker>(15, java.util.concurrent.TimeUnit.MINUTES).build()
  androidx.work.WorkManager.getInstance(context).enqueueUniquePeriodicWork("drexian_widget_auto_refresh", androidx.work.ExistingPeriodicWorkPolicy.UPDATE, request)
 }
}
