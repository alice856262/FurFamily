package com.example.furfamily.nutrition

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Divider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.example.furfamily.ViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NutritionScreen(viewModel: ViewModel) {
    LaunchedEffect(Unit) {
        viewModel.loadFoodList()
        viewModel.fetchLatestWeight()
    }

    val foods by viewModel.foodList.observeAsState(emptyList())
    val latestWeight by viewModel.latestWeight.observeAsState(null)
    var selectedFood by remember { mutableStateOf<Food?>(null) }
    var notes by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf(0.0F) }
    var mealType by remember { mutableStateOf("") }
    var isExpanded by remember { mutableStateOf(false) }
    var feedingSaved by remember { mutableStateOf(false) }
    val mealOptions = listOf("Breakfast", "Lunch", "Snack", "Dinner", "Midnight snack")
    var showAddFoodDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nutrition", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { showAddFoodDialog = true }) {
                        Icon(Icons.Filled.Add, contentDescription = "Add New Food")
                    }
                }
            )
        },
        content = { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    "Select a food to add feeding record!",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(foods) { food ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    // Reset form when a new food is selected
                                    selectedFood = food
                                    notes = ""
                                    amount = 0.0F
                                    mealType = ""
                                    feedingSaved = false
                                }
                                .padding(vertical = 8.dp)
                        ) {
                            Text(
                                food.name,
                                modifier = Modifier.padding(8.dp),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                        Divider()
                    }
                }
                selectedFood?.let {
                    if (!feedingSaved) {
                        Column(modifier = Modifier.padding(top = 12.dp)) {
                            latestWeight?.let { weight ->
                                val recommendedAmount = calculateRecommendedAmount(weight, it)
                                Text(
                                    "Recommended amount: ${"%.2f".format(recommendedAmount)} g",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                            } ?: Text("Fetching weight...", style = MaterialTheme.typography.bodyMedium)
                            TextField(
                                value = amount.toString(),
                                onValueChange = { amount = it.toFloat() },
                                label = { Text("Amount") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp)
                            )
                            ExposedDropdownMenuBox(
                                expanded = isExpanded,
                                onExpandedChange = { isExpanded = !isExpanded },
                            ) {
                                TextField(
                                    readOnly = true,
                                    value = mealType,
                                    onValueChange = { /* No-op since it is read-only */ },
                                    label = { Text("Select Meal Type") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpanded) },
                                    modifier = Modifier
                                        .menuAnchor()
                                        .fillMaxWidth()
                                        .padding(bottom = 8.dp)
                                )
                                ExposedDropdownMenu(
                                    expanded = isExpanded,
                                    onDismissRequest = { isExpanded = false }
                                ) {
                                    mealOptions.forEach { option ->
                                        DropdownMenuItem(
                                            text = { Text(option) },
                                            onClick = {
                                                mealType = option
                                                isExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                            TextField(
                                value = notes,
                                onValueChange = { notes = it },
                                label = { Text("Notes") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp)
                            )
                            Button(
                                onClick = {
                                    viewModel.saveFeeding(selectedFood!!, amount, notes, mealType)
                                    feedingSaved = true // Set feedingSaved to true after saving
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text("Save Feeding", color = Color.White)
                            }
                        }
                    } else {
                        Text("Feeding record saved successfully!",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.secondaryContainer)
                                .padding(12.dp)
                                .fillMaxWidth(),
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
            if (showAddFoodDialog) {
                AddFoodDialog(
                    onDismiss = { showAddFoodDialog = false },
                    onSave = { food ->
                        viewModel.addNewFood(food)
                    }
                )
            }
        }
    )
}

@Composable
fun AddFoodDialog(onDismiss: () -> Unit, onSave: (Food) -> Unit) {
    var name by remember { mutableStateOf("") }
    var calories by remember { mutableStateOf(0.0F) }
    var waterContent by remember { mutableStateOf(0.0F) }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Food", style = MaterialTheme.typography.titleMedium) },
        text = {
            Column {
                TextField(value = name, onValueChange = { name = it }, label = { Text("Food Name") }, modifier = Modifier.padding(vertical = 4.dp))
                TextField(value = calories.toString(), onValueChange = { calories = it.toFloat() }, label = { Text("Calories per 100g") }, modifier = Modifier.padding(vertical = 4.dp))
                TextField(value = waterContent.toString(), onValueChange = { waterContent = it.toFloat() }, label = { Text("Water content per 100g") }, modifier = Modifier.padding(vertical = 4.dp))
                TextField(value = notes, onValueChange = { notes = it }, label = { Text("Notes") }, modifier = Modifier.padding(vertical = 4.dp))
            }
        },
        confirmButton = {
            Button(onClick = {
                val food = Food(name = name, caloriesPer100g = calories, waterContentPer100g = waterContent, notes = notes)
                onSave(food)
                onDismiss()
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

fun calculateRecommendedAmount(weight: Float, food: Food): Float {
    if (weight == 0.0F) return 0.0F
    // Basic formula for calorie needs based on the pet's weight
    val calorieRequirement = weight * 30 + 70
    val foodCalories = food.caloriesPer100g

    return calorieRequirement / foodCalories * 100  // Return amount of food in grams
}