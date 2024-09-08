package com.example.furfamily.nutrition

data class Food(
    var foodId: String = "",
    val name: String = "",
    val caloriesPer100g: Float = 0.0F,
    val waterContentPer100g: Float = 0.0F,
    val notes: String = ""
)