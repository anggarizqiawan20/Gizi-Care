package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.entity.BmiRecord
import com.example.data.entity.FoodLog
import com.example.data.entity.DailyIntakeRecord
import com.example.data.entity.CustomFoodItem
import kotlinx.coroutines.flow.Flow

@Dao
interface BmiDao {
    @Query("SELECT * FROM bmi_records ORDER BY timestamp DESC")
    fun getAllRecords(): Flow<List<BmiRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: BmiRecord)

    @Delete
    suspend fun deleteRecord(record: BmiRecord)

    @Query("DELETE FROM bmi_records")
    suspend fun clearAllRecords()

    @Query("SELECT * FROM food_logs ORDER BY timestamp DESC")
    fun getAllFoodLogs(): Flow<List<FoodLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFoodLog(log: FoodLog)

    @Delete
    suspend fun deleteFoodLog(log: FoodLog)

    @Query("DELETE FROM food_logs")
    suspend fun clearAllFoodLogs()

    @Query("SELECT * FROM daily_intake_records ORDER BY timestamp DESC")
    fun getAllDailyIntakeRecords(): Flow<List<DailyIntakeRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailyIntakeRecord(record: DailyIntakeRecord)

    @Delete
    suspend fun deleteDailyIntakeRecord(record: DailyIntakeRecord)

    @Query("DELETE FROM daily_intake_records")
    suspend fun clearAllDailyIntakeRecords()

    @Query("SELECT * FROM custom_food_items ORDER BY name ASC")
    fun getAllCustomFoodItems(): Flow<List<CustomFoodItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomFoodItem(item: CustomFoodItem)

    @Delete
    suspend fun deleteCustomFoodItem(item: CustomFoodItem)

    @Query("DELETE FROM custom_food_items")
    suspend fun clearAllCustomFoodItems()
}
