package at.fightplan.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.TextStyle
import java.util.Locale

/** Ein Termin oder Essen mit konkretem Datum, z. B. für Widget und Heute-Ansicht. */
data class Upcoming(val at: LocalDateTime, val title: String, val detail: String)

object Agenda {
    private fun sessionsOn(plan: Plan, date: LocalDate): List<Upcoming> =
        plan.day(date.dayOfWeek).sessions.mapNotNull { s ->
            s.startTime?.let { Upcoming(LocalDateTime.of(date, it), s.title, s.content) }
        }

    private fun mealsOn(plan: Plan, date: LocalDate): List<Upcoming> =
        plan.day(date.dayOfWeek).meals.mapNotNull { m ->
            m.localTime?.let { Upcoming(LocalDateTime.of(date, it), "Essen", m.content) }
        }

    /** Nächster Termin (Training/Verein), der noch nicht begonnen hat, max. 7 Tage voraus. */
    fun nextSession(plan: Plan, now: LocalDateTime): Upcoming? = nextOf(now) { sessionsOn(plan, it) }

    /** Nächstes Essensfenster mit Uhrzeit, max. 7 Tage voraus. */
    fun nextMeal(plan: Plan, now: LocalDateTime): Upcoming? = nextOf(now) { mealsOn(plan, it) }

    private fun nextOf(now: LocalDateTime, items: (LocalDate) -> List<Upcoming>): Upcoming? {
        for (i in 0..7) {
            val date = now.toLocalDate().plusDays(i.toLong())
            items(date).filter { it.at.isAfter(now) }.minByOrNull { it.at }?.let { return it }
        }
        return null
    }

    /**
     * Nächster Zeitpunkt, an dem das Widget aktualisiert werden soll: jede Termin- (Beginn/Ende) und
     * Essenszeit sowie täglich 00:05.
     */
    fun nextWidgetUpdate(plan: Plan, now: LocalDateTime): LocalDateTime {
        val candidates = mutableListOf<LocalDateTime>()
        for (i in 0..1) {
            val date = now.toLocalDate().plusDays(i.toLong())
            val day = plan.day(date.dayOfWeek)
            day.sessions.forEach { s ->
                s.startTime?.let { candidates += LocalDateTime.of(date, it) }
                s.endTime?.let { candidates += LocalDateTime.of(date, it) }
            }
            day.meals.forEach { m -> m.localTime?.let { candidates += LocalDateTime.of(date, it) } }
            candidates += LocalDateTime.of(date, LocalTime.of(0, 5))
        }
        return candidates.filter { it.isAfter(now) }.min()
    }
}

object Format {
    val locale: Locale = Locale.GERMANY

    fun dayShort(d: DayOfWeek): String = d.getDisplayName(TextStyle.SHORT, locale).removeSuffix(".")
    fun dayLong(d: DayOfWeek): String = d.getDisplayName(TextStyle.FULL, locale)
    fun kg(v: Double, digits: Int = 1) = String.format(locale, "%.${digits}f kg", v)
    fun num(v: Double, digits: Int = 1) = String.format(locale, "%.${digits}f", v)
    fun signedKg(v: Double) = String.format(locale, "%+.2f kg", v)

    /** Akzeptiert "77,4" und "77.4". */
    fun parseDecimal(s: String): Double? = s.trim().replace(',', '.').toDoubleOrNull()
}
