package at.fightplan.alarm

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import at.fightplan.MainActivity
import at.fightplan.R
import at.fightplan.domain.AlarmDef
import at.fightplan.domain.AlarmKind

object Notifications {
    private const val CH_REMINDERS = "reminders"
    private const val CH_TRAINING = "training"

    fun createChannels(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CH_REMINDERS, "Essen, Wiegen, Schlaf", NotificationManager.IMPORTANCE_HIGH)
                .apply { description = "Erinnerungen an Essensfenster, Wiegen, Schlaf und Check-in" },
        )
        nm.createNotificationChannel(
            NotificationChannel(CH_TRAINING, "Training", NotificationManager.IMPORTANCE_HIGH)
                .apply { description = "45 Minuten vor jeder Einheit" },
        )
    }

    fun hasPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    fun show(context: Context, def: AlarmDef) {
        if (!hasPermission(context)) return
        val tab = when (def.kind) {
            AlarmKind.CHECK_IN -> "checkin"
            else -> "today"
        }
        val open = PendingIntent.getActivity(
            context,
            def.id.hashCode(),
            Intent(context, MainActivity::class.java)
                .putExtra(MainActivity.EXTRA_TAB, tab)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val channel = if (def.kind == AlarmKind.TRAINING) CH_TRAINING else CH_REMINDERS
        val n = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(def.title)
            .setContentText(def.text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(def.text))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(def.id.hashCode(), n)
        } catch (_: SecurityException) {
            // Berechtigung wurde zwischenzeitlich entzogen.
        }
    }
}
