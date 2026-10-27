package com.example.data.model

data class NutritionArticle(
    val title: String,
    val link: String,
    val pubDate: String,
    val creator: String,
    val description: String,
    val imageUrl: String? = null
)
