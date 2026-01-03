package info.mitalinfosys.workmanager.widget

import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import info.mitalinfosys.workmanager.R
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONArray
import java.util.concurrent.Executors

class WorkListWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory = WorkListRemoteViewsFactory(applicationContext)
}

class WorkListRemoteViewsFactory(private val context: Context) : RemoteViewsService.RemoteViewsFactory {
    private var works: List<WorkItem> = emptyList()
    data class WorkItem(val id: String, val customerName: String, val workType: String, val status: String)
    
    override fun onCreate() {}
    override fun onDataSetChanged() {
        val executor = Executors.newSingleThreadExecutor()
        val future = executor.submit<List<WorkItem>> {
            try {
                val url = URL("http://web.mitalinfosys.info/api/works")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 15000
                connection.readTimeout = 15000
                connection.setRequestProperty("Accept", "application/json")
                if (connection.responseCode == 200) {
                    val response = connection.inputStream.bufferedReader().readText()
                    val jsonArray = JSONArray(response)
                    val items = mutableListOf<WorkItem>()
                    for (i in 0 until minOf(jsonArray.length(), 10)) {
                        val obj = jsonArray.getJSONObject(i)
                        val status = obj.optString("status", "PENDING")
                        if (status == "PENDING" || status == "IN_PROGRESS") {
                            items.add(WorkItem(
                                obj.optString("id", ""),
                                obj.optString("customerName", "Unknown"),
                                obj.optString("workType", "BASIC"),
                                status
                            ))
                        }
                    }
                    items
                } else emptyList()
            } catch (e: Exception) {
                android.util.Log.e("Widget", "Error fetching works: ${e.message}")
                emptyList()
            }
        }
        works = try { future.get() } catch (e: Exception) { emptyList() }
        executor.shutdown()
    }
    override fun onDestroy() { works = emptyList() }
    override fun getCount(): Int = works.size
    override fun getViewAt(position: Int): RemoteViews {
        if (position >= works.size) return RemoteViews(context.packageName, R.layout.widget_work_item)
        val work = works[position]
        return RemoteViews(context.packageName, R.layout.widget_work_item).apply {
            setTextViewText(R.id.work_customer, work.customerName)
            setTextViewText(R.id.work_type, work.workType)
            val fillInIntent = Intent().apply { 
                action = "MARK_DONE"
                putExtra("work_id", work.id)
            }
            setOnClickFillInIntent(R.id.work_item_container, fillInIntent)
        }
    }
    override fun getLoadingView(): RemoteViews? = null
    override fun getViewTypeCount(): Int = 1
    override fun getItemId(position: Int): Long = position.toLong()
    override fun hasStableIds(): Boolean = true
}
