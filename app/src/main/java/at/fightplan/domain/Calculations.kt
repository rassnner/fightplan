package at.fightplan.domain

import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.math.max
import kotlin.math.roundToInt

data class Macros(val kcal: Int, val proteinG: Int, val carbsG: Int, val fatG: Int)

object MacroCalculator {
    /**
     * Makros für einen Tag. [kcalOffset] wird komplett über Kohlenhydrate abgebildet (1 g = 4 kcal).
     * kcal = 4·KH + 4·E + 9·F, gerechnet mit ungerundeten Grammwerten.
     */
    fun forDay(weightKg: Double, type: DayType, kcalOffset: Int = 0): Macros {
        val protein = type.proteinPerKg * weightKg
        val fat = type.fatPerKg * weightKg
        val carbs = max(0.0, type.carbsPerKg * weightKg + kcalOffset / 4.0)
        val kcal = 4 * carbs + 4 * protein + 9 * fat
        return Macros(
            kcal = kcal.roundToInt(),
            proteinG = protein.roundToInt(),
            carbsG = carbs.roundToInt(),
            fatG = fat.roundToInt(),
        )
    }

    /** Durchschnittliche kcal über die 7 Tage des Plans. */
    fun weeklyAverageKcal(plan: Plan, weightKg: Double, offsets: Map<String, Int> = emptyMap()): Double =
        DayOfWeek.entries.sumOf { d ->
            val key = plan.day(d).type
            forDay(weightKg, plan.dayTypes.getValue(key), offsets[key] ?: 0).kcal
        } / 7.0
}

data class WeightPoint(val date: LocalDate, val kg: Double)

object WeightStats {
    const val WINDOW_DAYS = 7L

    /** Mittelwert aller Einträge in [date-6, date]. Null, wenn weniger als [minEntries] Einträge. */
    fun rollingAverage(entries: List<WeightPoint>, date: LocalDate, minEntries: Int = 1): Double? {
        val from = date.minusDays(WINDOW_DAYS - 1)
        val window = entries.filter { !it.date.isBefore(from) && !it.date.isAfter(date) }
        if (window.size < minEntries || window.isEmpty()) return null
        return window.sumOf { it.kg } / window.size
    }

    /** 7-Tage-Schnitt für jeden Tag mit Eintrag (für das Diagramm). */
    fun averageSeries(entries: List<WeightPoint>): List<WeightPoint> =
        entries.sortedBy { it.date }.map { WeightPoint(it.date, rollingAverage(entries, it.date)!!) }

    /**
     * Durchschnittlicher Verlust pro Woche (positiv = abgenommen), aus dem 7-Tage-Schnitt heute
     * im Vergleich zu vor [weeks] Wochen. Braucht in beiden Fenstern mindestens [minEntries] Werte.
     */
    fun weeklyLoss(entries: List<WeightPoint>, today: LocalDate, weeks: Int, minEntries: Int = 3): Double? {
        val now = rollingAverage(entries, today, minEntries) ?: return null
        val before = rollingAverage(entries, today.minusWeeks(weeks.toLong()), minEntries) ?: return null
        return (before - now) / weeks
    }

    /**
     * Gewicht, aus dem die Makros berechnet werden: 7-Tage-Schnitt, sonst der Schnitt um den letzten
     * Eintrag, sonst das Startgewicht.
     */
    fun basisWeight(entries: List<WeightPoint>, date: LocalDate, fallbackKg: Double): Double {
        rollingAverage(entries, date)?.let { return it }
        val last = entries.filter { !it.date.isAfter(date) }.maxByOrNull { it.date } ?: return fallbackKg
        return rollingAverage(entries, last.date) ?: last.kg
    }
}

sealed interface Suggestion {
    data object NotEnoughData : Suggestion
    data class Keep(val lossPerWeek: Double) : Suggestion
    data class Decrease(val lossPerWeek: Double, val kcal: Int, val dayTypes: List<String>) : Suggestion
    data class Increase(val lossPerWeek: Double, val kcal: Int) : Suggestion
    data class GoalReached(val averageKg: Double, val extraKcal: Int) : Suggestion
}

