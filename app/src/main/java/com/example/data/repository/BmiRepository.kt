package com.example.data.repository

import com.example.data.dao.BmiDao
import com.example.data.entity.BmiRecord
import com.example.data.entity.FoodLog
import com.example.data.entity.DailyIntakeRecord
import com.example.data.entity.CustomFoodItem
import kotlinx.coroutines.flow.Flow

class BmiRepository(private val bmiDao: BmiDao) {
    val allRecords: Flow<List<BmiRecord>> = bmiDao.getAllRecords()

    suspend fun insert(record: BmiRecord) {
        bmiDao.insertRecord(record)
    }

    suspend fun delete(record: BmiRecord) {
        bmiDao.deleteRecord(record)
    }

    suspend fun clearAll() {
        bmiDao.clearAllRecords()
    }

    val allFoodLogs: Flow<List<FoodLog>> = bmiDao.getAllFoodLogs()

    suspend fun insertFoodLog(log: FoodLog) {
        bmiDao.insertFoodLog(log)
    }

    suspend fun deleteFoodLog(log: FoodLog) {
        bmiDao.deleteFoodLog(log)
    }

    suspend fun clearAllFoodLogs() {
        bmiDao.clearAllFoodLogs()
    }

    val allDailyIntakeRecords: Flow<List<DailyIntakeRecord>> = bmiDao.getAllDailyIntakeRecords()

    suspend fun insertDailyIntakeRecord(record: DailyIntakeRecord) {
        bmiDao.insertDailyIntakeRecord(record)
    }

    suspend fun deleteDailyIntakeRecord(record: DailyIntakeRecord) {
        bmiDao.deleteDailyIntakeRecord(record)
    }

    suspend fun clearAllDailyIntakeRecords() {
        bmiDao.clearAllDailyIntakeRecords()
    }

    val allCustomFoodItems: Flow<List<CustomFoodItem>> = bmiDao.getAllCustomFoodItems()

    suspend fun insertCustomFoodItem(item: CustomFoodItem) {
        bmiDao.insertCustomFoodItem(item)
    }

    suspend fun deleteCustomFoodItem(item: CustomFoodItem) {
        bmiDao.deleteCustomFoodItem(item)
    }

    suspend fun clearAllCustomFoodItems() {
        bmiDao.clearAllCustomFoodItems()
    }
}
