package com.example.furfamily.nutrition

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.furfamily.R
import com.example.furfamily.Routes
import com.example.furfamily.getCurrentUserId
import com.example.furfamily.profile.Pet
import com.example.furfamily.viewmodel.NutritionViewModel
import com.example.furfamily.viewmodel.ProfileViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NutritionScreen(navController: NavController) {
    val nutritionViewModel: NutritionViewModel = hiltViewModel()
    val profileViewModel: ProfileViewModel = hiltViewModel()

    val userId = getCurrentUserId()
    val foods by nutritionViewModel.foodList.observeAsState(emptyList())
    val pets by profileViewModel.pets.observeAsState(emptyList())
    val petWeights by nutritionViewModel.petWeights.observeAsState(emptyMap())
    val allFeedingEvents by nutritionViewModel.allFeedingEvents.observeAsState(emptyList())
    
    var isPetDropdownExpanded by remember { mutableStateOf(false) }
    var selectedPet by remember { mutableStateOf<Pet?>(null) }
    var selectedFood by remember { mutableStateOf<Food?>(null) }
    var showSnackbar by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf("") }
    var showEditFoodDialog by remember { mutableStateOf(false) }
    var showFeedingDialog by remember { mutableStateOf(false) }
    var showEditFeedingDialog by remember { mutableStateOf(false) }
    var selectedFeeding by remember { mutableStateOf<Feeding?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Format for displaying dates
    val dateFormatter = remember { DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm") }

    LaunchedEffect(Unit) {
        nutritionViewModel.loadFoodList()
        if (userId != null) {
            profileViewModel.loadPetsProfile(userId)
        }
        nutritionViewModel.fetchLatestWeight()
        // Load today's feeding events by default
        nutritionViewModel.loadFeedingEventsForDate(LocalDate.now())
        // Load all feeding events
        nutritionViewModel.loadAllFeedingEvents()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.nutrition),
                            contentDescription = "Food",
                            modifier = Modifier
                                .size(40.dp)
                                .padding(end = 10.dp)
                        )
                        Text("Food and Feeding", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate(Routes.NewFood.value) }) {
                        Icon(Icons.Filled.Add, contentDescription = "Add New Food")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        content = { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    // Pet Dropdown
                    Text(
                        text = "You can add a feeding record here!\nFirst, select a pet name:",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                    ExposedDropdownMenuBox(
                        expanded = isPetDropdownExpanded,
                        onExpandedChange = { isPetDropdownExpanded = !isPetDropdownExpanded }
                    ) {
                        TextField(
                            readOnly = true,
                            value = selectedPet?.name ?: "Please choose one",
                            onValueChange = { /* No-op */ },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isPetDropdownExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = isPetDropdownExpanded,
                            onDismissRequest = { isPetDropdownExpanded = false }
                        ) {
                            pets.forEach { pet ->
                                DropdownMenuItem(
                                    text = { Text(pet.name) },
                                    onClick = {
                                        selectedPet = pet
                                        isPetDropdownExpanded = false
                                        // Load feeding events for the selected pet
                                        nutritionViewModel.loadFeedingEventsForDate(LocalDate.now())
                                    }
                                )
                            }
                        }
                    }

                    // Food List
                    Text(
                        text = "Then, select a food product:",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(foods) { food ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (selectedPet == null) {
                                            snackbarMessage = "Please select a pet first!"
                                            showSnackbar = true
                                        } else {
                                            selectedFood = food
                                            showFeedingDialog = true
                                        }
                                    }
                                    .padding(vertical = 8.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f).padding(8.dp)) {
                                    Text(food.name, style = MaterialTheme.typography.bodyLarge)
                                    Text("Calories: ${food.caloriesPerKg} kcal/kg", style = MaterialTheme.typography.bodySmall)
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                    IconButton(
                                        onClick = {
                                            showEditFoodDialog = true;
                                            selectedFood = food
                                        }
                                    ) {
                                        Image(
                                            painter = painterResource (id = R. drawable.pencil),
                                            contentDescription = "Edit Food",
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            nutritionViewModel.deleteFood(food)
                                        }
                                    ) {
                                        Image(
                                            painter = painterResource (id = R. drawable.bin),
                                            contentDescription = "Delete Food",
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                            Divider()
                        }
                    }

                    // Feeding Records Section
                    Text(
                        text = "Feeding Records",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )

                    // Filter feeding events by selected pet if one is selected
                    val filteredFeedings = if (selectedPet != null) {
                        allFeedingEvents.filter { it.petId == selectedPet!!.petId }
                    } else {
                        allFeedingEvents
                    }

                    if (filteredFeedings.isEmpty()) {
                        Text(
                            text = "No feeding records found. Add a feeding record by selecting a pet and food above.",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        LazyColumn(modifier = Modifier.weight(1f)) {
                            items(filteredFeedings) { feeding ->
                                // Find the food and pet for this feeding record
                                val food = foods.find { it.foodId == feeding.foodId }
                                val pet = pets.find { it.petId == feeding.petId }
                                
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedFeeding = feeding
                                            showEditFeedingDialog = true
                                        }
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Pet profile image (circular)
                                    if (pet != null) {
                                        PetProfileImage(
                                            imageUrl = pet.profileImageUrl,
                                            modifier = Modifier.padding(end = 8.dp)
                                        )
                                    } else {
                                        // Placeholder circle if pet is not found
                                        Box(
                                            modifier = Modifier
                                                .padding(end = 8.dp)
                                                .size(48.dp)
                                                .clip(CircleShape)
                                                .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                        )
                                    }
                                    
                                    Column(modifier = Modifier.weight(1f).padding(8.dp)) {
                                        Text(
                                            text = pet?.name ?: "Unknown Pet",
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                        Text(
                                            text = "${food?.name ?: "Unknown Food"}: ${feeding.amount} g",
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                        Text(
                                            text = "Meal: ${feeding.mealType}",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                        Text(
                                            text = "Time: ${feeding.mealTime.format(dateFormatter)}",
                                            style = MaterialTheme.typography.bodySmall
                                        )

                                        if (feeding.notes.isNotEmpty()) {
                                            Text(
                                                text = "Notes: ${feeding.notes}",
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                        IconButton(
                                            onClick = {
                                                selectedFeeding = feeding
                                                showEditFeedingDialog = true
                                            }
                                        ) {
                                            Image(
                                                painter = painterResource(id = R.drawable.pencil),
                                                contentDescription = "Edit Feeding",
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        IconButton(
                                            onClick = {
                                                nutritionViewModel.deleteFeeding(feeding)
                                                snackbarMessage = "Feeding record deleted"
                                                showSnackbar = true
                                            }
                                        ) {
                                            Image(
                                                painter = painterResource(id = R.drawable.bin),
                                                contentDescription = "Delete Feeding",
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                                Divider()
                            }
                        }
                    }

                    if (showSnackbar) {
                        LaunchedEffect(snackbarMessage) {
                            snackbarHostState.showSnackbar(snackbarMessage)
                            showSnackbar = false
                        }
                    }
                }
            }
        }
    )

    // Edit Food Dialog
    if (showEditFoodDialog && selectedFood != null) {
        EditFoodDialog(
            food = selectedFood!!,
            onDismiss = { showEditFoodDialog = false },
            onSave = { updatedFood ->
                nutritionViewModel.updateFood(updatedFood)
                showEditFoodDialog = false
            }
        )
    }

    // Feeding Record Dialog
    if (showFeedingDialog && selectedPet != null && selectedFood != null) {
        CreateFeedingRecord(
            selectedPet = selectedPet!!,
            selectedFood = selectedFood!!,
            latestWeight = petWeights[selectedPet!!.petId],
            onSave = { amount, mealTime, mealType, notes, lifestyle, gestationLactation ->
                nutritionViewModel.saveFeeding(selectedPet!!, selectedFood!!, amount, mealTime, mealType, notes)
                showFeedingDialog = false
                snackbarMessage = "Feeding record added successfully!"
                showSnackbar = true
                // Reload all feeding events after adding a new one
                nutritionViewModel.loadAllFeedingEvents()
            },
            onDismiss = { showFeedingDialog = false }
        )
    }

    // Edit Feeding Dialog
    if (showEditFeedingDialog && selectedFeeding != null) {
        val food = foods.find { it.foodId == selectedFeeding!!.foodId }
        val pet = pets.find { it.petId == selectedFeeding!!.petId }
        
        if (food != null && pet != null) {
            EditFeedingDialog(
                feeding = selectedFeeding!!,
                pet = pet,
                food = food,
                onDismiss = { showEditFeedingDialog = false },
                onSave = { updatedFeeding ->
                    nutritionViewModel.updateFeeding(updatedFeeding)
                    showEditFeedingDialog = false
                    snackbarMessage = "Feeding record updated successfully!"
                    showSnackbar = true
                    // Reload all feeding events after updating
                    nutritionViewModel.loadAllFeedingEvents()
                }
            )
        }
    }
}

@Composable
fun PetProfileImage(imageUrl: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val imagePainter = rememberAsyncImagePainter(
        ImageRequest.Builder(context)
            .data(data = imageUrl)
            .crossfade(true)
            .build()
    )
    
    Box(
        modifier = modifier
            .size(48.dp)  // Fixed size to ensure perfect circle
            .clip(CircleShape)
            .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
    ) {
        Image(
            painter = imagePainter,
            contentDescription = "Pet Profile Image",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    }
}
