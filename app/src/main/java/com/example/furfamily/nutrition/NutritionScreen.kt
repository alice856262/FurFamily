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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavController
import com.example.furfamily.ViewModel
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NutritionScreen(viewModel: ViewModel, navController: NavController) {
    LaunchedEffect(Unit) {
        viewModel.loadFoodList()
        viewModel.fetchLatestWeight()
    }

    val foods by viewModel.foodList.observeAsState(emptyList())
    val latestWeight by viewModel.latestWeight.observeAsState(null)
    var selectedFood by remember { mutableStateOf<Food?>(null) }
    var amount by remember { mutableStateOf(0.0F) }
    var mealTime by remember { mutableStateOf(LocalDateTime.now()) }
    var mealType by remember { mutableStateOf("") }
    val mealOptions = listOf("Breakfast", "Lunch", "Snack", "Dinner")
    var notes by remember { mutableStateOf("") }
    var isExpanded by remember { mutableStateOf(false) }
    var feedingSaved by remember { mutableStateOf(false) }
    var showEditFoodDialog by remember { mutableStateOf(false) }
    var foodToEdit by remember { mutableStateOf<Food?>(null) }
    var showSnackbar by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf("") }

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
            // Adding Box to properly handle paddingValues and content layout
            Box(modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    Text("Select a food to add feeding record!",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 12.dp))
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(foods) { food ->
                            Row(modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        // Reset form when a new food is selected
                                        selectedFood = food
                                        amount = 0.0F
                                        mealType = ""
                                        notes = ""
                                        feedingSaved = false }
                                    .padding(vertical = 8.dp)
                            ) {
                                Text(
                                    food.name,
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(8.dp),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                IconButton(
                                    onClick = {
                                        foodToEdit = food
                                        showEditFoodDialog = true
                                    }
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit Food")
                                }
                            }
                            Divider()
                        }
                    }
                    selectedFood?.let {
                        if (!feedingSaved) {
                            Column(modifier = Modifier.padding(top = 12.dp)) {
                                latestWeight?.let { weight ->
                                    val recommendedAmount = calculateRecommendedAmount(weight, it)
                                    Text("Recommended amount: ${"%.2f".format(recommendedAmount)} g",
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
                                DateTimePicker("Meal Time", mealTime) { mealTime = it }
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
                                                    isExpanded = false }
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
                                        viewModel.saveFeeding(selectedFood!!, amount, mealTime, mealType, notes)
                                        feedingSaved = true // Set feedingSaved to true after saving
                                        showSnackbar = true // Show Snackbar when saved
                                        snackbarMessage = "Feeding record saved successfully!" },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Text("Save Feeding Record", color = Color.White)
                                }
                                OutlinedButton(
                                    onClick = {
                                        selectedFood = null
                                        amount = 0.0F
                                        mealType = ""
                                        notes = "" },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Cancel")
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

                if (showSnackbar) {
                    Snackbar(
                        action = { Button(onClick = { showSnackbar = false }) {Text("OK") } },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp)
                    ) {
                        Text(snackbarMessage)
                    }
                }

                if (showEditFoodDialog && foodToEdit != null) {
                    EditFoodDialog(
                        food = foodToEdit!!,
                        onDismiss = { showEditFoodDialog = false },
                        onSave = { updatedFood ->
                            viewModel.updateFood(updatedFood)
                            showEditFoodDialog = false }
                    )
                }
            }
        }
    )
}

fun calculateRecommendedAmount(weight: Float, food: Food): Float {
    if (weight == 0.0F) return 0.0F
    // Basic formula for calorie needs based on the pet's weight
    val calorieRequirement = weight * 30 + 70
    val foodCalories = food.caloriesPerKg

    return calorieRequirement / foodCalories * 1000  // Return amount of food in grams
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