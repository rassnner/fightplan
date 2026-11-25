package at.fightplan.app

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.view.drawToBitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import at.fightplan.domain.TestPlan
import at.fightplan.ui.FightPlanTheme
import at.fightplan.ui.SectionCard
import at.fightplan.ui.SessionDetails
import androidx.compose.material3.Text
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowLooper
import java.io.File

/** Rendert die Übungsliste von Full Body 2 als Bild (Sichtprüfung). */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "w411dp-h1400dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SessionScreenshotTest {
    @Test
    fun fullBody2() {
        val session = TestPlan.plan.days.flatMap { it.sessions }.first { it.id == "mo-kraft" }
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        activity.setContent {
            FightPlanTheme {
                Surface {
                    Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp)) {
                        SectionCard { Text(session.title); SessionDetails(session) }
                    }
                }
            }
        }
        ShadowLooper.idleMainLooper()
        val bmp = activity.window.decorView.rootView.drawToBitmap()
        File("build/screenshots").mkdirs()
        File("build/screenshots/6-full-body-2.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
