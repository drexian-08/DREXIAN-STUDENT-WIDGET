package com.drexian.studentwidget

import android.app.Activity
import android.os.Bundle
import android.webkit.*
import android.graphics.Color
import android.widget.*
import android.view.ViewGroup
import org.json.JSONArray
import org.json.JSONObject

class ImaalumImportActivity : Activity() {
    private lateinit var web: WebView
    private lateinit var status: TextView
    private var kind="courses"
    private var latest="[]"
    private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
    override fun onCreate(savedInstanceState:Bundle?) {
        super.onCreate(savedInstanceState)
        kind=intent.getStringExtra("kind") ?: "courses"
        val root=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setBackgroundColor(Color.WHITE) }
        status=TextView(this).apply { text="Sign in to i-Ma'luum. Then open the requested page and tap IMPORT."; setTextColor(Color.BLACK); setPadding(dp(12),dp(10),dp(12),dp(10)) }
        root.addView(status)
        web=WebView(this)
        web.settings.javaScriptEnabled=true
        web.settings.domStorageEnabled=true
        web.settings.databaseEnabled=true
        web.webViewClient=object:WebViewClient() {
            override fun onPageFinished(view:WebView,url:String) {
                val expected=if(kind=="courses") "MyAcademic/schedule" else "MyAcademic/final-exam"
                status.text=if(url.contains(expected)) "i-Ma'luum page loaded. Tap IMPORT DATA." else "Sign in, then open the " + (if(kind=="courses") "Schedule" else "Final Exam") + " page."
            }
        }
        root.addView(web,LinearLayout.LayoutParams(-1,0,1f))
        val importButton=Button(this).apply { text="IMPORT DATA FROM THIS PAGE"; setOnClickListener { extract() } }
        root.addView(importButton,LinearLayout.LayoutParams(-1,dp(54)))
        setContentView(root)
        web.loadUrl(if(kind=="courses") "https://imaluum.iium.edu.my/MyAcademic/schedule" else "https://imaluum.iium.edu.my/MyAcademic/final-exam")
    }
    private fun extract() {
        val script=if(kind=="courses") """
            (function(){
              const t=document.getElementsByClassName('table table-hover')[0];
              if(!t) return JSON.stringify({error:'No schedule table found. Sign in and open MyAcademic/Schedule.'});
              const rows=t.getElementsByTagName('tr'), out=[];
              for(let i=1;i<rows.length;i++){ const c=rows[i].cells; if(!c||c.length<3) continue;
                const code=(c[0].innerText||'').trim(), sec=(c[2].innerText||'').trim();
                if(code&&sec) out.push({courseCode:code,section:sec});
              } return JSON.stringify(out);
            })();
        """ else """
            (function(){
              const t=document.getElementsByClassName('table table-hover')[0];
              if(!t) return JSON.stringify({error:'No exam table found. Sign in and open MyAcademic/Final Exam.'});
              const rows=t.getElementsByTagName('tr'), out=[];
              for(let i=1;i<rows.length;i++){ const c=rows[i].cells; if(!c||c.length<7) continue;
                const date=(c[3].innerText||'').trim(); if(!date||date.toLowerCase()==='no final') continue;
                out.push({courseCode:(c[0].innerText||'').trim(),title:(c[1].innerText||'').trim(),section:(c[2].innerText||'').trim(),date:date,time:(c[4].innerText||'').trim(),venue:(c[5].innerText||'').trim(),seat:(c[6].innerText||'').trim()});
              } return JSON.stringify(out);
            })();
        """
        web.evaluateJavascript(script) { value ->
            val decoded=runCatching { org.json.JSONTokener(value?.trim('"') ?: "null").nextValue() }.getOrNull()
            if(decoded is JSONObject && decoded.has("error")) { status.text=decoded.optString("error"); return@evaluateJavascript }
            if(decoded !is JSONArray || decoded.length()==0) { status.text="No records found. Check the page and try again."; return@evaluateJavascript }
            latest=decoded.toString()
            status.text="Found " + decoded.length() + " records. Tap SAVE IMPORTED DATA."
            val root=status.parent as? LinearLayout
            val button=root?.getChildAt(root.childCount-1) as? Button
            button?.text="SAVE " + decoded.length() + " IMPORTED RECORDS"
            button?.setOnClickListener {
                setResult(RESULT_OK,android.content.Intent().putExtra("payload",latest).putExtra("summary",decoded.length().toString()+" records"))
                finish()
            }
        }
    }
    override fun onDestroy() { web.destroy(); super.onDestroy() }
}