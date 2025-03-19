package com.example.furfamily.profile

import java.util.Date

data class Pet(
    val petId: String = "",
    val name: String = "",
    val type: String = "",
    val breed: String = "",
    val selectedSex: String = "",
    val birthDate: Date = Date(),
    var profileImageUrl: String = ""
)
