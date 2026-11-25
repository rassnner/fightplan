package at.fightplan

import android.app.Application
import android.content.Context
import at.fightplan.data.BackupManager
import at.fightplan.data.FightDatabase
import at.fightplan.data.PlanRepository
import at.fightplan.data.SettingsRepository
import at.fightplan.domain.Plan
import at.fightplan.widget.WidgetRefresh

/** Einfache manuelle Dependency Injection. */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    val plan: Plan by lazy { PlanRepository.load(appContext) }
    val db: FightDatabase by lazy { FightDatabase.create(appContext) }
    val settings: SettingsRepository by lazy { SettingsRepository(appContext) }
    val backup: BackupManager by lazy { BackupManager(appContext, db.dao(), settings) }
}

class FightPlanApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        WidgetRefresh.scheduleNext(this, container.plan)
    }
}

val Context.container: AppContainer get() = (applicationContext as FightPlanApp).container
