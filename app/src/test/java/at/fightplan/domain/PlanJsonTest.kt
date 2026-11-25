package at.fightplan.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.time.DayOfWeek

class PlanJsonTest {
    private val plan = TestPlan.plan

    @Test
    fun startValues() {
        assertEquals(78.0, plan.defaults.startWeightKg, 0.0)
        assertEquals(74.0, plan.defaults.goalWeightKg, 0.0)
        assertEquals(0.4, plan.defaults.targetLossMinKgPerWeek, 0.0)
        assertEquals(0.6, plan.defaults.targetLossMaxKgPerWeek, 0.0)
    }

    @Test
    fun allDaysPresent() {
        DayOfWeek.entries.forEach { assertNotNull(plan.day(it)) }
        assertEquals(2, plan.day(DayOfWeek.MONDAY).sessions.size)
        assertEquals(2, plan.day(DayOfWeek.SATURDAY).sessions.size)
        assertEquals("13:25", plan.day(DayOfWeek.MONDAY).school!!.end)
    }

    @Test(expected = IllegalArgumentException::class)
    fun invalidPlanRejected() {
        Plan.parse(
            """{"version":1,"defaults":{"startWeightKg":78,"goalWeightKg":74,"targetLossMinKgPerWeek":0.4,
               "targetLossMaxKgPerWeek":0.6},"dayTypes":{},"days":[],"weighIn":{"defaultTime":"06:15"},
               "checkIn":{"day":"SUNDAY","time":"18:00"}}""",
        )
    }
}
