package com.drexian.studentwidget

import android.app.Activity
import android.os.Bundle
import android.webkit.*
import android.graphics.Color
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener

class ImaalumImportActivity : Activity() {
    private lateinit var web: WebView
    private lateinit var status: TextView
    private lateinit var importButton: Button
    private var kind="courses"
    private var latest="[]"
    private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState:Bundle?) {
        super.onCreate(savedInstanceState)
        kind=intent.getStringExtra("kind") ?: "courses"
        val root=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setBackgroundColor(Color.WHITE) }
        status=TextView(this).apply {
            text="Sign in to i-Ma'luum. Wait for the requested page to finish loading, then tap IMPORT."
            setTextColor(Color.BLACK); setPadding(dp(12),dp(10),dp(12),dp(10))
        }
        root.addView(status)
        web=WebView(this)
        web.settings.javaScriptEnabled=true
        web.settings.domStorageEnabled=true
        web.settings.databaseEnabled=true
        web.webViewClient=object:WebViewClient() {
            override fun onPageFinished(view:WebView,url:String) {
                val expected=if(kind=="courses") "MyAcademic/schedule" else "MyAcademic/final-exam"
                status.text=if(url.contains(expected,true)) "Target page loaded. If your course/exam table is visible, tap IMPORT DATA." else "Sign in, then open the " + (if(kind=="courses") "Schedule" else "Final Exam") + " page. Current page: "+url
            }
        }
        root.addView(web,LinearLayout.LayoutParams(-1,0,1f))
        importButton=Button(this).apply { text="IMPORT DATA FROM THIS PAGE"; setOnClickListener { extract() } }
        root.addView(importButton,LinearLayout.LayoutParams(-1,dp(54)))
        setContentView(root)
        web.loadUrl(if(kind=="courses") "https://imaluum.iium.edu.my/MyAcademic/schedule" else "https://imaluum.iium.edu.my/MyAcademic/final-exam")
    }

    private fun extract() {
        status.text="Reading i-Ma'luum table..."
        val script=if(kind=="courses") """
            (function(){
              const tables=Array.from(document.querySelectorAll('table'));
              const t=tables.find(x=>x.rows && x.rows.length>1);
              if(!t) return JSON.stringify({error:'No HTML table with data rows found. Make sure the registered course table is visible.'});
              const rows=Array.from(t.rows), out=[];
              for(let i=1;i<rows.length;i++){
                const c=rows[i].cells; if(!c||c.length<2) continue;
                const vals=Array.from(c).map(x=>(x.innerText||x.textContent||'').trim());
                const code=vals.find(x=>/[A-Z]{2,}[A-Z0-9]*\s*\d{3,}/i.test(x)) || vals[0] || '';
                const section=vals.length>=3 ? vals[2] : vals[1];
                if(code && section) out.push({courseCode:code,section:section});
              }
              return JSON.stringify(out.length?out:{error:'Found '+rows.length+' table rows, but could not identify course code and section columns. Please send a screenshot of the table.'});
            })();
        """ else """
            (function(){
              const tables=Array.from(document.querySelectorAll('table'));
              const t=tables.find(x=>x.rows && x.rows.length>1);
              if(!t) return JSON.stringify({error:'No HTML table with data rows found. Make sure the final exam table is visible.'});
              const rows=Array.from(t.rows), out=[];
              for(let i=1;i<rows.length;i++){
                const c=rows[i].cells; if(!c||c.length<4) continue;
                const vals=Array.from(c).map(x=>(x.innerText||x.textContent||'').trim());
                const dateIndex=vals.findIndex(x=>/\d{1,2}[\/-]\d{1,2}[\/-]\d{2,4}|[A-Za-z]{3,}\s+\d{1,2},?\s+\d{4}/.test(x));
                if(dateIndex<0 || vals[dateIndex].toLowerCase()==='no final') continue;
                out.push({courseCode:vals[0]||'',title:vals[1]||'',section:vals[2]||'',date:vals[dateIndex]||'',time:vals[dateIndex+1]||'',venue:vals[dateIndex+2]||'',seat:vals[dateIndex+3]||''});
              }
              return JSON.stringify(out.length?out:{error:'Exam table was found, but no exam rows could be recognised. Please send a screenshot of the table.'});
            })();
        """
        web.evaluateJavascript(script) { value ->
            // evaluateJavascript returns a JSON-encoded JS string. Decode that outer
            // string first, then parse the JSON text returned by JSON.stringify().
            val jsString=runCatching { JSONTokener(value ?: "null").nextValue() }.getOrNull()
            val decoded=if(jsString is String) runCatching { JSONTokener(jsString).nextValue() }.getOrNull() else jsString
            if(decoded is JSONObject && decoded.has("error")) {
                status.text=decoded.optString("error")
                return@evaluateJavascript
            }
            if(decoded !is JSONArray || decoded.length()==0) {
                status.text="The page returned data in an unexpected format. Try reloading the page and importing again."
                return@evaluateJavascript
            }
            latest=decoded.toString()
            status.text="Found "+decoded.length()+" records. Tap SAVE IMPORTED DATA."
            importButton.text="SAVE "+decoded.length()+" IMPORTED RECORDS"
            importButton.setOnClickListener {
                setResult(RESULT_OK,android.content.Intent().putExtra("payload",latest).putExtra("summary",decoded.length().toString()+" records"))
                finish()
            }
        }
    }

    override fun onDestroy() { web.destroy(); super.onDestroy() }
}