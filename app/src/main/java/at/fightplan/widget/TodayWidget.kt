package at.fightplan.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import at.fightplan.MainActivity
import at.fightplan.container
import at.fightplan.domain.Agenda
import at.fightplan.domain.DayTargetCalculator
import at.fightplan.domain.Format
import at.fightplan.domain.Upcoming
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private data class WidgetState(
    val dayLabel: String,
    val typeLabel: String,
    val macros: String,
    val nextSession: String,
    val nextMeal: String,
)

class TodayWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val state = load(context)
        provideContent { GlanceTheme { Content(state) } }
    }

    private suspend fun load(context: Context): WidgetState {
        val c = context.container
        val now = LocalDateTime.now()
        val today = LocalDate.now()
        val settings = c.settings.current()
        val weights = c.db.dao().weights().first().map { it.toPoint() }
        val t = DayTargetCalculator.targets(c.plan, settings, weights, today)
        val m = t.macros
        return WidgetState(
            dayLabel = Format.dayLong(today.dayOfWeek),
            typeLabel = t.type.label,
            macros = "${m.kcal} kcal · E ${m.proteinG} · KH ${m.carbsG} · F ${m.fatG} g",
            nextSession = Agenda.nextSession(c.plan, now)?.describe(today) ?: "–",
            nextMeal = Agenda.nextMeal(c.plan, now)?.let { it.describe(today) + ": " + it.detail } ?: "–",
        )
    }

    private fun Upcoming.describe(today: LocalDate): String {
        val time = at.format(DateTimeFormatter.ofPattern("HH:mm"))
        val prefix = if (at.toLocalDate() == today) "" else Format.dayShort(at.dayOfWeek) + " "
        return if (title == "Essen") "$prefix$time" else "$prefix$time $title"
    }

    @Composable
    private fun Content(s: WidgetState) {
        val title = TextStyle(color = GlanceTheme.colors.onSurface, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        val label = TextStyle(color = GlanceTheme.colors.primary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        val body = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 13.sp)
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.widgetBackground)
                .cornerRadius(16.dp)
                .padding(12.dp)
                .clickable(actionStartActivity<MainActivity>()),
        ) {
            Row {
                Text("${s.dayLabel} · ${s.typeLabel}", style = title)
            }
            Spacer(GlanceModifier.height(4.dp))
            Text(s.macros, style = body, maxLines = 1)
            Spacer(GlanceModifier.height(6.dp))
            Row {
                Text("Termin", style = label, modifier = GlanceModifier.width(56.dp))
                Text(s.nextSession, style = body, maxLines = 1)
            }
            Spacer(GlanceModifier.height(2.dp))
            Row {
                Text("Essen", style = label, modifier = GlanceModifier.width(56.dp))
                Text(s.nextMeal, style = body, maxLines = 2)
            }
        }
    }

    companion object {
        suspend fun updateAll(context: Context) {
            runCatching { TodayWidget().updateAll(context) }
        }
    }
}

class TodayWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TodayWidget()
}
