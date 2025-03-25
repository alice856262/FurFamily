package com.example.furfamily.calendar

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.YearMonth

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