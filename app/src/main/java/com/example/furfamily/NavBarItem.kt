package com.example.furfamily

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource

data class NavBarItem(
    val label: String,
    val icon: Painter, // Use Painter for custom icons
    val route: String
) {
    companion object {
        @Composable
        fun NavBarItems(): List<NavBarItem> {
            return listOf(
                NavBarItem(
                    label = "Calendar",
                    icon = painterResource(R.drawable.calendar),
                    route = Routes.CalendarScreen.value
                ),
                NavBarItem(
                    label = "Health",
                    icon = painterResource(R.drawable.health),
                    route = Routes.HealthScreen.value
                ),
                NavBarItem(
                    label = "Nutrition",
                    icon = painterResource(R.drawable.nutrition),
                    route = Routes.Nutrition.value
                ),
                NavBarItem(
                    label = "Profile",
                    icon = painterResource(R.drawable.profile),
                    route = Routes.Profile.value
                )
            )
        }
    }
}