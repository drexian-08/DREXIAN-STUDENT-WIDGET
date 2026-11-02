package com.drexian.studentwidget

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.graphics.Color
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("drexian_widget", MODE_PRIVATE) }
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun label(s: String, size: Float, color: Int = Color.WHITE) = TextView(this).apply {
        text=s; textSize=size; setTextColor(color); setPadding(0, dp(8), 0, dp(8))
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(dp(22),dp(28),dp(22),dp(22)); setBackgroundColor(Color.rgb(12,12,12)) }
        root.addView(label("DREXIAN STUDENT",25f))
        root.addView(label("IIUM i-Ma'luum connection",16f,Color.LTGRAY))
        root.addView(label("Sign in through the official IIUM page. This app does not ask for or store your password.",13f,Color.LTGRAY))
        root.addView(Button(this).apply { text="IMPORT REGISTERED COURSES"; setOnClickListener { startActivityForResult(Intent(this@MainActivity,ImaalumImportActivity::class.java).putExtra("kind","courses"),101) } },LinearLayout.LayoutParams(-1,dp(54)))
        root.addView(Button(this).apply { text="IMPORT FINAL EXAM TIMETABLE"; setOnClickListener { startActivityForResult(Intent(this@MainActivity,ImaalumImportActivity::class.java).putExtra("kind","exams"),102) } },LinearLayout.LayoutParams(-1,dp(54)))
        root.addView(label("IMPORTED DATA",12f,Color.GRAY))
        root.addView(label("Courses: " + (prefs.getString("courses_summary","Not imported") ?: "Not imported"),14f))
        root.addView(label("Exams: " + (prefs.getString("exams_summary","Not imported") ?: "Not imported"),14f))
        root.addView(label("Note: i-Ma'luum course import provides course codes and sections. Actual weekly class times still need to be matched to the official section timetable; this version will not invent class times.",12f,Color.LTGRAY))
        root.addView(Button(this).apply { text="REFRESH HOME WIDGET"; setOnClickListener { StudentWidgetProvider.updateAll(this@MainActivity) } },LinearLayout.LayoutParams(-1,dp(54)))
        setContentView(root)
    }
    @Deprecated("Deprecated in Android")
    override fun onActivityResult(requestCode:Int,resultCode:Int,data:Intent?) {
        super.onActivityResult(requestCode,resultCode,data)
        if(resultCode!=RESULT_OK || data==null) return
        val raw=data.getStringExtra("payload") ?: return
        if(requestCode==101) prefs.edit().putString("courses_json",raw).putString("courses_summary",data.getStringExtra("summary") ?: "Imported").apply()
        if(requestCode==102) prefs.edit().putString("exams_json",raw).putString("exams_summary",data.getStringExtra("summary") ?: "Imported").apply()
        StudentWidgetProvider.updateAll(this)
        recreate()
    }
}