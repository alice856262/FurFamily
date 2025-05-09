package com.example.furfamily.health

import java.util.Date

data class HealthRecord(
    val recordId: String = "",
    val userId: String = "",
    val petId: String? = null,
    val entryDate: Date = Date(),
    var weight: Float? = null,
    var temperature: Float? = null,
    var rbc: Float? = null,
    var wbc: Float? = null,
    var plt: Float? = null,
    var alb: Float? = null,
    var ast: Float? = null,
    var alt: Float? = null,
    var bun: Float? = null,
    var scr: Float? = null,
    var notes: String = ""
)