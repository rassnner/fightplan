package at.fightplan.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class WeightStatsTest {
    private val start = LocalDate.of(2026, 11, 2) // Montag

    /** Täglich ein Wert, linear sinkend um [perWeek] kg pro Woche. */
    fun linear(days: Int, startKg: Double, perWeek: Double) =
        (0 until days).map { WeightPoint(start.plusDays(it.toLong()), startKg - perWeek / 7.0 * it) }

    @Test
    fun rollingAverageUsesLastSevenDays() {
        val pts = listOf(80.0, 79.0, 78.0, 77.0, 76.0, 75.0, 74.0, 73.0)
            .mapIndexed { i, kg -> WeightPoint(start.plusDays(i.toLong()), kg) }
        // Tage 1..7: 79..73 -> Schnitt 76
        assertEquals(76.0, WeightStats.rollingAverage(pts, start.plusDays(7))!!, 1e-9)
    }

    @Test
    fun rollingAverageWithGaps() {
        val pts = listOf(WeightPoint(start, 78.0), WeightPoint(start.plusDays(3), 77.0))
        assertEquals(77.5, WeightStats.rollingAverage(pts, start.plusDays(6))!!, 1e-9)
        // Tag 7: nur noch der Eintrag von Tag 3 im Fenster
        assertEquals(77.0, WeightStats.rollingAverage(pts, start.plusDays(7))!!, 1e-9)
        assertNull(WeightStats.rollingAverage(pts, start.plusDays(10)))
    }

    @Test
    fun minEntriesRespected() {
        val pts = listOf(WeightPoint(start, 78.0), WeightPoint(start.plusDays(1), 77.8))
        assertNull(WeightStats.rollingAverage(pts, start.plusDays(1), minEntries = 3))
    }

    @Test
    fun weeklyLossOverTwoWeeks() {
        val pts = linear(21, 78.0, 0.5)
        val loss = WeightStats.weeklyLoss(pts, start.plusDays(20), 2)!!
        assertEquals(0.5, loss, 1e-9)
    }

    @Test
    fun weeklyLossNeedsData() {
        val pts = linear(10, 78.0, 0.5)
        assertNull(WeightStats.weeklyLoss(pts, start.plusDays(9), 2))
    }

    @Test
    fun weightGainIsNegativeLoss() {
        val pts = linear(21, 76.0, -0.3)
        assertEquals(-0.3, WeightStats.weeklyLoss(pts, start.plusDays(20), 2)!!, 1e-9)
    }

    @Test
    fun basisWeightFallbacks() {
        assertEquals(78.0, WeightStats.basisWeight(emptyList(), start, 78.0), 1e-9)
        val pts = listOf(WeightPoint(start, 77.0), WeightPoint(start.plusDays(1), 76.0))
        // Lange keine Messung: Schnitt um den letzten Eintrag
        assertEquals(76.5, WeightStats.basisWeight(pts, start.plusDays(30), 78.0), 1e-9)
        assertEquals(76.5, WeightStats.basisWeight(pts, start.plusDays(2), 78.0), 1e-9)
    }

    @Test
    fun averageSeries() {
        val pts = listOf(WeightPoint(start, 78.0), WeightPoint(start.plusDays(1), 77.0))
        val series = WeightStats.averageSeries(pts)
        assertEquals(listOf(78.0, 77.5), series.map { it.kg })
    }

    @Test
    fun dayTargetsUseAverage() {
        val plan = TestPlan.plan
        val pts = linear(7, 78.0, 0.0)
        val t = DayTargetCalculator.targets(plan, AppSettings(), pts, start.plusDays(6)) // Sonntag
        assertEquals("REST", t.typeKey)
        assertEquals(2305, t.macros.kcal)
        val withOffset = DayTargetCalculator.targets(plan, AppSettings(kcalOffsets = mapOf("REST" to -150)), pts, start.plusDays(6))
        assertEquals(2155, withOffset.macros.kcal)
    }
}
