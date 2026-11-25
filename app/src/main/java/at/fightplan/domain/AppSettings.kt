package at.fightplan.domain

import kotlinx.serialization.Serializable

/**
 * Alle Benutzereinstellungen. Wird als JSON in DataStore gespeichert und beim Export mitgeschrieben.
 * Nullable Felder fallen auf die Werte aus plan.json zurück.
 */
@Serializable
data class AppSettings(
    val startWeightKg: Double? = null,
    val goalWeightKg: Double? = null,
    /** Alarm-ID -> "HH:mm", überschreibt die Standardzeit. */
    val alarmTimeOverrides: Map<String, String> = emptyMap(),
    /** Ausgeschaltete Alarme. */
    val disabledAlarms: Set<String> = emptySet(),
    /** Tagestyp-Key -> kcal-Korrektur (über Kohlenhydrate), aus bestätigten Anpassungen. */
    val kcalOffsets: Map<String, Int> = emptyMap(),
    /** Datum (epochDay) der letzten Entscheidung zu einem Anpassungsvorschlag. */
    val lastAdjustmentDecisionEpochDay: Long? = null,
    val maintenanceApplied: Boolean = false,
) {
    fun startWeight(plan: Plan) = startWeightKg ?: plan.defaults.startWeightKg
    fun goalWeight(plan: Plan) = goalWeightKg ?: plan.defaults.goalWeightKg
}
