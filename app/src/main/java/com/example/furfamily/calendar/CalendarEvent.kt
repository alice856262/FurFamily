package com.example.furfamily.calendar

import com.google.firebase.database.IgnoreExtraProperties
import java.time.LocalDateTime

@IgnoreExtraProperties
data class CalendarEvent(
    val userId: String = "",
    val petId: String? = null,
    val title: String = "",
    val description: String? = null,
    val startTime: LocalDateTime = LocalDateTime.of(2000, 1, 1, 0, 0),
    val endTime: LocalDateTime = LocalDateTime.of(2000, 1, 1, 0, 0),
    val location: String? = null
)
