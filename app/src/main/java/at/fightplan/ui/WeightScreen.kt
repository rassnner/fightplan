package at.fightplan.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import at.fightplan.domain.Format
import at.fightplan.domain.WeightPoint
import at.fightplan.domain.WeightStats
import java.time.LocalDate
import kotlin.math.ceil

@Composable
fun WeightScreen(s: AppState, vm: MainViewModel) {
    val goal = s.settings.goalWeight(s.plan)
    val d = s.plan.defaults
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Gewicht", style = MaterialTheme.typography.headlineSmall)

        SectionCard {
            WeightChart(s.weights, goal, Modifier.fillMaxWidth().height(220.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Legend(MaterialTheme.colorScheme.outline, "Tageswert")
                Legend(MaterialTheme.colorScheme.primary, "7-Tage-Schnitt")
                Legend(MaterialTheme.colorScheme.tertiary, "Ziel")
            }
        }

        SectionCard {
            Text("Verlauf", style = MaterialTheme.typography.titleMedium)
            StatLine("7-Tage-Schnitt", s.average?.let { Format.kg(it, 2) } ?: "noch keine Daten")
            StatLine("Verlust letzte Woche", s.lossLastWeek?.let { Format.signedKg(it) } ?: "zu wenig Daten")
            StatLine("Ø Verlust/Woche (2 Wochen)", s.lossTwoWeeks?.let { Format.signedKg(it) } ?: "zu wenig Daten")
            StatLine("Zielbereich", "${Format.num(d.targetLossMinKgPerWeek)}–${Format.num(d.targetLossMaxKgPerWeek)} kg/Woche")
            StatLine("Ziel", Format.kg(goal))
            Text(goalHint(s, goal), style = MaterialTheme.typography.bodyMedium)
            Text(
                "Positive Werte = abgenommen. Verglichen werden immer 7-Tage-Schnitte, Tagesschwankungen spielen keine Rolle.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        s.pendingSuggestion?.let { SuggestionCard(it, vm) }

        AddEntryCard(
            title = "Gewicht nachtragen",
            unit = "kg",
            today = s.today,
            range = 30.0..200.0,
        ) { date, v -> vm.saveWeight(date, v) }

        SectionCard {
            Text("Taillenumfang (wöchentlich)", style = MaterialTheme.typography.titleMedium)
            if (s.waist.isEmpty()) Text("Noch keine Messung. Am besten sonntags morgens auf Nabelhöhe messen.")
            s.waist.takeLast(8).reversed().forEachIndexed { i, w ->
                val list = s.waist.takeLast(8).reversed()
                val diff = list.getOrNull(i + 1)?.let { w.cm - it.cm }
                EntryLine(
                    LocalDate.ofEpochDay(w.epochDay),
                    "${Format.num(w.cm)} cm" + (diff?.let { "  (" + String.format(Format.locale, "%+.1f", it) + ")" } ?: ""),
                ) { vm.deleteWaist(w.epochDay) }
            }
        }
        AddEntryCard(title = "Taille eintragen", unit = "cm", today = s.today, range = 40.0..150.0) { date, v ->
            vm.saveWaist(date, v)
        }

        SectionCard {
            Text("Letzte Einträge", style = MaterialTheme.typography.titleMedium)
            if (s.weights.isEmpty()) Text("Noch keine Einträge.")
            s.weights.takeLast(14).reversed().forEach { w ->
                EntryLine(w.date, Format.kg(w.kg) + "   Ø " + Format.kg(WeightStats.rollingAverage(s.weights, w.date)!!)) {
                    vm.deleteWeight(w.date)
                }
            }
        }
    }
}

private fun goalHint(s: AppState, goal: Double): String {
    val avg = s.average ?: return "Trag jeden Morgen dein Gewicht ein, dann siehst du hier deinen Trend."
    if (avg <= goal) return "Zielgewicht erreicht. Jetzt auf Erhaltungskalorien gehen (Tagesziele plus ${s.plan.defaults.maintenanceExtraKcal} kcal)."
    val rest = avg - goal
    val weeks = ceil(rest / 0.5).toInt()
    val loss = s.lossTwoWeeks ?: s.lossLastWeek
    val trend = when {
        loss == null -> ""
        loss < s.plan.defaults.targetLossMinKgPerWeek -> " Aktuell langsamer als geplant."
        loss > s.plan.defaults.targetLossMaxKgPerWeek -> " Aktuell schneller als geplant – nicht zu aggressiv werden."
        else -> " Du liegst im Zielbereich."
    }
    return "Noch ${Format.kg(rest)} bis ${Format.kg(goal)}, bei 0,5 kg/Woche ca. $weeks Wochen.$trend"
}

@Composable
private fun StatLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun Legend(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(Modifier.size(10.dp), shape = CircleShape, color = color) {}
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun EntryLine(date: LocalDate, text: String, onDelete: () -> Unit) {
    var confirm by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(date.format(DateShort), modifier = Modifier.width(88.dp), style = MaterialTheme.typography.labelLarge)
        Text(text, modifier = Modifier.weight(1f))
        if (confirm) {
            OutlinedButton(onClick = { onDelete(); confirm = false }) { Text("Löschen?") }
        } else {
            IconButton(onClick = { confirm = true }) { Icon(Icons.Filled.Delete, "Löschen") }
        }
    }
}

@Composable
private fun AddEntryCard(
    title: String,
    unit: String,
    today: LocalDate,
    range: ClosedFloatingPointRange<Double>,
    onSave: (LocalDate, Double) -> Unit,
) {
    var date by remember(today) { mutableStateOf(today) }
    var text by rememberSaveable { mutableStateOf("") }
    var pick by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf(false) }
    SectionCard {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { pick = true }) { Text(date.format(DateShort)) }
            DecimalField(text, { text = it; error = false }, unit, Modifier.weight(1f))
            Button(onClick = {
                val v = Format.parseDecimal(text)
                if (v == null || v !in range || date.isAfter(today)) error = true else { onSave(date, v); text = "" }
            }) { Text("OK") }
        }
        if (error) Text("Ungültiger Wert oder Datum in der Zukunft.", color = MaterialTheme.colorScheme.error)
    }
    if (pick) DatePickDialog(date, onDismiss = { pick = false }) { date = it }
}

