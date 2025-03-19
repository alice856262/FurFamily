package com.example.furfamily.nutrition

data class Food(
    var foodId: String = "",
    val category: String = "",
    val name: String = "",
    val ingredient: String = "",
    val caloriesPerKg: Float = 0.0F,
    val size: Int = 0,
    val proteinPercentage: Float = 0.0F,
    val fatPercentage: Float = 0.0F,
    val fiberPercentage: Float = 0.0F,
    val moisturePercentage: Float = 0.0F,
    val ashPercentage: Float = 0.0F,
    val feedingInfo: String = ""
)