package at.fightplan.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import at.fightplan.container
import at.fightplan.domain.Agenda
import at.fightplan.domain.Plan
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Stille Aktualisierung des Widgets zu jeder Termin- und Essenszeit und um 00:05.
 * Nutzt einen ungenauen RTC-Timer: keine Benachrichtigung, keine Berechtigung, weckt das Gerät nicht.
 */
object WidgetRefresh {
    fun scheduleNext(context: Context, plan: Plan, now: LocalDateTime = LocalDateTime.now()) {
        val at = Agenda.nextWidgetUpdate(plan, now)
        val millis = at.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        context.getSystemService(AlarmManager::class.java)
            .setAndAllowWhileIdle(AlarmManager.RTC, millis, pendingIntent(context))
    }

    private fun pendingIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context,
        0,
        Intent(context, WidgetRefreshReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}

class WidgetRefreshReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                TodayWidget.updateAll(context)
                WidgetRefresh.scheduleNext(context, context.container.plan)
            } finally {
                pending?.finish()
            }
        }
    }
}