object AdjustmentRule {
    fun evaluate(
        entries: List<WeightPoint>,
        today: LocalDate,
        goalKg: Double,
        defaults: PlanDefaults,
    ): Suggestion {
        val avg = WeightStats.rollingAverage(entries, today)
        if (avg != null && avg <= goalKg) return Suggestion.GoalReached(avg, defaults.maintenanceExtraKcal)
        val rules = defaults.rules
        val raw = WeightStats.weeklyLoss(entries, today, rules.compareWeeks) ?: return Suggestion.NotEnoughData
        // Auf 0,01 kg runden: so wird der Wert angezeigt, und Grenzwerte kippen nicht durch Gleitkommafehler.
        val loss = Math.round(raw * 100) / 100.0
        return when {
            loss < rules.slowLossThresholdKg -> Suggestion.Decrease(loss, rules.decreaseKcal, rules.decreaseDayTypes)
            loss > rules.fastLossThresholdKg -> Suggestion.Increase(loss, rules.increaseKcal)
            else -> Suggestion.Keep(loss)
        }
    }

    /** Wendet einen bestätigten Vorschlag auf die kcal-Korrekturen an. */
    fun apply(suggestion: Suggestion, offsets: Map<String, Int>, allDayTypes: Collection<String>): Map<String, Int> {
        val result = offsets.toMutableMap()
        when (suggestion) {
            is Suggestion.Decrease -> suggestion.dayTypes.forEach { result[it] = (result[it] ?: 0) - suggestion.kcal }
            is Suggestion.Increase -> allDayTypes.forEach { result[it] = (result[it] ?: 0) + suggestion.kcal }
            is Suggestion.GoalReached -> allDayTypes.forEach { result[it] = (result[it] ?: 0) + suggestion.extraKcal }
            else -> Unit
        }
        return result
    }

    /** Ein Vorschlag wird erst wieder gezeigt, wenn seit der letzten Entscheidung genug Daten (2 Wochen) da sind. */
    fun isDue(lastDecisionEpochDay: Long?, today: LocalDate, cooldownDays: Long = 14): Boolean =
        lastDecisionEpochDay == null || today.toEpochDay() - lastDecisionEpochDay >= cooldownDays
}

data class CheckInData(
    val sleepHours: Double,
    val mood: Int,
    val libidoNormal: Boolean,
    val infection: Boolean,
    val strengthOk: Boolean,
    val jumpCm: Double? = null,
)

data class WarningResult(val count: Int, val reasons: List<String>, val red: Boolean, val seeDoctor: Boolean)

object WarningSigns {
    const val RED_MESSAGE = "Eine Woche Erhaltungskalorien und Gym-Volumen halbieren, mit dem Trainer sprechen."
    const val DOCTOR_MESSAGE = "Wenn Libido-Probleme oder Infekte anhalten: zum Arzt gehen."

    fun evaluate(c: CheckInData): WarningResult {
        val reasons = buildList {
            if (c.sleepHours < 8.0) add("Schlaf unter 8 h")
            if (c.mood <= 2) add("Stimmung niedrig")
            if (!c.libidoNormal) add("Libido nicht normal")
            if (c.infection) add("Infekt")
            if (!c.strengthOk) add("Kraft schlechter")
        }
        return WarningResult(
            count = reasons.size,
            reasons = reasons,
            red = reasons.size >= 2,
            seeDoctor = !c.libidoNormal || c.infection,
        )
    }
}

data class DayTargets(val typeKey: String, val type: DayType, val basisWeightKg: Double, val macros: Macros)

object DayTargetCalculator {
    fun targets(plan: Plan, settings: AppSettings, weights: List<WeightPoint>, date: LocalDate): DayTargets {
        val key = plan.day(date.dayOfWeek).type
        val type = plan.dayTypes.getValue(key)
        val basis = WeightStats.basisWeight(weights, date, settings.startWeight(plan))
        return DayTargets(key, type, basis, MacroCalculator.forDay(basis, type, settings.kcalOffsets[key] ?: 0))
    }
}
