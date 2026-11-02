package com.drexian.studentwidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import org.json.JSONArray
import org.json.JSONObject
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

data class ClassSlot(val day: DayOfWeek, val subject: String, val start: String, val end: String, val room: String = "Check timetable") {
    fun startTime() = LocalTime.parse(start)
    fun endTime() = LocalTime.parse(end)
}

class StudentWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { updateOne(context, manager, it) }
    }

    companion object {
        internal val slots = listOf(
            ClassSlot(DayOfWeek.MONDAY,"Quranic Language","10:00","12:00"),
            ClassSlot(DayOfWeek.MONDAY,"Accounting","14:00","16:00"),
            ClassSlot(DayOfWeek.TUESDAY,"Digital Literacy","08:00","10:00"),
            ClassSlot(DayOfWeek.TUESDAY,"Introduction to Law","10:00","12:00"),
            ClassSlot(DayOfWeek.TUESDAY,"Tilawah","12:00","13:00"),
            ClassSlot(DayOfWeek.TUESDAY,"Mathematics","14:00","16:00"),
            ClassSlot(DayOfWeek.WEDNESDAY,"Digital Literacy","15:00","16:00"),
            ClassSlot(DayOfWeek.WEDNESDAY,"Quranic Language","16:00","18:00"),
            ClassSlot(DayOfWeek.THURSDAY,"Accounting","08:00","10:00"),
            ClassSlot(DayOfWeek.THURSDAY,"Quranic Language","10:00","12:00"),
            ClassSlot(DayOfWeek.THURSDAY,"Introduction to Law","14:00","16:00"),
            ClassSlot(DayOfWeek.FRIDAY,"Mathematics","08:00","10:00")
        )

        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            manager.getAppWidgetIds(ComponentName(context, StudentWidgetProvider::class.java)).forEach { updateOne(context, manager, it) }
        }

        private fun updateOne(context: Context, manager: AppWidgetManager, id: Int) {
            try {
                val now = LocalDateTime.now()
                val prefs = context.getSharedPreferences("drexian_widget", Context.MODE_PRIVATE)
                val views = RemoteViews(context.packageName, R.layout.widget_student)
                views.setTextViewText(R.id.widget_clock, now.format(DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())))
                views.setTextViewText(R.id.widget_date, now.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.ENGLISH)).uppercase(Locale.ENGLISH))

                val today = slots.filter { it.day == now.dayOfWeek }.sortedBy { it.startTime() }
                val next = slots.mapNotNull { slot ->
                    val date = LocalDate.now().with(java.time.temporal.TemporalAdjusters.nextOrSame(slot.day))
                    val start = LocalDateTime.of(date, slot.startTime())
                    val end = LocalDateTime.of(date, slot.endTime())
                    val effectiveStart = if (end.isBefore(now)) start.plusWeeks(1) else start
                    if (effectiveStart.isAfter(now)) slot to effectiveStart else null
                }.minByOrNull { it.second }

                if (next == null) {
                    views.setTextViewText(R.id.widget_next_name, "NO UPCOMING CLASS")
                    views.setTextViewText(R.id.widget_next_info, "Your weekly timetable is clear.")
                } else {
                    val mins = java.time.Duration.between(now, next.second).toMinutes().coerceAtLeast(0)
                    val countdown = if (mins >= 60) "${mins / 60}h ${mins % 60}m" else "${mins}m"
                    views.setTextViewText(R.id.widget_next_name, next.first.subject)
                    views.setTextViewText(R.id.widget_next_info, "${next.first.day.name.lowercase().replaceFirstChar { it.uppercase() }} · ${next.first.start}–${next.first.end} · ${next.first.room}\nSTARTS IN $countdown")
                }

                val todayText = if (today.isEmpty()) "No classes scheduled today" else today.joinToString("\n") { "${it.start}–${it.end}  ${it.subject}" }
                views.setTextViewText(R.id.widget_today, todayText)

                val assignments = runCatching { JSONArray(prefs.getString("assignments_json", "[]")) }.getOrDefault(JSONArray())
                val exams = runCatching { JSONArray(prefs.getString("exams_json", "[]")) }.getOrDefault(JSONArray())
                val reminders = mutableListOf<String>()
                for (i in 0 until minOf(assignments.length(), 2)) {
                    val a = assignments.optJSONObject(i) ?: continue
                    reminders.add("TASK · ${a.optString("title","Assignment")} · ${a.optString("due","Set due date")}")
                }
                for (i in 0 until minOf(exams.length(), 2)) {
                    val e = exams.optJSONObject(i) ?: continue
                    reminders.add("EXAM · ${e.optString("title", e.optString("course","Exam"))} · ${e.optString("date","Date not set")}")
                }
                views.setTextViewText(R.id.widget_reminders, if (reminders.isEmpty()) "No saved deadlines yet" else reminders.joinToString("\n"))

                val intent = Intent(context, MainActivity::class.java)
                val pending = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                views.setOnClickPendingIntent(R.id.widget_root, pending)
                manager.updateAppWidget(id, views)
            } catch (e: Exception) {
                android.util.Log.e("DrexianWidget", "Widget update failed", e)
            }
        }
    }
}
