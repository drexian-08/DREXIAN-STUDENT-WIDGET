package com.drexian.studentwidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

data class ClassSlot(val name: String, val code: String, val day: DayOfWeek, val start: String, val end: String, val room: String)

class StudentWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { updateOne(context, manager, it) }
    }

    companion object {
        private val slots = listOf(
            ClassSlot("ACCOUNTING", "ENMF 0224", DayOfWeek.MONDAY, "14:00", "16:00", "JURJANI LR(B).12"),
            ClassSlot("QURANIC LANGUAGE 1", "LQAF 0813", DayOfWeek.MONDAY, "10:00", "12:00", "JURJANI LR(B).45"),
            ClassSlot("ETHICAL DIGITAL LITERACY", "ICTF 0513", DayOfWeek.TUESDAY, "08:00", "10:00", "AL KHALIL EL1"),
            ClassSlot("INTRODUCTION TO LAW", "SSHF 0114", DayOfWeek.TUESDAY, "10:00", "12:00", "JURJANI T.ROOM 11"),
            ClassSlot("TILAWAH AL QURAN 1", "TQTD 1002", DayOfWeek.TUESDAY, "12:00", "13:00", "AL KHALIL EAV3"),
            ClassSlot("MATHEMATICS", "ENMF 0214", DayOfWeek.TUESDAY, "14:00", "16:00", "AL GHAZALI STUDIO 7"),
            ClassSlot("ETHICAL DIGITAL LITERACY", "ICTF 0513", DayOfWeek.WEDNESDAY, "15:00", "16:00", "AL KHALIL EM4"),
            ClassSlot("QURANIC LANGUAGE 1", "LQAF 0813", DayOfWeek.WEDNESDAY, "16:00", "18:00", "JURJANI LR(B).45"),
            ClassSlot("ACCOUNTING", "ENMF 0224", DayOfWeek.THURSDAY, "08:00", "10:00", "JURJANI LR(B).10"),
            ClassSlot("QURANIC LANGUAGE 1", "LQAF 0813", DayOfWeek.THURSDAY, "10:00", "12:00", "JURJANI LR(B).45"),
            ClassSlot("INTRODUCTION TO LAW", "SSHF 0114", DayOfWeek.THURSDAY, "14:00", "16:00", "JURJANI T.ROOM 11"),
            ClassSlot("MATHEMATICS", "ENMF 0214", DayOfWeek.FRIDAY, "08:00", "10:00", "AL GHAZALI STUDIO 7")
        )

        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, StudentWidgetProvider::class.java))
            ids.forEach { updateOne(context, manager, it) }
        }

        private fun updateOne(context: Context, manager: AppWidgetManager, id: Int) {
            val now = LocalDateTime.now()
            val views = RemoteViews(context.packageName, R.layout.widget_student)
            val timeFmt = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
            val dateFmt = DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.ENGLISH)
            views.setTextViewText(R.id.widget_clock, now.format(timeFmt))
            views.setTextViewText(R.id.widget_date, now.format(dateFmt).uppercase(Locale.ENGLISH))

            val upcoming = (0..7).flatMap { offset ->
                val date = now.toLocalDate().plusDays(offset.toLong())
                slots.filter { it.day == date.dayOfWeek }.mapNotNull { slot ->
                    val begins = LocalDateTime.of(date, LocalTime.parse(slot.start))
                    if (begins.isAfter(now)) Triple(begins, slot, date) else null
                }
            }.minByOrNull { it.first }

            if (upcoming != null) {
                val (_, slot, date) = upcoming
                val dayLabel = if (date == now.toLocalDate()) "TODAY" else date.format(DateTimeFormatter.ofPattern("EEE, d MMM", Locale.ENGLISH)).uppercase(Locale.ENGLISH)
                views.setTextViewText(R.id.widget_next_name, slot.name)
                views.setTextViewText(R.id.widget_next_info, "${slot.code}  ·  $dayLabel  ·  ${slot.start}–${slot.end}\n${slot.room}")
            } else {
                views.setTextViewText(R.id.widget_next_name, "NO UPCOMING CLASS")
                views.setTextViewText(R.id.widget_next_info, "Enjoy your free time.")
            }

            val today = slots.filter { it.day == now.dayOfWeek }.sortedBy { it.start }
            val todayText = if (today.isEmpty()) "No classes scheduled today" else today.joinToString("\n") { "${it.start}  ${it.name}" }.take(170)
            val prefs = context.getSharedPreferences("drexian_widget", Context.MODE_PRIVATE)
            val assignment = prefs.getString("assignment", "")?.trim().orEmpty()
            val due = prefs.getString("due", "")?.trim().orEmpty()
            val exam = prefs.getString("exam", "")?.trim().orEmpty()
            val extras = buildString {
                if (assignment.isNotBlank()) append("\nTASK  ·  $assignment${if (due.isNotBlank()) " (due $due)" else ""}")
                if (exam.isNotBlank()) append("\nEXAM  ·  $exam")
            }
            views.setTextViewText(R.id.widget_agenda, "TODAY'S AGENDA\n$todayText$extras")

            val open = Intent(context, MainActivity::class.java)
            val pending = PendingIntent.getActivity(context, 0, open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.widget_root, pending)
            manager.updateAppWidget(id, views)
        }
    }
}