@Composable
private fun WeightChart(points: List<WeightPoint>, goal: Double, modifier: Modifier) {
    val daily = MaterialTheme.colorScheme.outline
    val avgColor = MaterialTheme.colorScheme.primary
    val goalColor = MaterialTheme.colorScheme.tertiary
    val grid = MaterialTheme.colorScheme.outlineVariant
    val data = points.sortedBy { it.date }.takeLast(60)
    if (data.size < 2) {
        Box(modifier, contentAlignment = Alignment.Center) {
            Text("Ab zwei Einträgen erscheint hier das Diagramm.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    val avg = data.map { WeightPoint(it.date, WeightStats.rollingAverage(points, it.date)!!) }
    val minKg = minOf(data.minOf { it.kg }, goal) - 0.5
    val maxKg = data.maxOf { it.kg } + 0.5
    val first = data.first().date.toEpochDay()
    val span = (data.last().date.toEpochDay() - first).coerceAtLeast(1)

    Column(modifier) {
        Row(Modifier.fillMaxWidth()) {
            Text(Format.kg(maxKg, 0), style = MaterialTheme.typography.labelSmall)
        }
        Canvas(Modifier.fillMaxWidth().weight(1f)) {
            fun x(d: LocalDate) = (d.toEpochDay() - first).toFloat() / span * size.width
            fun y(kg: Double) = ((maxKg - kg) / (maxKg - minKg)).toFloat() * size.height

            for (i in 0..4) {
                val gy = size.height * i / 4f
                drawLine(grid, Offset(0f, gy), Offset(size.width, gy), strokeWidth = 1f)
            }
            drawLine(
                goalColor, Offset(0f, y(goal)), Offset(size.width, y(goal)), strokeWidth = 3f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 12f)),
            )
            val dailyPath = Path()
            data.forEachIndexed { i, p ->
                if (i == 0) dailyPath.moveTo(x(p.date), y(p.kg)) else dailyPath.lineTo(x(p.date), y(p.kg))
            }
            drawPath(dailyPath, daily, style = Stroke(width = 2f))
            data.forEach { drawCircle(daily, 4f, Offset(x(it.date), y(it.kg))) }
            val avgPath = Path()
            avg.forEachIndexed { i, p ->
                if (i == 0) avgPath.moveTo(x(p.date), y(p.kg)) else avgPath.lineTo(x(p.date), y(p.kg))
            }
            drawPath(avgPath, avgColor, style = Stroke(width = 6f))
        }
        Text(Format.kg(minKg, 0), style = MaterialTheme.typography.labelSmall)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(data.first().date.format(DateShort), style = MaterialTheme.typography.labelSmall)
            Text(data.last().date.format(DateShort), style = MaterialTheme.typography.labelSmall)
        }
    }
}
