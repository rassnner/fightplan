package at.fightplan.alarm

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import at.fightplan.container
import at.fightplan.domain.AlarmPlanner
import at.fightplan.widget.TodayWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime

/** Führt suspend-Code im Receiver aus, ohne dass der Prozess vorher beendet wird. */
private fun BroadcastReceiver.runAsync(block: suspend () -> Unit) {
    val pending = goAsync()
    CoroutineScope(Dispatchers.Default).launch {
        try {
            block()
        } finally {
            pending?.finish()
        }
    }
}

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val c = context.container
        runAsync {
            val settings = c.settings.current()
            // Basis eine Minute später, damit derselbe Alarm nicht sofort erneut geplant wird.
            val next = LocalDateTime.now().plusMinutes(1)
            when (intent.action) {
                ACTION_ALARM -> {
                    val id = intent.getStringExtra(EXTRA_ID) ?: return@runAsync
                    val def = AlarmPlanner.buildAlarms(c.plan).firstOrNull { it.id == id } ?: return@runAsync
                    if (def.isEnabled(settings)) {
                        Notifications.show(context, def)
                        c.alarms.schedule(def, settings, next)
                    }
                    TodayWidget.updateAll(context)
                }
                ACTION_WIDGET_TICK -> {
                    TodayWidget.updateAll(context)
                    c.alarms.scheduleWidgetTick(c.plan, LocalDateTime.now())
                }
            }
        }
    }

    companion object {
        const val ACTION_ALARM = "at.fightplan.ALARM"
        const val ACTION_WIDGET_TICK = "at.fightplan.WIDGET_TICK"
        const val EXTRA_ID = "alarm_id"
    }
}

/** Plant nach Neustart, App-Update, Zeit-/Zeitzonenwechsel und Rechteänderung alles neu. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED,
            -> runAsync {
                context.container.rescheduleAll()
                TodayWidget.updateAll(context)
            }
        }
    }
}
