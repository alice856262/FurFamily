package com.example.furfamily.calendar

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.example.furfamily.ViewModel
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
fun CreateCalendarEvent(viewModel: ViewModel, userId: String, onEventCreated: () -> Unit) {
    // Local state for event details
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var startTime by remember { mutableStateOf(LocalDateTime.now()) }
    var endTime by remember { mutableStateOf(LocalDateTime.now().plusHours(1)) }
    var location by remember { mutableStateOf("") }

    LazyColumn {
        item {
            Column {
                TextField(value = title, onValueChange = { title = it }, label = { Text("Title") })
                TextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") })
                DateTimePicker("Start Time", startTime) { startTime = it }
                DateTimePicker("End Time", endTime) { endTime = it }
                TextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Location") })
                Button(onClick = {
                    val event = CalendarEvent(
                        userId = userId,
                        title = title,
                        description = description,
                        startTime = startTime,
                        endTime = endTime,
                        location = location
                    )
                    viewModel.createEvent(event) {
                        onEventCreated()  // Notify the screen to hide the create event section
                    }
                }) {
                    Text("Save Event")
                }
            }
        }
    }
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
