package com.example.furfamily.profile

import java.util.Date

data class Pet(
    val petId: String = "",
    val name: String = "",
    val selectedSex: String = "",
    val birthDate: Date = Date(),
    val breed: String = "",
    var profileImageUrl: String = ""
)
