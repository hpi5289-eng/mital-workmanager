package info.mitalinfosys.workmanager.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import info.mitalinfosys.workmanager.R
import info.mitalinfosys.workmanager.MainActivity

class WorkManagerWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) { updateAppWidget(context, appWidgetManager, appWidgetId) }
    }

    companion object {
        const val ACTION_REFRESH = "info.mitalinfosys.workmanager.ACTION_REFRESH"
        
        internal fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_workmanager)
            val intent = Intent(context, WorkListWidgetService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
            }
            views.setRemoteAdapter(R.id.widget_list, intent)
            views.setEmptyView(R.id.widget_list, R.id.widget_empty)
            
            val openAppIntent = Intent(context, MainActivity::class.java)
            views.setOnClickPendingIntent(R.id.widget_title, PendingIntent.getActivity(context, 0, openAppIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
            
            val refreshIntent = Intent(context, WorkManagerWidget::class.java).apply { action = ACTION_REFRESH; putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId) }
            views.setOnClickPendingIntent(R.id.widget_refresh, PendingIntent.getBroadcast(context, appWidgetId, refreshIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
            
            val addWorkIntent = Intent(context, MainActivity::class.java).apply { putExtra("action", "add_work"); flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP }
            views.setOnClickPendingIntent(R.id.widget_add, PendingIntent.getActivity(context, 1, addWorkIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
            
            views.setPendingIntentTemplate(R.id.widget_list, PendingIntent.getBroadcast(context, 0, Intent(context, WorkManagerWidget::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE))
            
            appWidgetManager.updateAppWidget(appWidgetId, views)
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_list)
        }
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_REFRESH -> {
                val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
                if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                    AppWidgetManager.getInstance(context).notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_list)
                }
            }
            "MARK_DONE" -> {
                val workId = intent.getStringExtra("work_id")
                if (workId != null) {
                    context.startActivity(Intent(context, MainActivity::class.java).apply {
                        putExtra("action", "mark_done"); putExtra("work_id", workId)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    })
                }
            }
        }
    }
}
