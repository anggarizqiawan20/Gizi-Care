package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_intake_records")
data class DailyIntakeRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val dateString: String,
    val totalCalories: Float,
    val totalCarbs: Float,
    val totalProtein: Float,
    val totalFat: Float,
    val foodsSummary: String,
    val weightAtThatTime: Float,
    val bmiAtThatTime: Float,
    val calorieTarget: Float,
    val timestamp: Long = System.currentTimeMillis()
)
