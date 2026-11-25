package at.fightplan.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import at.fightplan.domain.Format
import at.fightplan.domain.MacroCalculator
import java.time.LocalDate

@Composable
fun SettingsScreen(s: AppState, vm: MainViewModel) {
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let(vm::export)
    }
    var importConfirm by remember { mutableStateOf(false) }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(vm::import)
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Einstellungen", style = MaterialTheme.typography.headlineSmall)

        WeightSettings(s, vm)

        SectionCard {
            Text("Makro-Anpassungen", style = MaterialTheme.typography.titleMedium)
            if (s.settings.kcalOffsets.values.all { it == 0 }) {
                Text("Keine – es gelten die Faktoren aus dem Plan.")
            } else {
                s.plan.dayTypes.forEach { (key, type) ->
                    val off = s.settings.kcalOffsets[key] ?: 0
                    if (off != 0) Text("${type.label}: %+d kcal".format(off))
                }
            }
            val avg = MacroCalculator.weeklyAverageKcal(s.plan, s.targets.basisWeightKg, s.settings.kcalOffsets)
            Text("Wochenschnitt aktuell: ${avg.toInt()} kcal/Tag", style = MaterialTheme.typography.bodySmall)
            if (s.settings.kcalOffsets.isNotEmpty() || s.settings.maintenanceApplied) {
                OutlinedButton(onClick = {
                    vm.updateSettings { it.copy(kcalOffsets = emptyMap(), maintenanceApplied = false, lastAdjustmentDecisionEpochDay = null) }
                }) { Text("Zurücksetzen") }
            }
        }

        SectionCard {
            Text("Daten", style = MaterialTheme.typography.titleMedium)
            Text("Sicherung als JSON (Gewicht, Taille, Check-ins, Häkchen, Einstellungen).")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { exportLauncher.launch("fightplan-${LocalDate.now()}.json") }) { Text("Export") }
                OutlinedButton(onClick = { importConfirm = true }) { Text("Import") }
            }
        }
    }

    if (importConfirm) {
        AlertDialog(
            onDismissRequest = { importConfirm = false },
            title = { Text("Daten importieren?") },
            text = { Text("Alle aktuellen Daten werden durch den Inhalt der Datei ersetzt.") },
            confirmButton = {
                TextButton(onClick = {
                    importConfirm = false
                    importLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream"))
                }) { Text("Datei wählen") }
            },
            dismissButton = { TextButton(onClick = { importConfirm = false }) { Text("Abbrechen") } },
        )
    }
}

@Composable
private fun WeightSettings(s: AppState, vm: MainViewModel) {
    var start by rememberSaveable(s.settings.startWeightKg) { mutableStateOf(Format.num(s.settings.startWeight(s.plan))) }
    var goal by rememberSaveable(s.settings.goalWeightKg) { mutableStateOf(Format.num(s.settings.goalWeight(s.plan))) }
    var error by remember { mutableStateOf(false) }
    SectionCard {
        Text("Gewicht", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DecimalField(start, { start = it; error = false }, "Startgewicht", Modifier.weight(1f), suffix = "kg")
            DecimalField(goal, { goal = it; error = false }, "Zielgewicht", Modifier.weight(1f), suffix = "kg")
        }
        if (error) Text("Bitte gültige Werte eingeben.", color = MaterialTheme.colorScheme.error)
        Button(onClick = {
            val st = Format.parseDecimal(start)
            val gl = Format.parseDecimal(goal)
            if (st == null || gl == null || st !in 30.0..200.0 || gl !in 30.0..200.0) {
                error = true
            } else {
                vm.updateSettings { it.copy(startWeightKg = st, goalWeightKg = gl, maintenanceApplied = false) }
            }
        }) { Text("Speichern") }
        Text(
            "Das Startgewicht wird für die Makros genutzt, solange noch keine Messungen da sind.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
