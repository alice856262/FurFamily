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
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.furfamily.profile.Pet
import com.example.furfamily.viewmodel.CalendarViewModel
import com.example.furfamily.viewmodel.MapViewModel
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import android.util.Log
import androidx.compose.runtime.collectAsState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateCalendarEvent(
    calendarViewModel: CalendarViewModel,
    mapViewModel: MapViewModel,
    userId: String,
    pets: List<Pet>,
    eventToEdit: CalendarEvent? = null,
    onEventCreated: () -> Unit,
    onDismiss: () -> Unit
) {
    val defaultTitles = listOf("Vet Appointment", "Grooming", "Playdate", "Training Session", "Medication", "Other")
    var selectedTitle by remember { mutableStateOf(eventToEdit?.title ?: defaultTitles.first()) }
    var customTitle by remember { mutableStateOf("") }
    var isTitleDropdownExpanded by remember { mutableStateOf(false) }
    var description by remember { mutableStateOf(eventToEdit?.description ?: "") }
    var startTime by remember { mutableStateOf(eventToEdit?.startTime ?: LocalDateTime.now()) }
    var endTime by remember { mutableStateOf(eventToEdit?.endTime ?: LocalDateTime.now().plusHours(1)) }
    var isLocationTypeExpanded by remember { mutableStateOf(false) }
    var selectedLocationType by remember { mutableStateOf("Custom Location") }
    val locationTypes = listOf("Vet Clinic", "Grooming Salon", "Pet Hotel", "Dog Park", "Pet Store", "Training Center", "Pet-Friendly Cafe", "Pharmacy", "Other", "Custom Location")
    var location by remember { mutableStateOf(eventToEdit?.location ?: "") }
    var isLocationExpanded by remember { mutableStateOf(false) }
    var selectedPet by remember { mutableStateOf<Pet?>(eventToEdit?.let { pets.find { p -> p.petId == it.petId } }) }
    var isPetExpanded by remember { mutableStateOf(false) }

    // Load place tags when component is created
    LaunchedEffect(Unit) {
        mapViewModel.loadPlaceTags()
    }

    // Collect place tags from MapViewModel
    val placeTags by mapViewModel.placeTags.collectAsState()
    val placeCategories by mapViewModel.placeCategories.collectAsState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (eventToEdit != null) "Edit Calendar Event" else "Create Calendar Event") },
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
                        label = { Text("Title *", color = MaterialTheme.colorScheme.onSurfaceVariant) },
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
                                    if (title != "Other") customTitle = ""
                                }
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
                        label = { Text("Custom Title *", color = MaterialTheme.colorScheme.onSurfaceVariant) },
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

                // Location Type Selection
                ExposedDropdownMenuBox(expanded = isLocationTypeExpanded, onExpandedChange = { isLocationTypeExpanded = it }) {
                    TextField(
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                            .focusProperties { canFocus = false },
                        readOnly = true,
                        value = selectedLocationType,
                        onValueChange = {},
                        label = { Text("Location Type *", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                        colors = TextFieldDefaults.textFieldColors(
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            containerColor = MaterialTheme.colorScheme.surface,
                            focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                            unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isLocationTypeExpanded) }
                    )
                    ExposedDropdownMenu(
                        expanded = isLocationTypeExpanded,
                        onDismissRequest = { isLocationTypeExpanded = false }
                    ) {
                        locationTypes.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type) },
                                onClick = {
                                    selectedLocationType = type
                                    isLocationTypeExpanded = false
                                    if (type == "Custom Location") {
                                        location = ""
                                    } else {
                                        // Find the first place tag matching the selected type
                                        val matchingPlace = placeTags.entries.firstOrNull { (_, name) ->
                                            name.contains(type, ignoreCase = true)
                                        }
                                        if (matchingPlace != null) {
                                            location = matchingPlace.value
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                // Location Selection based on type
                if (selectedLocationType == "Custom Location") {
                    TextField(
                        value = location,
                        onValueChange = { location = it },
                        label = { Text("Custom Location *", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                        colors = TextFieldDefaults.textFieldColors(
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            containerColor = MaterialTheme.colorScheme.surface,
                            focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                            unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                } else {
                    // Get the category from the place tags
                    val filteredPlaces = placeTags.entries.filter { (latLng, name) ->
                        val placeCategory = placeCategories[latLng] ?: "Other"
                        Log.d("CreateCalendarEvent", "Checking place: $name with category: $placeCategory")
                        placeCategory == selectedLocationType
                    }.map { it.value }
                    
                    if (filteredPlaces.isNotEmpty()) {
                        ExposedDropdownMenuBox(
                            expanded = isLocationExpanded,
                            onExpandedChange = { isLocationExpanded = it }
                        ) {
                            TextField(
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth()
                                    .focusProperties { canFocus = false },
                                readOnly = true,
                                value = location,
                                onValueChange = {},
                                label = { Text("Select Location *", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                colors = TextFieldDefaults.textFieldColors(
                                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                                    unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isLocationExpanded) }
                            )
                            ExposedDropdownMenu(
                                expanded = isLocationExpanded,
                                onDismissRequest = { isLocationExpanded = false }
                            ) {
                                filteredPlaces.forEach { placeName ->
                                    DropdownMenuItem(
                                        text = { Text(placeName) },
                                        onClick = {
                                            location = placeName
                                            isLocationExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    } else {
                        Text(
                            "No saved locations for ${selectedLocationType}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                // Pet Dropdown for selecting petId
                if (pets.isNotEmpty()) {
                    ExposedDropdownMenuBox(expanded = isPetExpanded, onExpandedChange = { isPetExpanded = it }) {
                        TextField(
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                                .focusProperties { canFocus = false }
                                .padding(bottom = 8.dp),
                            readOnly = true,
                            value = selectedPet?.name ?: "Please choose one",
                            onValueChange = {},
                            label = { Text("Pet Name *", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                            colors = TextFieldDefaults.textFieldColors(
                                focusedLabelColor = MaterialTheme.colorScheme.primary,
                                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                containerColor = MaterialTheme.colorScheme.surface,
                                focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                                unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isPetExpanded) }
                        )
                        ExposedDropdownMenu(
                            expanded = isPetExpanded,
                            onDismissRequest = { isPetExpanded = false }
                        ) {
                            pets.forEach { pet ->
                                DropdownMenuItem(
                                    text = { Text(pet.name) },
                                    onClick = {
                                        selectedPet = pet
                                        isPetExpanded = false
                                    }
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
                            eventId = eventToEdit?.eventId ?: "",
                            userId = userId,
                            petId = selectedPet?.petId ?: "",
                            title = eventTitle,
                            description = description,
                            startTime = startTime,
                            endTime = endTime,
                            location = location
                        )
                        
                        if (eventToEdit != null) {
                            Log.d("CreateCalendarEvent", "Updating event with ID: ${event.eventId}")
                            Log.d("CreateCalendarEvent", "Event details: $event")
                            calendarViewModel.updateEvent(event)
                            onEventCreated()
                        } else {
                            Log.d("CreateCalendarEvent", "Creating new event")
                            calendarViewModel.createEvent(
                                calendarEvent = event,
                                function = {
                                    calendarViewModel.loadEventsForDate(event.startTime.toLocalDate())
                                    onEventCreated()
                                }
                            )
                        }
                    },
                    enabled = (selectedTitle != "Other" || customTitle.isNotBlank()) && 
                             startTime.isBefore(endTime) && 
                             selectedPet != null &&
                             location.isNotBlank()
                ) {
                    Text(if (eventToEdit != null) "Update" else "Save")
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
