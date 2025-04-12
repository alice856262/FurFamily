package com.example.furfamily.nutrition

import java.time.LocalDateTime
import java.util.Date

data class Feeding(
    val feedingId: String = "",
    val petId: String = "",
    val foodId: String = "",
    val entryDate: Date = Date(),
    val amount: Float = 0.0F,
    val mealTime: LocalDateTime = LocalDateTime.of(2000, 1, 1, 0, 0),
    val mealType: String = "",
    val notes: String = ""
)
