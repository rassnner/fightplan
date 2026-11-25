package at.fightplan.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime

class ScheduleTest {
    private val plan = TestPlan.plan

    @Test
    fun agenda() {
        val wedNoon = LocalDateTime.of(2026, 11, 25, 12, 0) // Mittwoch
        assertEquals(LocalDateTime.of(2026, 11, 25, 18, 0), Agenda.nextSession(plan, wedNoon)!!.at)
        assertEquals(LocalDateTime.of(2026, 11, 25, 15, 45), Agenda.nextMeal(plan, wedNoon)!!.at)
        // Mittwochabend nach dem Training -> Donnerstag 06:35 Sprints
        val wedNight = LocalDateTime.of(2026, 11, 25, 21, 0)
        assertEquals(LocalDateTime.of(2026, 11, 26, 6, 35), Agenda.nextSession(plan, wedNight)!!.at)
        // Samstag nach dem letzten Termin -> Montag 06:50 (Sonntag hat keine Uhrzeit)
        val satNight = LocalDateTime.of(2026, 11, 28, 21, 0)
        assertEquals(LocalDateTime.of(2026, 11, 30, 6, 50), Agenda.nextSession(plan, satNight)!!.at)
    }

    @Test
    fun widgetUpdates() {
        val wedNoon = LocalDateTime.of(2026, 11, 25, 12, 0)
        assertEquals(LocalDateTime.of(2026, 11, 25, 15, 45), Agenda.nextWidgetUpdate(plan, wedNoon))
        val wedLate = LocalDateTime.of(2026, 11, 25, 20, 30)
        assertEquals(LocalDateTime.of(2026, 11, 26, 0, 5), Agenda.nextWidgetUpdate(plan, wedLate))
    }
}
