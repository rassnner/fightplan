package at.fightplan.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import at.fightplan.alarm.Notifications

object Permissions {
    fun notifications(context: Context) = Notifications.hasPermission(context)

    fun exactAlarms(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()

    fun batteryUnrestricted(context: Context): Boolean =
        context.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(context.packageName)

    fun allGranted(context: Context) = notifications(context) && exactAlarms(context) && batteryUnrestricted(context)
}

@SuppressLint("BatteryLife")
@Composable
fun PermissionsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var tick by remember { mutableIntStateOf(0) }
    LifecycleResumeEffect(Unit) {
        tick++
        onPauseOrDispose { }
    }
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { tick++ }
    // tick sorgt dafür, dass die Status-Abfragen nach Rückkehr aus den Einstellungen neu laufen.
    val notif = remember(tick) { Permissions.notifications(context) }
    val exact = remember(tick) { Permissions.exactAlarms(context) }
    val battery = remember(tick) { Permissions.batteryUnrestricted(context) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zurück") }
            Text("Berechtigungen", style = MaterialTheme.typography.headlineSmall)
        }
        Text(
            "Damit Erinnerungen pünktlich kommen, auch wenn die App geschlossen ist, braucht FightPlan drei Freigaben.",
            style = MaterialTheme.typography.bodyMedium,
        )

        PermissionCard(
            title = "Benachrichtigungen",
            text = "Ohne diese Erlaubnis siehst du keine Erinnerungen (ab Android 13 nötig).",
            granted = notif,
            action = "Erlauben",
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                context.startActivity(
                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
                )
            }
        }

        PermissionCard(
            title = "Exakte Alarme",
            text = "Ab Android 14 standardmäßig aus. Ohne sie kann Android Erinnerungen um mehrere Minuten verschieben.",
            granted = exact,
            action = "Einstellungen öffnen",
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.startActivity(
                    Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}")),
                )
            }
        }

        PermissionCard(
            title = "Akku-Optimierung ausnehmen",
            text = "Manche Hersteller (Samsung, Xiaomi, Huawei, OnePlus) beenden Apps im Hintergrund aggressiv. " +
                "Stell FightPlan auf \"Nicht eingeschränkt\". Bei Xiaomi zusätzlich \"Autostart\" erlauben.",
            granted = battery,
            action = "Ausnehmen",
        ) {
            val direct = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${context.packageName}"))
            runCatching { context.startActivity(direct) }
                .onFailure { context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) }
        }
        OutlinedButton(onClick = {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")),
            )
        }) { Text("App-Info öffnen") }
    }
}

@Composable
private fun PermissionCard(title: String, text: String, granted: Boolean, action: String, onClick: () -> Unit) {
    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(
                if (granted) Icons.Filled.CheckCircle else Icons.Filled.Error,
                contentDescription = null,
                tint = if (granted) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error,
            )
            Text(title, style = MaterialTheme.typography.titleMedium)
        }
        Text(text, style = MaterialTheme.typography.bodyMedium)
        if (!granted) Button(onClick = onClick) { Text(action) } else Text("Erteilt", color = MaterialTheme.colorScheme.tertiary)
    }
}
