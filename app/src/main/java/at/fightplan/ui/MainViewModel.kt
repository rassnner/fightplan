package at.fightplan.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import at.fightplan.container
import at.fightplan.data.CheckInEntity
import at.fightplan.data.MealCheckEntity
import at.fightplan.data.WaistEntity
import at.fightplan.data.WeightEntity
import at.fightplan.domain.AdjustmentRule
import at.fightplan.domain.AppSettings
import at.fightplan.domain.DayTargetCalculator
import at.fightplan.domain.DayTargets
import at.fightplan.domain.Plan
import at.fightplan.domain.Suggestion
import at.fightplan.domain.WeightPoint
import at.fightplan.domain.WeightStats
import at.fightplan.widget.TodayWidget
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class AppState(
    val today: LocalDate,
    val plan: Plan,
    val settings: AppSettings,
    val weights: List<WeightPoint>,
    val waist: List<WaistEntity>,
    val checkIns: List<CheckInEntity>,
    val checkedMeals: Set<String>,
) {
    val targets: DayTargets = DayTargetCalculator.targets(plan, settings, weights, today)
    val todayWeight: Double? = weights.firstOrNull { it.date == today }?.kg
    val average: Double? = WeightStats.rollingAverage(weights, today)
    val lossLastWeek: Double? = WeightStats.weeklyLoss(weights, today, 1)
    val lossTwoWeeks: Double? = WeightStats.weeklyLoss(weights, today, plan.defaults.rules.compareWeeks)
    val suggestion: Suggestion =
        AdjustmentRule.evaluate(weights, today, settings.goalWeight(plan), plan.defaults)

    /** Vorschlag, der dem Benutzer zur Bestätigung angezeigt wird (oder null). */
    val pendingSuggestion: Suggestion?
        get() = when (val s = suggestion) {
            is Suggestion.GoalReached -> if (settings.maintenanceApplied) null else s
            is Suggestion.Decrease, is Suggestion.Increase ->
                if (AdjustmentRule.isDue(settings.lastAdjustmentDecisionEpochDay, today)) s else null
            else -> null
        }
}

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val c = app.container
    private val dao = c.db.dao()
    private val today = MutableStateFlow(LocalDate.now())

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages

    val state: StateFlow<AppState?> = combine(
        today,
        c.settings.settings,
        dao.weights(),
        dao.waist(),
        dao.checkIns(),
    ) { day, settings, weights, waist, checkIns ->
        AppState(day, c.plan, settings, weights.map { it.toPoint() }, waist, checkIns, emptySet())
    }.flatMapLatest { base ->
        dao.mealChecks(base.today.toEpochDay()).map { checks -> base.copy(checkedMeals = checks.toSet()) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val plan: Plan get() = c.plan

    /** Wird bei jedem onResume aufgerufen, damit "Heute" nach Mitternacht stimmt. */
    fun refreshDate() {
        today.value = LocalDate.now()
    }

    private fun launchAndRefresh(block: suspend () -> Unit) = viewModelScope.launch {
        block()
        TodayWidget.updateAll(getApplication())
    }

    fun saveWeight(date: LocalDate, kg: Double) = launchAndRefresh {
        dao.upsertWeight(WeightEntity(date.toEpochDay(), kg))
    }

    fun deleteWeight(date: LocalDate) = launchAndRefresh { dao.deleteWeight(date.toEpochDay()) }

    fun saveWaist(date: LocalDate, cm: Double) = launchAndRefresh {
        dao.upsertWaist(WaistEntity(date.toEpochDay(), cm))
    }

    fun deleteWaist(epochDay: Long) = launchAndRefresh { dao.deleteWaist(epochDay) }

    fun toggleMeal(mealId: String, checked: Boolean) = viewModelScope.launch {
        val day = today.value.toEpochDay()
        if (checked) dao.insertMealCheck(MealCheckEntity(day, mealId)) else dao.deleteMealCheck(day, mealId)
    }

    fun saveCheckIn(entity: CheckInEntity) = viewModelScope.launch { dao.upsertCheckIn(entity) }

    fun acceptSuggestion(s: Suggestion) = launchAndRefresh {
        c.settings.update { st ->
            st.copy(
                kcalOffsets = AdjustmentRule.apply(s, st.kcalOffsets, c.plan.dayTypes.keys),
                lastAdjustmentDecisionEpochDay = today.value.toEpochDay(),
                maintenanceApplied = st.maintenanceApplied || s is Suggestion.GoalReached,
            )
        }
        _messages.tryEmit("Anpassung übernommen")
    }

    fun declineSuggestion(s: Suggestion) = viewModelScope.launch {
        c.settings.update { st ->
            st.copy(
                lastAdjustmentDecisionEpochDay = today.value.toEpochDay(),
                maintenanceApplied = st.maintenanceApplied || s is Suggestion.GoalReached,
            )
        }
    }

    fun updateSettings(transform: (AppSettings) -> AppSettings) = launchAndRefresh {
        c.settings.update(transform)
        c.rescheduleAll()
    }

    fun export(uri: Uri) = viewModelScope.launch {
        runCatching { c.backup.export(uri) }
            .onSuccess { _messages.tryEmit("Export gespeichert") }
            .onFailure { _messages.tryEmit("Export fehlgeschlagen: ${it.message}") }
    }

    fun import(uri: Uri) = launchAndRefresh {
        runCatching { c.backup.import(uri) }
            .onSuccess {
                c.rescheduleAll()
                _messages.tryEmit("Import: ${it.weights.size} Gewichte, ${it.checkIns.size} Check-ins")
            }
            .onFailure { _messages.tryEmit("Import fehlgeschlagen: ${it.message}") }
    }

    fun rescheduleAlarms() = viewModelScope.launch { c.rescheduleAll() }
}
