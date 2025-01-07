package com.example.furfamily.calendar

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.furfamily.ViewModel
import com.example.furfamily.profile.Pet
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateCalendarEvent(
    viewModel: ViewModel,
    userId: String,
    pets: List<Pet>,
    onEventCreated: () -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var startTime by remember { mutableStateOf(LocalDateTime.now()) }
    var endTime by remember { mutableStateOf(LocalDateTime.now().plusHours(1)) }
    var location by remember { mutableStateOf("") }
    var selectedPet by remember { mutableStateOf<Pet?>(null) }
    var isExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Calendar Event") },
        text = {
            Column {
                TextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                )
                TextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                )
                DateTimePicker("Start Time", startTime) { startTime = it }
                DateTimePicker("End Time", endTime) { endTime = it }
                TextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Location") },
                )

                // Pet Dropdown for selecting petId
                if (pets.isNotEmpty()) {
                    ExposedDropdownMenuBox(expanded = isExpanded, onExpandedChange = { isExpanded = it }) {
                        TextField(
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                                .focusProperties {
                                    canFocus = false
                                }
                                .padding(bottom = 8.dp),
                            readOnly = true,
                            value = selectedPet?.name ?: "Select Pet",
                            onValueChange = {},
                            label = { Text("Pet") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpanded) }
                        )
                        ExposedDropdownMenu(
                            expanded = isExpanded,
                            onDismissRequest = { isExpanded = false }
                        ) {
                            pets.forEach { pet ->
                                androidx.compose.material3.DropdownMenuItem(
                                    text = { Text(pet.name) },
                                    onClick = {
                                        selectedPet = pet
                                        isExpanded = false
                                    },
                                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                                )
                            }
                        }
                    }
                } else {
                    Text("No pets available", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(8.dp))
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val event = CalendarEvent(
                        userId = userId,
                        petId = selectedPet?.petId ?: "",
                        title = title,
                        description = description,
                        startTime = startTime,
                        endTime = endTime,
                        location = location
                    )
                    viewModel.createEvent(event) {
                        onEventCreated() // Notify the screen to hide the create event section
                    }
                },
                enabled = title.isNotBlank() && startTime.isBefore(endTime) && selectedPet != null // Enable button if conditions are met
            ) {
                Text("Save Event")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
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
