package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.TkpiFoodItem

@Entity(tableName = "custom_food_items")
data class CustomFoodItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val category: String,
    val caloriesPer100g: Float,
    val proteinPer100g: Float,
    val fatPer100g: Float,
    val carbsPer100g: Float
) {
    fun toTkpiFoodItem(): TkpiFoodItem {
        return TkpiFoodItem(
            name = name,
            category = category,
            caloriesPer100g = caloriesPer100g,
            proteinPer100g = proteinPer100g,
            fatPer100g = fatPer100g,
            carbsPer100g = carbsPer100g
        )
    }
}
