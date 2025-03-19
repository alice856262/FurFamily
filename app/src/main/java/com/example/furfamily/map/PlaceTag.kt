package com.example.furfamily.map

data class PlaceTag(
    val tagId: String = "",
    val userId: String = "",
    val name: String = "",
    val address: String? = null,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
)