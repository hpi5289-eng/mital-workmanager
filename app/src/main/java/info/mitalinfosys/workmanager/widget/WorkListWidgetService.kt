package info.mitalinfosys.workmanager.widget

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import info.mitalinfosys.workmanager.R
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class WorkListWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return WorkListRemoteViewsFactory(applicationContext)
    }
}

class WorkListRemoteViewsFactory(
    private val context: Context
) : RemoteViewsService.RemoteViewsFactory {

    private var works: List<WorkItem> = emptyList()
    private val executor = Executors.newSingleThreadExecutor()

    data class WorkItem(
        val id: String,
        val partyName: String,
        val workName: String,
        val workType: String,
        val status: String
    )

    override fun onCreate() {
        // Initial setup
    }

    override fun onDataSetChanged() {
        // Fetch pending works from API on background thread
        val latch = CountDownLatch(1)
        var fetchedWorks: List<WorkItem> = emptyList()
        
        executor.execute {
            fetchedWorks = fetchPendingWorks()
            latch.countDown()
        }
        
        try {
            latch.await(30, TimeUnit.SECONDS)
            works = fetchedWorks
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    private fun fetchPendingWorks(): List<WorkItem> {
        val result = mutableListOf<WorkItem>()
        try {
            val url = URL("https://web.mitalinfosys.info/api/works")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            
            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = reader.readText()
                reader.close()
                
                val jsonArray = JSONArray(response)
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val status = obj.optString("status", "")
                    // Only show pending works
                    if (status == "pending") {
                        result.add(WorkItem(
                            id = obj.optString("id", ""),
                            partyName = obj.optString("partyName", ""),
                            workName = obj.optString("workName", ""),
                            workType = obj.optString("workType", ""),
                            status = status
                        ))
                    }
                }
            }
            connection.disconnect()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return result.take(10) // Limit to 10 items
    }

    override fun onDestroy() {
        works = emptyList()
        executor.shutdown()
    }

    override fun getCount(): Int = works.size

    override fun getViewAt(position: Int): RemoteViews {
        if (position >= works.size) {
            return RemoteViews(context.packageName, R.layout.widget_work_item)
        }
        
        val work = works[position]
        val views = RemoteViews(context.packageName, R.layout.widget_work_item)
        
        views.setTextViewText(R.id.work_party_name, work.partyName)
        views.setTextViewText(R.id.work_name, work.workName)
        views.setTextViewText(R.id.work_type, work.workType)
        
        // Set fill-in intent for Mark Done action
        val fillInIntent = Intent().apply {
            action = "MARK_DONE"
            putExtra("work_id", work.id)
        }
        views.setOnClickFillInIntent(R.id.btn_mark_done, fillInIntent)
        
        return views
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long = position.toLong()

    override fun hasStableIds(): Boolean = true
}
