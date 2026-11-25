package at.fightplan.app

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import at.fightplan.MainActivity
import at.fightplan.widget.TodayWidget
import at.fightplan.widget.WidgetRefreshReceiver
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** Startet die echte App in Robolectric und klickt alle Tabs durch. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class AppSmokeTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun allScreensRender() {
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Morgengewicht").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Makro-Ziele heute").assertExists()
        compose.onNodeWithText("Woche").performClick()
        compose.onNodeWithText("Wochenplan").assertExists()
        // Krafteinheit antippen -> Übungsdialog
        compose.onAllNodesWithText("Kraft: Full Body 2")[0].performClick()
        compose.onNodeWithText("Schließen").performClick()
        compose.onAllNodesWithText("Gewicht")[0].performClick()
        compose.onNodeWithText("Verlauf").assertExists()
        compose.onAllNodesWithText("Check-in")[0].performClick()
        compose.onNodeWithText("Wochen-Check-in").assertExists()
        compose.onNodeWithText("Mehr").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Einstellungen").fetchSemanticsNodes().isNotEmpty() }
    }

    @Test
    fun onlyTheSilentWidgetRefreshIsScheduled() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val am = shadowOf(ctx.getSystemService(AlarmManager::class.java))
        assertEquals(1, am.scheduledAlarms.size)
        assertEquals(AlarmManager.RTC, am.scheduledAlarms[0].type)
        WidgetRefreshReceiver().onReceive(ctx, Intent())
        runBlocking { TodayWidget.updateAll(ctx) }
        compose.waitForIdle()
    }
}
