package at.fightplan.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek

class MacroCalculatorTest {
    private val plan = TestPlan.plan

    @Test
    fun doubleDayAt78kg() {
        val m = MacroCalculator.forDay(78.0, plan.dayTypes.getValue("DOUBLE"))
        assertEquals(520, m.carbsG)
        assertEquals(160, m.proteinG)
        assertEquals(55, m.fatG)
        assertEquals(3215, m.kcal)
    }

    @Test
    fun normalDayAt78kg() {
        val m = MacroCalculator.forDay(78.0, plan.dayTypes.getValue("NORMAL"))
        assertEquals(405, m.carbsG)
        assertEquals(160, m.proteinG)
        assertEquals(60, m.fatG)
        assertEquals(2800, m.kcal)
    }

    @Test
    fun restDayAt78kg() {
        val m = MacroCalculator.forDay(78.0, plan.dayTypes.getValue("REST"))
        assertEquals(270, m.carbsG)
        assertEquals(160, m.proteinG)
        assertEquals(65, m.fatG)
        assertEquals(2305, m.kcal)
    }

    @Test
    fun macrosScaleWithWeight() {
        val m = MacroCalculator.forDay(74.0, plan.dayTypes.getValue("DOUBLE"))
        assertEquals(152, m.proteinG) // 2,0513 × 74
        assertEquals(493, m.carbsG)   // 6,6667 × 74
    }

    @Test
    fun weeklyAverage() {
        // 4 Doppeltage, 2 Normaltage, 1 Ruhetag
        assertEquals((4 * 3215 + 2 * 2800 + 2305) / 7.0, MacroCalculator.weeklyAverageKcal(plan, 78.0), 1.0)
    }

    @Test
    fun kcalFormula() {
        val m = MacroCalculator.forDay(70.0, DayType("x", 4.0, 2.0, 1.0))
        assertEquals(4 * 280 + 4 * 140 + 9 * 70, m.kcal)
    }

    @Test
    fun offsetGoesThroughCarbs() {
        val type = plan.dayTypes.getValue("NORMAL")
        val base = MacroCalculator.forDay(78.0, type)
        val minus = MacroCalculator.forDay(78.0, type, -150)
        assertEquals(base.kcal - 150, minus.kcal)
        assertEquals(base.proteinG, minus.proteinG)
        assertEquals(base.fatG, minus.fatG)
        assertEquals(405 - 37.5, minus.carbsG.toDouble(), 0.6)
    }

    @Test
    fun carbsNeverNegative() {
        val m = MacroCalculator.forDay(50.0, DayType("x", 0.5, 2.0, 1.0), -5000)
        assertEquals(0, m.carbsG)
    }

    @Test
    fun dayTypesMatchWeekdays() {
        listOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.THURSDAY, DayOfWeek.SATURDAY).forEach {
            assertEquals("DOUBLE", plan.day(it).type)
        }
        listOf(DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY).forEach { assertEquals("NORMAL", plan.day(it).type) }
        assertEquals("REST", plan.day(DayOfWeek.SUNDAY).type)
    }

    @Test
    fun dayTypeFollowsTrainingCount() {
        // Doppeltag = 2 Einheiten, Normaltag = 1, Ruhetag = 0
        val expected = mapOf(2 to "DOUBLE", 1 to "NORMAL", 0 to "REST")
        plan.days.forEach { d -> assertEquals(d.day, expected[d.sessions.count { it.training }], d.type) }
    }
}
