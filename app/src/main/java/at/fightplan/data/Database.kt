package at.fightplan.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Upsert
import at.fightplan.domain.CheckInData
import at.fightplan.domain.WeightPoint
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable
import java.time.LocalDate

@Serializable
@Entity(tableName = "weights")
data class WeightEntity(@PrimaryKey val epochDay: Long, val kg: Double) {
    fun toPoint() = WeightPoint(LocalDate.ofEpochDay(epochDay), kg)
}

@Serializable
@Entity(tableName = "waist")
data class WaistEntity(@PrimaryKey val epochDay: Long, val cm: Double)

@Serializable
@Entity(tableName = "checkins")
data class CheckInEntity(
    @PrimaryKey val epochDay: Long,
    val sleepHours: Double,
    val mood: Int,
    val libidoNormal: Boolean,
    val infection: Boolean,
    val strengthOk: Boolean,
    val jumpCm: Double? = null,
) {
    fun toData() = CheckInData(sleepHours, mood, libidoNormal, infection, strengthOk, jumpCm)
}

@Serializable
@Entity(tableName = "meal_checks", primaryKeys = ["epochDay", "mealId"])
data class MealCheckEntity(val epochDay: Long, val mealId: String)

@Dao
abstract class FightDao {
    @Query("SELECT * FROM weights ORDER BY epochDay")
    abstract fun weights(): Flow<List<WeightEntity>>

    @Query("SELECT * FROM weights ORDER BY epochDay")
    abstract suspend fun allWeights(): List<WeightEntity>

    @Upsert
    abstract suspend fun upsertWeight(w: WeightEntity)

    @Query("DELETE FROM weights WHERE epochDay = :epochDay")
    abstract suspend fun deleteWeight(epochDay: Long)

    @Query("SELECT * FROM waist ORDER BY epochDay")
    abstract fun waist(): Flow<List<WaistEntity>>

    @Query("SELECT * FROM waist ORDER BY epochDay")
    abstract suspend fun allWaist(): List<WaistEntity>

    @Upsert
    abstract suspend fun upsertWaist(w: WaistEntity)

    @Query("DELETE FROM waist WHERE epochDay = :epochDay")
    abstract suspend fun deleteWaist(epochDay: Long)

    @Query("SELECT * FROM checkins ORDER BY epochDay DESC")
    abstract fun checkIns(): Flow<List<CheckInEntity>>

    @Query("SELECT * FROM checkins ORDER BY epochDay")
    abstract suspend fun allCheckIns(): List<CheckInEntity>

    @Upsert
    abstract suspend fun upsertCheckIn(c: CheckInEntity)

    @Query("SELECT mealId FROM meal_checks WHERE epochDay = :epochDay")
    abstract fun mealChecks(epochDay: Long): Flow<List<String>>

    @Query("SELECT * FROM meal_checks ORDER BY epochDay, mealId")
    abstract suspend fun allMealChecks(): List<MealCheckEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertMealCheck(m: MealCheckEntity)

    @Query("DELETE FROM meal_checks WHERE epochDay = :epochDay AND mealId = :mealId")
    abstract suspend fun deleteMealCheck(epochDay: Long, mealId: String)

    @Query("DELETE FROM weights") abstract suspend fun clearWeights()
    @Query("DELETE FROM waist") abstract suspend fun clearWaist()
    @Query("DELETE FROM checkins") abstract suspend fun clearCheckIns()
    @Query("DELETE FROM meal_checks") abstract suspend fun clearMealChecks()

    @Insert abstract suspend fun insertWeights(items: List<WeightEntity>)
    @Insert abstract suspend fun insertWaist(items: List<WaistEntity>)
    @Insert abstract suspend fun insertCheckIns(items: List<CheckInEntity>)
    @Insert abstract suspend fun insertMealChecks(items: List<MealCheckEntity>)

    /** Ersetzt alle Daten atomar (für den Import). */
    @Transaction
    open suspend fun replaceAll(
        weights: List<WeightEntity>,
        waist: List<WaistEntity>,
        checkIns: List<CheckInEntity>,
        mealChecks: List<MealCheckEntity>,
    ) {
        clearWeights(); clearWaist(); clearCheckIns(); clearMealChecks()
        insertWeights(weights); insertWaist(waist); insertCheckIns(checkIns); insertMealChecks(mealChecks)
    }
}

@Database(
    entities = [WeightEntity::class, WaistEntity::class, CheckInEntity::class, MealCheckEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class FightDatabase : RoomDatabase() {
    abstract fun dao(): FightDao

    companion object {
        fun create(context: Context): FightDatabase =
            Room.databaseBuilder(context, FightDatabase::class.java, "fightplan.db").build()
    }
}
