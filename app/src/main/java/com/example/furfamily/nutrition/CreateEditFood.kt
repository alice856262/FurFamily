package com.example.furfamily.nutrition

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.furfamily.ViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddNewFood(onDismiss: () -> Unit, onSave: (Food) -> Unit, viewModel: ViewModel, navController: NavController) {
    var name by remember { mutableStateOf("") }
    var ingredient by remember { mutableStateOf("") }
    var caloriesPerKg by remember { mutableStateOf(0.0F) }
    var size by remember { mutableStateOf(0) }
    var proteinPercentage by remember { mutableStateOf(0.0F) }
    var fatPercentage by remember { mutableStateOf(0.0F) }
    var fiberPercentage by remember { mutableStateOf(0.0F) }
    var moisturePercentage by remember { mutableStateOf(0.0F) }
    var ashPercentage by remember { mutableStateOf(0.0F) }
    var feedingInfo by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var extractedInfo by remember { mutableStateOf("") }
    var showLoading by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Create New Food") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back")
                    }
                }
            )
        },
        content = { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalAlignment = Alignment.Start // Align text to the left side
                ) {
                    Text("You can manually enter the food information or upload an image to extract the details!",
                        style = MaterialTheme.typography.titleMedium)
                    Text("Food name and calories are required!",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 8.dp))
                }

                // Image Preview Section
                imageUri?.let { uri ->
                    Image(
                        painter = rememberAsyncImagePainter(uri),
                        contentDescription = "Uploaded Image",
                        modifier = Modifier
                            .size(150.dp) // Adjust the size as needed
                            .padding(top = 8.dp)
                            .clip(RoundedCornerShape(8.dp)) // Add rounded corners
                            .border(1.dp, Color.LightGray) // Add border
                    )
                }

                // Choose Image Button
                val pickImage = rememberLauncherForActivityResult(
                    ActivityResultContracts.GetContent()
                ) { uri ->
                    uri?.let { imageUri = uri }
                }
                Button(
                    onClick = { pickImage.launch("image/*") },
                    modifier = Modifier
                        .wrapContentWidth() // Adjust button width to fit the text
                ) {
                    Text("Choose an Image")
                }

                // Extract Information Button
                imageUri?.let { uri ->
                    Button(
                        onClick = {
                            showLoading = true
                            viewModel.uploadImageToFirebase(uri,
                                onSuccess = { imageUrl ->
                                    // After successful upload, extract text from the image
                                    viewModel.extractTextFromImage(context, uri,
                                        onSuccess = { extractedText ->
                                            // After successful text extraction, send text to OpenAI
                                            viewModel.sendTextToOpenAI(extractedText,
                                                onSuccess = { foodInfo ->
                                                    // Update UI fields with the returned Food object
                                                    name = foodInfo.name
                                                    ingredient = foodInfo.ingredient
                                                    caloriesPerKg = foodInfo.caloriesPerKg
                                                    size = foodInfo.size
                                                    proteinPercentage = foodInfo.proteinPercentage
                                                    fatPercentage = foodInfo.fatPercentage
                                                    fiberPercentage = foodInfo.fiberPercentage
                                                    moisturePercentage = foodInfo.moisturePercentage
                                                    ashPercentage = foodInfo.ashPercentage
                                                    feedingInfo = foodInfo.feedingInfo
                                                    notes = foodInfo.notes
                                                    extractedInfo =
                                                        "Food details retrieved successfully. Please check and update if needed."
                                                    showLoading = false // Hide loading indicator
                                                },
                                                onError = { openAIError ->
                                                    // Handle OpenAI error
                                                    extractedInfo = "OpenAI Error: $openAIError"
                                                    showLoading = false // Hide loading indicator
                                                })
                                        },
                                        onError = { extractionError ->
                                            // Handle text extraction error
                                            extractedInfo = "Text Extraction Error: $extractionError"
                                            showLoading = false // Hide loading indicator
                                        })
                                },
                                onError = { uploadError ->
                                    // Handle image upload error
                                    extractedInfo = "Image Upload Error: $uploadError"
                                    showLoading = false // Hide loading indicator
                                })
                        },
                        modifier = Modifier
                            .wrapContentWidth() // Adjust button width to fit the text
                    ) {
                        Text("Retrieve Food Details")
                    }
                }

                // Loading Indicator
                if (showLoading) {
                    CircularProgressIndicator(modifier = Modifier.padding(vertical = 8.dp))
                }

                // Extracted Information
                if (extractedInfo.isNotEmpty()) {
                    Text(extractedInfo,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .padding(vertical = 8.dp)
                            .fillMaxWidth()
                    )
                }

                // Food Fields
                TextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Food Name *") },
                    modifier = Modifier
                        .padding(vertical = 4.dp)
                        .fillMaxWidth() // Set max width for the TextField
                )
                TextField(
                    value = caloriesPerKg.toString(),
                    onValueChange = { input ->
                        val parsedValue = input.toFloatOrNull()
                        if (parsedValue != null) {
                            caloriesPerKg = parsedValue
                        }
                    },
                    label = { Text("Calories (kcal/kg) *") },
                    modifier = Modifier
                        .padding(vertical = 4.dp)
                        .fillMaxWidth(), // Set max width for the TextField
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                TextField(
                    value = ingredient,
                    onValueChange = { ingredient = it },
                    label = { Text("Ingredient") },
                    modifier = Modifier
                        .padding(vertical = 4.dp)
                        .fillMaxWidth() // Set max width for the TextField
                )
                TextField(
                    value = size.toString(),
                    onValueChange = { input ->
                        val parsedValue = input.toIntOrNull()
                        if (parsedValue != null) {
                            size = parsedValue
                        }
                    },
                    label = { Text("Size (g)") },
                    modifier = Modifier
                        .padding(vertical = 4.dp)
                        .fillMaxWidth(), // Set max width for the TextField
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                TextField(
                    value = proteinPercentage.toString(),
                    onValueChange = { input ->
                        val parsedValue = input.toFloatOrNull()
                        if (parsedValue != null) {
                            proteinPercentage = parsedValue
                        }
                    },
                    label = { Text("Protein (%)") },
                    modifier = Modifier
                        .padding(vertical = 4.dp)
                        .fillMaxWidth(), // Set max width for the TextField
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                TextField(
                    value = fatPercentage.toString(),
                    onValueChange = { input ->
                        val parsedValue = input.toFloatOrNull()
                        if (parsedValue != null) {
                            fatPercentage = parsedValue
                        }
                    },
                    label = { Text("Fat (%)") },
                    modifier = Modifier
                        .padding(vertical = 4.dp)
                        .fillMaxWidth(), // Set max width for the TextField
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                TextField(
                    value = fiberPercentage.toString(),
                    onValueChange = { input ->
                        val parsedValue = input.toFloatOrNull()
                        if (parsedValue != null) {
                            fiberPercentage = parsedValue
                        }
                    },
                    label = { Text("Fiber (%)") },
                    modifier = Modifier
                        .padding(vertical = 4.dp)
                        .fillMaxWidth(), // Set max width for the TextField
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                TextField(
                    value = moisturePercentage.toString(),
                    onValueChange = { input ->
                        val parsedValue = input.toFloatOrNull()
                        if (parsedValue != null) {
                            moisturePercentage = parsedValue
                        }
                    },
                    label = { Text("Moisture (%)") },
                    modifier = Modifier
                        .padding(vertical = 4.dp)
                        .fillMaxWidth(), // Set max width for the TextField
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                TextField(
                    value = ashPercentage.toString(),
                    onValueChange = { input ->
                        val parsedValue = input.toFloatOrNull()
                        if (parsedValue != null) {
                            ashPercentage = parsedValue
                        }
                    },
                    label = { Text("Ash (%)") },
                    modifier = Modifier
                        .padding(vertical = 4.dp)
                        .fillMaxWidth(), // Set max width for the TextField
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                TextField(
                    value = feedingInfo,
                    onValueChange = { feedingInfo = it },
                    label = { Text("Feeding Guidance") },
                    modifier = Modifier
                        .padding(vertical = 4.dp)
                        .fillMaxWidth() // Set max width for the TextField
                )
                TextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes") },
                    modifier = Modifier
                        .padding(vertical = 4.dp)
                        .fillMaxWidth() // Set max width for the TextField
                )
                Button(
                    onClick = {
                        val food = Food(name = name,
                                        ingredient = ingredient,
                                        caloriesPerKg = caloriesPerKg,
                                        size = size,
                                        proteinPercentage = proteinPercentage,
                                        fatPercentage = fatPercentage,
                                        fiberPercentage = fiberPercentage,
                                        moisturePercentage = moisturePercentage,
                                        ashPercentage = ashPercentage,
                                        feedingInfo = feedingInfo,
                                        notes = notes)
                        onSave(food)
                        onDismiss()
                    },
                    enabled = name.isNotBlank() && caloriesPerKg > 0, // Enable only when required fields are filled
                    modifier = Modifier
                        .fillMaxWidth() // Set button to max width
                        .padding(vertical = 8.dp)
                ) {
                    Text("Save")
                }
            }
        }
    )
}

@Composable
fun EditFoodDialog(food: Food, onDismiss: () -> Unit, onSave: (Food) -> Unit) {
    var name by remember { mutableStateOf(food.name) }
    var ingredient by remember { mutableStateOf(food.ingredient) }
    var caloriesPerKg by remember { mutableStateOf(food.caloriesPerKg) }
    var size by remember { mutableStateOf(food.size) }
    var proteinPercentage by remember { mutableStateOf(food.proteinPercentage) }
    var fatPercentage by remember { mutableStateOf(food.fatPercentage) }
    var fiberPercentage by remember { mutableStateOf(food.fiberPercentage) }
    var moisturePercentage by remember { mutableStateOf(food.moisturePercentage) }
    var ashPercentage by remember { mutableStateOf(food.ashPercentage) }
    var feedingInfo by remember { mutableStateOf(food.feedingInfo) }
    var notes by remember { mutableStateOf(food.notes) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Food", style = MaterialTheme.typography.titleMedium) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()) // Allows scrolling
                    .padding(16.dp)
            ) {
                // Food Fields for editing
                TextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Food Name *") },
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                TextField(
                    value = caloriesPerKg.toString(),
                    onValueChange = { input ->
                        val parsedValue = input.toFloatOrNull()
                        if (parsedValue != null) {
                            caloriesPerKg = parsedValue
                        }
                    },
                    label = { Text("Calories (kcal/kg) *") },
                    modifier = Modifier.padding(vertical = 4.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                TextField(
                    value = ingredient,
                    onValueChange = { ingredient = it },
                    label = { Text("Ingredient") },
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                TextField(
                    value = size.toString(),
                    onValueChange = { input ->
                        val parsedValue = input.toIntOrNull()
                        if (parsedValue != null) {
                            size = parsedValue
                        }
                    },
                    label = { Text("Size (g)") },
                    modifier = Modifier.padding(vertical = 4.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                TextField(
                    value = proteinPercentage.toString(),
                    onValueChange = { input ->
                        val parsedValue = input.toFloatOrNull()
                        if (parsedValue != null) {
                            proteinPercentage = parsedValue
                        }
                    },
                    label = { Text("Protein (%)") },
                    modifier = Modifier.padding(vertical = 4.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                TextField(
                    value = fatPercentage.toString(),
                    onValueChange = { input ->
                        val parsedValue = input.toFloatOrNull()
                        if (parsedValue != null) {
                            fatPercentage = parsedValue
                        }
                    },
                    label = { Text("Fat (%)") },
                    modifier = Modifier.padding(vertical = 4.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                TextField(
                    value = fiberPercentage.toString(),
                    onValueChange = { input ->
                        val parsedValue = input.toFloatOrNull()
                        if (parsedValue != null) {
                            fiberPercentage = parsedValue
                        }
                    },
                    label = { Text("Fiber (%)") },
                    modifier = Modifier.padding(vertical = 4.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                TextField(
                    value = moisturePercentage.toString(),
                    onValueChange = { input ->
                        val parsedValue = input.toFloatOrNull()
                        if (parsedValue != null) {
                            moisturePercentage = parsedValue
                        }
                    },
                    label = { Text("Moisture (%)") },
                    modifier = Modifier.padding(vertical = 4.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                TextField(
                    value = ashPercentage.toString(),
                    onValueChange = { input ->
                        val parsedValue = input.toFloatOrNull()
                        if (parsedValue != null) {
                            ashPercentage = parsedValue
                        }
                    },
                    label = { Text("Ash (%)") },
                    modifier = Modifier.padding(vertical = 4.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                TextField(
                    value = feedingInfo,
                    onValueChange = { feedingInfo = it },
                    label = { Text("Feeding Guidance") },
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                TextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes") },
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val updatedFood = food.copy(
                        name = name,
                        ingredient = ingredient,
                        caloriesPerKg = caloriesPerKg,
                        proteinPercentage = proteinPercentage,
                        fatPercentage = fatPercentage,
                        fiberPercentage = fiberPercentage,
                        moisturePercentage = moisturePercentage,
                        ashPercentage = ashPercentage,
                        notes = notes)
                    onSave(updatedFood)
                },
                enabled = name.isNotBlank() && caloriesPerKg > 0 // Enable only when required fields are filled
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) { Text("Cancel") }
        }
    )
}