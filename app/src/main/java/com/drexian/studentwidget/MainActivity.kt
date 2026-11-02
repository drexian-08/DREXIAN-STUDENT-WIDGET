package com.drexian.studentwidget

import android.app.Activity
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.graphics.Color
import android.graphics.Typeface

class MainActivity : Activity() {
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("drexian_widget", MODE_PRIVATE)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(30), dp(22), dp(22))
            setBackgroundColor(Color.rgb(12, 12, 12))
        }
        fun label(text: String, size: Float, color: Int = Color.WHITE, bold: Boolean = false) = TextView(this).apply {
            this.text = text
            textSize = size
            setTextColor(color)
            if (bold) setTypeface(null, Typeface.BOLD)
            setPadding(0, dp(7), 0, dp(7))
        }
        root.addView(label("DREXIAN", 12f, Color.LTGRAY, true))
        root.addView(label("STUDENT WIDGET", 25f, Color.WHITE, true))
        root.addView(label("Your timetable widget is ready. Add it from your Samsung Home Screen widget picker.", 14f, Color.LTGRAY))
        root.addView(label("QUICK TASK DISPLAY", 10f, Color.GRAY, true))
        val task = EditText(this).apply { hint = "Assignment / task name"; setText(prefs.getString("assignment", "")); setTextColor(Color.WHITE); setHintTextColor(Color.GRAY); setSingleLine() }
        val due = EditText(this).apply { hint = "Due date (e.g. 12 Nov)"; setText(prefs.getString("due", "")); setTextColor(Color.WHITE); setHintTextColor(Color.GRAY); setSingleLine() }
        val exam = EditText(this).apply { hint = "Next exam (e.g. Accounting - 20 Nov)"; setText(prefs.getString("exam", "")); setTextColor(Color.WHITE); setHintTextColor(Color.GRAY); setSingleLine() }
        listOf(task, due, exam).forEach { root.addView(it, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(52))) }
        val save = Button(this).apply {
            text = "SAVE & UPDATE WIDGET"
            setOnClickListener {
                prefs.edit().putString("assignment", task.text.toString()).putString("due", due.text.toString()).putString("exam", exam.text.toString()).apply()
                StudentWidgetProvider.updateAll(this@MainActivity)
                text = "SAVED"
            }
        }
        root.addView(save, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(54)))
        root.addView(label("HOME SCREEN SETUP", 10f, Color.GRAY, true))
        root.addView(label("1. Return to Home Screen.\n2. Press and hold an empty area.\n3. Tap Widgets.\n4. Find DREXIAN Student Widget.\n5. Drag the 4 x 3 widget onto your screen.\n\nTimetable is preloaded from your current semester schedule. Tap the widget to edit task and exam details.", 13f, Color.LTGRAY))
        setContentView(root)
    }
}