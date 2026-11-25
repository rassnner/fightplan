package at.fightplan.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import at.fightplan.domain.Exercise
import at.fightplan.domain.Guideline
import at.fightplan.domain.Session

/** Übungen, Ablauf und Hinweise einer Einheit. */
@Composable
fun SessionDetails(s: Session) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (s.exercises.isNotEmpty()) {
            s.exercises.forEachIndexed { i, e ->
                if (i > 0) HorizontalDivider()
                ExerciseRow(i + 1, e)
            }
            if (s.exercises.any { it.dp }) {
                Text(
                    "DP = Double Progression, RIR 1–2",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        s.details.forEach { d ->
            Row {
                Text("•", modifier = Modifier.width(16.dp))
                Text(d, style = MaterialTheme.typography.bodyMedium)
            }
        }
        s.footer?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ExerciseRow(n: Int, e: Exercise) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text("$n.", style = MaterialTheme.typography.labelLarge, modifier = Modifier.width(24.dp))
        Column(Modifier.weight(1f)) {
            Text(e.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            e.note?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Column(Modifier.width(104.dp).padding(start = 8.dp)) {
            Text(e.sets, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            if (e.dp) Text("DP", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun SessionDialog(s: Session, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(s.title)
                Text(
                    s.timeText + (s.note?.let { " · $it" } ?: ""),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(s.content, style = MaterialTheme.typography.bodyMedium)
                SessionDetails(s)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Schließen") } },
    )
}

@Composable
fun GuidelinesCard(guidelines: List<Guideline>) {
    if (guidelines.isEmpty()) return
    SectionCard {
        Text("Progression & Steuerung", style = MaterialTheme.typography.titleMedium)
        guidelines.forEach { g ->
            Text(g.title, style = MaterialTheme.typography.titleSmall)
            Text(g.text, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
