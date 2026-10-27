package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "food_logs")
data class FoodLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val foodName: String,
    val category: String,
    val portionGrams: Float,
    val calories: Float,
    val carbs: Float,
    val protein: Float,
    val fat: Float,
    val timestamp: Long = System.currentTimeMillis()
)
