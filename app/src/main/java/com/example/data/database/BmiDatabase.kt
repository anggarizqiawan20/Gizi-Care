package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.BmiDao
import com.example.data.entity.BmiRecord
import com.example.data.entity.FoodLog
import com.example.data.entity.DailyIntakeRecord
import com.example.data.entity.CustomFoodItem

@Database(entities = [BmiRecord::class, FoodLog::class, DailyIntakeRecord::class, CustomFoodItem::class], version = 5, exportSchema = false)
abstract class BmiDatabase : RoomDatabase() {
    abstract fun bmiDao(): BmiDao

    companion object {
        @Volatile
        private var INSTANCE: BmiDatabase? = null

        fun getDatabase(context: Context): BmiDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BmiDatabase::class.java,
                    "bmi_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
