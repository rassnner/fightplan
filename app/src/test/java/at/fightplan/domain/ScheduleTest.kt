package at.fightplan.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime

class ScheduleTest {
    private val plan = TestPlan.plan
    private val alarms = AlarmPlanner.buildAlarms(plan)
    private fun alarm(id: String) = alarms.first { it.id == id }

    @Test
    fun weighInTimes() {
        assertEquals(LocalTime.of(6, 0), alarm("wiegen-monday").defaultTime)
        DayOfWeek.entries.filter { it != DayOfWeek.MONDAY }.forEach {
            assertEquals(LocalTime.of(6, 15), alarm("wiegen-${it.name.lowercase()}").defaultTime)
        }
    }

    @Test
    fun mealRemindersTenMinutesBefore() {
        val a = alarm("essen-mo-1")
        assertEquals(LocalTime.of(6, 5), a.defaultTime)
        assertEquals("Banane mit Honig und 500 ml Wasser mit Salz", a.text)
        // Essen ohne Uhrzeit (Mi Pause, So ganztags) bekommen keinen Alarm
        assertTrue(alarms.none { it.id == "essen-mi-2" || it.id == "essen-so-1" })
        val mealCount = plan.days.sumOf { d -> d.meals.count { it.time != null } }
        assertEquals(mealCount, alarms.count { it.kind == AlarmKind.MEAL })
    }

    @Test
    fun trainingReminders45MinutesBefore() {
        assertEquals(LocalTime.of(6, 15), alarm("training-mo-kraft-a").defaultTime)
        assertEquals(LocalTime.of(18, 45), alarm("training-di-mt").defaultTime)
        assertEquals(LocalTime.of(16, 45), alarm("training-fr-sparring").defaultTime)
        assertEquals("Training in 45 min, Snack gegessen?", alarm("training-sa-power").title)
        // Ruhetag hat keine Trainingserinnerung
        assertEquals(8, alarms.count { it.kind == AlarmKind.TRAINING })
    }

    @Test
    fun fixedRemindersAndCheckIn() {
        alarm("fix-di-bett").let { assertEquals(DayOfWeek.TUESDAY, it.day); assertEquals(LocalTime.of(22, 15), it.defaultTime) }
        alarm("fix-mi-licht").let { assertEquals(DayOfWeek.WEDNESDAY, it.day); assertEquals(LocalTime.of(21, 45), it.defaultTime) }
        alarm("checkin").let { assertEquals(DayOfWeek.SUNDAY, it.day); assertEquals(LocalTime.of(18, 0), it.defaultTime) }
    }

    @Test
    fun idsUnique() {
        assertEquals(alarms.size, alarms.map { it.id }.toSet().size)
    }

    @Test
    fun overrideAndDisable() {
        val s = AppSettings(alarmTimeOverrides = mapOf("checkin" to "19:30"), disabledAlarms = setOf("fix-di-bett"))
        assertEquals(LocalTime.of(19, 30), alarm("checkin").effectiveTime(s))
        assertTrue(!alarm("fix-di-bett").isEnabled(s))
    }

    @Test
    fun nextTrigger() {
        val wedNoon = LocalDateTime.of(2026, 11, 25, 12, 0) // Mittwoch
        assertEquals(LocalDateTime.of(2026, 11, 25, 21, 45), AlarmPlanner.nextTrigger(DayOfWeek.WEDNESDAY, LocalTime.of(21, 45), wedNoon))
        assertEquals(LocalDateTime.of(2026, 12, 2, 6, 15), AlarmPlanner.nextTrigger(DayOfWeek.WEDNESDAY, LocalTime.of(6, 15), wedNoon))
        assertEquals(LocalDateTime.of(2026, 11, 30, 6, 0), AlarmPlanner.nextTrigger(DayOfWeek.MONDAY, LocalTime.of(6, 0), wedNoon))
        // genau jetzt -> nächste Woche
        assertEquals(LocalDateTime.of(2026, 12, 2, 12, 0), AlarmPlanner.nextTrigger(DayOfWeek.WEDNESDAY, LocalTime.NOON, wedNoon))
    }

    @Test
    fun agenda() {
        val wedNoon = LocalDateTime.of(2026, 11, 25, 12, 0)
        assertEquals(LocalDateTime.of(2026, 11, 25, 18, 0), Agenda.nextSession(plan, wedNoon)!!.at)
        assertEquals(LocalDateTime.of(2026, 11, 25, 15, 45), Agenda.nextMeal(plan, wedNoon)!!.at)
        // Samstag nach dem letzten Termin -> Montag 07:00 (Sonntag hat keine Uhrzeit)
        val satNight = LocalDateTime.of(2026, 11, 28, 21, 0)
        assertEquals(LocalDateTime.of(2026, 11, 30, 7, 0), Agenda.nextSession(plan, satNight)!!.at)
    }

    @Test
    fun widgetUpdates() {
        val wedNoon = LocalDateTime.of(2026, 11, 25, 12, 0)
        assertEquals(LocalDateTime.of(2026, 11, 25, 15, 45), Agenda.nextWidgetUpdate(plan, wedNoon))
        val wedLate = LocalDateTime.of(2026, 11, 25, 20, 30)
        assertEquals(LocalDateTime.of(2026, 11, 26, 0, 5), Agenda.nextWidgetUpdate(plan, wedLate))
    }
}
