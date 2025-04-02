package com.example.furfamily.calendar

import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.furfamily.getCurrentUserId
import com.example.furfamily.viewmodel.CalendarViewModel
import com.example.furfamily.viewmodel.NutritionViewModel
import com.example.furfamily.viewmodel.ProfileViewModel
import java.time.LocalDate
import java.time.YearMonth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.AlertDialog
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import com.example.furfamily.viewmodel.MapViewModel
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.ui.unit.times

@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.O)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun CalendarScreen() {
    val calendarViewModel: CalendarViewModel = hiltViewModel()
    val nutritionViewModel: NutritionViewModel = hiltViewModel()
    val profileViewModel: ProfileViewModel = hiltViewModel()
    val mapViewModel: MapViewModel = hiltViewModel()

    val userId = getCurrentUserId()
    val context = LocalContext.current
    val intentForUserResolution by calendarViewModel.intentForUserResolution.observeAsState()
    val allEvents by calendarViewModel.calendarEvents.observeAsState(emptyList())
    val calendarEvents by calendarViewModel.selectedDateEvents.observeAsState(emptyList())
    val allEventsDates by calendarViewModel.eventsDates.observeAsState(emptyList())
    val feedingEvents by nutritionViewModel.feedingEvents.observeAsState(emptyList())
    val pets by profileViewModel.pets.observeAsState(emptyList())
    val foodList by nutritionViewModel.foodList.observeAsState(emptyList())
    var currentDate by remember { mutableStateOf(LocalDate.now()) }
    val today = LocalDate.now()
    var selectedDate by remember { mutableStateOf(today) }
    var eventsDates by remember { mutableStateOf(emptyList<LocalDate>()) }
    var showCreateEventDialog by remember { mutableStateOf(false) }
    var eventToEdit by remember { mutableStateOf<CalendarEvent?>(null) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var eventToDelete by remember { mutableStateOf<CalendarEvent?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedPetName by remember { mutableStateOf("All Pets") }
    val petNames = listOf("All Pets") + pets.map { it.name }
    var isPetNameExpanded by remember { mutableStateOf(false) }
    var successMessage by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }

    // Load data when userId is available
    LaunchedEffect(userId) {
        userId?.let {
            profileViewModel.loadPetsProfile(it)
            calendarViewModel.loadCalendarEvents()
            calendarViewModel.loadEventsForDate(selectedDate)
            nutritionViewModel.loadFeedingEventsForDate(selectedDate)
        }
    }

    // Filter events for the current month
    LaunchedEffect(currentDate, allEventsDates) {
        val yearMonth = YearMonth.from(currentDate)
        eventsDates = allEventsDates.filter { eventDate ->
            YearMonth.from(eventDate) == yearMonth
        }
    }

    // Handle intent for user resolution
    LaunchedEffect(intentForUserResolution) {
        intentForUserResolution?.let {
            context.startActivity(it)
        }
    }

    LaunchedEffect(successMessage) {
        if (successMessage.isNotEmpty()) {
            snackbarHostState.showSnackbar(successMessage)
            successMessage = ""
        }
    }

    val filteredEvents = remember(searchQuery, calendarEvents, allEvents) {
        Log.d("CalendarScreen", "Filtering events - Search query: $searchQuery")
        Log.d("CalendarScreen", "Calendar events count: ${calendarEvents.size}")
        Log.d("CalendarScreen", "All events count: ${allEvents.size}")
        if (searchQuery.isBlank()) {
            calendarEvents.map { event ->
                CalendarEvent(
                    eventId = event.id ?: "",
                    userId = userId ?: "",
                    petId = event.extendedProperties?.private?.get("petId") ?: "",
                    title = event.summary ?: "",
                    description = event.description ?: "",
                    startTime = event.start?.dateTime?.value?.let {
                        Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDateTime()
                    } ?: LocalDateTime.now(),
                    endTime = event.end?.dateTime?.value?.let {
                        Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDateTime()
                    } ?: LocalDateTime.now().plusHours(1),
                    location = event.location ?: ""
                )
            }
        } else {
            allEvents.map { event ->
                CalendarEvent(
                    eventId = event.id ?: "",
                    userId = userId ?: "",
                    petId = event.extendedProperties?.private?.get("petId") ?: "",
                    title = event.summary ?: "",
                    description = event.description ?: "",
                    startTime = event.start?.dateTime?.value?.let {
                        Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDateTime()
                    } ?: LocalDateTime.now(),
                    endTime = event.end?.dateTime?.value?.let {
                        Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDateTime()
                    } ?: LocalDateTime.now().plusHours(1),
                    location = event.location ?: ""
                )
            }.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                it.description!!.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    val filteredFeedingEvents = feedingEvents.filter {
        selectedPetName == "All Pets" || pets.find { p -> p.petId == it.petId }?.name == selectedPetName
    }

    // Calculate dynamic height for the feeding records grid
    val gridHeight = remember(filteredFeedingEvents.size) {
        val rows = (filteredFeedingEvents.size + 1) / 2 // Round up division for number of rows
        val rowHeight = 120.dp // Approximate height of each card including padding
        val spacing = 8.dp // Vertical spacing between rows
        val totalHeight = (rows * rowHeight) + ((rows - 1) * spacing)
        totalHeight.coerceAtMost(400.dp) // Cap the maximum height at 400.dp
    }

    // Handle event deletion
    fun handleDeleteEvent(event: CalendarEvent) {
        calendarViewModel.deleteEvent(event)
        calendarViewModel.loadEventsForDate(selectedDate)
        successMessage = "Event deleted successfully!"
    }

    // Handle event editing
    fun handleEditEvent(event: CalendarEvent) {
        eventToEdit = event
        showCreateEventDialog = true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Event Calendar", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { 
                        eventToEdit = null
                        showCreateEventDialog = true 
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "Create Event")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Fixed Calendar Section
            Box(
                modifier = Modifier.height(340.dp)
            ) {
                MonthView(
                    currentDate = currentDate,
                    selectedDate = selectedDate,
                    eventsDates = eventsDates,
                    onDaySelected = { date ->
                        selectedDate = date
                        calendarViewModel.loadEventsForDate(date)
                        nutritionViewModel.loadFeedingEventsForDate(date)
                    },
                    onMonthChanged = { newDate ->
                        currentDate = newDate
                        selectedDate = if (YearMonth.from(newDate) == YearMonth.from(today)) today else null

                        // Clear events when no date is selected
                        if (YearMonth.from(newDate) != YearMonth.from(today)) {
                            calendarViewModel.clearSelectedDateEvents()
                        }
                    }
                )
            }

            // Scrollable Content Section
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        // Search bar for events
                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            label = { Text("Search events by keywords", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                            colors = TextFieldDefaults.textFieldColors(
                                focusedLabelColor = MaterialTheme.colorScheme.primary,
                                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                containerColor = MaterialTheme.colorScheme.surface,
                                focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                                unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        )

                        // Events List
                        if (selectedDate == null && searchQuery.isBlank()) {
                            Text(
                                text = "Please select a date to view events.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Text(
                                text = if (searchQuery.isNotBlank())
                                    "Events matching \"$searchQuery\""
                                else
                                    "Events for $selectedDate",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )

                            if (filteredEvents.isEmpty()) {
                                Text(
                                    text = if (searchQuery.isNotBlank())
                                        "No events found for \"$searchQuery\"."
                                    else
                                        "No events for this date.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                filteredEvents.forEach { event ->
                                    EventItem(
                                        event = event,
                                        onEdit = { handleEditEvent(event) },
                                        onDelete = { 
                                            eventToDelete = event
                                            showDeleteConfirmation = true
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Pet Name Dropdown for Feeding Events
                        ExposedDropdownMenuBox(
                            expanded = isPetNameExpanded,
                            onExpandedChange = { isPetNameExpanded = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        ) {
                            TextField(
                                readOnly = true,
                                value = selectedPetName,
                                onValueChange = {},
                                label = { Text("Filter feeding records by pet name", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                colors = TextFieldDefaults.textFieldColors(
                                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                                    unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isPetNameExpanded) },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth()
                                    .focusProperties { canFocus = false },
                            )

                            ExposedDropdownMenu(
                                expanded = isPetNameExpanded,
                                onDismissRequest = { isPetNameExpanded = false }
                            ) {
                                petNames.forEach { name ->
                                    DropdownMenuItem(
                                        text = { Text(name) },
                                        onClick = {
                                            selectedPetName = name
                                            isPetNameExpanded = false
                                        },
                                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                                    )
                                }
                            }
                        }

                        // Feeding List
                        if (selectedDate == null) {
                            Text(
                                text = "Please select a date to view feeding records.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Text(
                                text = "Feeding records for $selectedDate",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )

                            if (feedingEvents.isEmpty()) {
                                Text(
                                    text = "No feeding records for this date.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(gridHeight)
                                ) {
                                    LazyVerticalGrid(
                                        columns = GridCells.Fixed(2),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        items(filteredFeedingEvents) { feeding ->
                                            val pet = pets.find { it.petId == feeding.petId }
                                            val food = foodList.find { it.foodId == feeding.foodId }
                                            FeedingEventItem(feeding, pet, food)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateEventDialog) {
        CreateCalendarEvent(
            calendarViewModel = calendarViewModel,
            mapViewModel = mapViewModel,
            userId = userId ?: "",
            pets = pets,
            eventToEdit = eventToEdit,
            onEventCreated = {
                val isUpdate = eventToEdit != null
                showCreateEventDialog = false
                eventToEdit = null
                successMessage = if (isUpdate) "Event updated successfully!" else "Event created successfully!"
            },
            onDismiss = {
                showCreateEventDialog = false
                eventToEdit = null
            }
        )
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { 
                showDeleteConfirmation = false
                eventToDelete = null
            },
            title = { Text("Delete Event: ${eventToDelete!!.title}") },
            text = { Text("Are you sure you want to delete this event?") },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OutlinedButton(
                        onClick = {
                            showDeleteConfirmation = false
                            eventToDelete = null
                        }
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            eventToDelete?.let { handleDeleteEvent(it) }
                            showDeleteConfirmation = false
                            eventToDelete = null
                        }
                    ) {
                        Text("Delete")
                    }
                }
            }
        )
    }
}
