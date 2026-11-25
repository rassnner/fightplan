package at.fightplan.app

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.core.view.drawToBitmap
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import at.fightplan.MainActivity
import at.fightplan.container
import at.fightplan.data.CheckInEntity
import at.fightplan.data.WeightEntity
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.LocalDate

/** Erzeugt Screenshots aller Tabs nach app/build/screenshots (zur Sichtprüfung, keine Assertion). */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ScreenshotTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun captureTabs() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        runBlocking {
            val dao = ctx.container.db.dao()
            val today = LocalDate.now()
            for (i in 0..20) {
                val kg = 78.0 - 0.2 / 7 * (20 - i) * -1 - 0.2 * 3 + (if (i % 3 == 0) 0.3 else -0.1)
                dao.upsertWeight(WeightEntity(today.minusDays(20L - i).toEpochDay(), kg))
            }
            dao.upsertCheckIn(CheckInEntity(today.minusDays(7).toEpochDay(), 7.0, 2, true, false, true, null))
        }
        val out = File("build/screenshots").apply { mkdirs() }
        fun shot(name: String) {
            compose.waitForIdle()
            val bmp = compose.activity.window.decorView.rootView.drawToBitmap()
            File(out, "$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Morgengewicht").fetchSemanticsNodes().isNotEmpty() }
        shot("1-heute")
        listOf("Woche" to "2-woche", "Gewicht" to "3-gewicht", "Check-in" to "4-checkin", "Mehr" to "5-einstellungen")
            .forEach { (tab, name) ->
                compose.onAllNodesWithText(tab)[0].performClick()
                shot(name)
            }
    }
}
