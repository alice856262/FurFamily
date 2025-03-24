package com.example.furfamily.calendar

import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.furfamily.ViewModel
import com.example.furfamily.getCurrentUserId
import com.example.furfamily.nutrition.Feeding
import com.example.furfamily.nutrition.Food
import com.example.furfamily.profile.Pet
import com.google.api.services.calendar.model.Event
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.O)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun CalendarScreen(viewModel: ViewModel) {
    val userId = getCurrentUserId()
    val context = LocalContext.current
    val intentForUserResolution by viewModel.intentForUserResolution.observeAsState()
    val allEvents by viewModel.calendarEvents.observeAsState(emptyList())
    val calendarEvents by viewModel.calendarEventDates.observeAsState(emptyList())
    val allEventsDates by viewModel.eventsDates.observeAsState(emptyList())
    val feedingEvents by viewModel.feedingEvents.observeAsState(emptyList())
    val pets by viewModel.pets.observeAsState(emptyList())
    var currentDate by remember { mutableStateOf(LocalDate.now()) }
    val today = LocalDate.now()
    var selectedDate by remember { mutableStateOf(today) }
    var eventsDates by remember { mutableStateOf(emptyList<LocalDate>()) }
    var showCreateEventDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedPetName by remember { mutableStateOf("All Pets") }
    val petNames = listOf("All Pets") + pets.map { it.name }
    var isPetNameExpanded by remember { mutableStateOf(false) }

    // Load data when userId is available
    LaunchedEffect(userId) {
        userId?.let {
            viewModel.loadPetsProfile(it)
            viewModel.loadCalendarEvents()
            viewModel.loadEventsForDate(selectedDate)
            viewModel.loadFeedingEventsForDate(selectedDate)
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
        content = { paddingValues ->
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .padding(horizontal = 6.dp)
                    .fillMaxSize()
            ) {
                // Month View
                MonthView(
                    currentDate = currentDate,
                    selectedDate = selectedDate,
                    eventsDates = eventsDates,
                    onDaySelected = { date ->
                        selectedDate = date
                        viewModel.loadEventsForDate(date)
                        viewModel.loadFeedingEventsForDate(date)
                    },
                    onMonthChanged = { newDate ->
                        currentDate = newDate
                        selectedDate = if (YearMonth.from(newDate) == YearMonth.from(today)) today else null

                        // Clear events when no date is selected
                        if (YearMonth.from(newDate) != YearMonth.from(today)) {
                            viewModel.clearSelectedDateEvents()
                        }
                    }
                )

                Spacer(modifier = Modifier.height(6.dp))

                Column(
                    modifier = Modifier
                        .padding(start = 12.dp, end = 12.dp)
                ) {
                    // Search bar for events
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("Search Events") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    )

                    // Events List (always shown below search bar)
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
                            EventsList(filteredEvents)
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
                            label = { Text("Pet Name") },
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

                    // Feeding List (below pet name dropdown)
                    if (selectedDate == null) {
                        Text(
                            text = "Please select a date to view feeding records.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = "Feeding Records for $selectedDate",
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
                            FeedingEventsList(filteredFeedingEvents, pets, viewModel)
                        }
                    }
                }
            }
        }
    )

    if (showCreateEventDialog) {
        CreateCalendarEvent(
            viewModel = viewModel,
            userId = userId ?: "",
            pets = pets,
            onEventCreated = {
                showCreateEventDialog = false
            },
            onDismiss = {
                showCreateEventDialog = false
            }
        )
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MonthView(
    currentDate: LocalDate,
    selectedDate: LocalDate?,
    eventsDates: List<LocalDate>, // List of dates with events for the current month
    onDaySelected: (LocalDate) -> Unit,
    onMonthChanged: (LocalDate) -> Unit
) {
    val yearMonth = remember(currentDate) { YearMonth.from(currentDate) }
    val daysInMonth = remember(yearMonth) { yearMonth.lengthOfMonth() }
    val firstDayOfWeek = remember(yearMonth) { yearMonth.atDay(1).dayOfWeek.value }
    val daysOfWeek = remember { listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun") }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = { onMonthChanged(currentDate.minusMonths(1)) }) {
                Icon(Icons.Default.ArrowBack, "Previous Month")
            }
            Text(text = yearMonth.toString(), style = MaterialTheme.typography.titleLarge)
            IconButton(onClick = { onMonthChanged(currentDate.plusMonths(1)) }) {
                Icon(Icons.Default.ArrowForward, "Next Month")
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            for (day in daysOfWeek) {
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            contentPadding = PaddingValues(2.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(daysInMonth + firstDayOfWeek - 1) { index ->
                if (index >= firstDayOfWeek - 1) {
                    val day = index - firstDayOfWeek + 2
                    val date = LocalDate.of(currentDate.year, currentDate.monthValue, day)
                    DayCell(
                        day = day,
                        date = date,
                        isSelected = selectedDate == date,
                        hasEvent = eventsDates.contains(date), // Highlight days with events
                        onClick = { onDaySelected(date) }
                    )
                } else {
                    Spacer(modifier = Modifier.background(Color.Transparent))
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun DayCell(day: Int, date: LocalDate, isSelected: Boolean, hasEvent: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .background(
                color = when {
                    isSelected -> Color(0xFFFF6F61) // Selected day color
                    hasEvent -> Color(0xFF81B29A) // Event day color
                    else -> Color.Transparent
                }
            )
            .border(
                BorderStroke(
                    2.dp,
                    if (isSelected) Color(0xFF932020) else Color.Transparent
                ),
                RoundedCornerShape(50)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = day.toString(),
            color = if (isSelected || hasEvent) Color.White else Color.Black
        )
    }
}

@Composable
fun EventsList(events: List<Event>) {
    LazyColumn {
        items(events) { event ->
            EventItem(event)
        }
    }
}

@Composable
fun EventItem(event: Event) {
    val startMillis = event.start?.dateTime?.value ?: event.start?.date?.value
    val endMillis = event.end?.dateTime?.value ?: event.end?.date?.value

    val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    val formattedStart = startMillis?.let {
        formatter.format(Date(it))
    } ?: "No Start Time"

    val formattedEnd = endMillis?.let {
        formatter.format(Date(it))
    } ?: "No End Time"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { /* Open event details */ },
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(text = event.summary ?: "No Title", style = MaterialTheme.typography.titleMedium)
            Text(text = "Start: $formattedStart", style = MaterialTheme.typography.bodySmall)
            Text(text = "End: $formattedEnd", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun FeedingEventsList(feedingEvents: List<Feeding>, pets: List<Pet>, viewModel: ViewModel) {
    LazyColumn {
        items(feedingEvents) { feeding ->
            val pet = pets.find { it.petId == feeding.petId }
            var food by remember { mutableStateOf<Food?>(null) }

            // Load food details asynchronously
            LaunchedEffect(feeding.foodId) {
                viewModel.getFoodById(feeding.foodId) { fetchedFood ->
                    food = fetchedFood
                }
            }

            FeedingEventItem(feeding, pet, food)
        }
    }
}

@Composable
fun FeedingEventItem(feeding: Feeding, pet: Pet?, food: Food?) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { /* Open feeding details */ },
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(text = pet?.name ?: "Unknown Pet", style = MaterialTheme.typography.titleMedium)
            Text(text = "Food: ${food?.name ?: "Unknown Food"}", style = MaterialTheme.typography.bodySmall)
            Text(text = "Meal Type: ${feeding.mealType}", style = MaterialTheme.typography.bodySmall)
            Text(text = "Amount: ${feeding.amount} g", style = MaterialTheme.typography.bodySmall)
            Text(text = "Time: ${feeding.mealTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))}", style = MaterialTheme.typography.bodySmall)

            if (feeding.notes.isNotEmpty()) {
                Text(text = "Notes: ${feeding.notes}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
        }
    }
}
