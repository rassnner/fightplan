package at.fightplan.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WarningSignsTest {
    private val ok = CheckInData(sleepHours = 8.5, mood = 4, libidoNormal = true, infection = false, strengthOk = true)

    @Test
    fun noWarnings() {
        val r = WarningSigns.evaluate(ok)
        assertEquals(0, r.count)
        assertFalse(r.red)
        assertFalse(r.seeDoctor)
    }

    @Test
    fun eachSignCounts() {
        assertEquals(1, WarningSigns.evaluate(ok.copy(sleepHours = 7.9)).count)
        assertEquals(0, WarningSigns.evaluate(ok.copy(sleepHours = 8.0)).count)
        assertEquals(1, WarningSigns.evaluate(ok.copy(mood = 2)).count)
        assertEquals(1, WarningSigns.evaluate(ok.copy(mood = 1)).count)
        assertEquals(0, WarningSigns.evaluate(ok.copy(mood = 3)).count)
        assertEquals(1, WarningSigns.evaluate(ok.copy(libidoNormal = false)).count)
        assertEquals(1, WarningSigns.evaluate(ok.copy(infection = true)).count)
        assertEquals(1, WarningSigns.evaluate(ok.copy(strengthOk = false)).count)
    }

    @Test
    fun oneSignIsNotRed() {
        assertFalse(WarningSigns.evaluate(ok.copy(sleepHours = 7.0)).red)
    }

    @Test
    fun twoSignsAreRed() {
        val r = WarningSigns.evaluate(ok.copy(sleepHours = 7.0, strengthOk = false))
        assertEquals(2, r.count)
        assertTrue(r.red)
        assertFalse(r.seeDoctor)
    }

    @Test
    fun allSigns() {
        val r = WarningSigns.evaluate(CheckInData(6.0, 1, false, true, false))
        assertEquals(5, r.count)
        assertTrue(r.red)
        assertTrue(r.seeDoctor)
    }

    @Test
    fun doctorHintForLibidoOrInfection() {
        assertTrue(WarningSigns.evaluate(ok.copy(libidoNormal = false)).seeDoctor)
        assertTrue(WarningSigns.evaluate(ok.copy(infection = true)).seeDoctor)
    }

    @Test
    fun jumpHeightIsIgnored() {
        assertEquals(0, WarningSigns.evaluate(ok.copy(jumpCm = 10.0)).count)
    }
}
