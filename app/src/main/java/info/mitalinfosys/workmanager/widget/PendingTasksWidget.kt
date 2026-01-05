package info.mitalinfosys.workmanager.widget
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import info.mitalinfosys.workmanager.R
class PendingTasksWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.pending_tasks_widget)
            views.setTextViewText(R.id.widget_task_count, "Sync pending...")
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
