package at.fightplan.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class AdjustmentRuleTest {
    private val plan = TestPlan.plan
    private val start = LocalDate.of(2026, 11, 2)
    private val today = start.plusDays(20)

    private fun linear(perWeek: Double, startKg: Double = 78.0) =
        (0 until 21).map { WeightPoint(start.plusDays(it.toLong()), startKg - perWeek / 7.0 * it) }

    private fun eval(perWeek: Double, startKg: Double = 78.0, goal: Double = 74.0) =
        AdjustmentRule.evaluate(linear(perWeek, startKg), today, goal, plan.defaults)

    @Test
    fun tooSlowDecreasesNormalAndRest() {
        val s = eval(0.2)
        assertTrue(s is Suggestion.Decrease)
        s as Suggestion.Decrease
        assertEquals(150, s.kcal)
        assertEquals(listOf("NORMAL", "REST"), s.dayTypes)
        assertEquals(0.2, s.lossPerWeek, 1e-9)
    }

    @Test
    fun gainingAlsoDecreases() {
        assertTrue(eval(-0.2) is Suggestion.Decrease)
    }

    @Test
    fun tooFastIncreases() {
        val s = eval(0.9)
        assertTrue(s is Suggestion.Increase)
        assertEquals(200, (s as Suggestion.Increase).kcal)
    }

    @Test
    fun inRangeKeeps() {
        assertTrue(eval(0.5) is Suggestion.Keep)
        assertTrue(eval(0.3) is Suggestion.Keep)   // Grenze: nicht "unter 0,3"
        assertTrue(eval(0.8) is Suggestion.Keep)   // Grenze: nicht "über 0,8"
    }

    @Test
    fun notEnoughData() {
        val pts = (0 until 5).map { WeightPoint(start.plusDays(it.toLong()), 78.0) }
        assertEquals(Suggestion.NotEnoughData, AdjustmentRule.evaluate(pts, start.plusDays(4), 74.0, plan.defaults))
    }

    @Test
    fun goalReachedWins() {
        val s = eval(0.5, startKg = 75.0, goal = 74.0)
        assertTrue(s is Suggestion.GoalReached)
        assertEquals(350, (s as Suggestion.GoalReached).extraKcal)
    }

    @Test
    fun applyDecrease() {
        val s = Suggestion.Decrease(0.1, 150, listOf("NORMAL", "REST"))
        val r = AdjustmentRule.apply(s, mapOf("REST" to -150), plan.dayTypes.keys)
        assertEquals(mapOf("REST" to -300, "NORMAL" to -150), r)
    }

    @Test
    fun applyIncreaseAndGoal() {
        val r = AdjustmentRule.apply(Suggestion.Increase(1.0, 200), mapOf("NORMAL" to -150), plan.dayTypes.keys)
        assertEquals(mapOf("DOUBLE" to 200, "SPARRING" to 200, "NORMAL" to 50, "REST" to 200), r)
        val g = AdjustmentRule.apply(Suggestion.GoalReached(74.0, 350), emptyMap(), plan.dayTypes.keys)
        assertTrue(g.values.all { it == 350 })
    }

    @Test
    fun cooldown() {
        val d = LocalDate.of(2026, 11, 20)
        assertTrue(AdjustmentRule.isDue(null, d))
        assertFalse(AdjustmentRule.isDue(d.minusDays(13).toEpochDay(), d))
        assertTrue(AdjustmentRule.isDue(d.minusDays(14).toEpochDay(), d))
    }
}
