package com.example.furfamily.nutrition

import java.util.Date

data class Feeding(
    val foodId: String = "",
    val entryDate: Date = Date(),
    val amount: Float = 0.0F,
    val notes: String = "",
    val mealType: String = ""
)
