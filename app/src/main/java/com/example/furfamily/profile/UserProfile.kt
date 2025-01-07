package com.example.furfamily.profile

import java.util.Date

data class UserProfile(
    val userId: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val password: String = "",
    val selectedGender: String = "",
    val phone: String = "",
    val birthDate: Date = Date(),
    val isGoogleUser: Boolean = false,
    var profileImageUrl: String = ""
)
