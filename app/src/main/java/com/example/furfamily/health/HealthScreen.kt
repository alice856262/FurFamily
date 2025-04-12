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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.rememberImagePainter
import com.example.furfamily.R
import com.example.furfamily.Routes
import com.example.furfamily.profile.Pet
import com.example.furfamily.viewmodel.HealthViewModel
import com.example.furfamily.viewmodel.NutritionViewModel
import com.example.furfamily.viewmodel.ProfileViewModel
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import java.text.SimpleDateFormat
import java.util.Locale
import kotlinx.coroutines.launch
import java.time.LocalDate

enum class ChartType {
    WEIGHT,
    CALORIE_INTAKE,
    WATER_INTAKE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthScreen(userId: String, navController: NavController) {
    val healthViewModel: HealthViewModel = hiltViewModel()
    val profileViewModel: ProfileViewModel = hiltViewModel()
    val nutritionViewModel: NutritionViewModel = hiltViewModel()
    val coroutineScope = rememberCoroutineScope()

    val pets by profileViewModel.pets.observeAsState(emptyList())
    val healthRecords by healthViewModel.healthRecords.observeAsState(emptyList())
    val allFeedingEvents by nutritionViewModel.allFeedingEvents.observeAsState(emptyList())
    var selectedPet by remember { mutableStateOf<Pet?>(null) }
    val lineEntries = remember { mutableStateListOf<Entry>() }
    val barEntries = remember { mutableStateListOf<BarEntry>() }
    val dateLabels = remember { mutableStateListOf<String>() }
    val snackbarHostState = remember { SnackbarHostState() }
    var snackbarMessage by remember { mutableStateOf("") }
    val savedStateHandle = navController.currentBackStackEntry?.savedStateHandle
    var recordToEdit by remember { mutableStateOf<HealthRecord?>(null) }
    var recordToDelete by remember { mutableStateOf<HealthRecord?>(null) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    
    // Chart type selection
    var selectedChartType by remember { mutableStateOf(ChartType.WEIGHT) }
    val chartTitles = listOf("Weight", "Calorie Intake", "Water Intake")

    // Load pets when the screen is displayed
    LaunchedEffect(userId) {
        profileViewModel.fetchPets(userId)
        nutritionViewModel.loadAllFeedingEvents()
        nutritionViewModel.loadFoodList()
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

    // Update chart data when chart type, health records, or feeding records change
    LaunchedEffect(selectedChartType, healthRecords, allFeedingEvents, selectedPet) {
        Log.d("HealthScreen", "Updating chart data for type: $selectedChartType")
        lineEntries.clear()
        barEntries.clear()
        dateLabels.clear()

        when (selectedChartType) {
            ChartType.WEIGHT -> {
                // Weight chart logic
        val sortedRecords = healthRecords
            .filter { record -> 
                val isValidRecord = record.petId == selectedPet?.petId && 
                    record.weight != null && 
                    record.weight != 0f
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
            }
            ChartType.CALORIE_INTAKE -> {
                // Calorie intake chart logic
                if (selectedPet == null) return@LaunchedEffect
                
                // Get feeding records for selected pet
                val petFeedingEvents = allFeedingEvents.filter { it.petId == selectedPet!!.petId }
                Log.d("HealthScreen", "Found ${petFeedingEvents.size} feeding events for ${selectedPet?.name}")
                
                // Check if food list is loaded
                val foodListSize = nutritionViewModel.foodList.value?.size ?: 0
                Log.d("HealthScreen", "Food list size: $foodListSize")
                
                // Group by date and calculate daily calorie intake
                val dailyCalorieIntake = mutableMapOf<LocalDate, Float>()
                
                petFeedingEvents.forEach { feeding ->
                    val date = feeding.mealTime.toLocalDate()
                    val food = nutritionViewModel.foodList.value?.find { it.foodId == feeding.foodId }
                    
                    if (food != null) {
                        val calories = (food.caloriesPerKg * feeding.amount / 1000) // Convert g to kg
                        Log.d("HealthScreen", "Adding calories: $calories for date: $date (food: ${food.name}, amount: ${feeding.amount}g)")
                        dailyCalorieIntake[date] = (dailyCalorieIntake[date] ?: 0f) + calories
                    } else {
                        Log.d("HealthScreen", "Food not found for ID: ${feeding.foodId}")
                    }
                }
                
                // Sort by date and create entries
                val sortedDates = dailyCalorieIntake.keys.sorted()
                Log.d("HealthScreen", "Created ${sortedDates.size} date entries for calorie chart")
                
                sortedDates.forEachIndexed { index, date ->
                    val calories = dailyCalorieIntake[date] ?: 0f
                    barEntries.add(BarEntry(index.toFloat(), calories))
                    dateLabels.add(date.toString())
                    Log.d("HealthScreen", "Added bar entry for date $date with calories $calories")
                }
            }
            ChartType.WATER_INTAKE -> {
                // Water intake chart logic
                if (selectedPet == null) return@LaunchedEffect
                
                // Get feeding records for selected pet
                val petFeedingEvents = allFeedingEvents.filter { it.petId == selectedPet!!.petId }
                Log.d("HealthScreen", "Found ${petFeedingEvents.size} feeding events for ${selectedPet?.name} (water chart)")
                
                // Check if food list is loaded
                val foodListSize = nutritionViewModel.foodList.value?.size ?: 0
                Log.d("HealthScreen", "Food list size for water chart: $foodListSize")
                
                // Group by date and calculate daily water intake
                val dailyWaterIntake = mutableMapOf<LocalDate, Float>()
                
                petFeedingEvents.forEach { feeding ->
                    val date = feeding.mealTime.toLocalDate()
                    val food = nutritionViewModel.foodList.value?.find { it.foodId == feeding.foodId }
                    
                    if (food != null) {
                        // Calculate water intake from moisture percentage
                        val water = (food.moisturePercentage * feeding.amount / 100) // Calculate water in ml
                        Log.d("HealthScreen", "Adding water: $water for date: $date (food: ${food.name}, moisture: ${food.moisturePercentage}%, amount: ${feeding.amount}g)")
                        dailyWaterIntake[date] = (dailyWaterIntake[date] ?: 0f) + water
                    } else {
                        Log.d("HealthScreen", "Food not found for ID: ${feeding.foodId} (water chart)")
                    }
                }
                
                // Sort by date and create entries
                val sortedDates = dailyWaterIntake.keys.sorted()
                Log.d("HealthScreen", "Created ${sortedDates.size} date entries for water chart")
                
                sortedDates.forEachIndexed { index, date ->
                    val water = dailyWaterIntake[date] ?: 0f
                    barEntries.add(BarEntry(index.toFloat(), water))
                    dateLabels.add(date.toString())
                    Log.d("HealthScreen", "Added bar entry for date $date with water $water ml")
                }
            }
        }
        
        val entriesSize = when (selectedChartType) {
            ChartType.WEIGHT -> lineEntries.size
            else -> barEntries.size
        }
        Log.d("HealthScreen", "Final entries size: $entriesSize")
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
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.health),
                            contentDescription = "Health",
                            modifier = Modifier
                                .size(40.dp)
                                .padding(end = 10.dp)
                        )
                        Text("Health Dashboard", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }
                },
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
                // Tab Row for chart type selection
                TabRow(
                    selectedTabIndex = selectedChartType.ordinal
                ) {
                    chartTitles.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedChartType.ordinal == index,
                            onClick = { selectedChartType = ChartType.values()[index] },
                            text = { Text(title) }
                        )
                    }
                }
                
