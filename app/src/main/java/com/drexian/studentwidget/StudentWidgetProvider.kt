package com.drexian.studentwidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import org.json.JSONArray
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class StudentWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context:Context,manager:AppWidgetManager,ids:IntArray) {
        ids.forEach { updateOne(context,manager,it) }
    }
    companion object {
        fun updateAll(context:Context) {
            val manager=AppWidgetManager.getInstance(context)
            manager.getAppWidgetIds(ComponentName(context,StudentWidgetProvider::class.java)).forEach { updateOne(context,manager,it) }
        }
        private fun updateOne(context:Context,manager:AppWidgetManager,id:Int) {
            try {
                val now=LocalDateTime.now()
                val prefs=context.getSharedPreferences("drexian_widget",Context.MODE_PRIVATE)
                val views=RemoteViews(context.packageName,R.layout.widget_student)
                views.setTextViewText(R.id.widget_clock,now.format(DateTimeFormatter.ofPattern("HH:mm",Locale.getDefault())))
                views.setTextViewText(R.id.widget_date,now.format(DateTimeFormatter.ofPattern("EEEE, d MMMM",Locale.ENGLISH)).uppercase(Locale.ENGLISH))
                val courses=runCatching { JSONArray(prefs.getString("courses_json","[]")) }.getOrNull()
                val exams=runCatching { JSONArray(prefs.getString("exams_json","[]")) }.getOrNull()
                val courseLines=mutableListOf<String>()
                if(courses!=null) for(i in 0 until courses.length()) {
                    val item=courses.optJSONObject(i) ?: continue
                    courseLines.add(item.optString("courseCode")+" · Section "+item.optString("section"))
                }
                val examLines=mutableListOf<String>()
                if(exams!=null) for(i in 0 until exams.length()) {
                    val item=exams.optJSONObject(i) ?: continue
                    examLines.add(item.optString("courseCode")+" · "+item.optString("date")+" "+item.optString("time"))
                }
                views.setTextViewText(R.id.widget_next_name,if(courseLines.isEmpty()) "CONNECT TO i-MA'LUUM" else "REGISTERED COURSES")
                views.setTextViewText(R.id.widget_next_info,if(courseLines.isEmpty()) "Open DREXIAN app and import your courses." else courseLines.take(3).joinToString("\n"))
                val agenda=buildString {
                    append("EXAMS\n")
                    append(if(examLines.isEmpty()) "No exam data imported" else examLines.take(3).joinToString("\n"))
                    if(courseLines.isNotEmpty()) append("\n\nMore courses: "+(courseLines.size- minOf(3,courseLines.size)))
                }
                views.setTextViewText(R.id.widget_agenda,agenda.take(300))
                val intent=Intent(context,MainActivity::class.java)
                val pending=PendingIntent.getActivity(context,0,intent,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                views.setOnClickPendingIntent(R.id.widget_root,pending)
                manager.updateAppWidget(id,views)
            } catch (e:Exception) {
                android.util.Log.e("DrexianWidget","Widget update failed",e)
            }
        }
    }
}