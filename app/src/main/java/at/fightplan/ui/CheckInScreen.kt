package at.fightplan.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import at.fightplan.data.CheckInEntity
import at.fightplan.domain.Format
import at.fightplan.domain.WarningResult
import at.fightplan.domain.WarningSigns
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/** Check-in gehört zum Sonntag der Woche: heute, wenn Sonntag, sonst der letzte Sonntag. */
fun checkInDate(today: LocalDate): LocalDate = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))

@Composable
fun CheckInScreen(s: AppState, vm: MainViewModel) {
    val date = checkInDate(s.today)
    val existing = s.checkIns.firstOrNull { it.epochDay == date.toEpochDay() }
    val key = existing?.hashCode() ?: 0

    var sleep by rememberSaveable(key) { mutableStateOf(existing?.sleepHours?.let { Format.num(it) } ?: "") }
    var mood by rememberSaveable(key) { mutableIntStateOf(existing?.mood ?: 0) }
    var libido by rememberSaveable(key) { mutableStateOf(existing?.libidoNormal ?: true) }
    var infection by rememberSaveable(key) { mutableStateOf(existing?.infection ?: false) }
    var strength by rememberSaveable(key) { mutableStateOf(existing?.strengthOk ?: true) }
    var jump by rememberSaveable(key) { mutableStateOf(existing?.jumpCm?.let { Format.num(it) } ?: "") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Wochen-Check-in", style = MaterialTheme.typography.headlineSmall)
        Text("Woche bis ${date.format(DateLong)}", color = MaterialTheme.colorScheme.onSurfaceVariant)

        existing?.let { WarningCard(WarningSigns.evaluate(it.toData())) }

        SectionCard {
            DecimalField(sleep, { sleep = it }, "Schlaf Ø pro Nacht", Modifier.fillMaxWidth(), suffix = "h")
            Text("Stimmung (1 = schlecht, 5 = super)")
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                (1..5).forEach { v ->
                    FilterChip(selected = mood == v, onClick = { mood = v }, label = { Text("$v") })
                }
            }
            SwitchLine("Libido normal", libido) { libido = it }
            SwitchLine("Infekt in dieser Woche", infection) { infection = it }
            SwitchLine("Kraft gleich oder besser", strength) { strength = it }
            DecimalField(jump, { jump = it }, "Sprunghöhe (optional)", Modifier.fillMaxWidth(), suffix = "cm")
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(onClick = {
                val sl = Format.parseDecimal(sleep)
                when {
                    sl == null || sl !in 0.0..16.0 -> error = "Schlafstunden eingeben (z. B. 7,5)."
                    mood !in 1..5 -> error = "Stimmung auswählen."
                    else -> {
                        error = null
                        vm.saveCheckIn(
                            CheckInEntity(
                                epochDay = date.toEpochDay(),
                                sleepHours = sl,
                                mood = mood,
                                libidoNormal = libido,
                                infection = infection,
                                strengthOk = strength,
                                jumpCm = Format.parseDecimal(jump),
                            ),
                        )
                    }
                }
            }) { Text(if (existing == null) "Speichern" else "Aktualisieren") }
        }

        if (s.checkIns.any { it.epochDay != date.toEpochDay() }) {
            Text("Frühere Wochen", style = MaterialTheme.typography.titleMedium)
            s.checkIns.filter { it.epochDay != date.toEpochDay() }.take(12).forEach { c ->
                HistoryCard(c)
            }
        }
    }
}

@Composable
private fun SwitchLine(label: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = value, onCheckedChange = onChange)
    }
}

@Composable
fun WarningCard(r: WarningResult) {
    val container = when {
        r.red -> MaterialTheme.colorScheme.errorContainer
        r.count == 1 -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.tertiaryContainer
    }
    SectionCard(container = container) {
        Text(
            when (r.count) {
                0 -> "Keine Warnzeichen"
                1 -> "1 Warnzeichen"
                else -> "${r.count} Warnzeichen"
            },
            style = MaterialTheme.typography.titleMedium,
        )
        if (r.reasons.isNotEmpty()) Text(r.reasons.joinToString(", "))
        if (r.red) Text(WarningSigns.RED_MESSAGE, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.titleSmall)
        if (r.seeDoctor) Text(WarningSigns.DOCTOR_MESSAGE, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun HistoryCard(c: CheckInEntity) {
    val r = WarningSigns.evaluate(c.toData())
    SectionCard(container = if (r.red) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceContainerLow) {
        Text(LocalDate.ofEpochDay(c.epochDay).format(DateShort), style = MaterialTheme.typography.titleSmall)
        Text(
            "Schlaf ${Format.num(c.sleepHours)} h · Stimmung ${c.mood} · " +
                "Warnzeichen ${r.count}" + (c.jumpCm?.let { " · Sprung ${Format.num(it)} cm" } ?: ""),
            style = MaterialTheme.typography.bodySmall,
        )
        if (r.reasons.isNotEmpty()) Text(r.reasons.joinToString(", "), style = MaterialTheme.typography.bodySmall)
    }
}
