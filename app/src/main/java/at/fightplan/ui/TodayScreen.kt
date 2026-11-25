package at.fightplan.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import at.fightplan.domain.Format
import at.fightplan.domain.Suggestion
import java.time.DayOfWeek

@Composable
fun TodayScreen(s: AppState, vm: MainViewModel) {
    val day = s.plan.day(s.today.dayOfWeek)

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(s.today.format(DateLong), style = MaterialTheme.typography.headlineSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AssistChip(onClick = {}, label = { Text(s.targets.type.label) })
            s.average?.let { AssistChip(onClick = {}, label = { Text("Ø 7 Tage ${Format.kg(it)}") }) }
        }

        WeightInputCard(s, vm)

        SectionCard {
            Text("Makro-Ziele heute", style = MaterialTheme.typography.titleMedium)
            MacroRow(s.targets.macros)
            Text(
                "Basis: ${Format.kg(s.targets.basisWeightKg)} (7-Tage-Schnitt)" +
                    (s.settings.kcalOffsets[s.targets.typeKey]?.takeIf { it != 0 }?.let { " · Anpassung %+d kcal".format(it) } ?: ""),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        s.pendingSuggestion?.let { SuggestionCard(it, vm) }

        if (s.today.dayOfWeek == DayOfWeek.SUNDAY && s.checkIns.none { it.epochDay == s.today.toEpochDay() }) {
            SectionCard(container = MaterialTheme.colorScheme.secondaryContainer) {
                Text("Heute ist Wochen-Check-in", style = MaterialTheme.typography.titleMedium)
                Text("Unten im Tab \"Check-in\" ausfüllen.")
            }
        }

        SectionCard {
            Text("Termine", style = MaterialTheme.typography.titleMedium)
            day.school?.let { TimeLine("${it.start}–${it.end}", "Schule", null) }
            day.sessions.sortedBy { it.start ?: "" }.forEach {
                TimeLine(it.timeText, it.title, it.content + (it.note?.let { n -> " · $n" } ?: ""))
            }
        }

        day.sessions.filter { it.hasDetails }.forEach { session ->
            SectionCard {
                Text(session.title, style = MaterialTheme.typography.titleMedium)
                Text(
                    session.timeText + (session.note?.let { " · $it" } ?: ""),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                SessionDetails(session)
            }
        }

        SectionCard {
            Text("Essensfenster", style = MaterialTheme.typography.titleMedium)
            day.meals.forEach { meal ->
                val checked = meal.id in s.checkedMeals
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = checked, onCheckedChange = { vm.toggleMeal(meal.id, it) })
                    Text(meal.timeText, style = MaterialTheme.typography.labelLarge, modifier = Modifier.width(64.dp))
                    Text(
                        meal.content,
                        style = MaterialTheme.typography.bodyMedium,
                        textDecoration = if (checked) TextDecoration.LineThrough else null,
                        color = if (checked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

@Composable
private fun TimeLine(time: String, title: String, detail: String?) {
    Row(Modifier.fillMaxWidth()) {
        Text(time, style = MaterialTheme.typography.labelLarge, modifier = Modifier.width(96.dp))
        Column {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            detail?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

@Composable
private fun WeightInputCard(s: AppState, vm: MainViewModel) {
    val focus = LocalFocusManager.current
    var text by rememberSaveable(s.today, s.todayWeight) {
        mutableStateOf(s.todayWeight?.let { Format.num(it) } ?: "")
    }
    var error by remember { mutableStateOf(false) }
    SectionCard {
        Text("Morgengewicht", style = MaterialTheme.typography.titleMedium)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            DecimalField(
                value = text,
                onChange = { text = it; error = false },
                label = "kg",
                modifier = Modifier.weight(1f),
            )
            Button(onClick = {
                val v = Format.parseDecimal(text)
                if (v == null || v < 30 || v > 200) {
                    error = true
                } else {
                    vm.saveWeight(s.today, v)
                    focus.clearFocus()
                }
            }) { Text(if (s.todayWeight == null) "Speichern" else "Ändern") }
        }
        if (error) Text("Bitte ein Gewicht zwischen 30 und 200 kg eingeben.", color = MaterialTheme.colorScheme.error)
        s.todayWeight?.let { Text("Heute eingetragen: ${Format.kg(it)}", style = MaterialTheme.typography.bodySmall) }
    }
}

@Composable
fun SuggestionCard(sg: Suggestion, vm: MainViewModel) {
    val (title, text) = when (sg) {
        is Suggestion.Decrease -> "Abnahme zu langsam" to
            "Ø ${Format.num(sg.lossPerWeek, 2)} kg/Woche (Ziel 0,4–0,6). Vorschlag: an Normal- und Ruhetagen " +
            "${sg.kcal} kcal weniger, über Kohlenhydrate."
        is Suggestion.Increase -> "Abnahme zu schnell" to
            "Ø ${Format.num(sg.lossPerWeek, 2)} kg/Woche. Vorschlag: an allen Tagen ${sg.kcal} kcal mehr."
        is Suggestion.GoalReached -> "Zielgewicht erreicht, Erhaltungskalorien" to
            "7-Tage-Schnitt ${Format.kg(sg.averageKg)}. Vorschlag: alle Tagesziele plus ${sg.extraKcal} kcal."
        else -> return
    }
    SectionCard(container = MaterialTheme.colorScheme.tertiaryContainer) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(text)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { vm.acceptSuggestion(sg) }) { Text("Übernehmen") }
            OutlinedButton(onClick = { vm.declineSuggestion(sg) }) { Text("Nicht ändern") }
        }
    }
}
