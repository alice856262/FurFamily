package com.example.furfamily

import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.padding
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.furfamily.calendar.CalendarScreen
import com.example.furfamily.health.CreateHealthRecord
import com.example.furfamily.health.HealthRecord
import com.example.furfamily.health.HealthScreen
import com.example.furfamily.profile.LoginScreen
import com.example.furfamily.profile.ProfileSettingsScreen
import com.example.furfamily.profile.RegistrationScreen
import com.example.furfamily.profile.loginWithEmailPassword
import com.google.firebase.auth.FirebaseAuth
import java.util.Date

@Composable
fun BottomNavigationBar(navController: NavController) {
    return BottomNavigation (backgroundColor= MaterialTheme.colorScheme.background ) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination
        NavBarItem().NavBarItems().forEach { navItem ->
            BottomNavigationItem(
                icon = {
                    Icon(
                        navItem.icon, contentDescription = null
                    )
                },
                label = { Text(navItem.label) },
                selected = currentDestination?.hierarchy?.any {
                    it.route == navItem.route
                } == true,
                onClick = {
                    navController.navigate(navItem.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}

@RequiresApi(64)
@Composable
fun HomeScreen(viewModel: ViewModel) {
    val navController = rememberNavController()
    val currentRoute = getCurrentRoute(navController)
    Scaffold(
        bottomBar = {
            if (currentRoute != Routes.Login.value && currentRoute != Routes.Registration.value) {
                BottomNavigationBar(navController = navController)
            }
        }
    ) { paddingValues ->
        NavHost(
            navController,
            startDestination = Routes.Login.value,
            Modifier.padding(paddingValues)
        ) {
            composable(Routes.Login.value) {
                LoginScreen(
                    loginWithEmailPassword = { email, password, onLoginError ->
                        loginWithEmailPassword(email, password, navController, onLoginError)
                    },
                    navController = navController,
                    viewModel = viewModel
                )
            }

            composable(Routes.Registration.value) {
                RegistrationScreen(
                    createUserWithEmailPassword = { firstName, lastName, email, password, gender, phone, birthDate -> },
                    navController = navController,
                    viewModel = viewModel
                )
            }

            /* Nutrition navigation tab */
            composable(Routes.Nutrition.value) {
//                PersonalNutritionView(navController, viewModel)
            }
            composable(
                route = "foodList/{category}",
                arguments = listOf(navArgument("category") { type = NavType.StringType })
            ) { backStackEntry ->
                val category = backStackEntry.arguments?.getString("category")
//                NutritionListView(navController, viewModel, category ?: "breakfast")
            }
            composable(
                route = "foodDetail/{foodName}",
                arguments = listOf(navArgument("foodName") { type = NavType.StringType })
            ) { backStackEntry ->
                val foodName = backStackEntry.arguments?.getString("foodName")
//                val selectedFood = viewModel.allFoods.value?.find { it.name == foodName }
//                if (selectedFood != null) {
//                    NutritionFormView(navController = navController, food = selectedFood)
//                }
            }
            composable(Routes.HealthScreen.value) {
                val userId = getCurrentUserId()
                if (userId != null) {
                    HealthScreen(userId, navController)
                }
            }
            composable(Routes.NewHealthRecord.value) {
                val userId = getCurrentUserId()
                if (userId != null) {
                    CreateHealthRecord(
                        HealthRecord = HealthRecord(),
                        userId = userId,
                        onSaveMetrics = { updatedRecord ->
                            // Here you would handle the saving logic, e.g., updating a database or state
                        },
                        navController = navController
                    )
                }
            }
            composable(Routes.CalendarScreen.value) {
                CalendarScreen()
            }
            composable(Routes.Profile.value) {
                val userId = getCurrentUserId()
                if (userId != null) {
                    ProfileSettingsScreen(navController, viewModel, userId)
                }
            }
        }
    }
}

@Composable
fun getCurrentRoute(navController: NavController): String? {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    return navBackStackEntry?.destination?.route
}

fun getCurrentUserId(): String? {
    val currentUser = FirebaseAuth.getInstance().currentUser
    return currentUser?.uid  // return the user's ID or null if no user is logged in
}
