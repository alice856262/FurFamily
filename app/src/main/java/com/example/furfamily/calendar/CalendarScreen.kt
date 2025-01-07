package com.example.furfamily.calendar

import android.annotation.SuppressLint
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.furfamily.ViewModel
import com.example.furfamily.getCurrentUserId
import com.google.api.services.calendar.model.Event
import java.time.LocalDate
import java.time.YearMonth

@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.O)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun CalendarScreen(viewModel: ViewModel = viewModel()) {
    val userId = getCurrentUserId()
    val context = LocalContext.current
    val intentForUserResolution by viewModel.intentForUserResolution.observeAsState()
    val calendarEvents by viewModel.calendarEventDates.observeAsState(emptyList())
    val allEventsDates by viewModel.eventsDates.observeAsState(emptyList()) // Observe all events
    val feedingEvents by viewModel.feedingEvents.observeAsState(emptyList())
    var currentDate by remember { mutableStateOf(LocalDate.now()) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) } // Default to current date
    var eventsDates by remember { mutableStateOf(emptyList<LocalDate>()) } // Events for the displayed month
    var showCreateEvent by remember { mutableStateOf(false) }

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Event Calendar", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { showCreateEvent = !showCreateEvent }) {
                        Icon(Icons.Filled.Add, contentDescription = "Create Event")
                    }
                }
            )
        },
        content = { paddingValues ->
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp)
                    .fillMaxSize()
            ) {
                // Month View
                MonthView(
                    currentDate = currentDate,
                    selectedDate = selectedDate,
                    eventsDates = eventsDates, // Pass filtered events
                    onDaySelected = { date ->
                        selectedDate = date // Update selected date
                        viewModel.loadEventsForDate(date)
                        viewModel.loadFeedingEventsForDate(date)
                    },
                    onMonthChanged = { newDate ->
                        currentDate = newDate // Update month
                        selectedDate = null // Reset selected date
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Display events for the selected date
                if (selectedDate != null) {
                    Text(
                        text = "Events for $selectedDate",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    EventsList(calendarEvents)

                    Text(
                        text = "Feeding Records for $selectedDate",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    EventsList(feedingEvents)
                } else {
                    Text(
                        text = "Select a date to view events.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    )
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
    Card(modifier = Modifier
        .fillMaxWidth()
        .padding(8.dp)
        .clickable { /* Open event details or perform an action */ }) {
        Column(
            modifier = Modifier.padding(8.dp)
        ) {
            Text(text = event.summary ?: "No Title", style = MaterialTheme.typography.titleMedium)
            Text(text = "Start: ${event.start.dateTime ?: event.start.date}", style = MaterialTheme.typography.bodySmall)
            Text(text = "End: ${event.end.dateTime ?: event.end.date}", style = MaterialTheme.typography.bodySmall)
        }
    }
}
