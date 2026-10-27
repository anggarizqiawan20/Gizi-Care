package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.database.BmiDatabase
import com.example.data.entity.BmiRecord
import com.example.data.entity.FoodLog
import com.example.data.entity.DailyIntakeRecord
import com.example.data.entity.CustomFoodItem
import com.example.data.model.TkpiFoodItem
import com.example.data.model.NutritionArticle
import com.example.util.ArticleFetcher
import com.example.data.repository.BmiRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.pow

data class DailyNutritionNeeds(
    val bmr: Float,
    val tdee: Float,
    val targetCalories: Float,
    val targetCarbs: Float,
    val targetProtein: Float,
    val targetFat: Float
)

class BmiViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: BmiRepository

    private val _nutritionArticles = MutableStateFlow<List<NutritionArticle>>(emptyList())
    val nutritionArticles: StateFlow<List<NutritionArticle>> = _nutritionArticles.asStateFlow()

    private val _isLoadingArticles = MutableStateFlow(false)
    val isLoadingArticles: StateFlow<Boolean> = _isLoadingArticles.asStateFlow()

    init {
        val database = BmiDatabase.getDatabase(application)
        repository = BmiRepository(database.bmiDao())
        loadArticles()
    }

    fun loadArticles() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoadingArticles.value = true
            try {
                val articles = ArticleFetcher.fetchArticles()
                _nutritionArticles.value = articles
            } catch (e: Exception) {
                _nutritionArticles.value = ArticleFetcher.getFallbackArticles()
            } finally {
                _isLoadingArticles.value = false
            }
        }
    }

    // Input States
    private val _weight = MutableStateFlow(60f) // in kg
    val weight: StateFlow<Float> = _weight.asStateFlow()

    private val _height = MutableStateFlow(165f) // in cm
    val height: StateFlow<Float> = _height.asStateFlow()

    private val _gender = MutableStateFlow("Laki-laki")
    val gender: StateFlow<String> = _gender.asStateFlow()

    // Daily Nutrition Needs calculation inputs
    private val _age = MutableStateFlow(25)
    val age: StateFlow<Int> = _age.asStateFlow()

    private val _activityLevel = MutableStateFlow("Sedentary")
    val activityLevel: StateFlow<String> = _activityLevel.asStateFlow()

    private val _nutritionGoal = MutableStateFlow("Biasa") // Biasa (Menjaga), Menurunkan, Menaikkan
    val nutritionGoal: StateFlow<String> = _nutritionGoal.asStateFlow()

    // History Flow
    val allRecords: StateFlow<List<BmiRecord>> = repository.allRecords
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allFoodLogs: StateFlow<List<FoodLog>> = repository.allFoodLogs
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allDailyIntakeRecords: StateFlow<List<DailyIntakeRecord>> = repository.allDailyIntakeRecords
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allCustomFoodItems: StateFlow<List<CustomFoodItem>> = repository.allCustomFoodItems
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )



    fun addCustomFoodItem(
        name: String,
        category: String,
        caloriesPer100g: Float,
        proteinPer100g: Float,
        fatPer100g: Float,
        carbsPer100g: Float
    ) {
        viewModelScope.launch {
            val newItem = CustomFoodItem(
                name = name,
                category = category,
                caloriesPer100g = caloriesPer100g,
                proteinPer100g = proteinPer100g,
                fatPer100g = fatPer100g,
                carbsPer100g = carbsPer100g
            )
            repository.insertCustomFoodItem(newItem)
        }
    }

    fun deleteCustomFoodItem(item: CustomFoodItem) {
        viewModelScope.launch {
            repository.deleteCustomFoodItem(item)
        }
    }

    fun clearAllCustomFoodItems() {
        viewModelScope.launch {
            repository.clearAllCustomFoodItems()
        }
    }

    fun updateWeight(newWeight: Float) {
        _weight.value = newWeight.coerceIn(20f, 250f)
    }

    fun updateHeight(newHeight: Float) {
        _height.value = newHeight.coerceIn(50f, 250f)
    }

    fun updateGender(newGender: String) {
        _gender.value = newGender
    }

    // Calculations
    fun calculateBmi(weightValue: Float, heightValue: Float): Float {
        val heightInMeters = heightValue / 100f
        if (heightInMeters <= 0) return 0f
        return weightValue / heightInMeters.pow(2)
    }

    fun getCategory(bmi: Float): String {
        return when {
            bmi < 18.5f -> "Kurang Berat Badan"
            bmi in 18.5f..24.9f -> "Berat Badan Normal"
            bmi in 25.0f..29.9f -> "Kelebihan Berat Badan"
            else -> "Obesitas"
        }
    }

    fun getIdealWeight(heightValue: Float): Float {
        // Ideal Weight based on standard healthy BMI median parameter of 21.7
        val heightInMeters = heightValue / 100f
        return 21.7f * heightInMeters.pow(2)
    }

    fun getIdealRange(heightValue: Float): Pair<Float, Float> {
        val heightInMeters = heightValue / 100f
        val minIdeal = 18.5f * heightInMeters.pow(2)
        val maxIdeal = 24.9f * heightInMeters.pow(2)
        return Pair(minIdeal, maxIdeal)
    }

    // Repository Actions
    fun saveRecord() {
        viewModelScope.launch {
            val weightVal = _weight.value
            val heightVal = _height.value
            val genderVal = _gender.value
            val ageVal = _age.value
            val activityVal = _activityLevel.value
            val goalVal = _nutritionGoal.value

            val calculatedBmi = calculateBmi(weightVal, heightVal)
            val currentCategory = getCategory(calculatedBmi)
            val needs = calculateDailyNeeds(weightVal, heightVal, ageVal, genderVal, activityVal, goalVal)

            val record = BmiRecord(
                weight = weightVal,
                height = heightVal,
                bmi = calculatedBmi,
                category = currentCategory,
                gender = genderVal,
                age = ageVal,
                activityLevel = activityVal,
                nutritionGoal = goalVal,
                bmr = needs.bmr,
                tdee = needs.tdee,
                targetCalories = needs.targetCalories,
                targetCarbs = needs.targetCarbs,
                targetProtein = needs.targetProtein,
                targetFat = needs.targetFat
            )
            repository.insert(record)
        }
    }

    fun deleteRecord(record: BmiRecord) {
        viewModelScope.launch {
            repository.delete(record)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearAll()
        }
    }

    fun updateAge(newAge: Int) {
        _age.value = newAge.coerceIn(1, 120)
    }

    fun updateActivityLevel(newLevel: String) {
        _activityLevel.value = newLevel
    }

    fun updateNutritionGoal(newGoal: String) {
        _nutritionGoal.value = newGoal
    }

    // Mifflin-St Jeor formula (best modern standard)
    fun calculateDailyNeeds(
        weightValue: Float,
        heightValue: Float,
        ageValue: Int,
        genderValue: String,
        activityValue: String,
        goalValue: String
    ): DailyNutritionNeeds {
        val bmr = if (genderValue == "Laki-laki") {
            (10f * weightValue) + (6.25f * heightValue) - (5f * ageValue) + 5f
        } else {
            (10f * weightValue) + (6.25f * heightValue) - (5f * ageValue) - 161f
        }

        val activityMultiplier = when (activityValue) {
            "Ringan" -> 1.375f
            "Sedang" -> 1.55f
            "Berat" -> 1.725f
            else -> 1.2f
        }
        val tdee = bmr * activityMultiplier

        val targetCalories = when (goalValue) {
            "Menurunkan" -> (tdee - 500f).coerceAtLeast(1200f)
            "Menaikkan" -> tdee + 500f
            else -> tdee
        }

        val targetCarbs = (targetCalories * 0.60f) / 4f
        val targetProtein = (targetCalories * 0.15f) / 4f
        val targetFat = (targetCalories * 0.25f) / 9f

        return DailyNutritionNeeds(
            bmr = bmr,
            tdee = tdee,
            targetCalories = targetCalories,
            targetCarbs = targetCarbs,
            targetProtein = targetProtein,
            targetFat = targetFat
        )
    }

    fun addFoodLog(item: TkpiFoodItem, portionGrams: Float, mealTime: String) {
        viewModelScope.launch {
            val caloriesVal = item.getCaloriesFor(portionGrams)
            val carbsVal = item.getCarbsFor(portionGrams)
            val proteinVal = item.getProteinFor(portionGrams)
            val fatVal = item.getFatFor(portionGrams)

            val log = FoodLog(
                foodName = item.name,
                category = mealTime, // e.g. "Sarapan", "Selingan Pagi", "Makan Siang", "Selingan Sore", "Makan Malam"
                portionGrams = portionGrams,
                calories = caloriesVal,
                carbs = carbsVal,
                protein = proteinVal,
                fat = fatVal
            )
            repository.insertFoodLog(log)
        }
    }

    fun deleteFoodLog(log: FoodLog) {
        viewModelScope.launch {
            repository.deleteFoodLog(log)
        }
    }

    fun clearAllFoodLogs() {
        viewModelScope.launch {
            repository.clearAllFoodLogs()
        }
    }

    fun saveDailyIntake(foodLogs: List<FoodLog>, targetCalories: Float, customDateString: String? = null) {
        viewModelScope.launch {
            if (foodLogs.isEmpty()) return@launch

            val totalCalories = foodLogs.sumOf { it.calories.toDouble() }.toFloat()
            val totalCarbs = foodLogs.sumOf { it.carbs.toDouble() }.toFloat()
            val totalProtein = foodLogs.sumOf { it.protein.toDouble() }.toFloat()
            val totalFat = foodLogs.sumOf { it.fat.toDouble() }.toFloat()

            // Order logs according to the natural dietary chronology of the day
            val orderMap = mapOf(
                "Sarapan" to 1,
                "Selingan Pagi" to 2,
                "Makan Siang" to 3,
                "Selingan Sore" to 4,
                "Makan Malam" to 5
            )
            val sortedLogs = foodLogs.sortedBy { orderMap[it.category] ?: 99 }
            val summary = sortedLogs.joinToString(" • ") { "${it.category}: ${it.foodName} (${it.portionGrams.toInt()}g)" }

            val currentWeight = _weight.value
            val currentHeight = _height.value
            val currentBmi = calculateBmi(currentWeight, currentHeight)

            val formattedDate = if (!customDateString.isNullOrBlank()) {
                customDateString
            } else {
                val formatter = SimpleDateFormat("EEEE, d MMMM yyyy", Locale("id", "ID"))
                formatter.format(Date())
            }

            val record = DailyIntakeRecord(
                dateString = formattedDate,
                totalCalories = totalCalories,
                totalCarbs = totalCarbs,
                totalProtein = totalProtein,
                totalFat = totalFat,
                foodsSummary = summary,
                weightAtThatTime = currentWeight,
                bmiAtThatTime = currentBmi,
                calorieTarget = targetCalories
            )

            repository.insertDailyIntakeRecord(record)
            repository.clearAllFoodLogs() // Clear today's active eating log
        }
    }

    fun deleteDailyIntakeRecord(record: DailyIntakeRecord) {
        viewModelScope.launch {
            repository.deleteDailyIntakeRecord(record)
        }
    }

    fun clearAllDailyIntakeRecords() {
        viewModelScope.launch {
            repository.clearAllDailyIntakeRecords()
        }
    }

    // Factory Class
    companion object {
        fun provideFactory(application: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return BmiViewModel(application) as T
                }
            }
    }
}
