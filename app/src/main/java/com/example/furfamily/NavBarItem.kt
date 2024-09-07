package com.example.furfamily

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.furfamily.calendar.CalendarScreen

data class NavBarItem (
    val label : String = "",
    val icon : ImageVector = Icons.Filled.AddCircle,
    val route : String = ""
) {
    fun NavBarItems(): List<NavBarItem> {
        return listOf(
            NavBarItem(
                label = "Calendar",
                icon = Icons.Filled.DateRange,
                route = Routes.CalendarScreen.value
            ),
            NavBarItem(
                label = "Health",
                icon = Icons.Filled.Add,
                route = Routes.HealthScreen.value
            ),
            NavBarItem(
                label = "Nutrition",
                icon = Icons.Filled.CheckCircle,
                route = Routes.Nutrition.value
            ),
            NavBarItem(
                label = "Profile",
                icon = Icons.Filled.Person,
                route = Routes.Profile.value
            )
        )
    }
}