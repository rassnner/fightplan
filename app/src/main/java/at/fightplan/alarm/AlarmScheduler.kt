package at.fightplan.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import at.fightplan.domain.AlarmDef
import at.fightplan.domain.AlarmPlanner
import at.fightplan.domain.Agenda
import at.fightplan.domain.AppSettings
import at.fightplan.domain.Plan
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Plant jede Erinnerung einzeln mit setExactAndAllowWhileIdle. Nach dem Auslösen plant der
 * [AlarmReceiver] die nächste Woche. Ohne Erlaubnis für exakte Alarme wird auf
 * setAndAllowWhileIdle (ungenau, einige Minuten Verzögerung möglich) zurückgegriffen.
 */
class AlarmScheduler(private val context: Context) {
    private val am = context.getSystemService(AlarmManager::class.java)

    fun canScheduleExact(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()

    fun rescheduleAll(plan: Plan, settings: AppSettings, now: LocalDateTime = LocalDateTime.now()) {
        AlarmPlanner.buildAlarms(plan).forEach { def ->
            if (def.isEnabled(settings)) schedule(def, settings, now) else cancel(def.id)
        }
        scheduleWidgetTick(plan, now)
    }

    fun schedule(def: AlarmDef, settings: AppSettings, now: LocalDateTime) {
        val at = AlarmPlanner.nextTrigger(def.day, def.effectiveTime(settings), now)
        setAlarm(at, alarmIntent(def.id))
    }

    fun cancel(id: String) {
        am.cancel(alarmIntent(id))
    }

    fun scheduleWidgetTick(plan: Plan, now: LocalDateTime) {
        setAlarm(Agenda.nextWidgetUpdate(plan, now), widgetIntent())
    }

    private fun setAlarm(at: LocalDateTime, pi: PendingIntent) {
        val millis = at.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        if (canScheduleExact()) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pi)
        } else {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pi)
        }
    }

    private fun alarmIntent(id: String): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java)
            .setAction(AlarmReceiver.ACTION_ALARM)
            .setData(Uri.parse("fightplan://alarm/$id"))
            .putExtra(AlarmReceiver.EXTRA_ID, id)
        return PendingIntent.getBroadcast(
            context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun widgetIntent(): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java)
            .setAction(AlarmReceiver.ACTION_WIDGET_TICK)
            .setData(Uri.parse("fightplan://widget"))
        return PendingIntent.getBroadcast(
            context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
