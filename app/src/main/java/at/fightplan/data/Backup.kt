package at.fightplan.data

import at.fightplan.domain.AppSettings
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Inhalt der Export-Datei. */
@Serializable
data class BackupData(
    val format: String = FORMAT,
    val version: Int = 1,
    val exportedAt: String,
    val settings: AppSettings,
    val weights: List<WeightEntity>,
    val waist: List<WaistEntity>,
    val checkIns: List<CheckInEntity>,
    val mealChecks: List<MealCheckEntity>,
) {
    companion object {
        const val FORMAT = "fightplan-backup"
    }
}

object BackupCodec {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encode(data: BackupData): String = json.encodeToString(BackupData.serializer(), data)

    fun decode(text: String): BackupData {
        val data = json.decodeFromString(BackupData.serializer(), text)
        require(data.format == BackupData.FORMAT) { "Keine FightPlan-Sicherung" }
        require(data.version <= 1) { "Sicherung stammt aus einer neueren App-Version" }
        return data
    }
}
