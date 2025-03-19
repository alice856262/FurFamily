package com.example.furfamily

import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.furfamily.calendar.CalendarScreen
import com.example.furfamily.calendar.CreateCalendarEvent
import com.example.furfamily.health.CreateHealthRecord
import com.example.furfamily.health.HealthRecord
import com.example.furfamily.health.HealthScreen
import com.example.furfamily.map.MapScreen
import com.example.furfamily.nutrition.AddNewFood
import com.example.furfamily.nutrition.NutritionScreen
import com.example.furfamily.profile.AddEditPetScreen
import com.example.furfamily.profile.LoginScreen
import com.example.furfamily.profile.ProfileSettingsScreen
import com.example.furfamily.profile.RegistrationScreen
import com.example.furfamily.profile.loginWithEmailPassword
import com.google.firebase.auth.FirebaseAuth

@Composable
fun BottomNavigationBar(navController: NavController) {
    BottomNavigation(
        backgroundColor = MaterialTheme.colorScheme.background
    ) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination
        val navBarItems = NavBarItem.NavBarItems()

        navBarItems.forEach { navItem ->
            BottomNavigationItem(
                icon = {
                    Image(
                        painter = navItem.icon, // Use Painter for custom image icons
                        contentDescription = navItem.label,
                        modifier = Modifier.size(24.dp)
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
            composable(Routes.CalendarScreen.value) {
                CalendarScreen(viewModel)
            }
            composable(Routes.HealthScreen.value) {
                val userId = getCurrentUserId()
                if (userId != null) {
                    HealthScreen(viewModel, userId, navController)
                }
            }
            composable("${Routes.NewHealthRecord.value}/{petId}") { backStackEntry ->
                val petId = backStackEntry.arguments?.getString("petId")
                val userId = getCurrentUserId()

                if (userId != null && petId != null) {
                    CreateHealthRecord(
                        healthRecord = HealthRecord(),
                        userId = userId,
                        pets = viewModel.pets.value ?: emptyList(),
                        onSaveMetrics = { updatedRecord ->
                            // Handle the saved record
                        },
                        navController = navController
                    )
                }
            }
            composable(Routes.Nutrition.value) {
                NutritionScreen(viewModel, navController)
            }
            composable(Routes.NewFood.value) {
                AddNewFood(
                    onDismiss = { navController.popBackStack() },
                    onSave = { food -> viewModel.addNewFood(food) },
                    viewModel = viewModel,
                    navController = navController
                )
            }
            composable(Routes.Map.value) {
                MapScreen(viewModel)
            }
            composable(Routes.Profile.value) {
                val userId = getCurrentUserId()
                if (userId != null) {
                    ProfileSettingsScreen(navController, viewModel, userId)
                }
            }
            composable(Routes.AddEditPet.value) {
                val userId = getCurrentUserId()
                if (userId != null) {
                    AddEditPetScreen(navController, viewModel, userId)
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
