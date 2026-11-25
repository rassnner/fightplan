package at.fightplan.data

import android.content.Context
import android.net.Uri
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import at.fightplan.domain.AppSettings
import at.fightplan.domain.Plan
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.time.LocalDateTime

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {
    private val key = stringPreferencesKey("settings_json")
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        prefs[key]?.let { runCatching { json.decodeFromString(AppSettings.serializer(), it) }.getOrNull() }
            ?: AppSettings()
    }

    suspend fun current(): AppSettings = settings.first()

    suspend fun update(transform: (AppSettings) -> AppSettings) {
        context.dataStore.edit { prefs ->
            val old = prefs[key]?.let { runCatching { json.decodeFromString(AppSettings.serializer(), it) }.getOrNull() }
                ?: AppSettings()
            prefs[key] = json.encodeToString(AppSettings.serializer(), transform(old))
        }
    }

    suspend fun replace(settings: AppSettings) = update { settings }
}

object PlanRepository {
    @Volatile private var cached: Plan? = null

    fun load(context: Context): Plan = cached ?: synchronized(this) {
        cached ?: context.assets.open("plan.json").bufferedReader().use { Plan.parse(it.readText()) }
            .also { cached = it }
    }
}

class BackupManager(
    private val context: Context,
    private val dao: FightDao,
    private val settings: SettingsRepository,
) {
    suspend fun snapshot(): BackupData = BackupData(
        exportedAt = LocalDateTime.now().withNano(0).toString(),
        settings = settings.current(),
        weights = dao.allWeights(),
        waist = dao.allWaist(),
        checkIns = dao.allCheckIns(),
        mealChecks = dao.allMealChecks(),
    )

    suspend fun export(uri: Uri) = withContext(Dispatchers.IO) {
        val text = BackupCodec.encode(snapshot())
        context.contentResolver.openOutputStream(uri, "wt")?.use { it.write(text.toByteArray()) }
            ?: error("Datei konnte nicht geschrieben werden")
    }

    suspend fun import(uri: Uri): BackupData = withContext(Dispatchers.IO) {
        val text = context.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() }
            ?: error("Datei konnte nicht gelesen werden")
        val data = BackupCodec.decode(text)
        dao.replaceAll(data.weights, data.waist, data.checkIns, data.mealChecks)
        settings.replace(data.settings)
        data
    }
}
