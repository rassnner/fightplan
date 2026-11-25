package at.fightplan.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.TextStyle
import java.util.Locale

enum class AlarmKind { WEIGH_IN, MEAL, TRAINING, FIXED, CHECK_IN }

/** Eine wöchentlich wiederkehrende Erinnerung. */
data class AlarmDef(
    val id: String,
    val kind: AlarmKind,
    val day: DayOfWeek,
    val defaultTime: LocalTime,
    val title: String,
    val text: String,
) {
    fun effectiveTime(settings: AppSettings): LocalTime =
        settings.alarmTimeOverrides[id]?.let(LocalTime::parse) ?: defaultTime

    fun isEnabled(settings: AppSettings) = id !in settings.disabledAlarms
}

object AlarmPlanner {
    const val CHECK_IN_ID = "checkin"

    fun buildAlarms(plan: Plan): List<AlarmDef> = buildList {
        val d = plan.defaults
        DayOfWeek.entries.forEach { day ->
            add(
                AlarmDef(
                    id = "wiegen-${day.name.lowercase()}",
                    kind = AlarmKind.WEIGH_IN,
                    day = day,
                    defaultTime = plan.weighIn.timeFor(day),
                    title = "Wiegen",
                    text = "Morgengewicht nach dem Aufstehen eintragen (nüchtern, nach dem WC).",
                ),
            )
            val dayPlan = plan.day(day)
            dayPlan.meals.forEach { meal ->
                val t = meal.localTime ?: return@forEach
                add(
                    AlarmDef(
                        id = "essen-${meal.id}",
                        kind = AlarmKind.MEAL,
                        day = day,
                        defaultTime = t.minusMinutes(d.mealReminderLeadMinutes.toLong()),
                        title = "Essen um ${meal.time}",
                        text = meal.content,
                    ),
                )
            }
            dayPlan.sessions.filter { it.training }.forEach { s ->
                val t = s.startTime ?: return@forEach
                add(
                    AlarmDef(
                        id = "training-${s.id}",
                        kind = AlarmKind.TRAINING,
                        day = day,
                        defaultTime = t.minusMinutes(d.trainingReminderLeadMinutes.toLong()),
                        title = "Training in ${d.trainingReminderLeadMinutes} min, Snack gegessen?",
                        text = "${s.start} ${s.title}",
                    ),
                )
            }
        }
        plan.reminders.forEach { r ->
            add(AlarmDef("fix-${r.id}", AlarmKind.FIXED, DayOfWeek.valueOf(r.day), LocalTime.parse(r.time), r.title, r.text))
        }
        add(
            AlarmDef(
                id = CHECK_IN_ID,
                kind = AlarmKind.CHECK_IN,
                day = plan.checkIn.dayOfWeek,
                defaultTime = plan.checkIn.localTime,
                title = "Wochen-Check-in",
                text = "Schlaf, Stimmung, Libido, Infekt, Kraft – dauert 1 Minute.",
            ),
        )
    }

    /** Nächster Zeitpunkt (streng nach [now]) an Wochentag [day] um [time]. */
    fun nextTrigger(day: DayOfWeek, time: LocalTime, now: LocalDateTime): LocalDateTime {
        var date = now.toLocalDate()
        val diff = (day.value - date.dayOfWeek.value + 7) % 7
        date = date.plusDays(diff.toLong())
        var candidate = LocalDateTime.of(date, time)
        if (!candidate.isAfter(now)) candidate = candidate.plusWeeks(1)
        return candidate
    }
}

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
