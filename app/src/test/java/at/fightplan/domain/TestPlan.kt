package at.fightplan.domain

import java.io.File

/** Lädt den echten Plan aus app/src/main/assets (Unit-Tests laufen im Modulverzeichnis). */
object TestPlan {
    val plan: Plan by lazy {
        val f = listOf(File("src/main/assets/plan.json"), File("app/src/main/assets/plan.json")).first { it.exists() }
        Plan.parse(f.readText())
    }
}
