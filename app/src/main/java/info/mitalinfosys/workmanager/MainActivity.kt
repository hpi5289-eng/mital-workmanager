package info.mitalinfosys.workmanager

import android.Manifest
import android.content.pm.PackageManager
import android.database.Cursor
import android.os.Bundle
import android.provider.CallLog
import android.provider.ContactsContract
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.view.KeyEvent
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    private val PERMISSIONS_REQUEST_CODE = 100
    private val requiredPermissions = arrayOf(
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.READ_CALL_LOG
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        webView = findViewById(R.id.webview)
        setupWebView()
        checkAndRequestPermissions()
        
        // Load the web app
        webView.loadUrl("http://web.mitalinfosys.info")
    }

    private fun setupWebView() {
        webView.webViewClient = WebViewClient()
        webView.webChromeClient = WebChromeClient()
        
        // Add JavaScript interface for native bridge
        webView.addJavascriptInterface(WorkManagerBridge(), "AndroidBridge")
        
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

    private fun checkAndRequestPermissions() {
        val permissionsToRequest = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        
        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissionsToRequest.toTypedArray(),
                PERMISSIONS_REQUEST_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            val denied = permissions.filterIndexed { index, _ ->
                grantResults[index] != PackageManager.PERMISSION_GRANTED
            }
            if (denied.isNotEmpty()) {
                Toast.makeText(this, "Some permissions denied - contacts/call log may not work", Toast.LENGTH_LONG).show()
            } else {
                // Reload page to trigger native data fetch
                webView.reload()
            }
        }
    }

    inner class WorkManagerBridge {
        @JavascriptInterface
        fun isNativeApp(): Boolean = true

        @JavascriptInterface
        fun getContacts(): String {
            if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.READ_CONTACTS) 
                != PackageManager.PERMISSION_GRANTED) {
                return "[]"
            }

            val contactsList = JSONArray()
            try {
                val cursor: Cursor? = contentResolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    arrayOf(
                        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                        ContactsContract.CommonDataKinds.Phone.NUMBER
                    ),
                    null,
                    null,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
                )

                cursor?.use {
                    val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                    val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                    var count = 0
                    val seen = mutableSetOf<String>()

                    while (it.moveToNext() && count < 500) {
                        val name = it.getString(nameIndex) ?: "Unknown"
                        val number = it.getString(numberIndex)?.replace(Regex("[\\s-()]"), "") ?: ""
                        
                        // Skip duplicates
                        if (number.isNotEmpty() && !seen.contains(number)) {
                            seen.add(number)
                            val contact = JSONObject().apply {
                                put("name", name)
                                put("number", number)
                            }
                            contactsList.put(contact)
                            count++
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("WorkManagerBridge", "Error getting contacts: ${e.message}")
            }

            return contactsList.toString()
        }

        @JavascriptInterface
        fun getCallLog(): String {
            if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.READ_CALL_LOG) 
                != PackageManager.PERMISSION_GRANTED) {
                return "[]"
            }

            val callList = JSONArray()
            try {
                val cursor: Cursor? = contentResolver.query(
                    CallLog.Calls.CONTENT_URI,
                    arrayOf(
                        CallLog.Calls.CACHED_NAME,
                        CallLog.Calls.NUMBER,
                        CallLog.Calls.DATE,
                        CallLog.Calls.TYPE
                    ),
                    null,
                    null,
                    CallLog.Calls.DATE + " DESC"
                )

                cursor?.use {
                    val nameIndex = it.getColumnIndex(CallLog.Calls.CACHED_NAME)
                    val numberIndex = it.getColumnIndex(CallLog.Calls.NUMBER)
                    val dateIndex = it.getColumnIndex(CallLog.Calls.DATE)
                    val typeIndex = it.getColumnIndex(CallLog.Calls.TYPE)
                    var count = 0

                    while (it.moveToNext() && count < 100) {
                        val name = it.getString(nameIndex) ?: "Unknown"
                        val number = it.getString(numberIndex) ?: ""
                        val dateMillis = it.getLong(dateIndex)
                        val callType = it.getInt(typeIndex)
                        
                        val timeAgo = getTimeAgo(dateMillis)

                        val call = JSONObject().apply {
                            put("name", if (name.isEmpty()) number else name)
                            put("number", number)
                            put("time", timeAgo)
                            put("type", callType)
                        }
                        callList.put(call)
                        count++
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("WorkManagerBridge", "Error getting call log: ${e.message}")
            }

            return callList.toString()
        }

        @JavascriptInterface
        fun requestPermissions() {
            runOnUiThread {
                checkAndRequestPermissions()
            }
        }

        private fun getTimeAgo(timeMillis: Long): String {
            val now = System.currentTimeMillis()
            val diff = now - timeMillis
            
            val seconds = diff / 1000
            val minutes = seconds / 60
            val hours = minutes / 60
            val days = hours / 24

            return when {
                days > 0 -> if (days == 1L) "Yesterday" else "$days days ago"
                hours > 0 -> "$hours hr ago"
                minutes > 0 -> "$minutes min ago"
                else -> "Just now"
            }
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
