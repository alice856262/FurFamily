package com.example.furfamily.calendar

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.furfamily.nutrition.Feeding
import com.example.furfamily.nutrition.Food
import com.example.furfamily.profile.Pet
import com.example.furfamily.viewmodel.NutritionViewModel
import com.google.api.services.calendar.model.Event
import java.text.SimpleDateFormat
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

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
fun FeedingEventsList(feedingEvents: List<Feeding>, pets: List<Pet>, nutritionViewModel: NutritionViewModel) {
    LazyColumn {
        items(feedingEvents) { feeding ->
            val pet = pets.find { it.petId == feeding.petId }
            var food by remember { mutableStateOf<Food?>(null) }

            // Load food details asynchronously
            LaunchedEffect(feeding.foodId) {
                nutritionViewModel.getFoodById(feeding.foodId) { fetchedFood ->
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
//            Text(text = "Time: ${feeding.mealTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))}", style = MaterialTheme.typography.bodySmall)

            if (feeding.notes.isNotEmpty()) {
                Text(text = "Notes: ${feeding.notes}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
        }
    }
}