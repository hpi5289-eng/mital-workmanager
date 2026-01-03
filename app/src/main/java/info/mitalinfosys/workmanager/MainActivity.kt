package info.mitalinfosys.workmanager

import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebSettings
import android.webkit.WebChromeClient
import androidx.appcompat.app.AppCompatActivity
import android.view.KeyEvent

class MainActivity : AppCompatActivity() {
    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        webView = findViewById(R.id.webview)
        setupWebView()
        
        val action = intent.getStringExtra("action")
        val url = when (action) {
            "add_work" -> "http://web.mitalinfosys.info/#/add-work"
            "mark_done" -> {
                val workId = intent.getStringExtra("work_id")
                "http://web.mitalinfosys.info/#/mark-done/$workId"
            }
            else -> "http://web.mitalinfosys.info"
        }
        webView.loadUrl(url)
    }
    
    private fun setupWebView() {
        webView.webViewClient = WebViewClient()
        webView.webChromeClient = WebChromeClient()
        
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            cacheMode = WebSettings.LOAD_DEFAULT
            allowFileAccess = true
            allowContentAccess = true
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
            useWideViewPort = true
            loadWithOverviewMode = true
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        }
    }
    
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && webView.canGoBack()) {
            webView.goBack()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
}
