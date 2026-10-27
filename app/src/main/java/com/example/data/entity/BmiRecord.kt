package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bmi_records")
data class BmiRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val weight: Float,
    val height: Float,
    val bmi: Float,
    val category: String,
    val gender: String,
    val age: Int = 25,
    val activityLevel: String = "Sedentary",
    val nutritionGoal: String = "Biasa",
    val bmr: Float = 0f,
    val tdee: Float = 0f,
    val targetCalories: Float = 0f,
    val targetCarbs: Float = 0f,
    val targetProtein: Float = 0f,
    val targetFat: Float = 0f,
    val timestamp: Long = System.currentTimeMillis()
)
