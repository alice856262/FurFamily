package com.example.furfamily.calendar

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.furfamily.profile.Pet
import com.example.furfamily.viewmodel.CalendarViewModel
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateCalendarEvent(
    calendarViewModel: CalendarViewModel,
    userId: String,
    pets: List<Pet>,
    onEventCreated: () -> Unit,
    onDismiss: () -> Unit
) {
    val defaultTitles = listOf("Vet Appointment", "Grooming", "Playdate", "Training Session", "Medication", "Other")
    var selectedTitle by remember { mutableStateOf(defaultTitles.first()) }
    var customTitle by remember { mutableStateOf("") }
    var isTitleDropdownExpanded by remember { mutableStateOf(false) }
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
                // Event Title Selection Dropdown
                ExposedDropdownMenuBox(expanded = isTitleDropdownExpanded, onExpandedChange = { isTitleDropdownExpanded = it }) {
                    TextField(
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                            .focusProperties { canFocus = false },
                        readOnly = true,
                        value = selectedTitle,
                        onValueChange = {},
                        label = { Text("Title", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                        colors = TextFieldDefaults.textFieldColors(
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            containerColor = MaterialTheme.colorScheme.surface,
                            focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                            unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isTitleDropdownExpanded) }
                    )
                    ExposedDropdownMenu(
                        expanded = isTitleDropdownExpanded,
                        onDismissRequest = { isTitleDropdownExpanded = false }
                    ) {
                        defaultTitles.forEach { title ->
                            DropdownMenuItem(
                                text = { Text(title) },
                                onClick = {
                                    selectedTitle = title
                                    isTitleDropdownExpanded = false
                                    if (title != "Other") customTitle = "" // Reset custom title when a predefined one is selected
                                },
                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                            )
                        }
                    }
                }

                // If "Other" is selected, show a TextField for custom input
                if (selectedTitle == "Other") {
                    TextField(
                        modifier = Modifier.padding(top = 8.dp),
                        value = customTitle,
                        onValueChange = { customTitle = it },
                        label = { Text("Custom Title", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                        colors = TextFieldDefaults.textFieldColors(
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            containerColor = MaterialTheme.colorScheme.surface,
                            focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                            unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                TextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    colors = TextFieldDefaults.textFieldColors(
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        containerColor = MaterialTheme.colorScheme.surface,
                        focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                        unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))

                TextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Location", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    colors = TextFieldDefaults.textFieldColors(
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        containerColor = MaterialTheme.colorScheme.surface,
                        focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                        unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))

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
                            value = selectedPet?.name ?: "Please choose one",
                            onValueChange = {},
                            label = { Text("Pet Name", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                            colors = TextFieldDefaults.textFieldColors(
                                focusedLabelColor = MaterialTheme.colorScheme.primary,
                                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                containerColor = MaterialTheme.colorScheme.surface,
                                focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                                unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpanded) }
                        )
                        ExposedDropdownMenu(
                            expanded = isExpanded,
                            onDismissRequest = { isExpanded = false }
                        ) {
                            pets.forEach { pet ->
                                DropdownMenuItem(
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

                DateTimePicker("Start Time", startTime) { startTime = it }
                DateTimePicker("End Time", endTime) { endTime = it }
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
                        val eventTitle = if (selectedTitle == "Other") customTitle else selectedTitle
                        val event = CalendarEvent(
                            userId = userId,
                            petId = selectedPet?.petId ?: "",
                            title = eventTitle,
                            description = description,
                            startTime = startTime,
                            endTime = endTime,
                            location = location
                        )
                        calendarViewModel.createEvent(
                            calendarEvent = event,
                            function = {
                                calendarViewModel.loadEventsForDate(event.startTime.toLocalDate()) // Refresh the list
                                onEventCreated()
                            }
                        )
                    },
                    enabled = (selectedTitle != "Other" || customTitle.isNotBlank()) && startTime.isBefore(endTime) && selectedPet != null // Enable button if conditions are met
                ) {
                    Text("Save")
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@Composable
fun DateTimePicker(label: String, dateTime: LocalDateTime, onDateTimeChanged: (LocalDateTime) -> Unit) {
    val context = LocalContext.current
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var tempDate by remember { mutableStateOf(dateTime) }

    // Launch dialogs properly without triggering recomposition
    LaunchedEffect(showDatePicker) {
        if (showDatePicker) {
            DatePickerDialog(
                context,
                { _, year, month, dayOfMonth ->
                    tempDate = tempDate.withYear(year).withMonth(month + 1).withDayOfMonth(dayOfMonth)
                    showDatePicker = false
                    showTimePicker = true
                },
                tempDate.year,
                tempDate.monthValue - 1,
                tempDate.dayOfMonth
            ).show()
        }
    }

    LaunchedEffect(showTimePicker) {
        if (showTimePicker) {
            TimePickerDialog(
                context,
                { _, hourOfDay, minute ->
                    val finalDateTime = tempDate.withHour(hourOfDay).withMinute(minute)
                    onDateTimeChanged(finalDateTime)
                    showTimePicker = false
                },
                tempDate.hour,
                tempDate.minute,
                true
            ).show()
        }
    }

    Column {
        TextButton(onClick = { showDatePicker = true }) {
            Text("$label: ${dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))}")
        }
    }
}
