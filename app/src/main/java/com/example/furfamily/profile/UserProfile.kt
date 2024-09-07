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
    val allowLocation: Boolean = true,
    val allowActivityShare: Boolean = true,
    val allowHealthDataShare: Boolean = true,
    val isGoogleUser: Boolean = false,
    var profileImageUrl: String = ""
)
