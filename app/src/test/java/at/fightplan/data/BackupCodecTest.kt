package at.fightplan.data

import at.fightplan.domain.AppSettings
import org.junit.Assert.assertEquals
import org.junit.Test

class BackupCodecTest {
    private val sample = BackupData(
        exportedAt = "2026-11-25T12:00:00",
        settings = AppSettings(
            startWeightKg = 78.0,
            goalWeightKg = 74.0,
            alarmTimeOverrides = mapOf("checkin" to "19:00"),
            disabledAlarms = setOf("fix-di-bett"),
            kcalOffsets = mapOf("NORMAL" to -150, "REST" to -150),
            lastAdjustmentDecisionEpochDay = 20500,
            maintenanceApplied = false,
        ),
        weights = listOf(WeightEntity(20500, 78.2), WeightEntity(20501, 77.95)),
        waist = listOf(WaistEntity(20500, 81.5)),
        checkIns = listOf(
            CheckInEntity(20503, 7.5, 3, true, false, true, null),
            CheckInEntity(20510, 8.25, 4, false, true, false, 52.5),
        ),
        mealChecks = listOf(MealCheckEntity(20500, "mo-1"), MealCheckEntity(20500, "mo-2")),
    )

    @Test
    fun roundTripIsIdentical() {
        val text = BackupCodec.encode(sample)
        assertEquals(sample, BackupCodec.decode(text))
        // zweimal kodieren ergibt denselben Text
        assertEquals(text, BackupCodec.encode(BackupCodec.decode(text)))
    }

    @Test
    fun emptyBackup() {
        val empty = BackupData(exportedAt = "x", settings = AppSettings(), weights = emptyList(), waist = emptyList(),
            checkIns = emptyList(), mealChecks = emptyList())
        assertEquals(empty, BackupCodec.decode(BackupCodec.encode(empty)))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsForeignJson() {
        BackupCodec.decode(BackupCodec.encode(sample).replace("fightplan-backup", "something-else"))
    }
}