                // Chart title based on selected type
                val chartTitle = when (selectedChartType) {
                    ChartType.WEIGHT -> "Weight Change for ${selectedPet?.name ?: "Pet"}"
                    ChartType.CALORIE_INTAKE -> "Daily Calorie Intake for ${selectedPet?.name ?: "Pet"}"
                    ChartType.WATER_INTAKE -> "Daily Water Intake (from food) for ${selectedPet?.name ?: "Pet"}"
                }
                
                Text(
                    text = chartTitle,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 12.dp)
                )

                // Chart display with units based on chart type
                val chartUnit = when (selectedChartType) {
                    ChartType.WEIGHT -> "kg"
                    ChartType.CALORIE_INTAKE -> "kcal"
                    ChartType.WATER_INTAKE -> "ml"
                }

                // Render different chart types based on selection
                when (selectedChartType) {
                    ChartType.WEIGHT -> {
                        // Weight Line Chart
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
                                    setNoDataText("No data available")
                                    
                                    // Add extra padding to prevent label clipping
                                    setExtraOffsets(20f, 30f, 20f, 20f)
                                    
                                    // Improve axis appearance
                                    xAxis.setDrawAxisLine(true)
                                    xAxis.setDrawLabels(true)
                                    xAxis.textSize = 10f
                                    
                                    // Ensure Y-axis shows enough range to prevent label clipping
                                    axisLeft.setDrawAxisLine(true)
                                    axisLeft.setDrawLabels(true)
                                    axisLeft.textSize = 10f
                                    
                                    // Improved legend configuration
                                    legend.textSize = 12f
                                    legend.isWordWrapEnabled = true
                        }
                    },
                    update = { chart ->
                        if (lineEntries.isNotEmpty()) {
                                    Log.d("HealthScreen", "Updating line chart with ${lineEntries.size} entries")
                                    val lineDataSet = LineDataSet(lineEntries, chartTitle).apply {
                                colors = ColorTemplate.COLORFUL_COLORS.toList()
                                        
                                        // Bold, clear value labels with large text size
                                        valueTextSize = 14f
                                setDrawValues(true)
                                        valueTextColor = Color.Black.hashCode()
                                        
                                        // Configure for clear label placement
                                        highlightLineWidth = 0f // No highlight line
                                        isHighlightEnabled = false // Disable highlight
                                        
                                        // Using standard formatter for labels
                                valueFormatter = object : com.github.mikephil.charting.formatter.ValueFormatter() {
                                    override fun getPointLabel(entry: Entry?): String {
                                                return entry?.y?.let { String.format("%.1f %s", it, chartUnit) } ?: ""
                                            }
                                        }
                                        
                                        // Set circle and line appearance
                                        setCircleRadius(6f) // Larger circles
                                        setDrawCircleHole(true)
                                        setCircleHoleRadius(4f)
                                        lineWidth = 3f
                                        
                                        // Make sure circles are drawn on top of the line
                                        setDrawCircles(true)
                                        setDrawHighlightIndicators(false)
                                    }
                                    
                                    // Configure chart
                                    chart.data = LineData(lineDataSet)
                                    
                                    // Add extra spacing to ensure labels have room
                                    chart.setExtraOffsets(20f, 50f, 20f, 20f) // More top padding (50f)
                                    chart.setViewPortOffsets(60f, 60f, 60f, 60f)
                                    
                                    // Hide legend to focus on data points and labels
                                    chart.legend.isEnabled = false
                                    
                                    // Configure x-axis
                                    chart.xAxis.apply {
                                        // Use a custom formatter to ensure exact label positioning
                                        valueFormatter = object : IndexAxisValueFormatter() {
                                            override fun getFormattedValue(value: Float): String {
                                                val position = value.toInt()
                                                return if (position >= 0 && position < dateLabels.size) {
                                                    dateLabels[position]
                                                } else {
                                                    ""
                                                }
                                            }
                                        }
                                        
                                        // Critical settings for proper alignment
                                        position = XAxis.XAxisPosition.BOTTOM
                                        setDrawLabels(true)
                                        textSize = 10f
                                        
                                        // Ensure one label per data point
                                        setLabelCount(dateLabels.size, false)
                                        granularity = 1f
                                        
                                        // Reset spacing to defaults
                                        spaceMin = 0f
                                        spaceMax = 0f
                                        
                                        // Set exact boundaries
                                        axisMinimum = -0.5f
                                        axisMaximum = (lineEntries.size - 1) + 0.5f
                                    }
                                    
                                    // Configure y-axis
                                    chart.axisLeft.apply {
                                        setDrawGridLines(false)
                                        spaceTop = 40f // Increased space at top for labels
                                        textSize = 10f
                                        textColor = Color.DarkGray.hashCode()
                                        
                                        // If there are data points, adjust the axis range to accommodate labels
                                        if (lineEntries.isNotEmpty()) {
                                            // Find the maximum y value in the data
                                            val maxYValue = lineEntries.maxOf { it.y }
                                            
                                            // Add about 20% extra space above the max value for labels
                                            val extraSpace = maxYValue * 0.2f
                                            
                                            // Set the axis maximum with extra space
                                            axisMaximum = maxYValue + extraSpace
                                        }
                                    }
                                    
                                    // Animate chart
                                    chart.animateY(1000)
                                    
                                    // Force a full redraw
                                    chart.notifyDataSetChanged()
                                    chart.invalidate()
                                } else {
                                    Log.d("HealthScreen", "No line entries available, clearing chart")
                                    chart.data = null
                                    chart.invalidate()
                                }
                            }
                        )
                    }
                    else -> {
                        // Bar Chart for Calorie and Water Intake
                        AndroidView(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(250.dp),
                            factory = { context ->
                                BarChart(context).apply {
                                    description.isEnabled = false
                                    xAxis.position = XAxis.XAxisPosition.BOTTOM
                                    axisRight.isEnabled = false
                                    axisLeft.setDrawGridLines(false)
                                    xAxis.setDrawGridLines(false)
                                    setTouchEnabled(true)
                                    setPinchZoom(true)
                                    setNoDataText("No data available")
                                    
                                    // Add extra padding to prevent label clipping
                                    setExtraOffsets(10f, 30f, 10f, 10f)
                                    
                                    // Improve axis appearance
                                    xAxis.setDrawAxisLine(true)
                                    xAxis.setDrawLabels(true)
                                    xAxis.textSize = 10f
                                    
                                    // Ensure Y-axis shows enough range to prevent label clipping
                                    axisLeft.setDrawAxisLine(true)
                                    axisLeft.setDrawLabels(true)
                                    axisLeft.textSize = 10f
                                    
                                    // Improved legend configuration
                                    legend.textSize = 12f
                                    legend.isWordWrapEnabled = true
                                    
                                    // Important: Set minimum visible count to prevent ArrayIndexOutOfBoundsException
                                    setVisibleXRangeMinimum(1f)
                                    
                                    // Fix for ArrayIndexOutOfBoundsException by setting proper buffer size
                                    setDrawValueAboveBar(true)
                                    setFitBars(true)
                                }
                            },
                            update = { chart ->
                                try {
                                    // Clear previous data first to avoid any inconsistencies
                                    chart.clear()
                                    
                                    if (barEntries.isNotEmpty()) {
                                        Log.d("HealthScreen", "Updating bar chart with ${barEntries.size} entries")
                                        
                                        // Ensure we have at least 5 entries to avoid buffer issues
                                        val paddedEntries = ArrayList<BarEntry>(barEntries)
                                        
                                        // Add dummy entries if needed to prevent ArrayIndexOutOfBoundsException
                                        // These will be hidden off-screen but help maintain buffer integrity
                                        while (paddedEntries.size < 5) {
                                            // Add dummy entries with negative x values (off-screen)
                                            paddedEntries.add(BarEntry(-10f - paddedEntries.size, 0f))
                                        }
                                        
                                        val barDataSet = BarDataSet(paddedEntries, chartTitle).apply {
                                            colors = ColorTemplate.COLORFUL_COLORS.toList()
                                            
                                            // Only draw values for real entries (not dummy entries)
                                            setDrawValues(true)
                                            valueTextSize = 14f
                                            valueTextColor = Color.Black.hashCode()
                                            
                                            // Using standard formatter for labels with null safety
                                            valueFormatter = object : com.github.mikephil.charting.formatter.ValueFormatter() {
                                                override fun getBarLabel(entry: BarEntry?): String {
                                                    // Only show labels for real entries (x >= 0)
                                                    return if (entry != null && entry.x >= 0) {
                                                        String.format("%.1f %s", entry.y, chartUnit)
                                                    } else {
                                                        ""
                                                    }
                                                }
                                            }
                                        }
                                        
                                        // Configure chart
                                        val barData = BarData(barDataSet)
                                        barData.barWidth = 0.5f // Narrower bars to avoid collision
                                        
                                        chart.data = barData
                                        
                                        // Add extra spacing to ensure labels have room
                                        chart.setExtraOffsets(20f, 50f, 20f, 20f)
                                        chart.setViewPortOffsets(60f, 60f, 60f, 60f)
                                        
                                        // Hide legend to focus on data
                                        chart.legend.isEnabled = false
                                        
                                        // Configure x-axis
                            chart.xAxis.apply {
                                // Use a custom formatter to ensure exact label positioning
                                valueFormatter = object : IndexAxisValueFormatter() {
                                    override fun getFormattedValue(value: Float): String {
                                        val position = value.toInt()
                                        return if (position >= 0 && position < dateLabels.size) {
                                            dateLabels[position]
                                        } else {
                                            ""
                                        }
                                    }
                                }
                                
                                // Critical settings for proper alignment
                                position = XAxis.XAxisPosition.BOTTOM
                                setDrawLabels(true)
                                textSize = 10f
                                
                                // Ensure one label per data point
                                setLabelCount(dateLabels.size, false)
                                granularity = 1f
                                
                                // Reset spacing to defaults
                                spaceMin = 0f
                                spaceMax = 0f
                                
                                // Set exact boundaries
                                axisMinimum = -0.5f
                                axisMaximum = (barEntries.size - 1) + 0.5f
                            }
                                        
                                        // Configure y-axis
                                        chart.axisLeft.apply {
                                            setDrawGridLines(false)
                                            spaceTop = 40f // Increased space at top for labels
                                            textSize = 10f
                                            textColor = Color.DarkGray.hashCode()
                                            
                                            // Set y-axis minimum to zero to ensure bars start from the axis
                                            axisMinimum = 0f 
                                            
                                            // If there are data points, adjust the axis range to accommodate labels
                                            if (barEntries.isNotEmpty()) {
                                                val maxYValue = barEntries.maxOf { it.y }
                                                val extraSpace = maxYValue * 0.2f
                                                axisMaximum = maxYValue + extraSpace
                                            }
                                            
                                            // Ensure gridlines align with axis labels
                                            setLabelCount(5, true)
                                            
                                            // Remove decimal places from y-axis values for cleaner appearance
                                            valueFormatter = object : com.github.mikephil.charting.formatter.ValueFormatter() {
                                                override fun getAxisLabel(value: Float, axis: com.github.mikephil.charting.components.AxisBase?): String {
                                                    return value.toInt().toString()
                                                }
                                            }
                                        }
                                        
                                        // Set visible range to only show the real entries
                                        if (barEntries.size > 0) {
                                            chart.setVisibleXRangeMaximum(barEntries.size.toFloat())
                                            chart.setVisibleXRangeMinimum(1f)
                                            chart.moveViewToX(0f) // Start from the first real entry
                                        }
                                        
                                        // Force layout calculation
                                        chart.notifyDataSetChanged()
                                        
                                        // Animate chart
                                        chart.animateY(1000)
                            chart.invalidate()
                        } else {
                                        Log.d("HealthScreen", "No bar entries available, clearing chart")
                                        chart.data = null
                                        chart.invalidate()
                                    }
                                } catch (e: Exception) {
                                    // Handle any exceptions during chart update
                                    Log.e("HealthScreen", "Error updating chart: ${e.message}", e)
                            chart.data = null
                            chart.invalidate()
                        }
                            }
                        )
                    }
                }

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
                                    .padding(4.dp),
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
                                            Image(
                                                painter = painterResource(id = R.drawable.pencil),
                                                contentDescription = "Edit record",
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        IconButton(
                                            onClick = {
                                                recordToDelete = record
                                                showDeleteConfirmation = true
                                            }
                                        ) {
                                            Image(
                                                painter = painterResource(id = R.drawable.bin),
                                                contentDescription = "Delete record",
                                                modifier = Modifier.size (20.dp)
                                            )
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