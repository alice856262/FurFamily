package com.example.furfamily.health

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import coil.compose.rememberImagePainter
import com.example.furfamily.ViewModel
import com.example.furfamily.profile.Pet
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthScreen(viewModel: ViewModel, userId: String, navController: NavController) {
    val pets by viewModel.pets.observeAsState(emptyList())
    val healthRecords by viewModel.healthRecords.observeAsState(emptyList())
    var selectedPet by remember { mutableStateOf<Pet?>(null) } // Use nullable Pet
    val lineEntries = remember { mutableStateListOf<Entry>() }
    val dateLabels = remember { mutableStateListOf<String>() }

    // Load pets when the screen is displayed
    LaunchedEffect(userId) {
        viewModel.fetchPets(userId)
    }

    // Automatically select the first pet when pets are loaded
    LaunchedEffect(pets) {
        if (selectedPet == null && pets.isNotEmpty()) {
            selectedPet = pets.first()
        }
    }

    // Fetch health records for the selected pet
    LaunchedEffect(selectedPet) {
        selectedPet?.let { pet ->
            viewModel.fetchHealthRecords(userId, pet.petId)
        }
    }

    // Update line chart data when health records change
    LaunchedEffect(healthRecords) {
        lineEntries.clear()
        dateLabels.clear()

        val sortedRecords = healthRecords
            .filter { it.weight != null && it.weight != 0f }
            .sortedBy { it.entryDate.time }

        sortedRecords.forEachIndexed { index, record ->
            lineEntries.add(Entry(index.toFloat(), record.weight!!))
            dateLabels.add(SimpleDateFormat("yyyy-MM-dd", Locale.US).format(record.entryDate))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Health Dashboard", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = {
                        val selectedPetId = selectedPet?.petId ?: ""
                        navController.navigate("CreateHealthRecord/$selectedPetId")
                    }) {
                        Icon(Icons.Filled.Add, contentDescription = "Add New Record")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Pet Carousel
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(pets) { pet ->
                    Box(
                        modifier = Modifier
                            .size(150.dp)
                            .clip(CircleShape)
                            .background(if (selectedPet?.petId == pet.petId) Color.Gray else Color.LightGray)
                            .clickable { selectedPet = pet },
                        contentAlignment = Alignment.Center
                    ) {
                        if (pet.profileImageUrl.isNotEmpty()) {
                            Image(
                                painter = rememberImagePainter(pet.profileImageUrl),
                                contentDescription = pet.name,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Text(
                                text = pet.name,
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // No health records message
            if (healthRecords.isEmpty()) {
                Text(
                    text = "No health records available for ${selectedPet?.name ?: "this pet"}.",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            } else {
                // Line chart
                if (lineEntries.isNotEmpty()) {
                    Text(
                        text = "Weight Change for ${selectedPet?.name ?: "Pet"}",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    AndroidView(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp),
                        factory = { context ->
                            LineChart(context).apply {
                                description.isEnabled = false
                                xAxis.position = XAxis.XAxisPosition.BOTTOM
                                axisRight.isEnabled = false
                                axisLeft.setDrawGridLines(false)
                                xAxis.setDrawGridLines(false)
                                setTouchEnabled(true)
                                setPinchZoom(true)
                            }
                        },
                        update = { chart ->
                            val lineDataSet = LineDataSet(lineEntries, "Weight").apply {
                                colors = ColorTemplate.COLORFUL_COLORS.toList()
                                valueTextSize = 12f
                                setDrawValues(true)
                                valueFormatter = object : com.github.mikephil.charting.formatter.ValueFormatter() {
                                    override fun getPointLabel(entry: Entry?): String {
                                        return entry?.y?.let { String.format("%.1f", it) } ?: ""
                                    }
                                }
                            }
                            chart.data = LineData(lineDataSet)
                            chart.xAxis.apply {
                                valueFormatter = IndexAxisValueFormatter(dateLabels)
                                setLabelCount(dateLabels.size, true) // Force exact label count
                                granularity = 1f // Ensure each index gets its own label
                                setAvoidFirstLastClipping(true) // Help prevent clipping of first/last labels
                            }
                            chart.invalidate() // Refresh the chart
                        }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Previous records section
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    Text(
                        text = "Previous Health Records for ${selectedPet?.name ?: "Pet"}",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    healthRecords
                        .sortedByDescending { it.entryDate.time }
                        .forEach { record ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Text(
                                        text = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(record.entryDate),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    record.weight?.takeIf { it != 0f }?.let {
                                        Text(text = "Weight: ${"%.1f".format(it)} kg", style = MaterialTheme.typography.bodySmall)
                                    }
                                    record.temperature?.takeIf { it != 0f }?.let {
                                        Text(text = "Body temperature: ${"%.1f".format(it)} °C", style = MaterialTheme.typography.bodySmall)
                                    }
                                    record.rbc?.takeIf { it != 0f }?.let {
                                        Text(text = "RBC: ${"%.1f".format(it)}", style = MaterialTheme.typography.bodySmall)
                                    }
                                    record.wbc?.takeIf { it != 0f }?.let {
                                        Text(text = "WBC: ${"%.1f".format(it)}", style = MaterialTheme.typography.bodySmall)
                                    }
                                    record.plt?.takeIf { it != 0f }?.let {
                                        Text(text = "PLT: ${it.toInt()}", style = MaterialTheme.typography.bodySmall)
                                    }
                                    record.alb?.takeIf { it != 0f }?.let {
                                        Text(text = "Alb (Albumin): ${"%.1f".format(it)}", style = MaterialTheme.typography.bodySmall)
                                    }
                                    record.ast?.takeIf { it != 0f }?.let {
                                        Text(text = "AST: ${it.toInt()}", style = MaterialTheme.typography.bodySmall)
                                    }
                                    record.alt?.takeIf { it != 0f }?.let {
                                        Text(text = "ALT: ${it.toInt()}", style = MaterialTheme.typography.bodySmall)
                                    }
                                    record.bun?.takeIf { it != 0f }?.let {
                                        Text(text = "BUN: ${it.toInt()}", style = MaterialTheme.typography.bodySmall)
                                    }
                                    record.scr?.takeIf { it != 0f }?.let {
                                        Text(text = "Scr (Serum creatinine): ${"%.1f".format(it)}", style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                    }
                }
            }
        }
    }
}