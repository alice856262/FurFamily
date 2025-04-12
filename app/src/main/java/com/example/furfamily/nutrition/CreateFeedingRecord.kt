package com.example.furfamily.nutrition

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Divider
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.furfamily.profile.Pet
import com.example.furfamily.R
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Date
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material3.Surface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateFeedingRecord(
    selectedPet: Pet,
    selectedFood: Food,
    latestWeight: Float?,
    onSave: (Float, LocalDateTime, String, String, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var amount by remember { mutableStateOf(0.0F) }
    var mealTime by remember { mutableStateOf(LocalDateTime.now()) }
    var mealType by remember { mutableStateOf("") }
    var isMealExpanded by remember { mutableStateOf(false) }
    val mealOptions = listOf("Breakfast", "Lunch", "Snack", "Dinner")
    var notes by remember { mutableStateOf("") }

    // Lifestyle options
    val lifestyleOptions = listOf("Normal", "Inactive Lifestyle", "Obese/Weight Loss")
    var selectedLifestyle by remember { mutableStateOf("Normal") }

    // Gestation and lactation options
    val gestationOptions = listOf("No", "Gestation", "Lactation", "Gestation and Lactation")
    var selectedGestation by remember { mutableStateOf("No") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Record for ${selectedPet.name}") },
        text = {
            Column {
                latestWeight?.let {
                    Text(
                        text = "Latest Body Weight: ${"%.1f".format(latestWeight)} kg",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                Spacer(modifier = Modifier.height(8.dp))

                // Lifestyle Question
                Text("Lifestyle:", style = MaterialTheme.typography.titleSmall)
                lifestyleOptions.forEach { option ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        RadioButton(
                            selected = selectedLifestyle == option,
                            onClick = { selectedLifestyle = option },
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(option)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                // Gestation and Lactation Question
                Text("Gestation and Lactation:", style = MaterialTheme.typography.titleSmall)
                gestationOptions.forEach { option ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        RadioButton(
                            selected = selectedGestation == option,
                            onClick = { selectedGestation = option },
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(option)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                Divider()
                Spacer(modifier = Modifier.height(12.dp))

                Text("Selected Food: ${selectedFood.name}", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(8.dp))

                // Recommended Amount with Conversion
                latestWeight?.let {
                    val recommendedAmount = calculateRecommendedAmount(
                        weight = latestWeight,
                        food = selectedFood,
                        petType = selectedPet.type,
                        selectedSex = selectedPet.selectedSex,
                        birthDate = selectedPet.birthDate,
                        lifestyle = selectedLifestyle,
                        gestationLactation = selectedGestation
                    )

                    val formattedAmount = "%.1f".format(recommendedAmount)
                    val (portionText, iconResId, count, decimalPart) = when (selectedFood.category) {
                        "Dry Food" -> {
                            val cups = recommendedAmount / 120
                            val wholeCups = cups.toInt()
                            val decimal = cups - wholeCups
                            Log.d("FeedingRecord", "Dry Food - cups: $cups, wholeCups: $wholeCups, decimal: $decimal")
                            val text = if (decimal > 0) {
                                "(%.1f cups)".format(cups)
                            } else {
                                "(${wholeCups} cups)"
                            }
                            Quad(text, R.drawable.pet_food_cup, wholeCups, decimal)
                        }
                        "Wet Food" -> {
                            val cans = recommendedAmount / selectedFood.size
                            val wholeCans = cans.toInt()
                            val decimal = cans - wholeCans
                            Log.d("FeedingRecord", "Wet Food - cans: $cans, wholeCans: $wholeCans, decimal: $decimal")
                            val text = if (decimal > 0) {
                                "(%.1f cans)".format(cans)
                            } else {
                                "(${wholeCans} cans)"
                            }
                            Quad(text, R.drawable.pet_food_can, wholeCans, decimal)
                        }
                        else -> Quad("", null, 0, 0f)
                    }

                    Text(
                        text = "Daily Recommended Amount: ${formattedAmount} g/day",
                        style = MaterialTheme.typography.titleSmall
                    )
                    if (portionText.isNotEmpty() && iconResId != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            // Show icons based on count
                            repeat(count) {
                                Image(
                                    painter = painterResource(id = iconResId),
                                    contentDescription = if (selectedFood.category == "Dry Food") "Food cup" else "Food can",
                                    modifier = Modifier.size(16.dp),
                                    contentScale = ContentScale.FillBounds
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                            }
                            // If there's a decimal part, show proportional icon
                            if (decimalPart > 0f) {
                                Box(
                                    modifier = Modifier.size(16.dp)
                                ) {
                                    Image(
                                        painter = painterResource(id = iconResId),
                                        contentDescription = if (selectedFood.category == "Dry Food") 
                                            "${(decimalPart * 100).toInt()}% food cup" 
                                        else 
                                            "${(decimalPart * 100).toInt()}% food can",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.FillBounds
                                    )
                                    // Overlay to cover the right portion
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .width((16 * (1 - decimalPart)).dp)
                                            .align(Alignment.TopEnd),
                                        color = MaterialTheme.colorScheme.surface,
                                        tonalElevation = 0.dp,
                                        shadowElevation = 0.dp
                                    ) { }
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                            } else {
                                Log.d("FeedingRecord", "No decimal part to show")
                            }
                            Text(
                                text = portionText,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                DateTimePicker("Meal Time", mealTime) { mealTime = it }

                TextField(
                    value = amount.toString(),
                    onValueChange = { amount = it.toFloatOrNull() ?: 0.0F },
                    label = { Text("Amount (grams) *", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    colors = TextFieldDefaults.textFieldColors(
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        containerColor = MaterialTheme.colorScheme.surface,
                        focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                        unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Meal Type Dropdown
                ExposedDropdownMenuBox(
                    expanded = isMealExpanded,
                    onExpandedChange = { isMealExpanded = !isMealExpanded }
                ) {
                    TextField(
                        readOnly = true,
                        value = if (mealType.isNotEmpty()) mealType else "Please choose one",
                        onValueChange = { /* No-op */ },
                        label = { Text("Meal Type", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                        colors = TextFieldDefaults.textFieldColors(
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            containerColor = MaterialTheme.colorScheme.surface,
                            focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                            unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isMealExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = isMealExpanded,
                        onDismissRequest = { isMealExpanded = false }
                    ) {
                        mealOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    mealType = option
                                    isMealExpanded = false
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                TextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    colors = TextFieldDefaults.textFieldColors(
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        containerColor = MaterialTheme.colorScheme.surface,
                        focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                        unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                OutlinedButton(onClick = onDismiss) {
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        onSave(amount, mealTime, mealType, notes, selectedLifestyle, selectedGestation)
                    },
                    enabled = amount > 0
                ) {
                    Text("Save Record")
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}

fun calculateRecommendedAmount(
    weight: Float,
    food: Food,
    petType: String,
    selectedSex: String,
    birthDate: Date,
    lifestyle: String,
    gestationLactation: String
): Float {
    if (weight <= 0.0F) return 0.0F

    val birthLocalDate = birthDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
    val currentAgeYears = ChronoUnit.YEARS.between(birthLocalDate, LocalDate.now()).toInt()
    val currentAgeMonths = ChronoUnit.MONTHS.between(birthLocalDate, LocalDate.now()).toInt()

    // Calculate RER based on weight
    val rer = 70 * Math.pow(weight.toDouble(), 0.75).toFloat()

    // Determine life stage factor based on pet type, age, and sex
    var lifeStageFactor = when (petType.lowercase()) {
        "dog" -> when {
            currentAgeMonths < 4 -> 3.0F // Puppies under 4 months
            currentAgeMonths in 4..12 -> 2.0F // Puppies 4 months to 1 year
            selectedSex.lowercase() in listOf("neutered male", "spayed female") -> 1.4F // Neutered adult dogs
            selectedSex.lowercase() in listOf("intact male", "intact female") -> 1.6F // Intact adult dogs
            else -> 1.4F // Default factor for adult dogs
        }
        "cat" -> when {
            currentAgeYears < 1 -> 2.5F // Kittens up to 1 year old
            selectedSex.lowercase() in listOf("neutered male", "spayed female") -> 1.2F // Neutered adult cats
            selectedSex.lowercase() in listOf("intact male", "intact female") -> 1.4F // Intact adult cats
            else -> 1.2F // Default factor for adult cats
        }
        else -> 1.0F // Default factor for unknown pet types
    }

    Log.d("lifeStageFactor", "lifeStageFactor: $lifeStageFactor")

    // Adjust for lifestyle
    lifeStageFactor = when (lifestyle) {
        "Inactive Lifestyle" -> 1.0F
        "Obese/Weight Loss" -> if (petType.lowercase() == "cat") 0.8F else 1.0F
        else -> lifeStageFactor
    }

    // Adjust for gestation and lactation
    lifeStageFactor = when (gestationLactation) {
        "Gestation" -> if (petType.lowercase() == "cat") 1.6F else 3.0F
        "Lactation" -> if (petType.lowercase() == "cat") 2.0F else 3.0F
        "Gestation and Lactation" -> if (petType.lowercase() == "cat") 3.6F else 6.0F
        else -> lifeStageFactor
    }

    Log.d("lifeStageFactor", "lifeStageFactor: $lifeStageFactor, lifestyle: $lifestyle, gestationLactation: $gestationLactation")

    // Adjusted energy requirement (MER)
    val mer = rer * lifeStageFactor

    // Calculate food requirement
    val foodCalories = food.caloriesPerKg
    if (foodCalories <= 0) return 0.0F // Avoid division by zero
    return (mer / foodCalories) * 1000 // Convert kg to grams
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

data class Quad<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)