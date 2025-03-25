package com.example.furfamily.nutrition

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.furfamily.getCurrentUserId
import com.example.furfamily.profile.Pet
import com.example.furfamily.viewmodel.HealthViewModel
import com.example.furfamily.viewmodel.NutritionViewModel
import com.example.furfamily.viewmodel.ProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NutritionScreen(navController: NavController) {
    val nutritionViewModel: NutritionViewModel = hiltViewModel()
    val profileViewModel: ProfileViewModel = hiltViewModel()

    val userId = getCurrentUserId()
    val foods by nutritionViewModel.foodList.observeAsState(emptyList())
    val pets by profileViewModel.pets.observeAsState(emptyList())
    var isPetDropdownExpanded by remember { mutableStateOf(false) }
    val latestWeight by nutritionViewModel.latestWeight.observeAsState(null)
    var selectedPet by remember { mutableStateOf<Pet?>(null) }
    var selectedFood by remember { mutableStateOf<Food?>(null) }
    var showSnackbar by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf("") }
    var showEditFoodDialog by remember { mutableStateOf(false) }
    var showFeedingDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        nutritionViewModel.loadFoodList()
        if (userId != null) {
            profileViewModel.loadPetsProfile(userId)
        }
        nutritionViewModel.fetchLatestWeight()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nutrition and Feeding", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { navController.navigate("CreateEditFood") }) {
                        Icon(Icons.Filled.Add, contentDescription = "Add New Food")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        content = { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    // Pet Dropdown
                    Text(
                        text = "You can add a feeding record here!\nFirst, select a pet name:",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                    ExposedDropdownMenuBox(
                        expanded = isPetDropdownExpanded,
                        onExpandedChange = { isPetDropdownExpanded = !isPetDropdownExpanded }
                    ) {
                        TextField(
                            readOnly = true,
                            value = selectedPet?.name ?: "Please choose one",
                            onValueChange = { /* No-op */ },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isPetDropdownExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = isPetDropdownExpanded,
                            onDismissRequest = { isPetDropdownExpanded = false }
                        ) {
                            pets.forEach { pet ->
                                DropdownMenuItem(
                                    text = { Text(pet.name) },
                                    onClick = {
                                        selectedPet = pet
                                        isPetDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Food List
                    Text(
                        text = "Then, select a food product:",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(foods) { food ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (selectedPet == null) {
                                            snackbarMessage = "Please select a pet first!"
                                            showSnackbar = true
                                        } else {
                                            selectedFood = food
                                            showFeedingDialog = true
                                        }
                                    }
                                    .padding(vertical = 8.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f).padding(8.dp)) {
                                    Text(food.name, style = MaterialTheme.typography.bodyLarge)
                                    Text("Calories: ${food.caloriesPerKg} kcal/kg", style = MaterialTheme.typography.bodySmall)
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    IconButton(onClick = { showEditFoodDialog = true; selectedFood = food }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit Food")
                                    }
                                    IconButton(onClick = { nutritionViewModel.deleteFood(food) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete Food")
                                    }
                                }
                            }
                            Divider()
                        }
                    }

                    if (showSnackbar) {
                        LaunchedEffect(snackbarMessage) {
                            snackbarHostState.showSnackbar(snackbarMessage)
                            showSnackbar = false
                        }
                    }
                }
            }
        }
    )

    // Edit Food Dialog
    if (showEditFoodDialog && selectedFood != null) {
        EditFoodDialog(
            food = selectedFood!!,
            onDismiss = { showEditFoodDialog = false },
            onSave = { updatedFood ->
                nutritionViewModel.updateFood(updatedFood)
                showEditFoodDialog = false
            }
        )
    }

    // Feeding Record Dialog
    if (showFeedingDialog && selectedPet != null && selectedFood != null) {
        CreateFeedingRecord(
            selectedPet = selectedPet!!,
            selectedFood = selectedFood!!,
            latestWeight = latestWeight,
            onSave = { amount, mealTime, mealType, notes, lifestyle, gestationLactation ->
                nutritionViewModel.saveFeeding(selectedPet!!, selectedFood!!, amount, mealTime, mealType, notes)
                showFeedingDialog = false
            },
            onDismiss = { showFeedingDialog = false }
        )
    }
}
