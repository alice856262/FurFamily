package com.example.furfamily.nutrition

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavController
import com.example.furfamily.ViewModel
import com.example.furfamily.getCurrentUserId
import com.example.furfamily.profile.Pet
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NutritionScreen(viewModel: ViewModel, navController: NavController) {
    val userId = getCurrentUserId()
    val foods by viewModel.foodList.observeAsState(emptyList())
    val pets by viewModel.pets.observeAsState(emptyList())
    val latestWeight by viewModel.latestWeight.observeAsState(null)
    var selectedPet by remember { mutableStateOf<Pet?>(null) }
    var selectedFood by remember { mutableStateOf<Food?>(null) }
    var amount by remember { mutableStateOf(0.0F) }
    var mealTime by remember { mutableStateOf(LocalDateTime.now()) }
    var mealType by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var isPetDropdownExpanded by remember { mutableStateOf(false) }
    var feedingSaved by remember { mutableStateOf(false) }
    var showSnackbar by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf("") }

    val mealOptions = listOf("Breakfast", "Lunch", "Snack", "Dinner")

    LaunchedEffect(Unit) {
        viewModel.loadFoodList()
        if (userId != null) {
            viewModel.loadPetsProfile(userId)
        }
        viewModel.fetchLatestWeight()
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
        content = { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    // Pet Dropdown
                    ExposedDropdownMenuBox(
                        expanded = isPetDropdownExpanded,
                        onExpandedChange = { isPetDropdownExpanded = !isPetDropdownExpanded }
                    ) {
                        TextField(
                            readOnly = true,
                            value = selectedPet?.name ?: "Select a pet",
                            onValueChange = { /* No-op */ },
                            label = { Text("Pet") },
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
                        text = "Select a food product to add feeding records!",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(foods) { food ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedFood = food
                                        amount = 0.0F
                                        mealType = ""
                                        notes = ""
                                        feedingSaved = false
                                    }
                                    .padding(vertical = 8.dp)
                            ) {
                                Text(
                                    food.name,
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(8.dp),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                            Divider()
                        }
                    }

                    // Feeding Form
                    selectedPet?.let { pet ->
                        selectedFood?.let { food ->
                            if (!feedingSaved) {
                                Column(modifier = Modifier.padding(top = 12.dp)) {
                                    latestWeight?.let { weight ->
                                        val recommendedAmount = calculateRecommendedAmount(weight, food)
                                        Text(
                                            text = "Recommended amount: ${"%.2f".format(recommendedAmount)} g",
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.padding(bottom = 8.dp)
                                        )
                                    }

                                    TextField(
                                        value = amount.toString(),
                                        onValueChange = { amount = it.toFloatOrNull() ?: 0.0F },
                                        label = { Text("Amount") },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 8.dp)
                                    )
                                    DateTimePicker("Meal Time", mealTime) { mealTime = it }
                                    ExposedDropdownMenuBox(
                                        expanded = feedingSaved,
                                        onExpandedChange = { feedingSaved = !feedingSaved },
                                    ) {
                                        TextField(
                                            readOnly = true,
                                            value = mealType,
                                            onValueChange = { /* No-op */ },
                                            label = { Text("Meal Type") },
                                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = feedingSaved) },
                                            modifier = Modifier
                                                .menuAnchor()
                                                .fillMaxWidth()
                                                .padding(bottom = 8.dp)
                                        )
                                        ExposedDropdownMenu(
                                            expanded = feedingSaved,
                                            onDismissRequest = { feedingSaved = false }
                                        ) {
                                            mealOptions.forEach { option ->
                                                DropdownMenuItem(
                                                    text = { Text(option) },
                                                    onClick = {
                                                        mealType = option
                                                        feedingSaved = false
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
                                            if (selectedPet != null && selectedFood != null) {
                                                viewModel.saveFeeding(selectedPet!!, selectedFood!!, amount, mealTime, mealType, notes)
                                                feedingSaved = true
                                                showSnackbar = true
                                                snackbarMessage = "Feeding record saved successfully!"
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        enabled = selectedPet != null && selectedFood != null
                                    ) {
                                        Text("Save Feeding Record")
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            selectedPet = null
                                            selectedFood = null
                                            amount = 0.0F
                                            mealType = ""
                                            notes = ""
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Cancel")
                                    }
                                }
                            } else {
                                Text(
                                    text = "Feeding record saved successfully!",
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
                }
            }
        }
    )
}

fun calculateRecommendedAmount(weight: Float, food: Food): Float {
    if (weight <= 0.0F) return 0.0F

    // Calculate RER based on weight
    val rer = 70 * Math.pow(weight.toDouble(), 0.75).toFloat()

    val foodCalories = food.caloriesPerKg
    if (foodCalories <= 0) return 0.0F  // Avoid division by zero
    return (rer / foodCalories) * 1000  // Convert kg to grams
}

@Composable
fun DateTimePicker(label: String, dateTime: LocalDateTime, onDateTimeChanged: (LocalDateTime) -> Unit) {
    val context = LocalContext.current
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var tempDate by remember { mutableStateOf(dateTime) }

    if (showDatePicker) {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                tempDate = tempDate.withYear(year).withMonth(month + 1).withDayOfMonth(dayOfMonth)
                showDatePicker = false
                showTimePicker = true // Open time picker after date selection
            },
            tempDate.year,
            tempDate.monthValue - 1,
            tempDate.dayOfMonth
        ).show()
    }

    if (showTimePicker) {
        TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                val finalDateTime = tempDate.withHour(hourOfDay).withMinute(minute)
                onDateTimeChanged(finalDateTime)
                showTimePicker = false // Close time picker after selection
            },
            tempDate.hour,
            tempDate.minute,
            true
        ).show()
    }

    Column {
        TextButton(onClick = { showDatePicker = true }) {
            Text(text = "$label: ${dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))}")
        }
    }
}