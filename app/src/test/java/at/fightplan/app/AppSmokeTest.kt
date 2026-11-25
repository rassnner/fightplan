package at.fightplan.app

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import at.fightplan.MainActivity
import at.fightplan.alarm.AlarmReceiver
import at.fightplan.alarm.BootReceiver
import at.fightplan.container
import at.fightplan.domain.AlarmPlanner
import at.fightplan.widget.TodayWidget
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** Startet die echte App in Robolectric, klickt alle Tabs durch und prüft die Alarmplanung. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class AppSmokeTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun allScreensRender() {
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Morgengewicht").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Morgengewicht").assertExists()
        compose.onNodeWithText("Makro-Ziele heute").assertExists()
        compose.onNodeWithText("Woche").performClick()
        compose.onNodeWithText("Wochenplan").assertExists()
        compose.onAllNodesWithText("Gewicht")[0].performClick()
        compose.onNodeWithText("Verlauf").assertExists()
        compose.onAllNodesWithText("Check-in")[0].performClick()
        compose.onNodeWithText("Wochen-Check-in").assertExists()
        compose.onNodeWithText("Mehr").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Einstellungen").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Prüfen").performScrollTo().performClick()
        compose.onNodeWithText("Exakte Alarme").assertExists()
    }

    @Test
    fun alarmsScheduledAndReceiversRun() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        runBlocking { ctx.container.rescheduleAll() }
        val am = shadowOf(ctx.getSystemService(AlarmManager::class.java))
        val expected = AlarmPlanner.buildAlarms(ctx.container.plan).size + 1 // + Widget-Tick
        assertTrue("geplant: ${am.scheduledAlarms.size}, erwartet $expected", am.scheduledAlarms.size == expected)

        // Alarm auslösen und Boot-Receiver ausführen: dürfen nicht abstürzen.
        AlarmReceiver().onReceive(
            ctx,
            Intent(AlarmReceiver.ACTION_ALARM).putExtra(AlarmReceiver.EXTRA_ID, "checkin"),
        )
        BootReceiver().onReceive(ctx, Intent(Intent.ACTION_BOOT_COMPLETED))
        runBlocking { TodayWidget.updateAll(ctx) }
        compose.waitForIdle()
        compose.onAllNodesWithText("Heute").fetchSemanticsNodes()
    }
}
