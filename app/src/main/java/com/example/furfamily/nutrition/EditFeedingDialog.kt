package com.example.furfamily.nutrition

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.furfamily.profile.Pet
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
fun EditFeedingDialog(
    feeding: Feeding,
    pet: Pet,
    food: Food,
    onDismiss: () -> Unit,
    onSave: (Feeding) -> Unit
) {
    var amount by remember { mutableStateOf(feeding.amount.toString()) }
    var mealTime by remember { mutableStateOf(feeding.mealTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))) }
    var mealType by remember { mutableStateOf(feeding.mealType) }
    var notes by remember { mutableStateOf(feeding.notes) }
    
    val scrollState = rememberScrollState()
    
    Dialog(onDismissRequest = onDismiss) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Edit Feeding Record") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(scrollState)
                        .padding(vertical = 8.dp)
                ) {
                    Text(
                        text = "Pet: ${pet.name}",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    Text(
                        text = "Food: ${food.name}",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it },
                        label = { Text("Amount (g)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    OutlinedTextField(
                        value = mealTime,
                        onValueChange = { mealTime = it },
                        label = { Text("Meal Time (yyyy-MM-dd HH:mm)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    OutlinedTextField(
                        value = mealType,
                        onValueChange = { mealType = it },
                        label = { Text("Meal Type (e.g., Breakfast, Lunch, Dinner)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notes (optional)") },
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
                            try {
                                val amountFloat = amount.toFloatOrNull() ?: 0f
                                val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                                val parsedDateTime = LocalDateTime.parse(mealTime, dateTimeFormatter)

                                val updatedFeeding = feeding.copy(
                                    amount = amountFloat,
                                    mealTime = parsedDateTime,
                                    mealType = mealType,
                                    notes = notes
                                )

                                onSave(updatedFeeding)
                            } catch (e: Exception) {
                                // Handle parsing errors
                            }
                        }
                    ) {
                        Text("Save")
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
} 