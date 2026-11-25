package at.fightplan.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
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
        assertEquals("13:25", plan.day(DayOfWeek.MONDAY).school!!.end)
    }

    @Test
    fun tenTrainingUnits() {
        val training = plan.days.flatMap { it.sessions }.filter { it.training }
        assertEquals(10, training.size)
        assertTrue(training.all { it.start != null && it.end != null })
    }

    @Test
    fun strengthDaysSwapped() {
        val mo = plan.day(DayOfWeek.MONDAY).sessions.first()
        assertEquals("Kraft: Full Body 2", mo.title)
        assertEquals("06:50", mo.start)
        assertEquals(9, mo.exercises.size)
        assertEquals("Kreuzheben (Conventional oder Trap Bar)", mo.exercises[4].name)
        assertEquals("1×5", mo.exercises[4].sets)
        assertTrue(mo.exercises[4].dp)
        assertFalse(mo.exercises[0].dp)

        val doKraft = plan.day(DayOfWeek.THURSDAY).sessions.first { it.id == "do-kraft" }
        assertEquals("Kraft: Full Body 1", doKraft.title)
        assertEquals(9, doKraft.exercises.size)
        assertEquals("Pendlay Rows", doKraft.exercises[5].name)
    }

    @Test
    fun freeMorningsAreNotTraining() {
        listOf(DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY, DayOfWeek.SUNDAY).forEach { d ->
            val first = plan.day(d).sessions.first()
            assertFalse(first.training)
            assertEquals(null, first.start)
        }
    }

    @Test
    fun guidelines() {
        assertEquals(5, plan.guidelines.size)
        assertTrue(plan.guidelines.any { it.title.startsWith("Deload") })
    }

    @Test(expected = IllegalArgumentException::class)
    fun invalidPlanRejected() {
        Plan.parse(
            """{"version":1,"defaults":{"startWeightKg":78,"goalWeightKg":74,"targetLossMinKgPerWeek":0.4,
               "targetLossMaxKgPerWeek":0.6},"dayTypes":{},"days":[]}""",
        )
    }
}
