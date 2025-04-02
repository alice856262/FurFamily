package com.example.furfamily.health

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.rememberImagePainter
import com.example.furfamily.Routes
import com.example.furfamily.profile.Pet
import com.example.furfamily.viewmodel.HealthViewModel
import com.example.furfamily.viewmodel.ProfileViewModel
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import java.text.SimpleDateFormat
import java.util.Locale
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthScreen(userId: String, navController: NavController) {
    val healthViewModel: HealthViewModel = hiltViewModel()
    val profileViewModel: ProfileViewModel = hiltViewModel()
    val coroutineScope = rememberCoroutineScope()

    val pets by profileViewModel.pets.observeAsState(emptyList())
    val healthRecords by healthViewModel.healthRecords.observeAsState(emptyList())
    var selectedPet by remember { mutableStateOf<Pet?>(null) }
    val lineEntries = remember { mutableStateListOf<Entry>() }
    val dateLabels = remember { mutableStateListOf<String>() }
    val snackbarHostState = remember { SnackbarHostState() }
    var snackbarMessage by remember { mutableStateOf("") }
    val savedStateHandle = navController.currentBackStackEntry?.savedStateHandle
    var recordToEdit by remember { mutableStateOf<HealthRecord?>(null) }
    var recordToDelete by remember { mutableStateOf<HealthRecord?>(null) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    // Load pets when the screen is displayed
    LaunchedEffect(userId) {
        profileViewModel.fetchPets(userId)
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
            Log.d("HealthScreen", "Fetching health records for pet: ${pet.name} (${pet.petId})")
            healthViewModel.fetchHealthRecords(userId, pet.petId)
        }
    }

    // Update line chart data when health records change
    LaunchedEffect(healthRecords, selectedPet) {
        Log.d("HealthScreen", "Updating line chart data for pet: ${selectedPet?.name}")
        lineEntries.clear()
        dateLabels.clear()

        val sortedRecords = healthRecords
            .filter { record -> 
                val isValidRecord = record.petId == selectedPet?.petId && 
                    record.weight != null && 
                    record.weight != 0f
                if (record.petId == selectedPet?.petId) {
                    Log.d("HealthScreen", "Record weight for ${selectedPet?.name}: ${record.weight}")
                }
                isValidRecord
            }
            .sortedBy { it.entryDate.time }

        Log.d("HealthScreen", "Found ${sortedRecords.size} valid weight records for ${selectedPet?.name}")
        
        sortedRecords.forEachIndexed { index, record ->
            record.weight?.let { weight ->
                Log.d("HealthScreen", "Adding weight entry: $weight at index $index")
                lineEntries.add(Entry(index.toFloat(), weight))
                dateLabels.add(SimpleDateFormat("yyyy-MM-dd", Locale.US).format(record.entryDate))
            }
        }
        
        Log.d("HealthScreen", "Final line entries size: ${lineEntries.size}")
    }

    // Show Snackbar
    LaunchedEffect(savedStateHandle) {
        savedStateHandle?.get<String>("healthRecordSaved")?.let { petName ->
            if (petName.isNotEmpty()) {
                snackbarMessage = "Health record for $petName added successfully!"
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(snackbarMessage)
                }
                savedStateHandle.remove<String>("healthRecordSaved")
            }
        }
    }

    // Handle delete record
    fun handleDeleteRecord(recordId: String) {
        healthViewModel.deleteHealthRecord(userId, recordId)
        showDeleteConfirmation = false
        recordToDelete = null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Health Dashboard", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = {
                        val selectedPetId = selectedPet?.petId ?: ""
                        navController.navigate("${Routes.NewHealthRecord.value}/$selectedPetId")
                    }) {
                        Icon(Icons.Filled.Add, contentDescription = "Add New Record")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            // Pet Carousel
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
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

            // Content area
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                // Line chart
                Text(
                    text = "Weight Change for ${selectedPet?.name ?: "Pet"}",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 12.dp)
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
                            setNoDataText("No weight records available")
                        }
                    },
                    update = { chart ->
                        if (lineEntries.isNotEmpty()) {
                            Log.d("HealthScreen", "Updating chart with ${lineEntries.size} entries")
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
                                setLabelCount(dateLabels.size, true)
                                granularity = 1f
                                setAvoidFirstLastClipping(true)
                            }
                            chart.invalidate()
                        } else {
                            Log.d("HealthScreen", "No entries available, clearing chart")
                            chart.data = null
                            chart.invalidate()
                        }
                        chart.notifyDataSetChanged()
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Previous records section
                Text(
                    text = "Previous Health Records for ${selectedPet?.name ?: "Pet"}",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                val filteredRecords = healthRecords.filter { record -> 
                    record.petId == selectedPet?.petId 
                }.sortedByDescending { it.entryDate.time }

                if (filteredRecords.isEmpty()) {
                    Text(
                        text = "No health records available for ${selectedPet?.name ?: "this pet"}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredRecords) { record ->
                            Log.d("HealthScreen", "Displaying record: ${record.recordId} for pet: ${record.petId}")
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                shape = RoundedCornerShape(8.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column(
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(record.entryDate),
                                            style = MaterialTheme.typography.titleMedium
                                        )
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
                                        record.notes?.takeIf { it.isNotBlank() }?.let {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(text = "Notes: $it", style = MaterialTheme.typography.bodySmall)
                                        }
                                    }

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        IconButton(
                                            onClick = {
                                                recordToEdit = record
                                                navController.navigate("${Routes.NewHealthRecord.value}/${record.petId}?recordId=${record.recordId}")
                                            }
                                        ) {
                                            Icon(Icons.Default.Edit, contentDescription = "Edit record")
                                        }
                                        IconButton(
                                            onClick = {
                                                recordToDelete = record
                                                showDeleteConfirmation = true
                                            }
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete record")
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

    // Delete confirmation dialog
    if (showDeleteConfirmation && recordToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete Health Record") },
            text = { Text("Are you sure you want to delete this health record? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = { handleDeleteRecord(recordToDelete!!.recordId) }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmation = false
                        recordToDelete = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}