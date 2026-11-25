package at.fightplan.domain

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.DayOfWeek
import java.time.LocalTime

/** Kompletter Wochenplan, wird aus assets/plan.json geladen. */
@Serializable
data class Plan(
    val version: Int,
    val defaults: PlanDefaults,
    val dayTypes: Map<String, DayType>,
    val days: List<DayPlan>,
    val reminders: List<FixedReminder> = emptyList(),
    val weighIn: WeighIn,
    val checkIn: TimedEvent,
) {
    fun day(dayOfWeek: DayOfWeek): DayPlan =
        days.firstOrNull { it.dayOfWeek == dayOfWeek }
            ?: error("Kein Eintrag für $dayOfWeek in plan.json")

    fun dayType(dayOfWeek: DayOfWeek): DayType {
        val key = day(dayOfWeek).type
        return dayTypes[key] ?: error("Unbekannter Tagestyp $key")
    }

    companion object {
        private val json = Json { ignoreUnknownKeys = true }
        fun parse(text: String): Plan = json.decodeFromString(serializer(), text).also { it.validate() }
    }

    /** Wirft eine Exception, wenn der Plan inkonsistent ist. */
    fun validate() {
        require(days.map { it.dayOfWeek }.toSet().size == 7) { "plan.json braucht genau 7 Tage" }
        days.forEach { d ->
            require(dayTypes.containsKey(d.type)) { "Tagestyp ${d.type} fehlt in dayTypes" }
        }
        defaults.rules.decreaseDayTypes.forEach {
            require(dayTypes.containsKey(it)) { "Tagestyp $it in rules.decreaseDayTypes fehlt" }
        }
        val ids = days.flatMap { d -> d.sessions.map { it.id } + d.meals.map { it.id } } + reminders.map { it.id }
        require(ids.size == ids.toSet().size) { "IDs in plan.json müssen eindeutig sein" }
    }
}

@Serializable
data class PlanDefaults(
    val startWeightKg: Double,
    val goalWeightKg: Double,
    val targetLossMinKgPerWeek: Double,
    val targetLossMaxKgPerWeek: Double,
    val mealReminderLeadMinutes: Int = 10,
    val trainingReminderLeadMinutes: Int = 45,
    val maintenanceExtraKcal: Int = 350,
    val rules: AdjustmentConfig = AdjustmentConfig(),
)

@Serializable
data class AdjustmentConfig(
    val slowLossThresholdKg: Double = 0.3,
    val fastLossThresholdKg: Double = 0.8,
    val decreaseKcal: Int = 150,
    val decreaseDayTypes: List<String> = listOf("NORMAL", "REST"),
    val increaseKcal: Int = 200,
    val compareWeeks: Int = 2,
)

@Serializable
data class DayType(
    val label: String,
    val carbsPerKg: Double,
    val proteinPerKg: Double,
    val fatPerKg: Double,
)

@Serializable
data class DayPlan(
    val day: String,
    val type: String,
    val school: TimeRange? = null,
    val sessions: List<Session> = emptyList(),
    val meals: List<Meal> = emptyList(),
) {
    val dayOfWeek: DayOfWeek get() = DayOfWeek.valueOf(day)
}

@Serializable
data class TimeRange(val start: String, val end: String) {
    val startTime: LocalTime get() = LocalTime.parse(start)
    val endTime: LocalTime get() = LocalTime.parse(end)
}

@Serializable
data class Session(
    val id: String,
    val start: String? = null,
    val end: String? = null,
    val title: String,
    val content: String,
    val training: Boolean = true,
) {
    val startTime: LocalTime? get() = start?.let(LocalTime::parse)
    val endTime: LocalTime? get() = end?.let(LocalTime::parse)
    val timeText: String
        get() = when {
            start != null && end != null -> "$start–$end"
            start != null -> start
            else -> "ganztags"
        }
}

@Serializable
data class Meal(
    val id: String,
    val time: String? = null,
    val timeLabel: String? = null,
    val content: String,
) {
    val localTime: LocalTime? get() = time?.let(LocalTime::parse)
    val timeText: String get() = time ?: timeLabel ?: "–"
}

@Serializable
data class FixedReminder(
    val id: String,
    val day: String,
    val time: String,
    val title: String,
    val text: String,
)

@Serializable
data class WeighIn(
    val defaultTime: String,
    val overrides: Map<String, String> = emptyMap(),
) {
    fun timeFor(day: DayOfWeek): LocalTime = LocalTime.parse(overrides[day.name] ?: defaultTime)
}

@Serializable
data class TimedEvent(val day: String, val time: String) {
    val dayOfWeek: DayOfWeek get() = DayOfWeek.valueOf(day)
    val localTime: LocalTime get() = LocalTime.parse(time)
}
