package at.fightplan.app

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import at.fightplan.container
import at.fightplan.data.CheckInEntity
import at.fightplan.data.MealCheckEntity
import at.fightplan.data.WaistEntity
import at.fightplan.data.WeightEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.File

/** Export über den echten BackupManager (Room + DataStore), alles löschen, importieren, vergleichen. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class BackupRoundTripTest {
    @Test
    fun exportImportRestoresIdenticalData() = runBlocking {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val c = ctx.container
        val dao = c.db.dao()
        dao.upsertWeight(WeightEntity(20500, 78.1))
        dao.upsertWeight(WeightEntity(20501, 77.85))
        dao.upsertWaist(WaistEntity(20500, 82.0))
        dao.upsertCheckIn(CheckInEntity(20503, 7.5, 2, false, true, true, 48.0))
        dao.insertMealCheck(MealCheckEntity(20500, "mo-1"))
        c.settings.update {
            it.copy(goalWeightKg = 73.5, alarmTimeOverrides = mapOf("checkin" to "19:15"),
                disabledAlarms = setOf("fix-mi-licht"), kcalOffsets = mapOf("NORMAL" to -150))
        }
        val before = c.backup.snapshot()

        val file = File(ctx.cacheDir, "backup.json")
        c.backup.export(Uri.fromFile(file))
        assertTrue(file.readText().contains("fightplan-backup"))

        // Alles löschen bzw. verändern
        dao.replaceAll(emptyList(), emptyList(), emptyList(), emptyList())
        c.settings.replace(at.fightplan.domain.AppSettings())
        assertEquals(0, dao.allWeights().size)

        c.backup.import(Uri.fromFile(file))
        val after = c.backup.snapshot()
        assertEquals(before.copy(exportedAt = ""), after.copy(exportedAt = ""))
    }
}
