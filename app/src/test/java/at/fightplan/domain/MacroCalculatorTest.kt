package at.fightplan.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek

class MacroCalculatorTest {
    private val plan = TestPlan.plan

    @Test
    fun doubleDayAt78kg() {
        val m = MacroCalculator.forDay(78.0, plan.dayTypes.getValue("DOUBLE"))
        assertEquals(429, m.carbsG)      // 5,5 × 78
        assertEquals(172, m.proteinG)    // 2,2 × 78 = 171,6
        assertEquals(62, m.fatG)         // 0,8 × 78 = 62,4
        assertEquals(2964, m.kcal)       // 4·429 + 4·171,6 + 9·62,4
    }

    @Test
    fun restDayAt78kg() {
        val m = MacroCalculator.forDay(78.0, plan.dayTypes.getValue("REST"))
        assertEquals(234, m.carbsG)
        assertEquals(70, m.fatG)         // 0,9 × 78 = 70,2
        assertEquals(2254, m.kcal)       // 936 + 686,4 + 631,8
    }

    @Test
    fun sparringAndNormalAt78kg() {
        assertEquals(2808, MacroCalculator.forDay(78.0, plan.dayTypes.getValue("SPARRING")).kcal)
        assertEquals(2652, MacroCalculator.forDay(78.0, plan.dayTypes.getValue("NORMAL")).kcal)
    }

    @Test
    fun weeklyAverageIsAbout2700() {
        val avg = MacroCalculator.weeklyAverageKcal(plan, 78.0)
        assertEquals(2706.6, avg, 1.0)
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
        assertEquals(351 - 37.5, minus.carbsG.toDouble(), 0.6)
    }

    @Test
    fun carbsNeverNegative() {
        val m = MacroCalculator.forDay(50.0, DayType("x", 0.5, 2.0, 1.0), -5000)
        assertEquals(0, m.carbsG)
    }

    @Test
    fun dayTypesMatchWeekdays() {
        assertEquals("DOUBLE", plan.day(DayOfWeek.MONDAY).type)
        assertEquals("DOUBLE", plan.day(DayOfWeek.SATURDAY).type)
        assertEquals("SPARRING", plan.day(DayOfWeek.FRIDAY).type)
        listOf(DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY).forEach {
            assertEquals("NORMAL", plan.day(it).type)
        }
        assertEquals("REST", plan.day(DayOfWeek.SUNDAY).type)
    }
}
