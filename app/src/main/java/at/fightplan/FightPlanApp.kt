package at.fightplan

import android.app.Application
import android.content.Context
import at.fightplan.alarm.AlarmScheduler
import at.fightplan.alarm.Notifications
import at.fightplan.data.BackupManager
import at.fightplan.data.FightDatabase
import at.fightplan.data.PlanRepository
import at.fightplan.data.SettingsRepository
import at.fightplan.domain.Plan
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Einfache manuelle Dependency Injection. */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    val plan: Plan by lazy { PlanRepository.load(appContext) }
    val db: FightDatabase by lazy { FightDatabase.create(appContext) }
    val settings: SettingsRepository by lazy { SettingsRepository(appContext) }
    val alarms: AlarmScheduler by lazy { AlarmScheduler(appContext) }
    val backup: BackupManager by lazy { BackupManager(appContext, db.dao(), settings) }
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Plant alle Alarme und das Widget-Update neu. */
    suspend fun rescheduleAll() {
        alarms.rescheduleAll(plan, settings.current())
    }
}

class FightPlanApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        Notifications.createChannels(this)
        container.scope.launch { container.rescheduleAll() }
    }
}

val Context.container: AppContainer get() = (applicationContext as FightPlanApp).container
