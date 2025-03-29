package com.example.furfamily.calendar

import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.Divider
import androidx.compose.material3.TextFieldDefaults

@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.O)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun CalendarScreen() {
    val calendarViewModel: CalendarViewModel = hiltViewModel()
    val nutritionViewModel: NutritionViewModel = hiltViewModel()
    val profileViewModel: ProfileViewModel = hiltViewModel()

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
        if (searchQuery.isBlank()) {
            calendarEvents
        } else {
            allEvents.filter {
                it.summary?.contains(searchQuery, ignoreCase = true) == true ||
                        it.description?.contains(searchQuery, ignoreCase = true) == true
            }
        }
    }

    val filteredFeedingEvents = feedingEvents.filter {
        selectedPetName == "All Pets" || pets.find { p -> p.petId == it.petId }?.name == selectedPetName
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Event Calendar", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { showCreateEventDialog = true }) {
                        Icon(Icons.Filled.Add, contentDescription = "Create Event")
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
                                    EventItem(event)
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
                                filteredFeedingEvents.forEach { feeding ->
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

    if (showCreateEventDialog) {
        CreateCalendarEvent(
            calendarViewModel = calendarViewModel,
            userId = userId ?: "",
            pets = pets,
            onEventCreated = {
                showCreateEventDialog = false
                successMessage = "Event created successfully!"
            },
            onDismiss = {
                showCreateEventDialog = false
            }
        )
    }
}
