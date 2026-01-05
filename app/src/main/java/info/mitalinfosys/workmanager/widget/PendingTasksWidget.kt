package info.mitalinfosys.workmanager.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.RemoteViews
import info.mitalinfosys.workmanager.R
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

class PendingTasksWidget : AppWidgetProvider() {
    private val executor = Executors.newSingleThreadExecutor()
    private val handler = Handler(Looper.getMainLooper())

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateWidgetData(context, appWidgetManager, appWidgetId)
        }
    }

    private fun updateWidgetData(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        val views = RemoteViews(context.packageName, R.layout.pending_tasks_widget)
        views.setTextViewText(R.id.widget_task_count, "Updating...")
        appWidgetManager.updateAppWidget(appWidgetId, views)
        
        executor.execute {
            try {
                val url = URL("http://web.mitalinfosys.info/api/tasks/pending")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 5000
                conn.readTimeout = 5000
                
                val response = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(response)
                val count = json.getInt("count")
                
                handler.post {
                    views.setTextViewText(R.id.widget_task_count, "Pending: $count")
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            } catch (e: Exception) {
                Log.e("PendingTasksWidget", "Error fetching tasks", e)
                handler.post {
                    views.setTextViewText(R.id.widget_task_count, "Sync Error")
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            }
        }
    }
}
