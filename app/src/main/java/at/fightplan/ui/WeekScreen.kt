package at.fightplan.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import at.fightplan.domain.Format
import java.time.DayOfWeek

private val ColDay = 76.dp
private val ColTime = 96.dp
private val ColUnit = 120.dp
private val ColContent = 240.dp
private val ColMeals = 260.dp

private data class WeekRow(val time: String, val unit: String, val content: String, val muted: Boolean)

@Composable
fun WeekScreen(s: AppState) {
    Column(Modifier.fillMaxSize().padding(top = 16.dp)) {
        Text("Wochenplan", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(horizontal = 16.dp))
        Text(
            "Zum Scrollen seitlich wischen",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Row(Modifier.background(MaterialTheme.colorScheme.secondaryContainer)) {
                Header("Tag", ColDay); Header("Uhrzeit", ColTime); Header("Einheit", ColUnit)
                Header("Inhalt", ColContent); Header("Essen", ColMeals)
            }
            DayOfWeek.entries.forEach { dow ->
                val day = s.plan.day(dow)
                val rows = buildList {
                    day.school?.let { add(WeekRow("${it.start}–${it.end}", "Schule", "", true)) }
                    day.sessions.forEach { add(WeekRow(it.timeText, it.title, it.content, false)) }
                }.sortedBy { it.time }
                val isToday = dow == s.today.dayOfWeek
                HorizontalDivider()
                Row(Modifier.background(if (isToday) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else Color.Transparent)) {
                    Column(Modifier.width(ColDay).padding(4.dp)) {
                        Text(Format.dayShort(dow), fontWeight = FontWeight.Bold)
                        Text(s.plan.dayType(dow).label, style = MaterialTheme.typography.labelSmall)
                    }
                    Column {
                        rows.forEach { r ->
                            val color = if (r.muted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                            Row {
                                Cell(r.time, ColTime, color)
                                Cell(r.unit, ColUnit, color, bold = !r.muted)
                                Cell(r.content, ColContent, color)
                            }
                        }
                    }
                    Column(Modifier.width(ColMeals).padding(4.dp)) {
                        day.meals.forEach { m ->
                            Text("${m.timeText}  ${m.content}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(bottom = 2.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Header(text: String, width: Dp) {
    Text(text, fontWeight = FontWeight.Bold, modifier = Modifier.width(width).padding(4.dp))
}

@Composable
private fun Cell(text: String, width: Dp, color: Color, bold: Boolean = false) {
    Text(
        text,
        color = color,
        style = MaterialTheme.typography.bodySmall,
        fontWeight = if (bold) FontWeight.SemiBold else null,
        modifier = Modifier.width(width).padding(4.dp),
    )
}
